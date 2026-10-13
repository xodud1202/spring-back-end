package com.xodud1202.springbackend.domain.work.stock;

import lombok.Data;

import java.math.BigDecimal;

@Data
// 원금을 100%로 보는 그래프의 확인일별 금액과 비율을 전달합니다.
public class WorkStockAccountProfitGraphPointVO {
	private String checkDt;
	private Long principalAmt;
	private Long checkAmt;
	// 원금이 0 이하이면 계산할 수 없으므로 null을 전달합니다.
	private BigDecimal principalRatio;
}
