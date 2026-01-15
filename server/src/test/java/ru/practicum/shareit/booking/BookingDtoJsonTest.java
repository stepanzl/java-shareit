package ru.practicum.shareit.booking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingItemDto;
import ru.practicum.shareit.booking.dto.BookingUserDto;
import ru.practicum.shareit.booking.model.BookingStatus;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class BookingDtoJsonTest {

    @Autowired
    private JacksonTester<BookingDto> json;

    @Test
    void serialize_bookingDto_writesIsoDatesWithoutMillisWhenNanosZero() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 1, 14, 8, 42, 11);
        LocalDateTime end = LocalDateTime.of(2026, 1, 15, 8, 42, 11);

        BookingDto dto = BookingDto.builder()
                .id(10L)
                .start(start)
                .end(end)
                .status(BookingStatus.WAITING)
                .item(BookingItemDto.builder().id(1L).name("Drill").build())
                .booker(BookingUserDto.builder().id(2L).build())
                .build();

        JsonContent<BookingDto> result = json.write(dto);

        assertThat(result).extractingJsonPathNumberValue("$.id").isEqualTo(10);
        assertThat(result).extractingJsonPathStringValue("$.start").isEqualTo("2026-01-14T08:42:11");
        assertThat(result).extractingJsonPathStringValue("$.end").isEqualTo("2026-01-15T08:42:11");
        assertThat(result).extractingJsonPathStringValue("$.status").isEqualTo("WAITING");

        assertThat(result).extractingJsonPathNumberValue("$.item.id").isEqualTo(1);
        assertThat(result).extractingJsonPathStringValue("$.item.name").isEqualTo("Drill");

        assertThat(result).extractingJsonPathNumberValue("$.booker.id").isEqualTo(2);
    }

    @Test
    void deserialize_bookingDto_readsIsoDatesWithoutMillis() throws Exception {
        String content = """
                {
                  "id": 10,
                  "start": "2026-01-14T08:42:11",
                  "end": "2026-01-15T08:42:11",
                  "status": "APPROVED",
                  "item": { "id": 1, "name": "Drill" },
                  "booker": { "id": 2 }
                }
                """;

        BookingDto dto = json.parseObject(content);

        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getStart()).isEqualTo(LocalDateTime.of(2026, 1, 14, 8, 42, 11));
        assertThat(dto.getEnd()).isEqualTo(LocalDateTime.of(2026, 1, 15, 8, 42, 11));
        assertThat(dto.getStatus()).isEqualTo(BookingStatus.APPROVED);

        assertThat(dto.getItem()).isNotNull();
        assertThat(dto.getItem().getId()).isEqualTo(1L);
        assertThat(dto.getItem().getName()).isEqualTo("Drill");

        assertThat(dto.getBooker()).isNotNull();
        assertThat(dto.getBooker().getId()).isEqualTo(2L);
    }
}
