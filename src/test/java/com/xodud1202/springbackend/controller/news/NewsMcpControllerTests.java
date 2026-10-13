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
    private UsernamePasswordAuthenticationToken auth;

    @BeforeEach void setup() {
        daily=mock(NewsDailyService.class); source=mock(NewsSourceService.class);
        mvc=MockMvcBuilders.standaloneSetup(new NewsMcpController(daily,source,json,"https://allowed.example")).build();
        UserBaseEntity user=new UserBaseEntity(); user.setUsrNo(42L);
        auth=new UsernamePasswordAuthenticationToken(user,null,user.getAuthorities());
    }
    private ResultActions rpc(String body) throws Exception {
        return mvc.perform(post("/api/news/mcp").principal(auth).contentType("application/json")
                .header("Accept","application/json, text/event-stream").header("MCP-Protocol-Version","2025-06-18").content(body));
    }
    @Test void initializeListAndNotification() throws Exception {
        rpc("""
            {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"test","version":"1"}}}
            """).andExpect(status().isOk()).andExpect(jsonPath("$.result.protocolVersion").value("2025-06-18"));
        rpc("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}")
                .andExpect(jsonPath("$.result.tools.length()").value(5))
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


