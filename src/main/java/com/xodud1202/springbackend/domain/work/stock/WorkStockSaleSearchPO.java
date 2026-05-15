package com.xodud1202.springbackend.domain.work.stock;

import lombok.Data;

import java.util.List;

@Data
// 매매일지 조회 조건과 페이징 조건을 MyBatis에 전달합니다.
public class WorkStockSaleSearchPO {
	private String startSaleDt;
	private String endSaleDt;
	private List<String> stockAccountCdList;
	private List<String> stockNmCdList;
	private Integer pageNo;
	private Integer pageSize;
	private Integer offset;
}
