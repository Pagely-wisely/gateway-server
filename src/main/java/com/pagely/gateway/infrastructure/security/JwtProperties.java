package com.pagely.gateway.infrastructure.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 관련 설정 매핑.
 *
 * <p>application.yml 의 jwt 섹션을 자동 바인딩.</p>
 *
 * @param secret      JWT 검증용 비밀키 (HS256, 32바이트 이상)
 * @param publicPaths JWT 검증을 우회할 경로 패턴 목록
 */
@ConfigurationProperties(prefix = "jwt") // GatewayApplication - @EnableConfigurationProperties 추가
public record JwtProperties(
        String secret,
        List<String> publicPaths
) {
}
