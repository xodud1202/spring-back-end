package com.xodud1202.springbackend.domain.admin.news;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 관리자 뉴스 카테고리 저장 행 요청 데이터를 보관합니다.
public class AdminNewsCategorySaveRowPO {
	private Long pressNo;
	private String categoryCd;
	private String categoryNm;
	private String useYn;
	private Integer sortSeq;
	private String sourceNm;
	private String rssUrl;
	private Long regNo;
	private Long udtNo;
}
