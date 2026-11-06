package com.acc.gazua.domain.payment.entity;

import com.acc.gazua.domain.policy.dto.RefundCalculation;
import com.acc.gazua.domain.policy.entity.CancellationPolicyType;
import com.acc.gazua.domain.reservation.entity.Reservation;
import com.acc.gazua.global.base.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(indexes = @Index(name = "idx_payment_auto_refund_retry_at", columnList = "auto_refund_retry_at"))
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Payment extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    private Reservation reservation;

    @Column(nullable = false, unique = true)
    private String paymentId; //서버에서 만드는 고유 ID

    private Integer price; //결제 금액

    private LocalDateTime paidAt; //결제된 시점

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status; //결제 상태

    private String payMethod; //결제 수단

    private String pgProvider; //PG사 정보

    @Builder.Default
    @Column(nullable = false)
    private Integer cancelledAmount = 0; //PG 기준 누적 취소 금액 (성공한 Refund 합계 캐시)

    private String failReason; //결제 실패 사유

    private LocalDateTime failedAt; //결제 실패 시점

    @Builder.Default
    @Column(nullable = false)
    private Integer autoRefundFailCount = 0; //실패한 자동 환불 수 (FAILED 자동 Refund 수 캐시)

    private LocalDateTime autoRefundRetryAt; //다음 자동 환불 시각, null 이면 재시도 대상 X

    private static final int MAX_AUTO_REFUND_ATTEMPTS = 3;
    private static final Duration AUTO_REFUND_RETRY_INTERVAL = Duration.ofHours(1);

    public void confirmPayment(LocalDateTime paidAt, String payMethod, String pgProvider) {
        this.status = PaymentStatus.PAID;
        this.paidAt = paidAt;
        this.payMethod = payMethod;
        this.pgProvider = pgProvider;
        this.failedAt = null; //재시도 전 실패 기록 제거
        this.failReason = null;
    }

    //새 시도를 시작한다 - 진행 중에는 재시도 대상에서 뺀다
    public void startAutoRefund() {
        this.autoRefundRetryAt = null;
    }

    public void recordAutoRefundFailure(LocalDateTime now) {
        this.autoRefundFailCount++;
        this.autoRefundRetryAt = autoRefundFailCount < MAX_AUTO_REFUND_ATTEMPTS
                ? now.plus(AUTO_REFUND_RETRY_INTERVAL) : null;
    }

    //다음 환불 요청 시 PG 에 보낼 취소 가능 금액
    public int cancellableAmount() {
        return price - cancelledAmount;
    }

    //PG 기준 누적 취소액과 상태를 맞춘다
    public void syncCancelled(long cancelledTotal, boolean fullyCancelled) {
        this.cancelledAmount = (int) cancelledTotal;
        this.status = fullyCancelled ? PaymentStatus.REFUND : PaymentStatus.PARTIAL_REFUND;
        this.autoRefundRetryAt = null; //환불이 반영된 결제는 더 이상 재시도 하지 않음
    }

    public void forfeit() {
        this.status = PaymentStatus.FORFEITED;
    }

    public void fail(LocalDateTime failedAt, String reason) {
        this.status = PaymentStatus.FAILED;
        this.failedAt = failedAt;
        this.failReason = reason;
    }

    //이미 환불 처리된 결제 건인지
    public boolean isRefundSettled() {
        return status == PaymentStatus.REFUND
                || status == PaymentStatus.PARTIAL_REFUND
                || status == PaymentStatus.FORFEITED;
    }

    public boolean isAwaitingPayment() {
        return this.status == PaymentStatus.READY || this.status == PaymentStatus.FAILED;
    }

    public boolean isAutoRefundExhausted() {
        return autoRefundFailCount >= MAX_AUTO_REFUND_ATTEMPTS;
    }

    public boolean canRequestAutoRefund(LocalDateTime now) {
        return autoRefundFailCount < MAX_AUTO_REFUND_ATTEMPTS
                && (autoRefundRetryAt == null || !autoRefundRetryAt.isAfter(now));
    }

}
