package com.xodud1202.springbackend.controller.news;

import com.xodud1202.springbackend.security.news.NewsOAuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;
import java.util.*;

// 전용 최초 로그인/동의 화면에는 비밀번호나 토큰을 다시 표시하지 않습니다.
@RestController
@ConditionalOnProperty(name="news.oauth.enabled",havingValue="true")
public class NewsOAuthController {
    private final NewsOAuthProperties settings;
    public NewsOAuthController(NewsOAuthProperties settings) { this.settings=settings; }
    // RFC9728 protected resource metadata입니다.
    @GetMapping("/.well-known/oauth-protected-resource/api/news/mcp")
    public Map<String,Object> metadata() {
        return Map.of("resource",settings.resource(),"authorization_servers",List.of(settings.issuer()),
            "scopes_supported",List.of("news.read","news.write"),"bearer_methods_supported",List.of("header"));
    }
    // 브라우저는 최초 승인 때만 이 로그인 화면을 사용합니다.
    @GetMapping(value="/oauth2/news/login",produces=MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> login(HttpServletRequest request) {
        return page("<h1>뉴스 연결 소유자 확인</h1><p>전용 뉴스 계정만 사용할 수 있습니다.</p>"
            +(request.getParameter("error")==null?"":"<p>인증에 실패했습니다. 반복 실패 시 15분 후 다시 시도하세요.</p>")
            +"<form method='post' action='/oauth2/news/login'>"+csrf(request)
            +"<label>계정 <input name='username' autocomplete='username' required></label>"
            +"<label>비밀번호 <input type='password' name='password' autocomplete='current-password' required maxlength='72'></label>"
            +"<button>로그인</button></form>");
    }
    // 요청 scope를 그대로 신뢰하지 않고 등록한 뉴스 권한만 표시합니다.
    @GetMapping(value="/oauth2/news/consent",produces=MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> consent(@RequestParam("client_id") String clientId,
            @RequestParam String state, @RequestParam(defaultValue="") String scope, HttpServletRequest request) {
        if (!settings.clientId().equals(clientId) || state.isBlank() || state.length()>512)
            return ResponseEntity.badRequest().build();
        StringBuilder content=new StringBuilder("<h1>ChatGPT 뉴스 연결 승인</h1><p>정상 갱신 시 연결이 연장되며, 미사용 기한 만료 또는 보안상 폐기 시 재연결이 필요합니다.</p>"
            +"<form method='post' action='/oauth2/news/authorize'>"+csrf(request)
            +hidden("client_id",clientId)+hidden("state",state));
        for(String value:scope.split(" ")) {
            if (Set.of("news.read","news.write").contains(value)) content.append("<label><input type='checkbox' name='scope' value='")
                .append(value).append("'>").append(value.equals("news.read")?"뉴스 원본·내 저장 상태 조회":"뉴스 요약·시도 저장").append("</label>");
        }
        return page(content+"<button>선택한 권한 승인</button></form><p>거절하려면 창을 닫으세요. 선택 없이 제출하면 승인이 거절됩니다.</p>");
    }
    // 직접 로그인만으로 권한을 발급하지 않습니다.
    @GetMapping(value="/oauth2/news/connected",produces=MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> connected() { return page("<p>ChatGPT에서 OAuth 연결을 시작하세요. 로그인만으로 접근 권한은 발급되지 않습니다.</p>"); }
    // CSRF 토큰을 HTML에 안전하게 삽입합니다.
    private String csrf(HttpServletRequest request) {
        CsrfToken token=(CsrfToken)request.getAttribute(CsrfToken.class.getName());
        return hidden(token.getParameterName(),token.getToken());
    }
    private String hidden(String name,String value) {
        return "<input type='hidden' name='"+HtmlUtils.htmlEscape(name)+"' value='"+HtmlUtils.htmlEscape(value)+"'>";
    }
    // 브라우저 및 중간 캐시의 인증 화면 저장을 방지합니다.
    private ResponseEntity<String> page(String content) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(
            "<!doctype html><html lang='ko'><meta charset='UTF-8'><meta name='viewport' content='width=device-width'>"
            +"<title>뉴스 OAuth 승인</title><body>"+content+"</body></html>");
    }
}