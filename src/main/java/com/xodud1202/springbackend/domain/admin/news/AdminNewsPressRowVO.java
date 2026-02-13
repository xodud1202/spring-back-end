package com.xodud1202.springbackend.domain.admin.news;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 관리자 뉴스 언론사 목록 행 데이터를 보관합니다.
public class AdminNewsPressRowVO {
	private Long pressNo;
	private String pressNm;
	private String useYn;
	private Integer sortSeq;
}
