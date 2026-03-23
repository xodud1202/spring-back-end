package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

import java.util.List;

@Data
// 주문취소 요청 본문을 전달합니다.
public class ShopOrderCancelPO {
	// 주문번호입니다.
	private String ordNo;
	// 취소 사유 코드입니다.
	private String reasonCd;
	// 취소 사유 상세 내용입니다.
	private String reasonDetail;
	// 취소 요청 주문상품 목록입니다.
	private List<ShopOrderCancelItemPO> cancelItemList;
	// 화면에서 계산한 취소 예정 금액 요약입니다.
	private ShopOrderCancelPreviewAmountPO previewAmount;
}
