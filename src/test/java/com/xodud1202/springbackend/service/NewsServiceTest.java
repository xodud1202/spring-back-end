package com.xodud1202.springbackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsCategoryRowVO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsListQueryPO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsListRowVO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsPressRowVO;
import com.xodud1202.springbackend.domain.news.NewsListJsonSnapshotPublishResultVO;
import com.xodud1202.springbackend.domain.news.NewsListJsonSnapshotVO;
import com.xodud1202.springbackend.domain.news.NewsListMetaJsonVO;
import com.xodud1202.springbackend.domain.news.NewsListPressArticleShardJsonVO;
import com.xodud1202.springbackend.domain.news.NewsListPressShardSnapshotPublishResultVO;
import com.xodud1202.springbackend.domain.news.NewsCategorySummaryVO;
import com.xodud1202.springbackend.domain.news.NewsPressSummaryVO;
import com.xodud1202.springbackend.domain.news.NewsRssTargetVO;
import com.xodud1202.springbackend.domain.news.NewsSnapshotVO;
import com.xodud1202.springbackend.domain.news.NewsTopArticleVO;
import com.xodud1202.springbackend.domain.news.RssArticleItem;
import com.xodud1202.springbackend.mapper.NewsMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
// 뉴스 서비스 단위 테스트입니다.
public class NewsServiceTest {
	@Mock
	private NewsMapper newsMapper;

	@Mock
	private RssFeedClient rssFeedClient;

	@Mock
	private FtpFileService ftpFileService;

	private NewsService newsService;

