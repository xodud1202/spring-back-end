package com.xodud1202.springbackend.domain.admin.order;

import com.xodud1202.springbackend.domain.shop.order.ShopOrderReturnPickupAddressPO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderReturnPreviewAmountPO;
import lombok.Data;

import java.util.List;

@Data
// 관리자 주문반품 요청 본문을 전달합니다.
public class AdminOrderReturnPO {
	// 주문번호입니다.
	private String ordNo;
	// 공통 반품 사유 코드입니다.
	private String reasonCd;
	// 공통 반품 사유 상세입니다.
	private String reasonDetail;
	// 반품 요청 주문상품 목록입니다.
	private List<AdminOrderReturnItemPO> returnItemList;
	// 화면에서 계산한 반품 예정 금액 요약입니다.
	private ShopOrderReturnPreviewAmountPO previewAmount;
	// 회수지 정보입니다.
	private ShopOrderReturnPickupAddressPO pickupAddress;
}
