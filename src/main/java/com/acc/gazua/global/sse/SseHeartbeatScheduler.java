package com.acc.gazua.global.sse;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SseHeartbeatScheduler {
    private final SseEmitterRegistry sseEmitterRegistry;

    //끊긴 연결을 찾아 정리하고, 프록시가 긴 시간 조용한 연결을 끊지 않게 한다.
    //이전 실행이 끝난 뒤 30초 뒤 재실행
    @Scheduled(fixedDelay = 30_000)
    public void heartbeat() {
        sseEmitterRegistry.sendHeartBeat();
    }
}
