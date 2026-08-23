package com.acc.gazua.domain.payment.exception;

import com.acc.gazua.global.dto.ErrorCode;
import lombok.Getter;

@Getter
public class PaymentException extends RuntimeException {
    private final ErrorCode errorCode;

    public PaymentException(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }
}
