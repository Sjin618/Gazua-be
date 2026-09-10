package com.acc.gazua.global.security.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieUtils {
    private final boolean secure;
    private final String sameSite;

    public CookieUtils(@Value("${app.cookie.secure:true}") boolean secure,
                       @Value("${app.cookie.same-site:Lax}") String sameSite) {
        this.secure = secure;
        this.sameSite = sameSite;
    }

    public ResponseCookie makeCookie(String cookieName, String cookieValue, long maxAge) {
        return ResponseCookie.from(cookieName, cookieValue)
                .httpOnly(true)
                .secure(secure)
                .path("/api/auth")
                .sameSite(sameSite)
                .maxAge(maxAge)
                .build();
    }

}
