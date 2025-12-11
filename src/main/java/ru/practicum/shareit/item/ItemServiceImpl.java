package ru.practicum.shareit.item;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemCreateDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemUpdateDto;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ItemServiceImpl implements ItemService {

    private final ItemMapper itemMapper;
    private final UserService userService;
    private final ItemRepository itemRepository;

    public ItemServiceImpl(ItemMapper itemMapper,
                           UserService userService,
                           ItemRepository itemRepository) {
        this.itemMapper = itemMapper;
        this.userService = userService;
        this.itemRepository = itemRepository;
    }

    @Override
    public ItemDto create(Long ownerId, ItemCreateDto dto) {
        log.info("Create item by ownerId={}", ownerId);

        UserDto ownerDto = userService.getById(ownerId);

        Item item = itemMapper.toModel(dto);

        User owner = new User();
        owner.setId(ownerDto.getId());
        item.setOwner(owner);

        Item saved = itemRepository.save(item);
        return itemMapper.toDto(saved);
    }

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
        return itemRepository.findByOwnerId(ownerId).stream()
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

        String query = text.toLowerCase();

        return itemRepository.findAll().stream()
                .filter(Item::isAvailable)
                .filter(item ->
                        (item.getName() != null && item.getName().toLowerCase().contains(query))
                                || (item.getDescription() != null
                                && item.getDescription().toLowerCase().contains(query)))
                .sorted(Comparator.comparing(Item::getId))
                .map(itemMapper::toDto)
                .collect(Collectors.toList());
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
