package com.acc.gazua.domain.notification.dto;

import com.acc.gazua.domain.notification.entity.Notification;
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
    public static NotificationGetResponse from(Notification n) {
        return new NotificationGetResponse(
                n.getId(),
                n.getContent(),
                n.getType(),
                n.getReservation() != null ? n.getReservation().getId() : null,
                n.isRead(),
                n.getCreatedAt()
        );
    }

}
