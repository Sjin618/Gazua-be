package com.acc.gazua.domain.notification.controller;

import com.acc.gazua.domain.notification.dto.NotificationGetResponse;
import com.acc.gazua.domain.notification.service.NotificationService;
import com.acc.gazua.global.dto.ApiResponse;
import com.acc.gazua.global.dto.CursorPage;
import com.acc.gazua.global.security.details.CustomUserDetails;
import com.acc.gazua.global.sse.SseEmitterRegistry;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    private final SseEmitterRegistry sseEmitterRegistry;

    //사용자의 알림 조회 (최근 31일에 발생한 알림만)
    @GetMapping
    public ResponseEntity<ApiResponse<CursorPage<NotificationGetResponse>>> getNotification(@RequestParam(required = false) String cursor,
                                                                                            @RequestParam(defaultValue = "10") @Min(1) @Max(30) int size,
                                                                                            @AuthenticationPrincipal CustomUserDetails user) {

        return ResponseEntity.ok(ApiResponse.success(notificationService.getNotifications(user.getUserId(), cursor, size), "알림 목록 조회 성공"));
    }

    //안 읽은 알림 개수 조회
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadNotificationCount(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getUnreadCount(user.getUserId()), "안 읽은 알림 개수 조회 성공"));
    }

    //특정 알림 읽음 처리
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> readNotification(@AuthenticationPrincipal CustomUserDetails user,
                                                              @PathVariable("id") Long id) {
        notificationService.readNotification(id, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success("알림 읽음 성공"));
    }

    //모두 읽음 버튼 클릭 시 모두 읽음 처리
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> readAllNotifications(@AuthenticationPrincipal CustomUserDetails user,
                                                                  @RequestParam Long lastId){
        notificationService.readAllNotification(user.getUserId(), lastId);
        return ResponseEntity.ok(ApiResponse.success("알림 전체 읽음 성공"));
    }

    //알림 실시간 구독 - DB 를 건드리지 않음
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal CustomUserDetails user,
                                HttpServletResponse response) {
        response.setHeader("X-Accel-Buffering", "no"); //nginx 가 응답을 모아서 보내지 않도록
        return sseEmitterRegistry.connect(user.getUserId());
    }

}
