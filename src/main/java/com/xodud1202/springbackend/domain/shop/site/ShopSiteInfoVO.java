package com.xodud1202.springbackend.domain.shop.site;

import lombok.Data;

@Data
// 쇼핑몰 사이트 통합 정보를 전달합니다.
public class ShopSiteInfoVO {
	// 사이트 아이디입니다.
	private String siteId;
	// 사이트명입니다.
	private String siteNm;
	// 기본 배송비입니다.
	private Integer deliveryFee;
	// 무료배송 기준 금액입니다.
	private Integer deliveryFeeLimit;
	// 회원가입 적립 포인트입니다.
	private Integer joinPoint;
	// 등록자 번호입니다.
	private Long regNo;
	// 등록일시입니다.
	private String regDt;
	// 수정자 번호입니다.
	private Long udtNo;
	// 수정일시입니다.
	private String udtDt;
	// 창고 우편번호입니다.
	private String whPostNo;
	// 창고 기본주소입니다.
	private String whAddrBase;
	// 창고 상세주소입니다.
	private String whAddrDtl;
	// 고객센터 연락처입니다.
	private String csTell;
}
