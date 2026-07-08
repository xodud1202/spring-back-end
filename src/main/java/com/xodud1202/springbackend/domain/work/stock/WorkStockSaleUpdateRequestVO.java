package com.xodud1202.springbackend.domain.work.stock;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
// 매매일지 수정 요청 값을 전달합니다.
public class WorkStockSaleUpdateRequestVO extends WorkStockSaleCreateRequestVO {
	private Long saleHistSeq;
}
