package com.xodud1202.springbackend.service;

import static com.xodud1202.springbackend.common.util.CommonPaginationUtils.*;
import static com.xodud1202.springbackend.common.util.CommonTextUtils.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.xodud1202.springbackend.common.mybatis.GeneratedLongKey;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderReturnManageClaimItemPO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderReturnManageClaimSummaryVO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderReturnManageListResponseVO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderReturnManageListRowVO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderReturnManagePickupCompleteClaimVO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderReturnManagePickupCompleteDetailVO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderReturnManagePickupCompletePageVO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderReturnManagePickupCompletePreviewAmountVO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderReturnManagePickupCompleteResultVO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderReturnManagePickupCompleteSavePO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderReturnManagePO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderReturnManagePickupItemPO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderReturnManagePickupRequestPO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderReturnManagePickupStartPO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderReturnManageStatusUpdateVO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderReturnItemPO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderReturnPO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderReturnPageVO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderReturnWithdrawItemPO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderReturnWithdrawPO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderReturnWithdrawVO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderClaimRowVO;
import com.xodud1202.springbackend.domain.common.CommonCodeVO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartSiteInfoVO;
import com.xodud1202.springbackend.domain.shop.auth.ShopCustomerPointDetailSavePO;
import com.xodud1202.springbackend.domain.shop.auth.ShopCustomerPointSavePO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderAmountSummaryVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderCancelReasonVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderDetailItemVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderGroupVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderReturnFeeContextVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageReturnDetailPageVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderReturnPageVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageReturnHistoryDetailVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageReturnHistoryPageVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageReturnHistoryVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageReturnPickupAddressVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageReturnPreviewAmountVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderCancelOrderBaseVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderChangeBaseSavePO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderChangeDetailSavePO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderChangeExchangeAddressSavePO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderCustomerInfoVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderPaymentSavePO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderPaymentVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderPointDetailSavePO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderPointDetailVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderRestoreCartItemVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderReturnDestinationAddressVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderReturnItemPO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderReturnPO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderReturnPickupAddressPO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderReturnPreviewAmountPO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderReturnResultVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderReturnWithdrawPO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderReturnWithdrawResultVO;
import com.xodud1202.springbackend.domain.shop.site.ShopSiteInfoVO;
import com.xodud1202.springbackend.entity.UserBaseEntity;
import com.xodud1202.springbackend.mapper.CommonMapper;
import com.xodud1202.springbackend.mapper.OrderMapper;
import com.xodud1202.springbackend.mapper.ShopAuthMapper;
import com.xodud1202.springbackend.mapper.SiteInfoMapper;
import com.xodud1202.springbackend.service.order.support.ShopMypageOrderDateRange;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;

import static com.xodud1202.springbackend.common.Constants.Shop.*;

@Service
@RequiredArgsConstructor
// 주문반품 도메인 비즈니스 로직을 제공합니다.
public class OrderReturnService {
	private static final int ORDER_CHANGE_ADDRESS_NAME_MAX_LENGTH = 20;
	private static final int ORDER_CHANGE_ADDRESS_POST_NO_MAX_LENGTH = 10;
	private static final int ORDER_CHANGE_ADDRESS_BASE_MAX_LENGTH = 100;
	private static final int ORDER_CHANGE_ADDRESS_DETAIL_MAX_LENGTH = 100;
	private static final int ADMIN_ORDER_DEFAULT_PAGE = 1;
	private static final int ADMIN_ORDER_DEFAULT_PAGE_SIZE = 20;
	private static final int ADMIN_ORDER_MAX_PAGE_SIZE = 200;
	private static final String ADMIN_ORDER_RETURN_MANAGE_DELIVERY_COMPANY_GRP_CD = "DELV_COMP";
	private static final int ADMIN_ORDER_RETURN_MANAGE_INVOICE_NO_MAX_LENGTH = 20;
	private static final String COMPANY_FAULT_REASON_PREFIX = "R_2";
	private static final String RETURN_APPLY_DETAIL_STATUS_CODE = SHOP_ORDER_CHANGE_DTL_STAT_RETURN_APPLY;
	private static final String RETURN_PICKUP_REQUEST_DETAIL_STATUS_CODE = "CHG_DTL_STAT_12";
	private static final String RETURN_PICKUP_IN_PROGRESS_DETAIL_STATUS_CODE = "CHG_DTL_STAT_13";
	private static final String RETURN_COMPLETE_DETAIL_STATUS_CODE = "CHG_DTL_STAT_14";
	private static final Set<String> ADMIN_ORDER_RETURN_MANAGE_STATUS_SET = Set.of(
		RETURN_APPLY_DETAIL_STATUS_CODE,
		RETURN_PICKUP_REQUEST_DETAIL_STATUS_CODE,
		RETURN_PICKUP_IN_PROGRESS_DETAIL_STATUS_CODE
	);
	private static final String ADMIN_ORDER_RETURN_MANAGE_STATUS_INVALID_MESSAGE = "반품 상태를 확인해주세요.";
	private static final String ADMIN_ORDER_RETURN_PICKUP_REQUEST_EMPTY_MESSAGE = "반품 신청건을 선택해주세요.";
	private static final String ADMIN_ORDER_RETURN_PICKUP_REQUEST_INVALID_MESSAGE = "반품 신청건만 송장저장이 가능합니다.";
	private static final String ADMIN_ORDER_RETURN_PICKUP_START_EMPTY_MESSAGE = "반품 회수 신청건을 선택해주세요.";
	private static final String ADMIN_ORDER_RETURN_PICKUP_START_INVALID_MESSAGE = "반품 회수 신청건만 회수중 처리가 가능합니다.";
	private static final String ADMIN_ORDER_RETURN_PICKUP_COMPLETE_PAGE_EMPTY_MESSAGE = "클레임번호를 확인해주세요.";
	private static final String ADMIN_ORDER_RETURN_PICKUP_COMPLETE_PAGE_INVALID_MESSAGE = "반품 회수중 건만 조회할 수 있습니다.";
	private static final String ADMIN_ORDER_RETURN_PICKUP_COMPLETE_SAVE_EMPTY_MESSAGE = "반품 정보를 확인해주세요.";
	private static final String ADMIN_ORDER_RETURN_PICKUP_COMPLETE_SAVE_INVALID_MESSAGE = "반품 회수중 건만 반품완료 처리할 수 있습니다.";
	private static final String ADMIN_ORDER_RETURN_PICKUP_COMPLETE_NEGATIVE_MESSAGE = "배송비 차감 후 반품 예정 금액이 0원 미만이라 신청할 수 없습니다.";
	private static final String ADMIN_ORDER_RETURN_PICKUP_COMPANY_REQUIRED_MESSAGE = "택배사를 선택해주세요.";
	private static final String ADMIN_ORDER_RETURN_PICKUP_INVOICE_REQUIRED_MESSAGE = "송장번호를 입력해주세요.";
	private static final String ADMIN_ORDER_RETURN_PICKUP_INVOICE_INVALID_MESSAGE = "송장번호는 숫자 20자리 이하로 입력해주세요.";
	private static final String ADMIN_ORDER_RETURN_WITHDRAW_EMPTY_MESSAGE = "철회할 반품건을 선택해주세요.";
	private static final String ADMIN_ORDER_RETURN_WITHDRAW_INVALID_MESSAGE = "반품 신청건만 철회가 가능합니다.";
	private static final String ADMIN_LOGIN_INVALID_MESSAGE = "관리자 로그인 정보를 확인해주세요.";
	private static final int ADMIN_ORDER_RETURN_COMPANY_FAULT_COUPON_EXTEND_DAYS = 3;
	private static final int ADMIN_ORDER_RETURN_COMPANY_FAULT_POINT_EXPIRE_DAYS = 30;
	private static final String ADMIN_ORDER_RETURN_COMPANY_FAULT_POINT_GIVE_GB_CD = "RETURN_COMP_REASON";
	private static final String ADMIN_ORDER_RETURN_POINT_RESTORE_MEMO = "반품 완료 포인트 복구";

	private final OrderService orderService;
	private final OrderMapper orderMapper;
	private final SiteInfoMapper siteInfoMapper;
	private final CommonMapper commonMapper;
	private final ShopAuthMapper shopAuthMapper;
	private final TossPaymentsClient tossPaymentsClient;
	private final PlatformTransactionManager transactionManager;

	// 반품 신청 선택 상품 계산 결과를 전달합니다.
	private record ShopOrderReturnSelectedItem(
		ShopMypageOrderDetailItemVO detailItem,
		int returnQty,
		String reasonCd,
		String reasonDetail
	) {
	}

	// 반품 신청 계산 결과를 전달합니다.
	private record ShopOrderReturnComputation(
		List<ShopOrderReturnSelectedItem> selectedItemList,
		ShopOrderReturnPreviewAmountPO previewAmount,
		long shippingDeductionAmt
	) {
	}

	// 관리자 반품 회수완료 저장 대상 주문상세 1건을 전달합니다.
	private record AdminOrderReturnManagePickupCompleteSelectedItem(
		ShopMypageOrderDetailItemVO detailItem,
		AdminOrderReturnManagePickupCompleteDetailVO claimDetail,
		int remainingAfterReturnQty
	) {
	}

	// 관리자 반품 회수완료 계산 결과를 전달합니다.
	private record AdminOrderReturnManagePickupCompleteComputation(
		AdminOrderReturnManagePickupCompleteClaimVO claim,
		Long custNo,
		ShopMypageOrderGroupVO orderGroup,
		ShopOrderCancelOrderBaseVO orderBase,
		List<AdminOrderReturnManagePickupCompleteSelectedItem> selectedItemList,
		ShopOrderReturnPreviewAmountPO previewAmount,
		String reasonCd,
		String reasonDetail,
		String reasonName,
		boolean companyFaultReasonYn,
		boolean fullReturnYn,
		long refundedCashAmt,
		long restoredPointAmt,
		long reissuedPointAmt
	) {
	}

	// 관리자 반품 회수완료 PG 취소 결과를 전달합니다.
	private record AdminOrderReturnManagePickupCompletePgResult(
		String rawResponse,
		String rspCode,
		String rspMsg,
		String tradeNo,
		String approvedDt,
		long canceledAmount
	) {
	}

	// 쇼핑몰 마이페이지 반품 신청 화면 데이터를 조회합니다.
	public ShopMypageOrderReturnPageVO getShopMypageOrderReturnPage(Long custNo, String ordNo, Integer ordDtlNo) {
		return orderService.getShopMypageOrderReturnPage(custNo, ordNo, ordDtlNo);
	}

	// 관리자 주문반품 신청 화면 데이터를 조회합니다.
	public AdminOrderReturnPageVO getAdminOrderReturnPage(String ordNo) {
		return orderService.getAdminOrderReturnPage(ordNo);
	}

	// 관리자 반품 회수 관리 목록을 조회합니다.
	public AdminOrderReturnManageListResponseVO getAdminOrderReturnManageList(
		Integer page,
		Integer pageSize,
		String chgDtlStatCd
	) {
		// 페이지 기본값과 오프셋을 계산합니다.
		int resolvedPage = normalizePage(page, ADMIN_ORDER_DEFAULT_PAGE);
		int resolvedPageSize = normalizePageSize(pageSize, ADMIN_ORDER_DEFAULT_PAGE_SIZE, ADMIN_ORDER_MAX_PAGE_SIZE);
		int offset = calculateOffset(resolvedPage, resolvedPageSize);

		// 허용된 반품 상태만 조회 조건으로 사용합니다.
		String normalizedChgDtlStatCd = normalizeAdminOrderReturnManageStatus(chgDtlStatCd);

		// 매퍼 조회용 파라미터를 구성합니다.
		AdminOrderReturnManagePO param = new AdminOrderReturnManagePO();
		param.setPage(resolvedPage);
		param.setPageSize(resolvedPageSize);
		param.setOffset(offset);
		param.setChgDtlStatCd(normalizedChgDtlStatCd);

		// 목록과 건수를 조회합니다.
		List<AdminOrderReturnManageListRowVO> list = orderMapper.getAdminOrderReturnManageList(param);
		int totalCount = orderMapper.getAdminOrderReturnManageCount(param);

		// 목록 응답 객체를 구성합니다.
		AdminOrderReturnManageListResponseVO result = new AdminOrderReturnManageListResponseVO();
		result.setList(list == null ? List.of() : list);
		result.setTotalCount(totalCount);
		result.setPage(resolvedPage);
		result.setPageSize(resolvedPageSize);
		return result;
	}

	// 관리자 반품 클레임을 반품 회수 신청 상태로 변경합니다.
	@Transactional
	public AdminOrderReturnManageStatusUpdateVO requestAdminOrderReturnPickup(AdminOrderReturnManagePickupRequestPO param) {
		// 요청 데이터와 택배사 공통코드를 검증 준비합니다.
		if (param == null) {
			throw new IllegalArgumentException(ADMIN_ORDER_RETURN_PICKUP_REQUEST_EMPTY_MESSAGE);
		}
		Set<String> deliveryCompanyCodeSet = getAdminOrderReturnManageDeliveryCompanyCodeSet();

		// 요청 클레임 목록을 정규화하고 택배사/송장번호를 검증합니다.
		List<AdminOrderReturnManagePickupItemPO> itemList = normalizeAdminOrderReturnManagePickupItemList(
			param.getItemList(),
			deliveryCompanyCodeSet
		);
		if (itemList.isEmpty()) {
			throw new IllegalArgumentException(ADMIN_ORDER_RETURN_PICKUP_REQUEST_EMPTY_MESSAGE);
		}

		// 현재 로그인한 관리자 번호를 감사 컬럼 값으로 사용합니다.
		Long udtNo = resolveCurrentAdminUserNo();
		if (udtNo == null || udtNo < 1L) {
			throw new IllegalArgumentException(ADMIN_LOGIN_INVALID_MESSAGE);
		}

		// 대상 클레임의 현재 상태와 회수지 행 존재 여부를 먼저 검증합니다.
		List<String> clmNoList = itemList.stream()
			.map(AdminOrderReturnManagePickupItemPO::getClmNo)
			.toList();
		int totalDetailCount = validateAdminOrderReturnManageClaimState(
			clmNoList,
			RETURN_APPLY_DETAIL_STATUS_CODE,
			ADMIN_ORDER_RETURN_PICKUP_REQUEST_INVALID_MESSAGE
		);

		// 반품 상세 상태를 반품 회수 신청으로 변경합니다.
		int updatedDetailCount = orderMapper.updateAdminOrderReturnManageStatusByClaimNoList(
			clmNoList,
			RETURN_APPLY_DETAIL_STATUS_CODE,
			RETURN_PICKUP_REQUEST_DETAIL_STATUS_CODE,
			udtNo
		);
		if (updatedDetailCount != totalDetailCount) {
			throw new IllegalArgumentException(ADMIN_ORDER_RETURN_PICKUP_REQUEST_INVALID_MESSAGE);
		}

		// 회수지 행의 택배사와 송장번호를 클레임 단위로 저장합니다.
		int updatedPickupAddressCount = orderMapper.updateAdminOrderReturnManagePickupAddressList(itemList, udtNo);
		if (updatedPickupAddressCount != clmNoList.size()) {
			throw new IllegalArgumentException(ADMIN_ORDER_RETURN_PICKUP_REQUEST_INVALID_MESSAGE);
		}

		// 변경된 클레임 건수를 응답 객체에 담아 반환합니다.
		AdminOrderReturnManageStatusUpdateVO result = new AdminOrderReturnManageStatusUpdateVO();
		result.setUpdatedCount(clmNoList.size());
		return result;
	}

	// 관리자 반품 회수 신청 클레임을 반품 회수중 상태로 변경합니다.
	@Transactional
	public AdminOrderReturnManageStatusUpdateVO startAdminOrderReturnPickup(AdminOrderReturnManagePickupStartPO param) {
		// 요청 클레임 목록을 정규화합니다.
		List<AdminOrderReturnManageClaimItemPO> itemList = normalizeAdminOrderReturnManageClaimItemList(
			param == null ? null : param.getItemList()
		);
		if (itemList.isEmpty()) {
			throw new IllegalArgumentException(ADMIN_ORDER_RETURN_PICKUP_START_EMPTY_MESSAGE);
		}

		// 현재 로그인한 관리자 번호를 감사 컬럼 값으로 사용합니다.
		Long udtNo = resolveCurrentAdminUserNo();
		if (udtNo == null || udtNo < 1L) {
			throw new IllegalArgumentException(ADMIN_LOGIN_INVALID_MESSAGE);
		}

		// 대상 클레임의 현재 상태와 회수지 행 존재 여부를 먼저 검증합니다.
		List<String> clmNoList = itemList.stream()
			.map(AdminOrderReturnManageClaimItemPO::getClmNo)
			.toList();
		int totalDetailCount = validateAdminOrderReturnManageClaimState(
			clmNoList,
			RETURN_PICKUP_REQUEST_DETAIL_STATUS_CODE,
			ADMIN_ORDER_RETURN_PICKUP_START_INVALID_MESSAGE
		);

		// 반품 상세 상태를 반품 회수중으로 변경합니다.
		int updatedDetailCount = orderMapper.updateAdminOrderReturnManageStatusByClaimNoList(
			clmNoList,
			RETURN_PICKUP_REQUEST_DETAIL_STATUS_CODE,
			RETURN_PICKUP_IN_PROGRESS_DETAIL_STATUS_CODE,
			udtNo
		);
		if (updatedDetailCount != totalDetailCount) {
			throw new IllegalArgumentException(ADMIN_ORDER_RETURN_PICKUP_START_INVALID_MESSAGE);
		}

		// 회수지 행에 회수 시작 일시를 저장합니다.
		int updatedPickupAddressCount = orderMapper.updateAdminOrderReturnManagePickupStartAddressList(clmNoList, udtNo);
		if (updatedPickupAddressCount != clmNoList.size()) {
			throw new IllegalArgumentException(ADMIN_ORDER_RETURN_PICKUP_START_INVALID_MESSAGE);
		}

		// 변경된 클레임 건수를 응답 객체에 담아 반환합니다.
		AdminOrderReturnManageStatusUpdateVO result = new AdminOrderReturnManageStatusUpdateVO();
		result.setUpdatedCount(clmNoList.size());
		return result;
	}

