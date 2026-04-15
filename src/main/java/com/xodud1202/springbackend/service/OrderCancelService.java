package com.xodud1202.springbackend.service;

import static com.xodud1202.springbackend.common.util.CommonTextUtils.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartSiteInfoVO;
import com.xodud1202.springbackend.domain.shop.mypage.*;
import com.xodud1202.springbackend.domain.shop.order.*;
import com.xodud1202.springbackend.mapper.OrderMapper;
import com.xodud1202.springbackend.service.order.support.ShopMypageOrderDateRange;
import com.xodud1202.springbackend.service.order.support.ShopOrderCancelComputation;
import com.xodud1202.springbackend.service.order.support.ShopOrderCancelPgResult;
import com.xodud1202.springbackend.service.order.support.ShopOrderCancelPreviewSummary;
import com.xodud1202.springbackend.service.order.support.ShopOrderCancelSelectedItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;

import static com.xodud1202.springbackend.common.Constants.Shop.*;

@Service
@RequiredArgsConstructor
// 주문취소 도메인 비즈니스 로직을 제공합니다.
public class OrderCancelService {
	private final OrderService orderService;
	private final OrderMapper orderMapper;
	private final TossPaymentsClient tossPaymentsClient;
	private final PlatformTransactionManager transactionManager;

