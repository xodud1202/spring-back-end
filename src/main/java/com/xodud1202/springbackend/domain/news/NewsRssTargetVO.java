package com.xodud1202.springbackend.domain.news;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 뉴스 RSS 수집 대상 정보를 보관합니다.
public class NewsRssTargetVO {
	private Long pressNo;
	private String pressNm;
	private String categoryCd;
	private String categoryNm;
	private String sourceNm;
	private String rssUrl;
}
