package com.xodud1202.springbackend.domain.shop.goods;

import lombok.Data;

@Data
// 쇼핑몰 상품상세 상단의 기본 상품 정보를 전달합니다.
public class ShopGoodsBasicVO {
	// 상품 코드입니다.
	private String goodsId;
	// 상품명입니다.
	private String goodsNm;
	// 상품 그룹 코드입니다.
	private String goodsGroupId;
	// 브랜드 번호입니다.
	private Integer brandNo;
	// 브랜드명입니다.
	private String brandNm;
	// 브랜드 로고 경로입니다.
	private String brandLogoPath;
	// 브랜드 안내문구(HTML)입니다.
	private String brandNoti;
	// ERP 컬러 코드입니다.
	private String erpColorCd;
	// 컬러명입니다.
	private String colorNm;
	// 컬러 RGB 값입니다.
	private String colorRgb;
	// 공급가입니다.
	private Integer supplyAmt;
	// 판매가입니다.
	private Integer saleAmt;
	// 노출 여부입니다.
	private String showYn;
	// 상품 상태 코드입니다.
	private String goodsStatCd;
}
