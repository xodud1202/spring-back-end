package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 반품 신청 회수지 주소 정보를 전달합니다.
public class ShopOrderReturnPickupAddressPO {
	// 받는사람명입니다.
	private String rsvNm;
	// 우편번호입니다.
	private String postNo;
	// 기본주소입니다.
	private String baseAddress;
	// 상세주소입니다.
	private String detailAddress;
}
