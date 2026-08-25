package com.acc.gazua.domain.policy.entity;

//환불 정책의 구간 한 줄(체크인 minDaysBefore 일 전 이상이면 refundPercent % 환불)
public record RefundRule(
        int minDaysBefore,
        int refundPercent
) {

    public String serialize(){
        return minDaysBefore + ":" + refundPercent;
    }

    public static RefundRule parse(String token) {
        String[] kv = token.split(":");
        return new RefundRule(Integer.parseInt(kv[0]), Integer.parseInt(kv[1]));
    }
}