	// 쇼핑몰 마이페이지 주문취소 신청 화면 데이터를 조회합니다.
	public ShopMypageOrderCancelPageVO getShopMypageOrderCancelPage(Long custNo, String ordNo, Integer ordDtlNo) {
		// 고객번호와 주문번호를 검증합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}
		String resolvedOrdNo = trimToNull(ordNo);
		if (resolvedOrdNo == null) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_NO_INVALID_MESSAGE);
		}

		// 현재 로그인 고객의 주문번호 1건과 금액/사유/배송 기준 정보를 조회합니다.
		ShopMypageOrderGroupVO orderGroup = orderService.getShopMypageOrderGroupWithDetail(custNo, resolvedOrdNo);
		validateShopMypageOrderCancelAccess(orderGroup, ordDtlNo);
		ShopMypageOrderAmountSummaryVO amountSummary = orderService.buildShopMypageOrderAmountSummary(custNo, orderGroup);
		List<ShopMypageOrderCancelReasonVO> reasonList = orderService.normalizeShopMypageOrderCancelReasonList(
			orderMapper.getShopMypageOrderCancelReasonList()
		);
		ShopCartSiteInfoVO siteInfo = orderService.resolveShopCartSiteInfo();

		// 주문취소 신청 화면 응답 객체를 구성합니다.
		ShopMypageOrderCancelPageVO result = new ShopMypageOrderCancelPageVO();
		result.setOrder(orderGroup);
		result.setAmountSummary(amountSummary);
		result.setReasonList(reasonList);
		result.setSiteInfo(siteInfo);
		return result;
	}

	// 관리자 주문취소 신청 화면 데이터를 조회합니다.
	public ShopMypageOrderCancelPageVO getAdminOrderCancelPage(String ordNo) {
		// 주문번호 필수 검증을 수행합니다.
		String resolvedOrdNo = trimToNull(ordNo);
		if (resolvedOrdNo == null) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_NO_INVALID_MESSAGE);
		}

		// 주문번호로 고객번호를 조회합니다.
		Long custNo = orderMapper.getOrderCustNo(resolvedOrdNo);
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_NO_INVALID_MESSAGE);
		}

		// 주문 그룹/금액 요약/취소 사유/배송비 기준 정보를 조회합니다.
		// 관리자는 validateShopMypageOrderCancelAccess 검증을 생략합니다.
		ShopMypageOrderGroupVO orderGroup = orderService.getShopMypageOrderGroupWithDetail(custNo, resolvedOrdNo);
		if (orderGroup == null) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_NO_INVALID_MESSAGE);
		}
		ShopMypageOrderAmountSummaryVO amountSummary = orderService.buildShopMypageOrderAmountSummary(custNo, orderGroup);
		List<ShopMypageOrderCancelReasonVO> reasonList = orderService.normalizeShopMypageOrderCancelReasonList(
			orderMapper.getShopMypageOrderCancelReasonList()
		);
		ShopCartSiteInfoVO siteInfo = orderService.resolveShopCartSiteInfo();

		// 관리자 주문취소 신청 화면 응답 객체를 구성합니다.
		ShopMypageOrderCancelPageVO result = new ShopMypageOrderCancelPageVO();
		result.setOrder(orderGroup);
		result.setAmountSummary(amountSummary);
		result.setReasonList(reasonList);
		result.setSiteInfo(siteInfo);
		return result;
	}

	// 쇼핑몰 마이페이지 취소내역 페이지 데이터를 조회합니다.
	public ShopMypageCancelHistoryPageVO getShopMypageCancelHistoryPage(
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

		// 취소 클레임 전체 건수와 전체 페이지 수를 계산합니다.
		int cancelCount = orderMapper.countShopMypageCancelHistory(
			custNo,
			dateRange.getStartDate(),
			dateRange.getEndDate()
		);
		int totalPageCount = orderService.calculateTotalPageCount(cancelCount, SHOP_MYPAGE_CANCEL_PAGE_SIZE);
		int resolvedPageNo = orderService.resolvePageNoWithinRange(resolvedRequestedPageNo, totalPageCount);
		int offset = orderService.calculateOffset(resolvedPageNo, SHOP_MYPAGE_CANCEL_PAGE_SIZE);

		// 현재 페이지의 취소 클레임 목록을 조회합니다.
		List<ShopMypageCancelHistoryVO> cancelList = orderMapper.getShopMypageCancelHistoryList(
			custNo,
			dateRange.getStartDate(),
			dateRange.getEndDate(),
			offset,
			SHOP_MYPAGE_CANCEL_PAGE_SIZE
		);

		// 클레임번호 목록 기준으로 취소 상품 상세를 조회해 각 클레임에 매핑합니다.
		if (!cancelList.isEmpty()) {
			List<String> clmNoList = cancelList.stream()
				.map(ShopMypageCancelHistoryVO::getClmNo)
				.filter(clmNo -> clmNo != null && !clmNo.isBlank())
				.distinct()
				.collect(java.util.stream.Collectors.toList());
			List<ShopMypageCancelHistoryDetailVO> detailList = orderMapper.getShopMypageCancelHistoryDetailList(clmNoList);
			Map<String, List<ShopMypageCancelHistoryDetailVO>> detailByClmNo = detailList.stream()
				.filter(detail -> detail != null && detail.getClmNo() != null)
				.collect(java.util.stream.Collectors.groupingBy(ShopMypageCancelHistoryDetailVO::getClmNo));
			for (ShopMypageCancelHistoryVO cancel : cancelList) {
				cancel.setDetailList(detailByClmNo.getOrDefault(cancel.getClmNo(), java.util.Collections.emptyList()));
			}
		}

		// 취소내역 페이지 응답 객체를 구성합니다.
		ShopMypageCancelHistoryPageVO result = new ShopMypageCancelHistoryPageVO();
		result.setCancelList(cancelList);
		result.setCancelCount(cancelCount);
		result.setPageNo(resolvedPageNo);
		result.setPageSize(SHOP_MYPAGE_CANCEL_PAGE_SIZE);
		result.setTotalPageCount(totalPageCount);
		result.setStartDate(dateRange.getStartDate());
		result.setEndDate(dateRange.getEndDate());
		return result;
	}

	// 쇼핑몰 마이페이지 취소상세 단건을 조회합니다.
	public ShopMypageCancelHistoryVO getShopMypageCancelHistoryDetail(Long custNo, String clmNo) {
		// 고객번호가 없으면 로그인 예외를 반환합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}
		if (clmNo == null || clmNo.isBlank()) {
			throw new IllegalArgumentException("클레임번호가 필요합니다.");
		}

		// 클레임 단건을 조회합니다.
		ShopMypageCancelHistoryVO cancelItem = orderMapper.getShopMypageCancelHistoryItemByClmNo(custNo, clmNo);
		if (cancelItem == null) {
			return null;
		}

		// 취소 상품 상세 목록을 조회해 클레임에 매핑합니다.
		List<ShopMypageCancelHistoryDetailVO> detailList = orderMapper.getShopMypageCancelHistoryDetailList(
			java.util.Collections.singletonList(clmNo)
		);
		cancelItem.setDetailList(detailList != null ? detailList : java.util.Collections.emptyList());
		return cancelItem;
	}

	// 쇼핑몰 마이페이지 주문취소를 즉시 완료 처리합니다.
	public ShopOrderCancelResultVO cancelShopMypageOrder(ShopOrderCancelPO param, Long custNo) {
		// 로그인 고객번호와 주문취소 요청값을 검증합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}
		if (param == null) {
			throw new IllegalArgumentException("취소 정보를 확인해주세요.");
		}
		String ordNo = trimToNull(param.getOrdNo());
		if (ordNo == null) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_NO_INVALID_MESSAGE);
		}

		// 현재 주문/주문마스터/배송 기준과 취소사유 코드를 조회합니다.
		ShopMypageOrderGroupVO orderGroup = orderService.getShopMypageOrderGroupWithDetail(custNo, ordNo);
		ShopOrderCancelOrderBaseVO orderBase = orderService.resolveShopOrderCancelOrderBase(custNo, ordNo);
		ShopCartSiteInfoVO siteInfo = orderService.resolveShopCartSiteInfo();
		List<ShopMypageOrderCancelReasonVO> reasonList = orderService.normalizeShopMypageOrderCancelReasonList(
			orderMapper.getShopMypageOrderCancelReasonList()
		);

		// 주문취소 요청을 현재 주문 기준으로 정규화하고 서버 금액을 다시 계산합니다.
		validateShopOrderCancelReason(param, reasonList);
		Map<Integer, Integer> cancelQtyMap = resolveShopOrderCancelQtyMap(param.getCancelItemList());
		ShopOrderCancelComputation cancelComputation = buildShopOrderCancelComputation(orderGroup, orderBase, siteInfo, cancelQtyMap);
		validateShopOrderCancelPreviewAmount(param.getPreviewAmount(), cancelComputation.previewAmount());

		// 취소 대상 원결제와 환불 결제 row를 먼저 준비합니다.
		ShopOrderPaymentVO originalPayment = resolveShopOrderPaymentForCancel(ordNo);
		String clmNo = generateShopOrderClaimNo(custNo);
		ShopOrderPaymentSavePO refundPaymentSavePO = createShopOrderCancelRefundPayment(
			orderBase,
			originalPayment,
			clmNo,
			param,
			cancelComputation,
			custNo
		);

		// PG 취소 성공 시 주문/클레임/재고/포인트를 한 트랜잭션으로 반영합니다.
		try {
			return executeInShopOrderTransaction(() -> applyShopOrderCancelSuccess(
				param,
				custNo,
				orderGroup,
				orderBase,
				originalPayment,
				refundPaymentSavePO,
				clmNo,
				cancelComputation
			));
		} catch (TossPaymentClientException exception) {
			// PG 취소 실패 시 환불 결제 row만 실패 상태로 남기고 주문 변경은 롤백합니다.
			handleShopOrderCancelPaymentFailure(refundPaymentSavePO.getPayNo(), exception, custNo);
			throw new IllegalArgumentException(resolveShopOrderCancelPgErrorMessage(exception));
		}
	}

	// 관리자 주문취소를 즉시 완료 처리합니다.
	public ShopOrderCancelResultVO cancelAdminOrder(ShopOrderCancelPO param) {
		// 취소 요청값을 검증합니다.
		if (param == null) {
			throw new IllegalArgumentException("취소 정보를 확인해주세요.");
		}
		String ordNo = trimToNull(param.getOrdNo());
		if (ordNo == null) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_NO_INVALID_MESSAGE);
		}

		// 주문번호로 고객번호를 조회합니다.
		Long custNo = orderMapper.getOrderCustNo(ordNo);
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_NO_INVALID_MESSAGE);
		}

		// 현재 주문/주문마스터/배송 기준과 취소사유 코드를 조회합니다.
		ShopMypageOrderGroupVO orderGroup = orderService.getShopMypageOrderGroupWithDetail(custNo, ordNo);
		ShopOrderCancelOrderBaseVO orderBase = orderService.resolveShopOrderCancelOrderBase(custNo, ordNo);
		ShopCartSiteInfoVO siteInfo = orderService.resolveShopCartSiteInfo();
		List<ShopMypageOrderCancelReasonVO> reasonList = orderService.normalizeShopMypageOrderCancelReasonList(
			orderMapper.getShopMypageOrderCancelReasonList()
		);

		// 취소 요청을 검증하고 관리자 모드로 서버 금액을 재계산합니다.
		validateShopOrderCancelReason(param, reasonList);
		Map<Integer, Integer> cancelQtyMap = resolveShopOrderCancelQtyMap(param.getCancelItemList());
		ShopOrderCancelComputation cancelComputation = buildShopOrderCancelComputation(orderGroup, orderBase, siteInfo, cancelQtyMap, true);
		validateShopOrderCancelPreviewAmount(param.getPreviewAmount(), cancelComputation.previewAmount());

		// 취소 대상 원결제와 환불 결제 row를 먼저 준비합니다.
		ShopOrderPaymentVO originalPayment = resolveShopOrderPaymentForCancel(ordNo);
		String clmNo = generateShopOrderClaimNo(custNo);
		ShopOrderPaymentSavePO refundPaymentSavePO = createShopOrderCancelRefundPayment(
			orderBase,
			originalPayment,
			clmNo,
			param,
			cancelComputation,
			custNo
		);

		// PG 취소 성공 시 주문/클레임/재고/포인트를 한 트랜잭션으로 반영합니다.
		try {
			return executeInShopOrderTransaction(() -> applyShopOrderCancelSuccess(
				param,
				custNo,
				orderGroup,
				orderBase,
				originalPayment,
				refundPaymentSavePO,
				clmNo,
				cancelComputation
			));
		} catch (TossPaymentClientException exception) {
			// PG 취소 실패 시 환불 결제 row만 실패 상태로 남기고 주문 변경은 롤백합니다.
			handleShopOrderCancelPaymentFailure(refundPaymentSavePO.getPayNo(), exception, custNo);
			throw new IllegalArgumentException(resolveShopOrderCancelPgErrorMessage(exception));
		}
	}

	// 주문취소 신청 화면 진입 시 주문상세번호와 취소 가능 상태를 검증합니다.
	private void validateShopMypageOrderCancelAccess(ShopMypageOrderGroupVO orderGroup, Integer ordDtlNo) {
		// 주문상세 목록이 없으면 취소 불가 예외를 반환합니다.
		if (orderGroup == null || orderGroup.getDetailList() == null || orderGroup.getDetailList().isEmpty()) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_CANCEL_UNAVAILABLE_MESSAGE);
		}

		// 취소 가능 상품 존재 여부와 요청 주문상세번호 유효성을 함께 확인합니다.
		boolean hasCancelableDetail = false;
		boolean matchedRequestedDetail = ordDtlNo == null;
		for (ShopMypageOrderDetailItemVO detailItem : orderGroup.getDetailList()) {
			if (detailItem == null) {
				continue;
			}

			// 취소 가능 상태와 잔여 수량이 있는 행만 취소 대상 후보로 인정합니다.
			int cancelableQty = normalizeNonNegativeNumber(detailItem.getCancelableQty());
			boolean cancelable =
				cancelableQty > 0
					&& (
						SHOP_ORDER_DTL_STAT_WAITING_DEPOSIT.equals(detailItem.getOrdDtlStatCd())
							|| SHOP_ORDER_DTL_STAT_DONE.equals(detailItem.getOrdDtlStatCd())
					);
			if (cancelable) {
				hasCancelableDetail = true;
			}

			// 요청 주문상세번호가 있으면 현재 주문의 취소 가능 상품과 일치하는지 확인합니다.
			if (ordDtlNo != null && ordDtlNo.equals(detailItem.getOrdDtlNo())) {
				if (!cancelable) {
					throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_DETAIL_ITEM_INVALID_MESSAGE);
				}
				matchedRequestedDetail = true;
			}
		}

		// 취소 가능한 상품이 없으면 취소 신청 화면 진입을 막습니다.
		if (!hasCancelableDetail) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_CANCEL_UNAVAILABLE_MESSAGE);
		}

		// 요청 주문상세번호가 현재 주문에 없으면 잘못된 접근으로 처리합니다.
		if (!matchedRequestedDetail) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_DETAIL_ITEM_INVALID_MESSAGE);
		}
	}

	// 주문취소 사유 코드와 직접입력값을 검증합니다.
	private void validateShopOrderCancelReason(ShopOrderCancelPO param, List<ShopMypageOrderCancelReasonVO> reasonList) {
		// 선택 상품별 취소 사유 코드가 비어 있거나 현재 사용 가능한 사유가 아니면 예외를 반환합니다.
		List<ShopOrderCancelItemPO> cancelItemList = param == null ? null : param.getCancelItemList();
		if (cancelItemList == null || cancelItemList.isEmpty()) {
			return;
		}
		for (ShopOrderCancelItemPO cancelItem : cancelItemList) {
			String reasonCd = trimToNull(cancelItem == null ? null : cancelItem.getReasonCd());
			if (reasonCd == null) {
				throw new IllegalArgumentException("주문 취소 사유를 선택해주세요.");
			}

			ShopMypageOrderCancelReasonVO matchedReason = findShopOrderCancelReason(reasonList, reasonCd);
			if (matchedReason == null) {
				throw new IllegalArgumentException("주문 취소 사유를 선택해주세요.");
			}

			// 기타 사유는 상품별 직접입력값을 필수로 확인합니다.
			if (isShopOrderReasonDetailRequired(matchedReason)
				&& trimToNull(cancelItem == null ? null : cancelItem.getReasonDetail()) == null) {
				throw new IllegalArgumentException("기타 사유를 입력해주세요.");
			}
		}
	}

	// 주문취소 사유 목록에서 요청 사유 코드 1건을 찾습니다.
	private ShopMypageOrderCancelReasonVO findShopOrderCancelReason(
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

	// 주문취소 사유가 직접입력값을 요구하는지 반환합니다.
	private boolean isShopOrderReasonDetailRequired(ShopMypageOrderCancelReasonVO reasonItem) {
		// 사유명에 기타가 포함되면 직접입력값을 필수로 처리합니다.
		String reasonName = trimToNull(reasonItem == null ? null : reasonItem.getCdNm());
		return reasonName != null && reasonName.contains("기타");
	}

	// 주문취소 요청 상품 목록을 주문상세번호별 취소수량 맵으로 정규화합니다.
	private Map<Integer, Integer> resolveShopOrderCancelQtyMap(List<ShopOrderCancelItemPO> cancelItemList) {
		// 선택된 주문상품이 없으면 요청 오류를 반환합니다.
		if (cancelItemList == null || cancelItemList.isEmpty()) {
			throw new IllegalArgumentException("취소할 상품을 선택해주세요.");
		}

		// 주문상세번호별 중복 여부와 취소수량 1 이상 여부를 함께 검증합니다.
		Map<Integer, Integer> result = new LinkedHashMap<>();
		for (ShopOrderCancelItemPO cancelItem : cancelItemList) {
			if (cancelItem == null || cancelItem.getOrdDtlNo() == null || cancelItem.getOrdDtlNo() < 1 || cancelItem.getCancelQty() == null || cancelItem.getCancelQty() < 1) {
				throw new IllegalArgumentException("주문상품 정보를 확인해주세요.");
			}
			if (result.putIfAbsent(cancelItem.getOrdDtlNo(), cancelItem.getCancelQty()) != null) {
				throw new IllegalArgumentException("주문상품 정보를 확인해주세요.");
			}
		}
		return result;
	}

	// 주문취소 요청 기준 서버 재계산 결과를 구성합니다.
	private ShopOrderCancelComputation buildShopOrderCancelComputation(
		ShopMypageOrderGroupVO orderGroup,
		ShopOrderCancelOrderBaseVO orderBase,
		ShopCartSiteInfoVO siteInfo,
		Map<Integer, Integer> cancelQtyMap
	) {
		// 기본 호출은 관리자 모드 없이 실행합니다.
		return buildShopOrderCancelComputation(orderGroup, orderBase, siteInfo, cancelQtyMap, false);
	}

	// 주문취소 요청 기준 서버 재계산 결과를 구성합니다.
	private ShopOrderCancelComputation buildShopOrderCancelComputation(
		ShopMypageOrderGroupVO orderGroup,
		ShopOrderCancelOrderBaseVO orderBase,
		ShopCartSiteInfoVO siteInfo,
		Map<Integer, Integer> cancelQtyMap,
		boolean adminMode
	) {
		// 주문상세 목록이 없으면 취소 불가 예외를 반환합니다.
		if (orderGroup == null || orderGroup.getDetailList() == null || orderGroup.getDetailList().isEmpty()) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_CANCEL_UNAVAILABLE_MESSAGE);
		}

		// 현재 남아 있는 주문행 기준으로 취소 모드와 환급 금액을 계산합니다.
		ShopOrderCancelPreviewSummary previewSummary = new ShopOrderCancelPreviewSummary();
		List<ShopOrderCancelSelectedItem> selectedItemList = new ArrayList<>();
		boolean hasWaitingDepositSelection = false;
		boolean hasPaymentDoneSelection = false;
		long currentOrderAmt = 0L;
		long remainingOrderAmtAfterCancel = 0L;
		int selectedQtyCount = 0;
		int activeRemainingRowCount = 0;
		int fullyCanceledRowCount = 0;
		for (ShopMypageOrderDetailItemVO detailItem : orderGroup.getDetailList()) {
			if (detailItem == null || detailItem.getOrdDtlNo() == null) {
				continue;
			}
			int originalQty = normalizeNonNegativeNumber(detailItem.getOrdQty());
			int remainingQty = resolveShopOrderRemainingQty(detailItem);
			if (originalQty < 1 || remainingQty < 1) {
				continue;
			}
			activeRemainingRowCount += 1;
			currentOrderAmt += (long) resolveShopOrderUnitOrderAmt(detailItem) * remainingQty;

			// 요청에 포함되지 않은 행은 남은 주문 금액 계산에만 반영합니다.
			Integer requestedCancelQty = cancelQtyMap.get(detailItem.getOrdDtlNo());
			if (requestedCancelQty == null) {
				remainingOrderAmtAfterCancel += (long) resolveShopOrderUnitOrderAmt(detailItem) * remainingQty;
				continue;
			}

			// 현재 상태와 남은 수량 기준으로 취소 가능 여부와 수량 범위를 검증합니다.
			boolean waitingDepositCancelable = SHOP_ORDER_DTL_STAT_WAITING_DEPOSIT.equals(detailItem.getOrdDtlStatCd());
			boolean paymentDoneCancelable = SHOP_ORDER_DTL_STAT_DONE.equals(detailItem.getOrdDtlStatCd())
				|| (adminMode && SHOP_ORDER_DTL_STAT_PREPARING.equals(detailItem.getOrdDtlStatCd()));
			if (!waitingDepositCancelable && !paymentDoneCancelable) {
				throw new IllegalArgumentException("주문상품 정보를 확인해주세요.");
			}
			if (requestedCancelQty < 1 || requestedCancelQty > remainingQty) {
				throw new IllegalArgumentException("주문상품 정보를 확인해주세요.");
			}
			if (waitingDepositCancelable) {
				hasWaitingDepositSelection = true;
			}
			if (paymentDoneCancelable) {
				hasPaymentDoneSelection = true;
			}

			// 선택 행의 취소 수량 기준 상품과 쿠폰과 포인트 환급 금액을 누적합니다.
			selectedQtyCount += requestedCancelQty;
			if (remainingQty - requestedCancelQty < 1) {
				fullyCanceledRowCount += 1;
			}
			remainingOrderAmtAfterCancel += (long) resolveShopOrderUnitOrderAmt(detailItem) * (remainingQty - requestedCancelQty);
			accumulateShopOrderCancelPreviewAmount(previewSummary, detailItem, requestedCancelQty);
			selectedItemList.add(buildShopOrderCancelSelectedItem(detailItem, requestedCancelQty));
		}

		// 무통장입금은 전체취소만, 결제완료는 부분취소와 전체취소를 허용합니다.
		if (selectedItemList.isEmpty() || selectedQtyCount < 1) {
			throw new IllegalArgumentException("취소할 상품을 선택해주세요.");
		}
		if (hasWaitingDepositSelection && hasPaymentDoneSelection) {
			throw new IllegalArgumentException("주문상품 정보를 확인해주세요.");
		}
		if (hasWaitingDepositSelection) {
			validateShopOrderCancelFullOnly(orderGroup, cancelQtyMap);
		}

		// 배송비 환급과 차감과 취소 예정 금액과 PG 현금 환불액을 현재 주문 금액 기준으로 계산합니다.
		boolean isFullCancel = activeRemainingRowCount > 0 && fullyCanceledRowCount == activeRemainingRowCount;
		long paidDeliveryFeeRefundAmt =
			isFullCancel
				? Math.max(
					resolveNonNegativeLong(orderBase == null ? null : Long.valueOf(normalizeNonNegativeNumber(orderBase.getOrdDelvAmt())))
						- resolveNonNegativeLong(orderBase == null ? null : Long.valueOf(normalizeNonNegativeNumber(orderBase.getDelvCpnDcAmt()))),
					0L
				)
				: 0L;
		long deliveryCouponRefundAmt =
			isFullCancel
				? resolveNonNegativeLong(orderBase == null ? null : Long.valueOf(normalizeNonNegativeNumber(orderBase.getDelvCpnDcAmt())))
				: 0L;
		long shippingDeductionAmt =
			!isFullCancel
				&& resolveNonNegativeLong(orderBase == null ? null : Long.valueOf(normalizeNonNegativeNumber(orderBase.getOrdDelvAmt()))) < 1L
				&& currentOrderAmt > 0L
				&& remainingOrderAmtAfterCancel > 0L
				&& remainingOrderAmtAfterCancel < resolveNonNegativeLong(siteInfo == null ? null : Long.valueOf(normalizeNonNegativeNumber(siteInfo.getDeliveryFeeLimit())))
					? resolveNonNegativeLong(siteInfo == null ? null : Long.valueOf(normalizeNonNegativeNumber(siteInfo.getDeliveryFee())))
					: 0L;
		long paidGoodsAmt = previewSummary.getTotalOrderAmt();
		long benefitAmt =
			previewSummary.getTotalGoodsCouponDiscountAmt()
				+ previewSummary.getTotalCartCouponDiscountAmt()
				+ previewSummary.getTotalPointRefundAmt();
		long shippingAdjustmentAmt = paidDeliveryFeeRefundAmt - shippingDeductionAmt;
		long expectedRefundAmt = paidGoodsAmt - benefitAmt + shippingAdjustmentAmt;
		long refundedCashAmt = expectedRefundAmt;
		if (refundedCashAmt < 0L) {
			throw new IllegalArgumentException("배송비 차감 후 취소 예정 금액이 0원 미만이라 신청할 수 없습니다.");
		}

		// 프론트와 비교할 취소 예정 금액 요약과 후속 처리 정보를 구성합니다.
		ShopOrderCancelPreviewAmountPO previewAmount = new ShopOrderCancelPreviewAmountPO();
		previewAmount.setExpectedRefundAmt(expectedRefundAmt);
		previewAmount.setPaidGoodsAmt(paidGoodsAmt);
		previewAmount.setBenefitAmt(benefitAmt);
		previewAmount.setShippingAdjustmentAmt(shippingAdjustmentAmt);
		previewAmount.setTotalPointRefundAmt(previewSummary.getTotalPointRefundAmt());
		previewAmount.setDeliveryCouponRefundAmt(deliveryCouponRefundAmt);
		return new ShopOrderCancelComputation(
			selectedItemList,
			previewAmount,
			isFullCancel,
			refundedCashAmt,
			previewSummary.getTotalPointRefundAmt(),
			shippingAdjustmentAmt
		);
	}

	// 무통장입금 주문은 현재 남은 주문 전체를 전량 선택했는지 확인합니다.
	private void validateShopOrderCancelFullOnly(ShopMypageOrderGroupVO orderGroup, Map<Integer, Integer> cancelQtyMap) {
		// 남은 주문행 중 하나라도 빠졌거나 전량 선택이 아니면 전체취소 전용 예외를 반환합니다.
		for (ShopMypageOrderDetailItemVO detailItem : orderGroup == null ? List.<ShopMypageOrderDetailItemVO>of() : orderGroup.getDetailList()) {
			if (detailItem == null) {
				continue;
			}
			int remainingQty = resolveShopOrderRemainingQty(detailItem);
			if (remainingQty < 1) {
				continue;
			}
			if (!SHOP_ORDER_DTL_STAT_WAITING_DEPOSIT.equals(detailItem.getOrdDtlStatCd())) {
				throw new IllegalArgumentException("무통장입금 주문은 전체취소만 가능합니다.");
			}
			Integer selectedCancelQty = cancelQtyMap.get(detailItem.getOrdDtlNo());
			if (selectedCancelQty == null || selectedCancelQty != remainingQty) {
				throw new IllegalArgumentException("무통장입금 주문은 전체취소만 가능합니다.");
			}
		}
	}

	// 취소 예정 금액 비교용 서버 계산 결과를 행 단위로 누적합니다.
	private void accumulateShopOrderCancelPreviewAmount(
		ShopOrderCancelPreviewSummary previewAmount,
		ShopMypageOrderDetailItemVO detailItem,
		int cancelQty
	) {
		// DB의 쿠폰과 포인트 할인금액은 이전 취소 후 이미 차감된 현재 잔여 금액이므로 RMN_QTY 기준으로 비례 계산합니다.
		int remainingQty = resolveShopOrderRemainingQty(detailItem);
		long supplyAmt = (long) normalizeNonNegativeNumber(detailItem.getSupplyAmt()) * cancelQty;
		long orderAmt = (long) resolveShopOrderUnitOrderAmt(detailItem) * cancelQty;
		long goodsCouponDiscountAmt = resolveShopOrderIncrementAllocatedAmt(detailItem.getGoodsCouponDiscountAmt(), remainingQty, 0, cancelQty);
		long cartCouponDiscountAmt = resolveShopOrderIncrementAllocatedAmt(detailItem.getCartCouponDiscountAmt(), remainingQty, 0, cancelQty);
		long pointRefundAmt = resolveShopOrderIncrementAllocatedAmt(detailItem.getPointUseAmt(), remainingQty, 0, cancelQty);

		// 공급가와 상품가와 상품할인과 상품쿠폰과 장바구니쿠폰과 포인트 환급 누계를 반영합니다.
		previewAmount.setTotalSupplyAmt(previewAmount.getTotalSupplyAmt() + supplyAmt);
		previewAmount.setTotalOrderAmt(previewAmount.getTotalOrderAmt() + orderAmt);
		previewAmount.setTotalGoodsCouponDiscountAmt(previewAmount.getTotalGoodsCouponDiscountAmt() + goodsCouponDiscountAmt);
		previewAmount.setTotalCartCouponDiscountAmt(previewAmount.getTotalCartCouponDiscountAmt() + cartCouponDiscountAmt);
		previewAmount.setTotalPointRefundAmt(previewAmount.getTotalPointRefundAmt() + pointRefundAmt);
	}

	// 서버 재계산 취소 예정 금액과 클라이언트 전송 금액이 정확히 같은지 확인합니다.
	private void validateShopOrderCancelPreviewAmount(
		ShopOrderCancelPreviewAmountPO clientPreviewAmount,
		ShopOrderCancelPreviewAmountPO serverPreviewAmount
	) {
		// 화면과 서버 계산 결과가 하나라도 다르면 진행하지 않습니다.
		if (resolveNonNegativeLong(clientPreviewAmount == null ? null : clientPreviewAmount.getExpectedRefundAmt()) != resolveNonNegativeLong(serverPreviewAmount.getExpectedRefundAmt())
			|| resolveNonNegativeLong(clientPreviewAmount == null ? null : clientPreviewAmount.getPaidGoodsAmt()) != resolveNonNegativeLong(serverPreviewAmount.getPaidGoodsAmt())
			|| resolveNonNegativeLong(clientPreviewAmount == null ? null : clientPreviewAmount.getBenefitAmt()) != resolveNonNegativeLong(serverPreviewAmount.getBenefitAmt())
			|| resolveNonNegativeLong(clientPreviewAmount == null ? null : clientPreviewAmount.getShippingAdjustmentAmt()) != resolveNonNegativeLong(serverPreviewAmount.getShippingAdjustmentAmt())
			|| resolveNonNegativeLong(clientPreviewAmount == null ? null : clientPreviewAmount.getTotalPointRefundAmt()) != resolveNonNegativeLong(serverPreviewAmount.getTotalPointRefundAmt())
			|| resolveNonNegativeLong(clientPreviewAmount == null ? null : clientPreviewAmount.getDeliveryCouponRefundAmt()) != resolveNonNegativeLong(serverPreviewAmount.getDeliveryCouponRefundAmt())) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_CANCEL_AMOUNT_MISMATCH_MESSAGE);
		}
	}

	// 주문취소 대상 원결제 정보를 조회합니다.
	private ShopOrderPaymentVO resolveShopOrderPaymentForCancel(String ordNo) {
		// 현재 주문의 승인 또는 입금대기 원결제가 없으면 취소를 진행할 수 없습니다.
		ShopOrderPaymentVO payment = orderMapper.getShopOrderPaymentForCancel(ordNo);
		if (payment == null || payment.getPayNo() == null || isBlank(payment.getPayMethodCd())) {
			throw new IllegalArgumentException("취소 가능한 결제 정보를 찾을 수 없습니다.");
		}
		return payment;
	}

	// 환불 결제 row를 선등록하고 생성된 결제번호를 반환합니다.
	private ShopOrderPaymentSavePO createShopOrderCancelRefundPayment(
		ShopOrderCancelOrderBaseVO orderBase,
		ShopOrderPaymentVO originalPayment,
		String clmNo,
		ShopOrderCancelPO param,
		ShopOrderCancelComputation cancelComputation,
		Long custNo
	) {
		// 요청 스냅샷을 함께 저장해 PG 실패 시에도 환불 결제 이력을 추적할 수 있게 합니다.
		Map<String, Object> refundSnapshot = new LinkedHashMap<>();
		refundSnapshot.put("ordNo", orderBase == null ? null : orderBase.getOrdNo());
		refundSnapshot.put("clmNo", clmNo);
		refundSnapshot.put("cancelPgReason", resolveShopOrderCancelPgReason(param));
		refundSnapshot.put("cancelItemList", param == null ? List.of() : param.getCancelItemList());
		refundSnapshot.put("previewAmount", cancelComputation == null ? null : cancelComputation.previewAmount());
		refundSnapshot.put("refundedCashAmt", cancelComputation == null ? 0L : cancelComputation.refundedCashAmt());
		refundSnapshot.put("refundReceiveAccount", buildRefundReceiveAccountSnapshot(orderBase));

		// 환불 PAYMENT row는 메인 트랜잭션과 분리해 먼저 커밋합니다.
		return executeInNewShopOrderTransaction(() -> {
			ShopOrderPaymentSavePO refundPaymentSavePO = new ShopOrderPaymentSavePO();
			refundPaymentSavePO.setOrdNo(orderBase == null ? null : orderBase.getOrdNo());
			refundPaymentSavePO.setCustNo(orderBase == null ? custNo : orderBase.getCustNo());
			refundPaymentSavePO.setPayStatCd(SHOP_ORDER_PAY_STAT_READY);
			refundPaymentSavePO.setPayGbCd(SHOP_ORDER_PAY_GB_REFUND);
			refundPaymentSavePO.setPayMethodCd(originalPayment == null ? null : originalPayment.getPayMethodCd());
			refundPaymentSavePO.setOrdGbCd(SHOP_ORDER_ORD_GB_ORDER);
			refundPaymentSavePO.setPgGbCd(originalPayment == null ? SHOP_ORDER_PG_GB_TOSS : originalPayment.getPgGbCd());
			refundPaymentSavePO.setOrgPayNo(originalPayment == null ? null : originalPayment.getPayNo());
			refundPaymentSavePO.setClmNo(clmNo);
			refundPaymentSavePO.setPayAmt(resolveRefundPaymentAmt(cancelComputation == null ? null : cancelComputation.refundedCashAmt()));
			refundPaymentSavePO.setDeviceGbCd(orderBase == null ? "PC" : firstNonBlank(trimToNull(orderBase.getDeviceGbCd()), "PC"));
			refundPaymentSavePO.setReqRawJson(writeShopOrderJson(refundSnapshot));
			refundPaymentSavePO.setRegNo(custNo);
			refundPaymentSavePO.setUdtNo(custNo);
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

		// 추적 가능한 동일 구조로 은행코드와 계좌번호와 예금주명을 저장합니다.
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("bank", refundBankCd);
		result.put("accountNumber", refundBankNo);
		result.put("holderName", refundHolderNm);
		return result;
	}

	// 메인 주문취소 트랜잭션을 실행합니다.
	private <T> T executeInShopOrderTransaction(Supplier<T> action) {
		// 기본 전파속성의 트랜잭션 템플릿으로 주문취소 반영을 실행합니다.
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		return transactionTemplate.execute(status -> action.get());
	}

	// 별도 커밋이 필요한 주문취소 보조 트랜잭션을 실행합니다.
	private <T> T executeInNewShopOrderTransaction(Supplier<T> action) {
		// 환불 PAYMENT 선등록과 실패 반영처럼 독립 커밋이 필요한 작업을 REQUIRES_NEW로 실행합니다.
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return transactionTemplate.execute(status -> action.get());
	}

	// 주문취소 성공 시 주문과 클레임과 포인트와 재고와 PAYMENT를 함께 반영합니다.
	private ShopOrderCancelResultVO applyShopOrderCancelSuccess(
		ShopOrderCancelPO param,
		Long custNo,
		ShopMypageOrderGroupVO orderGroup,
		ShopOrderCancelOrderBaseVO orderBase,
		ShopOrderPaymentVO originalPayment,
		ShopOrderPaymentSavePO refundPaymentSavePO,
		String clmNo,
		ShopOrderCancelComputation cancelComputation
	) {
		// 주문변경 마스터와 상세와 주문상세 취소 수량 및 쿠폰과 포인트 할인금액을 먼저 반영합니다.
		String cancelDt = LocalDateTime.now().format(SHOP_MYPAGE_ORDER_DATE_TIME_FORMATTER);
		orderMapper.insertShopOrderChangeBase(buildShopOrderChangeBaseSavePO(clmNo, orderBase, cancelComputation, cancelDt, custNo));
		for (ShopOrderCancelSelectedItem selectedItem : cancelComputation.selectedItemList()) {
			// 취소 수량 비례 배분 금액을 클레임 상세에 함께 저장합니다.
			ShopOrderChangeDetailSavePO changeDetail = buildShopOrderChangeDetailSavePO(clmNo, selectedItem, param, custNo);
			orderMapper.insertShopOrderChangeDetail(changeDetail);
			int updatedCount = orderMapper.updateShopOrderDetailCancelQuantity(
				orderBase.getOrdNo(),
				selectedItem.detailItem().getOrdDtlNo(),
				selectedItem.cancelQty(),
				selectedItem.nextOrdDtlStatCd(),
				changeDetail.getGoodsCpnDcAmt() != null ? changeDetail.getGoodsCpnDcAmt() : 0,
				changeDetail.getCartCpnDcAmt() != null ? changeDetail.getCartCpnDcAmt() : 0,
				changeDetail.getPointDcAmt() != null ? changeDetail.getPointDcAmt() : 0,
				custNo
			);
			if (updatedCount < 1) {
				throw new IllegalArgumentException("주문상품 정보를 확인해주세요.");
			}
		}

		// 취소 후 더 이상 사용 중이 아닌 고객쿠폰만 CUST_CPN_NO 기준으로 원복합니다.
		restoreShopOrderCancelCouponUse(custNo, orderBase.getOrdNo(), orderBase, orderGroup, cancelComputation);

		// 취소 수량만큼 재고와 포인트를 복구합니다.
		restoreShopOrderCancelSelectedStock(cancelComputation.selectedItemList(), custNo);
		restoreShopOrderPointByAmount(custNo, orderBase.getOrdNo(), cancelComputation.restoredPointAmt());

		// PG 취소 성공 응답을 저장하고 전체취소면 주문 마스터도 함께 완료 처리합니다.
		ShopOrderCancelPgResult cancelPgResult = cancelShopOrderPaymentWithPg(originalPayment, orderBase, param, cancelComputation);
		orderMapper.updateShopPaymentCancelSuccess(
			refundPaymentSavePO.getPayNo(),
			SHOP_ORDER_PAY_STAT_CANCEL,
			cancelPgResult.canceledAmount(),
			cancelPgResult.tradeNo(),
			cancelPgResult.rspCode(),
			cancelPgResult.rspMsg(),
			cancelPgResult.rawResponse(),
			cancelPgResult.approvedDt(),
			custNo
		);
		if (cancelComputation.fullCancel()) {
			orderMapper.updateShopOrderBaseFullCancel(orderBase.getOrdNo(), SHOP_ORDER_STAT_CANCEL, custNo);
		}

		// 주문취소 완료 응답 객체를 구성합니다.
		ShopOrderCancelResultVO result = new ShopOrderCancelResultVO();
		result.setClmNo(clmNo);
		result.setOrdNo(orderBase.getOrdNo());
		result.setRefundPayNo(refundPaymentSavePO.getPayNo());
		result.setPayStatCd(SHOP_ORDER_PAY_STAT_CANCEL);
		result.setRefundedCashAmt(cancelComputation.refundedCashAmt());
		result.setRestoredPointAmt(cancelComputation.restoredPointAmt());
		return result;
	}

	// 주문취소 완료 후 더 이상 사용 중이 아닌 고객쿠폰 사용 상태를 원복합니다.
	private void restoreShopOrderCancelCouponUse(
		Long custNo,
		String ordNo,
		ShopOrderCancelOrderBaseVO orderBase,
		ShopMypageOrderGroupVO orderGroup,
		ShopOrderCancelComputation cancelComputation
	) {
		// 고객번호와 주문번호가 없거나 계산 결과가 없으면 고객쿠폰 원복을 수행하지 않습니다.
		if (custNo == null || custNo < 1L || isBlank(ordNo) || cancelComputation == null) {
			return;
		}

		// 취소 후 더 이상 활성 주문상세가 참조하지 않는 고객쿠폰번호 목록만 원복합니다.
		List<Long> restorableCustCpnNoList = resolveRestorableShopOrderCancelCouponNoList(orderBase, orderGroup, cancelComputation);
		if (restorableCustCpnNoList.isEmpty()) {
			return;
		}
		orderMapper.restoreShopCustomerCouponUseByCustCpnNoList(custNo, ordNo, restorableCustCpnNoList, custNo);
	}

	// 주문취소 후 CUSTOMER_COUPON 미사용 원복 대상 고객쿠폰번호 목록을 계산합니다.
	private List<Long> resolveRestorableShopOrderCancelCouponNoList(
		ShopOrderCancelOrderBaseVO orderBase,
		ShopMypageOrderGroupVO orderGroup,
		ShopOrderCancelComputation cancelComputation
	) {
		// 선택된 주문상세가 없으면 원복 대상도 없습니다.
		if (cancelComputation == null || cancelComputation.selectedItemList() == null || cancelComputation.selectedItemList().isEmpty()) {
			return List.of();
		}

		// 취소 후 남은 수량과 활성 장바구니쿠폰 집계를 계산할 선택 주문상세 맵을 먼저 구성합니다.
		Map<Integer, ShopOrderCancelSelectedItem> selectedItemMap = new LinkedHashMap<>();
		for (ShopOrderCancelSelectedItem selectedItem : cancelComputation.selectedItemList()) {
			if (selectedItem == null || selectedItem.detailItem() == null || selectedItem.detailItem().getOrdDtlNo() == null) {
				continue;
			}
			selectedItemMap.put(selectedItem.detailItem().getOrdDtlNo(), selectedItem);
		}

		// 취소 후에도 남아 있는 장바구니쿠폰 집합을 한 번에 계산합니다.
		Set<Long> activeCartCouponNoSet = new HashSet<>();
		for (ShopMypageOrderDetailItemVO detailItem : orderGroup == null ? List.<ShopMypageOrderDetailItemVO>of() : orderGroup.getDetailList()) {
			if (detailItem == null) {
				continue;
			}
			ShopOrderCancelSelectedItem selectedItem = detailItem.getOrdDtlNo() == null ? null : selectedItemMap.get(detailItem.getOrdDtlNo());
			int remainingAfterCancelQty = selectedItem == null ? resolveShopOrderRemainingQty(detailItem) : Math.max(selectedItem.remainingAfterCancelQty(), 0);
			if (remainingAfterCancelQty < 1 || detailItem.getCartCpnNo() == null || detailItem.getCartCpnNo() < 1L) {
				continue;
			}
			activeCartCouponNoSet.add(detailItem.getCartCpnNo());
		}

		// 상품쿠폰과 장바구니쿠폰과 배송비쿠폰 중 이번 취소 후 완전히 해제되는 고객쿠폰번호만 수집합니다.
		Set<Long> restorableCustCpnNoSet = new LinkedHashSet<>();
		for (ShopOrderCancelSelectedItem selectedItem : cancelComputation.selectedItemList()) {
			if (selectedItem == null || selectedItem.detailItem() == null || selectedItem.remainingAfterCancelQty() > 0) {
				continue;
			}

			// 상품쿠폰은 해당 주문상세가 전량 취소된 시점에만 원복 후보가 됩니다.
			Long goodsCpnNo = selectedItem.detailItem().getGoodsCpnNo();
			if (goodsCpnNo != null && goodsCpnNo > 0L) {
				restorableCustCpnNoSet.add(goodsCpnNo);
			}

			// 장바구니쿠폰은 같은 쿠폰을 참조하는 활성 주문상세가 더 이상 없을 때만 원복합니다.
			Long cartCpnNo = selectedItem.detailItem().getCartCpnNo();
			if (cartCpnNo != null && cartCpnNo > 0L && !activeCartCouponNoSet.contains(cartCpnNo)) {
				restorableCustCpnNoSet.add(cartCpnNo);
			}
		}

		// 전체취소면 배송비쿠폰도 함께 원복합니다.
		Long deliveryCpnNo = orderBase == null ? null : orderBase.getDelvCpnNo();
		if (cancelComputation.fullCancel() && deliveryCpnNo != null && deliveryCpnNo > 0L) {
			restorableCustCpnNoSet.add(deliveryCpnNo);
		}
		return new ArrayList<>(restorableCustCpnNoSet);
	}

	// 주문취소 대상 상품 수량만큼 재고를 복구합니다.
	private void restoreShopOrderCancelSelectedStock(List<ShopOrderCancelSelectedItem> selectedItemList, Long auditNo) {
		// 동일 상품과 사이즈는 취소 수량을 합산해 재고를 복구합니다.
		Map<String, ShopOrderRestoreCartItemVO> stockItemMap = new LinkedHashMap<>();
		for (ShopOrderCancelSelectedItem selectedItem : selectedItemList == null ? List.<ShopOrderCancelSelectedItem>of() : selectedItemList) {
			if (selectedItem == null || selectedItem.detailItem() == null || isBlank(selectedItem.detailItem().getGoodsId()) || isBlank(selectedItem.detailItem().getSizeId())) {
				continue;
			}
			String stockKey = selectedItem.detailItem().getGoodsId().trim() + "|" + selectedItem.detailItem().getSizeId().trim();
			ShopOrderRestoreCartItemVO aggregateItem = stockItemMap.get(stockKey);
			if (aggregateItem == null) {
				aggregateItem = new ShopOrderRestoreCartItemVO();
				aggregateItem.setGoodsId(selectedItem.detailItem().getGoodsId().trim());
				aggregateItem.setSizeId(selectedItem.detailItem().getSizeId().trim());
				aggregateItem.setOrdQty(0);
				stockItemMap.put(stockKey, aggregateItem);
			}
			aggregateItem.setOrdQty(normalizeNonNegativeNumber(aggregateItem.getOrdQty()) + selectedItem.cancelQty());
		}
		for (ShopOrderRestoreCartItemVO stockItem : stockItemMap.values()) {
			if (stockItem == null || normalizeNonNegativeNumber(stockItem.getOrdQty()) < 1) {
				continue;
			}
			orderMapper.restoreShopGoodsSizeStock(stockItem.getGoodsId(), stockItem.getSizeId(), stockItem.getOrdQty(), auditNo);
		}
	}

	// 주문취소 대상 금액만큼 사용 포인트를 복구합니다.
	private void restoreShopOrderPointByAmount(Long custNo, String ordNo, long restoreAmt) {
		// 복구할 포인트가 없으면 처리하지 않습니다.
		int remainingRestoreAmt = (int) Math.max(restoreAmt, 0L);
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
			orderMapper.restoreShopCustomerPointUseAmt(pointDetail.getPntNo(), appliedRestoreAmt, custNo);
			ShopOrderPointDetailSavePO restoreDetail = new ShopOrderPointDetailSavePO();
			restoreDetail.setPntNo(pointDetail.getPntNo());
			restoreDetail.setPntAmt(appliedRestoreAmt);
			restoreDetail.setOrdNo(ordNo);
			restoreDetail.setBigo(SHOP_ORDER_POINT_RESTORE_MEMO);
			restoreDetail.setRegNo(custNo);
			orderMapper.insertShopOrderPointDetail(restoreDetail);
			remainingRestoreAmt -= appliedRestoreAmt;
		}
		if (remainingRestoreAmt > 0) {
			throw new IllegalStateException("포인트 복구 처리에 실패했습니다.");
		}
	}

	// 주문취소용 PG 취소 API를 호출하고 성공 응답을 해석합니다.
	private ShopOrderCancelPgResult cancelShopOrderPaymentWithPg(
		ShopOrderPaymentVO originalPayment,
		ShopOrderCancelOrderBaseVO orderBase,
		ShopOrderCancelPO param,
		ShopOrderCancelComputation cancelComputation
	) {
		// Toss 결제키가 없으면 PG 취소를 진행할 수 없습니다.
		if (originalPayment == null || isBlank(originalPayment.getTossPaymentKey())) {
			throw new IllegalArgumentException("취소 가능한 결제 정보를 찾을 수 없습니다.");
		}

		// 무통장입금 취소는 금액 없이, 일반 결제취소는 현금 환불액 기준으로 PG를 호출합니다.
		Long cancelAmount =
			SHOP_ORDER_PAY_STAT_WAITING_DEPOSIT.equals(originalPayment.getPayStatCd())
				? null
				: cancelComputation.refundedCashAmt() > 0L
					? cancelComputation.refundedCashAmt()
					: null;
		TossPaymentRefundReceiveAccount refundReceiveAccount =
			SHOP_ORDER_PAYMENT_METHOD_VIRTUAL_ACCOUNT.equals(originalPayment.getPayMethodCd())
				&& !SHOP_ORDER_PAY_STAT_WAITING_DEPOSIT.equals(originalPayment.getPayStatCd())
				? buildTossPaymentRefundReceiveAccount(orderBase)
				: null;
		String rawResponse = refundReceiveAccount == null
			? tossPaymentsClient.cancelPayment(
				originalPayment.getTossPaymentKey().trim(),
				resolveShopOrderCancelPgReason(param),
				cancelAmount
			)
			: tossPaymentsClient.cancelPayment(
				originalPayment.getTossPaymentKey().trim(),
				resolveShopOrderCancelPgReason(param),
				cancelAmount,
				refundReceiveAccount
			);
		JsonNode responseNode = readShopOrderJsonNode(rawResponse);
		String paymentStatus = firstNonBlank(resolveJsonText(responseNode, "status"), "");
		if (!"CANCELED".equals(paymentStatus) && !"PARTIAL_CANCELED".equals(paymentStatus)) {
			throw new IllegalArgumentException("주문취소 처리에 실패했습니다.");
		}

		// Toss 취소 응답에서 거래키와 취소일시와 실제 취소금액을 추출합니다.
		JsonNode cancelNode = responseNode.path("cancels").isArray() && !responseNode.path("cancels").isEmpty()
			? responseNode.path("cancels").get(responseNode.path("cancels").size() - 1)
			: responseNode;
		long canceledAmount = resolveJsonLong(cancelNode, "cancelAmount");
		if (canceledAmount < 1L && cancelAmount != null) {
			canceledAmount = cancelAmount;
		}
		String approvedDt = normalizeShopOrderDateTime(
			firstNonBlank(
				resolveJsonText(cancelNode, "canceledAt"),
				resolveJsonText(responseNode, "approvedAt")
			)
		);
		String tradeNo = firstNonBlank(
			resolveJsonText(cancelNode, "transactionKey"),
			firstNonBlank(resolveJsonText(responseNode, "lastTransactionKey"), originalPayment.getTradeNo())
		);
		return new ShopOrderCancelPgResult(
			rawResponse,
			paymentStatus,
			"취소 완료",
			tradeNo,
			approvedDt,
			canceledAmount
		);
	}

	// 무통장입금 결제 취소 시 Toss 환불 수취 계좌 파라미터를 구성합니다.
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

	// 주문취소 PG 실패 시 환불 PAYMENT row만 실패 상태로 남깁니다.
	private void handleShopOrderCancelPaymentFailure(Long refundPayNo, TossPaymentClientException exception, Long custNo) {
		// 환불 결제번호가 없으면 별도 실패 반영을 진행하지 않습니다.
		if (refundPayNo == null || refundPayNo < 1L) {
			return;
		}

		// Toss 오류 응답을 저장해 실패 원인을 확인할 수 있게 합니다.
		executeInNewShopOrderTransaction(() -> {
			JsonNode errorNode = readShopOrderJsonNode(exception.getResponseBody());
			orderMapper.updateShopPaymentCancelFailure(
				refundPayNo,
				SHOP_ORDER_PAY_STAT_FAIL,
				firstNonBlank(resolveJsonText(errorNode, "code"), "TOSS_CANCEL_ERROR"),
				firstNonBlank(resolveJsonText(errorNode, "message"), "주문취소 처리에 실패했습니다."),
				exception.getResponseBody(),
				custNo
			);
			return null;
		});
	}

	// 주문취소 PG 오류 응답에서 사용자 표시 메시지를 추출합니다.
	private String resolveShopOrderCancelPgErrorMessage(TossPaymentClientException exception) {
		// Toss 오류 메시지가 있으면 우선 사용하고 없으면 기본 문구를 반환합니다.
		JsonNode errorNode = readShopOrderJsonNode(exception == null ? null : exception.getResponseBody());
		return firstNonBlank(resolveJsonText(errorNode, "message"), "주문취소 처리에 실패했습니다.");
	}

	// PG 취소 사유 문자열을 Toss 전송용 텍스트로 구성합니다.
	private String resolveShopOrderCancelPgReason(ShopOrderCancelPO param) {
		// 상품별 사유가 1종류면 그대로 사용하고 여러 종류면 공통 문구로 축약합니다.
		List<ShopOrderCancelItemPO> cancelItemList = param == null ? null : param.getCancelItemList();
		if (cancelItemList == null || cancelItemList.isEmpty()) {
			return "주문 취소";
		}

		Set<String> reasonTextSet = new LinkedHashSet<>();
		for (ShopOrderCancelItemPO cancelItem : cancelItemList) {
			String reasonCd = trimToNull(cancelItem == null ? null : cancelItem.getReasonCd());
			String reasonDetail = trimToNull(cancelItem == null ? null : cancelItem.getReasonDetail());
			if (reasonCd == null) {
				continue;
			}
			reasonTextSet.add(reasonDetail == null ? reasonCd : reasonCd + " - " + reasonDetail);
		}
		if (reasonTextSet.isEmpty()) {
			return "주문 취소";
		}
		return reasonTextSet.size() == 1 ? reasonTextSet.iterator().next() : "주문 취소";
	}

	// 주문변경 마스터 저장 파라미터를 생성합니다.
	private ShopOrderChangeBaseSavePO buildShopOrderChangeBaseSavePO(
		String clmNo,
		ShopOrderCancelOrderBaseVO orderBase,
		ShopOrderCancelComputation cancelComputation,
		String cancelDt,
		Long auditNo
	) {
		// 취소 즉시완료 기준 변경구분과 상태와 배송비 조정 금액을 채웁니다.
		ShopOrderChangeBaseSavePO result = new ShopOrderChangeBaseSavePO();
		result.setClmNo(clmNo);
		result.setOrdNo(orderBase == null ? null : orderBase.getOrdNo());
		result.setChgGbCd(SHOP_ORDER_CHANGE_GB_CANCEL);
		result.setChgDt(cancelDt);
		result.setChgCompleteDt(cancelDt);
		result.setChgStatCd(SHOP_ORDER_CHANGE_STAT_PROGRESS);
		result.setPayDelvAmt((int) Math.max(Math.min(cancelComputation == null ? 0L : cancelComputation.shippingAdjustmentAmt(), Integer.MAX_VALUE), Integer.MIN_VALUE));
		result.setRegNo(auditNo);
		result.setUdtNo(auditNo);
		return result;
	}

	// 주문변경 상세 저장 파라미터를 생성합니다.
	private ShopOrderChangeDetailSavePO buildShopOrderChangeDetailSavePO(
		String clmNo,
		ShopOrderCancelSelectedItem selectedItem,
		ShopOrderCancelPO param,
		Long auditNo
	) {
		// 선택한 주문상품 기준 취소완료 이력 한 건을 구성합니다.
		ShopOrderCancelItemPO requestCancelItem = resolveShopOrderCancelItem(
			param,
			selectedItem == null || selectedItem.detailItem() == null ? null : selectedItem.detailItem().getOrdDtlNo()
		);
		ShopOrderChangeDetailSavePO result = new ShopOrderChangeDetailSavePO();
		result.setClmNo(clmNo);
		result.setOrdNo(selectedItem == null || selectedItem.detailItem() == null ? null : selectedItem.detailItem().getOrdNo());
		result.setOrdDtlNo(selectedItem == null || selectedItem.detailItem() == null ? null : selectedItem.detailItem().getOrdDtlNo());
		result.setChgDtlGbCd(SHOP_ORDER_CHANGE_DTL_GB_CANCEL);
		result.setChgDtlStatCd(SHOP_ORDER_CHANGE_DTL_STAT_DONE);
		result.setChgReasonCd(trimToNull(requestCancelItem == null ? null : requestCancelItem.getReasonCd()));
		result.setChgReasonDtl(trimToNull(requestCancelItem == null ? null : requestCancelItem.getReasonDetail()));
		result.setGoodsId(selectedItem == null || selectedItem.detailItem() == null ? null : selectedItem.detailItem().getGoodsId());
		result.setSizeId(selectedItem == null || selectedItem.detailItem() == null ? null : selectedItem.detailItem().getSizeId());
		result.setQty(selectedItem == null ? null : selectedItem.cancelQty());
		result.setAddAmt(selectedItem == null || selectedItem.detailItem() == null ? null : normalizeNonNegativeNumber(selectedItem.detailItem().getAddAmt()));
		result.setChangeOrdDtlNo(null);
		result.setRegNo(auditNo);
		result.setUdtNo(auditNo);

		// 취소 수량에 비례한 쿠폰과 포인트 배분 금액을 계산하여 설정합니다.
		ShopMypageOrderDetailItemVO detailItem = selectedItem == null ? null : selectedItem.detailItem();
		if (detailItem != null && selectedItem.cancelQty() > 0) {
			int remainingQty = resolveShopOrderRemainingQty(detailItem);
			int cancelQty = selectedItem.cancelQty();
			result.setGoodsCpnDcAmt((int) resolveShopOrderIncrementAllocatedAmt(detailItem.getGoodsCouponDiscountAmt(), remainingQty, 0, cancelQty));
			result.setCartCpnDcAmt((int) resolveShopOrderIncrementAllocatedAmt(detailItem.getCartCouponDiscountAmt(), remainingQty, 0, cancelQty));
			result.setPointDcAmt((int) resolveShopOrderIncrementAllocatedAmt(detailItem.getPointUseAmt(), remainingQty, 0, cancelQty));
		}
		return result;
	}

	// 주문상세번호 기준으로 주문취소 요청 상품 1건을 반환합니다.
	private ShopOrderCancelItemPO resolveShopOrderCancelItem(ShopOrderCancelPO param, Integer ordDtlNo) {
		// 요청 상품 목록에서 동일한 주문상세번호 1건을 찾아 반환합니다.
		if (param == null || ordDtlNo == null || ordDtlNo < 1 || param.getCancelItemList() == null) {
			return null;
		}
		for (ShopOrderCancelItemPO cancelItem : param.getCancelItemList()) {
			if (cancelItem != null && ordDtlNo.equals(cancelItem.getOrdDtlNo())) {
				return cancelItem;
			}
		}
		return null;
	}

	// 주문상세 1건과 취소수량으로 취소 반영 대상 행 정보를 생성합니다.
	private ShopOrderCancelSelectedItem buildShopOrderCancelSelectedItem(ShopMypageOrderDetailItemVO detailItem, int cancelQty) {
		// 취소 후 남은 수량에 따라 다음 주문상세 상태를 계산합니다.
		int remainingAfterCancelQty = Math.max(resolveShopOrderRemainingQty(detailItem) - Math.max(cancelQty, 0), 0);
		String nextOrdDtlStatCd = remainingAfterCancelQty < 1 ? SHOP_ORDER_DTL_STAT_CANCEL : detailItem.getOrdDtlStatCd();
		return new ShopOrderCancelSelectedItem(detailItem, cancelQty, remainingAfterCancelQty, nextOrdDtlStatCd);
	}

	// JSON 노드의 숫자 필드를 long으로 안전하게 읽습니다.
	private long resolveJsonLong(JsonNode node, String fieldName) {
		// 숫자 필드가 없거나 숫자로 해석되지 않으면 0을 반환합니다.
		String rawValue = resolveJsonText(node, fieldName);
		if (isBlank(rawValue)) {
			return 0L;
		}
		try {
			return Math.max(Long.parseLong(rawValue.trim()), 0L);
		} catch (NumberFormatException exception) {
			return 0L;
		}
	}

	// 접두사와 고객번호와 년월일시분초밀리초를 조합해 클레임번호를 생성합니다.
	private String generateShopOrderClaimNo(Long custNo) {
		// 클레임번호는 주문번호와 구분되도록 C 접두사를 사용합니다.
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
		long safeCustNo = custNo == null ? 0L : Math.max(custNo, 0L);
		return "C" + safeCustNo + timestamp;
	}

	// 환불 PAYMENT 저장금액을 음수 기준으로 변환합니다.
	private long resolveRefundPaymentAmt(Long refundedCashAmt) {
		// 환불 저장금액은 절댓값 기준으로 음수화합니다.
		long normalizedRefundedCashAmt = Math.abs(refundedCashAmt == null ? 0L : refundedCashAmt.longValue());
		return normalizedRefundedCashAmt * -1L;
	}

	// Integer 값을 0 이상의 안전한 숫자로 보정합니다.
	private int normalizeNonNegativeNumber(Integer value) {
		return orderService.normalizeNonNegativeNumber(value);
	}

	// Long 값을 0 이상의 안전한 숫자로 보정합니다.
	private long resolveNonNegativeLong(Long value) {
		return orderService.resolveNonNegativeLong(value);
	}

	// 주문상세의 현재 남은 수량을 반환합니다.
	private int resolveShopOrderRemainingQty(ShopMypageOrderDetailItemVO detailItem) {
		return orderService.resolveShopOrderRemainingQty(detailItem);
	}

	// 주문상세의 상품 판매가 단가를 반환합니다.
	private int resolveShopOrderUnitOrderAmt(ShopMypageOrderDetailItemVO detailItem) {
		return orderService.resolveShopOrderUnitOrderAmt(detailItem);
	}

	// 주문상세의 이번 취소분 배분 금액을 계산합니다.
	private long resolveShopOrderIncrementAllocatedAmt(Integer allocatedAmt, int originalQty, int canceledBeforeQty, int cancelQty) {
		return orderService.resolveShopOrderIncrementAllocatedAmt(allocatedAmt, originalQty, canceledBeforeQty, cancelQty);
	}

	// JSON 문자열을 JsonNode로 변환합니다.
	private JsonNode readShopOrderJsonNode(String rawJson) {
		return orderService.readShopOrderJsonNode(rawJson);
	}

	// 객체를 JSON 문자열로 직렬화합니다.
	private String writeShopOrderJson(Object value) {
		return orderService.writeShopOrderJson(value);
	}

	// JSON 노드에서 지정한 필드 문자열 값을 안전하게 조회합니다.
	private String resolveJsonText(JsonNode node, String fieldName) {
		return orderService.resolveJsonText(node, fieldName);
	}

	// 다양한 날짜 문자열을 주문 결제 저장용 형식으로 정규화합니다.
	private String normalizeShopOrderDateTime(String value) {
		return orderService.normalizeShopOrderDateTime(value);
	}

}
