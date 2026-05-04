package com.pagely.gateway.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pagely.gateway.infrastructure.exception.GatewayExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;

@Configuration
public class ErrorConfig {

    @Bean
    @Primary  // 같은 타입의 빈이 여러 개일 때 이 녀석을 1순위로!
    @Order(-2) // 기본 핸들러(-1)보다 높은 순위
    public ErrorWebExceptionHandler gatewayExceptionHandler(ObjectMapper objectMapper) {
        return new GatewayExceptionHandler(objectMapper);
    }
}
