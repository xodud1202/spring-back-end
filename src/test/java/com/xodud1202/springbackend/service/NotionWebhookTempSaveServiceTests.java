package com.xodud1202.springbackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xodud1202.springbackend.domain.notion.NotionWebhookTempEntryPO;
import com.xodud1202.springbackend.mapper.NotionWebhookMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// NotionWebhookTempSaveService의 웹훅 분해/저장 로직을 검증합니다.
class NotionWebhookTempSaveServiceTests {

	// Notion 웹훅 임시 저장 매퍼 목 객체입니다.
	@Mock
	private NotionWebhookMapper notionWebhookMapper;

	// 테스트 대상 서비스 객체입니다.
	private NotionWebhookTempSaveService notionWebhookTempSaveService;

	@BeforeEach
	// 테스트 실행 전에 서비스 객체를 초기화합니다.
	void setUp() {
		// 목 매퍼와 ObjectMapper를 결합해 테스트 대상을 생성합니다.
		notionWebhookTempSaveService = new NotionWebhookTempSaveService(notionWebhookMapper, new ObjectMapper());
	}

	@Test
	@DisplayName("저장 처리: 헤더/쿼리/JSON 바디를 모두 분해해 일괄 저장한다")
	// 헤더/쿼리/JSON 바디 입력을 키-값 엔트리로 분해해 저장합니다.
	void saveWebhookRequest_savesHeaderQueryAndJsonBody() {
		// 헤더/쿼리/바디 샘플 요청 데이터를 구성합니다.
		Map<String, String> headers = new LinkedHashMap<>();
		headers.put("authorization", "Bearer sample-token");
		headers.put("x-notion-signature", "signature-value");
		Map<String, String[]> queryParams = new LinkedHashMap<>();
		queryParams.put("challenge", new String[] { "abc123" });
		queryParams.put("state", new String[] { "one", "two" });
		String body = "{\"token\":\"temp-token\",\"event\":{\"type\":\"page.updated\"}}";

		// 매퍼 저장 결과를 목으로 고정합니다.
		when(notionWebhookMapper.insertNotionWebhookTempBatch(anyList())).thenReturn(9);

		// 저장 로직을 실행합니다.
		int affected = notionWebhookTempSaveService.saveWebhookRequest("/api/notion/webhook", headers, queryParams, body);

		// 반환 건수와 매퍼 입력 엔트리를 함께 검증합니다.
		assertEquals(9, affected);
		ArgumentCaptor<List<NotionWebhookTempEntryPO>> captor = ArgumentCaptor.forClass(List.class);
		verify(notionWebhookMapper).insertNotionWebhookTempBatch(captor.capture());
		List<NotionWebhookTempEntryPO> rows = captor.getValue();
		assertTrue(rows.stream().anyMatch(row -> "HEADER.authorization".equals(row.getTempKey()) && "Bearer sample-token".equals(row.getTempValue())));
		assertTrue(rows.stream().anyMatch(row -> "QUERY.challenge".equals(row.getTempKey()) && "abc123".equals(row.getTempValue())));
		assertTrue(rows.stream().anyMatch(row -> "QUERY.state[1]".equals(row.getTempKey()) && "two".equals(row.getTempValue())));
		assertTrue(rows.stream().anyMatch(row -> "BODY_RAW".equals(row.getTempKey())));
		assertTrue(rows.stream().anyMatch(row -> "BODY.token".equals(row.getTempKey()) && "temp-token".equals(row.getTempValue())));
		assertTrue(rows.stream().anyMatch(row -> "BODY.event.type".equals(row.getTempKey()) && "page.updated".equals(row.getTempValue())));
	}

	@Test
	@DisplayName("저장 처리: 저장할 데이터가 없으면 DB 저장을 수행하지 않는다")
	// 헤더/쿼리/바디가 모두 비어 있으면 DB 저장 없이 0을 반환합니다.
	void saveWebhookRequest_returnsZeroWhenNoInput() {
		// 비어 있는 입력으로 저장 로직을 실행합니다.
		int affected = notionWebhookTempSaveService.saveWebhookRequest("/api/notion/webhook", Map.of(), Map.of(), "   ");

		// 저장 건수와 매퍼 미호출 여부를 검증합니다.
		assertEquals(0, affected);
		verify(notionWebhookMapper, never()).insertNotionWebhookTempBatch(anyList());
	}

