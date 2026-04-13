package com.xodud1202.springbackend.domain.snippet;

import java.util.List;

// 스니펫 목록 화면 응답을 전달합니다.
public record SnippetListResponse(
	List<SnippetSummaryVO> list,
	int totalCount,
	int page,
	int size
) {
}
