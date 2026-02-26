package com.xodud1202.springbackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xodud1202.springbackend.domain.notion.NotionDataListUpsertPO;
import com.xodud1202.springbackend.mapper.NotionWebhookMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// NotionWebhookDataSyncService의 웹훅 기반 동기화 로직을 검증합니다.
class NotionWebhookDataSyncServiceTests {

	// Notion 데이터 upsert 매퍼 목 객체입니다.
	@Mock
	private NotionWebhookMapper notionWebhookMapper;

	// Notion API 클라이언트 목 객체입니다.
	@Mock
	private NotionApiClient notionApiClient;

	// 테스트에서 공용으로 사용하는 JSON 매퍼입니다.
	private ObjectMapper objectMapper;

	@BeforeEach
	// 각 테스트 실행 전 JSON 매퍼를 초기화합니다.
	void setUp() {
		objectMapper = new ObjectMapper();
	}

	@Test
	@DisplayName("동기화 처리: verification_token 이벤트는 저장하지 않는다")
	// 구독 검증 이벤트는 Notion 데이터 동기화에서 제외합니다.
	void syncNotionDataFromWebhook_skipsVerificationEvent() {
		NotionWebhookDataSyncService service = new NotionWebhookDataSyncService(
			notionWebhookMapper,
			notionApiClient,
			objectMapper,
			""
		);

		String body = "{\"verification_token\":\"sample-token\"}";
		int affected = service.syncNotionDataFromWebhook(body, Map.of());

		assertEquals(0, affected);
		verify(notionApiClient, never()).retrievePage(any());
		verify(notionWebhookMapper, never()).upsertNotionDataList(any());
	}

	@Test
	@DisplayName("동기화 처리: page 이벤트면 Notion 조회 후 NOTION_DATA_LIST에 upsert 한다")
	// page 엔티티 웹훅을 기준으로 페이지/블록을 조회해 DB에 저장합니다.
	void syncNotionDataFromWebhook_syncsPageEvent() throws Exception {
		NotionWebhookDataSyncService service = new NotionWebhookDataSyncService(
			notionWebhookMapper,
			notionApiClient,
			objectMapper,
			""
		);

		String webhookBody = """
			{
			  "entity": {"type":"page","id":"31359db6-4d86-8066-8302-f1f02561a8c1"},
			  "data": {"parent":{"id":"24759db6-4d86-8062-920b-c9cabd964411","data_source_id":"24759db6-4d86-811f-acce-000b244c4fd6"}}
			}
			""";
		JsonNode pageNode = objectMapper.readTree("""
			{
			  "url":"https://www.notion.so/sample",
			  "created_time":"2026-02-26T05:07:22.809Z",
			  "archived":false,
			  "in_trash":false,
			  "properties":{
			    "제목":{"type":"title","title":[{"plain_text":"샘플 타이틀"}]},
			    "category_id":{"type":"rich_text","rich_text":[{"plain_text":"CAT-1"}]}
			  }
			}
			""");
		List<JsonNode> blocks = List.of(
			objectMapper.readTree("""
				{
				  "type":"paragraph",
				  "paragraph":{"rich_text":[{"plain_text":"첫 줄"}]}
				}
				"""),
			objectMapper.readTree("""
				{
				  "type":"paragraph",
				  "paragraph":{"rich_text":[{"plain_text":"둘째 줄"}]}
				}
				""")
		);

		when(notionApiClient.retrievePage("31359db6-4d86-8066-8302-f1f02561a8c1")).thenReturn(pageNode);
		when(notionApiClient.retrieveAllChildBlocks("31359db6-4d86-8066-8302-f1f02561a8c1")).thenReturn(blocks);
		when(notionWebhookMapper.upsertNotionDataList(any(NotionDataListUpsertPO.class))).thenReturn(1);

		int affected = service.syncNotionDataFromWebhook(webhookBody, Map.of());

		assertEquals(1, affected);
		ArgumentCaptor<NotionDataListUpsertPO> captor = ArgumentCaptor.forClass(NotionDataListUpsertPO.class);
		verify(notionWebhookMapper).upsertNotionDataList(captor.capture());
		NotionDataListUpsertPO row = captor.getValue();
		assertEquals("31359db6-4d86-8066-8302-f1f02561a8c1", row.getId());
		assertEquals("24759db6-4d86-8062-920b-c9cabd964411", row.getDatabaseId());
		assertEquals("24759db6-4d86-811f-acce-000b244c4fd6", row.getDataSourceId());
		assertEquals("샘플 타이틀", row.getTitle());
		assertEquals("첫 줄\n둘째 줄", row.getNotes());
		assertEquals("N", row.getDelYn());
		assertEquals("CAT-1", row.getCategoryId());
	}

	@Test
	@DisplayName("동기화 처리: page.deleted 이벤트는 Notion 조회 없이 삭제 상태만 upsert 한다")
	// page.deleted 이벤트는 Notion API 조회를 생략하고 삭제 상태만 반영합니다.
	void syncNotionDataFromWebhook_marksDeletedWithoutNotionApiCall() {
		NotionWebhookDataSyncService service = new NotionWebhookDataSyncService(
			notionWebhookMapper,
			notionApiClient,
			objectMapper,
			""
		);

		String webhookBody = """
			{
			  "type":"page.deleted",
			  "entity":{"type":"page","id":"31359db6-4d86-807b-a8d0-eacd48f3f3c6"},
			  "data":{"parent":{"id":"24759db6-4d86-8062-920b-c9cabd964411","data_source_id":"24759db6-4d86-811f-acce-000b244c4fd6"}}
			}
			""";

		when(notionWebhookMapper.upsertNotionDataDeleted(any(NotionDataListUpsertPO.class))).thenReturn(1);

		int affected = service.syncNotionDataFromWebhook(webhookBody, Map.of());

		assertEquals(1, affected);
		verify(notionApiClient, never()).retrievePage(any());
		verify(notionApiClient, never()).retrieveAllChildBlocks(any());
		ArgumentCaptor<NotionDataListUpsertPO> captor = ArgumentCaptor.forClass(NotionDataListUpsertPO.class);
		verify(notionWebhookMapper).upsertNotionDataDeleted(captor.capture());
		assertEquals("31359db6-4d86-807b-a8d0-eacd48f3f3c6", captor.getValue().getId());
		assertEquals("Y", captor.getValue().getDelYn());
	}

	@Test
	@DisplayName("동기화 처리: 검증 토큰이 설정된 상태에서 서명이 불일치하면 예외를 발생시킨다")
	// 서명 검증 실패 요청은 DB 처리 전에 차단합니다.
	void syncNotionDataFromWebhook_throwsWhenSignatureMismatch() {
		NotionWebhookDataSyncService service = new NotionWebhookDataSyncService(
			notionWebhookMapper,
			notionApiClient,
			objectMapper,
			"verification-secret"
		);

		String webhookBody = "{\"entity\":{\"type\":\"page\",\"id\":\"page-id\"}}";

		assertThrows(
			SecurityException.class,
			() -> service.syncNotionDataFromWebhook(webhookBody, Map.of("x-notion-signature", "sha256=invalid"))
		);
		verify(notionApiClient, never()).retrievePage(any());
		verify(notionWebhookMapper, never()).upsertNotionDataList(any());
	}
}
