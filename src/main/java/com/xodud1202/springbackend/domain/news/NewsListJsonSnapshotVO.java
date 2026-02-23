package com.xodud1202.springbackend.domain.news;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
// 프론트가 직접 읽는 뉴스 목록 JSON 스냅샷 루트 구조를 보관합니다.
public class NewsListJsonSnapshotVO {
	private Meta meta;
	private List<PressItem> pressList;
	private Map<String, List<CategoryItem>> categoryListByPressId;
	private Map<String, List<ArticleItem>> articleListByPressCategoryKey;
	private DefaultSelection defaultSelection;
	private SnapshotIndex index;

	@Getter
	@Setter
	// 스냅샷 메타 정보를 보관합니다.
	public static class Meta {
		private String generatedAt;
		private String schemaVersion;
		private String source;
		private Integer targetCount;
		private Integer successTargetCount;
		private Integer failedTargetCount;
	}

	@Getter
	@Setter
	// 스냅샷 언론사 항목 정보를 보관합니다.
	public static class PressItem {
		private String id;
		private String name;
		private Integer sortSeq;
		private String useYn;
	}

	@Getter
	@Setter
	// 스냅샷 카테고리 항목 정보를 보관합니다.
	public static class CategoryItem {
		private String id;
		private String name;
		private Integer sortSeq;
		private String useYn;
		private String rssUrl;
		private String sourceNm;
	}

	@Getter
	@Setter
	// 스냅샷 기사 항목 정보를 보관합니다.
	public static class ArticleItem {
		private String id;
		private String title;
		private String url;
		private String publishedDt;
		private String summary;
		private String thumbnailUrl;
		private String authorNm;
		private Integer rankScore;
		private String useYn;
		private String collectedDt;
	}

	@Getter
	@Setter
	// 스냅샷 기본 선택값 정보를 보관합니다.
	public static class DefaultSelection {
		private String defaultPressId;
		private Map<String, String> defaultCategoryIdByPressId;
	}

	@Getter
	@Setter
	// 스냅샷 인덱스 보조 정보를 보관합니다.
	public static class SnapshotIndex {
		private List<String> pressIdList;
		private List<String> categoryKeyList;
	}
}
