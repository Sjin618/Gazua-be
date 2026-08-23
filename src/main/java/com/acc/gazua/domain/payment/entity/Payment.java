package com.acc.gazua.domain.payment.entity;

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
    @Id @GeneratedValue
    private Long id;

    @ManyToOne
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

    public void confirmPayment(){
        this.status = PaymentStatus.COMPLETED;
    }

    public void refundPayment(){
        this.status = PaymentStatus.REFUND;
    }

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
}
