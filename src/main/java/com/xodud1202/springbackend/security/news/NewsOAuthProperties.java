package com.xodud1202.springbackend.security.news;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.net.URI;
import java.nio.file.*;
import java.util.regex.Pattern;

// 전용 OAuth 설정만 읽으며 비밀값 자체는 설정에 보관하지 않습니다.
@ConfigurationProperties("news.oauth")
public record NewsOAuthProperties(boolean enabled, String issuer, String resource, String username,
        Long ownerUsrNo, String clientId, String clientSecretHashFile,
        String redirectUri, java.time.Duration refreshInactivityTimeout) {
    public NewsOAuthProperties {
        username = username == null ? "gpt_news_insert" : username;
        refreshInactivityTimeout = refreshInactivityTimeout == null ? java.time.Duration.ofDays(30) : refreshInactivityTimeout;
    }
    // 활성화 전 필수값과 HTTPS 경계를 검사합니다.
    public void validate() {
        https(issuer); https(resource); https(redirectUri);
        if (refreshInactivityTimeout.compareTo(java.time.Duration.ofMinutes(10)) < 0)
            throw new IllegalStateException("refresh inactivity timeout must be at least 10 minutes");
        if (!resource.equals(issuer + "/api/news/mcp") || issuer.endsWith("/")
                || !Pattern.matches("[A-Za-z0-9_-]{3,64}", username)
                || !Long.valueOf(3).equals(ownerUsrNo) || clientId == null || clientId.isBlank()
                || redirectUri.contains("*")) throw new IllegalStateException("뉴스 OAuth 설정이 올바르지 않습니다.");
        hash(clientSecretHashFile);
    }
    // 호스트 및 경로가 고정된 HTTPS 주소만 허용합니다.
    private static void https(String value) {
        try {
            URI uri = URI.create(value);
            if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getRawUserInfo() != null
                    || uri.getRawFragment() != null || uri.getRawQuery() != null) throw new IllegalArgumentException();
        } catch (Exception ex) { throw new IllegalStateException("뉴스 OAuth HTTPS 주소가 필요합니다."); }
    }
    // 외부 파일의 BCrypt 해시를 읽으며 오류에 경로나 내용을 노출하지 않습니다.
    public static String hash(String path) {
        try {
            Path file = Path.of(path);
            if (!file.isAbsolute() || Files.size(file) > 128) throw new IllegalArgumentException();
            String hash = Files.readString(file).strip();
            if (!hash.matches("\\$2[aby]\\$(1[2-6])\\$[./A-Za-z0-9]{53}")) throw new IllegalArgumentException();
            return hash;
        } catch (Exception ex) { throw new IllegalStateException("유효한 외부 BCrypt 해시 파일이 필요합니다."); }
    }
}