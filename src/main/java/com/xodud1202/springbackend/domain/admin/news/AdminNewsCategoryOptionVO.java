package com.xodud1202.springbackend.domain.admin.news;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 관리자 뉴스 카테고리 선택 목록을 담는 데이터 객체입니다.
public class AdminNewsCategoryOptionVO {
	private String categoryCd;
	private String categoryNm;
}
