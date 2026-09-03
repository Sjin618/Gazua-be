package com.acc.gazua.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableScheduling
public class SchedulerConfig {

    @Bean
    public TaskScheduler taskScheduler(){
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(3);
        scheduler.setThreadNamePrefix("gazua-sched-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true); //종료 시 실행 중인 작업을 인터럽트하지 않는다.
        //위 설정만으로는 대기 시간이 0이다. 이 값이 있어야 실제로 기다린다.
        scheduler.setAwaitTerminationSeconds(20); //앱이 내려가고 스케쥴러 종료될 때까지 기다리는 시간
        return scheduler;
    }
}
