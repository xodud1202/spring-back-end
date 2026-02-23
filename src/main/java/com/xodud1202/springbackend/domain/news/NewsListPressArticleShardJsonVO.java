package com.xodud1202.springbackend.domain.news;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
// 언론사별 기사 shard JSON 파일 구조를 보관합니다.
public class NewsListPressArticleShardJsonVO {
	private Meta meta;
	private Map<String, List<NewsListJsonSnapshotVO.ArticleItem>> articleListByCategoryId;
	private List<String> categoryOrder;

	@Getter
	@Setter
	// 언론사 shard 파일의 메타 정보를 보관합니다.
	public static class Meta {
		private String generatedAt;
		private String schemaVersion;
		private String pressId;
	}
}
