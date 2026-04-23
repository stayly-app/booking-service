package com.ciberaccion.stayly.bookingservice.service.impl;

import com.ciberaccion.stayly.bookingservice.client.HotelServiceClient;
import com.ciberaccion.stayly.bookingservice.dto.request.BookingRequest;
import com.ciberaccion.stayly.bookingservice.dto.response.BookingResponse;
import com.ciberaccion.stayly.bookingservice.dto.response.RoomResponse;
import com.ciberaccion.stayly.bookingservice.exception.ResourceNotFoundException;
import com.ciberaccion.stayly.bookingservice.model.Booking;
import com.ciberaccion.stayly.bookingservice.model.OutboxEvent;
import com.ciberaccion.stayly.bookingservice.model.enums.BookingStatus;
import com.ciberaccion.stayly.bookingservice.model.enums.OutboxEventStatus;
import com.ciberaccion.stayly.bookingservice.repository.BookingRepository;
import com.ciberaccion.stayly.bookingservice.repository.OutboxEventRepository;
import com.ciberaccion.stayly.bookingservice.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final HotelServiceClient hotelServiceClient;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public BookingResponse create(BookingRequest request) {
        // 1. Verificar disponibilidad — con Pessimistic Locking
        List<Booking> conflicts = bookingRepository.findConflictingBookings(
                request.getRoomId(), request.getCheckIn(), request.getCheckOut());

        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException("Room is not available for the selected dates");
        }

        // 2. Obtener precio de la habitación desde hotel-service
        RoomResponse room = hotelServiceClient.getRoomById(request.getHotelId(), request.getRoomId());

        // 3. Calcular total
        long nights = ChronoUnit.DAYS.between(request.getCheckIn(), request.getCheckOut());
        BigDecimal totalAmount = room.getPricePerNight().multiply(BigDecimal.valueOf(nights));

        // 4. Crear booking
        Booking booking = Booking.builder()
                .userId(request.getUserId())
                .hotelId(request.getHotelId())
                .roomId(request.getRoomId())
                .checkIn(request.getCheckIn())
                .checkOut(request.getCheckOut())
                .status(BookingStatus.CONFIRMED)
                .totalAmount(totalAmount)
                .build();

        Booking saved = bookingRepository.save(booking);

        // 5. Crear OutboxEvent en la misma transacción
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "eventType", "BookingConfirmed",
                    "eventId", UUID.randomUUID().toString(),
                    "occurredAt", LocalDateTime.now().toString(),
                    "payload", Map.of(
                            "bookingId", saved.getId().toString(),
                            "roomId", saved.getRoomId().toString(),
                            "hotelId", saved.getHotelId().toString(),
                            "userId", saved.getUserId().toString(),
                            "checkIn", saved.getCheckIn().toString(),
                            "checkOut", saved.getCheckOut().toString(),
                            "totalAmount", saved.getTotalAmount()
                    )
            ));

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventType("BookingConfirmed")
                    .payload(payload)
                    .status(OutboxEventStatus.PENDING)
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.info("OutboxEvent created for bookingId: {}", saved.getId());

        } catch (Exception e) {
            log.error("Failed to create OutboxEvent for bookingId: {}", saved.getId(), e);
            throw new RuntimeException("Failed to create booking event", e);
        }

        bookingRepository.flush();
        return toResponse(bookingRepository.findById(saved.getId()).orElseThrow());
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse findById(UUID id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        return toResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> findByUserId(UUID userId) {
        return bookingRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public BookingResponse cancel(UUID id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("Booking is already cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        return toResponse(bookingRepository.save(booking));
    }

    // ---- Mapper ----

    private BookingResponse toResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .userId(booking.getUserId())
                .hotelId(booking.getHotelId())
                .roomId(booking.getRoomId())
                .checkIn(booking.getCheckIn())
                .checkOut(booking.getCheckOut())
                .status(booking.getStatus())
                .totalAmount(booking.getTotalAmount())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }
}