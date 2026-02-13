package com.xodud1202.springbackend.domain.admin.news;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 관리자 뉴스 언론사 저장 행 요청 데이터를 보관합니다.
public class AdminNewsPressSaveRowPO {
	private Long pressNo;
	private String pressCd;
	private String pressNm;
	private String useYn;
	private Integer sortSeq;
	private Long regNo;
	private Long udtNo;
}
