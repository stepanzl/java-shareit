package ru.practicum.shareit.request;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import ru.practicum.shareit.request.dto.ItemAnswerDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class ItemRequestDtoJsonTest {

    @Autowired
    private JacksonTester<ItemRequestDto> json;

    @Test
    void serialize_itemRequestDto_writesCreatedAsIsoString_andItemsAsArray() throws Exception {
        LocalDateTime created = LocalDateTime.of(2026, 1, 14, 8, 42, 11);

        ItemRequestDto dto = new ItemRequestDto();
        dto.setId(100L);
        dto.setDescription("Need a drill");
        dto.setCreated(created);
        dto.setItems(new ArrayList<>());

        JsonContent<ItemRequestDto> result = json.write(dto);

        assertThat(result).extractingJsonPathNumberValue("$.id").isEqualTo(100);
        assertThat(result).extractingJsonPathStringValue("$.description").isEqualTo("Need a drill");
        assertThat(result).extractingJsonPathStringValue("$.created").isEqualTo("2026-01-14T08:42:11");
        assertThat(result).extractingJsonPathArrayValue("$.items").isEmpty();
    }

    @Test
    void deserialize_itemRequestDto_readsCreatedFromIsoString_andItemsList() throws Exception {
        String content = """
                {
                  "id": 100,
                  "description": "Need a drill",
                  "created": "2026-01-14T08:42:11",
                  "items": [
                    { "itemId": 1, "name": "Drill", "ownerId": 2 }
                  ]
                }
                """;

        ItemRequestDto dto = json.parseObject(content);

        assertThat(dto.getId()).isEqualTo(100L);
        assertThat(dto.getDescription()).isEqualTo("Need a drill");
        assertThat(dto.getCreated()).isEqualTo(LocalDateTime.of(2026, 1, 14, 8, 42, 11));

        List<ItemAnswerDto> items = dto.getItems();
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getItemId()).isEqualTo(1L);
        assertThat(items.get(0).getName()).isEqualTo("Drill");
        assertThat(items.get(0).getOwnerId()).isEqualTo(2L);
    }
}
