package com.xodud1202.springbackend.domain.work.stock;

import lombok.Data;

@Data
// 매매일지 선택 항목 노출순서 저장 대상을 전달합니다.
public class WorkStockSaleDisplayOrderItemPO {
	private String cd;
	private Integer dispOrd;
}
