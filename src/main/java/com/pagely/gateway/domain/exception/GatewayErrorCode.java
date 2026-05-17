package com.pagely.gateway.domain.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum GatewayErrorCode implements ErrorCode {
    INVALID_TOKEN_FORMAT("Authorization 헤더는 'Bearer {token}' 형식이어야 합니다.", HttpStatus.UNAUTHORIZED),
    EMPTY_TOKEN("토큰이 비어있습니다.", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("토큰이 유효하지 않거나 만료되었습니다.", HttpStatus.UNAUTHORIZED);
    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    GatewayErrorCode(String message, HttpStatus httpStatus) {
        this.code = this.name();
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