	// 관리자 반품 회수완료 검수 팝업 화면 데이터를 조회합니다.
	public AdminOrderReturnManagePickupCompletePageVO getAdminOrderReturnManagePickupCompletePage(String clmNo) {
		// 클레임번호 기본 형식을 검증하고 회수중 상태인지 먼저 확인합니다.
		String normalizedClmNo = trimToNull(clmNo);
		if (normalizedClmNo == null) {
			throw new IllegalArgumentException(ADMIN_ORDER_RETURN_PICKUP_COMPLETE_PAGE_EMPTY_MESSAGE);
		}
		validateAdminOrderReturnManageClaimState(
			List.of(normalizedClmNo),
			RETURN_PICKUP_IN_PROGRESS_DETAIL_STATUS_CODE,
			ADMIN_ORDER_RETURN_PICKUP_COMPLETE_PAGE_INVALID_MESSAGE
		);

		// 팝업 상단에 표시할 클레임 기본 정보와 상세 목록을 조회합니다.
		AdminOrderReturnManagePickupCompleteClaimVO claim = orderMapper.getAdminOrderReturnManagePickupCompleteClaim(normalizedClmNo);
		List<AdminOrderReturnManagePickupCompleteDetailVO> detailList =
			orderMapper.getAdminOrderReturnManagePickupCompleteDetailList(normalizedClmNo);
		if (claim == null || trimToNull(claim.getOrdNo()) == null || detailList == null || detailList.isEmpty()) {
			throw new IllegalArgumentException(ADMIN_ORDER_RETURN_PICKUP_COMPLETE_PAGE_INVALID_MESSAGE);
		}

		// 금액 계산과 사유 목록 구성을 위해 주문 기준 보조 정보를 함께 조회합니다.
		Long custNo = orderMapper.getOrderCustNo(claim.getOrdNo());
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException(ADMIN_ORDER_RETURN_PICKUP_COMPLETE_PAGE_INVALID_MESSAGE);
		}
		AdminOrderReturnPageVO adminOrderReturnPage = orderService.getAdminOrderReturnPage(claim.getOrdNo());
		ShopOrderCancelOrderBaseVO orderBase = orderService.resolveShopOrderCancelOrderBase(custNo, claim.getOrdNo());
		ShopMypageReturnHistoryVO returnItem = buildAdminOrderReturnManagePickupCompleteReturnItem(claim, detailList);

		// 고정 금액 요약과 귀책별 배송비 조정 금액을 계산합니다.
		boolean fullReturnYn = resolveShopMypageReturnFullReturnYn(returnItem, adminOrderReturnPage.getOrder());
		AdminOrderReturnManagePickupCompletePreviewAmountVO previewAmount =
			buildAdminOrderReturnManagePickupCompletePreviewAmount(returnItem, adminOrderReturnPage.getOrder(), orderBase);
		long paidDeliveryFeeRefundAmt = fullReturnYn
			? resolveNonNegativeLong(
				adminOrderReturnPage.getReturnFeeContext() == null
					? null
					: (long) normalizeNonNegativeNumber(adminOrderReturnPage.getReturnFeeContext().getOriginalPaidDeliveryAmt())
			)
			: 0L;
		long beforeShippingExpectedRefundAmt =
			resolveNonNegativeLong(previewAmount.getPaidGoodsAmt()) - resolveNonNegativeLong(previewAmount.getBenefitAmt());
		long customerFaultShippingAdjustmentAmt =
			paidDeliveryFeeRefundAmt - resolveShopOrderReturnShippingDeductionAmt(
				adminOrderReturnPage.getSiteInfo(),
				adminOrderReturnPage.getReturnFeeContext(),
				beforeShippingExpectedRefundAmt,
				false
			);

