package com.xodud1202.springbackend.domain.admin.common;

import lombok.Data;

@Data
// 메뉴 관리 저장 요청 정보를 전달합니다.
public class MenuManageSavePO {
	private int menuNo;
	private Integer upMenuNo;
	private Integer menuLevel;
	private String menuNm;
	private String menuUrl;
	private Integer sortSeq;
	private String useYn;
	private Long regNo;
	private Long udtNo;
}
