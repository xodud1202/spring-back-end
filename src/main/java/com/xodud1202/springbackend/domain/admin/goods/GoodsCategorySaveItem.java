package com.xodud1202.springbackend.domain.admin.goods;

import lombok.Data;

@Data
public class GoodsCategorySaveItem {
	private String goodsId;
	private String categoryId;
	private Integer dispOrd;
	private Long regNo;
	private Long udtNo;
}
