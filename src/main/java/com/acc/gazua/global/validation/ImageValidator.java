package com.acc.gazua.global.validation;

import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.exception.FileException;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

@Component
public class ImageValidator {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final Tika tika = new Tika(); //thread-safe, 재사용 가능

    /**
     * 파일 내용(매직 바이트) 으로 실제 타입을 판별해 허용된 이미지인지 검사한다.
     * 클라이언트가 보낸 Content-Type, 파일명은 신뢰하지 않는다.
     * @return 판별된 MIME 타입 (저장 시 확장자 결정에 사용)
     */
    public String validate(MultipartFile file){
        if (file == null || file.isEmpty()) {
            throw new FileException(ErrorCode.EMPTY_FILE);
        }

        String detectedType;
        try (InputStream inputStream = file.getInputStream()) {
            detectedType = tika.detect(inputStream); //파일명은 넘기지 않는다.
        }catch (IOException e){
            throw new FileException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }

        if(!ALLOWED_TYPES.contains(detectedType)){
            throw new FileException(ErrorCode.INVALID_IMAGE_TYPE);
        }

        return detectedType;
    }
}
