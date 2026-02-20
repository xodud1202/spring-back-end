package com.xodud1202.springbackend.mapper;

import com.xodud1202.springbackend.domain.admin.news.AdminNewsCategoryOptionVO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsCategoryRowVO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsCategorySaveRowPO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsListQueryPO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsListRowVO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsPressOptionVO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsPressRowVO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsPressSaveRowPO;
import com.xodud1202.springbackend.domain.news.NewsArticleCreatePO;
import com.xodud1202.springbackend.domain.news.NewsCategorySummaryVO;
import com.xodud1202.springbackend.domain.news.NewsPressSummaryVO;
import com.xodud1202.springbackend.domain.news.NewsRssTargetVO;
import com.xodud1202.springbackend.domain.news.NewsTopArticleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
// 뉴스 관련 조회/관리용 MyBatis 매퍼 인터페이스입니다.
public interface NewsMapper {
	// 관리자 뉴스 목록 화면 언론사 선택 목록을 조회합니다.
	List<AdminNewsPressOptionVO> getAdminNewsPressOptionList();

	// 관리자 뉴스 목록 화면 카테고리 선택 목록을 조회합니다.
	List<AdminNewsCategoryOptionVO> getAdminNewsCategoryOptionList(@Param("pressNo") Long pressNo);

	// 관리자 뉴스 목록을 페이지 단위로 조회합니다.
	List<AdminNewsListRowVO> getAdminNewsList(AdminNewsListQueryPO param);

	// 관리자 뉴스 목록의 전체 건수를 조회합니다.
	int getAdminNewsListCount(AdminNewsListQueryPO param);

	// 관리자 뉴스 RSS 관리용 언론사 목록을 조회합니다.
	List<AdminNewsPressRowVO> getAdminNewsPressManageList();

	// 관리자 뉴스 RSS 관리용 카테고리 목록을 조회합니다.
	List<AdminNewsCategoryRowVO> getAdminNewsCategoryManageListByPressNo(@Param("pressNo") Long pressNo);

	// 관리자 뉴스 RSS 관리용 언론사를 등록합니다.
	int insertAdminNewsPress(AdminNewsPressSaveRowPO param);

	// 관리자 뉴스 RSS 관리용 언론사를 수정합니다.
	int updateAdminNewsPress(AdminNewsPressSaveRowPO param);

	// 관리자 뉴스 RSS 관리용 언론사 정렬 순서를 수정합니다.
	int updateAdminNewsPressSortSeq(@Param("pressNo") Long pressNo, @Param("sortSeq") Integer sortSeq, @Param("udtNo") Long udtNo);

	// 관리자 뉴스 RSS 관리용 언론사 기사 데이터를 삭제합니다.
	int deleteAdminNewsArticleByPressNo(@Param("pressNo") Long pressNo);

	// 관리자 뉴스 RSS 관리용 언론사 카테고리를 삭제합니다.
	int deleteAdminNewsCategoryByPressNo(@Param("pressNo") Long pressNo);

	// 관리자 뉴스 RSS 관리용 언론사를 삭제합니다.
	int deleteAdminNewsPressByPressNo(@Param("pressNo") Long pressNo);

	// 관리자 뉴스 RSS 관리용 카테고리를 등록합니다.
	int insertAdminNewsCategory(AdminNewsCategorySaveRowPO param);

	// 관리자 뉴스 RSS 관리용 카테고리를 수정합니다.
	int updateAdminNewsCategory(AdminNewsCategorySaveRowPO param);

	// 관리자 뉴스 RSS 관리용 카테고리 정렬 순서를 수정합니다.
	int updateAdminNewsCategorySortSeq(
		@Param("pressNo") Long pressNo,
		@Param("categoryCd") String categoryCd,
		@Param("sortSeq") Integer sortSeq,
		@Param("udtNo") Long udtNo
	);

	// 관리자 뉴스 RSS 관리용 카테고리 기사 데이터를 삭제합니다.
	int deleteAdminNewsArticleByPressNoAndCategoryCd(@Param("pressNo") Long pressNo, @Param("categoryCd") String categoryCd);

	// 관리자 뉴스 RSS 관리용 카테고리를 삭제합니다.
	int deleteAdminNewsCategoryByPressNoAndCategoryCd(@Param("pressNo") Long pressNo, @Param("categoryCd") String categoryCd);

	// 공개 뉴스 화면용 활성 언론사 목록을 조회합니다.
	List<NewsPressSummaryVO> getActivePressList();

	// 공개 뉴스 화면용 활성 카테고리 목록을 조회합니다.
	List<NewsCategorySummaryVO> getActiveCategoryListByPressNo(@Param("pressNo") Long pressNo);

	// 공개 뉴스 화면용 상위 기사 목록을 조회합니다.
	List<NewsTopArticleVO> getTopArticleListByPressNoAndCategoryCd(
		@Param("pressNo") Long pressNo,
		@Param("categoryCd") String categoryCd,
		@Param("limit") int limit
	);

	// RSS 수집 대상 목록을 조회합니다.
	List<NewsRssTargetVO> getActiveNewsRssTargetList();

	// 수집일시 기준 7일을 초과한 기사 데이터를 삭제합니다.
	int deleteNewsArticleOlderThan7Days();

	// RSS 수집 대상 기사 점수를 초기화합니다.
	int resetRankScoreByTarget(NewsArticleCreatePO param);

	// RSS 수집 기사 데이터를 저장합니다.
	int insertNewsArticle(NewsArticleCreatePO param);
}
