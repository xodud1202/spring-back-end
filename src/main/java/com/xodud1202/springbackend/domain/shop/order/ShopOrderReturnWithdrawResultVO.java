package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 쇼핑몰 마이페이지 반품 철회 조회/결과 정보를 함께 전달합니다.
public class ShopOrderReturnWithdrawResultVO {
	// 클레임번호입니다.
	private String clmNo;
	// 주문번호입니다.
	private String ordNo;
	// 주문상세번호입니다.
	private Integer ordDtlNo;
	// 변경된 주문변경상세 상태코드입니다.
	private String chgDtlStatCd;
	// 이번 철회로 클레임 전체가 닫혔는지 여부입니다.
	private Boolean claimClosedYn;
	// 실제 업데이트 건수입니다.
	private Integer updatedCount;
}
