package com.xodud1202.springbackend.domain.snippet;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

// 스니펫 등록과 수정 요청을 전달합니다.
public record SnippetSavePO(
	Long folderNo,
	@NotBlank(message = "언어 코드를 선택해주세요.") String languageCd,
	@NotBlank(message = "제목을 입력해주세요.")
	@Size(max = 150, message = "제목은 150자 이내로 입력해주세요.")
	String title,
	@Size(max = 500, message = "요약은 500자 이내로 입력해주세요.")
	String summary,
	@NotBlank(message = "스니펫 본문을 입력해주세요.") String snippetBody,
	String memo,
	String favoriteYn,
	List<Long> tagNoList
) {
}
