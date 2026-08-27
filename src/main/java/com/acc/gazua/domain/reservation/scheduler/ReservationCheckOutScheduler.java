package com.acc.gazua.domain.reservation.scheduler;

import com.acc.gazua.domain.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationCheckOutScheduler {
    private final ReservationRepository reservationRepository;
    private final Clock clock;

    @Scheduled(cron = "0 0 * * * *",zone = "Asia/Seoul")
    @Transactional
    public void completeReservation(){
        int count = reservationRepository.completeReservation(LocalDateTime.now(clock));
        if(count > 0) log.info("체크아웃 완료 처리 - count={}", count);
    }
}
