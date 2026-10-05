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
                notice(calc)
        );
    }

    private static String notice(RefundCalculation calc){
        if(calc.isNoRefund()) return "환불 정책에 따라 환불 금액이 없습니다.";
        return "취소 요청이 접수되었습니다. 환불 금액은 결제하신 수단으로 영업일 기준 3~5일 내 처리됩니다.";
    }
}
