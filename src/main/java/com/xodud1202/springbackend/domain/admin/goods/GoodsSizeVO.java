package com.xodud1202.springbackend.domain.admin.goods;

import lombok.Data;

@Data
public class GoodsSizeVO {
	private String goodsId;
	private String sizeId;
	private Integer stockQty;
	private Integer addAmt;
	private String erpSyncYn;
	private String erpSizeCd;
	private Integer dispOrd;
	private String delYn;
}
