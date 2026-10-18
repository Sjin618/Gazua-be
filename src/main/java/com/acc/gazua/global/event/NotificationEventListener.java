package com.acc.gazua.global.event;

import com.acc.gazua.domain.notification.event.NotificationCreatedEvent;
import com.acc.gazua.global.sse.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {
    private final SseEmitterRegistry sseEmitterRegistry;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationCreated(NotificationCreatedEvent event) {
        log.info("알림 발행 - userId={}, notificationId={}",
                event.userId(), event.notification().notificationId());
        sseEmitterRegistry.send(event.userId(), "notification", event.notification());
    }
}
