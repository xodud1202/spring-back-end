package com.xodud1202.springbackend.domain.admin.order;

import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderCancelReasonVO;
import lombok.Data;

import java.util.List;

@Data
// 관리자 반품 회수완료 검수 팝업 전체 응답을 전달합니다.
public class AdminOrderReturnManagePickupCompletePageVO {
	// 클레임 기본 정보입니다.
	private AdminOrderReturnManagePickupCompleteClaimVO claim;
	// 반품 상품 목록입니다.
	private List<AdminOrderReturnManagePickupCompleteDetailVO> detailList;
	// 반품 사유 목록입니다.
	private List<ShopMypageOrderCancelReasonVO> reasonList;
	// 공통 기본 반품 사유 코드입니다.
	private String defaultReasonCd;
	// 공통 기본 반품 사유 상세입니다.
	private String defaultReasonDetail;
	// 상품별 저장 사유가 서로 다른지 여부입니다.
	private Boolean mixedReasonYn;
	// 고정 금액 요약입니다.
	private AdminOrderReturnManagePickupCompletePreviewAmountVO previewAmount;
	// 회사 귀책 선택 시 배송비 조정 금액입니다.
	private Long companyFaultShippingAdjustmentAmt;
	// 고객 귀책 선택 시 배송비 조정 금액입니다.
	private Long customerFaultShippingAdjustmentAmt;
}
