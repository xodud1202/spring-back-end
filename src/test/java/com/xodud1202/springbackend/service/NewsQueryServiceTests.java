package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.news.NewsCategorySummaryVO;
import com.xodud1202.springbackend.domain.news.NewsPressSummaryVO;
import com.xodud1202.springbackend.domain.news.NewsTopArticleVO;
import com.xodud1202.springbackend.mapper.NewsArticleMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// 뉴스 팝업 조회 서비스의 입력 보정/조회 위임 로직을 검증합니다.
class NewsQueryServiceTests {
	// 뉴스 조회 매퍼 목 객체를 보관합니다.
	@Mock
	private NewsArticleMapper newsArticleMapper;

	// 테스트 대상 서비스입니다.
	@InjectMocks
	private NewsQueryService newsQueryService;

	@Test
	@DisplayName("언론사 목록: 매퍼 조회 결과를 그대로 반환한다")
	// 언론사 목록 조회 위임 로직을 검증합니다.
	void getActivePressList_returnsMapperResult() {
		// 매퍼 응답 데이터를 구성합니다.
		NewsPressSummaryVO press = new NewsPressSummaryVO();
		press.setId("1");
		press.setName("JTBC");
		when(newsArticleMapper.getActivePressList()).thenReturn(List.of(press));

		// 서비스 조회를 실행합니다.
		List<NewsPressSummaryVO> result = newsQueryService.getActivePressList();

		// 매퍼 결과가 그대로 반환되는지 검증합니다.
		assertEquals(1, result.size());
		assertEquals("1", result.get(0).getId());
		assertEquals("JTBC", result.get(0).getName());
	}

	@Test
	@DisplayName("카테고리 목록: pressId가 숫자가 아니면 빈 목록을 반환한다")
	// 언론사 번호 파싱 실패 시 빈 목록 반환 로직을 검증합니다.
	void getActiveCategoryList_returnsEmptyWhenPressIdInvalid() {
		// 잘못된 언론사 ID로 서비스를 호출합니다.
		List<NewsCategorySummaryVO> result = newsQueryService.getActiveCategoryList("JTBC");

		// 빈 목록 반환 및 매퍼 미호출을 검증합니다.
		assertTrue(result.isEmpty());
		verify(newsArticleMapper, never()).getActiveCategoryListByPressNo(eq(1L));
	}

	@Test
	@DisplayName("상위 기사 목록: limit 미입력 시 기본값 5건으로 조회한다")
	// limit 미입력 시 기본 건수 보정 로직을 검증합니다.
	void getTopArticleList_usesDefaultLimitWhenNull() {
		// 매퍼 응답 데이터를 구성합니다.
		NewsTopArticleVO article = new NewsTopArticleVO();
		article.setId("100");
		article.setTitle("기사 제목");
		article.setUrl("https://news.example.com/100");
		when(newsArticleMapper.getTopArticleListByPressNoAndCategoryCd(eq(1L), eq("economy"), eq(5)))
			.thenReturn(List.of(article));

		// limit 없이 서비스 조회를 실행합니다.
		List<NewsTopArticleVO> result = newsQueryService.getTopArticleList("1", "economy", null);

		// 기본 건수(5)로 조회되는지 검증합니다.
		assertEquals(1, result.size());
		verify(newsArticleMapper).getTopArticleListByPressNoAndCategoryCd(eq(1L), eq("economy"), eq(5));
	}

	@Test
	@DisplayName("상위 기사 목록: limit가 20 초과이면 20으로 제한한다")
	// 과도한 조회 건수를 상한값으로 제한하는 로직을 검증합니다.
	void getTopArticleList_capsLimitAtTwenty() {
		// 상한 건수로 매퍼가 호출되도록 목 동작을 구성합니다.
		when(newsArticleMapper.getTopArticleListByPressNoAndCategoryCd(eq(1L), eq("economy"), eq(20)))
			.thenReturn(List.of());

		// 상한을 넘는 limit 값으로 서비스를 호출합니다.
		List<NewsTopArticleVO> result = newsQueryService.getTopArticleList("1", "economy", 99);

		// 빈 목록 반환과 상한 적용 호출을 검증합니다.
		assertTrue(result.isEmpty());
		verify(newsArticleMapper).getTopArticleListByPressNoAndCategoryCd(eq(1L), eq("economy"), eq(20));
	}

	@Test
	@DisplayName("상위 기사 목록: categoryId가 비어 있으면 빈 목록을 반환한다")
	// 카테고리 ID 미입력 시 매퍼 호출을 생략하는 로직을 검증합니다.
	void getTopArticleList_returnsEmptyWhenCategoryMissing() {
		// 카테고리 미입력 상태로 서비스를 호출합니다.
		List<NewsTopArticleVO> result = newsQueryService.getTopArticleList("1", " ", 5);

		// 빈 목록 반환 및 매퍼 미호출을 검증합니다.
		assertTrue(result.isEmpty());
		verify(newsArticleMapper, never()).getTopArticleListByPressNoAndCategoryCd(eq(1L), eq("economy"), anyInt());
	}
}
