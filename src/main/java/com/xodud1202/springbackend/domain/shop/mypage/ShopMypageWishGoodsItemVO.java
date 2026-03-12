package com.xodud1202.springbackend.domain.shop.mypage;

import lombok.Data;

@Data
// 쇼핑몰 마이페이지 위시리스트 상품 정보를 전달합니다.
public class ShopMypageWishGoodsItemVO {
	// 고객번호입니다.
	private Long custNo;
	// 상품코드입니다.
	private String goodsId;
	// 상품명입니다.
	private String goodsNm;
	// 판매금액입니다.
	private Integer saleAmt;
	// 대표이미지 경로입니다.
	private String imgPath;
	// 대표이미지 URL입니다.
	private String imgUrl;
}
