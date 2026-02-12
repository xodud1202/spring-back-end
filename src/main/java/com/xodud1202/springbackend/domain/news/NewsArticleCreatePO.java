package com.xodud1202.springbackend.domain.news;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
// 뉴스 기사 저장 요청 데이터를 보관합니다.
public class NewsArticleCreatePO {
	private Long pressNo;
	private String categoryCd;
	private String articleGuid;
	private String articleGuidHash;
	private String articleUrl;
	private String articleUrlHash;
	private String articleTitle;
	private String articleSummary;
	private String thumbnailUrl;
	private String authorNm;
	private LocalDateTime publishedDt;
	private BigDecimal rankScore;
	private String useYn;
}
