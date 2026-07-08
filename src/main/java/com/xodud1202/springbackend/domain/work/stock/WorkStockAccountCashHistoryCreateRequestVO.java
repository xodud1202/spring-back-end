package com.xodud1202.springbackend.domain.work.stock;

import lombok.Data;

@Data
// 주식계좌 입출금 등록, 수정, 목록 행 값을 전달합니다.
public class WorkStockAccountCashHistoryCreateRequestVO {
	private Long cashHistSeq;
	private String cashDt;
	private String stockAccountCd;
	private String stockAccountNm;
	private String cashInOutCd;
	private String cashInOutNm;
	private Long cashAmt;
	private Long regNo;
	private Long udtNo;
}
