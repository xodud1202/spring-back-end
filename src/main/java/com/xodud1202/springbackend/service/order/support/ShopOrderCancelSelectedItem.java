package com.xodud1202.springbackend.service.order.support;

import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderDetailItemVO;

/**
 * @param detailItem              취소 대상 주문상세 행입니다.
 * @param cancelQty               취소 수량입니다.
 * @param remainingAfterCancelQty 취소 후 남는 수량입니다.
 * @param nextOrdDtlStatCd        취소 반영 후 주문상세 상태코드입니다.
 */ // 주문취소 대상 주문상세 1건과 취소수량 정보를 전달합니다.
public record ShopOrderCancelSelectedItem(
	ShopMypageOrderDetailItemVO detailItem,
	int cancelQty,
	int remainingAfterCancelQty,
	String nextOrdDtlStatCd
) {
	// 주문취소 대상 주문상세 1건과 취소수량 정보를 생성합니다.
	public ShopOrderCancelSelectedItem {
	}
}

