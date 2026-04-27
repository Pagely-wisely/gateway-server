package com.pagley.gateway.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT 토큰 검증 및 클레임 파싱.
 *
 * <p>Gateway는 토큰의 <b>검증만</b> 책임. 토큰 생성은 User Service의 로그인 API가 담당.
 * 양쪽 서비스가 동일한 jwt.secret 환경변수를 공유해야 한다.</p>
 *
 * <p><b>HMAC HS256 사용</b></p>
 * <ul>
 *   <li>대칭키 방식 (생성/검증 동일 키)</li>
 *   <li>에 따라 키는 256비트(32바이트) 이상</li>
 *   <li>환경변수 JWT_SECRET 으로 주입 (운영)</li>
 * </ul>
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private static final String CLAIM_ROLE = "role";

    private final SecretKey secretKey;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret) {
        // UTF-8 바이트로 변환하여 HMAC 키 생성
        // 256비트 미만이면 jjwt 가 WeakKeyException 발생
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 토큰 유효성 검증 (서명, 만료, 형식).
     *
     * @return true = 정상 토큰, false = 검증 실패
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT 만료: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.debug("지원하지 않는 JWT 형식: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.debug("잘못된 JWT 형식: {}", e.getMessage());
        } catch (SignatureException e) {
            log.debug("JWT 서명 검증 실패: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.debug("JWT 클레임이 비어있음: {}", e.getMessage());
        } catch (JwtException e) {
            log.debug("JWT 검증 실패: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 토큰에서 클레임 추출.
     *
     * <p>호출 전 {@link #validateToken(String)}으로 검증 필수.
     * 검증 실패한 토큰을 파싱하면 예외 발생.</p>
     *
     * @return JwtClaims (userId UUID, role String)
     * @throws JwtException          파싱 실패 시
     * @throws IllegalStateException sub 클레임이 유효한 UUID 아닐 때
     */
    public JwtClaims parseClaims(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        UUID userId = parseUserId(claims.getSubject());
        String role = claims.get(CLAIM_ROLE, String.class);

        return new JwtClaims(userId, role);
    }

    private UUID parseUserId(String subject) {
        if (subject == null || subject.isBlank()) {
            throw new IllegalStateException("JWT sub 클레임이 비어있습니다.");
        }
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("JWT sub 클레임이 유효한 UUID가 아닙니다: " + subject, e);
        }
    }
}
