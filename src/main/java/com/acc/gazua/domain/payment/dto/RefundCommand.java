package com.acc.gazua.domain.payment.dto;

import com.acc.gazua.domain.payment.entity.Refund;
import com.acc.gazua.domain.payment.entity.RefundStatus;

//트랜잭션 밖에서 PG 환불을 요청할 때 필요한 값 - OSIV를 껐기 때문에 Entity를 트랜잭션 밖으로 들고 나가지 않는다.
public record RefundCommand(
        Long refundId,
        String paymentId,
        RefundStatus status,
        Long amount,        //null 이면 남은 금액 전액 취소
        String reason,
        long cancellableAmount
) {
    public static RefundCommand from(Refund refund) {
        return new RefundCommand(refund.getId(),
                refund.getPayment().getPaymentId(),
                refund.getStatus(),
                refund.pgAmount(),
                refund.getReason(),
                refund.getCancellableAmount());
    }
}
