package com.acc.gazua.global.security.exception;

import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.security.filter.JwtFilter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        ErrorCode errorCode = (ErrorCode) request.getAttribute(JwtFilter.TOKEN_ERROR);
        if(errorCode == null){
            errorCode = ErrorCode.EMPTY_TOKEN;
        }
        log.warn("인증 실패 - {} {} : {}", request.getMethod(), request.getRequestURI(), errorCode.getCode());
        JwtErrorHandler.sendErrorResponse(response, errorCode);
    }
}
