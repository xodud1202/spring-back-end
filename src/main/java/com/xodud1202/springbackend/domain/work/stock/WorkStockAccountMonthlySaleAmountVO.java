package com.xodud1202.springbackend.domain.work.stock;

import lombok.Data;

@Data
// 계좌별 월간 매수, 매도, 순원금 금액을 전달합니다.
public class WorkStockAccountMonthlySaleAmountVO {
	private String monthKey;
	private String stockAccountCd;
	private Long buyAmt;
	private Long sellAmt;
	private Long netSaleAmt;
}
