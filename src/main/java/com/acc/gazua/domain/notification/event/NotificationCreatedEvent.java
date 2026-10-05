package com.acc.gazua.domain.notification.event;

import com.acc.gazua.domain.notification.dto.NotificationGetResponse;

public record NotificationCreatedEvent(
        Long userId,
        NotificationGetResponse notification
) {
}
