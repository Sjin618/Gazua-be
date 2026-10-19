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
    UNAUTHORIZED(401, "C004", "인증이 필요합니다."),
    FORBIDDEN(403, "C005", "접근 권한이 없습니다."),
    NOT_FOUND(404, "C006", "요청하신 리소스를 찾을 수 없습니다."),
    UNSUPPORTED_MEDIA_TYPE(415, "C007", "지원하지 않는 미디어 타입입니다."),
    MISSING_PARAMETER(400, "C008", "필수 요청 파라미터가 누락되었습니다."),
    TYPE_MISMATCH(400, "C009", "요청 파라미터 타입이 올바르지 않습니다."),
    DATA_INTEGRITY_VIOLATION(409, "C010", "요청을 처리할 수 없습니다. 입력값을 확인해주세요."),
    EXTERNAL_API_ERROR(503, "C011", "외부 서비스 연동에 실패했습니다. 잠시 후 다시 시도해주세요."),
    INVALID_CURSOR(400,"C012","잘못된 형식의 커서입니다."),
    INVALID_SORT_TYPE(400,"C013","잘못된 정렬 형식입니다,"),

    NOT_IMPLEMENTED(501,"C014","아직 제공하지 않는 기능입니다."),


    //유저 관련 에러
    LOGIN_FAILED(400,"U001","아이디 또는 비밀번호가 일치하지 않습니다."),
    DUPLICATE_EMAIL(409,"U002","이미 존재하는 이메일입니다."),
    DELETED_USER(400,"U003","탈퇴한 계정입니다."),
    USER_NOT_FOUND(404,"U004","존재하지 않는 회원입니다."),
    NOT_HOST_USER(400,"U005","숙소 주인으로 지정할 수 없는 회원입니다."),

    //숙소 관련 에러
    ACCOMMODATION_NOT_FOUND(404,"A001","존재하지 않는 숙소입니다."),
    INVALID_CHECK_DATE(400,"A002","체크아웃 날짜는 체크인 날짜보다 최소 1일 이상 커야합니다."),
    TOO_MANY_STAY_NIGHTS(400,"A003","최대 30박을 넘길 수 없습니다."),
    PAST_CHECK_IN_DATE(400,"A004","과거 날짜는 불가능합니다."),

    //좋아요 관련 에러
    ALREADY_LIKE(400,"L001","이미 좋아요한 숙소입니다."),

    //리뷰 관련 에러
    REVIEW_NOT_FOUND(404,"RE001","존재하지 않는 리뷰입니다."),
    ALREADY_CREATE_REVIEW(400,"RE002","해당 예약에 대해서 이미 작성한 리뷰가 존재합니다."),
    NOT_REVIEWABLE(400,"RE003","리뷰할 수 없는 예약입니다."),

    //예약 관련 에러
    RESERVATION_NOT_FOUND(404,"R001","존재하지 않는 예약입니다."),
    RESERVATION_NOT_AVAILABLE(400,"R002","예약이 불가능합니다."),
    RESERVATION_NOT_CANCELABLE(400,"R003","취소할 수 없는 예약입니다"),
    INVALID_RESERVATION_STATUS(400,"R004","알 수 없는 상태입니다."),
    RESERVATION_ALREADY_STARTED(400,"R005","이미 이용이 시작된 예약은 취소할 수 없습니다."),

    //방 재고 관련 에러
    OUT_OF_STOCK(400,"RI001","해당 날짜에 방 재고가 없습니다."),

    //방 관련 에러
    ROOM_NOT_FOUND(404,"RM001","존재하지 않는 방입니다."),

    //결제 관련 에러
    PAYMENT_NOT_FOUND(404,"P001","존재하지 않는 결제입니다."),
    PAYMENT_NOT_CANCELABLE(400,"P002","취소할 수 없는 결제입니다."),
    ALREADY_REFUNDED(400,"P003","이미 환불 처리가 접수된 결제입니다."),
    PAYMENT_AMOUNT_MISMATCH(400,"P004","결제 금액이 일치하지 않아 자동 환불되었습니다."),
    PAYMENT_FAILED(400,"P005","결제에 실패했습니다. 다시 시도해주세요."),
    PAYMENT_STATUS_UNKNOWN(500,"P006","결제 상태를 확인할 수 없습니다. 고객센터로 문의해 주세요."),
    PAYMENT_CONFIRM_DELAYED(503,"P007","결제 확인이 지연되고 있습니다. 잠시 후 다시 시도해주세요."),
    RESERVATION_EXPIRED(400,"P008","예약 가능 시간이 지나 결제 금액이 자동 환불됩니다."),
    PAYMENT_MANUAL_REVIEW(500,"P009","결제 처리에 문제가 발생했습니다. 확인 후 안내드리겠습니다."),
    CANCEL_IN_PROGRESS(409, "P010", "환불이 진행 중에 있습니다."),
    PAYMENT_CANCELLED(409, "P011", "취소 또는 환불된 결제입니다. 예약 내역에서 확인해주세요."),


    //알림 관련 에러
    NOTIFICATION_NOT_FOUND(404,"N001","존재하지 않는 알림입니다."),

    //지역 코드 관련 에러
    REGION_NOT_FOUND(400,"RG001","존재하지 않는 지역 코드입니다."),


    //Jwt 관련 에러
    INVALID_TOKEN(401,"J001","잘못된 서명 또는 유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(401,"J002","만료된 토큰입니다. 다시 로그인하거나 갱신해주세요."),
    UNSUPPORTED_TOKEN(401,"J003","지원되지 않는 토큰 형식입니다."),
    EMPTY_TOKEN(401,"J004","토큰이 존재하지 않습니다."),
    TOKEN_REUSE_DETECTED(401,"J005","비정상적인 접근이 감지되어 로그아웃되었습니다. 다시 로그인해주세요."),

    //파일 관련 에러
    FILE_TOO_LARGE(413,"F001","파일이 용량을 초과했습니다."),
    EMPTY_FILE(400,"F002","파일이 비어 있습니다."),
    INVALID_IMAGE_TYPE(415,"F003","JPG, PNG, WEBP 형식의 이미지만 업로드할 수 있습니다.");



    private final int status;
    private final String code;
    private final String message;
}
