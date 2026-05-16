package com.xodud1202.springbackend.domain.work.stock;

import lombok.Data;

import java.util.List;

@Data
// 매매일지 계좌와 주식 노출순서 저장 요청을 전달합니다.
public class WorkStockSaleDisplayOrderUpdateRequestVO {
	private List<WorkStockSaleDisplayOrderItemPO> accountOrderList;
	private List<WorkStockSaleDisplayOrderItemPO> stockOrderList;
}
