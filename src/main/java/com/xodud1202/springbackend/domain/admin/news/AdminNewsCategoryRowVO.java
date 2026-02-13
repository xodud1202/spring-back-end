package com.xodud1202.springbackend.domain.admin.news;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 관리자 뉴스 카테고리 목록 행 데이터를 보관합니다.
public class AdminNewsCategoryRowVO {
	private Long pressNo;
	private String categoryCd;
	private String categoryNm;
	private String useYn;
	private Integer sortSeq;
	private String sourceNm;
	private String rssUrl;
}
