package ru.practicum.shareit.item;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.mapper.BookingMapper;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemCreateDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemOwnerDto;
import ru.practicum.shareit.item.dto.ItemUpdateDto;
import ru.practicum.shareit.item.mapper.CommentMapper;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {

    private static final Sort COMMENTS_SORT = Sort.by(Sort.Direction.ASC, "created");

    private final ItemMapper itemMapper;
    private final BookingMapper bookingMapper;
    private final CommentMapper commentMapper;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;

    public ItemServiceImpl(ItemMapper itemMapper,
                           UserRepository userRepository,
                           ItemRepository itemRepository,
                           BookingRepository bookingRepository,
                           BookingMapper bookingMapper,
                           CommentRepository commentRepository,
                           CommentMapper commentMapper) {
        this.itemMapper = itemMapper;
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
        this.commentRepository = commentRepository;
        this.commentMapper = commentMapper;
    }

    @Transactional
    @Override
    public ItemDto create(Long ownerId, ItemCreateDto dto) {
        log.info("Create item by ownerId={}", ownerId);

        validateCreate(dto);

        User owner = getUserOrThrow(ownerId);

        Item item = itemMapper.toModel(dto);
        item.setOwner(owner);

        Item saved = itemRepository.save(item);
        ItemDto result = itemMapper.toDto(saved);
        result.setComments(List.of());
        return result;
    }

    @Transactional
    @Override
    public ItemDto update(Long ownerId, Long itemId, ItemUpdateDto dto) {
        log.info("Update item id={} by ownerId={}", itemId, ownerId);

        Item item = getItemOrThrow(itemId);
        validateOwner(item, ownerId);

        validateNameUpdate(dto.getName());
        validateDescriptionUpdate(dto.getDescription());

        itemMapper.updateItemFromDto(dto, item);

        Item saved = itemRepository.save(item);
        ItemDto result = itemMapper.toDto(saved);
        result.setComments(loadCommentsForItem(itemId));
        return result;
    }

    @Override
    public ItemDto getById(Long userId, Long itemId) {
        log.info("Get item id={} by userId={}", itemId, userId);

        Item item = getItemOrThrow(itemId);
        ItemDto dto = itemMapper.toDto(item);
        dto.setComments(loadCommentsForItem(itemId));

        if (userId != null && item.getOwner() != null && Objects.equals(item.getOwner().getId(), userId)) {
            LocalDateTime now = LocalDateTime.now();

            bookingRepository.findLastBooking(item.getId(), now).stream()
                    .findFirst()
                    .map(bookingMapper::toDto)
                    .ifPresent(dto::setLastBooking);

            bookingRepository.findNextBooking(item.getId(), now).stream()
                    .findFirst()
                    .map(bookingMapper::toDto)
                    .ifPresent(dto::setNextBooking);
        }

        return dto;
    }

    @Override
    public List<ItemOwnerDto> getByOwner(Long ownerId) {
        log.info("Get items with bookings by ownerId={}", ownerId);
        getUserOrThrow(ownerId);

        LocalDateTime now = LocalDateTime.now();

        List<Item> items = itemRepository.findAllByOwner_Id(ownerId);

        Map<Long, List<CommentDto>> commentsByItemId = loadCommentsForItems(
                items.stream().map(Item::getId).toList()
        );

        return items.stream()
                .map(item -> toOwnerDto(item, now, commentsByItemId.getOrDefault(item.getId(), List.of())))
                .toList();
    }

    @Override
    public List<ItemDto> search(String text) {
        log.info("Search items by text='{}'", text);

        if (text == null || text.isBlank()) {
            return List.of();
        }

        return itemRepository.search(text).stream()
                .sorted(Comparator.comparing(Item::getId))
                .map(itemMapper::toDto)
                .peek(dto -> dto.setComments(loadCommentsForItem(dto.getId())))
                .toList();
    }

    @Transactional
    @Override
    public CommentDto addComment(Long authorId, Long itemId, CommentCreateDto dto) {
        log.info("Add comment: authorId={}, itemId={}", authorId, itemId);

        if (dto.getText() == null || dto.getText().isBlank()) {
            throw new BadRequestException("Comment text must not be blank");
        }

        User author = getUserOrThrow(authorId);
        Item item = getItemOrThrow(itemId);

        boolean hasPastApprovedBooking = bookingRepository.existsByItem_IdAndBooker_IdAndStatusAndEndIsBefore(
                itemId,
                authorId,
                BookingStatus.APPROVED,
                LocalDateTime.now()
        );

        if (!hasPastApprovedBooking) {
            throw new BadRequestException("User has not completed approved booking for this item");
        }

        Comment comment = new Comment();
        comment.setText(dto.getText());
        comment.setAuthor(author);
        comment.setItem(item);

        Comment saved = commentRepository.save(comment);
        return commentMapper.toDto(saved);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private Item getItemOrThrow(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found"));
    }

    private void validateOwner(Item item, Long ownerId) {
        if (item.getOwner() == null || !Objects.equals(item.getOwner().getId(), ownerId)) {
            throw new NotFoundException("Item not found for this owner");
        }
    }

    private void validateCreate(ItemCreateDto dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new BadRequestException("Item name must not be blank");
        }
        if (dto.getDescription() == null || dto.getDescription().isBlank()) {
            throw new BadRequestException("Item description must not be blank");
        }
        if (dto.getAvailable() == null) {
            throw new BadRequestException("Item availability must be provided");
        }
    }

    private void validateNameUpdate(String name) {
        if (name != null && name.isBlank()) {
            throw new BadRequestException("Item name must not be blank");
        }
    }

    private void validateDescriptionUpdate(String desc) {
        if (desc != null && desc.isBlank()) {
            throw new BadRequestException("Item description must not be blank");
        }
    }

    private ItemOwnerDto toOwnerDto(Item item, LocalDateTime now, List<CommentDto> comments) {
        ItemOwnerDto dto = new ItemOwnerDto();
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setDescription(item.getDescription());
        dto.setAvailable(item.isAvailable());
        dto.setComments(comments);

        bookingRepository.findLastBooking(item.getId(), now).stream()
                .findFirst()
                .map(bookingMapper::toDto)
                .ifPresent(dto::setLastBooking);

        bookingRepository.findNextBooking(item.getId(), now).stream()
                .findFirst()
                .map(bookingMapper::toDto)
                .ifPresent(dto::setNextBooking);

        return dto;
    }

    private List<CommentDto> loadCommentsForItem(Long itemId) {
        return commentRepository.findAllByItem_Id(itemId, COMMENTS_SORT).stream()
                .map(commentMapper::toDto)
                .toList();
    }


    private Map<Long, List<CommentDto>> loadCommentsForItems(List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Map.of();
        }

        return commentRepository.findAllByItem_IdIn(itemIds, COMMENTS_SORT).stream()
                .collect(Collectors.groupingBy(
                        c -> c.getItem().getId(),
                        Collectors.mapping(commentMapper::toDto, Collectors.toList())
                ));
    }

}
