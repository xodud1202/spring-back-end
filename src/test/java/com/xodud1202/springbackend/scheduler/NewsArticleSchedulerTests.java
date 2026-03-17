package com.xodud1202.springbackend.scheduler;

import com.xodud1202.springbackend.domain.news.NewsListPressShardSnapshotPublishResultVO;
import com.xodud1202.springbackend.service.NewsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// 뉴스 스케줄러 배치 흐름을 검증하는 테스트입니다.
class NewsArticleSchedulerTests {
	@Mock
	private NewsService newsService;

	@Test
	@DisplayName("스프링은 뉴스 스케줄러 생성자 주입 대상을 찾을 수 있다")
	// 스프링 컨텍스트가 뉴스 서비스 빈만으로 스케줄러 빈을 생성할 수 있는지 확인합니다.
	void newsArticleScheduler_canBeCreatedBySpringConstructorAutowiring() {
		// 테스트용 스프링 컨텍스트를 열고 필요한 빈 정의를 등록합니다.
		try (AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext()) {
			applicationContext.registerBean(NewsService.class, () -> mock(NewsService.class));
			applicationContext.register(NewsArticleScheduler.class);

			// 컨텍스트 초기화 후 스케줄러 빈 생성 여부를 검증합니다.
			applicationContext.refresh();
			assertThat(applicationContext.getBean(NewsArticleScheduler.class)).isNotNull();
		}
	}

	@Test
	@DisplayName("스케줄러는 뉴스 스냅샷 업로드를 실행한다")
	// 제한 시간 안에 정상 완료되면 스냅샷 업로드 호출이 수행되는지 확인합니다.
	void collectNewsArticleEveryThirtyMinutes_runsPublishOnly() {
		// 스케줄러 테스트 대상을 생성하고 업로드 응답을 스텁합니다.
		NewsArticleScheduler scheduler = new NewsArticleScheduler(newsService, Duration.ofSeconds(1));
		when(newsService.publishNewsListPressShardJsonSnapshot()).thenReturn(buildPublishResult());

		try {
			// 스케줄러 메서드를 실행합니다.
			scheduler.collectNewsArticleEveryThirtyMinutes();

			// 스냅샷 업로드가 호출되는지 검증합니다.
			verify(newsService).publishNewsListPressShardJsonSnapshot();
		} finally {
			scheduler.shutdownSchedulerWorkerExecutor();
		}
	}

	@Test
	@DisplayName("스케줄러는 스냅샷 업로드 실패를 내부에서 처리한다")
	// 워커 내부 예외 발생 시에도 메서드가 예외를 외부로 던지지 않는지 확인합니다.
	void collectNewsArticleEveryThirtyMinutes_handlesPublishFailureInternally() {
		// 스케줄러 테스트 대상을 생성하고 업로드 실패를 스텁합니다.
		NewsArticleScheduler scheduler = new NewsArticleScheduler(newsService, Duration.ofSeconds(1));
		when(newsService.publishNewsListPressShardJsonSnapshot()).thenThrow(new IllegalStateException("publish failed"));

		try {
			// 스케줄러 메서드를 실행합니다.
			scheduler.collectNewsArticleEveryThirtyMinutes();

			// 업로드 호출이 수행되는지 검증합니다.
			verify(newsService).publishNewsListPressShardJsonSnapshot();
		} finally {
			scheduler.shutdownSchedulerWorkerExecutor();
		}
	}

	@Test
	@DisplayName("스케줄러는 시간 초과 작업을 취소하고 다음 실행을 준비한다")
	// 시간 초과 시 워커 인터럽트와 다음 실행 가능 상태가 함께 보장되는지 확인합니다.
	void collectNewsArticleEveryThirtyMinutes_cancelsTimedOutTaskAndPreparesNextRun() throws Exception {
		CountDownLatch interruptedLatch = new CountDownLatch(1);
		AtomicInteger invocationCount = new AtomicInteger();
		NewsArticleScheduler scheduler = new NewsArticleScheduler(newsService, Duration.ofMillis(200));

		when(newsService.publishNewsListPressShardJsonSnapshot()).thenAnswer(invocation -> {
			if (invocationCount.getAndIncrement() == 0) {
				try {
					while (true) {
						Thread.sleep(50L);
					}
				} catch (InterruptedException exception) {
					interruptedLatch.countDown();
					Thread.currentThread().interrupt();
					throw new IllegalStateException("publish timeout", exception);
				}
			}
			return buildPublishResult();
		});

		try {
			// 첫 실행은 시간 초과로 취소되고 인터럽트가 전달되는지 확인합니다.
			scheduler.collectNewsArticleEveryThirtyMinutes();
			assertThat(interruptedLatch.await(2, TimeUnit.SECONDS)).isTrue();

			// 두 번째 실행은 새 워커에서 정상 완료되는지 확인합니다.
			scheduler.collectNewsArticleEveryThirtyMinutes();
			verify(newsService, times(2)).publishNewsListPressShardJsonSnapshot();
		} finally {
			scheduler.shutdownSchedulerWorkerExecutor();
		}
	}

	// 테스트용 성공 업로드 응답을 생성합니다.
	private NewsListPressShardSnapshotPublishResultVO buildPublishResult() {
		return NewsListPressShardSnapshotPublishResultVO.builder()
			.baseTargetPath("/HDD1/Media/nas/news")
			.metaFileName("meta.json")
			.pressShardCount(1)
			.shardSuccessCount(1)
			.shardFailedCount(0)
			.metaJsonByteSize(100)
			.totalShardJsonByteSize(200)
			.build();
	}
}
