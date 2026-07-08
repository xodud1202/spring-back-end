package com.xodud1202.springbackend.domain.work.stock;

import lombok.Data;

import java.math.BigDecimal;

@Data
// 주식계좌이력 전체 이력 탭의 단일 확인일 행을 전달합니다.
public class WorkStockAccountDailyHistoryRowVO {
	private String checkDt;
	private Long principalAmt;
	private Long checkAmt;
	private Long profitAmt;
	private BigDecimal profitRate;
}
