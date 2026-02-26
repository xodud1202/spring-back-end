package com.xodud1202.springbackend.mapper;

import com.xodud1202.springbackend.domain.notion.NotionWebhookTempEntryPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
// Notion 웹훅 임시 저장용 MyBatis 매퍼 인터페이스입니다.
public interface NotionWebhookMapper {
	// KEY_VALUE_TEMP_TABLE에 웹훅 원문 키/값 목록을 일괄 저장합니다.
	int insertNotionWebhookTempBatch(@Param("rows") List<NotionWebhookTempEntryPO> rows);
}
