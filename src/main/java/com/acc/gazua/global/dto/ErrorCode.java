package com.acc.gazua.global.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 공통 에러
    INVALID_INPUT_VALUE(400, "C001", "잘못된 입력값입니다."),
    METHOD_NOT_ALLOWED(405, "C002", "허용되지 않은 메서드입니다."),
    INTERNAL_SERVER_ERROR(500, "C003", "서버 내부 오류입니다."),

    //유저 관련 에러
    LOGIN_FAILED(400,"U001","아이디 또는 비밀번호가 일치하지 않습니다."),
    DUPLICATE_EMAIL(400,"U002","이미 존재하는 이메일입니다."),
    DELETED_USER(400,"U003","탈퇴한 계정입니다."),
    USER_NOT_FOUND(400,"U004","존재하지 않는 회원입니다."),

    //Jwt 관련 에러
    INVALID_TOKEN(401,"J001","잘못된 서명 또는 유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(401,"J002","만료된 토큰입니다. 다시 로그인하거나 갱신해주세요."),
    UNSUPPORTED_TOKEN(401,"J003","지원되지 않는 토큰 형식입니다."),
    EMPTY_TOKEN(401,"J004","토큰이 존재하지 않습니다.");

    private final int status;
    private final String code;
    private final String message;
}
