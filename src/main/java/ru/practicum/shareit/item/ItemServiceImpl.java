package ru.practicum.shareit.item;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemCreateDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemUpdateDto;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {

    private final ItemMapper itemMapper;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    public ItemServiceImpl(ItemMapper itemMapper,
                           UserRepository userRepository,
                           ItemRepository itemRepository) {
        this.itemMapper = itemMapper;
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
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
        return itemMapper.toDto(saved);
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
        return itemMapper.toDto(saved);
    }

    @Override
    public ItemDto getById(Long itemId) {
        log.info("Get item id={}", itemId);
        Item item = getItemOrThrow(itemId);
        return itemMapper.toDto(item);
    }

    @Override
    public List<ItemDto> getByOwner(Long ownerId) {
        log.info("Get items by ownerId={}", ownerId);
        getUserOrThrow(ownerId);

        return itemRepository.findAllByOwner_Id(ownerId).stream()
                .sorted(Comparator.comparing(Item::getId))
                .map(itemMapper::toDto)
                .collect(Collectors.toList());
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
                .collect(Collectors.toList());
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
}
