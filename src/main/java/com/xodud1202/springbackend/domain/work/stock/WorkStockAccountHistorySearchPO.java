package com.xodud1202.springbackend.domain.work.stock;

import lombok.Data;

import java.util.List;

@Data
// 주식계좌이력 조회 조건을 MyBatis에 전달합니다.
public class WorkStockAccountHistorySearchPO {
	private List<String> stockAccountCdList;
	private Integer cashHistoryOffset;
	private Integer cashHistoryLimit;
}
