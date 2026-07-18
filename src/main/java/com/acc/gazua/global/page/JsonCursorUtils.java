package com.acc.gazua.global.page;

import com.acc.gazua.global.dto.CustomCursor;
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
            throw new IllegalArgumentException("커서 직렬화에 실패했습니다.", e);
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
            throw new IllegalArgumentException("잘못된 형식의 커서입니다.", e);
        }
    }
}
