package com.xodud1202.springbackend.service;

import static com.xodud1202.springbackend.common.util.CommonTextUtils.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xodud1202.springbackend.config.properties.DiningBrandsGroupJiraProperties;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Component
// 다이닝브랜즈그룹 Jira API 호출 기능을 제공합니다.
public class DiningBrandsGroupJiraApiClient {
	private static final int HTTP_TIMEOUT_SECONDS = 15;
	private static final String JIRA_FIELDS_QUERY = "summary,description,priority,created,reporter,attachment";

	private final ObjectMapper objectMapper;
	private final DiningBrandsGroupJiraProperties jiraProperties;
	private final HttpClient httpClient = HttpClient.newBuilder()
		.followRedirects(HttpClient.Redirect.NORMAL)
		.build();

	// 생성자에서 Jira 인증 설정과 JSON 파서를 주입받습니다.
	public DiningBrandsGroupJiraApiClient(
		ObjectMapper objectMapper,
		DiningBrandsGroupJiraProperties jiraProperties
	) {
		this.objectMapper = objectMapper;
		this.jiraProperties = jiraProperties;
	}

	// 업무 키 기준 Jira 이슈 상세 정보를 조회합니다.
	public JsonNode getIssue(String apiBaseUrl, String workKey) {
		// 필수 설정과 입력값을 먼저 검증합니다.
		String normalizedApiBaseUrl = trimToNull(apiBaseUrl);
		String normalizedWorkKey = trimToNull(workKey);
		validateApiSettings();
		if (normalizedApiBaseUrl == null) {
			throw new IllegalArgumentException("회사 API URL을 확인해주세요.");
		}
		if (normalizedWorkKey == null) {
			throw new IllegalArgumentException("업무키를 입력해주세요.");
		}

		try {
			// Jira 이슈 상세 조회 요청을 생성합니다.
			String requestUrl = buildIssueRequestUrl(normalizedApiBaseUrl, normalizedWorkKey);
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(requestUrl))
				.timeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
				.header("Authorization", "Basic " + buildBasicAuthorizationValue())
				.header("Accept", "application/json")
				.GET()
				.build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			validateResponse(response);
			return objectMapper.readTree(safeValue(response.body()));
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Jira API 호출이 중단되었습니다.", exception);
		} catch (IllegalArgumentException exception) {
			throw exception;
		} catch (IllegalStateException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new IllegalStateException("Jira API 호출에 실패했습니다.", exception);
		}
	}

	// Jira 첨부파일 URL을 인증 헤더와 함께 다운로드합니다.
	public DiningBrandsGroupJiraAttachmentDownloadResult downloadAttachment(String attachmentUrl) {
		// 첨부 URL과 인증 설정을 먼저 확인합니다.
		String normalizedAttachmentUrl = trimToNull(attachmentUrl);
		validateApiSettings();
		if (normalizedAttachmentUrl == null) {
			throw new IllegalArgumentException("Jira 첨부파일 URL을 확인해주세요.");
		}

		try {
			// Jira 첨부 URL을 Basic 인증으로 직접 조회합니다.
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(normalizedAttachmentUrl))
				.timeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
				.header("Authorization", "Basic " + buildBasicAuthorizationValue())
				.header("Accept", "*/*")
				.GET()
				.build();
			HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
			validateAttachmentResponse(response);

			// 응답 헤더와 바이트 길이 기준으로 다운로드 결과를 구성합니다.
			byte[] responseBody = response.body() == null ? new byte[0] : response.body();
			String contentType = response.headers().firstValue("content-type").orElse("");
			long contentLength = response.headers()
				.firstValue("content-length")
				.map((headerValue) -> {
					try {
						return Long.parseLong(headerValue);
					} catch (NumberFormatException ignoredException) {
						return (long) responseBody.length;
					}
				})
				.orElse((long) responseBody.length);
			return new DiningBrandsGroupJiraAttachmentDownloadResult(responseBody, contentType, contentLength);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Jira 첨부파일 다운로드가 중단되었습니다.", exception);
		} catch (IllegalArgumentException exception) {
			throw exception;
		} catch (IllegalStateException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new IllegalStateException("Jira 첨부파일 다운로드에 실패했습니다.", exception);
		}
	}

	// Jira 이슈 상세 조회 URL을 생성합니다.
	private String buildIssueRequestUrl(String apiBaseUrl, String workKey) {
		// API URL 끝 경로 구분자를 보정한 뒤 쿼리를 조합합니다.
		String normalizedApiBaseUrl = apiBaseUrl.endsWith("/") ? apiBaseUrl : apiBaseUrl + "/";
		String encodedWorkKey = URLEncoder.encode(workKey, StandardCharsets.UTF_8);
		String encodedFields = URLEncoder.encode(JIRA_FIELDS_QUERY, StandardCharsets.UTF_8);
		return normalizedApiBaseUrl + encodedWorkKey + "?fields=" + encodedFields;
	}

	// Jira API 응답 상태코드를 검증합니다.
	private void validateResponse(HttpResponse<String> response) {
		// 성공 응답이 아니면 상태코드별 안내 메시지를 반환합니다.
		int statusCode = response == null ? 0 : response.statusCode();
		String responseBody = response == null ? "" : safeValue(response.body());
		if (statusCode >= 200 && statusCode < 300) {
			return;
		}
		if (statusCode == 401 || statusCode == 403) {
			throw new IllegalStateException("다이닝브랜즈그룹 Jira 인증에 실패했습니다.");
		}
		if (statusCode == 404) {
			throw new IllegalArgumentException("Jira 업무를 조회할 수 없습니다. 이슈가 존재하지 않거나 권한이 없습니다.");
		}
		throw new IllegalStateException("Jira API 호출에 실패했습니다. status=" + statusCode + ", body=" + responseBody);
	}

	// Jira 첨부파일 다운로드 응답 상태코드를 검증합니다.
	private void validateAttachmentResponse(HttpResponse<byte[]> response) {
		// 성공 응답이 아니면 상태코드별 안내 메시지를 반환합니다.
		int statusCode = response == null ? 0 : response.statusCode();
		if (statusCode >= 200 && statusCode < 300) {
			return;
		}
		if (statusCode == 401 || statusCode == 403) {
			throw new IllegalStateException("다이닝브랜즈그룹 Jira 첨부 인증에 실패했습니다.");
		}
		if (statusCode == 404) {
			throw new IllegalArgumentException("Jira 첨부파일을 조회할 수 없습니다.");
		}
		throw new IllegalStateException("Jira 첨부파일 다운로드에 실패했습니다. status=" + statusCode);
	}

	// Jira 기본 인증 헤더 값을 생성합니다.
	private String buildBasicAuthorizationValue() {
		// 이메일과 토큰을 email:token 형식으로 조합해 Base64 인코딩합니다.
		String rawValue = safeValue(jiraProperties.email()) + ":" + safeValue(jiraProperties.token());
		return Base64.getEncoder().encodeToString(rawValue.getBytes(StandardCharsets.UTF_8));
	}

	// API 호출 전 필수 Jira 인증 설정을 확인합니다.
	private void validateApiSettings() {
		if (trimToNull(jiraProperties.email()) == null || trimToNull(jiraProperties.token()) == null) {
			throw new IllegalStateException("다이닝브랜즈그룹 Jira 인증 설정이 필요합니다.");
		}
	}
}
