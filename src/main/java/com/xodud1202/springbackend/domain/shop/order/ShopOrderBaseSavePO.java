package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// ORDER_BASE 저장 파라미터를 전달합니다.
public class ShopOrderBaseSavePO {
	// 주문번호입니다.
	private String ordNo;
	// 고객번호입니다.
	private Long custNo;
	// 주문상태 코드입니다.
	private String ordStatCd;
	// 받는 사람 이름입니다.
	private String rcvNm;
	// 우편번호입니다.
	private String rcvPostNo;
	// 기본주소입니다.
	private String rcvAddrBase;
	// 상세주소입니다.
	private String rcvAddrDtl;
	// 배송비 쿠폰 고객쿠폰번호입니다.
	private Long delvCpnNo;
	// 배송비 쿠폰 할인 금액입니다.
	private Integer delvCpnDcAmt;
	// 배송비 쿠폰 적용 전 배송비 금액입니다.
	private Integer ordDelvAmt;
	// 장바구니 구매 여부입니다.
	private String cartYn;
	// 결제 디바이스 코드입니다.
	private String deviceGbCd;
	// 등록자 번호입니다.
	private Long regNo;
	// 수정자 번호입니다.
	private Long udtNo;
}
