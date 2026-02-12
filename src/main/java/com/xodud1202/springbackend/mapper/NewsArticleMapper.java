package com.xodud1202.springbackend.mapper;

import com.xodud1202.springbackend.domain.news.NewsArticleCreatePO;
import com.xodud1202.springbackend.domain.news.NewsRssTargetVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
// 뉴스 RSS 대상 조회 및 기사 적재 매퍼를 정의합니다.
public interface NewsArticleMapper {
	// 수집 대상 RSS URL 목록을 조회합니다.
	List<NewsRssTargetVO> getActiveNewsRssTargetList();

	// 대상 언론사/카테고리의 기존 랭킹 점수를 초기화합니다.
	int resetRankScoreByTarget(NewsArticleCreatePO param);

	// 수집한 뉴스 기사를 저장합니다.
	int insertNewsArticle(NewsArticleCreatePO param);
}
