package com.acc.gazua.global.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

//연결 저장소
@Slf4j
@Component
public class SseEmitterRegistry {
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final long timeout;

    //연결 수명을 액세스 토큰 수명(15분)과 맞춘다 - 재연결할 때 토큰을 다시 검사받게 하기 위함
    public SseEmitterRegistry(@Value("${JWT_EXPIRATION_ACCESS}") long timeout) {
        this.timeout = timeout;
    }

    public SseEmitter connect(Long userId) {
        SseEmitter emitter = new SseEmitter(timeout);

        //computeIfAbsent().add() 는 remove 와 경합할 수 있어서 compute 안에서 추가
        emitters.compute(userId,(id,list) ->{
            List<SseEmitter> target = (list != null) ? list : new CopyOnWriteArrayList<>();
            target.add(emitter);
            return target;
        });

        emitter.onCompletion(() -> remove(userId, emitter)); //정상 종료, 타임아웃, 에러 모두 마지막에 호출됨
        emitter.onTimeout(emitter::complete);
        emitter.onError(e -> remove(userId, emitter));

        //첫 이벤트를 보내야 응답이 시작된다 - 안 보내면 타임아웃 때 503
        sendTo(userId, emitter, SseEmitter.event().name("connected").data("ok"));
        return emitter;
    }


    public void send(Long userId, String eventName, Object data) {
        List<SseEmitter> list = emitters.get(userId);
        if(list == null) return; //접속 중이 아님 - 알림은 이미 DB 에 저장되어 있음
        for (SseEmitter emitter : list) {
            sendTo(userId, emitter, SseEmitter.event().name(eventName).data(data, MediaType.APPLICATION_JSON));
        }
    }

    public void sendHeartBeat() {
        emitters.forEach((userId, list) ->
                list.forEach(emitter -> sendTo(userId, emitter, SseEmitter.event().comment("heartbeat"))));
    }

    //탈퇴 시 해당 사용자의 모든 연결을 닫는다
    public void disconnectAll(Long userId) {
        List<SseEmitter> list = emitters.remove(userId);
        if(list != null) list.forEach(SseEmitter::complete);
    }

    private void sendTo(Long userId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException exception) {
            //클라이언트가 끊었거나 이미 종료된 연결 - 목록에서 제거
            remove(userId, emitter);
        }
    }

    private void remove(Long userId, SseEmitter emitter) {
        emitters.computeIfPresent(userId, (id,list) ->{
            list.remove(emitter);
            return list.isEmpty() ? null : list; //마지막 탭이 닫히면 키도 제거
        });
    }
}
