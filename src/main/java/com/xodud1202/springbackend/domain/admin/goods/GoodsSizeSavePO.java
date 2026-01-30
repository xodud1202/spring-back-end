package com.xodud1202.springbackend.domain.admin.goods;

import lombok.Data;

@Data
public class GoodsSizeSavePO {
	private String goodsId;
	private String sizeId;
	private String originSizeId;
	private Integer stockQty;
	private Integer addAmt;
	private String erpSyncYn;
	private String erpSizeCd;
	private Integer dispOrd;
	private String delYn;
	private Long regNo;
	private Long udtNo;
}
