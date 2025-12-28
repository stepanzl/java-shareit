package ru.practicum.shareit.item.dto;

import lombok.Data;
import ru.practicum.shareit.booking.dto.BookingDto;

@Data
public class ItemOwnerDto {

    private Long id;
    private String name;
    private String description;
    private boolean available;

    private BookingDto lastBooking;
    private BookingDto nextBooking;
}
