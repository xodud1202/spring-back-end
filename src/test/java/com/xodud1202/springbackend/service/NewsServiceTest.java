package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.admin.news.AdminNewsListQueryPO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsListRowVO;
import com.xodud1202.springbackend.domain.news.NewsCategorySummaryVO;
import com.xodud1202.springbackend.domain.news.NewsPressSummaryVO;
import com.xodud1202.springbackend.domain.news.NewsSnapshotVO;
import com.xodud1202.springbackend.domain.news.NewsTopArticleVO;
import com.xodud1202.springbackend.mapper.NewsMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
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

	@Test
	@DisplayName("뉴스 스냅샷은 유효한 저장 선택값을 그대로 사용한다")
	// 유효한 언론사/카테고리 저장값이 있으면 fallback 없이 스냅샷을 구성하는지 확인합니다.
	void getNewsSnapshot_usesRequestedSelectionWhenValid() {
		List<NewsPressSummaryVO> pressList = List.of(createPress("1", "JTBC"), createPress("2", "MBC"));
		List<NewsCategorySummaryVO> categoryList = List.of(createCategory("breaking", "속보"), createCategory("politics", "정치"));
		List<NewsTopArticleVO> articleList = List.of(createArticle("101", "기사1"));

		when(newsMapper.getActivePressList()).thenReturn(pressList);
		when(newsMapper.getActiveCategoryListByPressNo(1L)).thenReturn(categoryList);
		when(newsMapper.getTopArticleListByPressNoAndCategoryCd(1L, "politics", 5)).thenReturn(articleList);

		NewsSnapshotVO result = newsService.getNewsSnapshot("1", "politics", 5);

		assertThat(result.getSelectedPressId()).isEqualTo("1");
		assertThat(result.getSelectedCategoryId()).isEqualTo("politics");
		assertThat(result.getFallbackAppliedYn()).isEqualTo("N");
		assertThat(result.getArticleList()).hasSize(1);
	}

	@Test
	@DisplayName("뉴스 스냅샷은 무효한 저장 선택값을 1순위로 fallback 한다")
	// 무효한 언론사/카테고리 저장값이 전달되면 각 목록의 첫 항목으로 대체하는지 확인합니다.
	void getNewsSnapshot_fallbacksToFirstActiveSelectionWhenRequestedSelectionInvalid() {
		List<NewsPressSummaryVO> pressList = List.of(createPress("1", "JTBC"), createPress("2", "MBC"));
		List<NewsCategorySummaryVO> categoryList = List.of(createCategory("breaking", "속보"), createCategory("politics", "정치"));
		List<NewsTopArticleVO> articleList = List.of(createArticle("201", "기사2"));

		when(newsMapper.getActivePressList()).thenReturn(pressList);
		when(newsMapper.getActiveCategoryListByPressNo(1L)).thenReturn(categoryList);
		when(newsMapper.getTopArticleListByPressNoAndCategoryCd(1L, "breaking", 5)).thenReturn(articleList);

		NewsSnapshotVO result = newsService.getNewsSnapshot("999", "unknown", 5);

		assertThat(result.getSelectedPressId()).isEqualTo("1");
		assertThat(result.getSelectedCategoryId()).isEqualTo("breaking");
		assertThat(result.getFallbackAppliedYn()).isEqualTo("Y");
		assertThat(result.getArticleList()).hasSize(1);
	}

	@Test
	@DisplayName("뉴스 스냅샷은 카테고리가 없으면 빈 기사 목록을 반환한다")
	// 선택 언론사에 활성 카테고리가 없는 경계 상황에서 빈 목록을 반환하는지 확인합니다.
	void getNewsSnapshot_returnsEmptyArticleListWhenCategoryNotExists() {
		List<NewsPressSummaryVO> pressList = List.of(createPress("1", "JTBC"));

		when(newsMapper.getActivePressList()).thenReturn(pressList);
		when(newsMapper.getActiveCategoryListByPressNo(1L)).thenReturn(List.of());

		NewsSnapshotVO result = newsService.getNewsSnapshot("1", "breaking", 5);

		assertThat(result.getSelectedPressId()).isEqualTo("1");
		assertThat(result.getSelectedCategoryId()).isEmpty();
		assertThat(result.getCategoryList()).isEmpty();
		assertThat(result.getArticleList()).isEmpty();
		assertThat(result.getFallbackAppliedYn()).isEqualTo("N");
	}

	// 테스트용 언론사 응답 객체를 생성합니다.
	private NewsPressSummaryVO createPress(String id, String name) {
		NewsPressSummaryVO press = new NewsPressSummaryVO();
		press.setId(id);
		press.setName(name);
		return press;
	}

	// 테스트용 카테고리 응답 객체를 생성합니다.
	private NewsCategorySummaryVO createCategory(String id, String name) {
		NewsCategorySummaryVO category = new NewsCategorySummaryVO();
		category.setId(id);
		category.setName(name);
		return category;
	}

	// 테스트용 기사 응답 객체를 생성합니다.
	private NewsTopArticleVO createArticle(String id, String title) {
		NewsTopArticleVO article = new NewsTopArticleVO();
		article.setId(id);
		article.setTitle(title);
		article.setUrl("https://example.com/" + id);
		return article;
	}
}
