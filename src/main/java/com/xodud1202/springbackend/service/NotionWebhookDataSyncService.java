package com.xodud1202.springbackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xodud1202.springbackend.domain.notion.NotionCategoryUpsertPO;
import com.xodud1202.springbackend.domain.notion.NotionDataListUpsertPO;
import com.xodud1202.springbackend.mapper.NotionWebhookMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
// Notion 웹훅 이벤트를 기준으로 Notion 상세 데이터를 조회해 NOTION_DATA_LIST에 저장하는 서비스입니다.
public class NotionWebhookDataSyncService {
	private static final int MAX_ID_LENGTH = 72;
	private static final int MAX_TITLE_LENGTH = 200;
	private static final int MAX_URL_LENGTH = 255;
	private static final int MAX_CATEGORY_NM_LENGTH = 50;
	private static final int MAX_CATEGORY_COLOR_LENGTH = 24;
	private static final String SIGNATURE_HEADER = "x-notion-signature";
	private static final String EVENT_TYPE_PAGE_DELETED = "page.deleted";

	private final NotionWebhookMapper notionWebhookMapper;
	private final NotionApiClient notionApiClient;
	private final ObjectMapper objectMapper;
	private final String notionWebhookVerificationToken;

	// 생성자에서 동기화에 필요한 의존성과 검증 토큰 설정을 주입받습니다.
	public NotionWebhookDataSyncService(
		NotionWebhookMapper notionWebhookMapper,
		NotionApiClient notionApiClient,
		ObjectMapper objectMapper,
		@Value("${notion.webhook-verification-token:}") String notionWebhookVerificationToken
	) {
		this.notionWebhookMapper = notionWebhookMapper;
		this.notionApiClient = notionApiClient;
		this.objectMapper = objectMapper;
		this.notionWebhookVerificationToken = safeValue(notionWebhookVerificationToken);
	}

	@Transactional
	// 웹훅 바디를 파싱해 Notion 페이지 상세를 조회하고 NOTION_DATA_LIST에 upsert 합니다.
	public int syncNotionDataFromWebhook(String requestBody, Map<String, String> headerMap) {
		String normalizedBody = trimToNull(requestBody);
		if (normalizedBody == null) {
			return 0;
		}

		// 서명 검증이 설정된 경우 x-notion-signature를 검증합니다.
		validateSignatureIfNeeded(normalizedBody, headerMap);

		JsonNode rootNode = parseJson(normalizedBody);

		// 구독 검증 이벤트는 데이터 동기화 대상이 아니므로 즉시 종료합니다.
		if (trimToNull(rootNode.path("verification_token").asText(null)) != null) {
			return 0;
		}

		// page 이벤트가 아니면 동기화를 수행하지 않습니다.
		String entityType = trimToNull(rootNode.path("entity").path("type").asText(null));
		if (!"page".equals(entityType)) {
			return 0;
		}

		String pageId = trimToNull(rootNode.path("entity").path("id").asText(null));
		if (pageId == null) {
			return 0;
		}

		String eventType = trimToNull(rootNode.path("type").asText(null));
		// page.deleted 이벤트는 Notion 조회 API를 호출하지 않고 삭제 상태만 반영합니다.
		if (EVENT_TYPE_PAGE_DELETED.equals(eventType)) {
			NotionDataListUpsertPO deletedRow = buildDeletedUpsertRow(rootNode);
			return notionWebhookMapper.upsertNotionDataDeleted(deletedRow);
		}

		// Notion API에서 페이지 상세와 블록 목록을 조회합니다.
		JsonNode pageNode;
		List<JsonNode> blocks;
		try {
			pageNode = notionApiClient.retrievePage(pageId);
			blocks = notionApiClient.retrieveAllChildBlocks(pageId);
		} catch (Exception exception) {
			String rootMessage = trimToNull(NestedExceptionUtils.getMostSpecificCause(exception).getMessage());
			String detailMessage = rootMessage == null ? safeValue(exception.getMessage()) : rootMessage;
			throw new IllegalStateException(
				"Notion 동기화 API 호출 실패 eventType=" + safeValue(eventType) + ", pageId=" + pageId + ", detail=" + detailMessage,
				exception
			);
		}

		// 조회 결과를 DB upsert 파라미터로 변환해 저장합니다.
		NotionDataListUpsertPO upsertRow = buildUpsertRow(rootNode, pageNode, blocks);
		int syncedCount = notionWebhookMapper.upsertNotionDataList(upsertRow);

		// 게시글 저장이 끝난 뒤 Category 목록을 수집해 중복 제거 후 upsert 합니다.
		List<NotionCategoryUpsertPO> categoryRows = deduplicateCategories(extractCategoryRows(pageNode.path("properties")));
		if (!categoryRows.isEmpty()) {
			notionWebhookMapper.upsertNotionCategoryBatch(categoryRows);
		}
		return syncedCount;
	}

