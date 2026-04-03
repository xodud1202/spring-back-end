package com.xodud1202.springbackend.domain.shop.mypage;

import lombok.Data;

@Data
// 쇼핑몰 마이페이지 반품상세 회수지 정보를 전달합니다.
public class ShopMypageReturnPickupAddressVO {
	// 받는 사람 이름입니다.
	private String rsvNm;
	// 우편번호입니다.
	private String postNo;
	// 기본주소입니다.
	private String baseAddress;
	// 상세주소입니다.
	private String detailAddress;
}
