package com.xodud1202.springbackend.domain.shop.mypage;

import lombok.Data;

@Data
// 쇼핑몰 마이페이지 교환 신청 상품별 사이즈 선택 옵션을 전달합니다.
public class ShopMypageOrderExchangeSizeOptionVO {
	// 주문상세번호입니다.
	private Integer ordDtlNo;
	// 상품코드입니다.
	private String goodsId;
	// 사이즈 코드입니다.
	private String sizeId;
	// 현재 재고 수량입니다.
	private Integer stockQty;
	// 사이즈 추가 금액입니다.
	private Integer addAmt;
	// 노출 순서입니다.
	private Integer dispOrd;
	// 품절 여부입니다.
	private Boolean soldOut;
}
