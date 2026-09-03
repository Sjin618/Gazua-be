package com.acc.gazua.domain.reservation.dto;


import com.acc.gazua.domain.accommodation.entity.CheckDate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ReservationInitRequest(
        @NotNull @Valid CheckDate checkDate,
        @NotNull Long roomId
) {
}
