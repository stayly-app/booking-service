package com.ciberaccion.stayly.bookingservice.service;

import com.ciberaccion.stayly.bookingservice.dto.request.BookingRequest;
import com.ciberaccion.stayly.bookingservice.dto.response.BookingResponse;

import java.util.List;
import java.util.UUID;

public interface BookingService {

    BookingResponse create(BookingRequest request);

    BookingResponse findById(UUID id);

    List<BookingResponse> findByUserId(UUID userId);

    BookingResponse cancel(UUID id);
}