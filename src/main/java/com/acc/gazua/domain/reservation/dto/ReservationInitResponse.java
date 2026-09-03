package com.acc.gazua.domain.reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

public record ReservationInitResponse (
        String paymentId,
        Long reservationId,
        Integer price,
        String buyerName,
        String buyerTel,
        String buyerEmail
){

}
