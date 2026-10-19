package com.acc.gazua.domain.region.exception;

import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.exception.BusinessException;

public class RegionException extends BusinessException {
    public RegionException(ErrorCode errorCode) {
        super(errorCode);
    }
}
