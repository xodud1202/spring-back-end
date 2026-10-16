package com.xodud1202.springbackend.controller.news;

import com.xodud1202.springbackend.common.exception.GlobalRestExceptionHandler;
import com.xodud1202.springbackend.domain.news.NewsDailySaveRequest;
import com.xodud1202.springbackend.entity.UserBaseEntity;
import com.xodud1202.springbackend.service.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NewsDailyControllerTests {
    private NewsDailyService daily;
    private MockMvc mvc;
    private UsernamePasswordAuthenticationToken auth;
    @BeforeEach void setup() {
        daily=mock(NewsDailyService.class);
        mvc=MockMvcBuilders.standaloneSetup(new NewsDailyController(daily,mock(NewsSourceService.class)))
            .setControllerAdvice(new GlobalRestExceptionHandler()).build();
        UserBaseEntity user=new UserBaseEntity();user.setUsrNo(12L);
        auth=new UsernamePasswordAuthenticationToken(user,null,user.getAuthorities());
    }
    @Test void startAndFailReuseAuthenticatedActor() throws Exception {
        mvc.perform(post("/api/news/daily/batches/start").principal(auth).contentType("application/json")
            .content("{\"yyyymmdd\":\"20261013\",\"requestId\":\"attempt-01\",\"sourceGeneratedAt\":\"2026-10-13 09:12:00\"}"))
            .andExpect(status().isOk());
        mvc.perform(post("/api/news/daily/batches/fail").principal(auth).contentType("application/json")
            .content("{\"yyyymmdd\":\"20261013\",\"requestId\":\"attempt-01\",\"reasonCode\":\"SUMMARY_FAILED\"}"))
            .andExpect(status().isOk());
        verify(daily).start(any(),eq(12L)); verify(daily).fail(any(),eq(12L));
    }
    @Test void lengthErrorIdentifiesArticleAndCountsWithoutContent() throws Exception {
        when(daily.save(any(),any())).thenThrow(new NewsDailyValidationException(6,"newsContent",800,801,"길이 초과"));
        mvc.perform(post("/api/news/daily/batches").principal(auth).contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.articleDispOrd").value(6))
            .andExpect(jsonPath("$.field").value("newsContent")).andExpect(jsonPath("$.maxLength").value(800))
            .andExpect(jsonPath("$.actualLength").value(801));
    }
    @Test void authenticatedUserSuppliesAuditIdentity() throws Exception {
        mvc.perform(post("/api/news/daily/batches").principal(auth).contentType("application/json")
            .content("{\"yyyymmdd\":\"20261013\",\"requestId\":\"request-1\",\"regNo\":999,\"articles\":[]}"))
            .andExpect(status().isOk());
        verify(daily).save(any(NewsDailySaveRequest.class),eq(12L));
    }
    @Test void anonymousWriteNeverReachesService() throws Exception {
        mvc.perform(post("/api/news/daily/batches").contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(daily);
    }
    @Test void validationAndConflictStatusAreCorrect() throws Exception {
        when(daily.latest("20260230")).thenThrow(new IllegalArgumentException("잘못된 날짜"));
        mvc.perform(get("/api/news/daily/latest").param("yyyymmdd","20260230")).andExpect(status().isBadRequest());
        when(daily.save(any(),any())).thenThrow(new NewsDailyConflictException("요청 ID 충돌"));
        mvc.perform(post("/api/news/daily/batches").principal(auth).contentType("application/json").content("{}"))
            .andExpect(status().isConflict());
    }

    /** 요약 읽기 API는 로그인 없이 날짜별 뉴스와 선택 가능한 날짜를 반환합니다. */
    @Test void summaryAndDatesArePublicReadEndpoints() throws Exception {
        // 인증 주체 없이 호출하고 화면에서 사용하는 응답 필드와 캐시 정책을 확인합니다.
        when(daily.summary("20261016")).thenReturn(new com.xodud1202.springbackend.domain.news.NewsDailySummary(
                "20261016", 4, java.util.List.of(new com.xodud1202.springbackend.domain.news.NewsDailyArticle("사회", 1, "뉴스 요약"))));
        when(daily.summaryWeek()).thenReturn(new com.xodud1202.springbackend.domain.news.NewsDailySummaryWeek(
                "20261016", java.util.List.of(new com.xodud1202.springbackend.domain.news.NewsDailySummaryDate("20261016", 4))));
        mvc.perform(get("/api/news/daily/summary").param("yyyymmdd", "20261016"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.seq").value(4))
                .andExpect(jsonPath("$.articles[0].newsContent").value("뉴스 요약"));
        mvc.perform(get("/api/news/daily/summary/dates"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.today").value("20261016"))
                .andExpect(jsonPath("$.dates[0].seq").value(4));
        when(daily.summary("20260230")).thenThrow(new IllegalArgumentException("존재하지 않는 날짜입니다."));
        mvc.perform(get("/api/news/daily/summary").param("yyyymmdd", "20260230")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/news/daily/summary")).andExpect(status().isBadRequest());
    }
}


