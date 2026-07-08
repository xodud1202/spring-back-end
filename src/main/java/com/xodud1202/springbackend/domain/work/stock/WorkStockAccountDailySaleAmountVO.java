package com.xodud1202.springbackend.domain.work.stock;

import lombok.Data;

@Data
// 일자별 계좌 입출금 순원금 합계를 전달합니다.
public class WorkStockAccountDailySaleAmountVO {
	private String saleDt;
	private Long netSaleAmt;
}
