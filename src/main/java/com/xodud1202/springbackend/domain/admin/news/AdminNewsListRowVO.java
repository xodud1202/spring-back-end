package com.xodud1202.springbackend.domain.admin.news;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Getter
@Setter
// 관리자 뉴스 목록 조회 결과를 담는 데이터 객체입니다.
public class AdminNewsListRowVO {
	private Long articleNo;
	private Long pressNo;
	private String pressNm;
	private String categoryCd;
	private String categoryNm;
	private String articleTitle;
	private String articleUrl;
	private String thumbnailUrl;
	private BigDecimal rankScore;
	private Timestamp collectedDt;
}