	@Test
	@DisplayName("저장 처리: 바디가 평문이면 BODY_RAW만 저장한다")
	// key=value가 아닌 평문 바디 입력 시 원문만 저장합니다.
	void saveWebhookRequest_savesOnlyRawBodyWhenBodyIsNotJson() {
		// 평문 바디를 포함한 입력 데이터를 구성합니다.
		String body = "plain-text-body";

		// 매퍼 저장 결과를 목으로 고정합니다.
		when(notionWebhookMapper.insertNotionWebhookTempBatch(anyList())).thenReturn(1);

		// 저장 로직을 실행합니다.
		int affected = notionWebhookTempSaveService.saveWebhookRequest("/api/notion/webhook", Map.of(), Map.of(), body);

		// BODY_RAW만 저장되는지 검증합니다.
		assertEquals(1, affected);
		ArgumentCaptor<List<NotionWebhookTempEntryPO>> captor = ArgumentCaptor.forClass(List.class);
		verify(notionWebhookMapper).insertNotionWebhookTempBatch(captor.capture());
		List<NotionWebhookTempEntryPO> rows = captor.getValue();
		assertEquals(1, rows.size());
		assertEquals("BODY_RAW", rows.get(0).getTempKey());
		assertEquals("plain-text-body", rows.get(0).getTempValue());
	}

	@Test
	@DisplayName("저장 처리: 긴 JSON 바디는 하위 키 저장 후 BODY_RAW 원문 저장을 생략한다")
	// 긴 JSON 바디는 BODY 하위 키를 저장하고 BODY_RAW는 길이 초과로 생략합니다.
	void saveWebhookRequest_skipsLongBodyRawWhenJsonParsed() {
		// 255자를 초과하는 JSON 바디 문자열을 구성합니다.
		String body = "{\"token\":\"" + "A".repeat(280) + "\"}";

		// 매퍼 저장 결과를 목으로 고정합니다.
		when(notionWebhookMapper.insertNotionWebhookTempBatch(anyList())).thenReturn(1);

		// 저장 로직을 실행합니다.
		int affected = notionWebhookTempSaveService.saveWebhookRequest("/api/notion/webhook", Map.of(), Map.of(), body);

		// BODY.token만 저장되고 BODY_RAW는 저장되지 않는지 검증합니다.
		assertEquals(1, affected);
		ArgumentCaptor<List<NotionWebhookTempEntryPO>> captor = ArgumentCaptor.forClass(List.class);
		verify(notionWebhookMapper).insertNotionWebhookTempBatch(captor.capture());
		List<NotionWebhookTempEntryPO> rows = captor.getValue();
		assertEquals(1, rows.size());
		assertEquals("BODY.token", rows.get(0).getTempKey());
		assertEquals(255, rows.get(0).getTempValue().length());
	}

	@Test
	@DisplayName("저장 처리: 비JSON 바디가 key=value 포맷이면 BODY 하위 키로 분해한다")
	// 비JSON key=value 바디는 BODY.{key} 형태로 파싱해 저장합니다.
	void saveWebhookRequest_parsesNonJsonKeyValueBody() {
		// key=value 포맷 바디를 구성합니다.
		String body = "token=abc123&event=page.content_updated";

		// 매퍼 저장 결과를 목으로 고정합니다.
		when(notionWebhookMapper.insertNotionWebhookTempBatch(anyList())).thenReturn(3);

		// 저장 로직을 실행합니다.
		int affected = notionWebhookTempSaveService.saveWebhookRequest("/api/notion/webhook", Map.of(), Map.of(), body);

		// BODY.key 및 BODY_RAW 저장을 검증합니다.
		assertEquals(3, affected);
		ArgumentCaptor<List<NotionWebhookTempEntryPO>> captor = ArgumentCaptor.forClass(List.class);
		verify(notionWebhookMapper).insertNotionWebhookTempBatch(captor.capture());
		List<NotionWebhookTempEntryPO> rows = captor.getValue();
		assertTrue(rows.stream().anyMatch(row -> "BODY.token".equals(row.getTempKey()) && "abc123".equals(row.getTempValue())));
		assertTrue(rows.stream().anyMatch(row -> "BODY.event".equals(row.getTempKey()) && "page.content_updated".equals(row.getTempValue())));
		assertTrue(rows.stream().anyMatch(row -> "BODY_RAW".equals(row.getTempKey()) && body.equals(row.getTempValue())));
	}
}
