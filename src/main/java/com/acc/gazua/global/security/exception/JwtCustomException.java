package com.acc.gazua.global.security.exception;

import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.exception.BusinessException;

public class JwtCustomException extends BusinessException {
    public JwtCustomException(ErrorCode errorCode) {
        super(errorCode);
    }
}
