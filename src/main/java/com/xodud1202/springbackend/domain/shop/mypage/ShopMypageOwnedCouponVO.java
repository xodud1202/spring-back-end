package com.xodud1202.springbackend.domain.shop.mypage;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
// 쇼핑몰 마이페이지 쿠폰함 보유 쿠폰 정보를 전달합니다.
public class ShopMypageOwnedCouponVO {
	// 고객 보유 쿠폰 번호입니다.
	private Long custCpnNo;
	// 쿠폰 번호입니다.
	private Long cpnNo;
	// 쿠폰명입니다.
	private String cpnNm;
	// 쿠폰 종류 코드입니다.
	private String cpnGbCd;
	// 쿠폰 종류명입니다.
	private String cpnGbNm;
	// 쿠폰 할인 구분 코드입니다.
	private String cpnDcGbCd;
	// 쿠폰 할인 값입니다.
	private Integer cpnDcVal;
	// 쿠폰 대상 코드입니다.
	private String cpnTargetCd;
	// 사용 가능 시작 일시입니다.
	private LocalDateTime cpnUsableStartDt;
	// 사용 가능 종료 일시입니다.
	private LocalDateTime cpnUsableEndDt;
	// 쿠폰 사용 불가 상품 전체 건수입니다.
	private Integer unavailableGoodsCount;
	// 쿠폰 사용 불가 상품 목록입니다.
	private List<ShopMypageCouponUnavailableGoodsVO> unavailableGoodsList;
}
