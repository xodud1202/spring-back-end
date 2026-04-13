package com.xodud1202.springbackend.domain.snippet;

// 스니펫 언어 마스터 정보를 전달합니다.
public record SnippetLanguageVO(
	String languageCd,
	String languageNm,
	String editorMode,
	Integer sortSeq
) {
}
