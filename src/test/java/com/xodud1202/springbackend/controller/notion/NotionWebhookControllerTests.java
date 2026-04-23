package com.xodud1202.springbackend.controller.notion;

import com.xodud1202.springbackend.service.NotionWebhookDataSyncService;
import com.xodud1202.springbackend.service.NotionWebhookTempSaveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
// Notion 웹훅 컨트롤러의 검증/저장 순서를 검증합니다.
class NotionWebhookControllerTests {
	@Mock
	private NotionWebhookTempSaveService notionWebhookTempSaveService;

	@Mock
	private NotionWebhookDataSyncService notionWebhookDataSyncService;

	private MockMvc mockMvc;

	@BeforeEach
	// 컨트롤러 단위 테스트용 MockMvc를 초기화합니다.
	void setUp() {
		NotionWebhookController controller = new NotionWebhookController(notionWebhookTempSaveService, notionWebhookDataSyncService);
		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
	}

	@Test
	@DisplayName("Notion 웹훅은 검증 성공 후 allowlist 헤더만 임시 저장한다")
	// 서명 검증이 통과한 요청만 임시 저장하고 민감 헤더를 제외하는지 검증합니다.
	void notionWebhook_savesAfterValidationWithAllowedHeadersOnly() throws Exception {
		// 검증 성공과 저장/동기화 결과를 목으로 구성합니다.
		String requestBody = "{\"entity\":{\"type\":\"page\",\"id\":\"page-id\"}}";
		doNothing().when(notionWebhookDataSyncService).validateWebhookRequest(eq(requestBody), anyMap());
		when(notionWebhookTempSaveService.saveWebhookRequest(eq("/api/notion/webhook"), anyMap(), anyMap(), eq(requestBody))).thenReturn(1);
		when(notionWebhookDataSyncService.syncNotionDataFromWebhook(eq(requestBody), anyMap())).thenReturn(2);

		// 웹훅 요청을 전송합니다.
		mockMvc.perform(post("/api/notion/webhook")
				.contentType(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.USER_AGENT, "notion-test-agent")
				.header(HttpHeaders.AUTHORIZATION, "Bearer sensitive-token")
				.header("x-notion-signature", "sha256=signature")
				.header("x-notion-workspace-id", "workspace-id")
				.content(requestBody))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.savedCount").value(1))
			.andExpect(jsonPath("$.syncedCount").value(2));

		// 임시 저장에는 allowlist 헤더만 전달되는지 확인합니다.
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, String>> headerCaptor = ArgumentCaptor.forClass((Class) Map.class);
		verify(notionWebhookTempSaveService).saveWebhookRequest(eq("/api/notion/webhook"), headerCaptor.capture(), anyMap(), eq(requestBody));
		Map<String, String> storedHeaders = headerCaptor.getValue();
		assertTrue(containsHeader(storedHeaders, HttpHeaders.CONTENT_TYPE));
		assertTrue(containsHeader(storedHeaders, HttpHeaders.USER_AGENT));
		assertTrue(containsHeader(storedHeaders, "x-notion-workspace-id"));
		assertFalse(containsHeader(storedHeaders, "x-notion-signature"));
		assertFalse(containsHeader(storedHeaders, HttpHeaders.AUTHORIZATION));
	}

	@Test
	@DisplayName("Notion 웹훅은 서명 검증 실패 시 임시 저장하지 않는다")
	// 서명 실패 요청은 DB 임시 저장 전에 차단되는지 검증합니다.
	void notionWebhook_doesNotSaveWhenSignatureInvalid() throws Exception {
		// 검증 실패를 목으로 구성합니다.
		String requestBody = "{\"entity\":{\"type\":\"page\",\"id\":\"page-id\"}}";
		doThrow(new SecurityException("invalid signature"))
			.when(notionWebhookDataSyncService)
			.validateWebhookRequest(eq(requestBody), anyMap());

		// 웹훅 요청을 전송하고 401 응답을 확인합니다.
		mockMvc.perform(post("/api/notion/webhook")
				.contentType(MediaType.APPLICATION_JSON)
				.header("x-notion-signature", "sha256=invalid")
				.content(requestBody))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("웹훅 서명 검증 실패"));

		// 검증 실패 시 저장과 동기화를 수행하지 않아야 합니다.
		verify(notionWebhookTempSaveService, never()).saveWebhookRequest(any(), anyMap(), anyMap(), any());
		verify(notionWebhookDataSyncService, never()).syncNotionDataFromWebhook(any(), anyMap());
	}

	@Test
	@DisplayName("Notion 웹훅은 바디 검증 실패 시 임시 저장하지 않는다")
	// 바디 누락 요청은 DB 임시 저장 전에 400으로 차단되는지 검증합니다.
	void notionWebhook_doesNotSaveWhenBodyInvalid() throws Exception {
		// 요청값 검증 실패를 목으로 구성합니다.
		doThrow(new IllegalArgumentException("body required"))
			.when(notionWebhookDataSyncService)
			.validateWebhookRequest(any(), anyMap());

		// 빈 바디 웹훅 요청을 전송하고 400 응답을 확인합니다.
		mockMvc.perform(post("/api/notion/webhook")
				.contentType(MediaType.APPLICATION_JSON)
				.content(""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("웹훅 요청값 확인 실패"));

		// 검증 실패 시 저장과 동기화를 수행하지 않아야 합니다.
		verify(notionWebhookTempSaveService, never()).saveWebhookRequest(any(), anyMap(), anyMap(), any());
		verify(notionWebhookDataSyncService, never()).syncNotionDataFromWebhook(any(), anyMap());
	}

	// 헤더 맵에 대소문자와 무관하게 헤더명이 포함되어 있는지 확인합니다.
	private boolean containsHeader(Map<String, String> headers, String headerName) {
		if (headers == null || headerName == null) {
			return false;
		}
		String normalizedHeaderName = headerName.toLowerCase(Locale.ROOT);
		return headers.keySet().stream()
			.anyMatch(key -> key != null && normalizedHeaderName.equals(key.toLowerCase(Locale.ROOT)));
	}
}
