package com.xodud1202.springbackend.domain.work.stock;

import lombok.Data;

@Data
// 매매일지 상세 목록의 단일 거래 행을 전달합니다.
public class WorkStockSaleRowVO {
	private Long saleHistSeq;
	private String saleDt;
	private String stockAccountCd;
	private String stockAccountNm;
	private String stockNmCd;
	private String stockNm;
	private Integer saleCnt;
	private Long saleAmt;
	private Long profitAmt;
	private String memo;
}
