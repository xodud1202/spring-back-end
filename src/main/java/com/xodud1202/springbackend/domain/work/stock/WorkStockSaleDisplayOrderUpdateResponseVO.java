package com.xodud1202.springbackend.domain.work.stock;

import lombok.Data;

@Data
// 매매일지 노출순서 저장 결과를 전달합니다.
public class WorkStockSaleDisplayOrderUpdateResponseVO {
	private String message;
	private int accountUpdatedCount;
	private int stockUpdatedCount;
}
