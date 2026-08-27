package com.acc.gazua.global.page;

import com.acc.gazua.global.dto.CustomCursor;
import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class JsonCursorUtils {
    private final ObjectMapper objectMapper;

    public String serialize(CustomCursor cursor){
        if(cursor == null || (cursor.cursorValue() == null && cursor.cursorId() == null)){
            return null;
        }

        try{
            String json = objectMapper.writeValueAsString(cursor);
            return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        }catch(Exception e){
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }

    public CustomCursor deserialize(String encodedCursor){
        if(encodedCursor == null || encodedCursor.isBlank()){
            return null;
        }

        try{
            byte[] decodedBytes = Base64.getDecoder().decode(encodedCursor);
            String json = new String(decodedBytes, StandardCharsets.UTF_8);
            return objectMapper.readValue(json,CustomCursor.class);
        }catch(Exception e){
            throw new BusinessException(ErrorCode.INVALID_CURSOR, e);
        }
    }
}
