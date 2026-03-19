package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 쇼핑몰 주문서 배송지 수정 요청 데이터를 전달합니다.
public class ShopOrderAddressUpdatePO {
	// 수정 대상 기존 배송지 별칭입니다.
	private String originAddressNm;
	// 변경할 배송지 별칭입니다.
	private String addressNm;
	// 우편번호입니다.
	private String postNo;
	// 기본주소입니다.
	private String baseAddress;
	// 상세주소입니다.
	private String detailAddress;
	// 받는 사람 연락처입니다.
	private String phoneNumber;
	// 받는 사람 이름입니다.
	private String rsvNm;
	// 기본 배송지 저장 여부입니다.
	private String defaultYn;
}
