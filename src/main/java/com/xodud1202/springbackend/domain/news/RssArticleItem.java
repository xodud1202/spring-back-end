package com.xodud1202.springbackend.domain.news;

import java.time.LocalDateTime;

// RSS/Atom 피드에서 추출한 기사 원본 데이터를 표현합니다.
public record RssArticleItem(
	String guid,
	String link,
	String title,
	String summary,
	String thumbnailUrl,
	String authorNm,
	LocalDateTime publishedDt
) {
}
