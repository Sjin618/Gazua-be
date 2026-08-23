package com.acc.gazua.domain.payment.service;

public enum PaymentSyncResult {
    CONFIRMED,                  // 이번 호출로 확정됨
    CANCELLED,                  // 이번 호출로 취소/환불 반영됨
    FAILED,                     // PG에서 결제 실패로 확정
    ALREADY_DONE,               // 이미 동일 상태 (중복 호출)
    NOT_PAID_YET,               // 아직 미확정 (결제창 미완료, 가상계좌 입금대기 등)
    AMOUNT_MISMATCH,            // 위변조 의심 → 환불 처리됨
    UNKNOWN_PAYMENT,            // 우리 DB에 없는 paymentId
    PARTIAL_CANCEL_UNSUPPORTED, // 부분취소 — 수동 확인 필요
    PORTONE_UNAVAILABLE         // PortOne 장애/타임아웃 → 재시도 대상
}
