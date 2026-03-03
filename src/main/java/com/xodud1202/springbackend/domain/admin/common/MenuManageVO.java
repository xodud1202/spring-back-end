package com.xodud1202.springbackend.domain.admin.common;

import lombok.Data;

@Data
// 메뉴 관리 응답 데이터를 전달합니다.
public class MenuManageVO {
	private int menuNo;
	private int upMenuNo;
	private int menuLevel;
	private String menuNm;
	private String menuUrl;
	private int sortSeq;
	private String useYn;
	private int childCount;
}
