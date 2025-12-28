package ru.practicum.shareit.booking.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;

import java.time.LocalDateTime;
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

    @Query("""
           select b from Booking b
           where b.item.owner.id = ?1
             and b.status = ?2
           """)
    List<Booking> findByOwnerAndStatus(Long ownerId, BookingStatus status, Sort sort);

    // -------- Item helpers --------

    @Query("""
           select b from Booking b
           where b.item.id = ?1
             and b.start < ?2
             and b.status = 'APPROVED'
           order by b.start desc
           """)
    List<Booking> findLastBooking(Long itemId, LocalDateTime now);

    @Query("""
           select b from Booking b
           where b.item.id = ?1
             and b.start > ?2
             and b.status = 'APPROVED'
           order by b.start asc
           """)
    List<Booking> findNextBooking(Long itemId, LocalDateTime now);
}
