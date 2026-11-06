package com.acc.gazua.domain.reservation.service;

import com.acc.gazua.domain.notification.entity.NotificationType;
import com.acc.gazua.domain.notification.service.NotificationService;
import com.acc.gazua.domain.payment.dto.RefundCommand;
import com.acc.gazua.domain.payment.entity.*;
import com.acc.gazua.domain.payment.exception.PaymentException;
import com.acc.gazua.domain.payment.repository.PaymentRepository;
import com.acc.gazua.domain.payment.repository.RefundRepository;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static com.acc.gazua.global.portone.PortOneClient.*;

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
    private final RefundRepository refundRepository;
    private final ReservationHoldService reservationHoldService;
    private final Clock clock;

    private static final Duration MATCH_MARGIN = Duration.ofMinutes(1);

    //실제 결제 금액과 DB 금액 대조 로직(예약 확정 전 마지막 단계) - 결제 확정
    public PaymentSyncResult applyPaid(PortOnePaymentSnapshot snapshot) {
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
        return PaymentSyncResult.PAID;
    }


    //[TX 1] 취소 검증 + 환불액 확정 + Refund(REQUESTED) 의도 기록, 외부 호출 전에 반드시 커밋되어야 한다.
    //환불 0원이면 PG 를 부르지 않으므로 이 트랜잭션에서 바로 종결한다
    public CancelPreparation prepareCancel(Long reservationId, Long userId,
                                           String reason) {
        Payment payment = paymentRepository.findByReservationIdForUpdate(reservationId)
                .orElseThrow(() -> new PaymentException(ErrorCode.PAYMENT_NOT_FOUND));

        Reservation reservation = payment.getReservation();
        reservation.equalsUser(userId); //본인 확인

        //PAID 만 환불 가능, 나머지 상태는 통과 불가
        switch (payment.getStatus()) {
            case PAID -> {}
            case REFUND,PARTIAL_REFUND,FORFEITED -> throw new PaymentException(ErrorCode.ALREADY_REFUNDED);
            case READY, FAILED -> throw new PaymentException(ErrorCode.PAYMENT_NOT_CANCELABLE);
        }

        //Payment 행 장금 아래에서 확인하므로 동시에 들어온 취소 요청 중 하나만 통과한다
        if (refundRepository.existsByPaymentAndStatusIn(payment, RefundStatus.IN_PROGRESS)) {
            throw new PaymentException(ErrorCode.CANCEL_IN_PROGRESS);
        }

        //가예약, 체크아웃, 취소건 차단 - CONFIRM 상태의 예약만 취소 가능
        if(!reservation.isConfirmed()) throw new ReservationException(ErrorCode.RESERVATION_NOT_CANCELABLE);

        //체크인이 시작되기 전 시각에만 예약 취소 가능
        if(reservation.hasStarted(LocalDateTime.now(clock))){
            throw new ReservationException(ErrorCode.RESERVATION_ALREADY_STARTED);
        }

        RefundCalculation calc = refundCalculator.calculate(reservation, payment);
        Refund refund = refundRepository.save(Refund.ofUser(payment, calc, reason, LocalDateTime.now(clock)));

        if (calc.isNoRefund()) {
            payment.forfeit();
            settleReservation(payment, 0, NotificationType.NO_REFUND_FEE_CANCEL);
            log.info("환불 없이 예약 취소 - paymentId={}", payment.getPaymentId());
        }

        return new CancelPreparation(refund.getId(), calc);
    }

    //[TX 2] PG 환불 응답을 Refund 에 기록하고, 성공이면 결제 예약을 마무리한다.
    public PaymentSyncResult applyRefundResult(String paymentId, Long refundId, PortOneCancellation cancellation) {
        //항상 Payment -> Refund 순서로 잠근다
        Payment payment = lockPayment(paymentId);

        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new PaymentException(ErrorCode.REFUND_NOT_FOUND));

        //웹훅 / 재동기화가 먼저 반영했는지
        if(!refund.isInProgress()) return resultOf(refund);

        if(cancellation.isFailed()){
            failRefund(payment, refund, cancellation.id(), "PG 환불 요청 실패");
            log.error("PG 환불 요청 실패 응답 - refundId={}, paymentId={}", refundId, paymentId);

            return PaymentSyncResult.REFUND_FAILED;
        }

        if (cancellation.isRequested()) {
            refund.pend(cancellation.id(), cancellation.pgCancellationId());
            return PaymentSyncResult.REFUND_PENDING;
        }

        succeedRefund(refund, cancellation);
        payment.syncCancelled(payment.getCancelledAmount() + cancellation.totalAmount(), refund.coversRemaining());
        settleReservation(payment, cancellation.totalAmount(), null);
        return PaymentSyncResult.REFUNDED;
    }

    //PG 가 요청을 거절(4xx) - 돈이 나가지 않았으므로 FAILED 로 닫아 다시 취소할 수 있게 한다
    public void failRefund(String paymentId, Long refundId, String message) {
        Payment payment = lockPayment(paymentId);
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new PaymentException(ErrorCode.REFUND_NOT_FOUND));
        if(refund.isInProgress())
            failRefund(payment, refund, null, message);
    }

    private void failRefund(Payment payment, Refund refund, String cancellationId, String message) {
        refund.fail(cancellationId, message);
        if (!RefundOrigin.AUTO_REFUND.contains(refund.getOrigin())) {
            return;
        }

        payment.recordAutoRefundFailure(LocalDateTime.now(clock));
        if (payment.isAutoRefundExhausted()) {
            log.error("자동 환불 재시도 횟수 초과 - 수동 확인 필요 - paymentId={}", payment.getPaymentId());
        }
    }

    //[자동 환불 TX] 금액 불일치 / 만료 예약 결제 - PG 호출 전에 Refund 를 남긴다
    //웹훅과 /confirm 이 동시에 와도 Payment 잠금 아래에서 확인하므로 하나만 만들어진다
    public Optional<Long> requestSystemRefund(PortOnePaymentSnapshot snapshot, RefundOrigin origin, String reason) {
        Payment payment = lockPayment(snapshot.paymentId());

        //금액 불일치 결제는 확정될 일이 없다 - 환불 결과를 기다리지 않고 가예약을 만료시켜 재고와 dedupKey 를 바로 돌려준다
        //Refund 저장과 같은 트랜잭션에서 처리해, 중간에 서버가 죽어도 만료됐는데 환불 기록은 없는 상태가 생기지 않는다
        if (origin == RefundOrigin.AMOUNT_MISMATCH) {
            reservationHoldService.expireHold(payment.getReservation().getId());
        }

        //만료 스케쥴러에서 이미 환불 프로세스가 진행되었는지 확인
        if (payment.isRefundSettled() || refundRepository.existsByPaymentAndStatusIn(payment, RefundStatus.IN_PROGRESS)) {
            return Optional.empty();
        }

        //자동 환불 실패 후 백오프 시간이 지났는지 확인
        LocalDateTime now = LocalDateTime.now(clock);
        if(!payment.canRequestAutoRefund(now)){
            return Optional.empty();
        }
        payment.startAutoRefund();
        Refund refund = Refund.ofSystem(payment, origin, snapshot.paidTotal(), reason, LocalDateTime.now(clock));
        return Optional.of(refundRepository.save(refund).getId());
    }

    //PG 결제 기록의 취소 내역을 Refund 와 맞춘다 - 웹훅 / syncPayment / 재동기화 공통 (기존 applyCancelled 대체)
    public PaymentSyncResult applyPgCancellations(PortOnePaymentSnapshot snapshot) {
        Payment payment = paymentRepository.findByPaymentIdForUpdate(snapshot.paymentId()).orElse(null);

        if (payment == null) {
            log.warn("DB에 없는 결제 건 취소 시도 - paymentId={}", snapshot.paymentId());
            return PaymentSyncResult.UNKNOWN_PAYMENT;
        }

        Optional<Refund> requested = refundRepository.findFirstByPaymentAndStatus(payment, RefundStatus.REQUESTED);
        long newlyRefunded = 0;

        //한 Refund 에 PG 기록이 여러 건일 수 있어 성공 -> 접수 -> 실패 순으로 맞춘다
        List<PortOneCancellation> cancellations = snapshot.cancellations().stream()
                .sorted(Comparator.comparingInt(this::matchPriority))
                .toList();

        for (PortOneCancellation c : cancellations) {
            //cancellationId 로 먼저 찾고, 응답이 유실돼 ID 를 모르면 REQUESTED Refund 와 맞춘다
            Optional<Refund> matched = refundRepository.findByCancellationId(c.id())
                    .or(() -> requested.filter(r -> isSameRequest(r, c)));

            if (c.isFailed()) {
                matched.filter(Refund::isInProgress)
                        .ifPresent(r -> failRefund(payment, r, c.id(), "PG 취소 처리 실패"));
                continue;
            }

            if (c.isRequested()) {
                matched.filter(Refund::isInProgress)
                        .ifPresent(r -> r.pend(c.id(), c.pgCancellationId()));
                continue;
            }

            if(!c.isSucceeded()) continue;

            //그럼에도 없으면 우리 서버를 거치지 않은 취소로 간주한다(PortOne 콘솔 등)
            Refund refund = matched.orElseGet(() -> refundRepository.save(
                    Refund.ofExternal(payment, (int) c.totalAmount(), c.reason(), LocalDateTime.now(clock))
            ));

            if (refund.isInProgress()) {
                succeedRefund(refund, c);
                newlyRefunded += c.totalAmount();
            }

        }

        //이미 모두 반영된 상태 - 웹훅 재전송, 응답 처리와 웹훅의 경합
        if(newlyRefunded == 0) return resultOf(payment);

        payment.syncCancelled(snapshot.refundTotal(), snapshot.isFullyCancelled());
        settleReservation(payment, newlyRefunded, null);
        log.info("PG 취소 내역 반영 - paymentId={}, amount={}", snapshot.paymentId(), newlyRefunded);
        return PaymentSyncResult.REFUNDED;
    }

    private int matchPriority(PortOneCancellation c) {
        if(c.isSucceeded()) return 0;
        if(c.isRequested()) return 1;
        return 2;
    }

    //REQUESTED 인지 다시 확인한다 - 같은 반복문에서 이미 PENDING 이 됐으면 또 맞추면 안 된다
    private boolean isSameRequest(Refund refund, PortOneCancellation c){
        return refund.getStatus() == RefundStatus.REQUESTED
                && refund.getRequestedAmount() == c.totalAmount()
                && !toLocalDateTIme(c.requestedAt()).isBefore(refund.getRequestedAt().minus(MATCH_MARGIN));
    }

    @Transactional(readOnly = true)
    public Optional<RefundCommand> findRefundInProgress(Long refundId) {
        return refundRepository.findWithPaymentById(refundId)
                .filter(Refund::isInProgress)
                .map(RefundCommand::from);
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


    private LocalDateTime toLocalDateTIme(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, clock.getZone());
    }

    private void succeedRefund(Refund refund, PortOneCancellation cancellation) {
        //요청한 금액과 PG 실제 취소액이 다르면 반드시 인지해야 한다.
        if (cancellation.totalAmount() != refund.getRequestedAmount()) {
            log.error("환불 금액 불일치 - refundId={}, expected={}, actual={}", refund.getId(), refund.getRequestedAmount(), cancellation.totalAmount());
        }

        Instant cancelledAt = cancellation.cancelledAt() != null ? cancellation.cancelledAt() : clock.instant();
        refund.succeed(cancellation.id(), cancellation.pgCancellationId(), (int) cancellation.totalAmount(),
                toLocalDateTIme(cancelledAt), cancellation.receiptUrl());
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
            case PAID -> PaymentSyncResult.ALREADY_PAID;
            case REFUND, PARTIAL_REFUND, FORFEITED -> PaymentSyncResult.REFUNDED;
            case FAILED -> PaymentSyncResult.FAILED;
            case READY -> PaymentSyncResult.NOT_PAID_YET;
        };
    }

    private PaymentSyncResult resultOf(Refund refund) {
        return switch (refund.getStatus()) {
            case SUCCEEDED, NO_REFUND -> PaymentSyncResult.REFUNDED;
            case REQUESTED, PENDING -> PaymentSyncResult.REFUND_PENDING;
            case FAILED -> PaymentSyncResult.REFUND_FAILED;
        };
    }


    //결제가 환불/위약금으로 끝났을 때 예약 쪽 마무리 - 재고 복구 + 예약 취소 + 알림
    //이미 취소된 예약이면 (추가 부분 환불 등) 아무것도 하지 않는다
    private void settleReservation(Payment payment, long refundAmount, NotificationType type) {
        Reservation reservation = lockReservation(payment);
        ReservationStatus before = reservation.getStatus();
        if(before == ReservationStatus.CANCELLED) return;

        //환불 로직 도중에 만료 처리 스케쥴러가 재고를 풀었을 수 있다
        if(reservation.holdsInventory()){
            roomInventoryService.increaseRoomInventoryCount(reservation.getCheckDateTime(), reservation.getRoom());
        }
        reservation.cancelReservation();
        notificationService.notify(reservation, type != null ? type : cancelTypeOf(before), refundAmount);
    }

    private Payment lockPayment(String paymentId) {
        return paymentRepository.findByPaymentIdForUpdate(paymentId)
                .orElseThrow(() -> new PaymentException(ErrorCode.PAYMENT_NOT_FOUND));
    }
}
