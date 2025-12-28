package ru.practicum.shareit.item;

import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemCreateDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemOwnerDto;
import ru.practicum.shareit.item.dto.ItemUpdateDto;

import java.util.List;

public interface ItemService {

    ItemDto create(Long ownerId, ItemCreateDto dto);

    ItemDto update(Long ownerId, Long itemId, ItemUpdateDto dto);

    ItemDto getById(Long itemId);

    ItemOwnerDto getByIdForOwner(Long ownerId, Long itemId);

    List<ItemOwnerDto> getByOwner(Long ownerId);

    List<ItemDto> search(String text);

    CommentDto addComment(Long authorId, Long itemId, CommentCreateDto dto);
}