	// 테스트 대상 서비스를 초기화합니다.
	@BeforeEach
	void setUp() {
		newsService = new NewsService(newsMapper, rssFeedClient, new ObjectMapper(), ftpFileService);
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

	@Test
	@DisplayName("뉴스 JSON 스냅샷은 프론트 직접 조회 구조로 전체 RSS 기사를 저장한다")
	// 프론트 직접 조회용 맵 구조와 기본 선택값, RSS 전체 기사 저장이 정상 생성되는지 확인합니다.
	void buildNewsListJsonSnapshot_buildsFrontDirectSnapshotWithAllRssArticles() {
		AdminNewsPressRowVO pressRow = createAdminPressRow(1L, "JTBC", "Y", 1);
		AdminNewsCategoryRowVO breakingCategory = createAdminCategoryRow(1L, "breaking", "속보", "Y", 1, "JTBC", "https://rss/breaking");
		AdminNewsCategoryRowVO politicsCategory = createAdminCategoryRow(1L, "politics", "정치", "Y", 2, "JTBC", "https://rss/politics");
		NewsRssTargetVO rssTarget = createRssTarget(1L, "JTBC", "breaking", "속보", "JTBC", "https://rss/breaking");
		List<RssArticleItem> feedItemList = List.of(
			new RssArticleItem("g1", "https://a/1", "기사1", "요약1", "https://img/1", "기자1", LocalDateTime.of(2026, 2, 23, 10, 0)),
			new RssArticleItem("g2", "https://a/2", "기사2", "요약2", "https://img/2", "기자2", LocalDateTime.of(2026, 2, 23, 9, 0))
		);

		when(newsMapper.getAdminNewsPressManageList()).thenReturn(List.of(pressRow));
		when(newsMapper.getAdminNewsCategoryManageListByPressNo(1L)).thenReturn(List.of(breakingCategory, politicsCategory));
		when(newsMapper.getActiveNewsRssTargetList()).thenReturn(List.of(rssTarget));
		when(rssFeedClient.fetchFeed("https://rss/breaking")).thenReturn(feedItemList);

		NewsListJsonSnapshotVO snapshot = newsService.buildNewsListJsonSnapshot();

		assertThat(snapshot.getPressList()).hasSize(1);
		assertThat(snapshot.getPressList().get(0).getId()).isEqualTo("1");
		assertThat(snapshot.getCategoryListByPressId()).containsKey("1");
		assertThat(snapshot.getCategoryListByPressId().get("1")).hasSize(2);
		assertThat(snapshot.getDefaultSelection().getDefaultPressId()).isEqualTo("1");
		assertThat(snapshot.getDefaultSelection().getDefaultCategoryIdByPressId().get("1")).isEqualTo("breaking");
		assertThat(snapshot.getArticleListByPressCategoryKey()).containsKey("1|breaking");
		assertThat(snapshot.getArticleListByPressCategoryKey().get("1|breaking")).hasSize(2);
		assertThat(snapshot.getArticleListByPressCategoryKey().get("1|breaking").get(0).getRankScore()).isEqualTo(1);
		assertThat(snapshot.getArticleListByPressCategoryKey().get("1|breaking").get(1).getRankScore()).isEqualTo(2);
		assertThat(snapshot.getArticleListByPressCategoryKey()).containsKey("1|politics");
		assertThat(snapshot.getArticleListByPressCategoryKey().get("1|politics")).isEmpty();
		assertThat(snapshot.getMeta().getTargetCount()).isEqualTo(1);
		assertThat(snapshot.getMeta().getSuccessTargetCount()).isEqualTo(1);
		assertThat(snapshot.getMeta().getFailedTargetCount()).isEqualTo(0);
	}

	@Test
	@DisplayName("뉴스 JSON 스냅샷 JSON 문자열은 파싱 가능한 구조를 반환한다")
	// 스냅샷 직렬화 결과가 JSON 문자열로 생성되고 핵심 키가 포함되는지 확인합니다.
	void buildNewsListJsonSnapshotJson_returnsParsableJsonString() {
		when(newsMapper.getAdminNewsPressManageList()).thenReturn(List.of());
		when(newsMapper.getActiveNewsRssTargetList()).thenReturn(List.of());

		String json = newsService.buildNewsListJsonSnapshotJson();

		assertThat(json).contains("\"meta\"");
		assertThat(json).contains("\"pressList\"");
		assertThat(json).contains("\"categoryListByPressId\"");
		assertThat(json).contains("\"articleListByPressCategoryKey\"");
	}

	@Test
	@DisplayName("뉴스 메타 JSON은 언론사 메타와 shard 파일 경로 맵을 포함한다")
	// 메타 파일 구조가 언론사/카테고리 메타와 기사 shard 경로를 담는지 확인합니다.
	void buildNewsListMetaJson_containsArticleFileMap() {
		AdminNewsPressRowVO pressRow = createAdminPressRow(1L, "JTBC", "Y", 1);
		AdminNewsCategoryRowVO breakingCategory = createAdminCategoryRow(1L, "breaking", "속보", "Y", 1, "JTBC", "https://rss/breaking");

		when(newsMapper.getAdminNewsPressManageList()).thenReturn(List.of(pressRow));
		when(newsMapper.getAdminNewsCategoryManageListByPressNo(1L)).thenReturn(List.of(breakingCategory));
		when(newsMapper.getActiveNewsRssTargetList()).thenReturn(List.of());

		NewsListMetaJsonVO metaJson = newsService.buildNewsListMetaJson();

		assertThat(metaJson.getPressList()).hasSize(1);
		assertThat(metaJson.getCategoryListByPressId()).containsKey("1");
		assertThat(metaJson.getArticleFileByPressId()).containsEntry("1", "articles/press-1.json");
		assertThat(metaJson.getDefaultSelection().getDefaultPressId()).isEqualTo("1");
	}

	@Test
	@DisplayName("뉴스 언론사 shard JSON은 같은 언론사 카테고리 기사만 포함한다")
	// 언론사별 shard 파일이 카테고리별 기사 목록을 분리하여 구성하는지 확인합니다.
	void buildNewsListPressArticleShardJsonMap_groupsByPressId() {
		AdminNewsPressRowVO press1 = createAdminPressRow(1L, "JTBC", "Y", 1);
		AdminNewsPressRowVO press2 = createAdminPressRow(2L, "MBC", "Y", 2);
		AdminNewsCategoryRowVO p1c1 = createAdminCategoryRow(1L, "breaking", "속보", "Y", 1, "JTBC", "https://rss/1");
		AdminNewsCategoryRowVO p2c1 = createAdminCategoryRow(2L, "society", "사회", "Y", 1, "MBC", "https://rss/2");
		NewsRssTargetVO t1 = createRssTarget(1L, "JTBC", "breaking", "속보", "JTBC", "https://rss/1");
		NewsRssTargetVO t2 = createRssTarget(2L, "MBC", "society", "사회", "MBC", "https://rss/2");

		when(newsMapper.getAdminNewsPressManageList()).thenReturn(List.of(press1, press2));
		when(newsMapper.getAdminNewsCategoryManageListByPressNo(1L)).thenReturn(List.of(p1c1));
		when(newsMapper.getAdminNewsCategoryManageListByPressNo(2L)).thenReturn(List.of(p2c1));
		when(newsMapper.getActiveNewsRssTargetList()).thenReturn(List.of(t1, t2));
		when(rssFeedClient.fetchFeed("https://rss/1")).thenReturn(List.of(
			new RssArticleItem("g1", "https://a/1", "기사1", null, null, null, LocalDateTime.of(2026, 2, 23, 10, 0))
		));
		when(rssFeedClient.fetchFeed("https://rss/2")).thenReturn(List.of(
			new RssArticleItem("g2", "https://a/2", "기사2", null, null, null, LocalDateTime.of(2026, 2, 23, 9, 0))
		));

		Map<String, NewsListPressArticleShardJsonVO> shardMap = newsService.buildNewsListPressArticleShardJsonMap();

		assertThat(shardMap).containsKeys("1", "2");
		assertThat(shardMap.get("1").getArticleListByCategoryId()).containsKey("breaking");
		assertThat(shardMap.get("1").getArticleListByCategoryId().get("breaking")).hasSize(1);
		assertThat(shardMap.get("2").getArticleListByCategoryId()).containsKey("society");
		assertThat(shardMap.get("2").getArticleListByCategoryId().get("society")).hasSize(1);
		assertThat(shardMap.get("1").getCategoryOrder()).containsExactly("breaking");
		assertThat(shardMap.get("2").getCategoryOrder()).containsExactly("society");
	}

	@Test
	@DisplayName("뉴스 JSON 스냅샷 파일 생성 업로드는 원자적 FTP 업로드를 호출하고 결과를 반환한다")
	// 스냅샷 파일 업로드 메서드가 경로/파일명/JSON 내용을 생성해 FTP 서비스에 전달하는지 확인합니다.
	void publishNewsListJsonSnapshot_uploadsJsonFileAtomically() throws Exception {
		when(newsMapper.getAdminNewsPressManageList()).thenReturn(List.of());
		when(newsMapper.getActiveNewsRssTargetList()).thenReturn(List.of());
		when(ftpFileService.resolveNewsSnapshotTargetPath("/HDD1/Media/nas/news"))
			.thenReturn("/HDD1/Media/nas/news");
		when(ftpFileService.uploadUtf8TextFileAtomically(eq("/HDD1/Media/nas/news"), eq("newsList.json"), any(String.class)))
			.thenReturn("newsList.json.tmp.20260223160000000");

		NewsListJsonSnapshotPublishResultVO result = newsService.publishNewsListJsonSnapshot();

		ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
		verify(ftpFileService).uploadUtf8TextFileAtomically(
			eq("/HDD1/Media/nas/news"),
			eq("newsList.json"),
			jsonCaptor.capture()
		);

		assertThat(result.getTargetPath()).isEqualTo("/HDD1/Media/nas/news");
		assertThat(result.getFileName()).isEqualTo("newsList.json");
		assertThat(result.getTempFileName()).isEqualTo("newsList.json.tmp.20260223160000000");
		assertThat(result.getTargetCount()).isEqualTo(0);
		assertThat(result.getSuccessTargetCount()).isEqualTo(0);
		assertThat(result.getFailedTargetCount()).isEqualTo(0);
		assertThat(result.getJsonByteSize()).isPositive();
		assertThat(jsonCaptor.getValue()).contains("\"meta\"");
		assertThat(jsonCaptor.getValue()).contains("\"schemaVersion\"");
	}

	@Test
	@DisplayName("뉴스 메타+언론사 shard 업로드는 기사 shard를 먼저 올리고 메타를 마지막에 교체한다")
	// 기사 shard 업로드 성공 후에만 메타 업로드가 수행되고 호출 순서가 보장되는지 확인합니다.
	void publishNewsListPressShardJsonSnapshot_uploadsShardsBeforeMeta() throws Exception {
		AdminNewsPressRowVO press1 = createAdminPressRow(1L, "JTBC", "Y", 1);
		AdminNewsCategoryRowVO p1c1 = createAdminCategoryRow(1L, "breaking", "속보", "Y", 1, "JTBC", "https://rss/1");
		NewsRssTargetVO t1 = createRssTarget(1L, "JTBC", "breaking", "속보", "JTBC", "https://rss/1");

		when(newsMapper.getAdminNewsPressManageList()).thenReturn(List.of(press1));
		when(newsMapper.getAdminNewsCategoryManageListByPressNo(1L)).thenReturn(List.of(p1c1));
		when(newsMapper.getActiveNewsRssTargetList()).thenReturn(List.of(t1));
		when(rssFeedClient.fetchFeed("https://rss/1")).thenReturn(List.of(
			new RssArticleItem("g1", "https://a/1", "기사1", null, null, null, LocalDateTime.of(2026, 2, 23, 10, 0))
		));
		when(ftpFileService.resolveNewsSnapshotTargetPath("/HDD1/Media/nas/news")).thenReturn("/HDD1/Media/nas/news");
		when(ftpFileService.uploadUtf8TextFileAtomically(eq("/HDD1/Media/nas/news/articles"), eq("press-1.json"), any(String.class)))
			.thenReturn("press-1.json.tmp.1");
		when(ftpFileService.uploadUtf8TextFileAtomically(eq("/HDD1/Media/nas/news"), eq("meta.json"), any(String.class)))
			.thenReturn("meta.json.tmp.1");

		NewsListPressShardSnapshotPublishResultVO result = newsService.publishNewsListPressShardJsonSnapshot();

		InOrder inOrder = inOrder(ftpFileService);
		inOrder.verify(ftpFileService).resolveNewsSnapshotTargetPath("/HDD1/Media/nas/news");
		inOrder.verify(ftpFileService).uploadUtf8TextFileAtomically(eq("/HDD1/Media/nas/news/articles"), eq("press-1.json"), any(String.class));
		inOrder.verify(ftpFileService).uploadUtf8TextFileAtomically(eq("/HDD1/Media/nas/news"), eq("meta.json"), any(String.class));

		assertThat(result.getBaseTargetPath()).isEqualTo("/HDD1/Media/nas/news");
		assertThat(result.getMetaFileName()).isEqualTo("meta.json");
		assertThat(result.getPressShardCount()).isEqualTo(1);
		assertThat(result.getShardSuccessCount()).isEqualTo(1);
		assertThat(result.getShardFailedCount()).isEqualTo(0);
		assertThat(result.getMetaJsonByteSize()).isPositive();
		assertThat(result.getTotalShardJsonByteSize()).isPositive();
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

	// 테스트용 관리자 언론사 행 객체를 생성합니다.
	private AdminNewsPressRowVO createAdminPressRow(Long pressNo, String pressNm, String useYn, Integer sortSeq) {
		AdminNewsPressRowVO row = new AdminNewsPressRowVO();
		row.setPressNo(pressNo);
		row.setPressNm(pressNm);
		row.setUseYn(useYn);
		row.setSortSeq(sortSeq);
		return row;
	}

	// 테스트용 관리자 카테고리 행 객체를 생성합니다.
	private AdminNewsCategoryRowVO createAdminCategoryRow(
		Long pressNo,
		String categoryCd,
		String categoryNm,
		String useYn,
		Integer sortSeq,
		String sourceNm,
		String rssUrl
	) {
		AdminNewsCategoryRowVO row = new AdminNewsCategoryRowVO();
		row.setPressNo(pressNo);
		row.setCategoryCd(categoryCd);
		row.setCategoryNm(categoryNm);
		row.setUseYn(useYn);
		row.setSortSeq(sortSeq);
		row.setSourceNm(sourceNm);
		row.setRssUrl(rssUrl);
		return row;
	}

	// 테스트용 RSS 대상 객체를 생성합니다.
	private NewsRssTargetVO createRssTarget(
		Long pressNo,
		String pressNm,
		String categoryCd,
		String categoryNm,
		String sourceNm,
		String rssUrl
	) {
		NewsRssTargetVO target = new NewsRssTargetVO();
		target.setPressNo(pressNo);
		target.setPressNm(pressNm);
		target.setCategoryCd(categoryCd);
		target.setCategoryNm(categoryNm);
		target.setSourceNm(sourceNm);
		target.setRssUrl(rssUrl);
		return target;
	}
}
