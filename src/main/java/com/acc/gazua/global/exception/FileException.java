package com.acc.gazua.global.exception;

import com.acc.gazua.global.dto.ErrorCode;

public class FileException extends BusinessException{
    public FileException(ErrorCode errorCode) {
        super(errorCode);
    }

    public FileException(ErrorCode errorCode,Throwable e){
        super(errorCode, e);
    }
}
