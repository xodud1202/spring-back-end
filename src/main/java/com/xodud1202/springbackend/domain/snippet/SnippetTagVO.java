package com.xodud1202.springbackend.domain.snippet;

// 사용자별 태그 정보를 전달합니다.
public record SnippetTagVO(
	Long tagNo,
	Long snippetUserNo,
	String tagNm,
	String colorHex,
	Integer sortSeq,
	Long snippetCount
) {
}
