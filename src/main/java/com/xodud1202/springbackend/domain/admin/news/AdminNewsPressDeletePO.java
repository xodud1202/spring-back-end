package com.xodud1202.springbackend.domain.admin.news;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
// 관리자 뉴스 언론사 삭제 요청 데이터를 보관합니다.
public class AdminNewsPressDeletePO {
	private List<Long> pressNoList;
	private Long udtNo;
}
