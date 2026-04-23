package com.ciberaccion.stayly.bookingservice.controller;

import com.ciberaccion.stayly.bookingservice.dto.request.BookingRequest;
import com.ciberaccion.stayly.bookingservice.dto.response.BookingResponse;
import com.ciberaccion.stayly.bookingservice.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody BookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(bookingService.findById(id));
    }

    @GetMapping
    public ResponseEntity<List<BookingResponse>> findByUserId(@RequestParam UUID userId) {
        return ResponseEntity.ok(bookingService.findByUserId(userId));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(bookingService.cancel(id));
    }
}