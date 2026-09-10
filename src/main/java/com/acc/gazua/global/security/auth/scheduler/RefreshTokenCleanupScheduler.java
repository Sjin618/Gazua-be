package com.acc.gazua.global.security.auth.scheduler;

import com.acc.gazua.global.security.auth.repository.RefreshTokenRepository;
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
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    /**
     * 만료된 토큰 정리
     * 만료된 토큰은 parseClaims 단계에서 EXPIRED_TOKEN 으로 먼저 걸러지므로
     * DB 기록을 지워도 재사용 감지 능력에는 영향이 없다.
     */
    @Scheduled(cron = "0 30 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void deleteExpiredTokens() {
        int deleted = refreshTokenRepository.deleteExpiredBefore(LocalDateTime.now(clock));
        log.info("만료된 refresh token 정리 - {}건", deleted);
    }
}
