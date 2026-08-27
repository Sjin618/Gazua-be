package com.acc.gazua.domain.review.exception;

import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.exception.BusinessException;

public class ReviewException extends BusinessException {
    public ReviewException(ErrorCode errorCode) {
        super(errorCode);
    }
}
