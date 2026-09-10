package com.acc.gazua.global.security.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class TokenHasher {
    private TokenHasher() {}

    //리프레시 토큰은 고엔트로피 값이라 salt 없는 SHA-256으로 충분하다.
    //조회 키로 써야 하므로 같은 입력에 항상 같은 결과가 나와야 한다.
    public static String hash(String token){
        try{
            //MessageDigest 는 쓰레드 안전하지 않기 때문에 항상 생성해줘야 함
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed); //항상 64자
        } catch(NoSuchAlgorithmException e){
            throw new IllegalStateException("SHA-256을 지원하지 않는 환경입니다.", e);
        }
    }

}