	// page.deleted 이벤트를 NOTION_DATA_LIST 삭제 상태 upsert 파라미터로 변환합니다.
	private NotionDataListUpsertPO buildDeletedUpsertRow(JsonNode webhookNode) {
		NotionDataListUpsertPO row = new NotionDataListUpsertPO();
		String pageId = trimToNull(webhookNode.path("entity").path("id").asText(null));
		String databaseId = trimToNull(webhookNode.path("data").path("parent").path("id").asText(null));
		String dataSourceId = trimToNull(webhookNode.path("data").path("parent").path("data_source_id").asText(null));

		row.setId(limitLength(pageId, MAX_ID_LENGTH));
		row.setDatabaseId(limitLength(safeValue(databaseId), MAX_ID_LENGTH));
		row.setDataSourceId(limitLength(safeValue(dataSourceId), MAX_ID_LENGTH));
		row.setDelYn("Y");
		return row;
	}

	// 검증 토큰이 설정된 경우 헤더 서명을 HMAC SHA-256으로 검증합니다.
	private void validateSignatureIfNeeded(String requestBody, Map<String, String> headerMap) {
		String verificationToken = trimToNull(notionWebhookVerificationToken);
		if (verificationToken == null) {
			return;
		}

		String providedSignature = findHeaderIgnoreCase(headerMap, SIGNATURE_HEADER);
		if (providedSignature == null) {
			throw new SecurityException("Notion 서명 헤더가 존재하지 않습니다.");
		}

		String expectedSignature = "sha256=" + hmacSha256Hex(verificationToken, requestBody);
		byte[] providedBytes = providedSignature.getBytes(StandardCharsets.UTF_8);
		byte[] expectedBytes = expectedSignature.getBytes(StandardCharsets.UTF_8);
		if (!MessageDigest.isEqual(expectedBytes, providedBytes)) {
			throw new SecurityException("Notion 서명 검증에 실패했습니다.");
		}
	}

	// 요청 헤더에서 키 대소문자와 무관하게 값을 조회합니다.
	private String findHeaderIgnoreCase(Map<String, String> headerMap, String headerName) {
		if (headerMap == null || headerMap.isEmpty()) {
			return null;
		}

		for (Map.Entry<String, String> entry : headerMap.entrySet()) {
			String key = trimToNull(entry.getKey());
			if (key == null) {
				continue;
			}
			if (headerName.equalsIgnoreCase(key)) {
				return trimToNull(entry.getValue());
			}
		}
		return null;
	}

