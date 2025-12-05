package ru.practicum.shareit.item;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ItemServiceImpl implements ItemService {

    private final ItemMapper itemMapper;
    private final UserService userService;

    private final Map<Long, Item> items = new HashMap<>();
    private long nextId = 1L;

    public ItemServiceImpl(ItemMapper itemMapper, UserService userService) {
        this.itemMapper = itemMapper;
        this.userService = userService;
    }

    @Override
    public ItemDto create(Long ownerId, ItemDto dto) {
        UserDto ownerDto = userService.getById(ownerId);

        Item item = itemMapper.toModel(dto);
        item.setId(nextId++);

        User owner = new User();
        owner.setId(ownerDto.getId());
        item.setOwner(owner);

        items.put(item.getId(), item);

        return itemMapper.toDto(item);
    }

    @Override
    public ItemDto update(Long ownerId, Long itemId, ItemDto dto) {
        Item item = getItemOrThrow(itemId);

        if (item.getOwner() == null || !Objects.equals(item.getOwner().getId(), ownerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Item not found for this owner");
        }

        if (dto.getName() != null && !dto.getName().isBlank()) {
            item.setName(dto.getName());
        }
        if (dto.getDescription() != null && !dto.getDescription().isBlank()) {
            item.setDescription(dto.getDescription());
        }
        if (dto.getAvailable() != null) {
            item.setAvailable(dto.getAvailable());
        }

        return itemMapper.toDto(item);
    }

    @Override
    public ItemDto getById(Long userId, Long itemId) {
        Item item = getItemOrThrow(itemId);
        // userId сейчас не используем, но параметр оставляем — так требуют тесты
        return itemMapper.toDto(item);
    }

    @Override
    public List<ItemDto> getByOwner(Long ownerId) {
        return items.values().stream()
                .filter(item -> item.getOwner() != null
                        && Objects.equals(item.getOwner().getId(), ownerId))
                .sorted(Comparator.comparing(Item::getId))
                .map(itemMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ItemDto> search(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String query = text.toLowerCase();

        return items.values().stream()
                .filter(Item::isAvailable)
                .filter(item ->
                        (item.getName() != null
                                && item.getName().toLowerCase().contains(query))
                                || (item.getDescription() != null
                                && item.getDescription().toLowerCase().contains(query))
                )
                .sorted(Comparator.comparing(Item::getId))
                .map(itemMapper::toDto)
                .collect(Collectors.toList());
    }

    private Item getItemOrThrow(Long itemId) {
        Item item = items.get(itemId);
        if (item == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found");
        }
        return item;
    }
}
