package com.xodud1202.springbackend.mapper;

import com.xodud1202.springbackend.domain.news.NewsArticleCreatePO;
import com.xodud1202.springbackend.domain.news.NewsCategorySummaryVO;
import com.xodud1202.springbackend.domain.news.NewsPressSummaryVO;
import com.xodud1202.springbackend.domain.news.NewsRssTargetVO;
import com.xodud1202.springbackend.domain.news.NewsTopArticleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
// 뉴스 RSS 대상 조회 및 기사 적재 매퍼를 정의합니다.
public interface NewsArticleMapper {
	// 활성 언론사 목록을 조회합니다.
	List<NewsPressSummaryVO> getActivePressList();

	// 선택된 언론사의 활성 카테고리 목록을 조회합니다.
	List<NewsCategorySummaryVO> getActiveCategoryListByPressNo(@Param("pressNo") Long pressNo);

	// 선택된 언론사/카테고리의 상위 기사 목록을 조회합니다.
	List<NewsTopArticleVO> getTopArticleListByPressNoAndCategoryCd(
		@Param("pressNo") Long pressNo,
		@Param("categoryCd") String categoryCd,
		@Param("limit") int limit
	);

	// 수집 대상 RSS URL 목록을 조회합니다.
	List<NewsRssTargetVO> getActiveNewsRssTargetList();

	// 대상 언론사/카테고리의 기존 랭킹 점수를 초기화합니다.
	int resetRankScoreByTarget(NewsArticleCreatePO param);

	// 수집한 뉴스 기사를 저장합니다.
	int insertNewsArticle(NewsArticleCreatePO param);
}
