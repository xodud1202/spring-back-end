package com.xodud1202.springbackend.service;

import static com.xodud1202.springbackend.common.Constants.Shop.TOSS_API_BASE_URL;
import static com.xodud1202.springbackend.common.Constants.Shop.TOSS_API_VERSION;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
// Toss 결제 승인 API 호출을 처리합니다.
public class TossPaymentsClient {
	private final RestClient.Builder restClientBuilder;

	@Value("${toss.secret-key}")
	private String tossSecretKey;

	// Toss 결제 승인 API를 호출하고 원본 응답 문자열을 반환합니다.
	public String confirmPayment(String paymentKey, String orderId, Long amount) {
		// 승인 요청 본문을 Toss 규격에 맞게 구성합니다.
		Map<String, Object> requestBody = new LinkedHashMap<>();
		requestBody.put("paymentKey", paymentKey);
		requestBody.put("orderId", orderId);
		requestBody.put("amount", amount);

		// Basic 인증과 API 버전을 포함해 승인 API를 호출합니다.
		try {
			return restClientBuilder
				.baseUrl(TOSS_API_BASE_URL)
				.build()
				.post()
				.uri("/v1/payments/confirm")
				.header(HttpHeaders.AUTHORIZATION, buildAuthorizationHeader())
				.header("TossPayments-API-Version", TOSS_API_VERSION)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(requestBody)
				.retrieve()
				.body(String.class);
		} catch (RestClientResponseException exception) {
			throw new TossPaymentClientException(
				exception.getStatusCode().value(),
				exception.getResponseBodyAsString(),
				exception
			);
		}
	}

	// Toss 결제 취소 API를 호출하고 원본 응답 문자열을 반환합니다.
	public String cancelPayment(String paymentKey, String cancelReason, Long cancelAmount) {
		// 취소 사유와 부분취소 금액을 Toss 규격에 맞게 구성합니다.
		Map<String, Object> requestBody = new LinkedHashMap<>();
		requestBody.put("cancelReason", cancelReason);
		if (cancelAmount != null && cancelAmount > 0L) {
			requestBody.put("cancelAmount", cancelAmount);
		}

		// Basic 인증과 API 버전을 포함해 취소 API를 호출합니다.
		try {
			return restClientBuilder
				.baseUrl(TOSS_API_BASE_URL)
				.build()
				.post()
				.uri("/v1/payments/{paymentKey}/cancel", paymentKey)
				.header(HttpHeaders.AUTHORIZATION, buildAuthorizationHeader())
				.header("TossPayments-API-Version", TOSS_API_VERSION)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(requestBody)
				.retrieve()
				.body(String.class);
		} catch (RestClientResponseException exception) {
			throw new TossPaymentClientException(
				exception.getStatusCode().value(),
				exception.getResponseBodyAsString(),
				exception
			);
		}
	}

	// Toss 시크릿키 기준 Basic 인증 헤더 값을 생성합니다.
	private String buildAuthorizationHeader() {
		String source = tossSecretKey + ":";
		String encoded = Base64.getEncoder().encodeToString(source.getBytes(StandardCharsets.UTF_8));
		return "Basic " + encoded;
	}
}
