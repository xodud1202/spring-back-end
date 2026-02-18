package com.xodud1202.springbackend.domain.admin.news;

import lombok.Data;

@Data
// 관리자 뉴스 목록 조회 조건을 전달하는 데이터 객체입니다.
public class AdminNewsListQueryPO {
	private Long pressNo;
	private String categoryCd;
	private String collectedFrom;
	private String collectedTo;
	private Integer page;
	private Integer pageSize;
	private Integer offset;
}
