package com.acc.gazua.domain.payment.entity;

public enum PaymentStatus {
    READY,              //결제 전
    PAID,          //결제 완료
    PARTIAL_REFUND,     //위약금 공제 후 부분 환불
    REFUND,             //전액 환불
    FORFEITED,          //환불 없이 취소 종결(위약금 100%)
    FAILED              //결제 실패

}
