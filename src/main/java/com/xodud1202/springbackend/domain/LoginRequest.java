package com.xodud1202.springbackend.domain;

import jakarta.validation.constraints.NotBlank;

// 백오피스 로그인 요청 정보를 전달합니다.
public record LoginRequest(
	@NotBlank(message = "로그인 아이디를 입력해주세요.") String loginId,
	@NotBlank(message = "비밀번호를 입력해주세요.") String pwd,
	boolean rememberMe
) {
}
