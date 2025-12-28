package ru.practicum.shareit.item;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemCreateDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemOwnerDto;
import ru.practicum.shareit.item.dto.ItemUpdateDto;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @PostMapping
    public ItemDto create(@RequestHeader("X-Sharer-User-Id") Long userId,
                          @Valid @RequestBody ItemCreateDto dto) {
        log.info("POST /items userId={}", userId);
        return itemService.create(userId, dto);
    }

    @PatchMapping("/{itemId}")
    public ItemDto update(@RequestHeader("X-Sharer-User-Id") Long userId,
                          @PathVariable Long itemId,
                          @RequestBody ItemUpdateDto dto) {
        log.info("PATCH /items/{} userId={}", itemId, userId);
        return itemService.update(userId, itemId, dto);
    }

    @GetMapping("/{itemId}")
    public ItemDto getById(@RequestHeader(value = "X-Sharer-User-Id", required = false) Long userId,
                           @PathVariable Long itemId) {
        log.info("GET /items/{} userId={}", itemId, userId);
        return itemService.getById(itemId);
    }

    @GetMapping
    public List<ItemOwnerDto> getByOwner(@RequestHeader("X-Sharer-User-Id") Long userId) {
        log.info("GET /items userId={}", userId);
        return itemService.getByOwner(userId);
    }

    @GetMapping("/search")
    public List<ItemDto> search(@RequestParam String text) {
        log.info("GET /items/search text='{}'", text);
        return itemService.search(text);
    }

    @PostMapping("/{itemId}/comment")
    public CommentDto addComment(@RequestHeader("X-Sharer-User-Id") Long userId,
                                 @PathVariable Long itemId,
                                 @Valid @RequestBody CommentCreateDto dto) {
        return itemService.addComment(userId, itemId, dto);
    }
}
