package com.acc.gazua.domain.payment.entity;

import java.util.Set;

public enum RefundStatus {
    REQUESTED,      //DB 에 환불 의도만 커밋 - PG 결과를 모름(요청 전 / 응답 유실)
    PENDING,        //PG 가 환불을 접수만 함 (PortOne cancellation.status = REQUESTED) - 재요청 금지
    SUCCEEDED,      //PG 환불 완료
    FAILED,         //PG 가 환불을 거절 - 돈이 나가지 않았으므로 새 Refund 로 재요청 가능
    NO_REFUND;      //위약금 100% - PG 호출 없이 종결

    public static final Set<RefundStatus> IN_PROGRESS = Set.of(REQUESTED,PENDING);
}
