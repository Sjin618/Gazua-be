package com.acc.gazua.domain.accommodation.exception;

import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.exception.BusinessException;

public class AccommodationException extends BusinessException {
    public AccommodationException(ErrorCode errorCode) {
        super(errorCode);
    }
}
