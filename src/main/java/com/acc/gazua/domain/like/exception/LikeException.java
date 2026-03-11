package com.acc.gazua.domain.like.exception;

import com.acc.gazua.global.dto.ErrorCode;

public class LikeException extends RuntimeException{
    private ErrorCode errorCode;

    public LikeException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
