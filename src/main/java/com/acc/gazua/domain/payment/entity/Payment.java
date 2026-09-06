package com.acc.gazua.domain.payment.entity;

import com.acc.gazua.domain.policy.dto.RefundCalculation;
import com.acc.gazua.domain.policy.entity.CancellationPolicyType;
import com.acc.gazua.domain.reservation.entity.Reservation;
import com.acc.gazua.global.base.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Payment extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @Column(nullable = false, unique = true)
    private String paymentId; //서버에서 만드는 고유 ID

    // private String merchantId; //PortOne 고객사 ID

    private Integer price; //결제 금액

    private LocalDateTime paidAt; //결제된 시점

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status; //결제 상태

    private String payMethod; //결제 수단

    private String pgProvider; //PG사 정보

    private String cancelReason; //환불 정보

    private LocalDateTime cancelledAt; //환불 시점

    private Integer refundAmount; //환불 금액

    private Integer cancelFee; //위약금

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private CancellationPolicyType appliedPolicy; //적용된 환불 정책

    public void confirmPayment(LocalDateTime paidAt, String payMethod, String pgProvider) {
        this.status = PaymentStatus.COMPLETED;
        this.paidAt = paidAt;
        this.payMethod = payMethod;
        this.pgProvider = pgProvider;
    }

    public void refundPayment(LocalDateTime cancelledAt, String cancelReason) {
        this.status = PaymentStatus.REFUND;
        this.cancelledAt = cancelledAt;
        this.cancelReason = cancelReason;
    }

    //사용자 환불 요청 시, 우리 서버 기준으로 환불 요청 처리
    public void markRefundRequested(RefundCalculation calc, String reason, LocalDateTime now) {
        this.status = PaymentStatus.REFUND_REQUESTED;
        this.refundAmount = calc.refundAmount();
        this.cancelFee = calc.cancelFee();
        this.appliedPolicy = calc.policy();
        this.cancelReason = reason;
        this.cancelledAt = now;
    }

    //PG사에서 준 환불 상태를 그대로 저장한다. (우리 서버에서 임의로 처리하지 않음)
    public void completeRefund(LocalDateTime cancelledAt, long cancelledTotal,
                               String reason, boolean fullyCancelled) {
        this.status = fullyCancelled ? PaymentStatus.REFUND : PaymentStatus.PARTIAL_REFUND;
        this.refundAmount = (int) cancelledTotal;
        this.cancelFee = this.price - this.refundAmount;
        this.cancelledAt = cancelledAt;
        this.cancelReason = reason;
    }

    public void forfeit(LocalDateTime cancelledAt, String reason) {
        this.status = PaymentStatus.FORFEITED;
        this.refundAmount = 0;
        this.cancelFee = this.price;
        this.cancelledAt = cancelledAt;
        this.cancelReason = reason;
    }

    //이미 환불 처리된 결제 건인지
    public boolean isRefundSettled() {
        return status == PaymentStatus.REFUND
                || status == PaymentStatus.PARTIAL_REFUND
                || status == PaymentStatus.FORFEITED;
    }
}
