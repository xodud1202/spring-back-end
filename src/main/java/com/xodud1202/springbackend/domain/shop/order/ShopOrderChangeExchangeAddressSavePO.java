package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 주문변경 회수지/도착지 주소 저장 파라미터를 전달합니다.
public class ShopOrderChangeExchangeAddressSavePO {
	// 클레임번호입니다.
	private String clmNo;
	// 클레임 주소 구분 코드입니다.
	private String clmAddrGbCd;
	// 주소명입니다.
	private String addrName;
	// 우편번호입니다.
	private String addrPostNo;
	// 기본주소입니다.
	private String addrBase;
	// 상세주소입니다.
	private String addrDtl;
	// 등록자 번호입니다.
	private Long regNo;
	// 수정자 번호입니다.
	private Long udtNo;
}
