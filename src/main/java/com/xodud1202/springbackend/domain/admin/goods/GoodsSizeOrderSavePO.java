package com.xodud1202.springbackend.domain.admin.goods;

import lombok.Data;

import java.util.List;

@Data
public class GoodsSizeOrderSavePO {
	private String goodsId;
	private Long udtNo;
	private List<GoodsSizeOrderItem> orders;
}
