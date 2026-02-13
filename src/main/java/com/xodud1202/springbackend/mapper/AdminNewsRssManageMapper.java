package com.xodud1202.springbackend.mapper;

import com.xodud1202.springbackend.domain.admin.news.AdminNewsCategoryRowVO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsCategorySaveRowPO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsPressRowVO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsPressSaveRowPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
// 관리자 뉴스 RSS 관리 매퍼를 정의합니다.
public interface AdminNewsRssManageMapper {
	// 관리자 뉴스 언론사 목록을 조회합니다.
	List<AdminNewsPressRowVO> getAdminNewsPressList();

	// 관리자 뉴스 카테고리 목록을 조회합니다.
	List<AdminNewsCategoryRowVO> getAdminNewsCategoryListByPressNo(@Param("pressNo") Long pressNo);

	// 관리자 뉴스 언론사를 등록합니다.
	int insertAdminNewsPress(AdminNewsPressSaveRowPO param);

	// 관리자 뉴스 언론사를 수정합니다.
	int updateAdminNewsPress(AdminNewsPressSaveRowPO param);

	// 관리자 뉴스 언론사 정렬 순서를 수정합니다.
	int updateAdminNewsPressSortSeq(@Param("pressNo") Long pressNo, @Param("sortSeq") Integer sortSeq, @Param("udtNo") Long udtNo);

	// 관리자 뉴스 언론사별 기사 데이터를 삭제합니다.
	int deleteAdminNewsArticleByPressNo(@Param("pressNo") Long pressNo);

	// 관리자 뉴스 언론사별 카테고리 데이터를 삭제합니다.
	int deleteAdminNewsCategoryByPressNo(@Param("pressNo") Long pressNo);

	// 관리자 뉴스 언론사를 삭제합니다.
	int deleteAdminNewsPressByPressNo(@Param("pressNo") Long pressNo);

	// 관리자 뉴스 카테고리를 등록합니다.
	int insertAdminNewsCategory(AdminNewsCategorySaveRowPO param);

	// 관리자 뉴스 카테고리를 수정합니다.
	int updateAdminNewsCategory(AdminNewsCategorySaveRowPO param);

	// 관리자 뉴스 카테고리 정렬 순서를 수정합니다.
	int updateAdminNewsCategorySortSeq(
		@Param("pressNo") Long pressNo,
		@Param("categoryCd") String categoryCd,
		@Param("sortSeq") Integer sortSeq,
		@Param("udtNo") Long udtNo
	);

	// 관리자 뉴스 언론사/카테고리별 기사 데이터를 삭제합니다.
	int deleteAdminNewsArticleByPressNoAndCategoryCd(@Param("pressNo") Long pressNo, @Param("categoryCd") String categoryCd);

	// 관리자 뉴스 카테고리를 삭제합니다.
	int deleteAdminNewsCategoryByPressNoAndCategoryCd(@Param("pressNo") Long pressNo, @Param("categoryCd") String categoryCd);
}
