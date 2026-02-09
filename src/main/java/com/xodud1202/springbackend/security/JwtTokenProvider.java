package com.xodud1202.springbackend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.security.Key;

// JWT 토큰 발급과 검증을 담당합니다.
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.access-token-expiration}")
    private long jwtAccessTokenExpirationInMs;
    
    @Value("${jwt.refresh-token-expiration}")
    private long jwtRefreshTokenExpirationInMs;
    
    private final UserDetailsService userDetailService;
    
    // 서명 키를 생성합니다.
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
    
    // 로그인 ID로 액세스 토큰을 생성합니다.
    public String generateAccessTokenByLoginId(String loginId) {
        // loginId로 UserDetails 객체를 로드
        UserDetails userDetails = userDetailService.loadUserByUsername(loginId);
        
        // 인증 객체를 생성합니다.
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, Collections.emptyList());
        // 액세스 토큰을 생성합니다.
        return generateAccessToken(authentication);
    }
    
    // Access Token 생성
    public String generateAccessToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtAccessTokenExpirationInMs);
        
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .claim("tokenType", "ACCESS")
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }
    
    // Refresh Token 생성
    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtRefreshTokenExpirationInMs);
        
        return Jwts.builder()
                .setSubject(username)
                .claim("tokenType", "REFRESH")
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }
    
    // 토큰에서 사용자명 추출
    public String getUsernameFromJWT(String token) {
        // 토큰의 클레임을 파싱합니다.
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        // 사용자명을 반환합니다.
        return claims.getSubject();
    }

    // 토큰에서 타입 클레임을 추출합니다.
    public String getTokenTypeFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        Object tokenType = claims.get("tokenType");
        return tokenType != null ? String.valueOf(tokenType) : null;
    }

    // Access Token 타입인지 확인합니다.
    public boolean isAccessToken(String token) {
        try {
            return "ACCESS".equalsIgnoreCase(getTokenTypeFromJWT(token));
        } catch (Exception e) {
            return false;
        }
    }

    // Refresh Token 타입인지 확인합니다.
    public boolean isRefreshToken(String token) {
        try {
            return "REFRESH".equalsIgnoreCase(getTokenTypeFromJWT(token));
        } catch (Exception e) {
            return false;
        }
    }
    
    // 토큰 유효성 검증
    public String validateCheckToken(String authToken) {
        try {
            // 토큰 파싱으로 유효성 검증을 수행합니다.
            this.getUsernameFromJWT(authToken);
//            Claims claims = Jwts
//                    .parserBuilder()
//                    .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
//                    .build()
//                    .parseClaimsJws(authToken)
//                    .getBody();
            return "OK";
        } catch (io.jsonwebtoken.security.SignatureException ex) {
            return "Invalid JWT signature";
        } catch (MalformedJwtException ex) {
            return "Invalid JWT token";
        } catch (ExpiredJwtException ex) {
            return "EXPIRED";
        } catch (UnsupportedJwtException ex) {
            return "Unsupported JWT token";
        } catch (IllegalArgumentException ex) {
            return "JWT claims string is empty";
        } catch (Exception e) {
            return "Error";
        }
    }
    
    // 토큰 유효성 검증
    public boolean validateToken(String authToken) {
        try {
            // 토큰 파싱으로 유효성을 확인합니다.
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(authToken);
            return true;
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty");
        }
        return false;
    }
}
