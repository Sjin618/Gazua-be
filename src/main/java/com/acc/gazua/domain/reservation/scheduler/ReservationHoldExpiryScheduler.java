package com.acc.gazua.domain.reservation.scheduler;

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
    private final Clock clock;

    //건별 트랜잭션 - 한 건이 실패해도 나머지가 함께 롤백되지 않는다.
    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Seoul")
    public void expireHolds() {
        List<Long> targets = reservationRepository.findExpiredHoldIds(LocalDateTime.now(clock), PageRequest.of(0, BATCH_SIZE));
        if(targets.isEmpty()) return;

        int expired = 0;
        for (Long id : targets) {
            try {
                if (reservationHoldService.expireHold(id)) expired++;
            } catch (Exception e) {
                log.error("가예약 만료 처리 실패 - reservationId={}", id, e);
            }
        }
        log.info("가예약 만료 처리 - 대상={}, 처리={}", targets.size(), expired);
    }
}
