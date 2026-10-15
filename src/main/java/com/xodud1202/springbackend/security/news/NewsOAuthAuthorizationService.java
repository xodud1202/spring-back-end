package com.xodud1202.springbackend.security.news;

import org.springframework.security.oauth2.server.authorization.*;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Clock;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.function.Supplier;

// 모든 승인 조회/변경을 DB 잠금으로 직렬화하고 사용한 refresh 원문 대신 해시만 보존합니다.
public final class NewsOAuthAuthorizationService implements OAuth2AuthorizationService {
    private final OAuth2AuthorizationService delegate;
    private final NewsOAuthOwnerAuthentication owner;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final NewsOAuthProperties settings;
    private final Clock clock;
    public NewsOAuthAuthorizationService(OAuth2AuthorizationService delegate, NewsOAuthOwnerAuthentication owner,
            JdbcTemplate jdbc, TransactionTemplate transactions, NewsOAuthProperties settings, Clock clock) {
        this.delegate=delegate; this.owner=owner; this.jdbc=jdbc; this.transactions=transactions;
        this.settings=settings; this.clock=clock;
    }
    private <T> T locked(Supplier<T> action) {
        return transactions.execute(status -> {
            jdbc.queryForObject("SELECT ID FROM NEWS_OAUTH_LOCK WHERE ID=1 FOR UPDATE",Integer.class);
            return action.get();
        });
    }
    @Override public void save(OAuth2Authorization authorization) {
        locked(() -> {
            if ((authorization.getAccessToken()!=null && authorization.getAccessToken().isInvalidated())
                    || (authorization.getRefreshToken()!=null && authorization.getRefreshToken().isInvalidated())) {
                delegate.remove(authorization); return null;
            }
            var previous=delegate.findById(authorization.getId());
            var builder=OAuth2Authorization.from(authorization);
            if (authorization.getAttribute("news_credential")==null)
                builder.attribute("news_credential",owner.credentialVersion());
            // 최초 refresh 발급과 성공한 회전만 미사용 기한을 연장합니다.
            var current=authorization.getRefreshToken();
            var old=previous==null?null:previous.getRefreshToken();
            if (current!=null && (old==null || !old.getToken().getTokenValue().equals(current.getToken().getTokenValue()))) {
                if (old!=null) jdbc.update(
                    "INSERT INTO NEWS_OAUTH_REFRESH_HISTORY(TOKEN_HASH,AUTHORIZATION_ID,REGISTERED_CLIENT_ID,USED_AT) VALUES(?,?,?,?)",
                    digest(old.getToken().getTokenValue()),authorization.getId(),authorization.getRegisteredClientId(),
                    java.sql.Timestamp.from(clock.instant()));
                builder.attribute("news_idle_until",Long.toString(clock.instant().plus(settings.refreshInactivityTimeout()).getEpochSecond()));
            }
            // 이전 버전의 절대 만료 속성은 새 승인의 수명에 적용하지 않습니다.
            builder.attributes(attributes->attributes.remove("news_deadline"));
            delegate.save(builder.build()); return null;
        });
    }
    @Override public void remove(OAuth2Authorization authorization) {
        locked(()->{ delegate.remove(authorization); return null; });
    }
    @Override public OAuth2Authorization findById(String id) { return locked(()->valid(delegate.findById(id))); }
    @Override public OAuth2Authorization findByToken(String token, OAuth2TokenType type) {
        return locked(()->{
            var found=delegate.findByToken(token,type);
            if (found!=null) return valid(found);
            // 클라이언트 인증 후 refresh/revoke 조회에서만 재사용 감지를 수행합니다.
            // 인증하지 않은 bearer 호출이나 다른 클라이언트는 승인을 폐기할 수 없습니다.
            if (type==null || OAuth2TokenType.REFRESH_TOKEN.equals(type)) {
                var principal=SecurityContextHolder.getContext().getAuthentication();
                if (principal instanceof OAuth2ClientAuthenticationToken client && client.isAuthenticated()
                        && client.getRegisteredClient()!=null) {
                    var matches=jdbc.queryForList(
                        "SELECT AUTHORIZATION_ID FROM NEWS_OAUTH_REFRESH_HISTORY WHERE TOKEN_HASH=? AND REGISTERED_CLIENT_ID=?",
                        digest(token),client.getRegisteredClient().getId());
                    for (var row:matches) {
                        var family=delegate.findById((String)row.get("AUTHORIZATION_ID"));
                        if (family!=null) delegate.remove(family);
                    }
                }
            }
            return null;
        });
    }
    private OAuth2Authorization valid(OAuth2Authorization auth) {
        if (auth==null) return null;
        String idle=auth.getAttribute("news_idle_until");
        boolean expired=auth.getRefreshToken()!=null && (idle==null || clock.instant().getEpochSecond()>=Long.parseLong(idle));
        if (!owner.enabled() || expired || !owner.credentialVersion().equals(auth.getAttribute("news_credential"))) {
            delegate.remove(auth); return null;
        }
        return auth;
    }
    private static String digest(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}