	// HMAC SHA-256 해시를 16진수 문자열로 변환합니다.
	private String hmacSha256Hex(String secret, String message) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			byte[] digest = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
			StringBuilder hexBuilder = new StringBuilder();
			for (byte value : digest) {
				hexBuilder.append(String.format(Locale.ROOT, "%02x", value));
			}
			return hexBuilder.toString();
		} catch (Exception exception) {
			throw new IllegalStateException("Notion 서명 해시 생성에 실패했습니다.", exception);
		}
	}

	// JSON 문자열을 JsonNode로 파싱합니다.
	private JsonNode parseJson(String requestBody) {
		try {
			return objectMapper.readTree(requestBody);
		} catch (Exception exception) {
			throw new IllegalStateException("웹훅 바디 JSON 파싱에 실패했습니다.", exception);
		}
	}

	// 웹훅/페이지/블록 정보를 NOTION_DATA_LIST upsert 파라미터로 변환합니다.
	private NotionDataListUpsertPO buildUpsertRow(JsonNode webhookNode, JsonNode pageNode, List<JsonNode> blocks) {
		NotionDataListUpsertPO row = new NotionDataListUpsertPO();

		// ID/상위 식별자를 우선 웹훅 기준으로 설정하고 누락 시 페이지 응답으로 보완합니다.
		String pageId = trimToNull(webhookNode.path("entity").path("id").asText(null));
		String databaseId = firstNonNull(
			trimToNull(webhookNode.path("data").path("parent").path("id").asText(null)),
			trimToNull(pageNode.path("parent").path("database_id").asText(null))
		);
		String dataSourceId = firstNonNull(
			trimToNull(webhookNode.path("data").path("parent").path("data_source_id").asText(null)),
			trimToNull(pageNode.path("parent").path("data_source_id").asText(null))
		);

		row.setId(limitLength(pageId, MAX_ID_LENGTH));
		row.setDatabaseId(limitLength(safeValue(databaseId), MAX_ID_LENGTH));
		row.setDataSourceId(limitLength(safeValue(dataSourceId), MAX_ID_LENGTH));

		// 페이지 메타 정보를 title/url/notionUrl/delYn/createDt로 설정합니다.
		row.setTitle(limitLength(extractPageTitle(pageNode.path("properties")), MAX_TITLE_LENGTH));
		row.setUrl(limitLength(extractPageUrlProperty(pageNode.path("properties")), MAX_URL_LENGTH));
		row.setNotionUrl(limitLength(trimToNull(pageNode.path("url").asText(null)), MAX_URL_LENGTH));
		row.setDelYn(resolveDeleteFlag(pageNode));
		row.setCreateDt(parseOffsetDateTime(pageNode.path("created_time").asText(null)));

		// NOTES는 properties.Notes.rich_text[].text.content를 우선 사용하고 없으면 블록 본문으로 보완합니다.
		String notesFromProperties = extractNotesFromProperties(pageNode.path("properties"));
		String resolvedNotes = trimToNull(notesFromProperties) != null ? notesFromProperties : extractNotes(blocks);
		row.setNotes(resolvedNotes);

		// CATEGORY_ID는 properties.Category.multi_select[0].id를 우선 사용합니다.
		row.setCategoryId(limitLength(extractCategoryId(pageNode.path("properties")), MAX_ID_LENGTH));
		return row;
	}

	// properties.Notes.rich_text[].text.content를 순서대로 결합해 NOTES 문자열을 생성합니다.
	private String extractNotesFromProperties(JsonNode propertiesNode) {
		if (propertiesNode == null || !propertiesNode.isObject()) {
			return "";
		}

		JsonNode notesProperty = propertiesNode.path("Notes");
		if (!notesProperty.isObject()) {
			return "";
		}

		JsonNode richTextNode = notesProperty.path("rich_text");
		if (!richTextNode.isArray()) {
			return "";
		}

		StringBuilder notesBuilder = new StringBuilder();
		for (JsonNode itemNode : richTextNode) {
			JsonNode textNode = itemNode.path("text");
			String content = trimToNull(textNode.path("content").asText(null));
			if (content != null) {
				notesBuilder.append(content);
			}
		}
		return notesBuilder.toString();
	}

	// 페이지 properties에서 title 타입 필드를 찾아 plain_text를 결합합니다.
	private String extractPageTitle(JsonNode propertiesNode) {
		if (propertiesNode == null || !propertiesNode.isObject()) {
			return "";
		}

		Iterator<Map.Entry<String, JsonNode>> iterator = propertiesNode.properties().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, JsonNode> entry = iterator.next();
			JsonNode propertyNode = entry.getValue();
			if (!"title".equals(propertyNode.path("type").asText())) {
				continue;
			}
			String titleText = joinPlainText(propertyNode.path("title"));
			if (trimToNull(titleText) != null) {
				return titleText;
			}
		}
		return "";
	}

	// 페이지가 archived/in_trash 상태면 삭제여부를 Y로 반환합니다.
	private String resolveDeleteFlag(JsonNode pageNode) {
		boolean archived = pageNode.path("archived").asBoolean(false);
		boolean inTrash = pageNode.path("in_trash").asBoolean(false);
		return (archived || inTrash) ? "Y" : "N";
	}

	// ISO 오프셋 일시 문자열을 LocalDateTime으로 변환합니다.
	private LocalDateTime parseOffsetDateTime(String isoDateTimeText) {
		String normalized = trimToNull(isoDateTimeText);
		if (normalized == null) {
			return null;
		}
		try {
			return OffsetDateTime.parse(normalized).toLocalDateTime();
		} catch (Exception ignored) {
			return null;
		}
	}

	// 블록 목록에서 plain_text를 줄 단위로 추출해 NOTES 문자열로 결합합니다.
	private String extractNotes(List<JsonNode> blockNodes) {
		if (blockNodes == null || blockNodes.isEmpty()) {
			return "";
		}

		List<String> lines = new ArrayList<>();
		for (JsonNode blockNode : blockNodes) {
			List<String> texts = new ArrayList<>();
			collectPlainText(blockNode, texts);
			String line = trimToNull(String.join("", texts));
			if (line != null) {
				lines.add(line);
			}
		}
		return String.join("\n", lines);
	}

	// JsonNode를 순회하며 plain_text 필드를 재귀적으로 수집합니다.
	private void collectPlainText(JsonNode node, List<String> collector) {
		if (node == null || node.isNull()) {
			return;
		}

		if (node.isObject()) {
			Iterator<Map.Entry<String, JsonNode>> iterator = node.properties().iterator();
			while (iterator.hasNext()) {
				Map.Entry<String, JsonNode> entry = iterator.next();
				if ("plain_text".equals(entry.getKey()) && entry.getValue().isTextual()) {
					collector.add(entry.getValue().asText(""));
					continue;
				}
				collectPlainText(entry.getValue(), collector);
			}
			return;
		}

		if (node.isArray()) {
			for (JsonNode child : node) {
				collectPlainText(child, collector);
			}
		}
	}

	// properties에서 category_id 성격의 필드를 찾아 문자열로 반환합니다.
	private String extractCategoryId(JsonNode propertiesNode) {
		if (propertiesNode == null || !propertiesNode.isObject()) {
			return null;
		}

		JsonNode categoryNode = propertiesNode.path("Category");
		if (categoryNode.isObject()) {
			JsonNode multiSelectNode = categoryNode.path("multi_select");
			if (multiSelectNode.isArray() && multiSelectNode.size() > 0) {
				return trimToNull(multiSelectNode.get(0).path("id").asText(null));
			}
		}

		Iterator<Map.Entry<String, JsonNode>> iterator = propertiesNode.properties().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, JsonNode> entry = iterator.next();
			String propertyName = trimToNull(entry.getKey());
			if (propertyName == null || !propertyName.equalsIgnoreCase("category_id")) {
				continue;
			}

			JsonNode propertyNode = entry.getValue();
			String propertyType = trimToNull(propertyNode.path("type").asText(null));
			if (propertyType == null) {
				continue;
			}
			if ("rich_text".equals(propertyType)) {
				return trimToNull(joinPlainText(propertyNode.path("rich_text")));
			}
			if ("title".equals(propertyType)) {
				return trimToNull(joinPlainText(propertyNode.path("title")));
			}
			if ("number".equals(propertyType) && !propertyNode.path("number").isMissingNode()) {
				return trimToNull(propertyNode.path("number").asText(null));
			}
			if ("select".equals(propertyType)) {
				return firstNonNull(
					trimToNull(propertyNode.path("select").path("id").asText(null)),
					trimToNull(propertyNode.path("select").path("name").asText(null))
				);
			}
			if ("relation".equals(propertyType) && propertyNode.path("relation").isArray() && propertyNode.path("relation").size() > 0) {
				return trimToNull(propertyNode.path("relation").get(0).path("id").asText(null));
			}
		}
		return null;
	}

	// properties.Category.multi_select에서 카테고리 id/name/color 목록을 추출합니다.
	private List<NotionCategoryUpsertPO> extractCategoryRows(JsonNode propertiesNode) {
		List<NotionCategoryUpsertPO> rows = new ArrayList<>();
		if (propertiesNode == null || !propertiesNode.isObject()) {
			return rows;
		}

		JsonNode categoryNode = propertiesNode.path("Category");
		if (categoryNode.isObject() && "multi_select".equals(categoryNode.path("type").asText())) {
			appendCategoryRowsFromMultiSelect(categoryNode.path("multi_select"), rows);
		}

		if (!rows.isEmpty()) {
			return rows;
		}

		Iterator<Map.Entry<String, JsonNode>> iterator = propertiesNode.properties().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, JsonNode> entry = iterator.next();
			JsonNode propertyNode = entry.getValue();
			if (!propertyNode.isObject()) {
				continue;
			}
			if (!"multi_select".equals(propertyNode.path("type").asText())) {
				continue;
			}
			appendCategoryRowsFromMultiSelect(propertyNode.path("multi_select"), rows);
			if (!rows.isEmpty()) {
				return rows;
			}
		}
		return rows;
	}

	// multi_select 배열을 순회해 카테고리 upsert 목록을 생성합니다.
	private void appendCategoryRowsFromMultiSelect(JsonNode multiSelectNode, List<NotionCategoryUpsertPO> collector) {
		if (multiSelectNode == null || !multiSelectNode.isArray()) {
			return;
		}

		for (JsonNode categoryItemNode : multiSelectNode) {
			String categoryId = trimToNull(categoryItemNode.path("id").asText(null));
			if (categoryId == null) {
				continue;
			}

			NotionCategoryUpsertPO row = new NotionCategoryUpsertPO();
			row.setCategoryId(limitLength(categoryId, MAX_ID_LENGTH));
			row.setCategoryNm(limitLength(safeValue(trimToNull(categoryItemNode.path("name").asText(null))), MAX_CATEGORY_NM_LENGTH));
			row.setColor(limitLength(safeValue(trimToNull(categoryItemNode.path("color").asText(null))), MAX_CATEGORY_COLOR_LENGTH));
			collector.add(row);
		}
	}

	// CATEGORY_ID 기준으로 중복을 제거하고 마지막 값을 유지합니다.
	private List<NotionCategoryUpsertPO> deduplicateCategories(List<NotionCategoryUpsertPO> rows) {
		Map<String, NotionCategoryUpsertPO> rowMap = new LinkedHashMap<>();
		for (NotionCategoryUpsertPO row : rows) {
			if (row == null || trimToNull(row.getCategoryId()) == null) {
				continue;
			}
			rowMap.put(row.getCategoryId(), row);
		}
		return new ArrayList<>(rowMap.values());
	}

	// properties에서 URL 타입 속성값을 찾아 URL 컬럼 저장값으로 반환합니다.
	private String extractPageUrlProperty(JsonNode propertiesNode) {
		if (propertiesNode == null || !propertiesNode.isObject()) {
			return "";
		}

		JsonNode urlProperty = propertiesNode.path("URL");
		if (urlProperty.isObject() && "url".equals(urlProperty.path("type").asText())) {
			return safeValue(trimToNull(urlProperty.path("url").asText(null)));
		}

		Iterator<Map.Entry<String, JsonNode>> iterator = propertiesNode.properties().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, JsonNode> entry = iterator.next();
			JsonNode propertyNode = entry.getValue();
			if (!propertyNode.isObject()) {
				continue;
			}

			if (!"url".equals(propertyNode.path("type").asText())) {
				continue;
			}

			return safeValue(trimToNull(propertyNode.path("url").asText(null)));
		}
		return "";
	}

	// rich_text/title 배열에서 plain_text를 결합합니다.
	private String joinPlainText(JsonNode richTextNode) {
		if (richTextNode == null || !richTextNode.isArray()) {
			return "";
		}

		StringBuilder textBuilder = new StringBuilder();
		for (JsonNode textNode : richTextNode) {
			textBuilder.append(textNode.path("plain_text").asText(""));
		}
		return textBuilder.toString();
	}

	// 앞에서부터 null이 아닌 값을 반환합니다.
	private String firstNonNull(String... values) {
		for (String value : values) {
			String normalized = trimToNull(value);
			if (normalized != null) {
				return normalized;
			}
		}
		return null;
	}

	// 문자열 길이를 컬럼 길이 제한에 맞춰 자릅니다.
	private String limitLength(String value, int maxLength) {
		String normalized = safeValue(value);
		if (normalized.length() <= maxLength) {
			return normalized;
		}
		return normalized.substring(0, maxLength);
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
