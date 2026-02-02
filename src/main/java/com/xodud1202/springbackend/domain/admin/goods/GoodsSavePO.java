package com.xodud1202.springbackend.domain.admin.goods;

import lombok.Data;

import java.util.List;

// 관리자 상품 저장 요청 데이터를 정의합니다.
@Data
public class GoodsSavePO {
	private String goodsId;
	private Integer brandNo;
	private String goodsDivCd;
	private String goodsStatCd;
	private String goodsNm;
	private String goodsGroupId;
	private String goodsMerchId;
	private Integer supplyAmt;
	private Integer saleAmt;
	private String showYn;
	private Integer erpSupplyAmt;
	private Integer erpCostAmt;
	private String erpStyleCd;
	private String erpColorCd;
	private String erpMerchCd;
	private List<GoodsCategoryItem> categoryList;
	private Long regNo;
	private Long udtNo;
}
