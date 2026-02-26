package com.acc.gazua.domain.user.exception;

import com.acc.gazua.global.dto.ErrorCode;

public class UserException extends RuntimeException{
    private ErrorCode errorCode;

    public UserException(ErrorCode errorCode){
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
