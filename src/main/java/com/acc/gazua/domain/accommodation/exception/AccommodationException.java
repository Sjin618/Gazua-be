package com.acc.gazua.domain.accommodation.exception;

import com.acc.gazua.global.dto.ErrorCode;

public class AccommodationException extends RuntimeException {
    private ErrorCode errorCode;

    public AccommodationException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
