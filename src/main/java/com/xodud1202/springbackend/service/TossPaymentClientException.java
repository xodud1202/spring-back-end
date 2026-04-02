package com.xodud1202.springbackend.service;

import lombok.Getter;

// Toss 결제 승인 API 호출 실패 정보를 전달합니다.
@Getter
public class TossPaymentClientException extends RuntimeException {
	// HTTP 상태코드를 반환합니다.
	private final int statusCode;
	// 응답 본문을 반환합니다.
	private final String responseBody;

	// HTTP 상태코드와 응답 본문으로 예외를 생성합니다.
	public TossPaymentClientException(int statusCode, String responseBody, Throwable cause) {
		super(responseBody, cause);
		this.statusCode = statusCode;
		this.responseBody = responseBody;
	}
}
