package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 주문변경 마스터 저장 파라미터를 전달합니다.
public class ShopOrderChangeBaseSavePO {
	// 클레임번호입니다.
	private String clmNo;
	// 주문번호입니다.
	private String ordNo;
	// 변경 구분 코드입니다.
	private String chgGbCd;
	// 변경 일시입니다.
	private String chgDt;
	// 변경 완료 일시입니다.
	private String chgCompleteDt;
	// 변경 상태 코드입니다.
	private String chgStatCd;
	// 클레임 계산에 반영된 배송비 조정 금액입니다.
	private Integer payDelvAmt;
	// 등록자 번호입니다.
	private Long regNo;
	// 수정자 번호입니다.
	private Long udtNo;
}
