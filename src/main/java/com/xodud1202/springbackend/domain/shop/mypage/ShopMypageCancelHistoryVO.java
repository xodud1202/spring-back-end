package com.xodud1202.springbackend.domain.shop.mypage;

import lombok.Data;

import java.util.List;

@Data
// 쇼핑몰 마이페이지 취소내역 클레임 단위 항목을 전달합니다.
public class ShopMypageCancelHistoryVO {
	// 클레임 번호입니다.
	private String clmNo;
	// 주문 번호입니다.
	private String ordNo;
	// 취소 신청 일시입니다.
	private String chgDt;
	// 변경 상태 코드입니다.
	private String chgStatCd;
	// 변경 상태명입니다.
	private String chgStatNm;
	// 배송비 조정 금액입니다.
	private Integer payDelvAmt;
	// 환불된 현금 금액입니다.
	private Integer refundedCashAmt;
	// 복원된 포인트 금액입니다.
	private Integer restoredPointAmt;
	// 취소 상품 상세 목록입니다.
	private List<ShopMypageCancelHistoryDetailVO> detailList;
}
