package com.xodud1202.springbackend.domain.admin.board;

import lombok.Data;

@Data
public class BoardPO {
	// 게시글 상세 조회 시 게시글 번호를 전달합니다.
	private Long boardNo;
	
	private String boardDetailDivCd;
	private String title;
	private String content;
	// 등록자/수정자 번호를 전달합니다.
	private Long regNo;
	private Long udtNo;
	private Integer page;
	private Integer pageSize;
	private Integer offset;
}
