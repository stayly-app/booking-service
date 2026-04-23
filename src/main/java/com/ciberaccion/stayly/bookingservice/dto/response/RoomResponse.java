package com.ciberaccion.stayly.bookingservice.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomResponse {

    private UUID id;
    private UUID hotelId;
    private String roomNumber;
    private String type;
    private Integer capacity;
    private BigDecimal pricePerNight;
    private String status;
}