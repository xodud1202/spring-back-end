package com.xodud1202.springbackend.domain.admin.goods;

import lombok.Data;

@Data
public class GoodsCategoryVO {
	private String goodsId;
	private String categoryId;
	private String level1Id;
	private String level2Id;
	private String level3Id;
	private Integer dispOrd;
}
