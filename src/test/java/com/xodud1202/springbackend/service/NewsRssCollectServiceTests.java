package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.news.NewsCollectResultVO;
import com.xodud1202.springbackend.domain.news.NewsRssTargetVO;
import com.xodud1202.springbackend.domain.news.RssArticleItem;
import com.xodud1202.springbackend.mapper.NewsArticleMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// NewsRssCollectService의 수집/적재 핵심 로직을 검증합니다.
class NewsRssCollectServiceTests {

	// 뉴스 기사 매퍼 목 객체입니다.
	@Mock
	private NewsArticleMapper newsArticleMapper;

	// RSS 피드 클라이언트 목 객체입니다.
	@Mock
	private RssFeedClient rssFeedClient;

	// 테스트 대상 서비스입니다.
	@InjectMocks
	private NewsRssCollectService newsRssCollectService;

	@Test
	@DisplayName("정상 수집: RSS 항목이 5건을 넘으면 상위 5건만 저장한다")
	// RSS 항목이 5건을 초과해도 상위 5건만 적재하는지 검증합니다.
	void collectNewsArticles_insertsOnlyTopFiveItems() {
		// 수집 대상과 피드 데이터를 구성합니다.
		NewsRssTargetVO target = buildTarget(1L, "politics", "https://example.com/rss");
		List<RssArticleItem> feedItems = List.of(
			buildItem("guid-1", "https://example.com/1", "제목1"),
			buildItem("guid-2", "https://example.com/2", "제목2"),
			buildItem("guid-3", "https://example.com/3", "제목3"),
			buildItem("guid-4", "https://example.com/4", "제목4"),
			buildItem("guid-5", "https://example.com/5", "제목5"),
			buildItem("guid-6", "https://example.com/6", "제목6"),
			buildItem("guid-7", "https://example.com/7", "제목7")
		);

		// 목 응답을 설정합니다.
		when(newsArticleMapper.getActiveNewsRssTargetList()).thenReturn(List.of(target));
		when(rssFeedClient.fetchFeed(eq("https://example.com/rss"))).thenReturn(feedItems);
		when(newsArticleMapper.resetRankScoreByTarget(any())).thenReturn(1);
		when(newsArticleMapper.insertNewsArticle(any())).thenReturn(1);

		// 수집 배치를 실행합니다.
		NewsCollectResultVO result = newsRssCollectService.collectNewsArticles();

		// 저장 건수와 호출 횟수를 검증합니다.
		assertEquals(5, result.getAttemptedArticleCount());
		assertEquals(5, result.getInsertedArticleCount());
		assertEquals(0, result.getSkippedArticleCount());
		verify(newsArticleMapper, times(1)).resetRankScoreByTarget(any());
		verify(newsArticleMapper, times(5)).insertNewsArticle(any());

		// 첫 번째 저장 데이터의 랭크 점수를 검증합니다.
		ArgumentCaptor<com.xodud1202.springbackend.domain.news.NewsArticleCreatePO> captor =
			ArgumentCaptor.forClass(com.xodud1202.springbackend.domain.news.NewsArticleCreatePO.class);
		verify(newsArticleMapper, times(5)).insertNewsArticle(captor.capture());
		BigDecimal firstRankScore = captor.getAllValues().get(0).getRankScore();
		assertEquals(BigDecimal.valueOf(1), firstRankScore);
		assertEquals("Y", captor.getAllValues().get(0).getUseYn());
	}

	@Test
	@DisplayName("실패 격리: 특정 RSS URL 실패 시 다음 URL 수집은 계속한다")
	// RSS URL 단위 실패가 전체 배치를 중단하지 않는지 검증합니다.
	void collectNewsArticles_continuesWhenOneRssFails() {
		// 성공/실패 대상 URL을 구성합니다.
		NewsRssTargetVO failedTarget = buildTarget(1L, "politics", "https://example.com/fail");
		NewsRssTargetVO successTarget = buildTarget(1L, "economy", "https://example.com/success");
		List<RssArticleItem> successItems = List.of(buildItem("guid-ok", "https://example.com/ok", "정상기사"));

		// 목 응답을 설정합니다.
		when(newsArticleMapper.getActiveNewsRssTargetList()).thenReturn(List.of(failedTarget, successTarget));
		when(rssFeedClient.fetchFeed(eq("https://example.com/fail"))).thenThrow(new RuntimeException("통신 실패"));
		when(rssFeedClient.fetchFeed(eq("https://example.com/success"))).thenReturn(successItems);
		when(newsArticleMapper.resetRankScoreByTarget(any())).thenReturn(1);
		when(newsArticleMapper.insertNewsArticle(any())).thenReturn(1);

		// 수집 배치를 실행합니다.
		NewsCollectResultVO result = newsRssCollectService.collectNewsArticles();

		// 실패 URL 격리 및 성공 URL 저장 여부를 검증합니다.
		assertEquals(2, result.getTargetCount());
		assertEquals(1, result.getSuccessTargetCount());
		assertEquals(1, result.getFailedTargetCount());
		assertEquals(1, result.getInsertedArticleCount());
	}

