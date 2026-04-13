package com.xodud1202.springbackend.domain.snippet;

// 스니펫 세션 복구 응답을 전달합니다.
public record SnippetSessionRefreshResponse(
	boolean authenticated,
	Long snippetUserNo,
	String userNm,
	String email,
	String profileImgUrl
) {
	// 로그인 세션 복구 성공 응답을 생성합니다.
	public static SnippetSessionRefreshResponse authenticated(
		Long snippetUserNo,
		String userNm,
		String email,
		String profileImgUrl
	) {
		return new SnippetSessionRefreshResponse(true, snippetUserNo, userNm, email, profileImgUrl);
	}

	// 비로그인 상태 응답을 생성합니다.
	public static SnippetSessionRefreshResponse unauthenticated() {
		return new SnippetSessionRefreshResponse(false, null, "", "", "");
	}
}
