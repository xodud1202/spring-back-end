package com.xodud1202.springbackend.controller.Notion;

import com.xodud1202.springbackend.service.NotionWebhookTempSaveService;
import com.xodud1202.springbackend.service.NotionWebhookDataSyncService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
// Notion 웹훅 요청을 수신하고 요청 원문을 임시 테이블에 저장하는 컨트롤러입니다.
public class NotionApiController {
	private static final String WEBHOOK_PATH = "/api/notion/webhook";

	private final NotionWebhookTempSaveService notionWebhookTempSaveService;
	private final NotionWebhookDataSyncService notionWebhookDataSyncService;

	// Notion 웹훅 요청의 헤더/파라미터/바디를 KEY_VALUE_TEMP_TABLE에 저장합니다.
	@RequestMapping(value = WEBHOOK_PATH, method = { RequestMethod.GET, RequestMethod.POST })
	public ResponseEntity<Object> notionWebhook(HttpServletRequest request, @RequestBody(required = false) String requestBody) {
		try {
			// 요청 헤더를 모두 순회해 저장용 맵으로 구성합니다.
			Map<String, String> headerMap = new LinkedHashMap<>();
			Enumeration<String> headerNames = request.getHeaderNames();
			while (headerNames != null && headerNames.hasMoreElements()) {
				String headerName = headerNames.nextElement();
				headerMap.put(headerName, request.getHeader(headerName));
			}

			// 서비스에 전달해 웹훅 원문을 임시 저장합니다.
			int savedCount = notionWebhookTempSaveService.saveWebhookRequest(
				WEBHOOK_PATH,
				headerMap,
				request.getParameterMap(),
				requestBody
			);
			int syncedCount = notionWebhookDataSyncService.syncNotionDataFromWebhook(requestBody, headerMap);

			// 임시 저장 건수와 본문 동기화 건수를 포함한 성공 응답을 반환합니다.
			return ResponseEntity.ok(Map.of("message", "ok", "savedCount", savedCount, "syncedCount", syncedCount));
		} catch (SecurityException securityException) {
			// 서명 검증 실패 시 401 응답을 반환합니다.
			log.warn("Notion 웹훅 서명 검증 실패 message={}", securityException.getMessage());
			return ResponseEntity.status(401).body(Map.of("message", "웹훅 서명 검증 실패"));
		} catch (Exception exception) {
			// 처리 실패 시 에러 로그와 500 응답을 반환합니다.
			log.error("Notion 웹훅 처리 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "웹훅 처리 실패"));
		}
	}
}
