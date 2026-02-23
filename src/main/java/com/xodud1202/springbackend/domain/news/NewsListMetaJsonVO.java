package com.xodud1202.springbackend.domain.news;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
// 뉴스 목록 메타 JSON 파일 구조를 보관합니다.
public class NewsListMetaJsonVO {
	private Meta meta;
	private List<NewsListJsonSnapshotVO.PressItem> pressList;
	private Map<String, List<NewsListJsonSnapshotVO.CategoryItem>> categoryListByPressId;
	private NewsListJsonSnapshotVO.DefaultSelection defaultSelection;
	private Map<String, String> articleFileByPressId;

	@Getter
	@Setter
	// 메타 파일의 생성 메타 정보를 보관합니다.
	public static class Meta {
		private String generatedAt;
		private String schemaVersion;
		private String source;
		private Integer targetCount;
		private Integer successTargetCount;
		private Integer failedTargetCount;
	}
}
