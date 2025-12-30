package ru.practicum.shareit.booking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingState;
import ru.practicum.shareit.booking.mapper.BookingMapper;
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

@Slf4j
@Service
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    public BookingServiceImpl(BookingRepository bookingRepository,
                              ItemRepository itemRepository,
                              UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public BookingDto create(Long bookerId, BookingCreateDto dto) {
        User booker = getUser(bookerId);
        Item item = itemRepository.findById(dto.getItemId())
                .orElseThrow(() -> new NotFoundException("Item not found"));

        if (!item.isAvailable()) {
            throw new BadRequestException("Item is not available");
        }
        if (item.getOwner().getId().equals(bookerId)) {
            throw new NotFoundException("Owner cannot book own item");
        }
        if (!dto.getEnd().isAfter(dto.getStart())) {
            throw new BadRequestException("End must be after start");
        }

        Booking booking = BookingMapper.toEntity(dto, item, booker);

        return BookingMapper.toDto(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public BookingDto approve(Long ownerId, Long bookingId, boolean approved) {
        Booking booking = getBooking(bookingId);

        if (!booking.getItem().getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("Only owner can approve booking");
        }
        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new BadRequestException("Booking already processed");
        }

        booking.setStatus(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);
        return BookingMapper.toDto(bookingRepository.save(booking));
    }

    @Override
    public BookingDto getById(Long userId, Long bookingId) {
        Booking booking = getBooking(bookingId);

        if (!booking.getBooker().getId().equals(userId)
                && !booking.getItem().getOwner().getId().equals(userId)) {
            throw new NotFoundException("Booking not accessible");
        }
        return BookingMapper.toDto(booking);
    }

    @Override
    public List<BookingDto> getByBooker(Long bookerId, BookingState state) {
        getUser(bookerId);
        return map(selectByBooker(bookerId, state));
    }

    @Override
    public List<BookingDto> getByOwner(Long ownerId, BookingState state) {
        getUser(ownerId);
        return map(selectByOwner(ownerId, state));
    }

    // -------- helpers --------

    private List<Booking> selectByBooker(Long bookerId, BookingState state) {
        LocalDateTime now = LocalDateTime.now();
        Sort sort = Sort.by(Sort.Direction.DESC, "start");

        return switch (state) {
            case ALL -> bookingRepository.findByBooker_Id(bookerId, sort);
            case CURRENT -> bookingRepository.findCurrentByBooker(bookerId, now, sort);
            case PAST -> bookingRepository.findByBooker_IdAndEndIsBefore(bookerId, now, sort);
            case FUTURE -> bookingRepository.findByBooker_IdAndStartIsAfter(bookerId, now, sort);
            case WAITING -> bookingRepository.findByBooker_IdAndStatus(bookerId, BookingStatus.WAITING, sort);
            case REJECTED -> bookingRepository.findByBooker_IdAndStatus(bookerId, BookingStatus.REJECTED, sort);
        };
    }

    private List<Booking> selectByOwner(Long ownerId, BookingState state) {
        LocalDateTime now = LocalDateTime.now();
        Sort sort = Sort.by(Sort.Direction.DESC, "start");

        return switch (state) {
            case ALL -> bookingRepository.findByOwner(ownerId, sort);
            case CURRENT -> bookingRepository.findCurrentByOwner(ownerId, now, sort);
            case PAST -> bookingRepository.findPastByOwner(ownerId, now, sort);
            case FUTURE -> bookingRepository.findFutureByOwner(ownerId, now, sort);
            case WAITING -> bookingRepository.findByOwnerAndStatus(ownerId, BookingStatus.WAITING, sort);
            case REJECTED -> bookingRepository.findByOwnerAndStatus(ownerId, BookingStatus.REJECTED, sort);
        };
    }

    private List<BookingDto> map(List<Booking> bookings) {
        return bookings.stream().map(BookingMapper::toDto).toList();
    }

    private Booking getBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
