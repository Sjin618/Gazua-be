package com.acc.gazua.global.exception;

import com.acc.gazua.global.dto.ErrorCode;
import lombok.Getter;

/**
 * 도메인 예외의 공통 부모.
 * GlobalExceptionHandler 는 이 타입 하나만 잡아서 ErrorCode 기반 응답을 만든다.
 */
@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
