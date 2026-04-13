package com.xodud1202.springbackend.domain.snippet;

import jakarta.validation.constraints.NotBlank;

// 구글 ID 토큰 로그인 요청을 전달합니다.
public record SnippetGoogleLoginRequest(
	@NotBlank(message = "구글 credential 값을 확인해주세요.") String credential,
	@NotBlank(message = "구글 클라이언트 ID 값을 확인해주세요.") String clientId
) {
}
