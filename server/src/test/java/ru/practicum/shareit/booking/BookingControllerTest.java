package ru.practicum.shareit.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.dto.*;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.exception.ErrorHandler;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BookingController.class)
@Import(ErrorHandler.class)
class BookingControllerTest {

    private static final String USER_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    @Test
    void create_whenValidRequest_thenOkAndReturnsBody() throws Exception {
        BookingCreateDto in = new BookingCreateDto();
        in.setItemId(10L);
        in.setStart(LocalDateTime.of(2026, 1, 20, 10, 0));
        in.setEnd(LocalDateTime.of(2026, 1, 21, 10, 0));

        BookingDto out = BookingDto.builder()
                .id(1L)
                .start(in.getStart())
                .end(in.getEnd())
                .status(BookingStatus.WAITING)
                .item(BookingItemDto.builder().id(10L).name("Drill").build())
                .booker(BookingUserDto.builder().id(5L).build())
                .build();

        when(bookingService.create(ArgumentMatchers.eq(5L), ArgumentMatchers.any(BookingCreateDto.class)))
                .thenReturn(out);

        mockMvc.perform(post("/bookings")
                        .header(USER_HEADER, 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(in)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.start").exists())
                .andExpect(jsonPath("$.end").exists())
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.item.id").value(10L))
                .andExpect(jsonPath("$.item.name").value("Drill"))
                .andExpect(jsonPath("$.booker.id").value(5L));
    }

    @Test
    void approve_whenOk_thenOkAndReturnsBody() throws Exception {
        BookingDto out = BookingDto.builder()
                .id(99L)
                .start(LocalDateTime.of(2026, 1, 20, 10, 0))
                .end(LocalDateTime.of(2026, 1, 21, 10, 0))
                .status(BookingStatus.APPROVED)
                .item(BookingItemDto.builder().id(10L).name("Drill").build())
                .booker(BookingUserDto.builder().id(5L).build())
                .build();

        when(bookingService.approve(ArgumentMatchers.eq(1L), ArgumentMatchers.eq(99L), ArgumentMatchers.eq(true)))
                .thenReturn(out);

        mockMvc.perform(patch("/bookings/99")
                        .header(USER_HEADER, 1L)
                        .param("approved", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(99L))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void getById_whenExists_thenOkAndReturnsBody() throws Exception {
        BookingDto out = BookingDto.builder()
                .id(7L)
                .start(LocalDateTime.of(2026, 1, 20, 10, 0))
                .end(LocalDateTime.of(2026, 1, 21, 10, 0))
                .status(BookingStatus.WAITING)
                .item(BookingItemDto.builder().id(10L).name("Drill").build())
                .booker(BookingUserDto.builder().id(5L).build())
                .build();

        when(bookingService.getById(ArgumentMatchers.eq(5L), ArgumentMatchers.eq(7L)))
                .thenReturn(out);

        mockMvc.perform(get("/bookings/7")
                        .header(USER_HEADER, 5L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(7L))
                .andExpect(jsonPath("$.item.id").value(10L))
                .andExpect(jsonPath("$.booker.id").value(5L));
    }

    @Test
    void getById_whenNotFound_then404AndErrorBody() throws Exception {
        when(bookingService.getById(ArgumentMatchers.eq(5L), ArgumentMatchers.eq(404L)))
                .thenThrow(new NotFoundException("Booking not found"));

        mockMvc.perform(get("/bookings/404")
                        .header(USER_HEADER, 5L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Booking not found"));
    }

    @Test
    void getByBooker_whenHasBookings_thenOkAndReturnsList() throws Exception {
        BookingDto b1 = BookingDto.builder()
                .id(1L)
                .start(LocalDateTime.of(2026, 1, 20, 10, 0))
                .end(LocalDateTime.of(2026, 1, 21, 10, 0))
                .status(BookingStatus.WAITING)
                .item(BookingItemDto.builder().id(10L).name("A").build())
                .booker(BookingUserDto.builder().id(5L).build())
                .build();

        BookingDto b2 = BookingDto.builder()
                .id(2L)
                .start(LocalDateTime.of(2026, 1, 22, 10, 0))
                .end(LocalDateTime.of(2026, 1, 23, 10, 0))
                .status(BookingStatus.APPROVED)
                .item(BookingItemDto.builder().id(11L).name("B").build())
                .booker(BookingUserDto.builder().id(5L).build())
                .build();

        when(bookingService.getByBooker(ArgumentMatchers.eq(5L), ArgumentMatchers.eq(BookingState.ALL)))
                .thenReturn(List.of(b1, b2));

        mockMvc.perform(get("/bookings")
                        .header(USER_HEADER, 5L)
                        .param("state", "ALL")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L));
    }

    @Test
    void getByOwner_whenHasBookings_thenOkAndReturnsList() throws Exception {
        BookingDto b1 = BookingDto.builder()
                .id(3L)
                .start(LocalDateTime.of(2026, 1, 20, 10, 0))
                .end(LocalDateTime.of(2026, 1, 21, 10, 0))
                .status(BookingStatus.WAITING)
                .item(BookingItemDto.builder().id(10L).name("A").build())
                .booker(BookingUserDto.builder().id(6L).build())
                .build();

        when(bookingService.getByOwner(ArgumentMatchers.eq(1L), ArgumentMatchers.eq(BookingState.ALL)))
                .thenReturn(List.of(b1));

        mockMvc.perform(get("/bookings/owner")
                        .header(USER_HEADER, 1L)
                        .param("state", "ALL")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(3L));
    }

    @Test
    void approve_whenForbidden_then403AndErrorBody() throws Exception {
        when(bookingService.approve(ArgumentMatchers.eq(1L), ArgumentMatchers.eq(99L), ArgumentMatchers.eq(true)))
                .thenThrow(new ForbiddenException("Only owner can approve booking"));

        mockMvc.perform(patch("/bookings/99")
                        .header(USER_HEADER, 1L)
                        .param("approved", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Only owner can approve booking"));
    }
}
