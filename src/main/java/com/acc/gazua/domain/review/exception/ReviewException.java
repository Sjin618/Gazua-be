package com.acc.gazua.domain.review.exception;

import com.acc.gazua.global.dto.ErrorCode;

public class ReviewException extends RuntimeException {
    private ErrorCode errorCode;
    public ReviewException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
