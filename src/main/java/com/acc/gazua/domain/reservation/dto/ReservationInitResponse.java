package com.acc.gazua.domain.reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReservationInitResponse {
    private String paymentId;
    private Long reservationId;
    private Integer price;
    private String buyerName;
    private String buyerTel;
    private String buyerEmail;
}
