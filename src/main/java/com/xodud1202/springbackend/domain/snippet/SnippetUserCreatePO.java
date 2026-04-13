package com.xodud1202.springbackend.domain.snippet;

// 신규 스니펫 사용자를 등록할 때 사용하는 저장 명령입니다.
public record SnippetUserCreatePO(
	String googleSub,
	String email,
	String userNm,
	String profileImgUrl,
	Long regNo,
	Long udtNo
) {
}
