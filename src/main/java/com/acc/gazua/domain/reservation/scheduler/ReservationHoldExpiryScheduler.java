package com.acc.gazua.domain.reservation.scheduler;

import com.acc.gazua.domain.payment.service.PaymentSyncResult;
import com.acc.gazua.domain.payment.service.PaymentSyncService;
import com.acc.gazua.domain.reservation.dto.ExpiryReservation;
import com.acc.gazua.domain.reservation.repository.ReservationRepository;
import com.acc.gazua.domain.reservation.service.ReservationHoldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
/**
 * 가예약 만료 처리 스케쥴러
 * 시간이 지나도 결제되지 않은 가예약들을 만료 처리해서 재고를 풀어준다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationHoldExpiryScheduler {

    private static final int BATCH_SIZE = 200;

    private final ReservationRepository reservationRepository;
    private final ReservationHoldService reservationHoldService;
    private final PaymentSyncService paymentSyncService;
    private final Clock clock;

    //건별 트랜잭션 - 한 건이 실패해도 나머지가 함께 롤백되지 않는다.
    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Seoul")
    public void expireHolds() {
        List<ExpiryReservation> targets = reservationRepository.findExpiredHold(LocalDateTime.now(clock), PageRequest.of(0, BATCH_SIZE));
        if(targets.isEmpty()) return;

        int expired = 0;
        for (ExpiryReservation r : targets) {
            boolean isAvailable = true;
            try {
                PaymentSyncResult result = paymentSyncService.syncPayment(r.paymentId());
                switch(result){
                    case NOT_PAID_YET,FAILED -> {
                        if (reservationHoldService.expireHold(r.reservationId())) expired++;
                    }
                    case PORTONE_UNAVAILABLE -> {
                        log.warn("PG 확인 불가 - 만료 보류, 다음 주기 재시도 - paymentId={}", r.paymentId());
                        isAvailable = false;
                    }
                    case MANUAL_REVIEW_REQUIRED ->
                        log.error("PG 요청 실패 - 만료 보류, 확인 필요 - paymentId={}", r.paymentId());
                    default -> {}
                }
            } catch (Exception e) {
                log.error("가예약 만료 처리 실패 - reservationId={}", r.reservationId(), e);
            }
            if(!isAvailable){
                log.warn("PG 장애 발생 - 현재 배치 처리 종료");
                break;
            }
        }
        log.info("가예약 만료 처리 - 대상={}, 처리={}", targets.size(), expired);
    }
}
