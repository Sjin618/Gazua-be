package com.acc.gazua.global.config;

import io.portone.sdk.server.webhook.WebhookVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.LogicalType;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Configuration
public class CommonConfig {

    @Bean
    public ObjectMapper tourObjectMapper(){
        return JsonMapper.builder()
                .withCoercionConfig(LogicalType.POJO,config->
                        config.setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull))
                .build();
    }

    @Bean
    public WebhookVerifier webhookVerifier(@Value("${WEBHOOK_SECRET}") String webhookSecret){
        return new WebhookVerifier(webhookSecret);
    }

    @Bean
    public DateTimeProvider auditingDateTimeProvider(Clock clock){
        return () -> Optional.of(LocalDateTime.now(clock));
    }

    @Bean
    public Clock clock(){
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}
