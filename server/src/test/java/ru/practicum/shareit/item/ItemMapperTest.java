package ru.practicum.shareit.item;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.practicum.shareit.item.dto.ItemCreateDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemUpdateDto;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.user.model.User;

import static org.assertj.core.api.Assertions.assertThat;

class ItemMapperTest {

    private final ItemMapper mapper = Mappers.getMapper(ItemMapper.class);

    @Test
    void toDto_mapsOwnerIdAndRequestId() {
        User owner = new User();
        owner.setId(7L);

        ItemRequest req = new ItemRequest();
        req.setId(99L);

        Item item = new Item();
        item.setId(1L);
        item.setName("Drill");
        item.setDescription("Power");
        item.setAvailable(true);
        item.setOwner(owner);
        item.setRequest(req);

        ItemDto dto = mapper.toDto(item);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Drill");
        assertThat(dto.getDescription()).isEqualTo("Power");
        assertThat(dto.getAvailable()).isTrue();
        assertThat(dto.getOwnerId()).isEqualTo(7L);
        assertThat(dto.getRequestId()).isEqualTo(99L);
    }

    @Test
    void toModel_ignoresIdOwnerAndRequest() {
        ItemCreateDto dto = new ItemCreateDto();
        dto.setName("Bike");
        dto.setDescription("MTB");
        dto.setAvailable(true);
        dto.setRequestId(123L);

        Item item = mapper.toModel(dto);

        assertThat(item.getId()).isNull();
        assertThat(item.getOwner()).isNull();
        assertThat(item.getRequest()).isNull();
        assertThat(item.getName()).isEqualTo("Bike");
        assertThat(item.getDescription()).isEqualTo("MTB");
        assertThat(item.isAvailable()).isTrue();
    }

    @Test
    void updateItemFromDto_ignoresNulls() {
        Item existing = new Item();
        existing.setId(1L);
        existing.setName("Old");
        existing.setDescription("OldDesc");
        existing.setAvailable(true);

        ItemUpdateDto patch = new ItemUpdateDto();
        patch.setName("New");
        patch.setDescription(null);
        patch.setAvailable(null);

        mapper.updateItemFromDto(patch, existing);

        assertThat(existing.getName()).isEqualTo("New");
        assertThat(existing.getDescription()).isEqualTo("OldDesc");
        assertThat(existing.isAvailable()).isTrue();
    }
}
