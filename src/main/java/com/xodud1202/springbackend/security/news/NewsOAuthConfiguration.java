package com.xodud1202.springbackend.security.news;

import java.time.Duration;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.server.authorization.*;
import org.springframework.security.oauth2.server.authorization.client.*;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.*;
import org.springframework.security.oauth2.server.authorization.token.*;
import org.springframework.security.oauth2.server.resource.introspection.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.*;
import org.springframework.security.web.csrf.*;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.transaction.support.TransactionTemplate;

// 사이트 체인보다 먼저 MCP 전용 경계를 적용합니다. 설정 누락 시 공개하지 않습니다.
@Configuration(proxyBeanMethods=false)
@EnableConfigurationProperties(NewsOAuthProperties.class)
public class NewsOAuthConfiguration {
    // 비활성화해도 기존 사이트 JWT로 MCP를 우회 호출할 수 없습니다.
    @Bean @Order(1)
    SecurityFilterChain newsResourceChain(HttpSecurity http, NewsOAuthProperties settings,
            ObjectProvider<OpaqueTokenIntrospector> introspectors) throws Exception {
        http.securityMatcher("/api/news/mcp", "/api/news/mcp/**")
            .securityContext(c->c.securityContextRepository(new NullSecurityContextRepository()))
            .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(c->c.disable()).csrf(c->c.disable());
        if (!settings.enabled()) {
            http.authorizeHttpRequests(a->a.anyRequest().denyAll())
                .exceptionHandling(e->e.authenticationEntryPoint((r,s,x)->s.setStatus(503))
                    .accessDeniedHandler((r,s,x)->s.setStatus(503)));
        } else {
            http.authorizeHttpRequests(a->a.anyRequest().hasAuthority("SCOPE_news.read"))
                .oauth2ResourceServer(o->o.opaqueToken(t->t.introspector(introspectors.getObject()))
                    .authenticationEntryPoint((r,s,e)->challenge(s,settings,401))
                    .accessDeniedHandler((r,s,e)->challenge(s,settings,403)));
        }
        return http.build();
    }
    // RFC9728의 발견 경로를 401/403에 제공합니다.
    private static void challenge(jakarta.servlet.http.HttpServletResponse response, NewsOAuthProperties settings, int status)
            throws java.io.IOException {
        response.setHeader("WWW-Authenticate","Bearer resource_metadata=\"" + settings.issuer()
                + "/.well-known/oauth-protected-resource/api/news/mcp\", scope=\"news.read news.write\""
                + (status==403 ? ", error=\"insufficient_scope\"" : ""));
        response.setStatus(status);
    }

}
