package com.xodud1202.springbackend.security.news;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.security.access.AccessDeniedException;

// MCP에서는 사이트 사용자 객체 대신 검증된 OAuth 신원만 사용합니다.
public final class NewsOAuthAccess {
    private NewsOAuthAccess() {}
    // 서버가 부여한 소유자 번호만 서비스에 전달합니다.
    public static Long actor(Authentication authentication) {
        requireScope(authentication, "news.read");
        Object actor = ((BearerTokenAuthentication) authentication).getTokenAttributes().get("news_actor");
        if (!(actor instanceof Number number) || number.longValue() <= 0)
            throw new AccessDeniedException("뉴스 OAuth 소유자가 필요합니다.");
        return number.longValue();
    }
    // 도구별 권한을 검증합니다.
    public static void requireScope(Authentication authentication, String scope) {
        if (!(authentication instanceof BearerTokenAuthentication) || !authentication.isAuthenticated()
                || authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("SCOPE_" + scope)))
            throw new AccessDeniedException("뉴스 OAuth 권한이 필요합니다.");
    }
}