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

// 승인된 운영 설정이 모두 준비된 경우에만 표준 OAuth 서버를 구성합니다.
@Configuration(proxyBeanMethods=false)
@ConditionalOnProperty(name="news.oauth.enabled",havingValue="true")
public class NewsOAuthServerConfiguration {
        @Bean NewsOAuthOwnerAuthentication newsOwner(NewsOAuthProperties settings, DataSource dataSource, com.xodud1202.springbackend.mapper.UserMapper users, org.springframework.security.crypto.password.PasswordEncoder encoder) {
            settings.validate();
            // DDL을 실행하지 않고 필수 제어 행 준비 여부만 확인합니다.
            JdbcTemplate check=new JdbcTemplate(dataSource);
            if (check.queryForObject("SELECT COUNT(*) FROM NEWS_OAUTH_LOCK WHERE ID=1",Integer.class)!=1
                    || check.queryForObject("SELECT COUNT(*) FROM NEWS_OAUTH_LOGIN_GUARD WHERE ID=1",Integer.class)!=1)
                throw new IllegalStateException("뉴스 OAuth 제어 테이블 준비가 필요합니다.");
            return new NewsOAuthOwnerAuthentication(settings,new JdbcTemplate(dataSource),
                    new TransactionTemplate(new DataSourceTransactionManager(dataSource)),users,encoder);
        }
        @Bean RegisteredClientRepository newsClients(NewsOAuthProperties s) {
            // 클라이언트는 사전 등록 방식이며 임의 DCR/익명 권한 발급은 제공하지 않습니다.
            RegisteredClient client=RegisteredClient.withId("news-chatgpt").clientId(s.clientId())
                .clientSecret("{bcrypt}"+NewsOAuthProperties.hash(s.clientSecretHashFile()))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(s.redirectUri()).scope("news.read").scope("news.write")
                .clientSettings(ClientSettings.builder().requireProofKey(true).requireAuthorizationConsent(true).build())
                .tokenSettings(TokenSettings.builder().accessTokenFormat(OAuth2TokenFormat.REFERENCE)
                    .accessTokenTimeToLive(Duration.ofMinutes(10)).authorizationCodeTimeToLive(Duration.ofMinutes(2))
                    .refreshTokenTimeToLive(s.refreshInactivityTimeout()).reuseRefreshTokens(false).build()).build();
            return new InMemoryRegisteredClientRepository(client);
        }
        @Bean OAuth2AuthorizationService newsAuthorizations(DataSource ds, RegisteredClientRepository clients,
                NewsOAuthOwnerAuthentication owner, NewsOAuthProperties settings) {
            return new NewsOAuthAuthorizationService(new JdbcOAuth2AuthorizationService(new JdbcTemplate(ds),clients),owner,
                new JdbcTemplate(ds),new TransactionTemplate(new DataSourceTransactionManager(ds)),settings,java.time.Clock.systemUTC());
        }
        @Bean OAuth2AuthorizationConsentService newsConsent(DataSource ds, RegisteredClientRepository clients) {
            return new JdbcOAuth2AuthorizationConsentService(new JdbcTemplate(ds),clients);
        }
        @Bean AuthorizationServerSettings newsServerSettings(NewsOAuthProperties s) {
            return AuthorizationServerSettings.builder().issuer(s.issuer())
                .authorizationEndpoint("/oauth2/news/authorize").tokenEndpoint("/oauth2/news/token")
                .tokenRevocationEndpoint("/oauth2/news/revoke").tokenIntrospectionEndpoint("/oauth2/news/introspect").build();
        }
        @Bean OAuth2TokenGenerator<?> newsTokens(NewsOAuthProperties settings) {
            // 서명키 없이 표준 opaque token을 발급하고 필요한 claims는 서버 DB에만 저장합니다.
            OAuth2AccessTokenGenerator access=new OAuth2AccessTokenGenerator();
            access.setAccessTokenCustomizer(context->context.getClaims().audience(new ArrayList<>(List.of(settings.resource())))
                .claim("news_actor",settings.ownerUsrNo().toString()));
            return new DelegatingOAuth2TokenGenerator(access,new OAuth2RefreshTokenGenerator());
        }
        @Bean OpaqueTokenIntrospector newsIntrospector(OAuth2AuthorizationService service, NewsOAuthProperties settings) {
            return token -> {
                OAuth2Authorization grant=service.findByToken(token,OAuth2TokenType.ACCESS_TOKEN);
                if (grant==null || grant.getAccessToken()==null || !grant.getAccessToken().isActive()
                        || !settings.username().equals(grant.getPrincipalName())) throw new BadOpaqueTokenException("Invalid token");
                Map<String,Object> claims=grant.getAccessToken().getClaims();
                if (claims==null || !(claims.get("aud") instanceof Collection<?> audience)
                        || !audience.contains(settings.resource())
                        || !settings.ownerUsrNo().toString().equals(claims.get("news_actor"))) throw new BadOpaqueTokenException("Invalid token");
                var authorities=grant.getAccessToken().getToken().getScopes().stream()
                    .filter(scope->Set.of("news.read","news.write").contains(scope))
                    .<org.springframework.security.core.GrantedAuthority>map(scope->new SimpleGrantedAuthority("SCOPE_"+scope)).toList();
                Map<String,Object> attributes=new HashMap<>(claims);
                attributes.put("news_actor",settings.ownerUsrNo());
                return new DefaultOAuth2AuthenticatedPrincipal(settings.username(),attributes,authorities);
            };
        }
        @Bean @Order(2)
        SecurityFilterChain newsAuthorizationChain(HttpSecurity http, NewsOAuthProperties settings,
                NewsOAuthOwnerAuthentication owner, DataSource dataSource) throws Exception {
            // 사이트 AuthenticationManager로 실패 인증이 넘어가지 않도록 부모를 제거합니다.
            http.getSharedObject(org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder.class).parentAuthenticationManager(null);
            var server=OAuth2AuthorizationServerConfigurer.authorizationServer();
            var context=new HttpSessionSecurityContextRepository();
            context.setSpringSecurityContextKey("NEWS_OAUTH_SECURITY_CONTEXT");
            var csrf=new HttpSessionCsrfTokenRepository();
            csrf.setSessionAttributeName("NEWS_OAUTH_CSRF");
            var cache=new HttpSessionRequestCache();
            cache.setSessionAttrName("NEWS_OAUTH_SAVED_REQUEST");
            cache.setRequestMatcher(r->"GET".equals(r.getMethod()) && "/oauth2/news/authorize".equals(r.getServletPath()));
            http.securityMatcher("/oauth2/news/**","/.well-known/oauth-authorization-server",
                    "/.well-known/oauth-protected-resource/api/news/mcp")
                .securityContext(c->c.securityContextRepository(context))
                .authenticationProvider(new org.springframework.security.authentication.AuthenticationProvider() {
                    // Bean으로 노출하지 않아 사이트의 전역 인증 관리자에 등록되지 않습니다.
                    public org.springframework.security.core.Authentication authenticate(org.springframework.security.core.Authentication authentication) {
                        return owner.authenticate(authentication);
                    }
                    public boolean supports(Class<?> type) { return owner.supports(type); }
                })
                .requestCache(c->c.requestCache(cache))
                .sessionManagement(s->s.sessionFixation(f->f.changeSessionId()))
                .with(server,s->s.clientAuthentication(c->c.authenticationProviders(providers->providers.forEach(provider->{
                    // 사이트의 BCrypt 빈 설정과 독립적으로 표준 {bcrypt} 클라이언트 해시를 검증합니다.
                    if (provider instanceof org.springframework.security.oauth2.server.authorization.authentication.ClientSecretAuthenticationProvider secret)
                        secret.setPasswordEncoder(org.springframework.security.crypto.factory.PasswordEncoderFactories.createDelegatingPasswordEncoder());
                }))).authorizationEndpoint(a->a.consentPage("/oauth2/news/consent")))
                .authorizeHttpRequests(a->a.requestMatchers("/oauth2/news/login","/.well-known/**").permitAll()
                    .requestMatchers("/oauth2/news/authorize", "/oauth2/news/consent", "/oauth2/news/connected").hasAuthority("NEWS_OAUTH_OWNER")
                    .anyRequest().authenticated())
                .csrf(c->c.csrfTokenRepository(csrf).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .formLogin(f->f.loginPage("/oauth2/news/login").loginProcessingUrl("/oauth2/news/login")
                    .defaultSuccessUrl("/oauth2/news/connected",false).failureUrl("/oauth2/news/login?error"))
                .logout(l->l.disable())
                .exceptionHandling(e->e.authenticationEntryPoint((request,response,failure)->{
                    // 기계용 토큰 경로는 로그인 페이지로 보내지 않고 표준 인증 실패를 반환합니다.
                    if (Set.of("/oauth2/news/token","/oauth2/news/revoke","/oauth2/news/introspect").contains(request.getServletPath())) {
                        response.setStatus(401); response.setContentType("application/json");
                        response.setHeader("WWW-Authenticate","Basic realm=\"news-oauth\"");
                        response.getWriter().write("{\"error\":\"invalid_client\"}");
                    } else new LoginUrlAuthenticationEntryPoint("/oauth2/news/login").commence(request,response,failure);
                }))
                .headers(h->h.contentSecurityPolicy(c->c.policyDirectives("default-src 'none'; style-src 'unsafe-inline'; form-action 'self'; frame-ancestors 'none'; base-uri 'none'")));
            http.addFilterAfter(new NewsOAuthProtocolFilter(settings,owner,new JdbcTemplate(dataSource),
                    new TransactionTemplate(new DataSourceTransactionManager(dataSource))),CsrfFilter.class);
            return http.build();
        }
    }
