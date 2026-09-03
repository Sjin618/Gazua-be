package com.acc.gazua.domain.reservation.dto;

import com.acc.gazua.domain.policy.dto.RefundCalculation;

public record ReservationCancelResponse(
        String policy,
        int refundAmount,
        int cancelFee,
        String notice
) {
    public static ReservationCancelResponse from(RefundCalculation calc, boolean settled) {
        return new ReservationCancelResponse(
                calc.policy().getDisplayName(),
                calc.refundAmount(),
                calc.cancelFee(),
                notice(calc,settled)
        );
    }

    private static String notice(RefundCalculation calc, boolean settled){
        if(calc.isNoRefund()) return "환불 정책에 따라 환불 금액이 없습니다.";
        return settled ? "환불 금액은 결제하신 수단으로 영업일 기준 3~5일 내 처리됩니다.":
                "취소 요청이 접수되었습니다. 처리 결과는 예약 내역에서 확인하실 수 있습니다.";
    }
}
