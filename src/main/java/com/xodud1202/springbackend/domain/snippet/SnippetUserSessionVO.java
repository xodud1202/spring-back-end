package com.xodud1202.springbackend.domain.snippet;

// 스니펫 로그인 사용자 세션 정보를 전달합니다.
public record SnippetUserSessionVO(
	Long snippetUserNo,
	String googleSub,
	String email,
	String userNm,
	String profileImgUrl,
	String useYn,
	String delYn
) {
}
