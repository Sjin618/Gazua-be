package com.acc.gazua.domain.notification.dto;

import com.acc.gazua.domain.notification.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationGetResponse(
        Long notificationId,
        String content,
        NotificationType type,
        Long reservationId,
        boolean isRead,
        LocalDateTime createdAt
) {
}
