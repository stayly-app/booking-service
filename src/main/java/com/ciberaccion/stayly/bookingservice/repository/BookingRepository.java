package com.ciberaccion.stayly.bookingservice.repository;

import com.ciberaccion.stayly.bookingservice.model.Booking;
import com.ciberaccion.stayly.bookingservice.model.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByUserId(UUID userId);

    List<Booking> findByRoomIdAndStatus(UUID roomId, BookingStatus status);

    // Pessimistic Locking — bloquea el row para evitar reservas simultáneas
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.roomId = :roomId " +
           "AND b.status = 'CONFIRMED' " +
           "AND b.checkIn < :checkOut " +
           "AND b.checkOut > :checkIn")
    List<Booking> findConflictingBookings(UUID roomId, LocalDate checkIn, LocalDate checkOut);
}