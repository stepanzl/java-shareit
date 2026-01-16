package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.mapper.BookingMapper;
import ru.practicum.shareit.booking.model.Booking;
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
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private static final Sort COMMENTS_SORT = Sort.by(Sort.Direction.ASC, "created");
    private static final Sort BOOKINGS_SORT = Sort.by(Sort.Direction.ASC, "start");

    private final ItemMapper itemMapper;
    private final CommentMapper commentMapper;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;
    private final ItemRequestRepository itemRequestRepository;

    @Transactional
    @Override
    public ItemDto create(Long ownerId, ItemCreateDto dto) {
        log.info("Create item by ownerId={}", ownerId);

        validateCreate(dto);

        User owner = getUserOrThrow(ownerId);

        Item item = itemMapper.toModel(dto);
        item.setOwner(owner);

        if (dto.getRequestId() != null) {
            ItemRequest request = itemRequestRepository.findById(dto.getRequestId())
                    .orElseThrow(() -> new NotFoundException("Request not found: " + dto.getRequestId()));
            item.setRequest(request);
        }

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
            List<Booking> approvedBookings = loadApprovedBookingsForItem(item.getId());

            selectLastBooking(approvedBookings, now)
                    .map(BookingMapper::toDto)
                    .ifPresent(dto::setLastBooking);

            selectNextBooking(approvedBookings, now)
                    .map(BookingMapper::toDto)
                    .ifPresent(dto::setNextBooking);
        }

        return dto;
    }

    @Override
    public List<ItemOwnerDto> getByOwner(Long ownerId) {
        log.info("Get items with bookings by ownerId={}", ownerId);
        checkUserExists(ownerId);

        LocalDateTime now = LocalDateTime.now();
        List<Item> items = itemRepository.findAllByOwner_Id(ownerId);

        if (items.isEmpty()) {
            return List.of();
        }

        List<Long> itemIds = items.stream().map(Item::getId).toList();

        Map<Long, List<CommentDto>> commentsByItemId = loadCommentsForItems(itemIds);
        Map<Long, List<Booking>> approvedBookingsByItemId = loadApprovedBookingsForItems(itemIds);

        return items.stream()
                .map(item -> toOwnerDto(
                        item,
                        now,
                        commentsByItemId.getOrDefault(item.getId(), List.of()),
                        approvedBookingsByItemId.getOrDefault(item.getId(), List.of())
                ))
                .toList();
    }

    @Override
    public List<ItemDto> search(String text) {
        log.info("Search items by text='{}'", text);

        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<Item> items = itemRepository.search(text).stream()
                .sorted(Comparator.comparing(Item::getId))
                .toList();

        if (items.isEmpty()) {
            return List.of();
        }

        List<Long> itemIds = items.stream().map(Item::getId).toList();
        Map<Long, List<CommentDto>> commentsByItemId = loadCommentsForItems(itemIds);

        List<ItemDto> result = new java.util.ArrayList<>(items.size());

        for (Item item : items) {
            ItemDto dto = itemMapper.toDto(item);
            dto.setComments(commentsByItemId.getOrDefault(item.getId(), List.of()));
            result.add(dto);
        }

        return result;
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

    private void checkUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User not found");
        }
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

    private ItemOwnerDto toOwnerDto(Item item,
                                    LocalDateTime now,
                                    List<CommentDto> comments,
                                    List<Booking> approvedBookingsForItem) {
        ItemOwnerDto dto = new ItemOwnerDto();
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setDescription(item.getDescription());
        dto.setAvailable(item.isAvailable());
        dto.setComments(comments);

        selectLastBooking(approvedBookingsForItem, now)
                .map(BookingMapper::toDto)
                .ifPresent(dto::setLastBooking);

        selectNextBooking(approvedBookingsForItem, now)
                .map(BookingMapper::toDto)
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

    private Map<Long, List<Booking>> loadApprovedBookingsForItems(List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Map.of();
        }

        return bookingRepository.findByItem_IdInAndStatus(itemIds, BookingStatus.APPROVED, BOOKINGS_SORT).stream()
                .collect(Collectors.groupingBy(b -> b.getItem().getId()));
    }

    private List<Booking> loadApprovedBookingsForItem(Long itemId) {
        return bookingRepository.findAllByItem_IdAndStatus(itemId, BookingStatus.APPROVED, BOOKINGS_SORT);
    }

    private Optional<Booking> selectLastBooking(List<Booking> bookings, LocalDateTime now) {
        return bookings.stream()
                .filter(b -> b.getEnd() != null && b.getEnd().isBefore(now)) // строго < now (как чаще в тестах)
                .max(Comparator.comparing(Booking::getEnd));
    }

    private Optional<Booking> selectNextBooking(List<Booking> bookings, LocalDateTime now) {
        return bookings.stream()
                .filter(b -> b.getStart() != null && b.getStart().isAfter(now)) // строго > now
                .min(Comparator.comparing(Booking::getStart));
    }

}
