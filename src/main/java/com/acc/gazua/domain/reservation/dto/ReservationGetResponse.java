package com.acc.gazua.domain.reservation.dto;



import java.time.LocalDateTime;

public record ReservationGetResponse(
        Long reservationId,
        Long accommodationId,
        String thumbnailUrl,
        String accommodationName,
        String roomName,
        LocalDateTime checkInDateTime,
        LocalDateTime checkOutDateTime
) {
}
