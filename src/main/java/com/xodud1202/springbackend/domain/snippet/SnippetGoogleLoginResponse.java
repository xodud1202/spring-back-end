package com.xodud1202.springbackend.domain.snippet;

// 스니펫 구글 로그인 응답을 전달합니다.
public record SnippetGoogleLoginResponse(
	boolean loginSuccess,
	String firstLoginYn,
	Long snippetUserNo,
	String userNm,
	String email,
	String profileImgUrl
) {
	// 로그인 성공 응답을 생성합니다.
	public static SnippetGoogleLoginResponse authenticated(
		String firstLoginYn,
		Long snippetUserNo,
		String userNm,
		String email,
		String profileImgUrl
	) {
		return new SnippetGoogleLoginResponse(true, firstLoginYn, snippetUserNo, userNm, email, profileImgUrl);
	}
}
