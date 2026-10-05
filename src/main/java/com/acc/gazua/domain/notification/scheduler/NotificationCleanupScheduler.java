package com.acc.gazua.domain.notification.scheduler;

import com.acc.gazua.domain.notification.repository.NotificationRepository;
import com.acc.gazua.domain.notification.service.NotificationService;
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
public class NotificationCleanupScheduler {
    private final NotificationRepository notificationRepository;
    private final Clock clock;

    @Scheduled(cron = "0 47 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void deleteNotifications() {
        int deleted = notificationRepository.deleteCreatedBefore(LocalDateTime.now(clock).minusDays(NotificationService.RETENTION_DAYS));
        log.info("만료된 알림 제거 - {} 건", deleted);
    }
}
