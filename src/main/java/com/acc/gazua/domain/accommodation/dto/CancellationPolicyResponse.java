package com.acc.gazua.domain.accommodation.dto;

import com.acc.gazua.domain.policy.entity.CancellationPolicyType;
import com.acc.gazua.domain.policy.entity.RefundRule;

import java.util.List;

public record CancellationPolicyResponse(
        String code, // "MODERATE" - 프론트 분기, 필터용
        String displayName, // ex) "일반"
        int gracePeriodHours, // 24시간 등등
        List<RefundRule> rules // minDaysBefore:5, refundPercent:100
) {
    public static CancellationPolicyResponse from(CancellationPolicyType type){
        return new CancellationPolicyResponse(
                type.name(),
                type.getDisplayName(),
                type.getGracePeriodHours(),
                type.getRules()
        );
    }
}
