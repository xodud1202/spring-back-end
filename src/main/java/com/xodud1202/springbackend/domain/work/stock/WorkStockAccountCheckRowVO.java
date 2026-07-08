package com.xodud1202.springbackend.domain.work.stock;

import lombok.Data;

@Data
// 계좌 확인금액 원천 행을 전달합니다.
public class WorkStockAccountCheckRowVO {
	private Long stockCheckSeq;
	private String checkDt;
	private String monthKey;
	private String stockAccountCd;
	private Long stockTotalAmt;
	private Long regNo;
	private Long udtNo;
}
