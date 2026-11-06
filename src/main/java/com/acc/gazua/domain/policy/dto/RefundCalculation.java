package com.acc.gazua.domain.policy.dto;

import com.acc.gazua.domain.policy.entity.CancellationPolicyType;

public record RefundCalculation(
        long daysBefore,
        CancellationPolicyType policy, //적용된 환불 정책
        int refundPercent, //환불된 비율
        int refundAmount, //환불된 금액
        int cancelFee, //위약금
        String appliedReason // POLICY | GRACE_PERIOD
) {
    public boolean isNoRefund(){ return refundAmount == 0; }
    public boolean isFullRefund() { return cancelFee == 0;}

    public static RefundCalculation grace(CancellationPolicyType policy, long daysBefore, int paidPrice) {
        return new RefundCalculation(daysBefore, policy, 100, paidPrice, 0, "GRACE_PERIOD");
    }
}
