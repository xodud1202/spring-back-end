package com.xodud1202.springbackend.domain.admin.goods;

import lombok.Data;

@Data
public class GoodsImageVO {
	private Integer imgNo;
	private String goodsId;
	private Integer dispOrd;
	private String imgPath;
	private String imgUrl;
}
