package com.xodud1202.springbackend.common.response;

// 단일 message 필드 응답을 전달합니다.
public record ApiMessageResponse(
	String message
) {
}
