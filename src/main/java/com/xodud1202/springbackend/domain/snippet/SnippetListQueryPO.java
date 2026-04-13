package com.xodud1202.springbackend.domain.snippet;

// 스니펫 목록 조회 조건을 전달합니다.
public record SnippetListQueryPO(
	String q,
	Long folderNo,
	Long tagNo,
	String languageCd,
	String favoriteYn,
	String includeBodyYn,
	String sortBy,
	String quickFilter,
	Integer page,
	Integer size,
	Integer offset
) {
}
