package com.acc.gazua.global.security.exception;

import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

public class JwtErrorHandler {

    private final static ObjectMapper objectMapper = new ObjectMapper();

    public static void sendErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus());
        response.setContentType("application/json;charset=UTF-8");

        ErrorResponse errorResponse = ErrorResponse.of(errorCode);

        String json = objectMapper.writeValueAsString(errorResponse);

        response.getWriter().write(json);
    }
}
