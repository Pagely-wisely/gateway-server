package com.pagely.gateway.infrastructure.security;

import java.util.UUID;

/**
 * JWT 토큰에서 추출한 클레임 정보.
 *
 * <p>표준 클레임 sub는 유저 UUID, 커스텀 클레임 role은 권한 문자열.</p>
 */
public record JwtClaims(
        UUID userId,
        String role
) {
}
