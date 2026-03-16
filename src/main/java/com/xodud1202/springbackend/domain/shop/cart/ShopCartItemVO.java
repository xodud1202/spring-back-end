package com.xodud1202.springbackend.domain.shop.cart;

import lombok.Data;

import java.util.List;

@Data
// 쇼핑몰 장바구니 상품 행 데이터를 전달합니다.
public class ShopCartItemVO {
	// 고객번호입니다.
	private Long custNo;
	// 상품코드입니다.
	private String goodsId;
	// 브랜드 번호입니다.
	private Integer brandNo;
	// 상품명입니다.
	private String goodsNm;
	// 선택된 사이즈 코드입니다.
	private String sizeId;
	// 담긴 수량입니다.
	private Integer qty;
	// 공급가 단가입니다.
	private Integer supplyAmt;
	// 판매가 단가입니다.
	private Integer saleAmt;
	// 대표 이미지 원본 경로입니다.
	private String imgPath;
	// 대표 이미지 조회 URL입니다.
	private String imgUrl;
	// 선택 가능한 사이즈 옵션 목록입니다.
	private List<ShopCartSizeOptionVO> sizeOptions;
}
