package com.acc.gazua.domain.reservation.entity;

public enum ReservationStatus {
    PENDING, //결제 전
    CONFIRM, //결제 완료
    CANCELLED, //환불 or 기타 취소
    COMPLETED // 체크아웃 완료
}
