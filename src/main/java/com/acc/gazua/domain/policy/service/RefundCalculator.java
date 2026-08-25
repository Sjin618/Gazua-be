package com.acc.gazua.domain.policy.service;

import com.acc.gazua.domain.payment.entity.Payment;
import com.acc.gazua.domain.policy.dto.RefundCalculation;
import com.acc.gazua.domain.policy.entity.CancellationPolicyType;
import com.acc.gazua.domain.policy.entity.RefundPolicySnapshot;
import com.acc.gazua.domain.reservation.entity.Reservation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

//환불 금액 계산 - 외부 의존 없는 순수 계산
@Component
@RequiredArgsConstructor
public class RefundCalculator {

    private final Clock clock;

    public RefundCalculation calculate(Reservation reservation, Payment payment) {
        RefundPolicySnapshot policy = policyOf(reservation);
        int paidPrice = payment.getPrice();

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime checkIn = reservation.getCheckDateTime().getCheckInDateTime();

        //체크인 날짜 기준 D-day
        long daysBefore = ChronoUnit.DAYS.between(LocalDate.now(clock), checkIn.toLocalDate());

        //1) 결제 직후 유예 시간 - 정책 보다 우선
        if (isWithinGracePeriod(payment, policy, now, checkIn)) {
            return RefundCalculation.grace(policy.getCode(), daysBefore, paidPrice);
        }

        //2) 정책 적용
        int percent = policy.refundPercentOf(daysBefore);
        int refund = applyPercent(paidPrice, percent);

        return new RefundCalculation(daysBefore, policy.getCode(), percent,
                refund, paidPrice - refund, "POLICY");
    }

    //스냅샷이 없는 과거 예약은 플랫폼 기본값으로 폴백
    private RefundPolicySnapshot policyOf(Reservation reservation) {
        return reservation.getRefundPolicy() != null
                ? reservation.getRefundPolicy()
                : RefundPolicySnapshot.of(CancellationPolicyType.MODERATE);
    }

    private boolean isWithinGracePeriod(Payment payment, RefundPolicySnapshot policy,
                                        LocalDateTime now, LocalDateTime checkIn) {
        if(payment.getPaidAt() == null || policy.graceHours() == 0) return false;
        //현재 시각이 무료 취소 기간 이전이라면 true, 아니면 false
        return now.isBefore(payment.getPaidAt().plusHours(policy.graceHours()))
                && now.isBefore(checkIn); //체크인 시각은 지나지 않아야 함

    }

    //원 단위로 절사
    private int applyPercent(int paidPrice, int percent) {
        return BigDecimal.valueOf(paidPrice)
                .multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100),0, RoundingMode.DOWN)
                .intValue();
    }

}
