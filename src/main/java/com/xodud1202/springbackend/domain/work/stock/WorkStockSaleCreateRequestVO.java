package com.xodud1202.springbackend.domain.work.stock;

import lombok.Data;

@Data
// 매매일지 등록 요청 값을 전달합니다.
public class WorkStockSaleCreateRequestVO {
	private String saleDt;
	private String stockAccountCd;
	private String stockNmCd;
	private Integer saleCnt;
	private Long saleAmt;
	private Long profitAmt;
	private String memo;
	private Long regNo;
	private Long udtNo;
}
