package com.xodud1202.springbackend.controller.bo;

import com.xodud1202.springbackend.domain.news.NewsListPressShardSnapshotPublishResultVO;
import com.xodud1202.springbackend.service.NewsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// 관리자 뉴스 컨트롤러의 파일 갱신 API 흐름을 검증하는 테스트입니다.
class AdminNewsControllerTests {
	@Mock
	private NewsService newsService;

	@Test
	@DisplayName("파일 갱신 API는 스냅샷 결과를 반환한다")
	// 스냅샷 업로드가 성공하면 200 응답과 스냅샷 결과를 반환하는지 확인합니다.
	void refreshNewsListJsonSnapshot_returnsOkWhenPublishSuccess() {
		// 컨트롤러와 서비스 응답 스텁을 준비합니다.
		AdminNewsController controller = new AdminNewsController(newsService);
		NewsListPressShardSnapshotPublishResultVO publishResult = NewsListPressShardSnapshotPublishResultVO.builder()
			.baseTargetPath("/HDD1/Media/nas/news")
			.metaFileName("meta.json")
			.pressShardCount(1)
			.shardSuccessCount(1)
			.shardFailedCount(0)
			.metaJsonByteSize(100)
			.totalShardJsonByteSize(200)
			.build();
		when(newsService.publishNewsListPressShardJsonSnapshot()).thenReturn(publishResult);

		// 파일 갱신 API를 호출합니다.
		ResponseEntity<Object> response = controller.refreshNewsListJsonSnapshot();

		// 정상 상태와 응답 본문, 서비스 호출을 검증합니다.
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo(publishResult);
		verify(newsService).publishNewsListPressShardJsonSnapshot();
	}

	@Test
	@DisplayName("파일 갱신 API는 스냅샷 실패 시 500을 반환한다")
	// 스냅샷 업로드가 실패하면 500을 반환하는지 확인합니다.
	void refreshNewsListJsonSnapshot_returns500WhenPublishFails() {
		// 컨트롤러와 스냅샷 실패 시나리오를 준비합니다.
		AdminNewsController controller = new AdminNewsController(newsService);
		when(newsService.publishNewsListPressShardJsonSnapshot()).thenThrow(new IllegalStateException("publish failed"));

		// 파일 갱신 API를 호출합니다.
		ResponseEntity<Object> response = controller.refreshNewsListJsonSnapshot();

		// 500 상태코드와 오류 본문 포함 여부를 검증합니다.
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getBody()).isNotNull();
		verify(newsService).publishNewsListPressShardJsonSnapshot();
	}
}
