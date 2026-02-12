package com.xodud1202.springbackend.domain.news;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
// 뉴스 수집 실행 결과 집계를 보관합니다.
public class NewsCollectResultVO {
	private int targetCount;
	private int successTargetCount;
	private int failedTargetCount;
	private int attemptedArticleCount;
	private int insertedArticleCount;
	private int skippedArticleCount;
}
