package com.xodud1202.springbackend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
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
    
    public String generateAccessTokenByLoginId(String loginId) {
        // loginId로 UserDetails 객체를 로드
        UserDetails userDetails = userDetailService.loadUserByUsername(loginId);
        
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, Collections.emptyList());
        return generateAccessToken(authentication);
    }
    
    // Access Token 생성
    public String generateAccessToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtAccessTokenExpirationInMs);
        
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
    }
    
    // Refresh Token 생성
    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtRefreshTokenExpirationInMs);
        
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
    }
    
    // 토큰에서 사용자명 추출
    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .getBody();
        
        return claims.getSubject();
    }
    
    // 토큰 유효성 검증
    public String validateCheckToken(String authToken) {
        try {
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
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken);
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