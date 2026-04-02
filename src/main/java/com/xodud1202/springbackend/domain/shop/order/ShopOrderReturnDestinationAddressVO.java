package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 반품 도착지 창고 주소 정보를 전달합니다.
public class ShopOrderReturnDestinationAddressVO {
	// 주소명입니다.
	private String addrName;
	// 우편번호입니다.
	private String addrPostNo;
	// 기본주소입니다.
	private String addrBase;
	// 상세주소입니다.
	private String addrDtl;
}