		// 화면 응답 객체를 구성합니다.
		AdminOrderReturnManagePickupCompletePageVO result = new AdminOrderReturnManagePickupCompletePageVO();
		result.setClaim(claim);
		result.setDetailList(detailList);
		result.setReasonList(adminOrderReturnPage.getReasonList() == null ? List.of() : adminOrderReturnPage.getReasonList());
		applyAdminOrderReturnManagePickupCompleteDefaultReason(result, detailList);
		result.setPreviewAmount(previewAmount);
		result.setCompanyFaultShippingAdjustmentAmt(paidDeliveryFeeRefundAmt);
		result.setCustomerFaultShippingAdjustmentAmt(customerFaultShippingAdjustmentAmt);
		return result;
	}

	// 관리자 반품 회수완료를 저장합니다.
	@Transactional
	public AdminOrderReturnManagePickupCompleteResultVO completeAdminOrderReturnPickup(
		AdminOrderReturnManagePickupCompleteSavePO param
	) {
		// 관리자 로그인 번호와 저장 계산 결과를 먼저 준비합니다.
		Long udtNo = resolveCurrentAdminUserNo();
		if (udtNo == null || udtNo < 1L) {
			throw new IllegalArgumentException(ADMIN_LOGIN_INVALID_MESSAGE);
		}
		AdminOrderReturnManagePickupCompleteComputation computation = buildAdminOrderReturnManagePickupCompleteComputation(param);

		// 현금 환불이 있으면 환불 PAYMENT row를 선등록하고 원결제 정보를 준비합니다.
		ShopOrderPaymentVO originalPayment = null;
		ShopOrderPaymentSavePO refundPaymentSavePO = null;
		if (computation.refundedCashAmt() > 0L) {
			originalPayment = resolveShopOrderPaymentForReturnComplete(computation.claim().getOrdNo());
			refundPaymentSavePO = createAdminOrderReturnRefundPayment(computation, originalPayment, udtNo);
		}

		// 주문/클레임 반영과 PG 환불까지 모두 처리한 뒤 응답 객체를 구성합니다.
		try {
			applyAdminOrderReturnPickupCompleteSuccess(computation, originalPayment, refundPaymentSavePO, udtNo);
		} catch (TossPaymentClientException exception) {
			handleAdminOrderReturnPickupCompletePaymentFailure(
				refundPaymentSavePO == null ? null : refundPaymentSavePO.getPayNo(),
				exception,
				udtNo
			);
			throw new IllegalArgumentException(resolveAdminOrderReturnPickupCompletePgErrorMessage(exception));
		} catch (RuntimeException exception) {
			handleAdminOrderReturnPickupCompletePaymentFailure(
				refundPaymentSavePO == null ? null : refundPaymentSavePO.getPayNo(),
				"RETURN_COMPLETE_ERROR",
				firstNonBlank(exception.getMessage(), "반품완료 처리에 실패했습니다."),
				null,
				udtNo
			);
			throw exception;
		}

		AdminOrderReturnManagePickupCompleteResultVO result = new AdminOrderReturnManagePickupCompleteResultVO();
		result.setClmNo(computation.claim().getClmNo());
		result.setOrdNo(computation.claim().getOrdNo());
		result.setRefundPayNo(refundPaymentSavePO == null ? null : refundPaymentSavePO.getPayNo());
		result.setRefundedCashAmt(computation.refundedCashAmt());
		result.setRestoredPointAmt(computation.restoredPointAmt());
		result.setReissuedPointAmt(computation.reissuedPointAmt());
		return result;
	}

	// 관리자 반품 회수완료 저장 전 검증과 금액 재계산을 수행합니다.
	private AdminOrderReturnManagePickupCompleteComputation buildAdminOrderReturnManagePickupCompleteComputation(
		AdminOrderReturnManagePickupCompleteSavePO param
	) {
		// 요청 클레임과 현재 상태와 조회 대상 기본 데이터를 먼저 검증합니다.
		String clmNo = trimToNull(param == null ? null : param.getClmNo());
		if (clmNo == null) {
			throw new IllegalArgumentException(ADMIN_ORDER_RETURN_PICKUP_COMPLETE_SAVE_EMPTY_MESSAGE);
		}
		validateAdminOrderReturnManageClaimState(
			List.of(clmNo),
			RETURN_PICKUP_IN_PROGRESS_DETAIL_STATUS_CODE,
			ADMIN_ORDER_RETURN_PICKUP_COMPLETE_SAVE_INVALID_MESSAGE
		);
		AdminOrderReturnManagePickupCompleteClaimVO claim = orderMapper.getAdminOrderReturnManagePickupCompleteClaim(clmNo);
		List<AdminOrderReturnManagePickupCompleteDetailVO> detailList =
			orderMapper.getAdminOrderReturnManagePickupCompleteDetailList(clmNo);
		if (claim == null || trimToNull(claim.getOrdNo()) == null || detailList == null || detailList.isEmpty()) {
			throw new IllegalArgumentException(ADMIN_ORDER_RETURN_PICKUP_COMPLETE_SAVE_INVALID_MESSAGE);
		}

		// 주문 기준 조회 데이터와 공통 반품 사유를 함께 검증합니다.
		Long custNo = orderMapper.getOrderCustNo(claim.getOrdNo());
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException(ADMIN_ORDER_RETURN_PICKUP_COMPLETE_SAVE_INVALID_MESSAGE);
		}
		AdminOrderReturnPageVO adminOrderReturnPage = orderService.getAdminOrderReturnPage(claim.getOrdNo());
		ShopMypageOrderGroupVO orderGroup = adminOrderReturnPage == null ? null : adminOrderReturnPage.getOrder();
		ShopOrderCancelOrderBaseVO orderBase = orderService.resolveShopOrderCancelOrderBase(custNo, claim.getOrdNo());
		if (orderGroup == null || orderBase == null) {
			throw new IllegalArgumentException(ADMIN_ORDER_RETURN_PICKUP_COMPLETE_SAVE_INVALID_MESSAGE);
		}

		String reasonCd = trimToNull(param.getReasonCd());
		String reasonDetail = trimToNull(param.getReasonDetail());
		ShopOrderReturnItemPO reasonValidationItem = new ShopOrderReturnItemPO();
		reasonValidationItem.setReasonCd(reasonCd);
		reasonValidationItem.setReasonDetail(reasonDetail);
		validateShopOrderReturnReason(reasonValidationItem, adminOrderReturnPage.getReasonList());

		// 공통 사유 기준으로 화면 금액을 다시 계산하고 위변조를 검증합니다.
		ShopMypageOrderCancelReasonVO matchedReason = findShopOrderReturnReason(adminOrderReturnPage.getReasonList(), reasonCd);
		String reasonName = firstNonBlank(trimToNull(matchedReason == null ? null : matchedReason.getCdNm()), reasonCd);
		boolean companyFaultReasonYn = reasonCd != null && reasonCd.startsWith(COMPANY_FAULT_REASON_PREFIX);
		ShopMypageReturnHistoryVO returnItem = buildAdminOrderReturnManagePickupCompleteReturnItem(claim, detailList);
		applyAdminOrderReturnManagePickupCompleteReason(returnItem, reasonCd, reasonDetail);
		boolean fullReturnYn = resolveShopMypageReturnFullReturnYn(returnItem, orderGroup);
		AdminOrderReturnManagePickupCompletePreviewAmountVO fixedPreviewAmount =
			buildAdminOrderReturnManagePickupCompletePreviewAmount(returnItem, orderGroup, orderBase);
		long beforeShippingExpectedRefundAmt =
			resolveNonNegativeLong(fixedPreviewAmount.getPaidGoodsAmt()) - resolveNonNegativeLong(fixedPreviewAmount.getBenefitAmt());
		long paidDeliveryFeeRefundAmt = fullReturnYn
			? resolveNonNegativeLong(
				adminOrderReturnPage.getReturnFeeContext() == null
					? null
					: (long) normalizeNonNegativeNumber(adminOrderReturnPage.getReturnFeeContext().getOriginalPaidDeliveryAmt())
			)
			: 0L;
		long shippingDeductionAmt = resolveShopOrderReturnShippingDeductionAmt(
			adminOrderReturnPage.getSiteInfo(),
			adminOrderReturnPage.getReturnFeeContext(),
			beforeShippingExpectedRefundAmt,
			companyFaultReasonYn
		);
		long shippingAdjustmentAmt = paidDeliveryFeeRefundAmt - shippingDeductionAmt;
		long expectedRefundAmt =
			resolveNonNegativeLong(fixedPreviewAmount.getPaidGoodsAmt())
				- resolveNonNegativeLong(fixedPreviewAmount.getBenefitAmt())
				+ shippingAdjustmentAmt;
		if (expectedRefundAmt < 0L) {
			throw new IllegalArgumentException(ADMIN_ORDER_RETURN_PICKUP_COMPLETE_NEGATIVE_MESSAGE);
		}

		ShopOrderReturnPreviewAmountPO serverPreviewAmount = new ShopOrderReturnPreviewAmountPO();
		serverPreviewAmount.setExpectedRefundAmt(expectedRefundAmt);
		serverPreviewAmount.setPaidGoodsAmt(resolveNonNegativeLong(fixedPreviewAmount.getPaidGoodsAmt()));
		serverPreviewAmount.setBenefitAmt(resolveNonNegativeLong(fixedPreviewAmount.getBenefitAmt()));
		serverPreviewAmount.setShippingAdjustmentAmt(shippingAdjustmentAmt);
		serverPreviewAmount.setTotalPointRefundAmt(resolveNonNegativeLong(fixedPreviewAmount.getTotalPointRefundAmt()));
		serverPreviewAmount.setDeliveryCouponRefundAmt(resolveNonNegativeLong(fixedPreviewAmount.getDeliveryCouponRefundAmt()));
		validateShopOrderReturnPreviewAmount(param.getPreviewAmount(), serverPreviewAmount);

		// 주문상세 반영용 선택 상품 목록과 포인트 환급 금액 분기를 구성합니다.
		List<AdminOrderReturnManagePickupCompleteSelectedItem> selectedItemList =
			buildAdminOrderReturnManagePickupCompleteSelectedItemList(orderGroup, detailList);
		long pointRefundAmt = resolveNonNegativeLong(serverPreviewAmount.getTotalPointRefundAmt());
		return new AdminOrderReturnManagePickupCompleteComputation(
			claim,
			custNo,
			orderGroup,
			orderBase,
			selectedItemList,
			serverPreviewAmount,
			reasonCd,
			reasonDetail,
			reasonName,
			companyFaultReasonYn,
			fullReturnYn,
			expectedRefundAmt,
			companyFaultReasonYn ? 0L : pointRefundAmt,
			companyFaultReasonYn ? pointRefundAmt : 0L
		);
	}

	// 관리자 반품 회수완료 계산용 반품 이력에 공통 사유를 덮어씁니다.
	private void applyAdminOrderReturnManagePickupCompleteReason(
		ShopMypageReturnHistoryVO returnItem,
		String reasonCd,
		String reasonDetail
	) {
		// 반품 상세 전체를 클레임 공통 사유 1개로 통일합니다.
		for (ShopMypageReturnHistoryDetailVO detailItem : returnItem == null ? List.<ShopMypageReturnHistoryDetailVO>of() : returnItem.getDetailList()) {
			detailItem.setChgReasonCd(reasonCd);
			detailItem.setChgReasonDtl(reasonDetail);
		}
	}

	// 관리자 반품 회수완료 저장용 주문상세 목록을 구성합니다.
	private List<AdminOrderReturnManagePickupCompleteSelectedItem> buildAdminOrderReturnManagePickupCompleteSelectedItemList(
		ShopMypageOrderGroupVO orderGroup,
		List<AdminOrderReturnManagePickupCompleteDetailVO> detailList
	) {
		// 현재 주문상세를 주문상세번호 기준 맵으로 구성합니다.
		Map<Integer, ShopMypageOrderDetailItemVO> detailItemMap = new LinkedHashMap<>();
		for (ShopMypageOrderDetailItemVO detailItem : orderGroup == null ? List.<ShopMypageOrderDetailItemVO>of() : orderGroup.getDetailList()) {
			if (detailItem.getOrdDtlNo() == null) {
				continue;
			}
			detailItemMap.put(detailItem.getOrdDtlNo(), detailItem);
		}

		// 클레임 상세 전체가 현재 주문상세와 정확히 매칭되는지 검증합니다.
		List<AdminOrderReturnManagePickupCompleteSelectedItem> result = new ArrayList<>();
		for (AdminOrderReturnManagePickupCompleteDetailVO claimDetail : detailList == null ? List.<AdminOrderReturnManagePickupCompleteDetailVO>of() : detailList) {
			Integer ordDtlNo = claimDetail.getOrdDtlNo();
			ShopMypageOrderDetailItemVO detailItem = ordDtlNo == null ? null : detailItemMap.get(ordDtlNo);
			int returnQty = normalizeNonNegativeNumber(claimDetail.getQty());
			int currentRemainingQty = resolveShopOrderRemainingQty(detailItem);
			String currentOrdDtlStatCd = trimToNull(detailItem == null ? null : detailItem.getOrdDtlStatCd());
			if (ordDtlNo == null
				|| detailItem == null
				|| currentOrdDtlStatCd == null
				|| returnQty < 1
				|| currentRemainingQty < returnQty) {
				throw new IllegalArgumentException(ADMIN_ORDER_RETURN_PICKUP_COMPLETE_SAVE_INVALID_MESSAGE);
			}
			result.add(new AdminOrderReturnManagePickupCompleteSelectedItem(
				detailItem,
				claimDetail,
				Math.max(currentRemainingQty - returnQty, 0)
			));
		}
		if (result.isEmpty()) {
			throw new IllegalArgumentException(ADMIN_ORDER_RETURN_PICKUP_COMPLETE_SAVE_INVALID_MESSAGE);
		}
		return List.copyOf(result);
	}

	// 관리자 반품 회수완료 성공 시 주문과 클레임과 쿠폰과 포인트와 환불을 함께 반영합니다.
	private void applyAdminOrderReturnPickupCompleteSuccess(
		AdminOrderReturnManagePickupCompleteComputation computation,
		ShopOrderPaymentVO originalPayment,
		ShopOrderPaymentSavePO refundPaymentSavePO,
		Long auditNo
	) {
		// 완료 일시를 고정해 클레임과 쿠폰과 포인트 만료 계산에 공통으로 사용합니다.
		LocalDateTime completedAt = LocalDateTime.now().withNano(0);
		String completedAtText = completedAt.format(SHOP_MYPAGE_ORDER_DATE_TIME_FORMATTER);

		// 클레임 상세와 마스터와 회수지 완료 일시를 함께 반영합니다.
		int updatedDetailCount = orderMapper.updateAdminOrderReturnManagePickupCompleteDetail(
			computation.claim().getClmNo(),
			computation.claim().getOrdNo(),
			RETURN_PICKUP_IN_PROGRESS_DETAIL_STATUS_CODE,
			RETURN_COMPLETE_DETAIL_STATUS_CODE,
			computation.reasonCd(),
			computation.reasonDetail(),
			auditNo
		);
		if (updatedDetailCount != computation.selectedItemList().size()) {
			throw new IllegalArgumentException(ADMIN_ORDER_RETURN_PICKUP_COMPLETE_SAVE_INVALID_MESSAGE);
		}
		int updatedBaseCount = orderMapper.updateAdminOrderReturnManagePickupCompleteBase(
			computation.claim().getClmNo(),
			computation.claim().getOrdNo(),
			SHOP_ORDER_CHANGE_STAT_PROGRESS,
			completedAtText,
			(int) Math.clamp(resolvePreviewAmountValue(computation.previewAmount().getShippingAdjustmentAmt()), (long) Integer.MIN_VALUE, (long) Integer.MAX_VALUE),
			auditNo
		);
		int updatedAddressCount = orderMapper.updateAdminOrderReturnManagePickupCompleteAddress(
			computation.claim().getClmNo(),
			completedAtText,
			auditNo
		);
		if (updatedBaseCount != 1 || updatedAddressCount != 1) {
			throw new IllegalArgumentException(ADMIN_ORDER_RETURN_PICKUP_COMPLETE_SAVE_INVALID_MESSAGE);
		}

		// 주문상세 수량과 할인 금액을 반품 완료 기준으로 차감합니다.
		for (AdminOrderReturnManagePickupCompleteSelectedItem selectedItem : computation.selectedItemList()) {
			int returnQty = normalizeNonNegativeNumber(selectedItem.claimDetail().getQty());
			String nextOrdDtlStatCd = selectedItem.remainingAfterReturnQty() < 1
				? SHOP_ORDER_DTL_STAT_CANCEL
				: selectedItem.detailItem().getOrdDtlStatCd();
			int updatedCount = orderMapper.updateShopOrderDetailCancelQuantity(
				computation.claim().getOrdNo(),
				selectedItem.detailItem().getOrdDtlNo(),
				returnQty,
				nextOrdDtlStatCd,
				normalizeNonNegativeNumber(selectedItem.claimDetail().getGoodsCouponDiscountAmt()),
				normalizeNonNegativeNumber(selectedItem.claimDetail().getCartCouponDiscountAmt()),
				normalizeNonNegativeNumber(selectedItem.claimDetail().getPointDcAmt()),
				auditNo
			);
			if (updatedCount != 1) {
				throw new IllegalArgumentException(ADMIN_ORDER_RETURN_PICKUP_COMPLETE_SAVE_INVALID_MESSAGE);
			}
		}

		// 반품 완료로 더 이상 사용 중이 아닌 쿠폰과 포인트를 귀책 기준에 맞춰 복구합니다.
		restoreAdminOrderReturnCouponUse(computation, completedAt, auditNo);
		if (computation.companyFaultReasonYn()) {
			issueAdminOrderReturnCompanyFaultPoint(computation, completedAt, auditNo);
		} else {
			restoreAdminOrderReturnPointByAmount(auditNo, computation.claim().getOrdNo(), computation.restoredPointAmt());
		}

		// 주문 전체가 모두 소진되면 주문 마스터도 환불 완료 상태로 닫습니다.
		if (computation.fullReturnYn()) {
			int updatedOrderBaseCount = orderMapper.updateShopOrderBaseFullCancel(
				computation.claim().getOrdNo(),
				SHOP_ORDER_STAT_CANCEL,
				auditNo
			);
			if (updatedOrderBaseCount != 1) {
				throw new IllegalStateException("주문 상태 반영에 실패했습니다.");
			}
		}

		// 현금 환불이 있으면 Toss 환불을 호출하고 환불 결제 row를 성공 상태로 마무리합니다.
		if (refundPaymentSavePO != null && originalPayment != null) {
			AdminOrderReturnManagePickupCompletePgResult cancelPgResult = cancelAdminOrderReturnPaymentWithPg(
				originalPayment,
				computation.orderBase(),
				computation
			);
			orderMapper.updateShopPaymentCancelSuccess(
				refundPaymentSavePO.getPayNo(),
				SHOP_ORDER_PAY_STAT_CANCEL,
				cancelPgResult.canceledAmount(),
				cancelPgResult.tradeNo(),
				cancelPgResult.rspCode(),
				cancelPgResult.rspMsg(),
				cancelPgResult.rawResponse(),
				cancelPgResult.approvedDt(),
				auditNo
			);
		}
	}

	// 관리자 반품 회수완료 후 고객쿠폰 사용 상태를 원복합니다.
	private void restoreAdminOrderReturnCouponUse(
		AdminOrderReturnManagePickupCompleteComputation computation,
		LocalDateTime completedAt,
		Long auditNo
	) {
		// 원복 대상 고객쿠폰번호가 없으면 쿠폰 처리를 생략합니다.
		List<Long> restorableCustCpnNoList = resolveRestorableAdminOrderReturnCouponNoList(
			computation.orderBase(),
			computation.orderGroup(),
			computation.selectedItemList(),
			computation.fullReturnYn()
		);
		if (restorableCustCpnNoList.isEmpty()) {
			return;
		}

		// 회사 귀책이면 사용 가능 종료일을 반품완료 시점+3일과 비교해 연장합니다.
		String minimumUsableEndDt = computation.companyFaultReasonYn()
			? completedAt.plusDays(ADMIN_ORDER_RETURN_COMPANY_FAULT_COUPON_EXTEND_DAYS).format(SHOP_MYPAGE_ORDER_DATE_TIME_FORMATTER)
			: null;
		int updatedCount = orderMapper.restoreAdminOrderReturnCustomerCouponUseByCustCpnNoList(
			computation.custNo(),
			computation.claim().getOrdNo(),
			restorableCustCpnNoList,
			computation.companyFaultReasonYn(),
			minimumUsableEndDt,
			auditNo
		);
		if (updatedCount != restorableCustCpnNoList.size()) {
			throw new IllegalStateException("쿠폰 복구 처리에 실패했습니다.");
		}
	}

	// 관리자 반품 회수완료 후 원복 가능한 고객쿠폰번호 목록을 계산합니다.
	private List<Long> resolveRestorableAdminOrderReturnCouponNoList(
		ShopOrderCancelOrderBaseVO orderBase,
		ShopMypageOrderGroupVO orderGroup,
		List<AdminOrderReturnManagePickupCompleteSelectedItem> selectedItemList,
		boolean fullReturnYn
	) {
		// 선택 주문상세가 없으면 원복 대상도 없습니다.
		if (selectedItemList == null || selectedItemList.isEmpty()) {
			return List.of();
		}

		// 반품 완료 후에도 남아 있는 장바구니쿠폰 집합을 계산합니다.
		Map<Integer, AdminOrderReturnManagePickupCompleteSelectedItem> selectedItemMap = new LinkedHashMap<>();
		for (AdminOrderReturnManagePickupCompleteSelectedItem selectedItem : selectedItemList) {
			if (selectedItem == null || selectedItem.detailItem() == null || selectedItem.detailItem().getOrdDtlNo() == null) {
				continue;
			}
			selectedItemMap.put(selectedItem.detailItem().getOrdDtlNo(), selectedItem);
		}
		Set<Long> activeCartCouponNoSet = new HashSet<>();
		for (ShopMypageOrderDetailItemVO detailItem : orderGroup == null ? List.<ShopMypageOrderDetailItemVO>of() : orderGroup.getDetailList()) {
			AdminOrderReturnManagePickupCompleteSelectedItem selectedItem =
				detailItem.getOrdDtlNo() == null ? null : selectedItemMap.get(detailItem.getOrdDtlNo());
			int remainingAfterReturnQty = selectedItem == null
				? resolveShopOrderRemainingQty(detailItem)
				: Math.max(selectedItem.remainingAfterReturnQty(), 0);
			if (remainingAfterReturnQty < 1 || detailItem.getCartCpnNo() == null || detailItem.getCartCpnNo() < 1L) {
				continue;
			}
			activeCartCouponNoSet.add(detailItem.getCartCpnNo());
		}

		// 상품쿠폰/장바구니쿠폰/배송비쿠폰 중 완전히 해제되는 고객쿠폰번호만 유지합니다.
		Set<Long> restorableCustCpnNoSet = new LinkedHashSet<>();
		for (AdminOrderReturnManagePickupCompleteSelectedItem selectedItem : selectedItemList) {
			if (selectedItem == null || selectedItem.detailItem() == null || selectedItem.remainingAfterReturnQty() > 0) {
				continue;
			}
			Long goodsCpnNo = selectedItem.detailItem().getGoodsCpnNo();
			if (goodsCpnNo != null && goodsCpnNo > 0L) {
				restorableCustCpnNoSet.add(goodsCpnNo);
			}

			Long cartCpnNo = selectedItem.detailItem().getCartCpnNo();
			if (cartCpnNo != null && cartCpnNo > 0L && !activeCartCouponNoSet.contains(cartCpnNo)) {
				restorableCustCpnNoSet.add(cartCpnNo);
			}
		}
		Long deliveryCpnNo = orderBase == null ? null : orderBase.getDelvCpnNo();
		if (fullReturnYn && deliveryCpnNo != null && deliveryCpnNo > 0L) {
			restorableCustCpnNoSet.add(deliveryCpnNo);
		}
		return new ArrayList<>(restorableCustCpnNoSet);
	}

	// 관리자 반품 회수완료 후 기존 사용 포인트를 복구합니다.
	private void restoreAdminOrderReturnPointByAmount(Long auditNo, String ordNo, long restoreAmt) {
		// 복구할 포인트가 없으면 처리하지 않습니다.
		int remainingRestoreAmt = (int) Math.clamp(restoreAmt, 0L, (long) Integer.MAX_VALUE);
		if (remainingRestoreAmt < 1) {
			return;
		}

		// 아직 복구되지 않은 사용 포인트 이력 순서대로 포인트를 되돌립니다.
		for (ShopOrderPointDetailVO pointDetail : orderMapper.getShopOrderPointDetailBalanceList(ordNo)) {
			if (pointDetail == null || pointDetail.getPntNo() == null || remainingRestoreAmt < 1) {
				continue;
			}
			int restorableAmt = normalizeNonNegativeNumber(pointDetail.getPntAmt());
			if (restorableAmt < 1) {
				continue;
			}
			int appliedRestoreAmt = Math.min(restorableAmt, remainingRestoreAmt);
			orderMapper.restoreShopCustomerPointUseAmt(pointDetail.getPntNo(), appliedRestoreAmt, auditNo);
			ShopOrderPointDetailSavePO restoreDetail = new ShopOrderPointDetailSavePO();
			restoreDetail.setPntNo(pointDetail.getPntNo());
			restoreDetail.setPntAmt(appliedRestoreAmt);
			restoreDetail.setOrdNo(ordNo);
			restoreDetail.setBigo(ADMIN_ORDER_RETURN_POINT_RESTORE_MEMO);
			restoreDetail.setRegNo(auditNo);
			orderMapper.insertShopOrderPointDetail(restoreDetail);
			remainingRestoreAmt -= appliedRestoreAmt;
		}
		if (remainingRestoreAmt > 0) {
			throw new IllegalStateException("포인트 복구 처리에 실패했습니다.");
		}
	}

	// 관리자 반품 회수완료 회사 귀책 포인트를 신규 지급합니다.
	private void issueAdminOrderReturnCompanyFaultPoint(
		AdminOrderReturnManagePickupCompleteComputation computation,
		LocalDateTime completedAt,
		Long auditNo
	) {
		// 재지급할 포인트가 없으면 신규 지급을 생략합니다.
		int pointAmt = (int) Math.clamp(computation.reissuedPointAmt(), 0L, (long) Integer.MAX_VALUE);
		if (pointAmt < 1) {
			return;
		}

		// 회사 귀책 반품완료 포인트 마스터와 상세 이력을 함께 등록합니다.
		String pointMemo = computation.claim().getClmNo() + " " + firstNonBlank(computation.reasonName(), computation.reasonCd());
		ShopCustomerPointSavePO pointSaveCommand = new ShopCustomerPointSavePO(
			computation.custNo(),
			ADMIN_ORDER_RETURN_COMPANY_FAULT_POINT_GIVE_GB_CD,
			pointMemo,
			pointAmt,
			computation.claim().getOrdNo(),
			completedAt.plusDays(ADMIN_ORDER_RETURN_COMPANY_FAULT_POINT_EXPIRE_DAYS),
			auditNo,
			auditNo
		);
		GeneratedLongKey generatedKey = new GeneratedLongKey();
		shopAuthMapper.insertCustomerPointBase(pointSaveCommand, generatedKey);
		if (generatedKey.getValue() == null || generatedKey.getValue() < 1L) {
			throw new IllegalStateException("포인트 재지급 처리에 실패했습니다.");
		}

		ShopCustomerPointDetailSavePO pointDetailCommand = new ShopCustomerPointDetailSavePO(
			generatedKey.getValue(),
			pointAmt,
			computation.claim().getOrdNo(),
			pointMemo,
			auditNo
		);
		shopAuthMapper.insertCustomerPointDetail(pointDetailCommand);
	}

	// 관리자 반품 회수완료 대상 원결제 정보를 조회합니다.
	private ShopOrderPaymentVO resolveShopOrderPaymentForReturnComplete(String ordNo) {
		// 현재 주문의 승인 결제가 없으면 반품 환불을 진행할 수 없습니다.
		ShopOrderPaymentVO payment = orderMapper.getShopOrderPaymentForCancel(ordNo);
		if (payment == null || payment.getPayNo() == null || trimToNull(payment.getPayMethodCd()) == null) {
			throw new IllegalArgumentException("환불 가능한 결제 정보를 찾을 수 없습니다.");
		}
		return payment;
	}

	// 관리자 반품 회수완료 환불 PAYMENT row를 선등록합니다.
	private ShopOrderPaymentSavePO createAdminOrderReturnRefundPayment(
		AdminOrderReturnManagePickupCompleteComputation computation,
		ShopOrderPaymentVO originalPayment,
		Long auditNo
	) {
		// PG 실패 시에도 추적할 수 있도록 요청 스냅샷을 환불 PAYMENT row에 함께 저장합니다.
		Map<String, Object> refundSnapshot = new LinkedHashMap<>();
		refundSnapshot.put("ordNo", computation.claim().getOrdNo());
		refundSnapshot.put("clmNo", computation.claim().getClmNo());
		refundSnapshot.put("reasonCd", computation.reasonCd());
		refundSnapshot.put("reasonDetail", computation.reasonDetail());
		refundSnapshot.put("previewAmount", computation.previewAmount());
		refundSnapshot.put("refundedCashAmt", computation.refundedCashAmt());
		refundSnapshot.put("refundReceiveAccount", buildRefundReceiveAccountSnapshot(computation.orderBase()));

		return executeInNewShopOrderTransaction(() -> {
			ShopOrderPaymentSavePO refundPaymentSavePO = new ShopOrderPaymentSavePO();
			refundPaymentSavePO.setOrdNo(computation.claim().getOrdNo());
			refundPaymentSavePO.setCustNo(computation.custNo());
			refundPaymentSavePO.setPayStatCd(SHOP_ORDER_PAY_STAT_READY);
			refundPaymentSavePO.setPayGbCd(SHOP_ORDER_PAY_GB_REFUND);
			refundPaymentSavePO.setPayMethodCd(originalPayment == null ? null : originalPayment.getPayMethodCd());
			refundPaymentSavePO.setOrdGbCd(SHOP_ORDER_ORD_GB_ORDER);
			refundPaymentSavePO.setPgGbCd(originalPayment == null ? SHOP_ORDER_PG_GB_TOSS : originalPayment.getPgGbCd());
			refundPaymentSavePO.setOrgPayNo(originalPayment == null ? null : originalPayment.getPayNo());
			refundPaymentSavePO.setClmNo(computation.claim().getClmNo());
			refundPaymentSavePO.setPayAmt(resolveRefundPaymentAmt(computation.refundedCashAmt()));
			refundPaymentSavePO.setDeviceGbCd(firstNonBlank(trimToNull(computation.orderBase().getDeviceGbCd()), "PC"));
			refundPaymentSavePO.setReqRawJson(orderService.writeShopOrderJson(refundSnapshot));
			refundPaymentSavePO.setRegNo(auditNo);
			refundPaymentSavePO.setUdtNo(auditNo);
			orderMapper.insertShopPayment(refundPaymentSavePO);
			if (refundPaymentSavePO.getPayNo() == null || refundPaymentSavePO.getPayNo() < 1L) {
				throw new IllegalStateException("환불 결제 준비에 실패했습니다.");
			}
			return refundPaymentSavePO;
		});
	}

	// 환불 PAYMENT 요청 스냅샷에 저장할 환불 수취 계좌 정보를 구성합니다.
	private Map<String, Object> buildRefundReceiveAccountSnapshot(ShopOrderCancelOrderBaseVO orderBase) {
		// 주문 마스터 환불계좌 정보가 없으면 null을 반환합니다.
		String refundBankCd = trimToNull(orderBase == null ? null : orderBase.getRefundBankCd());
		String refundBankNo = trimToNull(orderBase == null ? null : orderBase.getRefundBankNo());
		String refundHolderNm = trimToNull(orderBase == null ? null : orderBase.getRefundHolderNm());
		if (refundBankCd == null && refundBankNo == null && refundHolderNm == null) {
			return null;
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("bank", refundBankCd);
		result.put("accountNumber", refundBankNo);
		result.put("holderName", refundHolderNm);
		return result;
	}

	// 별도 커밋이 필요한 반품완료 보조 트랜잭션을 실행합니다.
	private <T> T executeInNewShopOrderTransaction(Supplier<T> action) {
		// 환불 PAYMENT 선등록과 PG 실패 반영처럼 독립 커밋이 필요한 작업을 REQUIRES_NEW로 실행합니다.
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return transactionTemplate.execute(status -> action.get());
	}

	// 관리자 반품 회수완료용 PG 환불 API를 호출하고 성공 응답을 해석합니다.
	private AdminOrderReturnManagePickupCompletePgResult cancelAdminOrderReturnPaymentWithPg(
		ShopOrderPaymentVO originalPayment,
		ShopOrderCancelOrderBaseVO orderBase,
		AdminOrderReturnManagePickupCompleteComputation computation
	) {
		// Toss 결제키가 없으면 PG 환불을 진행할 수 없습니다.
		if (originalPayment == null || trimToNull(originalPayment.getTossPaymentKey()) == null) {
			throw new IllegalArgumentException("환불 가능한 결제 정보를 찾을 수 없습니다.");
		}

		// 무통장입금 취소는 금액 없이, 일반 결제취소는 현금 환불액 기준으로 PG를 호출합니다.
		Long cancelAmount =
			SHOP_ORDER_PAY_STAT_WAITING_DEPOSIT.equals(originalPayment.getPayStatCd())
				? null
				: computation.refundedCashAmt() > 0L
					? computation.refundedCashAmt()
					: null;
		TossPaymentRefundReceiveAccount refundReceiveAccount =
			SHOP_ORDER_PAYMENT_METHOD_VIRTUAL_ACCOUNT.equals(originalPayment.getPayMethodCd())
				&& !SHOP_ORDER_PAY_STAT_WAITING_DEPOSIT.equals(originalPayment.getPayStatCd())
				? buildTossPaymentRefundReceiveAccount(orderBase)
				: null;
		String rawResponse = refundReceiveAccount == null
			? tossPaymentsClient.cancelPayment(
				originalPayment.getTossPaymentKey().trim(),
				resolveAdminOrderReturnPickupCompletePgReason(computation),
				cancelAmount
			)
			: tossPaymentsClient.cancelPayment(
				originalPayment.getTossPaymentKey().trim(),
				resolveAdminOrderReturnPickupCompletePgReason(computation),
				cancelAmount,
				refundReceiveAccount
			);
		JsonNode responseNode = orderService.readShopOrderJsonNode(rawResponse);
		String paymentStatus = firstNonBlank(orderService.resolveJsonText(responseNode, "status"), "");
		if (!"CANCELED".equals(paymentStatus) && !"PARTIAL_CANCELED".equals(paymentStatus)) {
			throw new IllegalArgumentException("반품완료 처리에 실패했습니다.");
		}

		// Toss 취소 응답에서 거래키와 취소일시와 실제 취소금액을 추출합니다.
		JsonNode cancelNode = responseNode.path("cancels").isArray() && !responseNode.path("cancels").isEmpty()
			? responseNode.path("cancels").get(responseNode.path("cancels").size() - 1)
			: responseNode;
		long canceledAmount = resolveJsonLong(cancelNode, "cancelAmount");
		if (canceledAmount < 1L && cancelAmount != null) {
			canceledAmount = cancelAmount;
		}
		String approvedDt = orderService.normalizeShopOrderDateTime(
			firstNonBlank(
				orderService.resolveJsonText(cancelNode, "canceledAt"),
				orderService.resolveJsonText(responseNode, "approvedAt")
			)
		);
		String tradeNo = firstNonBlank(
			orderService.resolveJsonText(cancelNode, "transactionKey"),
			firstNonBlank(orderService.resolveJsonText(responseNode, "lastTransactionKey"), originalPayment.getTradeNo())
		);
		return new AdminOrderReturnManagePickupCompletePgResult(
			rawResponse,
			paymentStatus,
			"취소 완료",
			tradeNo,
			approvedDt,
			canceledAmount
		);
	}

	// 무통장입금 결제 환불 시 Toss 환불 수취 계좌 파라미터를 구성합니다.
	private TossPaymentRefundReceiveAccount buildTossPaymentRefundReceiveAccount(ShopOrderCancelOrderBaseVO orderBase) {
		// 주문 마스터에 저장된 환불계좌 3개 필드가 모두 있어야 Toss 취소 API를 호출합니다.
		String refundBankCd = trimToNull(orderBase == null ? null : orderBase.getRefundBankCd());
		String refundBankNo = trimToNull(orderBase == null ? null : orderBase.getRefundBankNo());
		String refundHolderNm = trimToNull(orderBase == null ? null : orderBase.getRefundHolderNm());
		if (refundBankCd == null || refundBankNo == null || refundHolderNm == null) {
			throw new IllegalArgumentException("무통장입금 환불 계좌 정보를 확인해주세요.");
		}
		if (!refundBankNo.matches("\\d+")) {
			throw new IllegalArgumentException("무통장입금 환불 계좌 정보를 확인해주세요.");
		}
		orderService.validateShopOrderRefundBankCode(refundBankCd);
		return new TossPaymentRefundReceiveAccount(refundBankCd, refundBankNo, refundHolderNm);
	}

	// 관리자 반품 회수완료 PG 오류 시 환불 PAYMENT row만 실패 상태로 남깁니다.
	private void handleAdminOrderReturnPickupCompletePaymentFailure(
		Long refundPayNo,
		TossPaymentClientException exception,
		Long auditNo
	) {
		// 환불 결제번호가 없으면 별도 실패 반영을 진행하지 않습니다.
		if (refundPayNo == null || refundPayNo < 1L) {
			return;
		}

		// Toss 오류 응답을 저장해 실패 원인을 확인할 수 있게 합니다.
		JsonNode errorNode = orderService.readShopOrderJsonNode(exception.getResponseBody());
		handleAdminOrderReturnPickupCompletePaymentFailure(
			refundPayNo,
			firstNonBlank(orderService.resolveJsonText(errorNode, "code"), "TOSS_CANCEL_ERROR"),
			firstNonBlank(orderService.resolveJsonText(errorNode, "message"), "반품완료 처리에 실패했습니다."),
			exception.getResponseBody(),
			auditNo
		);
	}

	// 관리자 반품 회수완료 환불 PAYMENT row를 실패 상태로 저장합니다.
	private void handleAdminOrderReturnPickupCompletePaymentFailure(
		Long refundPayNo,
		String rspCode,
		String rspMsg,
		String rspRawJson,
		Long auditNo
	) {
		// 환불 결제번호가 없으면 별도 실패 반영을 진행하지 않습니다.
		if (refundPayNo == null || refundPayNo < 1L) {
			return;
		}

		// 실패 코드와 메시지와 응답 원문을 함께 저장합니다.
		executeInNewShopOrderTransaction(() -> {
			orderMapper.updateShopPaymentCancelFailure(
				refundPayNo,
				SHOP_ORDER_PAY_STAT_FAIL,
				firstNonBlank(rspCode, "RETURN_COMPLETE_ERROR"),
				firstNonBlank(rspMsg, "반품완료 처리에 실패했습니다."),
				rspRawJson,
				auditNo
			);
			return null;
		});
	}

	// 관리자 반품 회수완료 PG 오류 응답에서 사용자 표시 메시지를 추출합니다.
	private String resolveAdminOrderReturnPickupCompletePgErrorMessage(TossPaymentClientException exception) {
		// Toss 오류 메시지가 있으면 우선 사용하고 없으면 기본 문구를 반환합니다.
		JsonNode errorNode = orderService.readShopOrderJsonNode(exception == null ? null : exception.getResponseBody());
		return firstNonBlank(orderService.resolveJsonText(errorNode, "message"), "반품완료 처리에 실패했습니다.");
	}

	// 관리자 반품 회수완료 PG 취소 사유 문자열을 Toss 전송용 텍스트로 구성합니다.
	private String resolveAdminOrderReturnPickupCompletePgReason(AdminOrderReturnManagePickupCompleteComputation computation) {
		// 공통 반품 사유명과 상세 입력값을 결합해 PG 취소 사유를 구성합니다.
		String reasonText = firstNonBlank(computation == null ? null : computation.reasonName(), computation == null ? null : computation.reasonCd());
		String reasonDetail = trimToNull(computation == null ? null : computation.reasonDetail());
		if (reasonText == null) {
			return "반품 완료";
		}
		return reasonDetail == null ? reasonText : reasonText + " - " + reasonDetail;
	}

	// 쇼핑몰 마이페이지 반품내역 페이지 데이터를 조회합니다.
	public ShopMypageReturnHistoryPageVO getShopMypageReturnHistoryPage(
		Long custNo,
		Integer requestedPageNo,
		String requestedStartDate,
		String requestedEndDate
	) {
		// 고객번호가 없으면 로그인 예외를 반환합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}

		// 요청 페이지 번호와 조회 기간을 각각 보정합니다.
		int resolvedRequestedPageNo = normalizePage(requestedPageNo, 1);
		ShopMypageOrderDateRange dateRange = orderService.resolveShopMypageOrderDateRange(
			requestedStartDate,
			requestedEndDate
		);

		// 반품 클레임 전체 건수와 전체 페이지 수를 계산합니다.
		int returnCount = orderMapper.countShopMypageReturnHistory(
			custNo,
			dateRange.getStartDate(),
			dateRange.getEndDate()
		);
		int totalPageCount = calculateTotalPageCount(returnCount, SHOP_MYPAGE_RETURN_PAGE_SIZE);
		int resolvedPageNo = resolvePageNoWithinRange(resolvedRequestedPageNo, totalPageCount);
		int offset = calculateOffset(resolvedPageNo, SHOP_MYPAGE_RETURN_PAGE_SIZE);

		// 현재 페이지의 반품 클레임 목록을 조회합니다.
		List<ShopMypageReturnHistoryVO> returnList = orderMapper.getShopMypageReturnHistoryList(
			custNo,
			dateRange.getStartDate(),
			dateRange.getEndDate(),
			offset,
			SHOP_MYPAGE_RETURN_PAGE_SIZE
		);

		// 클레임번호 목록 기준으로 반품 상품 상세를 조회해 각 클레임에 매핑합니다.
		if (!returnList.isEmpty()) {
			List<String> clmNoList = returnList.stream()
				.map(ShopMypageReturnHistoryVO::getClmNo)
				.filter(clmNo -> clmNo != null && !clmNo.isBlank())
				.distinct()
				.collect(java.util.stream.Collectors.toList());
			List<ShopMypageReturnHistoryDetailVO> detailList = orderMapper.getShopMypageReturnHistoryDetailList(clmNoList);
			Map<String, List<ShopMypageReturnHistoryDetailVO>> detailByClmNo = detailList.stream()
				.filter(detail -> detail != null && detail.getClmNo() != null)
				.collect(java.util.stream.Collectors.groupingBy(ShopMypageReturnHistoryDetailVO::getClmNo));
			for (ShopMypageReturnHistoryVO returnItem : returnList) {
				returnItem.setDetailList(detailByClmNo.getOrDefault(returnItem.getClmNo(), java.util.Collections.emptyList()));
			}
		}

		// 반품내역 페이지 응답 객체를 구성합니다.
		ShopMypageReturnHistoryPageVO result = new ShopMypageReturnHistoryPageVO();
		result.setReturnList(returnList);
		result.setReturnCount(returnCount);
		result.setPageNo(resolvedPageNo);
		result.setPageSize(SHOP_MYPAGE_RETURN_PAGE_SIZE);
		result.setTotalPageCount(totalPageCount);
		result.setStartDate(dateRange.getStartDate());
		result.setEndDate(dateRange.getEndDate());
		return result;
	}

	// 쇼핑몰 마이페이지 반품상세 화면 데이터를 조회합니다.
	public ShopMypageReturnDetailPageVO getShopMypageReturnHistoryDetail(Long custNo, String clmNo) {
		// 고객번호가 없으면 로그인 예외를 반환합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}
		if (clmNo == null || clmNo.isBlank()) {
			throw new IllegalArgumentException("클레임번호가 필요합니다.");
		}

		// 클레임 단건을 조회합니다.
		ShopMypageReturnHistoryVO returnItem = orderMapper.getShopMypageReturnHistoryItemByClmNo(custNo, clmNo);
		if (returnItem == null) {
			return null;
		}

		// 반품 상품 상세 목록을 조회해 클레임에 매핑합니다.
		List<ShopMypageReturnHistoryDetailVO> detailList = orderMapper.getShopMypageReturnHistoryDetailList(
			java.util.Collections.singletonList(clmNo)
		);
		returnItem.setDetailList(detailList != null ? detailList : java.util.Collections.emptyList());

		// 화면 구성에 필요한 주문/고객/회수지/환불 예정 금액을 함께 조합합니다.
		ShopOrderCancelOrderBaseVO orderBase = orderService.resolveShopOrderCancelOrderBase(custNo, returnItem.getOrdNo());
		ShopMypageOrderGroupVO orderGroup = orderService.getShopMypageOrderGroupWithDetail(custNo, returnItem.getOrdNo());
		ShopOrderCustomerInfoVO customerInfo = orderMapper.getShopOrderCustomerInfo(custNo);
		ShopMypageReturnPickupAddressVO pickupAddress = orderMapper.getShopMypageReturnPickupAddress(clmNo);
		if (pickupAddress == null) {
			pickupAddress = buildShopMypageReturnFallbackPickupAddress(orderBase);
		}

		// 상세 화면 응답 객체를 구성합니다.
		ShopMypageReturnDetailPageVO result = new ShopMypageReturnDetailPageVO();
		result.setReturnItem(returnItem);
		result.setPreviewAmount(buildShopMypageReturnPreviewAmount(returnItem, orderGroup, orderBase));
		result.setPickupAddress(pickupAddress);
		result.setCustomerPhoneNumber(trimToNull(customerInfo == null ? null : customerInfo.getPhoneNumber()));
		return result;
	}

	// 반품상세 화면에 노출할 환불 예정 금액 요약을 계산합니다.
	private ShopMypageReturnPreviewAmountVO buildShopMypageReturnPreviewAmount(
		ShopMypageReturnHistoryVO returnItem,
		ShopMypageOrderGroupVO orderGroup,
		ShopOrderCancelOrderBaseVO orderBase
	) {
		// 상세 행 목록을 기준으로 상품금액과 환급 할인금액을 합산합니다.
		long totalSupplyAmt = 0L;
		long totalOrderAmt = 0L;
		long totalGoodsCouponDiscountAmt = 0L;
		long totalCartCouponDiscountAmt = 0L;
		long totalPointRefundAmt = 0L;
		for (ShopMypageReturnHistoryDetailVO detailItem : returnItem == null ? List.<ShopMypageReturnHistoryDetailVO>of() : returnItem.getDetailList()) {
			int qty = normalizeNonNegativeNumber(detailItem.getQty());
			long unitSupplyAmt = normalizeNonNegativeNumber(detailItem.getSupplyAmt());
			long unitOrderAmt =
				(long) normalizeNonNegativeNumber(detailItem.getSaleAmt())
					+ normalizeNonNegativeNumber(detailItem.getAddAmt());
			totalSupplyAmt += unitSupplyAmt * qty;
			totalOrderAmt += unitOrderAmt * qty;
			totalGoodsCouponDiscountAmt += normalizeNonNegativeNumber(detailItem.getGoodsCouponDiscountAmt());
			totalCartCouponDiscountAmt += normalizeNonNegativeNumber(detailItem.getCartCouponDiscountAmt());
			totalPointRefundAmt += normalizeNonNegativeNumber(detailItem.getPointDcAmt());
		}

		// 전체 반품 여부에 따라 배송비 환급/쿠폰 환급 금액을 계산합니다.
		boolean fullReturnYn = resolveShopMypageReturnFullReturnYn(returnItem, orderGroup);
		long paidDeliveryFeeRefundAmt = fullReturnYn
			? Math.max(
				(long) normalizeNonNegativeNumber(orderBase == null ? null : orderBase.getOrdDelvAmt())
					- normalizeNonNegativeNumber(orderBase == null ? null : orderBase.getDelvCpnDcAmt()),
				0L
			)
			: 0L;
		long deliveryCouponRefundAmt = fullReturnYn
			? normalizeNonNegativeNumber(orderBase == null ? null : orderBase.getDelvCpnDcAmt())
			: 0L;
		long benefitAmt = totalGoodsCouponDiscountAmt + totalCartCouponDiscountAmt + totalPointRefundAmt;
		long shippingAdjustmentAmt = paidDeliveryFeeRefundAmt + resolveSignedLong(returnItem == null ? null : returnItem.getPayDelvAmt());
		long expectedRefundAmt = totalOrderAmt - benefitAmt + shippingAdjustmentAmt;

		// 프론트 금액 표 렌더링에 필요한 요약 값을 구성합니다.
		ShopMypageReturnPreviewAmountVO result = new ShopMypageReturnPreviewAmountVO();
		result.setTotalSupplyAmt(totalSupplyAmt);
		result.setTotalGoodsDiscountAmt(Math.max(totalOrderAmt - totalSupplyAmt, 0L));
		result.setTotalGoodsCouponDiscountAmt(totalGoodsCouponDiscountAmt);
		result.setTotalCartCouponDiscountAmt(totalCartCouponDiscountAmt);
		result.setDeliveryCouponRefundAmt(deliveryCouponRefundAmt);
		result.setTotalPointRefundAmt(totalPointRefundAmt);
		result.setPaidGoodsAmt(totalOrderAmt);
		result.setBenefitAmt(benefitAmt);
		result.setShippingAdjustmentAmt(shippingAdjustmentAmt);
		result.setExpectedRefundAmt(expectedRefundAmt);
		return result;
	}

	// 관리자 반품 회수완료 검수 팝업용 클레임 상세 이력을 공통 반품 이력 형식으로 변환합니다.
	private ShopMypageReturnHistoryVO buildAdminOrderReturnManagePickupCompleteReturnItem(
		AdminOrderReturnManagePickupCompleteClaimVO claim,
		List<AdminOrderReturnManagePickupCompleteDetailVO> detailList
	) {
		// 기존 반품 상세 금액 계산 로직을 재사용할 수 있도록 공통 형식으로 매핑합니다.
		ShopMypageReturnHistoryVO result = new ShopMypageReturnHistoryVO();
		result.setClmNo(trimToNull(claim == null ? null : claim.getClmNo()));
		result.setOrdNo(trimToNull(claim == null ? null : claim.getOrdNo()));

		List<ShopMypageReturnHistoryDetailVO> mappedDetailList = new ArrayList<>();
		for (AdminOrderReturnManagePickupCompleteDetailVO detail : detailList == null ? List.<AdminOrderReturnManagePickupCompleteDetailVO>of() : detailList) {
			ShopMypageReturnHistoryDetailVO mappedDetail = new ShopMypageReturnHistoryDetailVO();
			mappedDetail.setClmNo(trimToNull(claim == null ? null : claim.getClmNo()));
			mappedDetail.setOrdDtlNo(detail.getOrdDtlNo());
			mappedDetail.setGoodsId(trimToNull(detail.getGoodsId()));
			mappedDetail.setGoodsNm(trimToNull(detail.getGoodsNm()));
			mappedDetail.setSizeId(trimToNull(detail.getSizeId()));
			mappedDetail.setQty(detail.getQty());
			mappedDetail.setSaleAmt(detail.getSaleAmt());
			mappedDetail.setAddAmt(detail.getAddAmt());
			mappedDetail.setSupplyAmt(detail.getSupplyAmt());
			mappedDetail.setGoodsCouponDiscountAmt(detail.getGoodsCouponDiscountAmt());
			mappedDetail.setCartCouponDiscountAmt(detail.getCartCouponDiscountAmt());
			mappedDetail.setPointDcAmt(detail.getPointDcAmt());
			mappedDetail.setChgReasonCd(trimToNull(detail.getChgReasonCd()));
			mappedDetail.setChgReasonDtl(trimToNull(detail.getChgReasonDtl()));
			mappedDetail.setChgDtlStatCd(trimToNull(detail.getChgDtlStatCd()));
			mappedDetailList.add(mappedDetail);
		}
		result.setDetailList(mappedDetailList);
		return result;
	}

	// 관리자 반품 회수완료 검수 팝업의 고정 금액 요약을 계산합니다.
	private AdminOrderReturnManagePickupCompletePreviewAmountVO buildAdminOrderReturnManagePickupCompletePreviewAmount(
		ShopMypageReturnHistoryVO returnItem,
		ShopMypageOrderGroupVO orderGroup,
		ShopOrderCancelOrderBaseVO orderBase
	) {
		// 상세 행 목록 기준으로 고정 상품/쿠폰/포인트 합계를 계산합니다.
		long totalSupplyAmt = 0L;
		long totalOrderAmt = 0L;
		long totalGoodsCouponDiscountAmt = 0L;
		long totalCartCouponDiscountAmt = 0L;
		long totalPointRefundAmt = 0L;
		for (ShopMypageReturnHistoryDetailVO detailItem : returnItem == null ? List.<ShopMypageReturnHistoryDetailVO>of() : returnItem.getDetailList()) {
			int qty = normalizeNonNegativeNumber(detailItem.getQty());
			long unitSupplyAmt = normalizeNonNegativeNumber(detailItem.getSupplyAmt());
			long unitOrderAmt =
				(long) normalizeNonNegativeNumber(detailItem.getSaleAmt())
					+ normalizeNonNegativeNumber(detailItem.getAddAmt());
			totalSupplyAmt += unitSupplyAmt * qty;
			totalOrderAmt += unitOrderAmt * qty;
			totalGoodsCouponDiscountAmt += normalizeNonNegativeNumber(detailItem.getGoodsCouponDiscountAmt());
			totalCartCouponDiscountAmt += normalizeNonNegativeNumber(detailItem.getCartCouponDiscountAmt());
			totalPointRefundAmt += normalizeNonNegativeNumber(detailItem.getPointDcAmt());
		}

		// 전체 반품 여부에 따라 배송비쿠폰 환급 금액만 고정값으로 계산합니다.
		boolean fullReturnYn = resolveShopMypageReturnFullReturnYn(returnItem, orderGroup);
		long deliveryCouponRefundAmt = fullReturnYn
			? normalizeNonNegativeNumber(orderBase == null ? null : orderBase.getDelvCpnDcAmt())
			: 0L;
		long benefitAmt = totalGoodsCouponDiscountAmt + totalCartCouponDiscountAmt + totalPointRefundAmt;

		// 프론트 재계산에 필요한 고정 금액 필드만 응답 객체에 담습니다.
		AdminOrderReturnManagePickupCompletePreviewAmountVO result = new AdminOrderReturnManagePickupCompletePreviewAmountVO();
		result.setTotalSupplyAmt(totalSupplyAmt);
		result.setTotalGoodsDiscountAmt(Math.max(totalOrderAmt - totalSupplyAmt, 0L));
		result.setTotalGoodsCouponDiscountAmt(totalGoodsCouponDiscountAmt);
		result.setTotalCartCouponDiscountAmt(totalCartCouponDiscountAmt);
		result.setDeliveryCouponRefundAmt(deliveryCouponRefundAmt);
		result.setTotalPointRefundAmt(totalPointRefundAmt);
		result.setPaidGoodsAmt(totalOrderAmt);
		result.setBenefitAmt(benefitAmt);
		return result;
	}

	// 관리자 반품 회수완료 검수 팝업의 공통 기본 반품 사유를 계산합니다.
	private void applyAdminOrderReturnManagePickupCompleteDefaultReason(
		AdminOrderReturnManagePickupCompletePageVO result,
		List<AdminOrderReturnManagePickupCompleteDetailVO> detailList
	) {
		// 첫 상세 행을 기준으로 같은 사유인지 비교하고, 다르면 공통 기본값을 비웁니다.
		String defaultReasonCd = null;
		String defaultReasonDetail = null;
		boolean initialized = false;
		boolean mixedReasonYn = false;
		for (AdminOrderReturnManagePickupCompleteDetailVO detail : detailList == null ? List.<AdminOrderReturnManagePickupCompleteDetailVO>of() : detailList) {
			String currentReasonCd = trimToNull(detail.getChgReasonCd());
			String currentReasonDetail = trimToNull(detail.getChgReasonDtl());
			if (!initialized) {
				defaultReasonCd = currentReasonCd;
				defaultReasonDetail = currentReasonDetail;
				initialized = true;
				continue;
			}
			if (!Objects.equals(defaultReasonCd, currentReasonCd)
				|| !Objects.equals(defaultReasonDetail, currentReasonDetail)) {
				mixedReasonYn = true;
				break;
			}
		}

		result.setMixedReasonYn(mixedReasonYn);
		result.setDefaultReasonCd(mixedReasonYn ? null : defaultReasonCd);
		result.setDefaultReasonDetail(mixedReasonYn ? null : defaultReasonDetail);
	}

	// 현재 주문 기준으로 이번 반품이 전체 반품인지 계산합니다.
	private boolean resolveShopMypageReturnFullReturnYn(
		ShopMypageReturnHistoryVO returnItem,
		ShopMypageOrderGroupVO orderGroup
	) {
		// 반품 상세 행의 주문상세별 반품 수량을 먼저 합산합니다.
		Map<Integer, Integer> returnQtyByOrdDtlNo = buildShopMypageReturnQtyMap(returnItem);
		Map<Integer, Boolean> completedReturnOrdDtlNoMap = buildShopMypageReturnCompletedOrdDtlNoMap(returnItem);
		int activeItemCount = 0;
		int selectedItemCount = 0;
		int fullyReturnedItemCount = 0;

		// 현재 주문에 남아 있는 주문상세와 현재 반품 수량을 비교합니다.
		for (ShopMypageOrderDetailItemVO detailItem : orderGroup == null ? List.<ShopMypageOrderDetailItemVO>of() : orderGroup.getDetailList()) {
			Integer ordDtlNo = detailItem.getOrdDtlNo();
			if (ordDtlNo == null) {
				continue;
			}

			int currentRemainingQty = resolveShopOrderRemainingQty(detailItem);
			int currentClaimQty = returnQtyByOrdDtlNo.getOrDefault(ordDtlNo, 0);
			boolean currentClaimCompletedYn = Boolean.TRUE.equals(completedReturnOrdDtlNoMap.get(ordDtlNo));
			int remainingQtyBeforeCurrentClaim = currentRemainingQty + (currentClaimCompletedYn ? currentClaimQty : 0);
			if (remainingQtyBeforeCurrentClaim < 1) {
				continue;
			}
			activeItemCount += 1;

			if (currentClaimQty < 1) {
				continue;
			}
			selectedItemCount += 1;
			if (currentClaimQty >= remainingQtyBeforeCurrentClaim) {
				fullyReturnedItemCount += 1;
			}
		}
		return activeItemCount > 0 && selectedItemCount > 0 && fullyReturnedItemCount == activeItemCount;
	}

	// 반품 상세 행 목록을 주문상세번호별 반품 수량 맵으로 변환합니다.
	private Map<Integer, Integer> buildShopMypageReturnQtyMap(ShopMypageReturnHistoryVO returnItem) {
		// 중복 주문상세번호가 있으면 수량을 누적합니다.
		Map<Integer, Integer> result = new LinkedHashMap<>();
		for (ShopMypageReturnHistoryDetailVO detailItem : returnItem == null ? List.<ShopMypageReturnHistoryDetailVO>of() : returnItem.getDetailList()) {
			Integer ordDtlNo = detailItem.getOrdDtlNo();
			if (ordDtlNo == null) {
				continue;
			}
			int qty = normalizeNonNegativeNumber(detailItem.getQty());
			result.merge(ordDtlNo, qty, Integer::sum);
		}
		return result;
	}

	// 반품 상세 행 목록에서 완료 처리된 주문상세번호 여부를 맵으로 변환합니다.
	private Map<Integer, Boolean> buildShopMypageReturnCompletedOrdDtlNoMap(ShopMypageReturnHistoryVO returnItem) {
		// 같은 주문상세번호에 완료 상태가 하나라도 있으면 완료로 간주합니다.
		Map<Integer, Boolean> result = new LinkedHashMap<>();
		for (ShopMypageReturnHistoryDetailVO detailItem : returnItem == null ? List.<ShopMypageReturnHistoryDetailVO>of() : returnItem.getDetailList()) {
			Integer ordDtlNo = detailItem.getOrdDtlNo();
			if (ordDtlNo == null) {
				continue;
			}
			boolean completedYn = RETURN_COMPLETE_DETAIL_STATUS_CODE.equals(trimToNull(detailItem.getChgDtlStatCd()));
			result.merge(ordDtlNo, completedYn, (previousValue, currentValue) -> previousValue || currentValue);
		}
		return result;
	}

	// 저장된 반품 회수지가 없을 때 주문 배송지 기준 기본 회수지를 구성합니다.
	private ShopMypageReturnPickupAddressVO buildShopMypageReturnFallbackPickupAddress(ShopOrderCancelOrderBaseVO orderBase) {
		// 주문서 배송지 정보가 있으면 동일한 주소를 기본 회수지로 사용합니다.
		ShopMypageReturnPickupAddressVO result = new ShopMypageReturnPickupAddressVO();
		result.setRsvNm(trimToNull(orderBase == null ? null : orderBase.getRcvNm()));
		result.setPostNo(trimToNull(orderBase == null ? null : orderBase.getRcvPostNo()));
		result.setBaseAddress(trimToNull(orderBase == null ? null : orderBase.getRcvAddrBase()));
		result.setDetailAddress(trimToNull(orderBase == null ? null : orderBase.getRcvAddrDtl()));
		return result;
	}

	// 쇼핑몰 마이페이지 반품 신청을 저장합니다.
	@Transactional
	public ShopOrderReturnResultVO returnShopMypageOrder(ShopOrderReturnPO param, Long custNo) {
		// 로그인 고객번호가 없으면 요청을 진행하지 않습니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}
		return applyShopOrderReturn(param, custNo);
	}

	// 쇼핑몰 마이페이지 반품 신청 상품 1건을 철회합니다.
	@Transactional
	public ShopOrderReturnWithdrawResultVO withdrawShopMypageOrderReturn(ShopOrderReturnWithdrawPO param, Long custNo) {
		// 로그인 고객번호와 요청 본문 기본값을 먼저 검증합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}
		if (param == null) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_RETURN_WITHDRAW_UNAVAILABLE_MESSAGE);
		}

		String ordNo = trimToNull(param.getOrdNo());
		Integer ordDtlNo = param.getOrdDtlNo();
		if (ordNo == null || ordDtlNo == null || ordDtlNo < 1) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_RETURN_WITHDRAW_UNAVAILABLE_MESSAGE);
		}

		// 현재 로그인 고객이 철회할 수 있는 최신 반품 신청 이력 1건을 조회합니다.
		ShopOrderReturnWithdrawResultVO withdrawTarget = orderMapper.getShopOrderReturnWithdrawTarget(custNo, ordNo, ordDtlNo);
		if (withdrawTarget == null
			|| !SHOP_ORDER_CHANGE_DTL_STAT_RETURN_APPLY.equals(trimToNull(withdrawTarget.getChgDtlStatCd()))) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_RETURN_WITHDRAW_UNAVAILABLE_MESSAGE);
		}

		// 반품 신청 상태인 상세 1건만 반품 철회 상태로 변경합니다.
		int detailUpdatedCount = orderMapper.withdrawShopOrderChangeDetail(
			withdrawTarget.getClmNo(),
			withdrawTarget.getOrdNo(),
			withdrawTarget.getOrdDtlNo(),
			SHOP_ORDER_CHANGE_DTL_STAT_RETURN_APPLY,
			SHOP_ORDER_CHANGE_DTL_STAT_RETURN_WITHDRAW,
			custNo
		);
		if (detailUpdatedCount != 1) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_RETURN_WITHDRAW_UNAVAILABLE_MESSAGE);
		}

		// 같은 클레임에 남아 있는 반품 상세가 없으면 클레임 마스터도 철회 상태로 닫습니다.
		int remainingReturnDetailCount = orderMapper.countShopOrderRemainingReturnDetailByClaim(
			withdrawTarget.getClmNo(),
			withdrawTarget.getOrdNo()
		);
		boolean claimClosedYn = remainingReturnDetailCount < 1;
		int claimUpdatedCount = 0;
		if (claimClosedYn) {
			claimUpdatedCount = orderMapper.withdrawShopOrderChangeBase(
				withdrawTarget.getClmNo(),
				withdrawTarget.getOrdNo(),
				SHOP_ORDER_CHANGE_STAT_WITHDRAW,
				custNo
			);
			if (claimUpdatedCount != 1) {
				throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_RETURN_WITHDRAW_UNAVAILABLE_MESSAGE);
			}
		}

		// 프론트 새로고침 분기용 응답 객체를 구성합니다.
		ShopOrderReturnWithdrawResultVO result = new ShopOrderReturnWithdrawResultVO();
		result.setClmNo(withdrawTarget.getClmNo());
		result.setOrdNo(withdrawTarget.getOrdNo());
		result.setOrdDtlNo(withdrawTarget.getOrdDtlNo());
		result.setChgDtlStatCd(SHOP_ORDER_CHANGE_DTL_STAT_RETURN_WITHDRAW);
		result.setClaimClosedYn(claimClosedYn);
		result.setUpdatedCount(detailUpdatedCount + claimUpdatedCount);
		return result;
	}

	// 관리자 주문반품 신청 상품 여러 건을 철회합니다.
	@Transactional
	public AdminOrderReturnWithdrawVO withdrawAdminOrderReturn(AdminOrderReturnWithdrawPO param) {
		// 관리자 로그인 번호와 요청 본문을 먼저 검증합니다.
		Long udtNo = resolveCurrentAdminUserNo();
		if (udtNo == null || udtNo < 1L) {
			throw new IllegalArgumentException(ADMIN_LOGIN_INVALID_MESSAGE);
		}
		if (param == null) {
			throw new IllegalArgumentException(ADMIN_ORDER_RETURN_WITHDRAW_EMPTY_MESSAGE);
		}

		String ordNo = trimToNull(param.getOrdNo());
		if (ordNo == null) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_NO_INVALID_MESSAGE);
		}

		// 중복을 제거한 철회 대상 목록과 현재 주문의 클레임 목록을 각각 준비합니다.
		List<AdminOrderReturnWithdrawItemPO> claimItemList = normalizeAdminOrderReturnWithdrawItemList(param.getClaimItemList());
		if (claimItemList.isEmpty()) {
			throw new IllegalArgumentException(ADMIN_ORDER_RETURN_WITHDRAW_EMPTY_MESSAGE);
		}
		Map<String, AdminOrderClaimRowVO> claimRowMap = buildAdminOrderClaimRowMap(orderMapper.getAdminOrderClaimList(ordNo));

		// 선택된 모든 행이 반품 신청 반품건인지 먼저 검증합니다.
		List<AdminOrderClaimRowVO> validatedClaimRowList = new ArrayList<>();
		for (AdminOrderReturnWithdrawItemPO claimItem : claimItemList) {
			String claimItemKey = buildAdminOrderReturnWithdrawItemKey(claimItem.getClmNo(), claimItem.getOrdDtlNo());
			AdminOrderClaimRowVO claimRow = claimRowMap.get(claimItemKey);
			if (!isAdminOrderReturnWithdrawableClaimRow(claimRow)) {
				throw new IllegalArgumentException(ADMIN_ORDER_RETURN_WITHDRAW_INVALID_MESSAGE);
			}
			validatedClaimRowList.add(claimRow);
		}

		// 검증을 모두 통과한 상세 행만 반품 철회 상태로 일괄 반영합니다.
		int updatedCount = 0;
		Map<String, Boolean> selectedClaimNoMap = new LinkedHashMap<>();
		for (AdminOrderClaimRowVO claimRow : validatedClaimRowList) {
			int detailUpdatedCount = orderMapper.withdrawShopOrderChangeDetail(
				claimRow.getClmNo(),
				ordNo,
				claimRow.getOrdDtlNo(),
				SHOP_ORDER_CHANGE_DTL_STAT_RETURN_APPLY,
				SHOP_ORDER_CHANGE_DTL_STAT_RETURN_WITHDRAW,
				udtNo
			);
			if (detailUpdatedCount != 1) {
				throw new IllegalArgumentException(ADMIN_ORDER_RETURN_WITHDRAW_INVALID_MESSAGE);
			}
			updatedCount += detailUpdatedCount;
			selectedClaimNoMap.put(claimRow.getClmNo(), Boolean.TRUE);
		}

		// 철회로 남은 반품 상세가 없어진 클레임 마스터만 종료 상태로 닫습니다.
		int closedClaimCount = 0;
		for (String clmNo : selectedClaimNoMap.keySet()) {
			int remainingReturnDetailCount = orderMapper.countShopOrderRemainingReturnDetailByClaim(clmNo, ordNo);
			if (remainingReturnDetailCount > 0) {
				continue;
			}

			int claimUpdatedCount = orderMapper.withdrawShopOrderChangeBase(
				clmNo,
				ordNo,
				SHOP_ORDER_CHANGE_STAT_WITHDRAW,
				udtNo
			);
			if (claimUpdatedCount != 1) {
				throw new IllegalArgumentException(ADMIN_ORDER_RETURN_WITHDRAW_INVALID_MESSAGE);
			}
			closedClaimCount += claimUpdatedCount;
		}

		// 관리자 화면 재조회에 사용할 결과 객체를 구성합니다.
		AdminOrderReturnWithdrawVO result = new AdminOrderReturnWithdrawVO();
		result.setOrdNo(ordNo);
		result.setUpdatedCount(updatedCount);
		result.setClosedClaimCount(closedClaimCount);
		return result;
	}

	// 관리자 주문반품 신청을 저장합니다.
	@Transactional
	public ShopOrderReturnResultVO returnAdminOrder(AdminOrderReturnPO param) {
		// 관리자 요청 본문과 주문번호를 먼저 검증합니다.
		if (param == null) {
			throw new IllegalArgumentException("반품 정보를 확인해주세요.");
		}
		String ordNo = trimToNull(param.getOrdNo());
		if (ordNo == null) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_NO_INVALID_MESSAGE);
		}

		// 주문번호 기준 고객번호를 조회해 동일한 저장 로직을 재사용합니다.
		Long custNo = orderMapper.getOrderCustNo(ordNo);
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_NO_INVALID_MESSAGE);
		}
		return applyShopOrderReturn(createShopOrderReturnPOFromAdmin(param), custNo);
	}

	// 관리자 반품 철회 요청 목록을 정규화하고 중복을 제거합니다.
	private List<AdminOrderReturnWithdrawItemPO> normalizeAdminOrderReturnWithdrawItemList(
		List<AdminOrderReturnWithdrawItemPO> claimItemList
	) {
		// 요청 목록이 없으면 빈 목록을 반환합니다.
		if (claimItemList == null || claimItemList.isEmpty()) {
			return List.of();
		}

		// 클레임번호와 주문상세번호가 유효한 행만 중복 없이 유지합니다.
		Map<String, AdminOrderReturnWithdrawItemPO> normalizedItemMap = new LinkedHashMap<>();
		for (AdminOrderReturnWithdrawItemPO claimItem : claimItemList) {
			String clmNo = trimToNull(claimItem == null ? null : claimItem.getClmNo());
			Integer ordDtlNo = claimItem == null ? null : claimItem.getOrdDtlNo();
			if (clmNo == null || ordDtlNo == null || ordDtlNo < 1) {
				throw new IllegalArgumentException(ADMIN_ORDER_RETURN_WITHDRAW_INVALID_MESSAGE);
			}

			AdminOrderReturnWithdrawItemPO normalizedItem = new AdminOrderReturnWithdrawItemPO();
			normalizedItem.setClmNo(clmNo);
			normalizedItem.setOrdDtlNo(ordDtlNo);
			normalizedItemMap.putIfAbsent(buildAdminOrderReturnWithdrawItemKey(clmNo, ordDtlNo), normalizedItem);
		}
		return List.copyOf(normalizedItemMap.values());
	}

	// 관리자 주문 클레임 목록을 철회 검증용 맵으로 변환합니다.
	private Map<String, AdminOrderClaimRowVO> buildAdminOrderClaimRowMap(List<AdminOrderClaimRowVO> claimList) {
		// 클레임번호와 주문상세번호 조합을 키로 사용해 빠르게 찾을 수 있게 구성합니다.
		Map<String, AdminOrderClaimRowVO> claimRowMap = new LinkedHashMap<>();
		for (AdminOrderClaimRowVO claimRow : claimList == null ? List.<AdminOrderClaimRowVO>of() : claimList) {
			String clmNo = trimToNull(claimRow.getClmNo());
			Integer ordDtlNo = claimRow.getOrdDtlNo();
			if (clmNo == null || ordDtlNo == null || ordDtlNo < 1) {
				continue;
			}
			claimRowMap.putIfAbsent(buildAdminOrderReturnWithdrawItemKey(clmNo, ordDtlNo), claimRow);
		}
		return claimRowMap;
	}

	// 관리자 주문 클레임 행이 반품 철회 가능한 상태인지 반환합니다.
	private boolean isAdminOrderReturnWithdrawableClaimRow(AdminOrderClaimRowVO claimRow) {
		// 반품 클레임이면서 현재 반품 신청 상태인 행만 철회할 수 있습니다.
		if (claimRow == null) {
			return false;
		}
		return SHOP_ORDER_CHANGE_DTL_GB_RETURN.equals(trimToNull(claimRow.getChgDtlGbCd()))
			&& SHOP_ORDER_CHANGE_DTL_STAT_RETURN_APPLY.equals(trimToNull(claimRow.getChgDtlStatCd()));
	}

	// 관리자 반품 철회 요청 항목의 복합키 문자열을 생성합니다.
	private String buildAdminOrderReturnWithdrawItemKey(String clmNo, Integer ordDtlNo) {
		return clmNo + ":" + ordDtlNo;
	}

	// 관리자 반품 요청을 쇼핑몰 반품 공통 요청 형식으로 변환합니다.
	private ShopOrderReturnPO createShopOrderReturnPOFromAdmin(AdminOrderReturnPO param) {
		// 공통 사유를 상품별 요청 행에 복제해 공통 저장 로직에서 그대로 사용합니다.
		ShopOrderReturnPO result = new ShopOrderReturnPO();
		result.setOrdNo(param.getOrdNo());
		result.setPreviewAmount(param.getPreviewAmount());
		result.setPickupAddress(param.getPickupAddress());

		List<ShopOrderReturnItemPO> returnItemList = new ArrayList<>();
		for (AdminOrderReturnItemPO adminReturnItem : param.getReturnItemList() == null ? List.<AdminOrderReturnItemPO>of() : param.getReturnItemList()) {
			ShopOrderReturnItemPO returnItem = new ShopOrderReturnItemPO();
			returnItem.setOrdDtlNo(adminReturnItem.getOrdDtlNo());
			returnItem.setReturnQty(adminReturnItem.getReturnQty());
			returnItem.setReasonCd(param.getReasonCd());
			returnItem.setReasonDetail(param.getReasonDetail());
			returnItemList.add(returnItem);
		}
		result.setReturnItemList(returnItemList);
		return result;
	}

	// 반품 신청 저장 공통 로직을 수행합니다.
	private ShopOrderReturnResultVO applyShopOrderReturn(ShopOrderReturnPO param, Long custNo) {
		// 반품 요청 본문과 주문번호를 먼저 검증합니다.
		if (param == null) {
			throw new IllegalArgumentException("반품 정보를 확인해주세요.");
		}
		String ordNo = trimToNull(param.getOrdNo());
		if (ordNo == null) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_NO_INVALID_MESSAGE);
		}

		// 현재 주문과 반품 계산에 필요한 기준 정보를 조회합니다.
		ShopMypageOrderGroupVO orderGroup = orderService.getShopMypageOrderGroupWithDetail(custNo, ordNo);
		ShopOrderCancelOrderBaseVO orderBase = orderService.resolveShopOrderCancelOrderBase(custNo, ordNo);
		ShopCartSiteInfoVO siteInfo = orderService.resolveShopCartSiteInfo();
		ShopMypageOrderAmountSummaryVO amountSummary = orderService.buildShopOrderRemainingAmountSummary(orderGroup, orderBase, siteInfo);
		ShopMypageOrderReturnFeeContextVO returnFeeContext = buildShopOrderReturnFeeContext(ordNo, orderBase, amountSummary);
		List<ShopMypageOrderCancelReasonVO> reasonList = orderService.normalizeShopMypageOrderCancelReasonList(
			orderMapper.getShopMypageOrderReturnReasonList()
		);

		// 요청 회수지와 선택 상품과 반품 예정 금액을 서버 기준으로 다시 검증합니다.
		ShopOrderReturnPickupAddressPO normalizedPickupAddress = normalizeShopOrderReturnPickupAddress(param.getPickupAddress());
		Map<Integer, ShopOrderReturnItemPO> returnItemMap = resolveShopOrderReturnItemMap(param.getReturnItemList());
		ShopOrderReturnComputation computation = buildShopOrderReturnComputation(
			orderGroup,
			amountSummary,
			siteInfo,
			returnFeeContext,
			reasonList,
			returnItemMap
		);
		validateShopOrderReturnPreviewAmount(param.getPreviewAmount(), computation.previewAmount());

		// 반품 신청 클레임번호를 생성하고 주문변경 마스터/상세/주소를 함께 저장합니다.
		String clmNo = generateShopOrderClaimNo(custNo);
		String returnDt = LocalDateTime.now().format(SHOP_MYPAGE_ORDER_DATE_TIME_FORMATTER);
		orderMapper.insertShopOrderChangeBase(buildShopOrderChangeBaseSavePO(clmNo, orderBase, computation, returnDt, custNo));
		for (ShopOrderReturnSelectedItem selectedItem : computation.selectedItemList()) {
			orderMapper.insertShopOrderChangeDetail(buildShopOrderChangeDetailSavePO(clmNo, selectedItem, custNo));
		}
		orderMapper.insertShopOrderChangeExchangeAddress(buildPickupAddressSavePO(clmNo, normalizedPickupAddress, custNo));
		orderMapper.insertShopOrderChangeExchangeAddress(buildDestinationAddressSavePO(clmNo, resolveShopOrderReturnDestinationAddress(), custNo));

		// 반품 신청 결과를 응답 객체에 담아 반환합니다.
		ShopOrderReturnResultVO result = new ShopOrderReturnResultVO();
		result.setClmNo(clmNo);
		result.setOrdNo(orderBase.getOrdNo());
		return result;
	}

	// 반품 배송비 계산 컨텍스트를 서버 기준으로 다시 구성합니다.
	private ShopMypageOrderReturnFeeContextVO buildShopOrderReturnFeeContext(
		String ordNo,
		ShopOrderCancelOrderBaseVO orderBase,
		ShopMypageOrderAmountSummaryVO amountSummary
	) {
		// 이력 기반 배송비 계산 컨텍스트가 없으면 기본 객체를 생성합니다.
		ShopMypageOrderReturnFeeContextVO result = orderMapper.getShopMypageOrderReturnFeeContext(ordNo);
		if (result == null) {
			result = new ShopMypageOrderReturnFeeContextVO();
		}

		// 원주문 실결제 배송비와 무료배송 여부를 계산합니다.
		int originalPaidDeliveryAmt = Math.max(
			normalizeNonNegativeNumber(orderBase == null ? null : orderBase.getOrdDelvAmt())
				- normalizeNonNegativeNumber(orderBase == null ? null : orderBase.getDelvCpnDcAmt()),
			0
		);
		result.setOriginalPaidDeliveryAmt(originalPaidDeliveryAmt);
		result.setOriginalFreeDeliveryYn(originalPaidDeliveryAmt < 1);

		// 이력 여부와 현재 잔여 결제금액을 프론트 계산용 기준과 동일하게 보정합니다.
		result.setHasPriorCompanyFaultReturnOrExchange(Boolean.TRUE.equals(result.getHasPriorCompanyFaultReturnOrExchange()));
		result.setHasPriorCustomerFaultReturnDeduction(Boolean.TRUE.equals(result.getHasPriorCustomerFaultReturnDeduction()));
		result.setCurrentRemainingFinalPayAmt((int) Math.min(
			resolveNonNegativeLong(amountSummary == null ? null : amountSummary.getFinalPayAmt()),
			Integer.MAX_VALUE
		));
		return result;
	}

	// 반품 요청 상품 목록을 주문상세번호 기준 맵으로 정규화합니다.
	private Map<Integer, ShopOrderReturnItemPO> resolveShopOrderReturnItemMap(List<ShopOrderReturnItemPO> returnItemList) {
		// 선택 상품이 없으면 반품 신청을 진행하지 않습니다.
		if (returnItemList == null || returnItemList.isEmpty()) {
			throw new IllegalArgumentException("반품할 상품을 선택해주세요.");
		}

		// 주문상세번호 중복과 반품 수량 1 이상 여부를 함께 검증합니다.
		Map<Integer, ShopOrderReturnItemPO> result = new LinkedHashMap<>();
		for (ShopOrderReturnItemPO returnItem : returnItemList) {
			if (returnItem == null || returnItem.getOrdDtlNo() == null || returnItem.getOrdDtlNo() < 1 || returnItem.getReturnQty() == null || returnItem.getReturnQty() < 1) {
				throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_DETAIL_ITEM_INVALID_MESSAGE);
			}
			if (result.putIfAbsent(returnItem.getOrdDtlNo(), returnItem) != null) {
				throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_DETAIL_ITEM_INVALID_MESSAGE);
			}
		}
		return result;
	}

	// 반품 신청 화면 기준 예상 환불 금액과 저장 상세 목록을 계산합니다.
	private ShopOrderReturnComputation buildShopOrderReturnComputation(
		ShopMypageOrderGroupVO orderGroup,
		ShopMypageOrderAmountSummaryVO amountSummary,
		ShopCartSiteInfoVO siteInfo,
		ShopMypageOrderReturnFeeContextVO returnFeeContext,
		List<ShopMypageOrderCancelReasonVO> reasonList,
		Map<Integer, ShopOrderReturnItemPO> returnItemMap
	) {
		// 누적 반품 금액과 전체 반품 여부를 계산할 집계값을 준비합니다.
		long totalOrderAmt = 0L;
		long totalGoodsCouponDiscountAmt = 0L;
		long totalCartCouponDiscountAmt = 0L;
		long totalPointRefundAmt = 0L;
		int activeItemCount = 0;
		int selectedItemCount = 0;
		int fullyReturnedItemCount = 0;
		boolean hasCompanyFaultReason = false;
		List<ShopOrderReturnSelectedItem> selectedItemList = new ArrayList<>();
		Map<Integer, ShopOrderReturnItemPO> unmatchedItemMap = new LinkedHashMap<>(returnItemMap);

		// 현재 주문상세를 순회하며 선택 반품 수량과 사유와 금액을 모두 다시 계산합니다.
		for (ShopMypageOrderDetailItemVO detailItem : orderGroup == null ? List.<ShopMypageOrderDetailItemVO>of() : orderGroup.getDetailList()) {
			if (detailItem.getOrdDtlNo() == null) {
				continue;
			}

			int currentRemainingQty = resolveShopOrderRemainingQty(detailItem);
			if (currentRemainingQty < 1) {
				continue;
			}
			activeItemCount += 1;

			ShopOrderReturnItemPO requestReturnItem = returnItemMap.get(detailItem.getOrdDtlNo());
			if (requestReturnItem == null) {
				continue;
			}
			unmatchedItemMap.remove(detailItem.getOrdDtlNo());

			// 반품 가능 상태와 수량과 사유를 서버 기준으로 다시 검증합니다.
			if (!isShopOrderReturnApplyable(detailItem)) {
				throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_DETAIL_ITEM_INVALID_MESSAGE);
			}
			int returnQty = requestReturnItem.getReturnQty();
			if (returnQty > currentRemainingQty) {
				throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_DETAIL_ITEM_INVALID_MESSAGE);
			}
			validateShopOrderReturnReason(requestReturnItem, reasonList);

			// 현재 ORDER_DETAIL 잔여 할인금액을 반품 수량 기준으로 비례 배분합니다.
			totalOrderAmt += (long) resolveShopOrderUnitOrderAmt(detailItem) * returnQty;
			totalGoodsCouponDiscountAmt += resolveShopOrderIncrementAllocatedAmt(detailItem.getGoodsCouponDiscountAmt(), currentRemainingQty, 0, returnQty);
			totalCartCouponDiscountAmt += resolveShopOrderIncrementAllocatedAmt(detailItem.getCartCouponDiscountAmt(), currentRemainingQty, 0, returnQty);
			totalPointRefundAmt += resolveShopOrderIncrementAllocatedAmt(detailItem.getPointUseAmt(), currentRemainingQty, 0, returnQty);
			selectedItemCount += 1;
			if (currentRemainingQty - returnQty < 1) {
				fullyReturnedItemCount += 1;
			}

			String reasonCd = trimToNull(requestReturnItem.getReasonCd());
			String reasonDetail = trimToNull(requestReturnItem.getReasonDetail());
			hasCompanyFaultReason = hasCompanyFaultReason || (reasonCd != null && reasonCd.startsWith(COMPANY_FAULT_REASON_PREFIX));
			selectedItemList.add(new ShopOrderReturnSelectedItem(detailItem, returnQty, reasonCd, reasonDetail));
		}

		// 요청 주문상세번호가 실제 주문에 없거나 반품 불가 행이면 진행하지 않습니다.
		if (selectedItemList.isEmpty()) {
			throw new IllegalArgumentException("반품할 상품을 선택해주세요.");
		}
		if (!unmatchedItemMap.isEmpty()) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_DETAIL_ITEM_INVALID_MESSAGE);
		}

		// 전체 반품 여부와 배송비 차감/환급 반영 후 최종 반품 예정 금액을 계산합니다.
		boolean isFullReturn = activeItemCount > 0 && selectedItemCount > 0 && fullyReturnedItemCount == activeItemCount;
		long beforeShippingExpectedRefundAmt = totalOrderAmt - (totalGoodsCouponDiscountAmt + totalCartCouponDiscountAmt + totalPointRefundAmt);
		long shippingDeductionAmt = resolveShopOrderReturnShippingDeductionAmt(
			siteInfo,
			returnFeeContext,
			beforeShippingExpectedRefundAmt,
			hasCompanyFaultReason
		);
		long paidDeliveryFeeRefundAmt = isFullReturn
			? resolveNonNegativeLong(returnFeeContext == null ? null : (long) normalizeNonNegativeNumber(returnFeeContext.getOriginalPaidDeliveryAmt()))
			: 0L;
		long deliveryCouponRefundAmt = isFullReturn
			? resolveNonNegativeLong(amountSummary == null ? null : amountSummary.getDeliveryCouponDiscountAmt())
			: 0L;
		long benefitAmt = totalGoodsCouponDiscountAmt + totalCartCouponDiscountAmt + totalPointRefundAmt;
		long shippingAdjustmentAmt = paidDeliveryFeeRefundAmt - shippingDeductionAmt;
		long expectedRefundAmt = totalOrderAmt - benefitAmt + shippingAdjustmentAmt;
		if (expectedRefundAmt < 0L) {
			throw new IllegalArgumentException("배송비 차감 후 반품 예정 금액이 0원 미만이라 신청할 수 없습니다.");
		}

		// 화면 위변조 검증에 사용할 반품 예정 금액 요약을 구성합니다.
		ShopOrderReturnPreviewAmountPO previewAmount = new ShopOrderReturnPreviewAmountPO();
		previewAmount.setExpectedRefundAmt(expectedRefundAmt);
		previewAmount.setPaidGoodsAmt(totalOrderAmt);
		previewAmount.setBenefitAmt(benefitAmt);
		previewAmount.setShippingAdjustmentAmt(shippingAdjustmentAmt);
		previewAmount.setTotalPointRefundAmt(totalPointRefundAmt);
		previewAmount.setDeliveryCouponRefundAmt(deliveryCouponRefundAmt);
		return new ShopOrderReturnComputation(selectedItemList, previewAmount, shippingDeductionAmt);
	}

	// 고객 귀책/회사 귀책과 과거 이력을 기준으로 배송비 차감 금액을 계산합니다.
	private long resolveShopOrderReturnShippingDeductionAmt(
		ShopCartSiteInfoVO siteInfo,
		ShopMypageOrderReturnFeeContextVO returnFeeContext,
		long beforeShippingExpectedRefundAmt,
		boolean hasCompanyFaultReason
	) {
		// 회사 귀책이거나 기본 배송비가 없으면 차감하지 않습니다.
		long siteDeliveryFee = resolveNonNegativeLong(siteInfo == null ? null : (long) normalizeNonNegativeNumber(siteInfo.getDeliveryFee()));
		long siteDeliveryFeeLimit = resolveNonNegativeLong(siteInfo == null ? null : (long) normalizeNonNegativeNumber(siteInfo.getDeliveryFeeLimit()));
		if (siteDeliveryFee < 1L || hasCompanyFaultReason) {
			return 0L;
		}

		// 유료배송 주문의 고객 귀책 반품은 회수배송비 1회만 차감합니다.
		if (!Boolean.TRUE.equals(returnFeeContext == null ? null : returnFeeContext.getOriginalFreeDeliveryYn())) {
			return siteDeliveryFee;
		}

		// 회사 귀책 이력 또는 기존 고객 귀책 배송비 차감 이력이 있으면 회수배송비 1회만 차감합니다.
		if (Boolean.TRUE.equals(returnFeeContext.getHasPriorCompanyFaultReturnOrExchange())
			|| Boolean.TRUE.equals(returnFeeContext.getHasPriorCustomerFaultReturnDeduction())) {
			return siteDeliveryFee;
		}

		// 무료배송 주문은 반품 후 남을 결제금액이 기준 미만이면 왕복 배송비를 차감합니다.
		long remainingFinalPayAmtAfterReturn =
			resolveNonNegativeLong(Long.valueOf(returnFeeContext.getCurrentRemainingFinalPayAmt()))
				- beforeShippingExpectedRefundAmt;
		return remainingFinalPayAmtAfterReturn < siteDeliveryFeeLimit ? siteDeliveryFee * 2L : siteDeliveryFee;
	}

	// 서버 재계산 반품 예정 금액과 클라이언트 전송 금액이 정확히 같은지 확인합니다.
	private void validateShopOrderReturnPreviewAmount(
		ShopOrderReturnPreviewAmountPO clientPreviewAmount,
		ShopOrderReturnPreviewAmountPO serverPreviewAmount
	) {
		// 화면과 서버 계산 결과가 하나라도 다르면 진행하지 않습니다.
		if (resolvePreviewAmountValue(clientPreviewAmount == null ? null : clientPreviewAmount.getExpectedRefundAmt()) != resolvePreviewAmountValue(serverPreviewAmount.getExpectedRefundAmt())
			|| resolvePreviewAmountValue(clientPreviewAmount == null ? null : clientPreviewAmount.getPaidGoodsAmt()) != resolvePreviewAmountValue(serverPreviewAmount.getPaidGoodsAmt())
			|| resolvePreviewAmountValue(clientPreviewAmount == null ? null : clientPreviewAmount.getBenefitAmt()) != resolvePreviewAmountValue(serverPreviewAmount.getBenefitAmt())
			|| resolvePreviewAmountValue(clientPreviewAmount == null ? null : clientPreviewAmount.getShippingAdjustmentAmt()) != resolvePreviewAmountValue(serverPreviewAmount.getShippingAdjustmentAmt())
			|| resolvePreviewAmountValue(clientPreviewAmount == null ? null : clientPreviewAmount.getTotalPointRefundAmt()) != resolvePreviewAmountValue(serverPreviewAmount.getTotalPointRefundAmt())
			|| resolvePreviewAmountValue(clientPreviewAmount == null ? null : clientPreviewAmount.getDeliveryCouponRefundAmt()) != resolvePreviewAmountValue(serverPreviewAmount.getDeliveryCouponRefundAmt())) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_RETURN_AMOUNT_MISMATCH_MESSAGE);
		}
	}

	// 반품 사유 코드와 직접입력값을 검증합니다.
	private void validateShopOrderReturnReason(ShopOrderReturnItemPO returnItem, List<ShopMypageOrderCancelReasonVO> reasonList) {
		// 선택 상품별 반품 사유 코드가 비어 있거나 현재 사용 가능한 사유가 아니면 예외를 반환합니다.
		String reasonCd = trimToNull(returnItem == null ? null : returnItem.getReasonCd());
		if (reasonCd == null) {
			throw new IllegalArgumentException("반품 사유를 선택해주세요.");
		}

		ShopMypageOrderCancelReasonVO matchedReason = findShopOrderReturnReason(reasonList, reasonCd);
		if (matchedReason == null) {
			throw new IllegalArgumentException("반품 사유를 선택해주세요.");
		}

		// 기타 사유는 상품별 직접입력값을 필수로 확인합니다.
		if (isShopOrderReasonDetailRequired(matchedReason)
			&& trimToNull(returnItem.getReasonDetail()) == null) {
			throw new IllegalArgumentException("기타 사유를 입력해주세요.");
		}
	}

	// 반품 사유 목록에서 요청 사유 코드 1건을 찾습니다.
	private ShopMypageOrderCancelReasonVO findShopOrderReturnReason(
		List<ShopMypageOrderCancelReasonVO> reasonList,
		String reasonCd
	) {
		// 현재 사용 가능한 사유 목록에서 코드가 일치하는 항목을 반환합니다.
		for (ShopMypageOrderCancelReasonVO reasonItem : reasonList == null ? List.<ShopMypageOrderCancelReasonVO>of() : reasonList) {
			if (reasonCd.equals(trimToNull(reasonItem.getCd()))) {
				return reasonItem;
			}
		}
		return null;
	}

	// 반품 사유가 직접입력값을 요구하는지 반환합니다.
	private boolean isShopOrderReasonDetailRequired(ShopMypageOrderCancelReasonVO reasonItem) {
		// 사유명에 기타가 포함되면 직접입력값을 필수로 처리합니다.
		String reasonName = trimToNull(reasonItem == null ? null : reasonItem.getCdNm());
		return reasonName != null && reasonName.contains("기타");
	}

	// 주문상세 행이 반품신청 가능한 상태인지 반환합니다.
	private boolean isShopOrderReturnApplyable(ShopMypageOrderDetailItemVO detailItem) {
		// 배송완료 상태, 잔여 수량, 진행중 클레임 차단 여부를 함께 확인합니다.
		if (detailItem == null) {
			return false;
		}
		return SHOP_ORDER_DTL_STAT_DELIVERY_COMPLETE.equals(detailItem.getOrdDtlStatCd())
			&& resolveShopOrderRemainingQty(detailItem) > 0
			&& Boolean.TRUE.equals(detailItem.getReturnApplyableYn());
	}

	// 반품 신청 회수지 입력값을 정규화하고 길이를 검증합니다.
	// 관리자 반품 회수 관리 조회 상태값을 허용 범위로 보정합니다.
	private String normalizeAdminOrderReturnManageStatus(String chgDtlStatCd) {
		// 값이 비어 있으면 기본 조회 상태를 반품 신청으로 고정합니다.
		String normalizedChgDtlStatCd = trimToNull(chgDtlStatCd);
		if (normalizedChgDtlStatCd == null) {
			return RETURN_APPLY_DETAIL_STATUS_CODE;
		}
		if (!ADMIN_ORDER_RETURN_MANAGE_STATUS_SET.contains(normalizedChgDtlStatCd)) {
			throw new IllegalArgumentException(ADMIN_ORDER_RETURN_MANAGE_STATUS_INVALID_MESSAGE);
		}
		return normalizedChgDtlStatCd;
	}

	// 관리자 반품 회수 관리 택배사 공통코드 목록을 조회해 코드 집합으로 반환합니다.
	private Set<String> getAdminOrderReturnManageDeliveryCompanyCodeSet() {
		// 공통코드 조회 결과에서 실제 사용 가능한 택배사 코드만 추출합니다.
		Set<String> result = new LinkedHashSet<>();
		for (CommonCodeVO item : commonMapper.getCommonCodeList(ADMIN_ORDER_RETURN_MANAGE_DELIVERY_COMPANY_GRP_CD)) {
			String code = trimToNull(item == null ? null : item.getCd());
			if (code != null) {
				result.add(code);
			}
		}
		return Set.copyOf(result);
	}

	// 관리자 반품 회수 신청 저장 대상 목록을 정규화합니다.
	private List<AdminOrderReturnManagePickupItemPO> normalizeAdminOrderReturnManagePickupItemList(
		List<AdminOrderReturnManagePickupItemPO> itemList,
		Set<String> deliveryCompanyCodeSet
	) {
		// null 또는 빈 목록이면 빈 결과를 반환합니다.
		if (itemList == null || itemList.isEmpty()) {
			return List.of();
		}

		Map<String, AdminOrderReturnManagePickupItemPO> normalizedMap = new LinkedHashMap<>();
		for (AdminOrderReturnManagePickupItemPO item : itemList) {
			String clmNo = trimToNull(item == null ? null : item.getClmNo());
			String delvCompCd = trimToNull(item == null ? null : item.getDelvCompCd());
			String invoiceNo = normalizeAdminOrderReturnManageInvoiceNo(item == null ? null : item.getInvoiceNo());

			// 클레임번호, 택배사, 송장번호의 기본 형식을 검증합니다.
			if (clmNo == null) {
				throw new IllegalArgumentException(ADMIN_ORDER_RETURN_PICKUP_REQUEST_EMPTY_MESSAGE);
			}
			if (delvCompCd == null || !deliveryCompanyCodeSet.contains(delvCompCd)) {
				throw new IllegalArgumentException(ADMIN_ORDER_RETURN_PICKUP_COMPANY_REQUIRED_MESSAGE);
			}
			if (invoiceNo == null) {
				throw new IllegalArgumentException(ADMIN_ORDER_RETURN_PICKUP_INVOICE_REQUIRED_MESSAGE);
			}
			if (!isValidAdminOrderReturnManageInvoiceNo(invoiceNo)) {
				throw new IllegalArgumentException(ADMIN_ORDER_RETURN_PICKUP_INVOICE_INVALID_MESSAGE);
			}

			// 검증한 값만 새 객체로 복사해 이후 로직에서 사용합니다.
			AdminOrderReturnManagePickupItemPO normalizedItem = new AdminOrderReturnManagePickupItemPO();
			normalizedItem.setClmNo(clmNo);
			normalizedItem.setDelvCompCd(delvCompCd);
			normalizedItem.setInvoiceNo(invoiceNo);
			normalizedMap.putIfAbsent(clmNo, normalizedItem);
		}
		return List.copyOf(normalizedMap.values());
	}

	// 관리자 반품 회수 관리 클레임 목록을 정규화합니다.
	private List<AdminOrderReturnManageClaimItemPO> normalizeAdminOrderReturnManageClaimItemList(
		List<AdminOrderReturnManageClaimItemPO> itemList
	) {
		// null 또는 빈 목록이면 빈 결과를 반환합니다.
		if (itemList == null || itemList.isEmpty()) {
			return List.of();
		}

		Map<String, AdminOrderReturnManageClaimItemPO> normalizedMap = new LinkedHashMap<>();
		for (AdminOrderReturnManageClaimItemPO item : itemList) {
			String clmNo = trimToNull(item == null ? null : item.getClmNo());
			if (clmNo == null) {
				throw new IllegalArgumentException(ADMIN_ORDER_RETURN_PICKUP_START_EMPTY_MESSAGE);
			}

			// 검증한 클레임번호만 새 객체로 복사해 이후 로직에서 사용합니다.
			AdminOrderReturnManageClaimItemPO normalizedItem = new AdminOrderReturnManageClaimItemPO();
			normalizedItem.setClmNo(clmNo);
			normalizedMap.putIfAbsent(clmNo, normalizedItem);
		}
		return List.copyOf(normalizedMap.values());
	}

	// 관리자 반품 회수 관리 대상 클레임의 현재 상태와 회수지 행 존재 여부를 검증합니다.
	private int validateAdminOrderReturnManageClaimState(
		List<String> clmNoList,
		String expectedChgDtlStatCd,
		String invalidMessage
	) {
		// 선택한 클레임 요약을 조회해 현재 상태와 회수지 존재 여부를 함께 확인합니다.
		List<AdminOrderReturnManageClaimSummaryVO> summaryList = orderMapper.getAdminOrderReturnManageClaimSummaryList(clmNoList);
		Map<String, AdminOrderReturnManageClaimSummaryVO> summaryMap = new LinkedHashMap<>();
		for (AdminOrderReturnManageClaimSummaryVO summary : summaryList) {
			String clmNo = trimToNull(summary == null ? null : summary.getClmNo());
			if (clmNo != null) {
				summaryMap.putIfAbsent(clmNo, summary);
			}
		}

		int totalDetailCount = 0;
		for (String clmNo : clmNoList) {
			AdminOrderReturnManageClaimSummaryVO summary = summaryMap.get(clmNo);
			if (summary == null) {
				throw new IllegalArgumentException(invalidMessage);
			}

			// 클레임 상세 상태가 모두 기대 상태인지와 회수지 행이 1건 존재하는지 검증합니다.
			int detailCount = normalizeNonNegativeNumber(summary.getDetailCount());
			int pickupAddressCount = normalizeNonNegativeNumber(summary.getPickupAddressCount());
			String minChgDtlStatCd = trimToNull(summary.getMinChgDtlStatCd());
			String maxChgDtlStatCd = trimToNull(summary.getMaxChgDtlStatCd());
			if (detailCount < 1 || pickupAddressCount != 1) {
				throw new IllegalArgumentException(invalidMessage);
			}
			if (!expectedChgDtlStatCd.equals(minChgDtlStatCd) || !expectedChgDtlStatCd.equals(maxChgDtlStatCd)) {
				throw new IllegalArgumentException(invalidMessage);
			}

			// 후속 update 결과 검증을 위해 전체 상세 건수를 누적합니다.
			totalDetailCount += detailCount;
		}
		return totalDetailCount;
	}

	// 관리자 반품 회수 관리 송장번호를 정규화합니다.
	private String normalizeAdminOrderReturnManageInvoiceNo(String invoiceNo) {
		// 공백만 제거하고 숫자 형식 검증은 별도 메서드에서 수행합니다.
		return trimToNull(invoiceNo);
	}

	// 관리자 반품 회수 관리 송장번호 형식을 검증합니다.
	private boolean isValidAdminOrderReturnManageInvoiceNo(String invoiceNo) {
		// 숫자만 허용하고 DB 컬럼 길이 20자를 초과하지 않아야 합니다.
		return invoiceNo != null
			&& invoiceNo.length() <= ADMIN_ORDER_RETURN_MANAGE_INVOICE_NO_MAX_LENGTH
			&& invoiceNo.chars().allMatch(Character::isDigit);
	}

	// 諛섑뭹 ?좎껌 ?뚯닔吏 ?낅젰媛믪쓣 ?뺢퇋?뷀븯怨?湲몄씠瑜?寃利앺빀?덈떎.
	private ShopOrderReturnPickupAddressPO normalizeShopOrderReturnPickupAddress(ShopOrderReturnPickupAddressPO pickupAddress) {
		// 회수지 정보가 없거나 필수값이 비어 있으면 진행하지 않습니다.
		String receiverName = trimToNull(pickupAddress == null ? null : pickupAddress.getRsvNm());
		String postNo = trimToNull(pickupAddress == null ? null : pickupAddress.getPostNo());
		String baseAddress = trimToNull(pickupAddress == null ? null : pickupAddress.getBaseAddress());
		String detailAddress = trimToNull(pickupAddress == null ? null : pickupAddress.getDetailAddress());
		validateShopOrderChangeAddress("회수지 정보를 확인해주세요.", receiverName, postNo, baseAddress, detailAddress);

		// 저장에 사용할 회수지 주소 객체를 새로 구성합니다.
		ShopOrderReturnPickupAddressPO result = new ShopOrderReturnPickupAddressPO();
		result.setRsvNm(receiverName);
		result.setPostNo(postNo);
		result.setBaseAddress(baseAddress);
		result.setDetailAddress(detailAddress);
		return result;
	}

	// 반품 도착지 창고 주소를 조회하고 저장 가능 상태인지 검증합니다.
	private ShopOrderReturnDestinationAddressVO resolveShopOrderReturnDestinationAddress() {
		// 사이트 정보에서 창고 주소를 조회하고 반품 도착지 필수값을 검증합니다.
		ShopSiteInfoVO siteInfo = siteInfoMapper.getShopSiteInfo(SHOP_SITE_ID);
		String addrName = trimToNull(siteInfo == null ? null : siteInfo.getSiteNm());
		String addrPostNo = trimToNull(siteInfo == null ? null : siteInfo.getWhPostNo());
		String addrBase = trimToNull(siteInfo == null ? null : siteInfo.getWhAddrBase());
		String addrDtl = trimToNull(siteInfo == null ? null : siteInfo.getWhAddrDtl());
		validateShopOrderChangeAddress("반품 도착지 정보를 확인해주세요.", addrName, addrPostNo, addrBase, addrDtl);

		// 저장에 사용할 도착지 주소 객체를 새로 구성합니다.
		ShopOrderReturnDestinationAddressVO normalizedResult = new ShopOrderReturnDestinationAddressVO();
		normalizedResult.setAddrName(addrName);
		normalizedResult.setAddrPostNo(addrPostNo);
		normalizedResult.setAddrBase(addrBase);
		normalizedResult.setAddrDtl(addrDtl);
		return normalizedResult;
	}

	// 주문변경 주소 필수값과 길이를 ORDER_CHANGE_EXCHANGE_ADDRESS 기준으로 검증합니다.
	private void validateShopOrderChangeAddress(
		String invalidMessage,
		String addrName,
		String addrPostNo,
		String addrBase,
		String addrDtl
	) {
		// 필수값이 하나라도 비어 있으면 진행하지 않습니다.
		if (addrName == null || addrPostNo == null || addrBase == null || addrDtl == null) {
			throw new IllegalArgumentException(invalidMessage);
		}

		// 저장 테이블 길이를 초과하는 주소값은 미리 차단합니다.
		if (addrName.length() > ORDER_CHANGE_ADDRESS_NAME_MAX_LENGTH
			|| addrPostNo.length() > ORDER_CHANGE_ADDRESS_POST_NO_MAX_LENGTH
			|| addrBase.length() > ORDER_CHANGE_ADDRESS_BASE_MAX_LENGTH
			|| addrDtl.length() > ORDER_CHANGE_ADDRESS_DETAIL_MAX_LENGTH) {
			throw new IllegalArgumentException(invalidMessage);
		}
	}

	// 주문변경 마스터 저장 파라미터를 생성합니다.
	private ShopOrderChangeBaseSavePO buildShopOrderChangeBaseSavePO(
		String clmNo,
		ShopOrderCancelOrderBaseVO orderBase,
		ShopOrderReturnComputation computation,
		String returnDt,
		Long auditNo
	) {
		// 반품 신청 기준 변경구분과 상태와 실제 배송비 차감 금액만 저장합니다.
		ShopOrderChangeBaseSavePO result = new ShopOrderChangeBaseSavePO();
		result.setClmNo(clmNo);
		result.setOrdNo(orderBase == null ? null : orderBase.getOrdNo());
		result.setChgGbCd(SHOP_ORDER_CHANGE_GB_RETURN);
		result.setChgDt(returnDt);
		result.setChgCompleteDt(null);
		result.setChgStatCd(SHOP_ORDER_CHANGE_STAT_PROGRESS);
		result.setPayDelvAmt(Math.clamp(computation == null || computation.shippingDeductionAmt() < 1L ? 0L : computation.shippingDeductionAmt() * -1L,
				Integer.MIN_VALUE
				, Integer.MAX_VALUE));
		result.setRegNo(auditNo);
		result.setUdtNo(auditNo);
		return result;
	}

	// 주문변경 상세 저장 파라미터를 생성합니다.
	private ShopOrderChangeDetailSavePO buildShopOrderChangeDetailSavePO(
		String clmNo,
		ShopOrderReturnSelectedItem selectedItem,
		Long auditNo
	) {
		// 선택한 주문상품 기준 반품신청 이력 한 건을 구성합니다.
		ShopMypageOrderDetailItemVO detailItem = selectedItem == null ? null : selectedItem.detailItem();
		ShopOrderChangeDetailSavePO result = new ShopOrderChangeDetailSavePO();
		result.setClmNo(clmNo);
		result.setOrdNo(detailItem == null ? null : detailItem.getOrdNo());
		result.setOrdDtlNo(detailItem == null ? null : detailItem.getOrdDtlNo());
		result.setChgDtlGbCd(SHOP_ORDER_CHANGE_DTL_GB_RETURN);
		result.setChgDtlStatCd(SHOP_ORDER_CHANGE_DTL_STAT_RETURN_APPLY);
		result.setChgReasonCd(selectedItem == null ? null : selectedItem.reasonCd());
		result.setChgReasonDtl(selectedItem == null ? null : selectedItem.reasonDetail());
		result.setGoodsId(detailItem == null ? null : detailItem.getGoodsId());
		result.setSizeId(detailItem == null ? null : detailItem.getSizeId());
		result.setQty(selectedItem == null ? null : selectedItem.returnQty());
		result.setAddAmt(detailItem == null ? null : normalizeNonNegativeNumber(detailItem.getAddAmt()));
		result.setChangeOrdDtlNo(null);
		result.setRegNo(auditNo);
		result.setUdtNo(auditNo);

		// 반품 수량에 비례한 쿠폰과 포인트 배분 금액을 계산해 함께 저장합니다.
		if (detailItem != null && selectedItem.returnQty() > 0) {
			int currentRemainingQty = resolveShopOrderRemainingQty(detailItem);
			int returnQty = selectedItem.returnQty();
			result.setGoodsCpnDcAmt((int) resolveShopOrderIncrementAllocatedAmt(detailItem.getGoodsCouponDiscountAmt(), currentRemainingQty, 0, returnQty));
			result.setCartCpnDcAmt((int) resolveShopOrderIncrementAllocatedAmt(detailItem.getCartCouponDiscountAmt(), currentRemainingQty, 0, returnQty));
			result.setPointDcAmt((int) resolveShopOrderIncrementAllocatedAmt(detailItem.getPointUseAmt(), currentRemainingQty, 0, returnQty));
		}
		return result;
	}

	// 반품 회수지 저장 파라미터를 생성합니다.
	private ShopOrderChangeExchangeAddressSavePO buildPickupAddressSavePO(
		String clmNo,
		ShopOrderReturnPickupAddressPO pickupAddress,
		Long auditNo
	) {
		// 고객 회수지 주소 1건을 주문변경 주소 테이블 형식으로 변환합니다.
		ShopOrderChangeExchangeAddressSavePO result = new ShopOrderChangeExchangeAddressSavePO();
		result.setClmNo(clmNo);
		result.setClmAddrGbCd(SHOP_ORDER_CHANGE_ADDR_GB_RETURN_PICKUP);
		result.setAddrName(pickupAddress.getRsvNm());
		result.setAddrPostNo(pickupAddress.getPostNo());
		result.setAddrBase(pickupAddress.getBaseAddress());
		result.setAddrDtl(pickupAddress.getDetailAddress());
		result.setRegNo(auditNo);
		result.setUdtNo(auditNo);
		return result;
	}

	// 반품 도착지 저장 파라미터를 생성합니다.
	private ShopOrderChangeExchangeAddressSavePO buildDestinationAddressSavePO(
		String clmNo,
		ShopOrderReturnDestinationAddressVO destinationAddress,
		Long auditNo
	) {
		// 물류창고 도착지 주소 1건을 주문변경 주소 테이블 형식으로 변환합니다.
		ShopOrderChangeExchangeAddressSavePO result = new ShopOrderChangeExchangeAddressSavePO();
		result.setClmNo(clmNo);
		result.setClmAddrGbCd(SHOP_ORDER_CHANGE_ADDR_GB_RETURN_DESTINATION);
		result.setAddrName(destinationAddress.getAddrName());
		result.setAddrPostNo(destinationAddress.getAddrPostNo());
		result.setAddrBase(destinationAddress.getAddrBase());
		result.setAddrDtl(destinationAddress.getAddrDtl());
		result.setRegNo(auditNo);
		result.setUdtNo(auditNo);
		return result;
	}

	// 접두사와 고객번호와 년월일시분초밀리초를 조합해 클레임번호를 생성합니다.
	private String generateShopOrderClaimNo(Long custNo) {
		// 클레임번호는 주문번호와 구분되도록 C 접두사를 사용합니다.
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
		long safeCustNo = custNo == null ? 0L : Math.max(custNo, 0L);
		return "C" + safeCustNo + timestamp;
	}

	// preview 금액 비교용 signed long 값을 안전하게 반환합니다.
	private long resolvePreviewAmountValue(Long value) {
		// null 값은 0으로 보고 음수/양수 부호는 그대로 유지합니다.
		return value == null ? 0L : value;
	}

	// 환불 PAYMENT 저장금액을 음수 기준으로 변환합니다.
	private long resolveRefundPaymentAmt(Long refundedCashAmt) {
		// 환불 저장금액은 절댓값 기준으로 음수화합니다.
		long normalizedRefundedCashAmt = Math.abs(refundedCashAmt == null ? 0L : refundedCashAmt.longValue());
		return normalizedRefundedCashAmt * -1L;
	}

	// JsonNode의 숫자 필드를 long 값으로 안전하게 반환합니다.
	private long resolveJsonLong(JsonNode node, String fieldName) {
		// 숫자 노드가 아니면 텍스트 값을 long으로 다시 파싱합니다.
		if (node == null || trimToNull(fieldName) == null) {
			return 0L;
		}
		JsonNode valueNode = node.path(fieldName);
		if (valueNode.isNumber()) {
			return valueNode.longValue();
		}
		String valueText = trimToNull(valueNode.asText(null));
		if (valueText == null) {
			return 0L;
		}
		try {
			return Long.parseLong(valueText);
		} catch (NumberFormatException exception) {
			return 0L;
		}
	}

	// 현재 로그인한 관리자 번호를 조회합니다.
	private Long resolveCurrentAdminUserNo() {
		// 스프링 시큐리티 인증정보에서 관리자 사용자번호를 추출합니다.
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			return null;
		}
		Object principal = authentication.getPrincipal();
		if (principal instanceof UserBaseEntity userBaseEntity) {
			return userBaseEntity.getUsrNo();
		}
		return null;
	}

	// Integer 값을 0 이상의 안전한 숫자로 보정합니다.
	private int normalizeNonNegativeNumber(Integer value) {
		return orderService.normalizeNonNegativeNumber(value);
	}

	// Long 값을 0 이상의 안전한 숫자로 보정합니다.
	private long resolveNonNegativeLong(Long value) {
		return orderService.resolveNonNegativeLong(value);
	}

	// Integer 값을 부호 유지 long 값으로 안전하게 변환합니다.
	private long resolveSignedLong(Integer value) {
		return value == null ? 0L : value.longValue();
	}

	// 주문상세의 현재 남은 수량을 반환합니다.
	private int resolveShopOrderRemainingQty(ShopMypageOrderDetailItemVO detailItem) {
		return orderService.resolveShopOrderRemainingQty(detailItem);
	}

	// 주문상세의 상품 판매가 단가를 반환합니다.
	private int resolveShopOrderUnitOrderAmt(ShopMypageOrderDetailItemVO detailItem) {
		return orderService.resolveShopOrderUnitOrderAmt(detailItem);
	}

	// 주문상세의 이번 반품분 배분 금액을 계산합니다.
	private long resolveShopOrderIncrementAllocatedAmt(Integer allocatedAmt, int originalQty, int canceledBeforeQty, int cancelQty) {
		return orderService.resolveShopOrderIncrementAllocatedAmt(allocatedAmt, originalQty, canceledBeforeQty, cancelQty);
	}
}
