package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.admin.news.AdminNewsListQueryPO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsListRowVO;
import com.xodud1202.springbackend.mapper.NewsMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// 뉴스 서비스 단위 테스트입니다.
public class NewsServiceTest {
	@Mock
	private NewsMapper newsMapper;

	@Mock
	private RssFeedClient rssFeedClient;

	private NewsService newsService;

	// 테스트 대상 서비스를 초기화합니다.
	@BeforeEach
	void setUp() {
		newsService = new NewsService(newsMapper, rssFeedClient);
	}

	@Test
	@DisplayName("수집일시 기간 입력을 정규화한다")
	// 수집일시 기간 입력을 정규화하는지 확인합니다.
	void validateAdminNewsListQuery_normalizesDateTime() {
		AdminNewsListQueryPO param = new AdminNewsListQueryPO();
		param.setCollectedFrom("2026-02-18T10:00");
		param.setCollectedTo("2026-02-18T11:00");

		String message = newsService.validateAdminNewsListQuery(param);

		assertThat(message).isNull();
		assertThat(param.getCollectedFrom()).isEqualTo("2026-02-18 10:00:00");
		assertThat(param.getCollectedTo()).isEqualTo("2026-02-18 11:00:00");
	}

	@Test
	@DisplayName("수집 시작일시가 종료일시 이후면 오류를 반환한다")
	// 수집 시작일시가 종료일시보다 이후인 경우 오류를 반환하는지 확인합니다.
	void validateAdminNewsListQuery_invalidRange() {
		AdminNewsListQueryPO param = new AdminNewsListQueryPO();
		param.setCollectedFrom("2026-02-18T12:00");
		param.setCollectedTo("2026-02-18T11:00");

		String message = newsService.validateAdminNewsListQuery(param);

		assertThat(message).isEqualTo("수집 시작일시가 종료일시보다 이후입니다.");
	}

	@Test
	@DisplayName("페이지 정보를 포함한 목록 응답을 생성한다")
	// 페이지네이션 정보가 포함된 결과를 생성하는지 확인합니다.
	void getAdminNewsList_returnsPagedResponse() {
		AdminNewsListQueryPO param = new AdminNewsListQueryPO();
		param.setPage(2);

		when(newsMapper.getAdminNewsList(any(AdminNewsListQueryPO.class)))
			.thenReturn(Collections.singletonList(new AdminNewsListRowVO()));
		when(newsMapper.getAdminNewsListCount(any(AdminNewsListQueryPO.class)))
			.thenReturn(42);

		Map<String, Object> result = newsService.getAdminNewsList(param);

		assertThat(result.get("page")).isEqualTo(2);
		assertThat(result.get("pageSize")).isEqualTo(20);
		assertThat(result.get("totalCount")).isEqualTo(42);
		assertThat(result.get("list")).isInstanceOf(Iterable.class);
	}
}
