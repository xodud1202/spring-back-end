package com.xodud1202.springbackend.domain.work.stock;

import lombok.Data;

@Data
// 계좌별 월간 입금과 출금 금액을 전달합니다.
public class WorkStockAccountMonthlyCashAmountVO {
	private String monthKey;
	private String stockAccountCd;
	private Long depositAmt;
	private Long withdrawAmt;
}
