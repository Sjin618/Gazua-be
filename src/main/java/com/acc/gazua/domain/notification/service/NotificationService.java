package com.acc.gazua.domain.notification.service;

import com.acc.gazua.domain.notification.dto.NotificationGetResponse;
import com.acc.gazua.domain.notification.entity.Notification;
import com.acc.gazua.domain.notification.entity.NotificationType;
import com.acc.gazua.domain.notification.event.NotificationCreatedEvent;
import com.acc.gazua.domain.notification.exception.NotificationException;
import com.acc.gazua.domain.notification.repository.NotificationRepository;
import com.acc.gazua.domain.reservation.entity.Reservation;
import com.acc.gazua.global.dto.CursorPage;
import com.acc.gazua.global.dto.CustomCursor;
import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.page.JsonCursorUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

    public static final int RETENTION_DAYS = 31;

    //사용자의 알림 목록 가져오기(최근 31일 까지만)
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

    //읽지 않은 알림 수 가져오기
    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId) {
        LocalDateTime since = LocalDateTime.now(clock).minusDays(RETENTION_DAYS);

        return notificationRepository.getUnreadCount(userId, since);
    }

    //특정 알림 읽음 처리
    public void readNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotificationException(ErrorCode.NOTIFICATION_NOT_FOUND));

        notification.read();
    }

    //모든 알림 읽음 처리
    public void readAllNotification(Long userId,Long lastId) {
        notificationRepository.updateReadAll(userId, lastId);
    }

    //호출한 쪽의 트랜잭션에 참여한다 - 예약 상태 변경과 함께 커밋/롤백된다
    public void notify(Reservation reservation, NotificationType type, long refundAmount) {
        String accName = reservation.getRoom().getAccommodation().getName();
        String content = switch(type) {
            case CONFIRM -> "[%s] 예약이 확정되었습니다.".formatted(accName);
            case REQUESTED_CANCEL -> "[%s] 예약이 취소되었습니다. 환불 금액 : %d원".formatted(accName,refundAmount);
            case AUTO_CANCEL -> "[%s] 결제 확인 중 문제가 있어 자동 환불 처리 되었습니다. 환불 금액 : %d원".formatted(accName, refundAmount);
            case NO_REFUND_FEE_CANCEL -> "[%s] 예약이 취소되었습니다. 환불 정책에 따라 환불 금액은 없습니다.".formatted(accName);
            case PAYMENT_FAILED -> "[%s] 결제에 실패했습니다. 다시 결제해주세요.".formatted(accName);
        };

        Notification notification = notificationRepository.save(Notification.builder()
                .user(reservation.getUser())
                .reservation(reservation)
                .type(type)
                .content(content)
                .build());

        eventPublisher.publishEvent(new NotificationCreatedEvent(
                reservation.getUser().getId(),NotificationGetResponse.from(notification)
        ));
    }
}
