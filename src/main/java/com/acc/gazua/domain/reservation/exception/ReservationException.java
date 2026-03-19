package com.acc.gazua.domain.reservation.exception;

import com.acc.gazua.global.dto.ErrorCode;

public class ReservationException extends RuntimeException{
    private ErrorCode errorCode;
    public ReservationException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
