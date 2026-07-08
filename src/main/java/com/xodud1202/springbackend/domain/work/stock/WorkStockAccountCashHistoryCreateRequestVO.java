package com.xodud1202.springbackend.domain.work.stock;

import lombok.Data;

@Data
// 주식계좌 입출금 등록 요청 값을 전달합니다.
public class WorkStockAccountCashHistoryCreateRequestVO {
	private String cashDt;
	private String stockAccountCd;
	private String cashInOutCd;
	private Long cashAmt;
	private Long regNo;
	private Long udtNo;
}
