package com.acc.gazua.domain.payment.service;

public enum PaymentSyncResult {
    CONFIRMED,                  // 이번 호출로 확정됨
    CANCELLED,                  // 취소/환불 반영됨
    FAILED,                     // PG에서 결제 실패로 확정
    ALREADY_CONFIRMED,          // 이미 확정됨
    NOT_PAID_YET,               // 아직 미확정 (결제창 미완료, 가상계좌 입금대기 등)
    AMOUNT_MISMATCH,            // 위변조 의심 → 환불 처리됨
    UNKNOWN_PAYMENT,            // 우리 DB에 없는 paymentId
    PORTONE_UNAVAILABLE,        // PortOne 장애/타임아웃 → 재시도 대상
    RESERVATION_EXPIRED,        // 만료 처리된 예약 -> 자동 환불 처리
    MANUAL_REVIEW_REQUIRED      // 우리 서버에서 PortOne 으로의 요청 실패, 확인이 필요한 요청
}
