package com.acc.gazua.domain.notification.exception;


import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.exception.BusinessException;

public class NotificationException extends BusinessException {
    public NotificationException(ErrorCode errorCode) {
        super(errorCode);
    }
}
