package com.acc.gazua.domain.room.exception;

import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.exception.BusinessException;

public class RoomException extends BusinessException {
    public RoomException(ErrorCode errorCode) {
        super(errorCode);
    }
}
