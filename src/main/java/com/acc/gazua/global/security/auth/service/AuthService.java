package com.acc.gazua.global.security.auth.service;

import com.acc.gazua.global.security.auth.dto.LoginRequest;
import com.acc.gazua.global.security.details.CustomUserDetails;
import com.acc.gazua.global.security.provider.dto.TokenDto;

public interface AuthService {
    TokenDto login(LoginRequest request);

    void logout(String refreshToken);

    TokenDto reissue(String refreshToken);
}
