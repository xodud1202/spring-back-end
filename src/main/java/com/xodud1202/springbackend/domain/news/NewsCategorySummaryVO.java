package com.xodud1202.springbackend.domain.news;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 뉴스 카테고리 목록 응답 데이터를 보관합니다.
public class NewsCategorySummaryVO {
	private String id;
	private String name;
}
