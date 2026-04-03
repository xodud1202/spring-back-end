package com.xodud1202.springbackend.domain.shop.mypage;

import lombok.Data;

@Data
// 쇼핑몰 마이페이지 반품상세 화면 전체 응답을 전달합니다.
public class ShopMypageReturnDetailPageVO {
	// 반품 클레임 기본 정보입니다.
	private ShopMypageReturnHistoryVO returnItem;
	// 반품 예정 금액 요약입니다.
	private ShopMypageReturnPreviewAmountVO previewAmount;
	// 반품 회수지 정보입니다.
	private ShopMypageReturnPickupAddressVO pickupAddress;
	// 고객 연락처입니다.
	private String customerPhoneNumber;
}
