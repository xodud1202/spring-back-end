package com.xodud1202.springbackend.domain.shop.goods;

import lombok.Data;

@Data
// 상품상세 쿠폰 적용/제외 대상 정보를 전달합니다.
public class ShopGoodsCouponTargetVO {
	// 쿠폰 번호입니다.
	private Long cpnNo;
	// 대상 구분 코드입니다.
	private String targetGbCd;
	// 대상 값입니다.
	private String targetValue;
}
