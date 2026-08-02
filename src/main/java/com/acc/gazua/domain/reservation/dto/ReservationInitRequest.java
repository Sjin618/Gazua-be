package com.acc.gazua.domain.reservation.dto;


import com.acc.gazua.domain.accommodation.entity.CheckDate;

public record ReservationInitRequest(
        CheckDate checkDate,
        Long roomId
) {
}
