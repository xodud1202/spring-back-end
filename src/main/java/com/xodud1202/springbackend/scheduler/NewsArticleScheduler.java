package com.xodud1202.springbackend.scheduler;

import com.xodud1202.springbackend.domain.news.NewsCollectResultVO;
import com.xodud1202.springbackend.service.NewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
// 매 30분 10초에 뉴스 RSS 수집 배치를 실행합니다.
public class NewsArticleScheduler {
	private final NewsService newsService;

	@Scheduled(cron = "10 0,30 * * * *", zone = "Asia/Seoul")
	// 30분 주기 배치로 RSS 기사 수집을 실행하고 결과를 로그로 남깁니다.
	public void collectNewsArticleEveryThirtyMinutes() {
		// 수집 배치를 실행하고 집계 결과를 기록합니다.
		NewsCollectResultVO collectResult = newsService.collectNewsArticles();
		log.info(
			"뉴스 RSS 수집 완료 targetCount={}, successTargetCount={}, failedTargetCount={}, attemptedArticleCount={}, insertedArticleCount={}, skippedArticleCount={}",
			collectResult.getTargetCount(),
			collectResult.getSuccessTargetCount(),
			collectResult.getFailedTargetCount(),
			collectResult.getAttemptedArticleCount(),
			collectResult.getInsertedArticleCount(),
			collectResult.getSkippedArticleCount()
		);
	}
}
