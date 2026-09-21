package com.acc.gazua.domain.accommodation.entity;

import com.acc.gazua.domain.accommodation.exception.AccommodationException;
import com.acc.gazua.global.dto.ErrorCode;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record CheckDate (
        @NotNull(message = "체크인 날짜는 필수입니다.")
        @FutureOrPresent(message = "과거 날짜는 불가능합니다.")
        LocalDate checkIn,
        @NotNull(message = "체크아웃 날짜는 필수입니다.")
        LocalDate checkOut

){
    //숙박 일 수
    //파라미터가 누락되거나 체크아웃이 체크인보다 뒤가 아니면 예외
    public long stayDays() {
        if (checkIn == null || checkOut == null) {
            throw new AccommodationException(ErrorCode.INVALID_CHECK_DATE);
        }

        long stayDays = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (stayDays <= 0) {
            throw new AccommodationException(ErrorCode.INVALID_CHECK_DATE);
        }
        if (stayDays > 30) {
            throw new AccommodationException(ErrorCode.TOO_MANY_STAY_NIGHTS);
        }

        return stayDays;
    }
}
