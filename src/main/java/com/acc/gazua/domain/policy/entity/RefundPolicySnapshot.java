package com.acc.gazua.domain.policy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

//예약 시점의 정책을 복사해 보관한다
//숙소가 나중에 정책을 바꿔도 기존 예약은 예약 당시 기준으로 환불되어야 하기 때문
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefundPolicySnapshot {

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_policy_code", length = 30)
    private CancellationPolicyType code; //표시 필터 통계용

    @Column(name = "refund_policy_rules", length = 200)
    private String rules;  // ex) "10:100,7:50,0:0" 환불 정책을 글로 저장

    @Column(name = "refund_grace_hours")
    private Integer gracePeriodHours;

    private RefundPolicySnapshot(CancellationPolicyType code, String rules, Integer gracePeriodHours) {
        this.code = code;
        this.rules = rules;
        this.gracePeriodHours = gracePeriodHours;
    }

    public static RefundPolicySnapshot of(CancellationPolicyType type) {
        return new RefundPolicySnapshot(type, type.serializeRules(), type.getGracePeriodHours());
    }

    public int refundPercentOf(long daysBefore) {
        if(rules == null || rules.isBlank()) return 0; //환불 불가
        for (String token : rules.split(",")) {
            RefundRule rule = RefundRule.parse(token);
            //남은 일 수 >= D일 전 성립해야 함
            if(daysBefore >= rule.minDaysBefore()) return rule.refundPercent();
        }
        return 0; //체크인일이 이미 지나서 환불 불가
    }

    public int graceHours(){
        return gracePeriodHours == null ? 0 : gracePeriodHours;
    }
}
