package com.acc.gazua.domain.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReservationConfirmRequest {
    @NotBlank private String paymentId;
}
