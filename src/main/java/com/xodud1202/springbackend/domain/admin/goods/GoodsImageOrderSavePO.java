package com.xodud1202.springbackend.domain.admin.goods;

import lombok.Data;

import java.util.List;

@Data
public class GoodsImageOrderSavePO {
	private String goodsId;
	private Long udtNo;
	private List<GoodsImageOrderItem> orders;
}
