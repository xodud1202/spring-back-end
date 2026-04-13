package com.xodud1202.springbackend.domain.snippet;

// 스니펫 저장 완료 응답을 전달합니다.
public record SnippetSaveResponse(
	Long snippetNo,
	String message
) {
}
