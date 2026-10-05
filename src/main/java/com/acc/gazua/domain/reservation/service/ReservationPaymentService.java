package com.acc.gazua.domain.reservation.service;

import com.acc.gazua.domain.notification.entity.NotificationType;
import com.acc.gazua.domain.notification.service.NotificationService;
import com.acc.gazua.domain.payment.entity.Payment;
import com.acc.gazua.domain.payment.entity.PaymentStatus;
import com.acc.gazua.domain.payment.exception.PaymentException;
import com.acc.gazua.domain.payment.repository.PaymentRepository;
import com.acc.gazua.domain.payment.service.PaymentSyncResult;
import com.acc.gazua.domain.policy.dto.RefundCalculation;
import com.acc.gazua.domain.policy.service.RefundCalculator;
import com.acc.gazua.domain.reservation.dto.CancelPreparation;
import com.acc.gazua.domain.reservation.entity.Reservation;
import com.acc.gazua.domain.reservation.entity.ReservationStatus;
import com.acc.gazua.domain.reservation.exception.ReservationException;
import com.acc.gazua.domain.reservation.repository.ReservationRepository;
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
    private final NotificationService notificationService;
    private final Clock clock;

    //실제 결제 금액과 DB 금액 대조 로직(예약 확정 전 마지막 단계) - 결제 확정
    public PaymentSyncResult applyPaid(PortOneClient.PortOnePaymentSnapshot snapshot) {
        Payment payment = paymentRepository.findByPaymentIdForUpdate(snapshot.paymentId()).orElse(null);

        if(payment == null){
            log.warn("DB에 없는 결제 건 동기화 시도 - paymentId={}", snapshot.paymentId());
            return PaymentSyncResult.UNKNOWN_PAYMENT;
        }

        //READY, FAILED 상태의 Payment 만 통과시킨다.
        if (!payment.isAwaitingPayment()) {
            //이미 확정·환불·환불 요청·위약금 처리된 결제 - 웹훅 재전송, /confirm 재호출, 순서 역전
            return resultOf(payment);
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
        notificationService.notify(reservation, NotificationType.CONFIRM, 0);

        log.info("결제 확정 - paymentId={}, reservationId={}",
                snapshot.paymentId(), reservation.getId());
        return PaymentSyncResult.CONFIRMED;
    }

    //예약 취소 및 환불 로직 — READY(결제됐지만 확정을 기록하기 전의 결제) COMPLETED(결제 완료)든 재고는 동일하게 복구
    public PaymentSyncResult applyCancelled(String paymentId, Instant cancelledAt, String reason,
                                            long refundTotal, boolean fullyCancelled) {
        Payment payment = paymentRepository.findByPaymentIdForUpdate(paymentId).orElse(null);

        if(payment == null){
            log.warn("DB에 없는 결제 건 취소 시도 - paymentId={}", paymentId);
            return PaymentSyncResult.UNKNOWN_PAYMENT;
        }

        //이미 환불된 결제 재요청 막기
        if(payment.isRefundSettled()){
            return resultOf(payment);
        }

        //요청한 금액과 PG 실제 취소액이 다르면 반드시 인지해야 한다
        if (payment.getRefundAmount() != null && payment.getRefundAmount() != refundTotal) {
            log.error("환불 금액 불일치 - paymentId={}, expected={}, actual={}",
                    paymentId, payment.getRefundAmount(), refundTotal);
        }

        Reservation reservation = lockReservation(payment);
        ReservationStatus before = reservation.getStatus();
        payment.completeRefund(toLocalDateTIme(cancelledAt), refundTotal, reason, fullyCancelled);

        //환불 로직 도중에 만료 처리 스케쥴러가 동작해서 재고를 풀 수도 있기 때문에 예약이 재고를 잡고 있는지 확인한다.
        if(reservation.holdsInventory()){
            roomInventoryService.increaseRoomInventoryCount(reservation.getCheckDateTime(), reservation.getRoom());
        }
        //결제했다가 환불된 건이기 때문에 EXPIRED, CONFIRM -> CANCELLED 로 변경
        reservation.cancelReservation();
        NotificationType type = cancelTypeOf(before);
        if(type != null) notificationService.notify(reservation, type, refundTotal);


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
        notificationService.notify(reservation, NotificationType.NO_REFUND_FEE_CANCEL, 0);


        log.info("환불 없이 예약 취소 - paymentId={}, reservationId={}", paymentId, reservation.getId());
    }

    //결제 실패
    //환불 0원, 수수료도 0원
    //결제 실패이기 때문에, 가예약을 실패처리하지 않고 재고도 풀지 않음
    //재시도 시 reuseHold() 로 결제를 다시 시도할 수 있음
    public PaymentSyncResult applyFailed(String paymentId, String reason) {
        Payment payment = paymentRepository.findByPaymentIdForUpdate(paymentId).orElse(null);

        if (payment == null) {
            log.warn("DB에 없는 결제 건 실패 동기화 시도 - paymentId={}", paymentId);
            return PaymentSyncResult.UNKNOWN_PAYMENT;
        }

        //이미 환불 or 실패 처리된 결제에 대해서는 처리하지 않는다. (웹훅 중복 시도 방지)
        if(payment.getStatus() == PaymentStatus.FAILED) return PaymentSyncResult.FAILED;
        if(payment.getStatus() != PaymentStatus.READY) return resultOf(payment);

        Reservation reservation = lockReservation(payment);
        payment.fail(LocalDateTime.now(clock), reason); //status = FAILED

        notificationService.notify(reservation, NotificationType.PAYMENT_FAILED, 0);

        return PaymentSyncResult.FAILED;
    }

    //[TX 1] 취소 검증 + 환불액 확정 + 의도 기록, 외부 호출 전에 반드시 커밋되어야 한다.
    //Payment 를 REFUND_REQUESTED 상태로 전이
    public CancelPreparation prepareCancel(Long reservationId, Long userId,
                                           String reason) {
        Payment payment = paymentRepository.findByReservationIdForUpdate(reservationId)
                .orElseThrow(() -> new PaymentException(ErrorCode.PAYMENT_NOT_FOUND));

        Reservation reservation = payment.getReservation();
        reservation.equalsUser(userId); //본인 확인

        switch (payment.getStatus()) {
            case COMPLETED -> {}
            case REFUND,PARTIAL_REFUND,FORFEITED -> throw new PaymentException(ErrorCode.ALREADY_REFUNDED);
            case REFUND_REQUESTED -> throw new PaymentException(ErrorCode.CANCEL_IN_PROGRESS);
            case READY, FAILED -> throw new PaymentException(ErrorCode.PAYMENT_NOT_CANCELABLE);
        }

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

    private NotificationType cancelTypeOf(ReservationStatus status) {
        return switch (status) {
            case CONFIRM, COMPLETED -> NotificationType.REQUESTED_CANCEL; //결제 환불 요청 -> 누군가의 요청으로 인한 환불
            case EXPIRED, PENDING -> NotificationType.AUTO_CANCEL; //만료된 예약에 결제 시도, AMOUNT_MISMATCH 결제 금액 불일치 -> 자동 환불
            case CANCELLED -> null; //예약이 취소된 상태면 if 문에 걸리기 때문에 도달하지 않음
        };
    }

    private PaymentSyncResult resultOf(Payment payment) {
        return switch(payment.getStatus()){
            case COMPLETED -> PaymentSyncResult.ALREADY_CONFIRMED;
            case REFUND, REFUND_REQUESTED, PARTIAL_REFUND, FORFEITED -> PaymentSyncResult.CANCELLED;
            case FAILED -> PaymentSyncResult.FAILED;
            case READY -> PaymentSyncResult.NOT_PAID_YET;
        };
    }
}
