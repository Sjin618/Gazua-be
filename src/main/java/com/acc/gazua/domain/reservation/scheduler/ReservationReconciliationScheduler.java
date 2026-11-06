package com.acc.gazua.domain.reservation.scheduler;

import com.acc.gazua.domain.payment.entity.RefundStatus;
import com.acc.gazua.domain.payment.repository.PaymentRepository;
import com.acc.gazua.domain.payment.repository.RefundRepository;
import com.acc.gazua.domain.payment.service.PaymentSyncResult;
import com.acc.gazua.domain.payment.service.PaymentSyncService;
import com.acc.gazua.domain.reservation.service.PaymentReconciler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * /cancel 에서 어떠한 오류로 처리되지 못한
 * REFUND_REQUESTED 상태의 결제(Payment)를 10분 주기로 PortOne 에 환불 요청을 한다.
 * PortOne 환불 처리 결과에 따라 DB와 동기화하거나, 수동 확인이 필요한 경우도 존재한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationReconciliationScheduler {
    private static final int BATCH_SIZE = 100;
    private static final Duration GRACE = Duration.ofMinutes(5); //진행 중인 취소와 겹치지 않도록
    private static final Duration GIVE_UP = Duration.ofHours(24); //이후로는 자동 재시도를 멈춘다.

    private final PaymentReconciler paymentReconciler;
    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentSyncService paymentSyncService;
    private final Clock clock;

    //PG 응답을 받지 못해 REQUESTED 로 굳은 환불을 마무리한다.
    //ReservationFacade.cancel 이 PG 호출에 실패해도 여기서 회수된다.
    //홀드 만료(0,5,10…분)와 겹치지 않도록 3,13,23…분에 실행한다.
    @Scheduled(cron = "0 3/10 * * * *", zone = "Asia/Seoul")
    public void cancelRefundRequested() {
        LocalDateTime now = LocalDateTime.now(clock);
        //REFUND_REQUESTED 상태의 Payment 조회
        List<Long> requested = refundRepository.findPendingRefundIds(Set.of(RefundStatus.REQUESTED), now.minus(GRACE), now.minus(GIVE_UP), PageRequest.of(0, BATCH_SIZE));
        List<Long> pending = refundRepository.findRefundIdsByStatusBefore(RefundStatus.PENDING, now.minus(GRACE), PageRequest.of(0, BATCH_SIZE));

        if (!pending.isEmpty()) {
            int done = 0;
            for (Long refundId : pending) {
                try {
                    if (paymentReconciler.reconcile(refundId)) done++;
                } catch (Exception e) {
                    log.error("환불 동기화 실패 - refundId={}", refundId, e);
                }
            }
            log.info("환불 재동기화 - 대상={}, 처리={}", pending.size(), done);
        }

        //REQUESTED 상태의  Payment를 PortOne 에 환불 요청 한다.
        if (!requested.isEmpty()) {
            int done = 0;
            for (Long refundId : requested) {
                try {
                    if (paymentReconciler.reconcile(refundId)) done++;
                } catch (Exception e) {
                    log.error("환불 재요청 실패 - refundId={}", refundId, e);
                }
            }
            log.info("환불 재동기화 - 대상={}, 처리={}", requested.size(), done);
        }
        alertStalled(now);
    }

    private void alertStalled(LocalDateTime now) {
        List<Long> stalled = refundRepository.findStalledRefundIds(RefundStatus.IN_PROGRESS, now.minus(GIVE_UP), PageRequest.of(0, BATCH_SIZE));
        if (!stalled.isEmpty()) {
            log.error("환불 24시간 이상 미완료 - 수동 확인 필요 - count={}, refundIds={}", stalled.size(), stalled);
        }
    }

    @Scheduled(cron = "0 8/10 * * * *", zone = "Asia/Seoul")
    public void retryFailedAutoRefunds() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<String> targets = paymentRepository.findAutoRefundRetryTargets(now, PageRequest.of(0, BATCH_SIZE));
        if(targets.isEmpty()) return;

        for (String paymentId : targets) {
            try {
                PaymentSyncResult result = paymentSyncService.syncPayment(paymentId);
                log.info("자동 환불 재시도 - paymentId={}, result={}", paymentId, result);
            } catch (Exception e) {
                log.error("자동 환불 재시도 실패 - paymentId={}", paymentId, e);
            }
        }
    }
}
