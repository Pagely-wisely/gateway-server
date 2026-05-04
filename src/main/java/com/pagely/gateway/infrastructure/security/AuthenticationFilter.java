package com.pagely.gateway.infrastructure.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * JWT 인증 필터 (Reactive GlobalFilter).
 *
 * <p><b>처리 흐름</b></p>
 * <ol>
 *   <li>요청 경로가 Public Path 매칭 → 통과</li>
 *   <li>Authorization 헤더 추출 (없으면 401)</li>
 *   <li>"Bearer " prefix 검증 (없으면 401)</li>
 *   <li>JWT 서명/만료 검증 (실패 시 401)</li>
 *   <li>클레임 추출하여 X-User-Id, X-User-Role 헤더 주입</li>
 *   <li>다운스트림 서비스로 전달</li>
 * </ol>
 *
 * <p><b>Public Path 매칭</b>은 Ant 패턴 (예: /api/v1/auth/**) 지원.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_ROLE = "X-User-Role";

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        // [1] Public Path 매칭 / 회원가입 경로 (POST /users/) → 인증 우회
        if (isPublicPath(path) ||
                "/api/v1/users".equals(path) && HttpMethod.POST.equals(method)) {
            return chain.filter(exchange);
        }

        // [2] Authorization 헤더 추출
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || authHeader.isBlank()) {
            return unauthorized(exchange, "MISSING_TOKEN", "인증 토큰이 필요합니다.");
        }

        // [3] Bearer prefix 검증
        if (!authHeader.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange, "INVALID_TOKEN_FORMAT",
                    "Authorization 헤더는 'Bearer {token}' 형식이어야 합니다.");
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            return unauthorized(exchange, "EMPTY_TOKEN", "토큰이 비어있습니다.");
        }

        // [4] JWT 검증
        if (!jwtTokenProvider.validateToken(token)) {
            return unauthorized(exchange, "INVALID_TOKEN",
                    "토큰이 유효하지 않거나 만료되었습니다.");
        }

        // [5] 클레임 추출
        JwtClaims claims;
        try {
            claims = jwtTokenProvider.parseClaims(token);
        } catch (JwtException | IllegalStateException e) {
            log.warn("JWT 파싱 실패 (검증은 통과): {}", e.getMessage());
            return unauthorized(exchange, "INVALID_TOKEN", "토큰 정보를 읽을 수 없습니다.");
        }

        // [6] 헤더 주입 후 다음 필터로
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(HEADER_USER_ID, claims.userId().toString())
                .header(HEADER_USER_ROLE, claims.role())
                .build();

        log.debug("인증 성공: userId={}, role={}, path={}",
                claims.userId(), claims.role(), path);

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    /**
     * 요청 경로가 Public Path 패턴 중 하나에 매칭되는지.
     */
    private boolean isPublicPath(String path) {
        List<String> publicPaths = jwtProperties.publicPaths();
        if (publicPaths == null || publicPaths.isEmpty()) {
            return false;
        }
        return publicPaths.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * 401 Unauthorized 응답 반환.
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String error, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "error", error,
                "message", message,
                "timestamp", LocalDateTime.now().toString()
        );

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("401 응답 직렬화 실패", e);
            DataBuffer fallback = response.bufferFactory()
                    .wrap("{\"error\":\"INTERNAL_ERROR\"}".getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(fallback));
        }
    }

    /**
     * 다른 GlobalFilter 보다 일찍 실행.
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
