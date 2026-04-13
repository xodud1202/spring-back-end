package com.xodud1202.springbackend.domain.snippet;

import jakarta.validation.constraints.NotBlank;

// 즐겨찾기 상태 변경 요청을 전달합니다.
public record SnippetFavoriteUpdateRequest(
	@NotBlank(message = "즐겨찾기 값을 확인해주세요.") String favoriteYn
) {
}
