package com.xodud1202.springbackend.domain.admin.order;

import com.xodud1202.springbackend.domain.shop.order.ShopOrderReturnPreviewAmountPO;
import lombok.Data;

@Data
// 관리자 반품 회수완료 저장 요청을 전달합니다.
public class AdminOrderReturnManagePickupCompleteSavePO {
	// 클레임번호입니다.
	private String clmNo;
	// 공통 반품 사유 코드입니다.
	private String reasonCd;
	// 공통 반품 사유 상세입니다.
	private String reasonDetail;
	// 화면에서 계산한 반품 예정 금액입니다.
	private ShopOrderReturnPreviewAmountPO previewAmount;
}
