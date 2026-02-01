package com.xodud1202.springbackend.domain.admin.goods;

import lombok.Data;

import java.util.List;

@Data
public class GoodsDescSavePO {
	private String goodsId;
	private Long regNo;
	private Long udtNo;
	private List<GoodsDescSaveItem> list;
}
