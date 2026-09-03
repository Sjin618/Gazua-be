package com.acc.gazua.domain.reservation.scheduler;

import com.acc.gazua.domain.payment.repository.PaymentRepository;
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

    private final PaymentRepository paymentRepository;
    private final PaymentReconciler paymentReconciler;
    private final Clock clock;

    //PG 응답을 받지 못해 REFUND_REQUESTED 로 굳은 결제를 마무리한다.
    //ReservationFacade.cancel 이 PG 호출에 실패해도 여기서 회수된다.
    //홀드 만료(0,5,10…분)와 겹치지 않도록 3,13,23…분에 실행한다.
    @Scheduled(cron = "0 3/10 * * * *", zone = "Asia/Seoul")
    public void cancelRefundRequested(){
        LocalDateTime now = LocalDateTime.now(clock);
        //REFUND_REQUESTED 상태의 Payment 조회
        List<String> targets = paymentRepository.findPendingRefundPaymentIds(now.minus(GRACE), now.minus(GIVE_UP), PageRequest.of(0, BATCH_SIZE));

        //찾은 Payment를 환불시키고, PortOne과 동기화를 한다.
        if(!targets.isEmpty()){
            int done = 0;
            for (String paymentId : targets) {
                try{
                    if(paymentReconciler.reconcile(paymentId)) done++;
                }catch (Exception e){
                    log.error("환불 재동기화 실패 - paymentId={}", paymentId, e);
                }
            }
            log.info("환불 재동기화 - 대상={}, 처리={}", targets.size(), done);
        }
        alertStalled(now);
    }

    private void alertStalled(LocalDateTime now){
        List<String> stalled = paymentRepository.findStalledRefundPaymentIds(now.minus(GIVE_UP), PageRequest.of(0, BATCH_SIZE));
        if (!stalled.isEmpty()) {
            log.error("환불 24시간 이상 미완료 - 수동 확인 필요 - count={}, paymentIds={}", stalled.size(), stalled);
        }
    }
}
