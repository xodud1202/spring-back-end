package com.xodud1202.springbackend.domain.snippet;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 폴더 등록과 수정 요청을 전달합니다.
public record SnippetFolderSavePO(
	@NotBlank(message = "폴더명을 입력해주세요.")
	@Size(max = 60, message = "폴더명은 60자 이내로 입력해주세요.")
	String folderNm,
	@Size(max = 20, message = "폴더 색상값은 20자 이내로 입력해주세요.")
	String colorHex,
	Integer sortSeq
) {
}
