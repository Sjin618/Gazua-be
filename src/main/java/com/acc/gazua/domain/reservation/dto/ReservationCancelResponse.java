package com.acc.gazua.domain.reservation.dto;

import com.acc.gazua.domain.policy.dto.RefundCalculation;

public record ReservationCancelResponse(
        String policy,
        int refundAmount,
        int cancelFee,
        String notice
) {
    public static ReservationCancelResponse from(RefundCalculation calc) {
        return new ReservationCancelResponse(
                calc.policy().getDisplayName(),
                calc.refundAmount(),
                calc.cancelFee(),
                calc.isNoRefund()
                        ? "환불 가능 기간이 지나 환불 금액이 없습니다."
                        : "환불 금액은 결제하신 수단으로 영업일 기준 3~5일 내 처리됩니다."
        );
    }
}
