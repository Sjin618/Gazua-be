package com.acc.gazua.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.LogicalType;

@Configuration
public class CommonConfig {

    @Bean
    public ObjectMapper tourObjectMapper(){
        return JsonMapper.builder()
                .withCoercionConfig(LogicalType.POJO,config->
                        config.setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull))
                .build();
    }
}
