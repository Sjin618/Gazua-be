package com.acc.gazua.domain.roominventory.exception;

import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.exception.BusinessException;

public class RoomInventoryException extends BusinessException {
    public RoomInventoryException(ErrorCode errorCode) {
        super(errorCode);
    }
}
