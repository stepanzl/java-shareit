package ru.practicum.shareit.booking.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // -------- Booker views --------

    List<Booking> findByBooker_Id(Long bookerId, Sort sort);

    List<Booking> findByBooker_IdAndEndIsBefore(Long bookerId, LocalDateTime time, Sort sort);

    List<Booking> findByBooker_IdAndStartIsAfter(Long bookerId, LocalDateTime time, Sort sort);

    @Query("""
            select b from Booking b
            where b.booker.id = ?1
              and b.start <= ?2
              and b.end >= ?2
            """)
    List<Booking> findCurrentByBooker(Long bookerId, LocalDateTime now, Sort sort);

    List<Booking> findByBooker_IdAndStatus(Long bookerId, BookingStatus status, Sort sort);

    // -------- Owner views --------

    @Query("""
            select b from Booking b
            where b.item.owner.id = ?1
            """)
    List<Booking> findByOwner(Long ownerId, Sort sort);

    @Query("""
            select b from Booking b
            where b.item.owner.id = ?1
              and b.end < ?2
            """)
    List<Booking> findPastByOwner(Long ownerId, LocalDateTime time, Sort sort);

    @Query("""
            select b from Booking b
            where b.item.owner.id = ?1
              and b.start > ?2
            """)
    List<Booking> findFutureByOwner(Long ownerId, LocalDateTime time, Sort sort);

    @Query("""
            select b from Booking b
            where b.item.owner.id = ?1
              and b.start <= ?2
              and b.end >= ?2
            """)
    List<Booking> findCurrentByOwner(Long ownerId, LocalDateTime now, Sort sort);

    List<Booking> findByItem_Owner_IdAndStatus(
            Long ownerId,
            BookingStatus status,
            Sort sort
    );

    // -------- Item helpers --------

    List<Booking> findByItem_IdInAndStatus(List<Long> itemIds, BookingStatus status, Sort sort);

    List<Booking> findAllByItem_IdAndStatus(Long itemId, BookingStatus status, Sort sort);

    boolean existsByItem_IdAndBooker_IdAndStatusAndEndIsBefore(
            Long itemId,
            Long bookerId,
            BookingStatus status,
            LocalDateTime time
    );

    boolean existsByItem_IdAndStatusInAndStartLessThanAndEndGreaterThan(
            Long itemId,
            Collection<BookingStatus> statuses,
            LocalDateTime end,
            LocalDateTime start
    );
}
