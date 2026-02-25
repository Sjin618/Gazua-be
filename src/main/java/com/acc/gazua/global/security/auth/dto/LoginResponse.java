package com.acc.gazua.global.security.auth.dto;

public record LoginResponse(
        String grantType,
        String accessToken
) {
}
