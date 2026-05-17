package com.pagely.gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Properties;
import javax.crypto.SecretKey;

/**
 * 개발용 JWT 토큰 생성 도구. main 메서드 실행하여 콘솔에 토큰 출력 → 테스트에 사용.
 *
 * <p>실제 로그인 API 구현 전까지 임시 사용. User Service 로그인 이슈
 * 완료되면 이 클래스 제거.</p>
 */
public class JwtTestTokenGenerator {

    public static void main(String[] args) {
        // application.yml의 기본 secret과 동일해야 함
        String secret = null;

        try {
            // 1. .env 파일 경로 지정 (프로젝트 루트 기준)
            // 만약 src/main/resources 아래에 있다면 아래 경로를 사용하세요.
            String envPath = "src/main/resources/.env";

            Properties props = new Properties();
            props.load(new FileInputStream(envPath));

            // 2. .env 파일에서 JWT_SECRET 키로 값을 가져옴
            secret = props.getProperty("JWT_SECRET");

        } catch (IOException e) {
            System.err.println(".env 파일을 찾을 수 없습니다: " + e.getMessage());
        }
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET 환경변수가 설정되지 않았습니다. " +
                            "예: export JWT_SECRET=$(openssl rand -base64 64)"
            );
        }
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        // 정상 토큰 (1시간 유효)
        String validToken = Jwts.builder()
                .subject("550e8400-e29b-41d4-a716-446655440000")
                .claim("role", "USER")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(key)
                .compact();

        // 만료 토큰
        String expiredToken = Jwts.builder()
                .subject("550e8400-e29b-41d4-a716-446655440000")
                .claim("role", "USER")
                .issuedAt(new Date(System.currentTimeMillis() - 7200_000))
                .expiration(new Date(System.currentTimeMillis() - 3600_000))  // 1시간 전 만료
                .signWith(key)
                .compact();

        System.out.println("=== 정상 토큰 ===");
        System.out.println(validToken);
        System.out.println();
        System.out.println("=== 만료 토큰 ===");
        System.out.println(expiredToken);
    }
}