	@Test
	@DisplayName("경계 케이스: 기사 URL이 비어 있으면 '-' 치환 후 USE_YN='N'으로 저장한다")
	// 필수값 누락 건은 '-'로 치환하고 USE_YN='N'으로 저장하는지 검증합니다.
	void collectNewsArticles_replacesNullWithDashAndMarksUseYnN() {
		// URL 누락 피드 데이터를 구성합니다.
		NewsRssTargetVO target = buildTarget(1L, "society", "https://example.com/rss");
		RssArticleItem invalidItem = new RssArticleItem(
			"guid-1",
			" ",
			"제목",
			"요약",
			null,
			"기자",
			LocalDateTime.of(2026, 2, 12, 16, 0)
		);

		// 목 응답을 설정합니다.
		when(newsArticleMapper.getActiveNewsRssTargetList()).thenReturn(List.of(target));
		when(rssFeedClient.fetchFeed(eq("https://example.com/rss"))).thenReturn(List.of(invalidItem));
		when(newsArticleMapper.resetRankScoreByTarget(any())).thenReturn(1);
		when(newsArticleMapper.insertNewsArticle(any())).thenReturn(1);

		// 수집 배치를 실행합니다.
		NewsCollectResultVO result = newsRssCollectService.collectNewsArticles();

		// URL 누락 건이 '-' 치환되어 저장되는지 검증합니다.
		assertEquals(1, result.getAttemptedArticleCount());
		assertEquals(1, result.getInsertedArticleCount());
		assertEquals(0, result.getSkippedArticleCount());
		ArgumentCaptor<com.xodud1202.springbackend.domain.news.NewsArticleCreatePO> captor =
			ArgumentCaptor.forClass(com.xodud1202.springbackend.domain.news.NewsArticleCreatePO.class);
		verify(newsArticleMapper, times(1)).insertNewsArticle(captor.capture());
		assertEquals("-", captor.getValue().getArticleUrl());
		assertEquals("N", captor.getValue().getUseYn());
	}

	@Test
	@DisplayName("nullable 누락: 썸네일/요약/작성자 누락은 NULL로 저장하고 USE_YN은 Y다")
	// nullable 컬럼 누락 시 "-" 치환 없이 NULL 저장 및 USE_YN=Y를 검증합니다.
	void collectNewsArticles_keepsNullableFieldsNullAndUseYnY() {
		// nullable 필드가 비어있는 피드 데이터를 구성합니다.
		NewsRssTargetVO target = buildTarget(1L, "society", "https://example.com/rss");
		RssArticleItem item = new RssArticleItem(
			"guid-1",
			"https://example.com/article/1",
			"제목",
			null,
			null,
			null,
			LocalDateTime.of(2026, 2, 12, 16, 0)
		);

		// 목 응답을 설정합니다.
		when(newsArticleMapper.getActiveNewsRssTargetList()).thenReturn(List.of(target));
		when(rssFeedClient.fetchFeed(eq("https://example.com/rss"))).thenReturn(List.of(item));
		when(newsArticleMapper.resetRankScoreByTarget(any())).thenReturn(1);
		when(newsArticleMapper.insertNewsArticle(any())).thenReturn(1);

		// 수집 배치를 실행합니다.
		newsRssCollectService.collectNewsArticles();

		// nullable 필드가 null로 유지되고 USE_YN이 Y인지 검증합니다.
		ArgumentCaptor<com.xodud1202.springbackend.domain.news.NewsArticleCreatePO> captor =
			ArgumentCaptor.forClass(com.xodud1202.springbackend.domain.news.NewsArticleCreatePO.class);
		verify(newsArticleMapper, times(1)).insertNewsArticle(captor.capture());
		assertEquals("Y", captor.getValue().getUseYn());
		assertNull(captor.getValue().getArticleSummary());
		assertNull(captor.getValue().getThumbnailUrl());
		assertNull(captor.getValue().getAuthorNm());
	}

