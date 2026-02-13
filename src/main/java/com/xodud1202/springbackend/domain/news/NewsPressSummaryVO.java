package com.xodud1202.springbackend.domain.news;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 뉴스 언론사 목록 응답 데이터를 보관합니다.
public class NewsPressSummaryVO {
	private String id;
	private String name;
}
