package ru.practicum.shareit.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class BookingServiceIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    private User owner;
    private User booker;
    private Item item;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setName("owner");
        owner.setEmail("owner@mail.com");
        owner = userRepository.save(owner);

        booker = new User();
        booker.setName("booker");
        booker.setEmail("booker@mail.com");
        booker = userRepository.save(booker);

        item = new Item();
        item.setName("drill");
        item.setDescription("good drill");
        item.setAvailable(true);
        item.setOwner(owner);
        item = itemRepository.save(item);
    }

    @Test
    void create_shouldSaveBooking_whenValidAndNoOverlap() {
        BookingCreateDto dto = new BookingCreateDto();
        dto.setItemId(item.getId());
        dto.setStart(LocalDateTime.now().plusHours(1));
        dto.setEnd(LocalDateTime.now().plusHours(2));

        BookingDto created = bookingService.create(booker.getId(), dto);

        assertThat(created.getId()).isNotNull();

        Booking saved = bookingRepository.findById(created.getId()).orElseThrow();
        assertThat(saved.getItem().getId()).isEqualTo(item.getId());
        assertThat(saved.getBooker().getId()).isEqualTo(booker.getId());
        assertThat(saved.getStart()).isEqualTo(dto.getStart());
        assertThat(saved.getEnd()).isEqualTo(dto.getEnd());
        assertThat(saved.getStatus()).isEqualTo(BookingStatus.WAITING);
    }

    @Test
    void approve_shouldChangeStatusToApproved_forOwner_andRejectNonOwner() {
        BookingCreateDto dto = new BookingCreateDto();
        dto.setItemId(item.getId());
        dto.setStart(LocalDateTime.now().plusHours(1));
        dto.setEnd(LocalDateTime.now().plusHours(2));

        BookingDto created = bookingService.create(booker.getId(), dto);

        assertThatThrownBy(() -> bookingService.approve(booker.getId(), created.getId(), true))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only owner can approve booking");

        BookingDto approved = bookingService.approve(owner.getId(), created.getId(), true);

        assertThat(approved.getId()).isEqualTo(created.getId());
        assertThat(approved.getStatus()).isEqualTo(BookingStatus.APPROVED);

        Booking saved = bookingRepository.findById(created.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(BookingStatus.APPROVED);

        assertThatThrownBy(() -> bookingService.approve(owner.getId(), created.getId(), true))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Booking already processed");
    }
}
