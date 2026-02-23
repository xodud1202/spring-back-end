package com.xodud1202.springbackend.domain.news;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
// 뉴스 확장프로그램 단일 호출 스냅샷 응답 데이터를 보관합니다.
public class NewsSnapshotVO {
	private List<NewsPressSummaryVO> pressList;
	private List<NewsCategorySummaryVO> categoryList;
	private List<NewsTopArticleVO> articleList;
	private String selectedPressId;
	private String selectedCategoryId;
	private String fallbackAppliedYn;
}
