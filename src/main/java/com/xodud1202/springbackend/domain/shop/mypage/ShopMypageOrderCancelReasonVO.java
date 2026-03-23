package com.xodud1202.springbackend.domain.shop.mypage;

import lombok.Data;

@Data
// 쇼핑몰 마이페이지 주문취소 사유 코드 정보를 전달합니다.
public class ShopMypageOrderCancelReasonVO {
	// 취소 사유 코드입니다.
	private String cd;
	// 취소 사유 코드명입니다.
	private String cdNm;
}
