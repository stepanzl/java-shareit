package ru.practicum.shareit.booking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingState;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @Test
    void create_whenItemNotAvailable_thenThrowsBadRequest() {
        long bookerId = 1L;

        BookingCreateDto dto = new BookingCreateDto();
        dto.setItemId(10L);
        dto.setStart(LocalDateTime.now().plusHours(1));
        dto.setEnd(LocalDateTime.now().plusHours(2));

        User booker = new User();
        booker.setId(bookerId);

        Item item = new Item();
        item.setId(10L);
        item.setAvailable(false);

        when(userRepository.findById(bookerId)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> bookingService.create(bookerId, dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not available");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void create_whenOwnerBooksOwnItem_thenThrowsNotFound() {
        long bookerId = 1L;

        BookingCreateDto dto = new BookingCreateDto();
        dto.setItemId(10L);
        dto.setStart(LocalDateTime.now().plusHours(1));
        dto.setEnd(LocalDateTime.now().plusHours(2));

        User booker = new User();
        booker.setId(bookerId);

        User owner = new User();
        owner.setId(bookerId);

        Item item = new Item();
        item.setId(10L);
        item.setAvailable(true);
        item.setOwner(owner);

        when(userRepository.findById(bookerId)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> bookingService.create(bookerId, dto))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Owner cannot book own item");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void create_whenEndNotAfterStart_thenThrowsBadRequest() {
        long bookerId = 1L;

        BookingCreateDto dto = new BookingCreateDto();
        dto.setItemId(10L);
        LocalDateTime start = LocalDateTime.now().plusHours(2);
        dto.setStart(start);
        dto.setEnd(start); // not after

        User booker = new User();
        booker.setId(bookerId);

        User owner = new User();
        owner.setId(2L);

        Item item = new Item();
        item.setId(10L);
        item.setAvailable(true);
        item.setOwner(owner);

        when(userRepository.findById(bookerId)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> bookingService.create(bookerId, dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("End must be after start");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void create_whenOverlapsExisting_thenThrowsBadRequest() {
        long bookerId = 1L;

        BookingCreateDto dto = new BookingCreateDto();
        dto.setItemId(10L);
        dto.setStart(LocalDateTime.now().plusHours(1));
        dto.setEnd(LocalDateTime.now().plusHours(2));

        User booker = new User();
        booker.setId(bookerId);

        User owner = new User();
        owner.setId(2L);

        Item item = new Item();
        item.setId(10L);
        item.setAvailable(true);
        item.setOwner(owner);

        when(userRepository.findById(bookerId)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        when(bookingRepository.existsByItem_IdAndStatusInAndStartLessThanAndEndGreaterThan(
                eq(10L),
                eq(List.of(BookingStatus.WAITING, BookingStatus.APPROVED)),
                eq(dto.getEnd()),
                eq(dto.getStart())
        )).thenReturn(true);

        assertThatThrownBy(() -> bookingService.create(bookerId, dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("intersects");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void approve_whenNotOwner_thenThrowsForbidden() {
        long ownerId = 1L;
        long bookingId = 100L;

        User realOwner = new User();
        realOwner.setId(2L);

        Item item = new Item();
        item.setOwner(realOwner);

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setItem(item);
        booking.setStatus(BookingStatus.WAITING);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.approve(ownerId, bookingId, true))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only owner");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void approve_whenNotWaiting_thenThrowsBadRequest() {
        long ownerId = 1L;
        long bookingId = 100L;

        User owner = new User();
        owner.setId(ownerId);

        Item item = new Item();
        item.setOwner(owner);

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setItem(item);
        booking.setStatus(BookingStatus.APPROVED);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.approve(ownerId, bookingId, true))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already processed");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void approve_whenValid_thenUpdatesStatus() {
        long ownerId = 1L;
        long bookingId = 100L;

        User owner = new User();
        owner.setId(ownerId);

        Item item = new Item();
        item.setOwner(owner);

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setItem(item);
        booking.setStatus(BookingStatus.WAITING);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingDto result = bookingService.approve(ownerId, bookingId, true);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.APPROVED);

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(BookingStatus.APPROVED);
    }

    @Test
    void getById_whenNotBookerAndNotOwner_thenThrowsNotFound() {
        long requesterId = 1L;
        long bookingId = 100L;

        User booker = new User();
        booker.setId(2L);

        User owner = new User();
        owner.setId(3L);

        Item item = new Item();
        item.setOwner(owner);

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setBooker(booker);
        booking.setItem(item);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.getById(requesterId, bookingId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("not accessible");
    }

    @Test
    void getByBooker_whenOk_thenUsesRepositoryByStateAll() {
        long bookerId = 1L;
        when(userRepository.findById(bookerId)).thenReturn(Optional.of(new User()));

        Booking b = new Booking();
        b.setId(1L);

        when(bookingRepository.findByBooker_Id(eq(bookerId), any(Sort.class))).thenReturn(List.of(b));

        List<BookingDto> result = bookingService.getByBooker(bookerId, BookingState.ALL);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);

        verify(bookingRepository).findByBooker_Id(eq(bookerId), any(Sort.class));
    }

    @Test
    void getByOwner_whenOk_thenUsesRepositoryByStateWaiting() {
        long ownerId = 1L;
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(new User()));

        Booking b = new Booking();
        b.setId(1L);

        when(bookingRepository.findByItem_Owner_IdAndStatus(eq(ownerId), eq(BookingStatus.WAITING), any(Sort.class)))
                .thenReturn(List.of(b));

        List<BookingDto> result = bookingService.getByOwner(ownerId, BookingState.WAITING);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);

        verify(bookingRepository).findByItem_Owner_IdAndStatus(eq(ownerId), eq(BookingStatus.WAITING), any(Sort.class));
    }

    @Test
    void create_whenValid_thenSavesBooking() {
        long bookerId = 1L;

        BookingCreateDto dto = new BookingCreateDto();
        dto.setItemId(10L);
        dto.setStart(LocalDateTime.now().plusHours(1));
        dto.setEnd(LocalDateTime.now().plusHours(2));

        User booker = new User();
        booker.setId(bookerId);

        User owner = new User();
        owner.setId(2L);

        Item item = new Item();
        item.setId(10L);
        item.setAvailable(true);
        item.setOwner(owner);

        when(userRepository.findById(bookerId)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        when(bookingRepository.existsByItem_IdAndStatusInAndStartLessThanAndEndGreaterThan(
                eq(10L),
                eq(List.of(BookingStatus.WAITING, BookingStatus.APPROVED)),
                eq(dto.getEnd()),
                eq(dto.getStart())
        )).thenReturn(false);

        Booking saved = new Booking();
        saved.setId(123L);
        saved.setItem(item);
        saved.setBooker(booker);
        saved.setStart(dto.getStart());
        saved.setEnd(dto.getEnd());
        saved.setStatus(BookingStatus.WAITING);

        when(bookingRepository.save(any(Booking.class))).thenReturn(saved);

        BookingDto result = bookingService.create(bookerId, dto);

        assertThat(result.getId()).isEqualTo(123L);
        assertThat(result.getStatus()).isEqualTo(BookingStatus.WAITING);

        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void create_whenBookerNotFound_thenThrowsNotFound() {
        long bookerId = 1L;

        BookingCreateDto dto = new BookingCreateDto();
        dto.setItemId(10L);
        dto.setStart(LocalDateTime.now().plusHours(1));
        dto.setEnd(LocalDateTime.now().plusHours(2));

        when(userRepository.findById(bookerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.create(bookerId, dto))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findById(bookerId);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(itemRepository, bookingRepository);
    }

    @Test
    void create_whenItemNotFound_thenThrowsNotFound() {
        long bookerId = 1L;

        BookingCreateDto dto = new BookingCreateDto();
        dto.setItemId(10L);
        dto.setStart(LocalDateTime.now().plusHours(1));
        dto.setEnd(LocalDateTime.now().plusHours(2));

        when(userRepository.findById(bookerId)).thenReturn(Optional.of(new User()));
        when(itemRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.create(bookerId, dto))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Item not found");

        verify(userRepository).findById(bookerId);
        verify(itemRepository).findById(10L);
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void approve_whenBookingNotFound_thenThrowsNotFound() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.approve(1L, 999L, true))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Booking not found");

        verify(bookingRepository).findById(999L);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void approve_whenRejected_thenSetsRejected() {
        long ownerId = 1L;
        long bookingId = 100L;

        User owner = new User();
        owner.setId(ownerId);

        Item item = new Item();
        item.setOwner(owner);

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setItem(item);
        booking.setStatus(BookingStatus.WAITING);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingDto result = bookingService.approve(ownerId, bookingId, false);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.REJECTED);

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(BookingStatus.REJECTED);
    }

    @Test
    void getById_whenRequesterIsBooker_thenReturnsDto() {
        long bookerId = 2L;
        long bookingId = 100L;

        User booker = new User();
        booker.setId(bookerId);

        User owner = new User();
        owner.setId(3L);

        Item item = new Item();
        item.setOwner(owner);

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setBooker(booker);
        booking.setItem(item);
        booking.setStatus(BookingStatus.WAITING);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        BookingDto result = bookingService.getById(bookerId, bookingId);

        assertThat(result.getId()).isEqualTo(bookingId);
        assertThat(result.getBooker().getId()).isEqualTo(bookerId);
        assertThat(result.getItem().getId()).isEqualTo(item.getId()); // может быть null если item.id не задан
        assertThat(result.getStatus()).isEqualTo(BookingStatus.WAITING);

        verify(bookingRepository).findById(bookingId);
    }

    @Test
    void getById_whenRequesterIsOwner_thenReturnsDto() {
        long ownerId = 3L;
        long bookingId = 100L;

        User booker = new User();
        booker.setId(2L);

        User owner = new User();
        owner.setId(ownerId);

        Item item = new Item();
        item.setOwner(owner);

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setBooker(booker);
        booking.setItem(item);
        booking.setStatus(BookingStatus.WAITING);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        BookingDto result = bookingService.getById(ownerId, bookingId);

        assertThat(result.getId()).isEqualTo(bookingId);
        assertThat(result.getStatus()).isEqualTo(BookingStatus.WAITING);

        verify(bookingRepository).findById(bookingId);
    }

    @Test
    void getByBooker_whenStateCurrent_thenCallsCurrentRepoMethod() {
        long bookerId = 1L;
        when(userRepository.findById(bookerId)).thenReturn(Optional.of(new User()));

        Booking b = new Booking();
        b.setId(1L);

        when(bookingRepository.findCurrentByBooker(eq(bookerId), any(LocalDateTime.class), any(Sort.class)))
                .thenReturn(List.of(b));

        List<BookingDto> result = bookingService.getByBooker(bookerId, BookingState.CURRENT);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);

        verify(bookingRepository).findCurrentByBooker(eq(bookerId), any(LocalDateTime.class), any(Sort.class));
        verify(bookingRepository, never()).findByBooker_Id(anyLong(), any());
    }

    @Test
    void getByOwner_whenStateRejected_thenCallsRejectedRepoMethod() {
        long ownerId = 1L;
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(new User()));

        Booking b = new Booking();
        b.setId(1L);

        when(bookingRepository.findByItem_Owner_IdAndStatus(eq(ownerId), eq(BookingStatus.REJECTED), any(Sort.class)))
                .thenReturn(List.of(b));

        List<BookingDto> result = bookingService.getByOwner(ownerId, BookingState.REJECTED);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);

        verify(bookingRepository).findByItem_Owner_IdAndStatus(eq(ownerId), eq(BookingStatus.REJECTED), any(Sort.class));
        verify(bookingRepository, never()).findByOwner(anyLong(), any());
    }

}
