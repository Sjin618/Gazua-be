package com.acc.gazua.domain.notification.service;

import com.acc.gazua.domain.notification.dto.NotificationGetResponse;
import com.acc.gazua.domain.notification.entity.Notification;
import com.acc.gazua.domain.notification.exception.NotificationException;
import com.acc.gazua.domain.notification.repository.NotificationRepository;
import com.acc.gazua.global.dto.CursorPage;
import com.acc.gazua.global.dto.CustomCursor;
import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.page.JsonCursorUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final JsonCursorUtils jsonCursorUtils;

    private final Clock clock;

    private static final int RETENTION_DAYS = 31;

    @Transactional(readOnly = true)
    public CursorPage<NotificationGetResponse> getNotifications(Long userId, String cursor, int size) {
        CustomCursor customCursor = jsonCursorUtils.deserialize(cursor);
        Long cursorId = customCursor != null ? customCursor.cursorId() : Long.MAX_VALUE;
        LocalDateTime since = LocalDateTime.now(clock).minusDays(RETENTION_DAYS);

        Slice<NotificationGetResponse> notifications = notificationRepository.getNotifications(userId, since, cursorId, PageRequest.of(0, size));

        if (notifications.isEmpty()) {
            return CursorPage.empty();
        }

        List<NotificationGetResponse> list = notifications.getContent();
        String nextCursor = notifications.hasNext() ?
                jsonCursorUtils.serialize(new CustomCursor(null, list.get(list.size() - 1).notificationId()))
                : null;
        return new CursorPage<>(list, nextCursor, notifications.hasNext());
    }

    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId) {
        LocalDateTime since = LocalDateTime.now(clock).minusDays(RETENTION_DAYS);

        return notificationRepository.getUnreadCount(userId, since);
    }

    public void readNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotificationException(ErrorCode.NOTIFICATION_NOT_FOUND));

        notification.read();
    }

    public void readAllNotification(Long userId,Long lastId) {
        notificationRepository.updateReadAll(userId, lastId);
    }

}
