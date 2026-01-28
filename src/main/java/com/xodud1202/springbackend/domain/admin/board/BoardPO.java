package com.xodud1202.springbackend.domain.admin.board;

import lombok.Data;

@Data
public class BoardPO {
	private String boardDetailDivCd;
	private String title;
	private Integer page;
	private Integer pageSize;
	private Integer offset;
}
