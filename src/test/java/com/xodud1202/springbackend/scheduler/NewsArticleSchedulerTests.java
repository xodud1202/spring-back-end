package com.xodud1202.springbackend.scheduler;

import com.xodud1202.springbackend.domain.news.NewsListPressShardSnapshotPublishResultVO;
import com.xodud1202.springbackend.service.NewsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// 뉴스 스케줄러 배치 흐름을 검증하는 테스트입니다.
class NewsArticleSchedulerTests {
	@Mock
	private NewsService newsService;

	@Test
	@DisplayName("스케줄러는 뉴스 스냅샷 업로드를 실행한다")
	// 스케줄러 실행 시 스냅샷 업로드 호출이 수행되는지 확인합니다.
	void collectNewsArticleEveryThirtyMinutes_runsPublishOnly() {
		// 스케줄러 테스트 대상을 생성하고 업로드 응답을 스텁합니다.
		NewsArticleScheduler scheduler = new NewsArticleScheduler(newsService);
		when(newsService.publishNewsListPressShardJsonSnapshot()).thenReturn(
			NewsListPressShardSnapshotPublishResultVO.builder()
				.baseTargetPath("/HDD1/Media/nas/news")
				.metaFileName("meta.json")
				.pressShardCount(1)
				.shardSuccessCount(1)
				.shardFailedCount(0)
				.metaJsonByteSize(100)
				.totalShardJsonByteSize(200)
				.build()
		);

		// 스케줄러 메서드를 실행합니다.
		scheduler.collectNewsArticleEveryThirtyMinutes();

		// 스냅샷 업로드가 호출되는지 검증합니다.
		verify(newsService).publishNewsListPressShardJsonSnapshot();
	}

	@Test
	@DisplayName("스케줄러는 스냅샷 업로드 실패를 내부에서 처리한다")
	// 업로드 예외 발생 시에도 메서드가 예외를 외부로 던지지 않는지 확인합니다.
	void collectNewsArticleEveryThirtyMinutes_handlesPublishFailureInternally() {
		// 스케줄러 테스트 대상을 생성하고 업로드 실패를 스텁합니다.
		NewsArticleScheduler scheduler = new NewsArticleScheduler(newsService);
		when(newsService.publishNewsListPressShardJsonSnapshot()).thenThrow(new IllegalStateException("publish failed"));

		// 스케줄러 메서드를 실행합니다.
		scheduler.collectNewsArticleEveryThirtyMinutes();

		// 업로드 호출이 수행되는지 검증합니다.
		verify(newsService).publishNewsListPressShardJsonSnapshot();
	}
}
