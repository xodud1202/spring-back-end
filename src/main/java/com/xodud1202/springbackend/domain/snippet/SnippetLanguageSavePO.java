package com.xodud1202.springbackend.domain.snippet;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 언어 등록 요청을 전달합니다.
public record SnippetLanguageSavePO(
	@NotBlank(message = "언어명을 입력해주세요.")
	@Size(max = 50, message = "언어명은 50자 이내로 입력해주세요.")
	String languageNm,
	Integer sortSeq
) {
}
