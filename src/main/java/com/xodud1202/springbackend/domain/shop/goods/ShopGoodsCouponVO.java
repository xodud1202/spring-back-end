package com.xodud1202.springbackend.domain.shop.goods;

import lombok.Data;

@Data
// 쇼핑몰 상품상세 노출용 상품쿠폰 정보를 전달합니다.
public class ShopGoodsCouponVO {
	// 쿠폰 번호입니다.
	private Long cpnNo;
	// 쿠폰명입니다.
	private String cpnNm;
	// 쿠폰 상태 코드입니다.
	private String cpnStatCd;
	// 쿠폰 종류 코드입니다.
	private String cpnGbCd;
	// 쿠폰 타겟 코드입니다.
	private String cpnTargetCd;
	// 할인 구분 코드입니다.
	private String cpnDcGbCd;
	// 할인 값입니다.
	private Integer cpnDcVal;
	// 다운로드 시작 일시입니다.
	private String cpnDownStartDt;
	// 다운로드 종료 일시입니다.
	private String cpnDownEndDt;
	// 사용기간 구분 코드입니다.
	private String cpnUseDtGb;
	// 사용 가능 일수입니다.
	private Integer cpnUsableDt;
	// 사용 시작 일시입니다.
	private String cpnUseStartDt;
	// 사용 종료 일시입니다.
	private String cpnUseEndDt;
}
