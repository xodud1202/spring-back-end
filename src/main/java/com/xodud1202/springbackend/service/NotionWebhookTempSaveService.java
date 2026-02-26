package com.xodud1202.springbackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xodud1202.springbackend.domain.notion.NotionWebhookTempEntryPO;
import com.xodud1202.springbackend.mapper.NotionWebhookMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
	private static final int MAX_TEMP_KEY_LENGTH = 255;
	private static final int MAX_TEMP_VALUE_LENGTH = 255;

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
			appendRow(rows, requestUrl, HEADER_PREFIX + headerName, safeValue(entry.getValue()));
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
				appendRow(rows, requestUrl, QUERY_PREFIX + paramName, "");
				continue;
			}

			for (int index = 0; index < values.length; index += 1) {
				String key = values.length == 1
					? QUERY_PREFIX + paramName
					: QUERY_PREFIX + paramName + "[" + index + "]";
				appendRow(rows, requestUrl, key, safeValue(values[index]));
			}
		}
	}

	// 요청 바디를 raw 값과 JSON 경로 키로 분해해 저장용 엔트리로 변환합니다.
	private void appendBodyRows(List<NotionWebhookTempEntryPO> rows, String requestUrl, String requestBody) {
		String body = trimToNull(requestBody);
		if (body == null) {
			return;
		}

		boolean parsedBody = false;
		try {
			JsonNode rootNode = objectMapper.readTree(body);
			flattenJsonNode(rows, requestUrl, BODY_PREFIX, rootNode);
			parsedBody = true;
		} catch (Exception ignored) {
			// JSON 파싱 실패 시 key=value 포맷 여부를 추가로 확인합니다.
			parsedBody = appendKeyValueTextRows(rows, requestUrl, body);
		}

		// BODY_RAW는 컬럼 길이를 초과하지 않을 때만 저장합니다.
		if (body.length() <= MAX_TEMP_VALUE_LENGTH) {
			appendRow(rows, requestUrl, BODY_RAW_KEY, body);
		} else if (!parsedBody) {
			appendRow(rows, requestUrl, BODY_RAW_KEY, body.substring(0, MAX_TEMP_VALUE_LENGTH));
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
		appendRow(rows, requestUrl, key, node.asText(""));
	}

	// 원문 텍스트가 key=value 포맷인지 확인해 BODY 하위 키로 저장합니다.
	private boolean appendKeyValueTextRows(List<NotionWebhookTempEntryPO> rows, String requestUrl, String rawText) {
		String normalizedText = trimToNull(rawText);
		if (normalizedText == null) {
			return false;
		}

		boolean appended = false;
		String[] pairs = normalizedText.split("&");
		for (String pair : pairs) {
			String token = trimToNull(pair);
			if (token == null) {
				continue;
			}

			int separatorIndex = token.indexOf('=');
			if (separatorIndex <= 0) {
				continue;
			}

			String rawKey = URLDecoder.decode(token.substring(0, separatorIndex), StandardCharsets.UTF_8);
			String decodedKey = trimToNull(rawKey);
			if (decodedKey == null) {
				continue;
			}

			String decodedValue = URLDecoder.decode(token.substring(separatorIndex + 1), StandardCharsets.UTF_8);
			appendRow(rows, requestUrl, BODY_PREFIX + decodedKey, decodedValue);
			appended = true;
		}
		return appended;
	}

	// 저장값 길이를 컬럼 제한에 맞춰 보정합니다.
	private void appendRow(List<NotionWebhookTempEntryPO> rows, String requestUrl, String key, String value) {
		String normalizedKey = normalizeKey(key);
		String normalizedValue = safeValue(value);
		String limitedValue = normalizedValue.length() > MAX_TEMP_VALUE_LENGTH
			? normalizedValue.substring(0, MAX_TEMP_VALUE_LENGTH)
			: normalizedValue;
		rows.add(buildSingleRow(requestUrl, normalizedKey, limitedValue));
	}

	// 저장용 단일 엔트리를 생성합니다.
	private NotionWebhookTempEntryPO buildSingleRow(String requestUrl, String key, String value) {
		NotionWebhookTempEntryPO row = new NotionWebhookTempEntryPO();
		row.setRequestUrl(safeValue(requestUrl));
		row.setTempKey(normalizeKey(key));
		row.setTempValue(safeValue(value));
		return row;
	}

	// TEMP_KEY 컬럼 길이에 맞춰 키 값을 정규화합니다.
	private String normalizeKey(String key) {
		String resolvedKey = safeValue(key);
		if (resolvedKey.length() <= MAX_TEMP_KEY_LENGTH) {
			return resolvedKey;
		}
		return resolvedKey.substring(0, MAX_TEMP_KEY_LENGTH);
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
