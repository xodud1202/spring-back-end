package com.xodud1202.springbackend.domain.admin.goods;

import lombok.Data;

@Data
public class GoodsDescSaveItem {
	private String goodsId;
	private String deviceGbCd;
	private String goodsDesc;
	private Long regNo;
	private Long udtNo;
}
