package com.acc.gazua.domain.reservation.service;

import com.acc.gazua.domain.payment.entity.Payment;
import com.acc.gazua.domain.payment.entity.PaymentStatus;
import com.acc.gazua.domain.payment.exception.PaymentException;
import com.acc.gazua.domain.payment.repository.PaymentRepository;
import com.acc.gazua.domain.payment.service.PaymentSyncResult;
import com.acc.gazua.domain.policy.dto.RefundCalculation;
import com.acc.gazua.domain.policy.service.RefundCalculator;
import com.acc.gazua.domain.reservation.dto.CancelPreparation;
import com.acc.gazua.domain.reservation.entity.Reservation;
import com.acc.gazua.domain.reservation.exception.ReservationException;
import com.acc.gazua.domain.reservation.repository.ReservationRepository;
import com.acc.gazua.domain.roominventory.repository.RoomInventoryRepository;
import com.acc.gazua.domain.roominventory.service.RoomInventoryService;
import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.portone.PortOneClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
/**
 * 결제와 연관된 메서드들을 모아둔 서비스 클래스
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ReservationPaymentService {
    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final RoomInventoryService roomInventoryService;
    private final RefundCalculator refundCalculator;
    private final Clock clock;

    //실제 결제 금액과 DB 금액 대조 로직(예약 확정 전 마지막 단계) - 결제 확정
    public PaymentSyncResult applyPaid(PortOneClient.PortOnePaymentSnapshot snapshot) {
        Payment payment = paymentRepository.findByPaymentIdForUpdate(snapshot.paymentId()).orElse(null);

        if(payment == null){
            log.warn("DB에 없는 결제 건 동기화 시도 - paymentId={}", snapshot.paymentId());
            return PaymentSyncResult.UNKNOWN_PAYMENT;
        }

        //이미 결제 확정된 건은 재시도 하지 않음
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            return PaymentSyncResult.ALREADY_DONE;
        }
        if (payment.getStatus() == PaymentStatus.REFUND) {
            // 순서 역전: Cancelled가 먼저 처리된 뒤 Paid가 뒤늦게 도착
            log.warn("이미 환불된 건에 결제 확정 시도 - 무시 - paymentId={}", snapshot.paymentId());
            return PaymentSyncResult.ALREADY_DONE;
        }

        if (payment.getPrice().longValue() != snapshot.paidTotal()) {
            log.warn("결제 금액 불일치 - paymentId={}, expected={}, actual={}",
                    snapshot.paymentId(), payment.getPrice(), snapshot.paidTotal());
            return PaymentSyncResult.AMOUNT_MISMATCH;
        }

        Reservation reservation = lockReservation(payment);
        if (!reservation.isPending()) {
            log.error("만료/취소된 예약에 결제 승인 도착 - 자동 환불 필요 - paymentId={}, status={}",
                    snapshot.paymentId(), reservation.getStatus());
            return PaymentSyncResult.RESERVATION_EXPIRED;
        }

        payment.confirmPayment(toLocalDateTIme(snapshot.paidAt()), snapshot.payMethod(), snapshot.pgProvider());
        reservation.confirmReservation();

        log.info("결제 확정 - paymentId={}, reservationId={}",
                snapshot.paymentId(), reservation.getId());
        return PaymentSyncResult.CONFIRMED;
    }

    //예약 취소 및 환불 로직 — READY(미결제 취소)든 COMPLETED(환불)든 재고는 동일하게 복구
    public PaymentSyncResult applyCancelled(String paymentId, Instant cancelledAt, String reason,
                                            long cancelledTotal, boolean fullyCancelled) {
        Payment payment = paymentRepository.findByPaymentIdForUpdate(paymentId).orElse(null);

        if(payment == null){
            log.warn("DB에 없는 결제 건 취소 시도 - paymentId={}", paymentId);
            return PaymentSyncResult.UNKNOWN_PAYMENT;
        }

        //이미 환불된 결제 재요청 막기
        if(payment.isRefundSettled()){
            return PaymentSyncResult.ALREADY_DONE;
        }

        //요청한 금액과 PG 실제 취소액이 다르면 반드시 인지해야 한다
        if (payment.getRefundAmount() != null && payment.getRefundAmount() != cancelledTotal) {
            log.error("환불 금액 불일치 - paymentId={}, expected={}, actual={}",
                    paymentId, payment.getRefundAmount(), cancelledTotal);
        }

        Reservation reservation = lockReservation(payment);
        payment.completeRefund(toLocalDateTIme(cancelledAt), cancelledTotal, reason, fullyCancelled);

        //환불 로직 도중에 만료 처리 스케쥴러가 동작해서 재고를 풀 수도 있기 때문에 예약이 재고를 잡고 있는지 확인한다.
        if(reservation.holdsInventory()){
            roomInventoryService.increaseRoomInventoryCount(reservation.getCheckDateTime(), reservation.getRoom());
        }
        //결제했다가 환불된 건이기 때문에 EXPIRED, CONFIRM -> CANCELLED 로 변경
        reservation.cancelReservation();

        log.info("예약 취소 - paymentId={}, reservationId={}, reason={}",
                paymentId, reservation.getId(), reason);
        return PaymentSyncResult.CANCELLED;
    }

    //환불 0원 - PG 호출 없이 예약만 취소하고 재고를 푼다
    //PG는 환불 0원이 불가능하기 때문에 환불 없이 예약 취소만 한다.
    //환불 정책에 따라 100% 위약금이면 이 메서드를 호출
    public void applyForfeited(String paymentId, String reason) {
        Payment payment = paymentRepository.findByPaymentIdForUpdate(paymentId)
                .orElseThrow(() -> new PaymentException(ErrorCode.PAYMENT_NOT_FOUND));

        if(payment.isRefundSettled()) return;

        Reservation reservation = lockReservation(payment);
        if(reservation.holdsInventory()){
            roomInventoryService.increaseRoomInventoryCount(reservation.getCheckDateTime(),
                    reservation.getRoom());
        }
        payment.forfeit(LocalDateTime.now(clock), reason);
        reservation.cancelReservation();


        log.info("환불 없이 예약 취소 - paymentId={}, reservationId={}", paymentId, reservation.getId());
    }

    //[TX 1] 취소 검증 + 환불액 확정 + 의도 기록, 외부 호출 전에 반드시 커밋되어야 한다.
    //Payment 를 REFUND_REQUESTED 상태로 전이
    public CancelPreparation prepareCancel(Long reservationId, Long userId,
                                           String reason) {
        Payment payment = paymentRepository.findSettledByReservationIdForUpdate(reservationId)
                .orElseThrow(() -> new PaymentException(ErrorCode.PAYMENT_NOT_FOUND));

        Reservation reservation = payment.getReservation();
        reservation.equalsUser(userId); //본인 확인

        //이미 환불 요청이 된 건은 여기서 반환한다. -> 하지 않으면 마지막 if 문에 막힘
        if(payment.getStatus() == PaymentStatus.REFUND_REQUESTED){
            return new CancelPreparation(payment.getPaymentId(), refundCalculator.calculate(reservation, payment));
        }

        //이미 환불 처리된 결제 건인지 확인 -> REFUND, PARTIAL_REFUND, FORFEITED
        if(payment.isRefundSettled()) throw new PaymentException(ErrorCode.ALREADY_REFUNDED);
        //이미 완료된 결제만 환불 가능
        if(payment.getStatus() != PaymentStatus.COMPLETED) throw new PaymentException(ErrorCode.PAYMENT_NOT_CANCELABLE);

        //가예약, 체크아웃, 취소건 차단 - CONFIRM 상태의 예약만 취소 가능
        if(!reservation.isConfirmed()) throw new ReservationException(ErrorCode.RESERVATION_NOT_CANCELABLE);

        //체크인이 시작되기 전 시각에만 예약 취소 가능
        if(reservation.hasStarted(LocalDateTime.now(clock))){
            throw new ReservationException(ErrorCode.RESERVATION_ALREADY_STARTED);
        }

        RefundCalculation calc = refundCalculator.calculate(reservation, payment);
        payment.markRefundRequested(calc, reason, LocalDateTime.now(clock));

        return new CancelPreparation(payment.getPaymentId(), calc);
    }

    private LocalDateTime toLocalDateTIme(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.of("Asia/Seoul"));
    }

    //스케줄러 만료와 결제 시점이 겹치면 예상치 못한 에러가 발생하기에 항상 잠금 읽기로 가져온다.
    //LAZY Proxy 읽기는 스냅샷 읽기라 잠금이 되지 않는다.
    private Reservation lockReservation(Payment payment) {
        return reservationRepository.findByIdForUpdate(payment.getReservation().getId())
                .orElseThrow(() -> new ReservationException(ErrorCode.RESERVATION_NOT_FOUND));
    }
}
