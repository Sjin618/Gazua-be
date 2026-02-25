package com.acc.gazua.global.security.auth.controller;

import com.acc.gazua.global.dto.ApiResponse;
import com.acc.gazua.global.security.auth.dto.LoginRequest;
import com.acc.gazua.global.security.auth.dto.LoginResponse;
import com.acc.gazua.global.security.auth.service.AuthServiceRDBImpl;
import com.acc.gazua.global.security.details.CustomUserDetails;
import com.acc.gazua.global.security.provider.dto.TokenDto;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthServiceRDBImpl authServiceRDBImpl;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest,
                                                   HttpServletResponse response){
        TokenDto tokenDto = authServiceRDBImpl.login(loginRequest);

        ResponseCookie cookie = makeCookie("refreshToken",
                tokenDto.getRefreshToken(),
                tokenDto.getRefreshTokenExpirationTime() / 1000);

        response.addHeader("Set-Cookie",cookie.toString());

        LoginResponse loginResponse = new LoginResponse(tokenDto.getGrantType(), tokenDto.getAccessToken());

        return ResponseEntity.ok(ApiResponse.success(loginResponse,"로그인 성공"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response,
                                                    @CookieValue(value = "refreshToken",required = false) String refreshToken){
        //RDB 에서 refresh token 제거
        //refresh token이 있는 경우에만 제거
        //없는 경우는 refresh token을 만료 처리만 한다.
        if(refreshToken != null) {
            authServiceRDBImpl.logout(refreshToken);
        }
        //refresh token 유효기간 0으로 설정해서 제거
        ResponseCookie cookie = makeCookie("refreshToken","",0);

        response.addHeader("Set-Cookie", cookie.toString());

        return ResponseEntity.ok(ApiResponse.success("로그아웃 성공"));
    }

    @PostMapping("/re-issue")
    public ResponseEntity<ApiResponse<LoginResponse>> reissue(HttpServletResponse response,
                                                              @CookieValue(value = "refreshToken", required = false) String refreshToken) {
        TokenDto tokenDto = authServiceRDBImpl.reissue(refreshToken);

        ResponseCookie cookie = makeCookie("refreshToken",
                tokenDto.getRefreshToken(),
                tokenDto.getRefreshTokenExpirationTime() / 1000);

        response.addHeader("Set-Cookie", cookie.toString());

        LoginResponse loginResponse = new LoginResponse(tokenDto.getGrantType(), tokenDto.getAccessToken());

        return ResponseEntity.ok(ApiResponse.success(loginResponse, "reIssue 완료"));
    }

    private ResponseCookie makeCookie(String cookieName, String cookieValue, long maxAge) {
        return ResponseCookie.from(cookieName, cookieValue)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
