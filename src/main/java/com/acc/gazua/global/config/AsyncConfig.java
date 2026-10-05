package com.acc.gazua.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    //작업 도착 → 스레드 2개(core)가 처리
    //         → 2개가 모두 바쁘면 큐(200)에 대기
    //         → 큐까지 가득 차야 스레드를 4개(max)까지 늘림
    //         → 그래도 넘치면 거절 핸들러 실행
    @Bean(name = "notificationExecutor")
    public ThreadPoolTaskExecutor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("gazua-notify-");
        //큐까지 가득 차면 버리고 로그만 남긴다 - 알림 원본은 DB 에 있다
        executor.setRejectedExecutionHandler((task, pool) ->
                log.warn("알림 작업 거절 - 큐 포화, queueSize={}", pool.getQueue().size()));
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        return executor;
    }
}
