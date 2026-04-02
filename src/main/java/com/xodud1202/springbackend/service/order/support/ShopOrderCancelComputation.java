package com.xodud1202.springbackend.service.order.support;

import com.xodud1202.springbackend.domain.shop.order.ShopOrderCancelPreviewAmountPO;

import java.util.List;

/**
 * @param selectedItemList      선택된 주문취소 대상 행 목록입니다.
 * @param previewAmount         프론트와 비교할 취소 예정 금액 요약입니다.
 * @param fullCancel            전체취소 여부입니다.
 * @param refundedCashAmt       PG 현금 환불 금액입니다.
 * @param restoredPointAmt      복구할 포인트 금액입니다.
 * @param shippingAdjustmentAmt 배송비 조정 금액입니다.
 */ // 주문취소 계산 결과를 전달합니다.
public record ShopOrderCancelComputation(
	List<ShopOrderCancelSelectedItem> selectedItemList,
	ShopOrderCancelPreviewAmountPO previewAmount,
	boolean fullCancel,
	long refundedCashAmt,
	long restoredPointAmt,
	long shippingAdjustmentAmt
) {
	// 주문취소 계산 결과를 생성합니다.
	public ShopOrderCancelComputation {
		selectedItemList = selectedItemList == null ? List.of() : selectedItemList;
	}
}

