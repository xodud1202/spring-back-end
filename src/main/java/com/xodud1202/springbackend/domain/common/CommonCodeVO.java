package com.xodud1202.springbackend.domain.common;

import lombok.Data;

@Data
// 공통 코드 정보를 전달합니다.
public class CommonCodeVO {
	private String grpCd;
	private String cd;
	private String cdNm;
	private String cdDesc;
	private String useYn;
	private Integer dispOrd;
	private Long regNo;
	private String regDt;
	private Long udtNo;
	private String udtDt;
}
