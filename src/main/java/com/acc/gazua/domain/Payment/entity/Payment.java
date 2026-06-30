package com.acc.gazua.domain.Payment.entity;

import com.acc.gazua.domain.reservation.entity.Reservation;
import com.acc.gazua.global.base.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Payment extends BaseTimeEntity {
    @Id @GeneratedValue
    @Column(name = "payment_id")
    private Long id;

    @ManyToOne
    private Reservation reservation;

    @Column(nullable = false)
    private Long userId;

    private String impUid; //PortOne 에서 생성된 고유 ID

    @Column(nullable = false, unique = true)
    private String merchantId; //서버에서 만든 고유 ID

    private Integer amount; //결제 금액

    private LocalDateTime paidAt; //결제된 시점

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status; //결제 상태

    private String payMethod; //결제 수단

    private String pgProvider; //PG사 정보

    private String cancelReason; //환불 정보

    private LocalDateTime cancelledAt; //환불 시점
}
