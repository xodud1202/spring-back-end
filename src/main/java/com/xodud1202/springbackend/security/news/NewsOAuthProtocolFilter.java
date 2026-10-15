package com.xodud1202.springbackend.security.news;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

// resource, PKCE, 승인 CSRF 및 여러 인스턴스의 토큰 재사용 경쟁을 제어합니다.
public final class NewsOAuthProtocolFilter extends OncePerRequestFilter {
    private final NewsOAuthProperties settings;
    private final NewsOAuthOwnerAuthentication owner;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    public NewsOAuthProtocolFilter(NewsOAuthProperties settings, NewsOAuthOwnerAuthentication owner,
            JdbcTemplate jdbc, TransactionTemplate transactions) {
        this.settings=settings; this.owner=owner; this.jdbc=jdbc; this.transactions=transactions;
    }
    // 프로토콜 입력은 정확히 한 번만 나타나는 값으로 제한합니다.
    private boolean exact(HttpServletRequest request, String key, String expected) {
        String[] values=request.getParameterValues(key);
        return values != null && values.length == 1 && expected.equals(values[0]);
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // 브라우저 세션 쿠키가 안전한 HTTPS 요청에서만 만들어지도록 합니다.
        if (!request.isSecure()) { response.setStatus(400); return; }
        String path=request.getServletPath();
        boolean authorize=path.equals("/oauth2/news/authorize");
        boolean token=path.equals("/oauth2/news/token");
        boolean revoke=path.equals("/oauth2/news/revoke");
        boolean initialAuthorize=authorize && (request.getMethod().equals("GET") || request.getParameter("response_type")!=null);
        if (initialAuthorize || token) {
            if (!exact(request,"resource",settings.resource())) { reject(response); return; }
        }
        if (initialAuthorize) {
            String challenge=request.getParameter("code_challenge");
            if (!exact(request,"code_challenge_method","S256") || challenge==null
                    || !challenge.matches("[A-Za-z0-9_-]{43}")
                    || !exact(request,"redirect_uri",settings.redirectUri())) { reject(response); return; }
        }
        if (authorize) {
            if (!owner.enabled()) { response.setStatus(403); return; }
            var sessionAuth=SecurityContextHolder.getContext().getAuthentication();
            if (sessionAuth != null && sessionAuth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("NEWS_OAUTH_OWNER"))) {
                boolean fresh=sessionAuth.getAuthorities().stream().filter(a->a.getAuthority().startsWith("SESSION_UNTIL_"))
                    .anyMatch(a->{try{return Long.parseLong(a.getAuthority().substring(14)) > java.time.Instant.now().getEpochSecond();}
                        catch(NumberFormatException ex){return false;}});
                if (!fresh) {
                    SecurityContextHolder.clearContext();
                    if (request.getSession(false)!=null) request.getSession(false).removeAttribute("NEWS_OAUTH_SECURITY_CONTEXT");
                    response.sendRedirect("/oauth2/news/login"); return;
                }
            }
            var auth=SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")
                    && (!auth.getName().equals(settings.username()) || auth.getAuthorities().stream()
                    .noneMatch(a->a.getAuthority().equals("CREDENTIAL_"+owner.credentialVersion())))) {
                response.setStatus(403); return;
            }
            // SAS의 프로토콜 CSRF 예외와 별개로 브라우저 동의 POST를 검증합니다.
            if (request.getMethod().equals("POST")) {
                CsrfToken csrf=(CsrfToken)request.getAttribute(CsrfToken.class.getName());
                String submitted=csrf == null ? null : request.getParameter(csrf.getParameterName());
                if (submitted==null || !MessageDigest.isEqual(csrf.getToken().getBytes(StandardCharsets.UTF_8),
                        submitted.getBytes(StandardCharsets.UTF_8))) { response.setStatus(403); return; }
            }
        }
        if ((token || revoke) && request.getMethod().equals("POST")) {
            // DB 행 잠금과 동일 트랜잭션 안에서 code/refresh 소비 및 철회를 처리합니다.
            var buffered=new org.springframework.web.util.ContentCachingResponseWrapper(response);
            try {
                transactions.executeWithoutResult(status -> {
                    jdbc.queryForObject("SELECT ID FROM NEWS_OAUTH_LOCK WHERE ID=1 FOR UPDATE",Integer.class);
                    try { chain.doFilter(request,buffered); }
                    catch (IOException | ServletException ex) { throw new IllegalStateException(ex); }
                });
                buffered.copyBodyToResponse();
            } catch (RuntimeException ex) {
                if (!response.isCommitted()) { response.reset(); response.setStatus(503); }
            }
            return;
        }
        chain.doFilter(request,response);
    }
    // 오류 응답에는 입력값, 토큰, 자격증명을 포함하지 않습니다.
    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(400); response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"invalid_request\"}");
    }
}