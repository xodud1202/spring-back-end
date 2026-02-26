package com.xodud1202.springbackend.mapper;

import com.xodud1202.springbackend.domain.admin.notion.AdminNotionCategoryVO;
import com.xodud1202.springbackend.domain.admin.notion.AdminNotionListQueryPO;
import com.xodud1202.springbackend.domain.admin.notion.AdminNotionListRowVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
// 관리자 Notion 저장 목록/카테고리 관리용 MyBatis 매퍼 인터페이스입니다.
public interface AdminNotionMapper {
	// 관리자 Notion 저장 목록을 조회합니다.
	List<AdminNotionListRowVO> getAdminNotionSaveList(AdminNotionListQueryPO param);

	// 관리자 Notion 저장 목록 건수를 조회합니다.
	int getAdminNotionSaveCount(AdminNotionListQueryPO param);

	// 관리자 Notion 카테고리 목록을 조회합니다.
	List<AdminNotionCategoryVO> getAdminNotionCategoryList();

	// 관리자 Notion 카테고리 정렬 순서를 수정합니다.
	int updateAdminNotionCategorySortSeq(
		@Param("categoryId") String categoryId,
		@Param("sortSeq") Integer sortSeq,
		@Param("udtNo") Long udtNo
	);
}
