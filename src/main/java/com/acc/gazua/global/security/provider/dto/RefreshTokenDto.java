package com.acc.gazua.global.security.provider.dto;

import java.time.LocalDateTime;

public record RefreshTokenDto(
        String refreshToken,
        LocalDateTime expiredAt
) {
}
