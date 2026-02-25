package com.acc.gazua.global.security.exception;

import com.acc.gazua.global.dto.ErrorCode;
import lombok.Getter;

@Getter
public class JwtCustomException extends RuntimeException{
    private ErrorCode errorCode;

    public JwtCustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
