package com.xodud1202.springbackend.security.news;

import com.xodud1202.springbackend.entity.UserBaseEntity;
import com.xodud1202.springbackend.mapper.UserMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.HexFormat;

// 기존 사용자 저장소와 PasswordEncoder를 사용하되 뉴스 전용 3번 계정만 허용합니다.
public final class NewsOAuthOwnerAuthentication {
    private final NewsOAuthProperties settings;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final UserMapper users;
    private final PasswordEncoder encoder;
    // 사용자 조회는 공통 UserMapper에 두고 사이트 인증 매니저와 세션은 공유하지 않습니다.
    public NewsOAuthOwnerAuthentication(NewsOAuthProperties settings, JdbcTemplate jdbc, TransactionTemplate transactions,
            UserMapper users, PasswordEncoder encoder) {
        this.settings=settings; this.jdbc=jdbc; this.transactions=transactions; this.users=users; this.encoder=encoder;
    }
    // 기존 조회에서 정상 상태로 사용하는 02만 허용합니다. 99 탈퇴/기타/NULL/삭제는 모두 거부합니다.
    private boolean allowed(UserBaseEntity user) {
        return Long.valueOf(3).equals(settings.ownerUsrNo()) && user!=null && Long.valueOf(3).equals(user.getUsrNo())
            && settings.username().equals(user.getLoginId()) && "02".equals(user.getUsrStatCd())
            && user.getPwd()!=null && !user.getPwd().isBlank();
    }
    // DB에서 계정 존재/상태와 OAuth 전용 가드를 매 요청 확인합니다.
    public boolean enabled() {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT ENABLED FROM NEWS_OAUTH_LOGIN_GUARD WHERE ID=1",Boolean.class))
            && allowed(users.findAuthenticationUserByUsrNo(3L));
    }
    // 캐시하지 않은 현재 계정으로 기존 토큰과 로그인 세션의 자격을 확인합니다.
    public String credentialVersion() {
        var user=users.findAuthenticationUserByUsrNo(3L);
        return allowed(user)?version(user):"unavailable";
    }
    // 해시 원문은 승인 메타데이터에 저장하지 않고 계정 상태/식별자와 함께 지문만 만듭니다.
    private String version(UserBaseEntity user) {
        try {
            String value=user.getPwd()+"|"+user.getUsrNo()+"|"+user.getLoginId()+"|"+user.getUsrStatCd()
                +"|"+(user.getRegDt()==null?null:user.getRegDt().getTime())+"|"+(user.getUdtDt()==null?null:user.getUdtDt().getTime())+"|"+settings.resource()+"|"+settings.clientId()
                +"|"+jdbc.queryForObject("SELECT TOKEN_VERSION FROM NEWS_OAUTH_LOGIN_GUARD WHERE ID=1",Long.class)
                +"|"+NewsOAuthProperties.hash(settings.clientSecretHashFile());
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) { throw new IllegalStateException("인증 설정을 읽을 수 없습니다."); }
    }
    // 실패 제한은 유지하고, 검증한 비밀번호와 같은 계정 스냅샷의 지문으로 세션을 발급합니다.
    public Authentication authenticate(Authentication request) {
        String credential=transactions.execute(status->{
            var guard=jdbc.queryForMap("SELECT FAILURES, LOCKED_UNTIL, ENABLED FROM NEWS_OAUTH_LOGIN_GUARD WHERE ID=1 FOR UPDATE");
            if (!Boolean.TRUE.equals(jdbc.queryForObject("SELECT ENABLED FROM NEWS_OAUTH_LOGIN_GUARD WHERE ID=1",Boolean.class))) return null;
            Timestamp until=(Timestamp)guard.get("LOCKED_UNTIL");
            if(until!=null && until.toInstant().isAfter(Instant.now())) return null;
            var user=users.findAuthenticationUserByUsrNo(3L);
            String password=String.valueOf(request.getCredentials());
            boolean valid=allowed(user) && user.getLoginId().equals(request.getName())
                && password.getBytes(StandardCharsets.UTF_8).length<=72 && encoder.matches(password,user.getPwd());
            int failures=until==null?((Number)guard.get("FAILURES")).intValue():0;
            if(valid) {
                jdbc.update("UPDATE NEWS_OAUTH_LOGIN_GUARD SET FAILURES=0, LOCKED_UNTIL=NULL WHERE ID=1");
                return version(user);
            }
            failures++;
            jdbc.update("UPDATE NEWS_OAUTH_LOGIN_GUARD SET FAILURES=?, LOCKED_UNTIL=? WHERE ID=1",
                failures,failures>=5?Timestamp.from(Instant.now().plusSeconds(900)):null);
            return null;
        });
        if(credential==null) throw new BadCredentialsException("인증에 실패했습니다.");
        var principal=User.withUsername(settings.username()).password("")
            .authorities("NEWS_OAUTH_OWNER","CREDENTIAL_"+credential,"SESSION_UNTIL_"+Instant.now().plusSeconds(300).getEpochSecond()).build();
        return UsernamePasswordAuthenticationToken.authenticated(principal,null,principal.getAuthorities());
    }
    // 전용 체인의 ID/비밀번호 인증만 처리합니다.
    public boolean supports(Class<?> type) { return UsernamePasswordAuthenticationToken.class.isAssignableFrom(type); }
}