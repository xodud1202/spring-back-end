package com.xodud1202.springbackend.domain.shop.goods;

import lombok.Data;

@Data
// 쇼핑몰 상품상세 쿠폰 다운로드 요청 정보를 전달합니다.
public class ShopGoodsCouponDownloadRequestPO {
	// 상품 코드입니다.
	private String goodsId;
	// 다운로드할 쿠폰 번호입니다.
	private Long cpnNo;
}
