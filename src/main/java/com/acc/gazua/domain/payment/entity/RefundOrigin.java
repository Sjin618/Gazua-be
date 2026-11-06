package com.acc.gazua.domain.payment.entity;

import java.util.Set;

/**
 * 환불이 촉발된 원인이 무엇인지 기록하는 enum
 */
public enum RefundOrigin {
    USER,               //사용자 요청
    AMOUNT_MISMATCH,    //DB 와 PG 의 결제 금액 불일치 자동 환불
    EXPIRED,            //만료된 예약에 결제 승인 도착 자동 환불
    EXTERNAL;           //PortOne 콘솔 등 외부에서 요청된 환불 - 동기화 중 발견

    public static final Set<RefundOrigin> AUTO_REFUND = Set.of(AMOUNT_MISMATCH, EXPIRED);
}
