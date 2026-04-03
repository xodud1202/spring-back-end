package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.admin.order.AdminOrderReturnItemPO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderReturnPO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderReturnPageVO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartSiteInfoVO;
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
import com.xodud1202.springbackend.domain.shop.order.ShopOrderReturnDestinationAddressVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderReturnItemPO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderReturnPO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderReturnPickupAddressPO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderReturnPreviewAmountPO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderReturnResultVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderReturnWithdrawPO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderReturnWithdrawResultVO;
import com.xodud1202.springbackend.domain.shop.site.ShopSiteInfoVO;
import com.xodud1202.springbackend.mapper.OrderMapper;
import com.xodud1202.springbackend.mapper.SiteInfoMapper;
import com.xodud1202.springbackend.service.order.support.ShopMypageOrderDateRange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.xodud1202.springbackend.common.Constants.Shop.*;

@Service
@RequiredArgsConstructor
// 주문반품 도메인 비즈니스 로직을 제공합니다.
public class OrderReturnService {
	private static final int ORDER_CHANGE_ADDRESS_NAME_MAX_LENGTH = 20;
	private static final int ORDER_CHANGE_ADDRESS_POST_NO_MAX_LENGTH = 10;
	private static final int ORDER_CHANGE_ADDRESS_BASE_MAX_LENGTH = 100;
	private static final int ORDER_CHANGE_ADDRESS_DETAIL_MAX_LENGTH = 100;
	private static final String COMPANY_FAULT_REASON_PREFIX = "R_2";
	private static final String RETURN_COMPLETE_DETAIL_STATUS_CODE = "CHG_DTL_STAT_14";

	private final OrderService orderService;
	private final OrderMapper orderMapper;
	private final SiteInfoMapper siteInfoMapper;

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

	// 쇼핑몰 마이페이지 반품 신청 화면 데이터를 조회합니다.
	public ShopMypageOrderReturnPageVO getShopMypageOrderReturnPage(Long custNo, String ordNo, Integer ordDtlNo) {
		return orderService.getShopMypageOrderReturnPage(custNo, ordNo, ordDtlNo);
	}

	// 관리자 주문반품 신청 화면 데이터를 조회합니다.
	public AdminOrderReturnPageVO getAdminOrderReturnPage(String ordNo) {
		return orderService.getAdminOrderReturnPage(ordNo);
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
		int resolvedRequestedPageNo = orderService.resolveRequestedPageNo(requestedPageNo);
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
		int totalPageCount = orderService.calculateTotalPageCount(returnCount, SHOP_MYPAGE_RETURN_PAGE_SIZE);
		int resolvedPageNo = orderService.resolvePageNoWithinRange(resolvedRequestedPageNo, totalPageCount);
		int offset = orderService.calculateOffset(resolvedPageNo, SHOP_MYPAGE_RETURN_PAGE_SIZE);

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
			int qty = normalizeNonNegativeNumber(detailItem == null ? null : detailItem.getQty());
			long unitSupplyAmt = normalizeNonNegativeNumber(detailItem == null ? null : detailItem.getSupplyAmt());
			long unitOrderAmt =
				(long) normalizeNonNegativeNumber(detailItem == null ? null : detailItem.getSaleAmt())
					+ normalizeNonNegativeNumber(detailItem == null ? null : detailItem.getAddAmt());
			totalSupplyAmt += unitSupplyAmt * qty;
			totalOrderAmt += unitOrderAmt * qty;
			totalGoodsCouponDiscountAmt += normalizeNonNegativeNumber(detailItem == null ? null : detailItem.getGoodsCouponDiscountAmt());
			totalCartCouponDiscountAmt += normalizeNonNegativeNumber(detailItem == null ? null : detailItem.getCartCouponDiscountAmt());
			totalPointRefundAmt += normalizeNonNegativeNumber(detailItem == null ? null : detailItem.getPointDcAmt());
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
			Integer ordDtlNo = detailItem == null ? null : detailItem.getOrdDtlNo();
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
			Integer ordDtlNo = detailItem == null ? null : detailItem.getOrdDtlNo();
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
			Integer ordDtlNo = detailItem == null ? null : detailItem.getOrdDtlNo();
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
			&& trimToNull(returnItem == null ? null : returnItem.getReasonDetail()) == null) {
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

	// 문자열의 공백을 제거하고 빈 값이면 null을 반환합니다.
	private String trimToNull(String value) {
		return orderService.trimToNull(value);
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
