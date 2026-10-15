package com.xodud1202.springbackend.security.news;

import com.fasterxml.jackson.databind.*;
import com.xodud1202.springbackend.config.SecurityConfig;
import com.xodud1202.springbackend.controller.news.*;
import com.xodud1202.springbackend.service.*;
import com.xodud1202.springbackend.security.JwtTokenProvider;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.oauth2.server.authorization.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.util.UriComponentsBuilder;
import org.h2.jdbcx.JdbcDataSource;
import javax.sql.DataSource;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;

// 실제 SAS와 Spring Resource Server를 사용하며 DB·자격·서비스는 모두 격리합니다.
@SpringBootTest(classes=NewsOAuthFlowTests.App.class,properties={
    "spring.config.location=optional:classpath:/news-oauth-isolated.properties",
    "news.oauth.enabled=true","news.daily.enabled=true"})
@AutoConfigureMockMvc(print=org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
class NewsOAuthFlowTests {
    static final String OWNER="gpt_news_insert", CLIENT="isolated-client", RESOURCE="https://news.test/api/news/mcp";
    static final String REDIRECT="https://chatgpt.com/connector/oauth/isolated-fixture";
    static final String PASSWORD=UUID.randomUUID().toString(), SECRET=UUID.randomUUID().toString();
    static Path secretFile;
    static final String USER_HASH=new BCryptPasswordEncoder().encode(PASSWORD);
    @DynamicPropertySource static void settings(DynamicPropertyRegistry p) throws Exception {
        secretFile=Files.createTempFile("news-oauth-client-test-",".hash");
        Files.writeString(secretFile,new BCryptPasswordEncoder(12).encode(SECRET));
        p.add("news.oauth.issuer",()->"https://news.test"); p.add("news.oauth.resource",()->RESOURCE);
        p.add("news.oauth.username",()->OWNER); p.add("news.oauth.owner-usr-no",()->3);
        p.add("news.oauth.client-id",()->CLIENT); p.add("news.oauth.client-secret-hash-file",()->secretFile.toString());
        p.add("news.oauth.redirect-uri",()->REDIRECT);
    }
    @AfterAll static void cleanup() throws Exception { Files.deleteIfExists(secretFile); }
    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class,HibernateJpaAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class,MybatisAutoConfiguration.class})
    @Import({SecurityConfig.class,NewsOAuthConfiguration.class,NewsOAuthServerConfiguration.class,NewsOAuthController.class,NewsMcpController.class})
    static class App {
        // 운영 UserMapper의 실제 SELECT를 격리 H2에서 실행합니다.
        @Bean com.xodud1202.springbackend.mapper.UserMapper userMapper(DataSource ds) throws Exception {
            var factory=new org.mybatis.spring.SqlSessionFactoryBean(); factory.setDataSource(ds);
            var config=new org.apache.ibatis.session.Configuration();
            config.addMapper(com.xodud1202.springbackend.mapper.UserMapper.class);
            factory.setConfiguration(config);
            return new org.mybatis.spring.SqlSessionTemplate(factory.getObject()).getMapper(com.xodud1202.springbackend.mapper.UserMapper.class);
        }
        @Bean DataSource dataSource() {
            JdbcDataSource ds=new JdbcDataSource(); ds.setURL("jdbc:h2:mem:oauth-"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1");
            new ResourceDatabasePopulator(
                new ClassPathResource("org/springframework/security/oauth2/server/authorization/oauth2-authorization-schema.sql"),
                new ClassPathResource("org/springframework/security/oauth2/server/authorization/oauth2-authorization-consent-schema.sql")).execute(ds);
            JdbcTemplate jdbc=new JdbcTemplate(ds);
            jdbc.execute("CREATE TABLE NEWS_OAUTH_LOGIN_GUARD(ID INT PRIMARY KEY, FAILURES INT NOT NULL, LOCKED_UNTIL TIMESTAMP NULL, ENABLED BOOLEAN NOT NULL, TOKEN_VERSION BIGINT NOT NULL)");
            jdbc.execute("INSERT INTO NEWS_OAUTH_LOGIN_GUARD VALUES(1,0,NULL,TRUE,1)");
            jdbc.execute("CREATE TABLE NEWS_OAUTH_LOCK(ID INT PRIMARY KEY)");
            jdbc.execute("INSERT INTO NEWS_OAUTH_LOCK VALUES(1)");
            jdbc.execute("CREATE TABLE NEWS_OAUTH_REFRESH_HISTORY(TOKEN_HASH VARCHAR(64) PRIMARY KEY, AUTHORIZATION_ID VARCHAR(100) NOT NULL, REGISTERED_CLIENT_ID VARCHAR(100) NOT NULL, USED_AT TIMESTAMP NOT NULL)");
            jdbc.execute("CREATE SCHEMA eshop");
            jdbc.execute("CREATE TABLE eshop.USER_BASE(USR_NO BIGINT PRIMARY KEY,LOGIN_ID VARCHAR(100),PWD VARCHAR(100),USR_STAT_CD VARCHAR(2),REG_DT TIMESTAMP,UDT_DT TIMESTAMP)");
            return ds;
        }
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired DataSource ds;
    @Autowired OAuth2AuthorizationService authorizations;
    @MockBean JwtTokenProvider siteTokens;
    @MockBean CustomUserDetailService siteUsers;
    @MockBean NewsDailyService daily;
    @MockBean NewsSourceService source;
    @BeforeEach void reset() {
        JdbcTemplate jdbc=new JdbcTemplate(ds);
        jdbc.update("DELETE FROM eshop.USER_BASE");
        jdbc.update("INSERT INTO eshop.USER_BASE VALUES(3,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",OWNER,USER_HASH,"02");
        jdbc.update("DELETE FROM NEWS_OAUTH_REFRESH_HISTORY");
        jdbc.update("DELETE FROM oauth2_authorization");
        jdbc.update("DELETE FROM oauth2_authorization_consent");
        jdbc.update("UPDATE NEWS_OAUTH_LOGIN_GUARD SET FAILURES=0, LOCKED_UNTIL=NULL, ENABLED=TRUE, TOKEN_VERSION=1");
    }
    // 실제 로그인 CSRF와 전용 컨텍스트를 사용합니다.
    MockHttpSession login() throws Exception {
        MvcResult form=mvc.perform(get("/oauth2/news/login").secure(true)).andExpect(status().isOk()).andReturn();
        CsrfToken csrf=(CsrfToken)form.getRequest().getAttribute(CsrfToken.class.getName());
        MockHttpSession session=(MockHttpSession)form.getRequest().getSession();
        MvcResult result=mvc.perform(post("/oauth2/news/login").secure(true).session(session)
            .formField(csrf.getParameterName(),csrf.getToken()).formField("username",OWNER).formField("password",PASSWORD))
            .andExpect(status().is3xxRedirection()).andReturn();
        session=(MockHttpSession)result.getRequest().getSession();
        assertThat(session.getAttribute("NEWS_OAUTH_SECURITY_CONTEXT")).isNotNull();
        assertThat(session.getAttribute("SPRING_SECURITY_CONTEXT")).isNull();
        return session;
    }
    String verifier() { return UUID.randomUUID().toString()+UUID.randomUUID(); }
    String challenge(String verifier) throws Exception {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
    }
    // 실제 승인 페이지에서 state와 CSRF를 받아 authorization code를 얻습니다.
    String code(String verifier,String scopes) throws Exception {
        MockHttpSession session=login();
        MvcResult request=mvc.perform(get("https://localhost/oauth2/news/authorize").servletPath("/oauth2/news/authorize")
            .secure(true).session(session).queryParam("response_type","code").queryParam("client_id",CLIENT)
            .queryParam("redirect_uri",REDIRECT).queryParam("scope",scopes).queryParam("state","client-state")
            .queryParam("resource",RESOURCE).queryParam("code_challenge",challenge(verifier)).queryParam("code_challenge_method","S256"))
            .andReturn();
        assertThat(request.getResponse().getStatus()).withFailMessage(request.getResponse().getContentAsString()).isBetween(300,399);
        String consent=request.getResponse().getRedirectedUrl();
        assertThat(consent).contains("/oauth2/news/consent");
        var query=UriComponentsBuilder.fromUriString(consent).build().getQueryParams();
        MvcResult page=mvc.perform(get("/oauth2/news/consent").secure(true).session(session)
            .param("client_id",CLIENT).param("state",java.net.URLDecoder.decode(query.getFirst("state"),StandardCharsets.UTF_8)).param("scope",scopes))
            .andExpect(status().isOk()).andReturn();
        CsrfToken csrf=(CsrfToken)page.getRequest().getAttribute(CsrfToken.class.getName());
        MvcResult approved=mvc.perform(post("/oauth2/news/authorize").servletPath("/oauth2/news/authorize")
            .secure(true).session(session).formField("client_id",CLIENT).formField("state",java.net.URLDecoder.decode(query.getFirst("state"),StandardCharsets.UTF_8))
            .formField("scope",scopes.split(" ")).formField(csrf.getParameterName(),csrf.getToken()))
            .andReturn();
        assertThat(approved.getResponse().getStatus()).withFailMessage("consent status %s error %s",approved.getResponse().getStatus(),approved.getResponse().getErrorMessage()).isBetween(300,399);
        String redirect=approved.getResponse().getRedirectedUrl();
        assertThat(redirect).startsWith(REDIRECT+"?");
        return java.net.URLDecoder.decode(UriComponentsBuilder.fromUriString(redirect).build().getQueryParams().getFirst("code"),StandardCharsets.UTF_8);
    }
    ResultActions exchange(String code,String verifier,String resource) throws Exception {
        return mvc.perform(post("/oauth2/news/token").servletPath("/oauth2/news/token").secure(true)
            .formField("grant_type","authorization_code").formField("client_id",CLIENT).formField("client_secret",SECRET)
            .formField("code",code).formField("redirect_uri",REDIRECT).formField("code_verifier",verifier).formField("resource",resource));
    }
    JsonNode tokens(String scopes) throws Exception {
        String verifier=verifier();
        var response=exchange(code(verifier,scopes),verifier,RESOURCE).andReturn().getResponse();
        JsonNode result=json.readTree(response.getContentAsString());
        assertThat(response.getStatus()).withFailMessage("token status %s error %s",response.getStatus(),result.path("error").asText()).isEqualTo(200);
        return result;
    }
    ResultActions rpc(String token,String body) throws Exception {
        return mvc.perform(post("/api/news/mcp").secure(true).header("Authorization","Bearer "+token)
            .header("MCP-Protocol-Version","2025-06-18").header("Accept","application/json, text/event-stream")
            .contentType("application/json").content(body));
    }
    @Test void discoveryAndChallengeDoNotRequireSiteLogin() throws Exception {
        mvc.perform(get("/.well-known/oauth-protected-resource/api/news/mcp").secure(true)).andExpect(status().isOk())
            .andExpect(jsonPath("$.resource").value(RESOURCE));
        mvc.perform(get("/.well-known/oauth-authorization-server").secure(true)).andExpect(status().isOk())
            .andExpect(jsonPath("$.code_challenge_methods_supported[0]").value("S256"));
        mvc.perform(post("/api/news/mcp").contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized()).andExpect(header().string("WWW-Authenticate",org.hamcrest.Matchers.containsString("resource_metadata")));
        verifyNoInteractions(siteTokens,siteUsers,daily,source);
    }
    @Test void completeFlowPreservesActorAndRefreshRotationAndRevokesWholeGrant() throws Exception {
        JsonNode token=tokens("news.read news.write");
        rpc(token.path("access_token").asText(),"{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}")
            .andExpect(status().isOk()).andExpect(jsonPath("$.result.tools.length()").value(5));
        rpc(token.path("access_token").asText(),"{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"news_save_status\",\"arguments\":{\"yyyymmdd\":\"20261015\",\"requestId\":\"request-test\"}}}")
            .andExpect(status().isOk());
        verify(daily).status("20261015","request-test",3L);
        MvcResult refreshed=refresh(token.path("refresh_token").asText()).andExpect(status().isOk()).andReturn();
        JsonNode next=json.readTree(refreshed.getResponse().getContentAsString());
        assertThat(next.path("refresh_token").asText()).isNotEqualTo(token.path("refresh_token").asText());
        mvc.perform(post("/oauth2/news/revoke").servletPath("/oauth2/news/revoke").secure(true)
            .formField("client_id",CLIENT).formField("client_secret",SECRET).formField("token",next.path("refresh_token").asText()))
            .andExpect(status().isOk());
        rpc(next.path("access_token").asText(),"{}").andExpect(status().isUnauthorized());
        refresh(next.path("refresh_token").asText()).andExpect(status().isBadRequest());
        verifyNoInteractions(siteTokens,siteUsers);
    }
    ResultActions refresh(String token) throws Exception {
        return mvc.perform(post("/oauth2/news/token").servletPath("/oauth2/news/token").secure(true)
            .formField("grant_type","refresh_token").formField("refresh_token",token).formField("client_id",CLIENT)
            .formField("client_secret",SECRET).formField("resource",RESOURCE));
    }
    @Test void readOnlyScopeCannotWriteAndCodeIsSingleUse() throws Exception {
        String verifier=verifier(),code=code(verifier,"news.read");
        String access=json.readTree(exchange(code,verifier,RESOURCE).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("access_token").asText();
        rpc(access,"{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"news_summary_save\",\"arguments\":{}}}")
            .andExpect(status().isForbidden());
        verifyNoInteractions(daily);
        exchange(code,verifier,RESOURCE).andExpect(status().isBadRequest());
    }
    @Test void wrongResourceVerifierAndRedirectAreRejected() throws Exception {
        String verifier=verifier(),code=code(verifier,"news.read");
        exchange(code,verifier,"https://other.test").andExpect(status().isBadRequest());
        exchange(code,verifier+"wrong",RESOURCE).andExpect(status().isBadRequest());
        mvc.perform(get("https://localhost/oauth2/news/authorize").servletPath("/oauth2/news/authorize").secure(true)
            .queryParam("resource",RESOURCE).queryParam("redirect_uri","https://attacker.test").queryParam("code_challenge",challenge(verifier))
            .queryParam("code_challenge_method","S256")).andExpect(status().isBadRequest());
        mvc.perform(get("https://localhost/oauth2/news/authorize").servletPath("/oauth2/news/authorize").secure(true)
            .queryParam("resource",RESOURCE).queryParam("redirect_uri",REDIRECT).queryParam("code_challenge",challenge(verifier))
            .queryParam("code_challenge_method","plain")).andExpect(status().isBadRequest());
    }
    @Test void loginAndConsentCsrfAndSiteSessionAreIsolated() throws Exception {
        mvc.perform(post("/oauth2/news/login").secure(true).formField("username",OWNER).formField("password",PASSWORD)).andExpect(status().isForbidden());
        MockHttpSession session=login();
        mvc.perform(post("/oauth2/news/authorize").servletPath("/oauth2/news/authorize").secure(true)
            .session(session).formField("client_id",CLIENT).formField("state","fake")).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/test").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/api/news/mcp").session(session).contentType("application/json").content("{}")).andExpect(status().isUnauthorized());
    }
    @Test void expiredGrantInvalidatesTokens() throws Exception {
        JsonNode token=tokens("news.read");
        String access=token.path("access_token").asText();
        var grant=authorizations.findByToken(access,OAuth2TokenType.ACCESS_TOKEN);
        authorizations.save(OAuth2Authorization.from(grant).attribute("news_idle_until",Long.toString(Instant.now().minusSeconds(1).getEpochSecond())).build());
        rpc(access,"{}").andExpect(status().isUnauthorized());
        refresh(token.path("refresh_token").asText()).andExpect(status().isBadRequest());
    }
    @Test void fiveFailuresLockEvenCorrectPassword() throws Exception {
        MvcResult form=mvc.perform(get("/oauth2/news/login").secure(true)).andReturn();
        CsrfToken csrf=(CsrfToken)form.getRequest().getAttribute(CsrfToken.class.getName());
        MockHttpSession session=(MockHttpSession)form.getRequest().getSession();
        for(int i=0;i<5;i++) mvc.perform(post("/oauth2/news/login").secure(true).session(session)
            .formField(csrf.getParameterName(),csrf.getToken()).formField("username","other-user").formField("password","wrong"))
            .andExpect(redirectedUrl("/oauth2/news/login?error"));
        mvc.perform(post("/oauth2/news/login").secure(true).session(session)
            .formField(csrf.getParameterName(),csrf.getToken()).formField("username",OWNER).formField("password",PASSWORD))
            .andExpect(redirectedUrl("/oauth2/news/login?error"));
        assertThat(new JdbcTemplate(ds).queryForObject("SELECT FAILURES FROM NEWS_OAUTH_LOGIN_GUARD WHERE ID=1",Integer.class)).isEqualTo(5);
    }

    // 서로 다른 발급 대상과 만료된 access token은 저장되어 있어도 사용할 수 없습니다.
    @Test void wrongAudienceAndExpiredAccessAreRejected() throws Exception {
        JsonNode tokens=tokens("news.read");
        String access=tokens.path("access_token").asText();
        OAuth2Authorization grant=authorizations.findByToken(access,OAuth2TokenType.ACCESS_TOKEN);
        Map<String,Object> claims=new HashMap<>(grant.getAccessToken().getClaims());
        claims.put("aud",new ArrayList<>(List.of("https://other.test")));
        authorizations.save(OAuth2Authorization.from(grant).token(grant.getAccessToken().getToken(),
            metadata->metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME,claims)).build());
        rpc(access,"{}").andExpect(status().isUnauthorized());
        var expired=new org.springframework.security.oauth2.core.OAuth2AccessToken(
            org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER,access,
            Instant.now().minusSeconds(60),Instant.now().minusSeconds(1),Set.of("news.read"));
        authorizations.save(OAuth2Authorization.from(grant).token(expired,metadata->metadata.putAll(grant.getAccessToken().getMetadata())).build());
        rpc(access,"{}").andExpect(status().isUnauthorized());
    }
    // USER_BASE 비밀번호 변경은 기존 access/refresh를 모두 무효화합니다.
    @Test void passwordRotationInvalidatesExistingGrant() throws Exception {
        JsonNode tokens=tokens("news.read");
        String original=USER_HASH;
        try {
            new JdbcTemplate(ds).update("UPDATE eshop.USER_BASE SET PWD=? WHERE USR_NO=3",new BCryptPasswordEncoder().encode(UUID.randomUUID().toString()));
            rpc(tokens.path("access_token").asText(),"{}").andExpect(status().isUnauthorized());
            refresh(tokens.path("refresh_token").asText()).andExpect(status().isBadRequest());
        } finally { new JdbcTemplate(ds).update("UPDATE eshop.USER_BASE SET PWD=? WHERE USR_NO=3",original); }
    }
    // 같은 code의 동시 교환은 DB 잠금으로 한 번만 성공합니다.
    @Test void concurrentCodeExchangeHasOneSuccessfulConsumer() throws Exception {
        String verifier=verifier(),code=code(verifier,"news.read");
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        var start=new java.util.concurrent.CountDownLatch(1);
        try {
            java.util.concurrent.Callable<Integer> task=()->{start.await();return exchange(code,verifier,RESOURCE).andReturn().getResponse().getStatus();};
            var first=pool.submit(task); var second=pool.submit(task); start.countDown();
            assertThat(List.of(first.get(20,java.util.concurrent.TimeUnit.SECONDS),second.get(20,java.util.concurrent.TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(200,400);
        } finally { pool.shutdownNow(); }
    }
    // 사이트 로그인 세션이나 JWT가 전용 OAuth 권한으로 대체되지 않습니다.
    @Test void siteAuthenticationCannotAuthorizeOrCallMcp() throws Exception {
        var user=new com.xodud1202.springbackend.entity.UserBaseEntity(); user.setUsrNo(1L); user.setLoginId("site-admin");
        var authentication=org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(user,null,user.getAuthorities());
        MockHttpSession session=new MockHttpSession();
        session.setAttribute("SPRING_SECURITY_CONTEXT",new org.springframework.security.core.context.SecurityContextImpl(authentication));
        mvc.perform(get("https://localhost/oauth2/news/authorize").servletPath("/oauth2/news/authorize").secure(true).session(session)
            .queryParam("response_type","code").queryParam("client_id",CLIENT).queryParam("redirect_uri",REDIRECT)
            .queryParam("scope","news.read").queryParam("resource",RESOURCE)
            .queryParam("code_challenge",challenge(verifier())).queryParam("code_challenge_method","S256"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("https://localhost/oauth2/news/login"));
        when(siteTokens.validateToken("site-token")).thenReturn(true);
        when(siteTokens.isAccessToken("site-token")).thenReturn(true);
        rpc("site-token","{}").andExpect(status().isUnauthorized());
        verifyNoInteractions(siteTokens,siteUsers,daily,source);
    }
    // 로그인과 동의 전에 HTTP 및 누락 설정을 거부합니다.
    @Test void httpAndMissingConfigurationAreRejected() throws Exception {
        mvc.perform(get("/oauth2/news/login")).andExpect(status().isBadRequest());
        var incomplete=new NewsOAuthProperties(true,"https://news.test",RESOURCE,OWNER,null,CLIENT,null,REDIRECT,null);
        assertThatThrownBy(incomplete::validate).isInstanceOf(IllegalStateException.class);
    }
    // 비밀 없는/잘못된 클라이언트와 승인 범위를 늘리는 갱신을 거부합니다.
    @Test void confidentialClientAndGrantedScopesCannotBeBypassed() throws Exception {
        mvc.perform(post("/oauth2/news/token").servletPath("/oauth2/news/token").secure(true)
            .formField("grant_type","authorization_code").formField("client_id",CLIENT)
            .formField("code","invalid").formField("resource",RESOURCE))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/oauth2/news/token").servletPath("/oauth2/news/token").secure(true)
            .formField("grant_type","authorization_code").formField("client_id",CLIENT).formField("client_secret","invalid")
            .formField("code","invalid").formField("resource",RESOURCE))
            .andExpect(status().isUnauthorized());
        JsonNode tokens=tokens("news.read");
        mvc.perform(post("/oauth2/news/token").servletPath("/oauth2/news/token").secure(true)
            .formField("grant_type","refresh_token").formField("client_id",CLIENT).formField("client_secret",SECRET)
            .formField("refresh_token",tokens.path("refresh_token").asText()).formField("resource",RESOURCE)
            .formField("scope","news.read news.write"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(daily,source);
    }
    // 응답 유실 후 이전 refresh 재시도는 승인 전체를 폐기합니다.
    @Test void reusedRefreshRevokesCurrentAccessAndRefresh() throws Exception {
        var initial=tokens("news.read");
        var next=json.readTree(refresh(initial.path("refresh_token").asText()).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        refresh(initial.path("refresh_token").asText()).andExpect(status().isBadRequest());
        rpc(next.path("access_token").asText(),"{}").andExpect(status().isUnauthorized());
        refresh(next.path("refresh_token").asText()).andExpect(status().isBadRequest());
        assertThat(new JdbcTemplate(ds).queryForObject("SELECT COUNT(*) FROM oauth2_authorization",Integer.class)).isZero();
        String stored=new JdbcTemplate(ds).queryForObject("SELECT TOKEN_HASH FROM NEWS_OAUTH_REFRESH_HISTORY",String.class);
        assertThat(stored).matches("[0-9a-f]{64}").isNotEqualTo(initial.path("refresh_token").asText());
    }
    // 두 서버 요청을 같은 DB 잠금으로 직렬화합니다. 두 번째 요청은 재사용이므로 첫 결과도 폐기됩니다.
    @Test void concurrentRefreshHasOneResponseButNoSurvivingFamily() throws Exception {
        var initial=tokens("news.read");
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        var start=new java.util.concurrent.CountDownLatch(1);
        try {
            java.util.concurrent.Callable<MvcResult> task=()->{start.await();return refresh(initial.path("refresh_token").asText()).andReturn();};
            var first=pool.submit(task); var second=pool.submit(task); start.countDown();
            var results=List.of(first.get(20,java.util.concurrent.TimeUnit.SECONDS),second.get(20,java.util.concurrent.TimeUnit.SECONDS));
            assertThat(results.stream().map(r->r.getResponse().getStatus()).toList()).containsExactlyInAnyOrder(200,400);
            var success=results.stream().filter(r->r.getResponse().getStatus()==200).findFirst().orElseThrow();
            var next=json.readTree(success.getResponse().getContentAsString());
            rpc(next.path("access_token").asText(),"{}").andExpect(status().isUnauthorized());
            refresh(next.path("refresh_token").asText()).andExpect(status().isBadRequest());
        } finally { pool.shutdownNow(); }
    }
    @Test void unauthenticatedReplayAndWrongResourceCannotRevokeFamily() throws Exception {
        var initial=tokens("news.read");
        var next=json.readTree(refresh(initial.path("refresh_token").asText()).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        mvc.perform(post("/oauth2/news/token").servletPath("/oauth2/news/token").secure(true)
            .formField("grant_type","refresh_token").formField("refresh_token",initial.path("refresh_token").asText())
            .formField("client_id",CLIENT).formField("client_secret","wrong").formField("resource",RESOURCE))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/oauth2/news/token").servletPath("/oauth2/news/token").secure(true)
            .formField("grant_type","refresh_token").formField("refresh_token",initial.path("refresh_token").asText())
            .formField("client_id",CLIENT).formField("client_secret",SECRET).formField("resource","https://other.test"))
            .andExpect(status().isBadRequest());
        rpc(next.path("access_token").asText(),"{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}")
            .andExpect(status().isOk());
    }
    @Test void disablingOwnerAndVersionChangeInvalidateExistingFamily() throws Exception {
        var initial=tokens("news.read");
        new JdbcTemplate(ds).update("UPDATE NEWS_OAUTH_LOGIN_GUARD SET ENABLED=FALSE, TOKEN_VERSION=TOKEN_VERSION+1 WHERE ID=1");
        rpc(initial.path("access_token").asText(),"{}").andExpect(status().isUnauthorized());
        refresh(initial.path("refresh_token").asText()).andExpect(status().isBadRequest());
        new JdbcTemplate(ds).update("UPDATE NEWS_OAUTH_LOGIN_GUARD SET ENABLED=TRUE WHERE ID=1");
        rpc(initial.path("access_token").asText(),"{}").andExpect(status().isUnauthorized());
    }
    @Autowired NewsOAuthProperties oauthSettings;
    @Autowired NewsOAuthOwnerAuthentication oauthOwner;
    @Autowired org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository clients;
    NewsOAuthAuthorizationService reopened(java.time.Clock clock) {
        return new NewsOAuthAuthorizationService(new JdbcOAuth2AuthorizationService(new JdbcTemplate(ds),clients),oauthOwner,
            new JdbcTemplate(ds),new org.springframework.transaction.support.TransactionTemplate(
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(ds)),oauthSettings,clock);
    }
    // 시간을 가속한 정책 테스트: 실제 JDBC 저장소를 매일 다시 열어도 40일간 회전 가능하며 이후 30일 미사용은 만료됩니다.
    @Test void rollingExpirySurvivesFortyDaysAndRepositoryRecreation() throws Exception {
        var initial=tokens("news.read");
        var grant=authorizations.findByToken(initial.path("access_token").asText(),OAuth2TokenType.ACCESS_TOKEN);
        Instant start=Instant.now();
        for (int day=1;day<=40;day++) {
            Instant now=start.plus(java.time.Duration.ofDays(day));
            var service=reopened(java.time.Clock.fixed(now,java.time.ZoneOffset.UTC));
            var current=service.findById(grant.getId());
            assertThat(current).isNotNull();
            service.save(OAuth2Authorization.from(current).refreshToken(new org.springframework.security.oauth2.core.OAuth2RefreshToken(
                UUID.randomUUID().toString(),now,now.plus(oauthSettings.refreshInactivityTimeout()))).build());
        }
        var afterForty=reopened(java.time.Clock.fixed(start.plus(java.time.Duration.ofDays(40)),java.time.ZoneOffset.UTC));
        assertThat(afterForty.findById(grant.getId())).isNotNull();
        assertThat(new JdbcTemplate(ds).queryForObject("SELECT COUNT(*) FROM NEWS_OAUTH_REFRESH_HISTORY",Integer.class)).isEqualTo(40);
        var idleExpired=reopened(java.time.Clock.fixed(start.plus(java.time.Duration.ofDays(70)),java.time.ZoneOffset.UTC));
        assertThat(idleExpired.findById(grant.getId())).isNull();
        assertThat(afterForty.findById(grant.getId())).isNull();
    }
    @Test void reuseHistorySurvivesRepositoryRestartAndIsBoundToClient() throws Exception {
        var initial=tokens("news.read");
        var next=json.readTree(refresh(initial.path("refresh_token").asText()).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        var service=reopened(java.time.Clock.systemUTC());
        var client=clients.findByClientId(CLIENT);
        var foreign=org.springframework.security.oauth2.server.authorization.client.RegisteredClient.from(client).id("other-client").build();
        try {
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken(
                    foreign,org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_POST,null));
            assertThat(service.findByToken(initial.path("refresh_token").asText(),OAuth2TokenType.REFRESH_TOKEN)).isNull();
            assertThat(service.findByToken(next.path("access_token").asText(),OAuth2TokenType.ACCESS_TOKEN)).isNotNull();
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken(
                    client,org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_POST,null));
            assertThat(service.findByToken(initial.path("refresh_token").asText(),OAuth2TokenType.REFRESH_TOKEN)).isNull();
            assertThat(service.findByToken(next.path("access_token").asText(),OAuth2TokenType.ACCESS_TOKEN)).isNull();
        } finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
    }


    @Test void rotationStorageFailureRollsBackHistoryAndCurrentTokens() throws Exception {
        var initial=tokens("news.read");
        var grant=authorizations.findByToken(initial.path("access_token").asText(),OAuth2TokenType.ACCESS_TOKEN);
        var delegate=spy(new JdbcOAuth2AuthorizationService(new JdbcTemplate(ds),clients));
        doAnswer(call->{
            call.callRealMethod();
            throw new IllegalStateException("isolated persistence failure");
        }).when(delegate).save(any(OAuth2Authorization.class));
        var service=new NewsOAuthAuthorizationService(delegate,oauthOwner,new JdbcTemplate(ds),
            new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.datasource.DataSourceTransactionManager(ds)),
            oauthSettings,java.time.Clock.systemUTC());
        var next=OAuth2Authorization.from(grant).refreshToken(new org.springframework.security.oauth2.core.OAuth2RefreshToken(
            UUID.randomUUID().toString(),Instant.now(),Instant.now().plus(java.time.Duration.ofDays(30)))).build();
        assertThatThrownBy(()->service.save(next)).isInstanceOf(IllegalStateException.class);
        assertThat(new JdbcTemplate(ds).queryForObject("SELECT COUNT(*) FROM NEWS_OAUTH_REFRESH_HISTORY",Integer.class)).isZero();
        assertThat(authorizations.findByToken(initial.path("refresh_token").asText(),OAuth2TokenType.REFRESH_TOKEN)).isNotNull();
        refresh(initial.path("refresh_token").asText()).andExpect(status().isOk());
    }
    @Test void oldRefreshCanExplicitlyRevokeCurrentFamily() throws Exception {
        var initial=tokens("news.read");
        var next=json.readTree(refresh(initial.path("refresh_token").asText()).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        mvc.perform(post("/oauth2/news/revoke").servletPath("/oauth2/news/revoke").secure(true)
            .formField("client_id",CLIENT).formField("client_secret",SECRET).formField("token",initial.path("refresh_token").asText()))
            .andExpect(status().isOk());
        rpc(next.path("access_token").asText(),"{}").andExpect(status().isUnauthorized());
        refresh(next.path("refresh_token").asText()).andExpect(status().isBadRequest());
    }
    @Test void configuredInactivityTimeoutControlsRollingDeadline() throws Exception {
        assertThat(oauthSettings.refreshInactivityTimeout()).isEqualTo(java.time.Duration.ofDays(30));
        assertThat(clients.findByClientId(CLIENT).getTokenSettings().getRefreshTokenTimeToLive()).isEqualTo(java.time.Duration.ofDays(30));
        assertThat(clients.findByClientId(CLIENT).getTokenSettings().getAccessTokenTimeToLive()).isEqualTo(java.time.Duration.ofMinutes(10));
        var custom=new NewsOAuthProperties(true,oauthSettings.issuer(),RESOURCE,OWNER,3L,
            CLIENT,secretFile.toString(),REDIRECT,java.time.Duration.ofDays(2));
        custom.validate();
        var initial=tokens("news.read");
        var grant=authorizations.findByToken(initial.path("access_token").asText(),OAuth2TokenType.ACCESS_TOKEN);
        Instant now=Instant.now();
        var jdbc=new JdbcTemplate(ds);
        var transactions=new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.datasource.DataSourceTransactionManager(ds));
        var service=new NewsOAuthAuthorizationService(new JdbcOAuth2AuthorizationService(jdbc,clients),oauthOwner,jdbc,transactions,custom,
            java.time.Clock.fixed(now,java.time.ZoneOffset.UTC));
        service.save(OAuth2Authorization.from(grant).refreshToken(new org.springframework.security.oauth2.core.OAuth2RefreshToken(
            UUID.randomUUID().toString(),now,now.plus(java.time.Duration.ofDays(2)))).build());
        String idle=service.findById(grant.getId()).getAttribute("news_idle_until");
        assertThat(Long.parseLong(idle)).isEqualTo(now.plus(java.time.Duration.ofDays(2)).getEpochSecond());
        assertThat(reopened(java.time.Clock.fixed(now.plus(java.time.Duration.ofDays(2)),java.time.ZoneOffset.UTC)).findById(grant.getId())).isNull();
    }


    // 사이트와 동일한 BCrypt 기본 비용(10)으로 로그인하고 검증된 3번만 저장 서비스로 전달합니다.
    @Test void verifiedAccountThreeIsPassedToSummarySave() throws Exception {
        var token=tokens("news.read news.write");
        assertThat(USER_HASH).startsWith("$2a$10$");
        var articles=List.of(Map.of("categoryNm","사회","dispOrd",1,"newsContent","격리 테스트"));
        String body=json.writeValueAsString(Map.of("jsonrpc","2.0","id",1,"method","tools/call","params",
            Map.of("name","news_summary_save","arguments",Map.of("yyyymmdd","20261015","requestId","actor-three",
                "sourceGeneratedAt","2026-10-15 08:00:00","articles",articles))));
        rpc(token.path("access_token").asText(),body).andExpect(status().isOk());
        verify(daily).save(any(com.xodud1202.springbackend.domain.news.NewsDailySaveRequest.class),eq(3L));
    }
    // 일반 회원의 올바른 비밀번호도 뉴스 OAuth 권한을 부여하지 않습니다.
    @Test void otherAccountWithCorrectPasswordCannotLogin() throws Exception {
        new JdbcTemplate(ds).update("INSERT INTO eshop.USER_BASE(USR_NO,LOGIN_ID,PWD,USR_STAT_CD) VALUES(?,?,?,?)",
            4,"other-user",USER_HASH,"02");
        rejectedLogin("other-user",PASSWORD);
        new JdbcTemplate(ds).update("DELETE FROM eshop.USER_BASE WHERE USR_NO=3");
        new JdbcTemplate(ds).update("UPDATE eshop.USER_BASE SET LOGIN_ID=? WHERE USR_NO=4",OWNER);
        rejectedLogin(OWNER,PASSWORD);
    }
    // 계정 상태 코드가 정상 02가 아니면 로그인과 기존 access/refresh를 모두 거부합니다.
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"01","03","99",""})
    @org.junit.jupiter.params.provider.NullSource
    void inactiveOrWithdrawnAccountRevokesGrant(String state) throws Exception {
        var token=tokens("news.read");
        new JdbcTemplate(ds).update("UPDATE eshop.USER_BASE SET USR_STAT_CD=? WHERE USR_NO=3",state);
        rejectedLogin(OWNER,PASSWORD);
        rpc(token.path("access_token").asText(),"{}").andExpect(status().isUnauthorized());
        refresh(token.path("refresh_token").asText()).andExpect(status().isBadRequest());
    }
    // 물리 삭제와 로그인 ID 변경도 이전 승인을 폐기합니다.
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans={true,false})
    void deletedOrRenamedAccountRevokesGrant(boolean delete) throws Exception {
        var token=tokens("news.read");
        var jdbc=new JdbcTemplate(ds);
        if(delete) jdbc.update("DELETE FROM eshop.USER_BASE WHERE USR_NO=3");
        else jdbc.update("UPDATE eshop.USER_BASE SET LOGIN_ID=? WHERE USR_NO=3","renamed-owner");
        rejectedLogin(OWNER,PASSWORD);
        rpc(token.path("access_token").asText(),"{}").andExpect(status().isUnauthorized());
        refresh(token.path("refresh_token").asText()).andExpect(status().isBadRequest());
    }
    // 실패 응답만 검사하며 폼이나 자격정보를 출력하지 않습니다.
    void rejectedLogin(String username,String password) throws Exception {
        var page=mvc.perform(get("/oauth2/news/login").secure(true)).andReturn();
        var csrf=(CsrfToken)page.getRequest().getAttribute(CsrfToken.class.getName());
        mvc.perform(post("/oauth2/news/login").secure(true).session((MockHttpSession)page.getRequest().getSession())
            .formField(csrf.getParameterName(),csrf.getToken()).formField("username",username).formField("password",password))
            .andExpect(redirectedUrl("/oauth2/news/login?error"));
    }
    // MyBatis의 1차/2차 캐시가 계정 변경을 숨기지 않는지 실제 매퍼로 확인합니다.
    @Autowired com.xodud1202.springbackend.mapper.UserMapper userMapper;
    @Test void accountLookupDoesNotReuseCachedState() {
        var jdbc=new JdbcTemplate(ds);
        new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.datasource.DataSourceTransactionManager(ds))
            .executeWithoutResult(tx->{
                assertThat(userMapper.findAuthenticationUserByUsrNo(3L).getUsrStatCd()).isEqualTo("02");
                jdbc.update("UPDATE eshop.USER_BASE SET USR_STAT_CD=? WHERE USR_NO=3","99");
                assertThat(userMapper.findAuthenticationUserByUsrNo(3L).getUsrStatCd()).isEqualTo("99");
            });
    }

}
