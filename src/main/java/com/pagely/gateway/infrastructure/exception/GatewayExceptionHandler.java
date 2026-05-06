package com.pagely.gateway.infrastructure.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pagely.gateway.domain.exception.BusinessException;
import com.pagely.gateway.domain.exception.CommonErrorCode;
import com.pagely.gateway.domain.exception.ErrorCode;
import com.pagely.gateway.domain.exception.ErrorResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Gateway는 WebFlux 기반의 정적인 라이브러리 환경이기 때문에, 일반적인 MVC의 @RestControllerAdvice를 사용할 수 없습니다.
 *
 */
@Slf4j
@RequiredArgsConstructor
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        // 1. 에러 코드 추출
        ErrorCode errorCode = determineErrorCode(ex);

        // 2. 응답 설정
        response.setStatusCode(errorCode.getHttpStatus());
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // 3. 공통 에러 응답 바디
        Map<String, Object> apiResponse = Map.of(
                "success", false,
                "error", ErrorResponse.of(
                        errorCode.getCode(),
                        ex instanceof BusinessException ? ex.getMessage() : errorCode.getMessage()
                )
        );

        return response.writeWith(Mono.fromSupplier(() -> {
            try {
                byte[] bytes = objectMapper.writeValueAsBytes(apiResponse);
                return response.bufferFactory().wrap(bytes);
            } catch (Exception e) {
                log.error("Error writing response", e);
                return response.bufferFactory()
                        .wrap("{\"success\":false,\"error\":{\"code\":\"INTERNAL_SERVER_ERROR\"}}".getBytes());
            }
        }));
    }

    private ErrorCode determineErrorCode(Throwable ex) {
        if (ex instanceof BusinessException) {
            return ((BusinessException) ex).getErrorCode();
        }
        // Gateway 자체에서 발생하는 예외들에 대한 매핑
        return CommonErrorCode.INTERNAL_SERVER_ERROR;
    }
}
