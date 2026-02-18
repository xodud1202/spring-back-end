package com.xodud1202.springbackend.domain.admin.news;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 관리자 뉴스 언론사 선택 목록을 담는 데이터 객체입니다.
public class AdminNewsPressOptionVO {
	private Long pressNo;
	private String pressNm;
}
