package com.acc.gazua.domain.reservation.dto;


import com.acc.gazua.domain.accommodation.entity.CheckDate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ReservationInitRequest(
        @NotNull(message = "체크인, 체크아웃 날짜는 필수입니다.") CheckDate checkDate,
        @NotNull(message = "방 ID는 필수입니다.") Long roomId
) {
}
