package com.acc.gazua.domain.policy.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

//플랫폼이 제공하는 환불 정책 프리셋
@Getter
@AllArgsConstructor
public enum CancellationPolicyType {

    FLEXIBLE("유연",24,List.of(
            new RefundRule(1,100),
            new RefundRule(0,50)
    )),
    MODERATE("일반", 24, List.of(
            new RefundRule(5, 100),
            new RefundRule(3, 70),
            new RefundRule(1, 50),
            new RefundRule(0, 0)
    )),
    STRICT("엄격", 24, List.of(
            new RefundRule(10, 100),
            new RefundRule(7, 50),
            new RefundRule(0, 0)
    )),
    NON_REFUNDABLE("환불 불가", 0, List.of()),;

    private final String displayName;      //보여지는 이름
    private final int gracePeriodHours;    //결제 직후 무조건 무료 취소되는 시간
    private final List<RefundRule> rules;  //minDaysBefore 내림차순으로 선언

    public String serializeRules() {
        return rules.stream().map(RefundRule::serialize).collect(Collectors.joining(","));
    }
}
