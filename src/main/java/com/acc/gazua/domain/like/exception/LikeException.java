package com.acc.gazua.domain.like.exception;

import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.exception.BusinessException;

public class LikeException extends BusinessException {
    public LikeException(ErrorCode errorCode) {
        super(errorCode);
    }
}
