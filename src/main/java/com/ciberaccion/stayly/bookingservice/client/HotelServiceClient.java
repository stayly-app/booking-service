package com.ciberaccion.stayly.bookingservice.client;

import com.ciberaccion.stayly.bookingservice.dto.response.RoomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HotelServiceClient {

    private final RestClient restClient;

    @Value("${hotel-service.url}")
    private String hotelServiceUrl;

    public RoomResponse getRoomById(UUID hotelId, UUID roomId) {
        return restClient.get()
                .uri(hotelServiceUrl + "/api/v1/hotels/{hotelId}/rooms/{roomId}", hotelId, roomId)
                .retrieve()
                .body(RoomResponse.class);
    }
}