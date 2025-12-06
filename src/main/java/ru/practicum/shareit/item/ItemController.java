package ru.practicum.shareit.item;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.item.dto.ItemDto;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @PostMapping
    public ItemDto create(@RequestHeader("X-Sharer-User-Id") Long userId,
                          @Valid @RequestBody ItemDto dto) {
        log.info("POST /items userId={}", userId);
        return itemService.create(userId, dto);
    }

    @PatchMapping("/{itemId}")
    public ItemDto update(@RequestHeader("X-Sharer-User-Id") Long userId,
                          @PathVariable Long itemId,
                          @RequestBody ItemDto dto) {
        log.info("PATCH /items/{} userId={}", itemId, userId);
        return itemService.update(userId, itemId, dto);
    }

    @GetMapping("/{itemId}")
    public ItemDto getById(@RequestHeader(value = "X-Sharer-User-Id", required = false) Long userId,
                           @PathVariable Long itemId) {
        log.info("GET /items/{} userId={}", itemId, userId);
        return itemService.getById(userId, itemId);
    }

    @GetMapping
    public List<ItemDto> getByOwner(@RequestHeader("X-Sharer-User-Id") Long userId) {
        log.info("GET /items userId={}", userId);
        return itemService.getByOwner(userId);
    }

    @GetMapping("/search")
    public List<ItemDto> search(@RequestParam String text) {
        log.info("GET /items/search text='{}'", text);
        return itemService.search(text);
    }
}
