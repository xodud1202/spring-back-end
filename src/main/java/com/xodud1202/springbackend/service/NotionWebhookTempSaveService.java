package com.xodud1202.springbackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xodud1202.springbackend.domain.notion.NotionWebhookTempEntryPO;
import com.xodud1202.springbackend.mapper.NotionWebhookMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
// Notion 웹훅 요청 원문을 KEY_VALUE_TEMP_TABLE에 저장하는 서비스입니다.
public class NotionWebhookTempSaveService {
	private static final String HEADER_PREFIX = "HEADER.";
	private static final String QUERY_PREFIX = "QUERY.";
	private static final String BODY_PREFIX = "BODY.";
	private static final String BODY_RAW_KEY = "BODY_RAW";

	private final NotionWebhookMapper notionWebhookMapper;
	private final ObjectMapper objectMapper;

	@Transactional
	// 웹훅 요청의 헤더/파라미터/바디 정보를 키-값 형태로 분해해 저장합니다.
	public int saveWebhookRequest(
		String requestUrl,
		Map<String, String> headerMap,
		Map<String, String[]> parameterMap,
		String requestBody
	) {
		// 저장할 키-값 엔트리를 순서대로 누적합니다.
		List<NotionWebhookTempEntryPO> rows = new ArrayList<>();
		appendHeaderRows(rows, requestUrl, headerMap);
		appendQueryRows(rows, requestUrl, parameterMap);
		appendBodyRows(rows, requestUrl, requestBody);

		// 저장 대상이 없으면 DB 작업 없이 종료합니다.
		if (rows.isEmpty()) {
			return 0;
		}

		// 누적된 엔트리를 일괄 저장합니다.
		return notionWebhookMapper.insertNotionWebhookTempBatch(rows);
	}

	// 요청 헤더를 저장용 엔트리로 변환합니다.
	private void appendHeaderRows(List<NotionWebhookTempEntryPO> rows, String requestUrl, Map<String, String> headerMap) {
		if (headerMap == null || headerMap.isEmpty()) {
			return;
		}

		for (Map.Entry<String, String> entry : headerMap.entrySet()) {
			String headerName = trimToNull(entry.getKey());
			if (headerName == null) {
				continue;
			}
			rows.add(buildRow(requestUrl, HEADER_PREFIX + headerName, safeValue(entry.getValue())));
		}
	}

	// 요청 쿼리 파라미터를 저장용 엔트리로 변환합니다.
	private void appendQueryRows(List<NotionWebhookTempEntryPO> rows, String requestUrl, Map<String, String[]> parameterMap) {
		if (parameterMap == null || parameterMap.isEmpty()) {
			return;
		}

		for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
			String paramName = trimToNull(entry.getKey());
			if (paramName == null) {
				continue;
			}

			String[] values = entry.getValue();
			if (values == null || values.length == 0) {
				rows.add(buildRow(requestUrl, QUERY_PREFIX + paramName, ""));
				continue;
			}

			for (int index = 0; index < values.length; index += 1) {
				String key = values.length == 1
					? QUERY_PREFIX + paramName
					: QUERY_PREFIX + paramName + "[" + index + "]";
				rows.add(buildRow(requestUrl, key, safeValue(values[index])));
			}
		}
	}

	// 요청 바디를 raw 값과 JSON 경로 키로 분해해 저장용 엔트리로 변환합니다.
	private void appendBodyRows(List<NotionWebhookTempEntryPO> rows, String requestUrl, String requestBody) {
		String body = trimToNull(requestBody);
		if (body == null) {
			return;
		}

		rows.add(buildRow(requestUrl, BODY_RAW_KEY, body));

		try {
			JsonNode rootNode = objectMapper.readTree(body);
			flattenJsonNode(rows, requestUrl, BODY_PREFIX, rootNode);
		} catch (Exception ignored) {
			// JSON 파싱 실패 시 BODY_RAW만 저장하고 추가 처리하지 않습니다.
		}
	}

	// JSON 노드를 경로 기반 키로 평탄화해 저장용 엔트리로 변환합니다.
	private void flattenJsonNode(List<NotionWebhookTempEntryPO> rows, String requestUrl, String prefix, JsonNode node) {
		if (node == null || node.isNull()) {
			return;
		}

		if (node.isObject()) {
			for (Map.Entry<String, JsonNode> field : node.properties()) {
				String name = trimToNull(field.getKey());
				if (name == null) {
					continue;
				}
				flattenJsonNode(rows, requestUrl, prefix + name + ".", field.getValue());
			}
			return;
		}

		if (node.isArray()) {
			for (int index = 0; index < node.size(); index += 1) {
				flattenJsonNode(rows, requestUrl, prefix + "[" + index + "].", node.get(index));
			}
			return;
		}

		String key = removeTrailingDot(prefix);
		rows.add(buildRow(requestUrl, key, node.asText("")));
	}

	// 저장용 단일 엔트리를 생성합니다.
	private NotionWebhookTempEntryPO buildRow(String requestUrl, String key, String value) {
		NotionWebhookTempEntryPO row = new NotionWebhookTempEntryPO();
		row.setRequestUrl(safeValue(requestUrl));
		row.setTempKey(safeValue(key));
		row.setTempValue(safeValue(value));
		return row;
	}

	// 문자열 끝의 구분 점(.)을 제거합니다.
	private String removeTrailingDot(String value) {
		if (value == null || value.isEmpty()) {
			return "";
		}
		return value.endsWith(".") ? value.substring(0, value.length() - 1) : value;
	}

	// 문자열을 trim 처리하고 비어 있으면 null로 변환합니다.
	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	// DB 저장용 문자열 null 안전값을 반환합니다.
	private String safeValue(String value) {
		return value == null ? "" : value;
	}
}
