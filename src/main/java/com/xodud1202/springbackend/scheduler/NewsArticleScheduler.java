package com.xodud1202.springbackend.scheduler;

import com.xodud1202.springbackend.domain.news.NewsListPressShardSnapshotPublishResultVO;
import com.xodud1202.springbackend.service.NewsService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
// 매 30분 10초에 뉴스 JSON 스냅샷 발행 배치를 실행하는 스케줄러입니다.
public class NewsArticleScheduler {
	private static final Duration DEFAULT_SCHEDULER_TIMEOUT = Duration.ofMinutes(10);

	private final NewsService newsService;
	private final Duration schedulerTimeout;
	private final Object schedulerExecutorLock = new Object();
	private ExecutorService schedulerWorkerExecutor;

	// 운영 기본 10분 타임아웃 설정으로 뉴스 스케줄러를 생성합니다.
	@Autowired
	public NewsArticleScheduler(NewsService newsService) {
		this(newsService, DEFAULT_SCHEDULER_TIMEOUT);
	}

	// 테스트와 확장 시 사용할 타임아웃 설정으로 뉴스 스케줄러를 생성합니다.
	NewsArticleScheduler(NewsService newsService, Duration schedulerTimeout) {
		this.newsService = newsService;
		this.schedulerTimeout = schedulerTimeout == null ? DEFAULT_SCHEDULER_TIMEOUT : schedulerTimeout;
		this.schedulerWorkerExecutor = createSchedulerWorkerExecutor();
	}

	@Scheduled(cron = "10 0,30 * * * *", zone = "Asia/Seoul")
	// 30분 주기 배치로 뉴스 메타+언론사 shard JSON 파일 생성/업로드를 실행합니다.
	public void collectNewsArticleEveryThirtyMinutes() {
		// 타임아웃 제어가 가능한 전용 워커에서 뉴스 발행 작업을 실행합니다.
		log.info(
			"뉴스 메타+언론사 shard JSON 업로드 시작 timeoutMinutes={}, timeoutMillis={}",
			schedulerTimeout.toMinutes(),
			schedulerTimeout.toMillis()
		);
		Future<NewsListPressShardSnapshotPublishResultVO> publishFuture = getSchedulerWorkerExecutor().submit(
			newsService::publishNewsListPressShardJsonSnapshot
		);
		try {
			NewsListPressShardSnapshotPublishResultVO publishResult = publishFuture.get(
				schedulerTimeout.toMillis(),
				TimeUnit.MILLISECONDS
			);
			log.info(
				"뉴스 메타+언론사 shard JSON 업로드 완료 baseTargetPath={}, metaFileName={}, pressShardCount={}, shardSuccessCount={}, shardFailedCount={}, metaJsonByteSize={}, totalShardJsonByteSize={}",
				publishResult.getBaseTargetPath(),
				publishResult.getMetaFileName(),
				publishResult.getPressShardCount(),
				publishResult.getShardSuccessCount(),
				publishResult.getShardFailedCount(),
				publishResult.getMetaJsonByteSize(),
				publishResult.getTotalShardJsonByteSize()
			);
		} catch (TimeoutException exception) {
			// 제한 시간을 초과한 워커를 취소하고 다음 실행용 워커를 새로 준비합니다.
			publishFuture.cancel(true);
			replaceSchedulerWorkerExecutor();
			log.error(
				"뉴스 메타+언론사 shard JSON 업로드 시간 초과 timeoutMinutes={}, timeoutMillis={}",
				schedulerTimeout.toMinutes(),
				schedulerTimeout.toMillis(),
				exception
			);
		} catch (InterruptedException exception) {
			// 스케줄러 대기 스레드 인터럽트는 즉시 복구하고 워커 취소를 시도합니다.
			publishFuture.cancel(true);
			Thread.currentThread().interrupt();
			log.error("뉴스 메타+언론사 shard JSON 업로드 대기 중 인터럽트가 발생했습니다. message={}", exception.getMessage(), exception);
		} catch (ExecutionException exception) {
			// 워커 내부 예외는 원인 예외 기준으로 기록합니다.
			Throwable cause = exception.getCause() == null ? exception : exception.getCause();
			log.error("뉴스 메타+언론사 shard JSON 업로드 실패 message={}", cause.getMessage(), cause);
		}
	}

	@PreDestroy
	// 애플리케이션 종료 시 뉴스 스케줄러 전용 워커를 정리합니다.
	public void shutdownSchedulerWorkerExecutor() {
		ExecutorService workerExecutor = null;
		synchronized (schedulerExecutorLock) {
			workerExecutor = schedulerWorkerExecutor;
			schedulerWorkerExecutor = null;
		}
		if (workerExecutor != null) {
			workerExecutor.shutdownNow();
		}
	}

	// 현재 사용할 뉴스 스케줄러 전용 워커를 반환합니다.
	private ExecutorService getSchedulerWorkerExecutor() {
		synchronized (schedulerExecutorLock) {
			if (schedulerWorkerExecutor == null || schedulerWorkerExecutor.isShutdown()) {
				schedulerWorkerExecutor = createSchedulerWorkerExecutor();
			}
			return schedulerWorkerExecutor;
		}
	}

	// 시간 초과 후 다음 실행을 위해 뉴스 스케줄러 워커를 교체합니다.
	private void replaceSchedulerWorkerExecutor() {
		ExecutorService previousWorkerExecutor;
		synchronized (schedulerExecutorLock) {
			previousWorkerExecutor = schedulerWorkerExecutor;
			schedulerWorkerExecutor = createSchedulerWorkerExecutor();
		}
		if (previousWorkerExecutor != null) {
			previousWorkerExecutor.shutdownNow();
		}
	}

	// 뉴스 스케줄러 전용 단일 워커 실행기를 생성합니다.
	private ExecutorService createSchedulerWorkerExecutor() {
		return Executors.newSingleThreadExecutor(buildSchedulerWorkerThreadFactory());
	}

	// 뉴스 스케줄러 전용 워커 스레드 생성 규칙을 반환합니다.
	private ThreadFactory buildSchedulerWorkerThreadFactory() {
		return runnable -> {
			Thread thread = new Thread(runnable, "news-article-scheduler-worker");
			thread.setDaemon(true);
			return thread;
		};
	}
}
