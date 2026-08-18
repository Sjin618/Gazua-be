package com.acc.gazua.domain.reservation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReservationConfirmRequest {
    private String paymentId;
    private Long ReservationId;
}
