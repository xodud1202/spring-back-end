package com.xodud1202.springbackend.domain.work.stock;

import lombok.Data;

import java.math.BigDecimal;

@Data
// 매매일지 검색 조건에 맞는 종목별 합계 행을 전달합니다.
public class WorkStockSaleSummaryRowVO {
	private String stockNmCd;
	private String stockNm;
	private Long saleCnt;
	private Long saleAmt;
	private BigDecimal averageSaleAmt;
	private Long profitAmt;
}
