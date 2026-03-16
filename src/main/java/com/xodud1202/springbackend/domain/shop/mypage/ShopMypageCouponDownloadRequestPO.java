package com.xodud1202.springbackend.domain.shop.mypage;

import lombok.Data;

@Data
// 쇼핑몰 마이페이지 쿠폰 다운로드 요청 정보를 전달합니다.
public class ShopMypageCouponDownloadRequestPO {
	// 다운로드할 쿠폰 번호입니다.
	private Long cpnNo;
}
