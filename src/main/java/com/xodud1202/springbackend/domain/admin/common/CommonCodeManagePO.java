package com.xodud1202.springbackend.domain.admin.common;

import lombok.Data;

@Data
// 공통 코드 관리 저장 요청 정보를 전달합니다.
public class CommonCodeManagePO {
	private String originGrpCd;
	private String originCd;
	private String grpCd;
	private String cd;
	private String cdNm;
	private String cdDesc;
	private String useYn;
	private Integer dispOrd;
	private Long regNo;
	private Long udtNo;
}
