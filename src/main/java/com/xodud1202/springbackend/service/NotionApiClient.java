package com.xodud1202.springbackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
// Notion 공개 API 호출을 담당하는 클라이언트 컴포넌트입니다.
public class NotionApiClient {
	private static final String NOTION_API_BASE_URL = "https://api.notion.com/v1";
	private static final int HTTP_TIMEOUT_SECONDS = 15;
	private static final int PAGE_SIZE = 100;

	private final ObjectMapper objectMapper;
	private final String notionApiKey;
	private final String notionApiVersion;
	private final HttpClient httpClient = HttpClient.newBuilder().build();

	// 생성자에서 Notion API 인증/버전 설정을 주입받습니다.
	public NotionApiClient(
		ObjectMapper objectMapper,
		@Value("${notion.api-key:}") String notionApiKey,
		@Value("${notion.api-version:2025-09-03}") String notionApiVersion
	) {
		this.objectMapper = objectMapper;
		this.notionApiKey = safeValue(notionApiKey);
		this.notionApiVersion = safeValue(notionApiVersion);
	}

	// 페이지 ID로 Notion 페이지 상세 정보를 조회합니다.
	public JsonNode retrievePage(String pageId) {
		String normalizedPageId = trimToNull(pageId);
		if (normalizedPageId == null) {
			throw new IllegalArgumentException("Notion pageId가 비어 있습니다.");
		}
		String encodedPageId = URLEncoder.encode(normalizedPageId, StandardCharsets.UTF_8);
		return executeGet(NOTION_API_BASE_URL + "/pages/" + encodedPageId);
	}

	// 블록 ID 기준으로 자식 블록 전체를 페이지네이션 포함 조회합니다.
	public List<JsonNode> retrieveAllChildBlocks(String blockId) {
		String normalizedBlockId = trimToNull(blockId);
		if (normalizedBlockId == null) {
			return List.of();
		}

		List<JsonNode> blocks = new ArrayList<>();
		String nextCursor = null;
		do {
			String encodedBlockId = URLEncoder.encode(normalizedBlockId, StandardCharsets.UTF_8);
			String endpoint = NOTION_API_BASE_URL + "/blocks/" + encodedBlockId + "/children?page_size=" + PAGE_SIZE;
			if (nextCursor != null) {
				endpoint = endpoint + "&start_cursor=" + URLEncoder.encode(nextCursor, StandardCharsets.UTF_8);
			}
			JsonNode responseNode = executeGet(endpoint);
			JsonNode resultsNode = responseNode.path("results");
			if (resultsNode.isArray()) {
				for (JsonNode blockNode : resultsNode) {
					blocks.add(blockNode);
				}
			}
			nextCursor = trimToNull(responseNode.path("next_cursor").asText(null));
		} while (nextCursor != null);

		return blocks;
	}

	// Notion GET 요청을 실행하고 JSON 응답을 파싱합니다.
	private JsonNode executeGet(String url) {
		validateApiSettings();
		try {
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.timeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
				.header("Authorization", "Bearer " + notionApiKey)
				.header("Notion-Version", notionApiVersion)
				.header("Accept", "application/json")
				.GET()
				.build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				throw new IllegalStateException("Notion API 응답 코드가 비정상입니다. status=" + response.statusCode() + ", body=" + response.body());
			}
			return objectMapper.readTree(response.body());
		} catch (Exception exception) {
			throw new IllegalStateException("Notion API 호출에 실패했습니다. url=" + url, exception);
		}
	}

	// API 호출 전 필수 인증/버전 설정을 확인합니다.
	private void validateApiSettings() {
		if (trimToNull(notionApiKey) == null) {
			throw new IllegalStateException("notion.api-key 설정이 필요합니다.");
		}
		if (trimToNull(notionApiVersion) == null) {
			throw new IllegalStateException("notion.api-version 설정이 필요합니다.");
		}
	}

	// 문자열을 trim 처리하고 비어 있으면 null로 변환합니다.
	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	// 문자열 null 안전값을 반환합니다.
	private String safeValue(String value) {
		return value == null ? "" : value;
	}
}
