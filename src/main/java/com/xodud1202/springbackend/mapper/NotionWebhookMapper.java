package com.xodud1202.springbackend.mapper;

import com.xodud1202.springbackend.domain.notion.NotionDataListUpsertPO;
import com.xodud1202.springbackend.domain.notion.NotionCategoryUpsertPO;
import com.xodud1202.springbackend.domain.notion.NotionWebhookTempEntryPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
// Notion 웹훅 임시 저장용 MyBatis 매퍼 인터페이스입니다.
public interface NotionWebhookMapper {
	// KEY_VALUE_TEMP_TABLE에 웹훅 원문 키/값 목록을 일괄 저장합니다.
	int insertNotionWebhookTempBatch(@Param("rows") List<NotionWebhookTempEntryPO> rows);

	// Notion 페이지 상세 데이터를 NOTION_DATA_LIST에 저장/수정합니다.
	int upsertNotionDataList(@Param("row") NotionDataListUpsertPO row);

	// Notion 카테고리 목록을 NOTION_CATEGORY에 저장/수정합니다.
	int upsertNotionCategoryBatch(@Param("rows") List<NotionCategoryUpsertPO> rows);

	// page.deleted 이벤트 기준으로 NOTION_DATA_LIST 삭제 상태를 저장/수정합니다.
	int upsertNotionDataDeleted(@Param("row") NotionDataListUpsertPO row);
}