	@Test
	@DisplayName("GUID 누락: ARTICLE_GUID가 없으면 ARTICLE_URL로 대체 저장한다")
	// GUID 누락 시 URL을 GUID로 대체해 중복 제약을 적용하는지 검증합니다.
	void collectNewsArticles_replacesMissingGuidWithArticleUrl() {
		// GUID가 비어있는 피드 데이터를 구성합니다.
		NewsRssTargetVO target = buildTarget(1L, "international", "https://example.com/rss");
		RssArticleItem item = new RssArticleItem(
			null,
			"https://example.com/article/no-guid",
			"가이드 없는 기사",
			"요약",
			null,
			null,
			LocalDateTime.of(2026, 2, 12, 17, 30)
		);

		// 목 응답을 설정합니다.
		when(newsArticleMapper.getActiveNewsRssTargetList()).thenReturn(List.of(target));
		when(rssFeedClient.fetchFeed(eq("https://example.com/rss"))).thenReturn(List.of(item));
		when(newsArticleMapper.resetRankScoreByTarget(any())).thenReturn(1);
		when(newsArticleMapper.insertNewsArticle(any())).thenReturn(1);

		// 수집 배치를 실행합니다.
		newsRssCollectService.collectNewsArticles();

		// GUID 대체 저장 여부를 검증합니다.
		ArgumentCaptor<com.xodud1202.springbackend.domain.news.NewsArticleCreatePO> captor =
			ArgumentCaptor.forClass(com.xodud1202.springbackend.domain.news.NewsArticleCreatePO.class);
		verify(newsArticleMapper, times(1)).insertNewsArticle(captor.capture());
		assertEquals("https://example.com/article/no-guid", captor.getValue().getArticleGuid());
		assertEquals("https://example.com/article/no-guid", captor.getValue().getArticleUrl());
		assertEquals("Y", captor.getValue().getUseYn());
	}

	@Test
	@DisplayName("랭킹 보정: N이 섞여도 Y 5개가 채워질 때까지 읽고 rank는 순차 증가한다")
	// 누락 데이터가 포함되어도 USE_YN=Y 5개를 채울 때까지 수집하는지 검증합니다.
	void collectNewsArticles_fillsFiveActiveArticlesWithSequentialRank() {
		// 앞 2건은 N, 뒤 5건은 Y가 되도록 데이터를 구성합니다.
		NewsRssTargetVO target = buildTarget(1L, "economy", "https://example.com/rss");
		List<RssArticleItem> feedItems = List.of(
			new RssArticleItem("guid-n1", null, "제목N1", null, null, null, LocalDateTime.of(2026, 2, 12, 12, 0)),
			new RssArticleItem("guid-n2", "https://example.com/n2", null, null, null, null, LocalDateTime.of(2026, 2, 12, 12, 1)),
			buildItem("guid-1", "https://example.com/1", "제목1"),
			buildItem("guid-2", "https://example.com/2", "제목2"),
			buildItem("guid-3", "https://example.com/3", "제목3"),
			buildItem("guid-4", "https://example.com/4", "제목4"),
			buildItem("guid-5", "https://example.com/5", "제목5")
		);

		// 목 응답을 설정합니다.
		when(newsArticleMapper.getActiveNewsRssTargetList()).thenReturn(List.of(target));
		when(rssFeedClient.fetchFeed(eq("https://example.com/rss"))).thenReturn(feedItems);
		when(newsArticleMapper.resetRankScoreByTarget(any())).thenReturn(1);
		when(newsArticleMapper.insertNewsArticle(any())).thenReturn(1);

		// 수집 배치를 실행합니다.
		NewsCollectResultVO result = newsRssCollectService.collectNewsArticles();

		// 7건 처리(1~7 rank), Y 5건 충족 시 종료되는지 검증합니다.
		assertEquals(7, result.getAttemptedArticleCount());
		assertEquals(7, result.getInsertedArticleCount());
		ArgumentCaptor<com.xodud1202.springbackend.domain.news.NewsArticleCreatePO> captor =
			ArgumentCaptor.forClass(com.xodud1202.springbackend.domain.news.NewsArticleCreatePO.class);
		verify(newsArticleMapper, times(7)).insertNewsArticle(captor.capture());
		assertEquals(BigDecimal.valueOf(1), captor.getAllValues().get(0).getRankScore());
		assertEquals("N", captor.getAllValues().get(0).getUseYn());
		assertEquals(BigDecimal.valueOf(7), captor.getAllValues().get(6).getRankScore());
		assertEquals("Y", captor.getAllValues().get(6).getUseYn());
	}

	// 테스트용 RSS 대상 데이터를 생성합니다.
	private NewsRssTargetVO buildTarget(Long pressNo, String categoryCd, String rssUrl) {
		NewsRssTargetVO target = new NewsRssTargetVO();
		target.setPressNo(pressNo);
		target.setCategoryCd(categoryCd);
		target.setRssUrl(rssUrl);
		return target;
	}

	// 테스트용 RSS 기사 데이터를 생성합니다.
	private RssArticleItem buildItem(String guid, String link, String title) {
		return new RssArticleItem(
			guid,
			link,
			title,
			"요약",
			"https://example.com/thumb.jpg",
			"기자",
			LocalDateTime.of(2026, 2, 12, 15, 0)
		);
	}
}
