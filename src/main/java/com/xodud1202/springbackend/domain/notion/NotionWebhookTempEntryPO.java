package com.xodud1202.springbackend.domain.notion;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// Notion 웹훅 임시 저장용 KEY_VALUE_TEMP_TABLE 입력 파라미터 객체입니다.
public class NotionWebhookTempEntryPO {
	private String requestUrl;
	private String tempKey;
	private String tempValue;
}
