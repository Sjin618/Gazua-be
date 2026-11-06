package com.acc.gazua.domain.payment.entity;

import com.acc.gazua.domain.policy.dto.RefundCalculation;
import com.acc.gazua.domain.policy.entity.CancellationPolicyType;
import com.acc.gazua.global.base.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(indexes = @Index(name = "idx_refund_status_requested_at", columnList = "status, requested_at"))
public class Refund extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RefundStatus status;        //환불 상태

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RefundOrigin origin;        //환불 발생 원인

    @Column(nullable = false)
    private Integer requestedAmount;    //우리가 요청한 환불 금액

    @Column(nullable = false)
    private Integer cancellableAmount;  //요청 시점 취소 가능 금액 -
    //currentCancellableAmount 로 보내 이중 환불 막는 용도

    private Integer cancelFee;          //위약금

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private CancellationPolicyType appliedPolicy;   //적용된 환불 정책 (자동 환불은 null)

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime requestedAt;  //Clock 기준 - 재동기화 대상 조회의 기준

    //---- PG 응답으로 채워지는 값 ----
    @Column(unique = true)
    private String cancellationId;      //PortOne 취소 ID - 웹훅/재동기화 시 Refund 를 찾는 키

    private String pgCancellationId;    //PG 의 취소 ID

    private Integer refundedAmount;     //PG 가 실제 취소한 금액

    private LocalDateTime cancelledAt;  //PG 취소 완료 시각

    private String receiptUrl;

    private String failureMessage;

    //사용자 요청에 의한 환불 - 정책에 따라 환불한다
    public static Refund ofUser(Payment payment, RefundCalculation calc, String reason, LocalDateTime now) {
        Refund refund = create(payment, RefundOrigin.USER, calc.refundAmount(),
                payment.cancellableAmount(), reason, now);
        refund.cancelFee = calc.cancelFee();
        refund.appliedPolicy = calc.policy();
        if(calc.isNoRefund()) refund.status = RefundStatus.NO_REFUND;
        return refund;
    }

    //자동 환불 - 실제 결제된 금액 전체를 돌려준다 (금액 불일치면 price 와 다르다)
    public static Refund ofSystem(Payment payment, RefundOrigin origin, long paidTotal,
                                  String reason, LocalDateTime now) {
        return create(payment, origin, (int) paidTotal, (int) paidTotal, reason, now);
    }

    //우리 서버를 거치지 않은 PG 취소를 기록한다
    public static Refund ofExternal(Payment payment, int amount, String reason, LocalDateTime now) {
        return create(payment, RefundOrigin.EXTERNAL, amount,
                payment.cancellableAmount(),
                reason == null ? "외부 취소" : reason, now);
    }

    private static Refund create(Payment payment, RefundOrigin origin, int amount, int cancellable,
                                 String reason, LocalDateTime now) {
        Refund refund = new Refund();
        refund.payment = payment;
        refund.origin = origin;
        refund.status = RefundStatus.REQUESTED;
        refund.requestedAmount = amount;
        refund.cancellableAmount = cancellable;
        refund.reason = reason;
        refund.requestedAt = now;
        return refund;
    }

    //남은 금액 전부를 돌려주면 amount 를 비워 보내 PG 가 CANCELLED 로 끝내게 한다
    public Long pgAmount() {
        return requestedAmount.equals(cancellableAmount) ? null : requestedAmount.longValue();
    }

    //환불 진행 중인 상태인지
    public boolean isInProgress() {
        return RefundStatus.IN_PROGRESS.contains(this.status);
    }

    //이 환불로 결제가 전액 취소되었는지
    // null 체크 - 환불이 되지 않은(REQUESTED,PENDING,FAILED) 상태는 실제 환불액이 없어서 NPE 가능성
    // 환불된 금액이 현재 환불 가능한 금액보다 높다면 전액 환불로 본다
    public boolean coversRemaining() {
        return refundedAmount != null && refundedAmount >= cancellableAmount;
    }

    public void succeed(String cancellationId, String pgCancellationId, int refundedAmount,
                        LocalDateTime cancelledAt, String receiptUrl) {
        this.status = RefundStatus.SUCCEEDED;
        this.cancellationId = cancellationId;
        this.pgCancellationId = pgCancellationId;
        this.refundedAmount = refundedAmount;
        this.cancelledAt = cancelledAt;
        this.receiptUrl = receiptUrl;
        this.failureMessage = null;
    }

    public void pend(String cancellationId, String pgCancellationId) {
        this.status = RefundStatus.PENDING;
        this.cancellationId = cancellationId;
        this.pgCancellationId = pgCancellationId;
    }

    public void fail(String cancellationId, String failureMessage) {
        this.status = RefundStatus.FAILED;
        this.cancellationId = cancellationId;
        this.failureMessage = failureMessage;
    }

}
