package com.xodud1202.springbackend.controller.news;

import com.fasterxml.jackson.databind.*;
import com.xodud1202.springbackend.domain.news.*;
import com.xodud1202.springbackend.entity.UserBaseEntity;
import com.xodud1202.springbackend.service.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NewsMcpControllerTests {
    private final ObjectMapper json=new ObjectMapper();
    private NewsDailyService daily;
    private NewsSourceService source;
    private MockMvc mvc;
    private org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication auth;

    @BeforeEach void setup() {
        daily=mock(NewsDailyService.class); source=mock(NewsSourceService.class);
        mvc=MockMvcBuilders.standaloneSetup(new NewsMcpController(daily,source,json,"https://allowed.example")).build();
        java.util.List<org.springframework.security.core.GrantedAuthority> authorities=List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_news.read"),
            new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_news.write"));
        var principal=new org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal("fixture",Map.of("news_actor",42L),authorities);
        auth=new org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication(principal,
            new org.springframework.security.oauth2.core.OAuth2AccessToken(org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER,"fixture",java.time.Instant.now(),java.time.Instant.now().plusSeconds(60)),authorities);
    }
    private ResultActions rpc(String body) throws Exception {
        return mvc.perform(post("/api/news/mcp").principal(auth).contentType("application/json")
                .header("Accept","application/json, text/event-stream").header("MCP-Protocol-Version","2025-06-18").content(body));
    }
    private String latestCall(String date) {
        return "{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"tools/call\",\"params\":{\"name\":\"news_daily_latest\",\"arguments\":{\"yyyymmdd\":\""+date+"\"}}}";
    }
    @Test void latestCompletedAndEmptyAreNormalResults() throws Exception {
        String today=java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        when(daily.latest(today)).thenReturn(new NewsDailyResult("COMPLETED",today,3,"manual-today","2026-10-15 17:30:00",
                List.of(new NewsDailyArticle("사회",1,"완료된 요약"))));
        var response=rpc(latestCall(today)).andExpect(jsonPath("$.result.isError").value(false)).andReturn();
        JsonNode result=json.readTree(json.readTree(response.getResponse().getContentAsString()).at("/result/content/0/text").asText());
        org.assertj.core.api.Assertions.assertThat(result.path("status").asText()).isEqualTo("COMPLETED");
        org.assertj.core.api.Assertions.assertThat(result.path("seq").asInt()).isEqualTo(3);
        when(daily.latest(today)).thenReturn(new NewsDailyResult("EMPTY",today,null,null,null,List.of()));
        response=rpc(latestCall(today)).andExpect(jsonPath("$.result.isError").value(false)).andReturn();
        result=json.readTree(json.readTree(response.getResponse().getContentAsString()).at("/result/content/0/text").asText());
        org.assertj.core.api.Assertions.assertThat(result.path("status").asText()).isEqualTo("EMPTY");
        org.assertj.core.api.Assertions.assertThat(result.path("articles").size()).isZero();
        verify(daily,times(2)).latest(today);
    }
    @Test void latestErrorsAreNeverEmptyAndDateValidationUsesRealService() throws Exception {
        var mapper=mock(com.xodud1202.springbackend.mapper.NewsDailyMapper.class);
        var real=new NewsDailyService(mapper,json,mock(org.springframework.transaction.PlatformTransactionManager.class));
        mvc=MockMvcBuilders.standaloneSetup(new NewsMcpController(real,source,json,"")).build();
        rpc(latestCall("20260230")).andExpect(jsonPath("$.result.isError").value(true));
        verifyNoInteractions(mapper);
        when(mapper.latest("20261015")).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("private-db-detail"));
        var response=rpc(latestCall("20261015")).andExpect(jsonPath("$.result.isError").value(true)).andReturn();
        org.assertj.core.api.Assertions.assertThat(response.getResponse().getContentAsString()).doesNotContain("EMPTY","private-db-detail");
        doThrow(new IllegalStateException("private-internal-detail")).when(mapper).latest("20261015");
        rpc(latestCall("20261015")).andExpect(jsonPath("$.result.isError").value(true));
    }
    @Test void latestSchemaIsReadOnlyAndRequiresDate() throws Exception {
        rpc("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}")
            .andExpect(jsonPath("$.result.tools[5].name").value("news_daily_latest"))
            .andExpect(jsonPath("$.result.tools[5].annotations.readOnlyHint").value(true))
            .andExpect(jsonPath("$.result.tools[5].inputSchema.required[0]").value("yyyymmdd"))
            .andExpect(jsonPath("$.result.tools[5].securitySchemes[0].scopes.length()").value(1))
            .andExpect(jsonPath("$.result.tools[5].securitySchemes[0].scopes[0]").value("news.read"));
        rpc("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/call\",\"params\":{\"name\":\"news_daily_latest\",\"arguments\":{}}}")
            .andExpect(jsonPath("$.result.isError").value(true));
        verifyNoInteractions(daily);
    }

    @Test void initializeListAndNotification() throws Exception {
        rpc("""
            {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"test","version":"1"}}}
            """).andExpect(status().isOk()).andExpect(jsonPath("$.result.protocolVersion").value("2025-06-18"));
        rpc("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}")
                .andExpect(jsonPath("$.result.tools.length()").value(6))
                .andExpect(jsonPath("$.result.tools[3].name").value("news_summary_save"));
        rpc("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}").andExpect(status().isAccepted()).andExpect(content().string(""));
        verifyNoInteractions(daily,source);
    }
    @Test void sourceAndStatusDelegateToSharedServices() throws Exception {
        when(source.read(null,null)).thenReturn(json.readTree("{\"meta\":{\"generatedAt\":\"2026-10-13 09:12:00\"}}"));
        rpc("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"news_source\",\"arguments\":{}}}")
                .andExpect(jsonPath("$.result.isError").value(false));
        rpc("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/call\",\"params\":{\"name\":\"news_save_status\",\"arguments\":{\"yyyymmdd\":\"20261013\",\"requestId\":\"request-1\"}}}")
                .andExpect(jsonPath("$.result.isError").value(false));
        verify(daily).status("20261013","request-1",42L);
    }
    @Test void saveUsesPrincipalAndSurfacesValidationError() throws Exception {
        when(daily.save(any(),eq(42L))).thenThrow(new IllegalArgumentException("20~25개 필요"));
        rpc("""
            {"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"news_summary_save",
            "arguments":{"yyyymmdd":"20261013","requestId":"request-1","sourceGeneratedAt":"2026-10-13 09:12:00","articles":[]}}}
            """).andExpect(jsonPath("$.result.isError").value(true));
        verify(daily).save(any(NewsDailySaveRequest.class),eq(42L));
    }
    @Test void badOriginUnsupportedVersionAndGetAreRejected() throws Exception {
        mvc.perform(post("/api/news/mcp").principal(auth).contentType("application/json")
                .header("Origin","https://evil.example").content("{}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/news/mcp").principal(auth).contentType("application/json")
                .header("MCP-Protocol-Version","invalid").content("{}")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/news/mcp").principal(auth)).andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(daily,source);
    }
    @Test void errorsAndNotificationsNeverExecuteWrites() throws Exception {
        rpc("[]").andExpect(jsonPath("$.error.code").value(-32600));
        rpc("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"unknown\"}").andExpect(jsonPath("$.error.code").value(-32601));
        rpc("{\"jsonrpc\":\"2.0\",\"method\":\"tools/call\",\"params\":{\"name\":\"news_summary_save\",\"arguments\":{}}}")
                .andExpect(status().isBadRequest());
        rpc("""
            {"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"news_summary_save",
            "arguments":{"yyyymmdd":"20261013","requestId":"request-1","regNo":999,"articles":[]}}}
            """).andExpect(jsonPath("$.result.isError").value(true));
        verifyNoInteractions(daily,source);
    }
}


