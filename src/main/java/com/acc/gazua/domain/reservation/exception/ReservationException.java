package com.acc.gazua.domain.reservation.exception;

import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.exception.BusinessException;

public class ReservationException extends BusinessException {
    public ReservationException(ErrorCode errorCode) {
        super(errorCode);
    }
}
