package com.xodud1202.springbackend.service;

import static com.xodud1202.springbackend.common.Constants.Common.NO;
import static com.xodud1202.springbackend.common.Constants.Shop.*;
import static com.xodud1202.springbackend.common.util.CommonTextUtils.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.xodud1202.springbackend.domain.common.CommonCodeVO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartSiteInfoVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsSizeItemVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderCancelReasonVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderDetailItemVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderExchangePageVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderExchangeSizeOptionVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderGroupVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderAddressVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderCancelOrderBaseVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderChangeBaseSavePO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderChangeDetailSavePO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderChangeExchangeAddressSavePO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderCustomerInfoVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderExchangeAddressPO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderExchangeItemPO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderExchangePO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderExchangePaymentClaimVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderExchangePaymentConfirmPO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderExchangePaymentConfirmVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderExchangePaymentFailPO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderExchangeResultVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderPaymentPrepareVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderPaymentSavePO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderPaymentVO;
import com.xodud1202.springbackend.domain.shop.site.ShopSiteInfoVO;
import com.xodud1202.springbackend.mapper.CommonMapper;
import com.xodud1202.springbackend.mapper.GoodsMapper;
import com.xodud1202.springbackend.mapper.OrderMapper;
import com.xodud1202.springbackend.mapper.SiteInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
// 주문교환 도메인 비즈니스 로직을 제공합니다.
public class OrderExchangeService {
	private static final int ORDER_CHANGE_ADDRESS_NAME_MAX_LENGTH = 20;
	private static final int ORDER_CHANGE_ADDRESS_POST_NO_MAX_LENGTH = 10;
	private static final int ORDER_CHANGE_ADDRESS_BASE_MAX_LENGTH = 100;
	private static final int ORDER_CHANGE_ADDRESS_DETAIL_MAX_LENGTH = 100;
	private static final String CUSTOMER_FAULT_REASON_PREFIX = "E_1";
	private static final String COMPANY_FAULT_REASON_PREFIX = "E_2";
	private static final String EXCHANGE_ORDER_NAME = "교환 배송비";

	private final OrderService orderService;
	private final OrderMapper orderMapper;
	private final GoodsMapper goodsMapper;
	private final CommonMapper commonMapper;
	private final SiteInfoMapper siteInfoMapper;
	private final TossPaymentsClient tossPaymentsClient;

	// 교환 신청 선택 상품 계산 결과를 전달합니다.
	private record ShopOrderExchangeSelectedItem(
		ShopMypageOrderDetailItemVO detailItem,
		ShopOrderExchangeItemPO requestItem,
		ShopGoodsSizeItemVO targetSize,
		int exchangeQty,
		String reasonCd,
		String reasonDetail
	) {
	}

	// 교환 신청 계산 결과를 전달합니다.
	private record ShopOrderExchangeComputation(
		List<ShopOrderExchangeSelectedItem> selectedItemList,
		boolean companyFaultYn,
		long payDelvAmt,
		String initialChgDtlStatCd
	) {
	}

	// 쇼핑몰 마이페이지 교환 신청 화면 데이터를 조회합니다.
	public ShopMypageOrderExchangePageVO getShopMypageOrderExchangePage(Long custNo, String ordNo, Integer ordDtlNo) {
		// 고객번호와 주문번호를 검증합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}
		String resolvedOrdNo = trimToNull(ordNo);
		if (resolvedOrdNo == null) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_NO_INVALID_MESSAGE);
		}

		// 현재 로그인 고객의 주문과 교환 화면 기준 데이터를 조회합니다.
		ShopMypageOrderGroupVO orderGroup = orderService.getShopMypageOrderGroupWithDetail(custNo, resolvedOrdNo);
		validateShopMypageOrderExchangeAccess(orderGroup, ordDtlNo);
		ShopOrderCancelOrderBaseVO orderBase = orderService.resolveShopOrderCancelOrderBase(custNo, resolvedOrdNo);
		List<ShopMypageOrderCancelReasonVO> reasonList = orderService.normalizeShopMypageOrderCancelReasonList(
			orderMapper.getShopMypageOrderExchangeReasonList()
		);
		ShopCartSiteInfoVO siteInfo = orderService.resolveShopCartSiteInfo();
		List<ShopOrderAddressVO> addressList = orderService.resolveShopOrderAddressList(custNo);
		ShopOrderCustomerInfoVO customerInfo = orderService.resolveShopOrderCustomerInfo(custNo, null);
		ShopOrderAddressVO orderAddress = orderService.createShopOrderPickupAddress(custNo, orderBase);

		// 교환 신청 화면 응답 객체를 구성합니다.
		ShopMypageOrderExchangePageVO result = new ShopMypageOrderExchangePageVO();
		result.setOrder(orderGroup);
		result.setReasonList(reasonList);
		result.setSiteInfo(siteInfo);
		result.setSizeOptionList(buildShopMypageOrderExchangeSizeOptionList(orderGroup));
		result.setAddressList(addressList);
		result.setPickupAddress(orderAddress);
		result.setDeliveryAddress(orderAddress);
		result.setCustomerPhoneNumber(safeValue(firstNonBlank(trimToNull(customerInfo == null ? null : customerInfo.getPhoneNumber()), "")));
		return result;
	}

	// 쇼핑몰 마이페이지 교환 신청을 저장합니다.
	@Transactional
	public ShopOrderExchangeResultVO exchangeShopMypageOrder(
		ShopOrderExchangePO param,
		Long custNo,
		String deviceGbCd,
		String shopOrigin
	) {
		// 로그인 고객번호와 요청 본문을 먼저 검증합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}
		if (param == null) {
			throw new IllegalArgumentException("교환 정보를 확인해주세요.");
		}
		String ordNo = trimToNull(param.getOrdNo());
		if (ordNo == null) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_NO_INVALID_MESSAGE);
		}

		// 현재 주문과 교환 계산에 필요한 기준 정보를 조회합니다.
		ShopMypageOrderGroupVO orderGroup = orderService.getShopMypageOrderGroupWithDetail(custNo, ordNo);
		ShopOrderCancelOrderBaseVO orderBase = orderService.resolveShopOrderCancelOrderBase(custNo, ordNo);
		ShopCartSiteInfoVO siteInfo = orderService.resolveShopCartSiteInfo();
		List<ShopMypageOrderCancelReasonVO> reasonList = orderService.normalizeShopMypageOrderCancelReasonList(
			orderMapper.getShopMypageOrderExchangeReasonList()
		);

		// 요청 주소와 선택 상품을 서버 기준으로 다시 검증합니다.
		ShopOrderExchangeAddressPO pickupAddress = normalizeShopOrderExchangeAddress(param.getPickupAddress(), "회수지 정보를 확인해주세요.");
		ShopOrderExchangeAddressPO deliveryAddress = normalizeShopOrderExchangeAddress(param.getDeliveryAddress(), "교환 배송지 정보를 확인해주세요.");
		Map<Integer, ShopOrderExchangeItemPO> exchangeItemMap = resolveShopOrderExchangeItemMap(param.getExchangeItemList());
		ShopOrderExchangeComputation computation = buildShopOrderExchangeComputation(orderGroup, siteInfo, reasonList, exchangeItemMap);

		// 교환 클레임 마스터/상세/주소를 저장합니다.
		String clmNo = generateShopOrderClaimNo(custNo);
		String exchangeDt = LocalDateTime.now().format(SHOP_MYPAGE_ORDER_DATE_TIME_FORMATTER);
		orderMapper.insertShopOrderChangeBase(buildShopOrderExchangeBaseSavePO(clmNo, orderBase, computation, exchangeDt, custNo));
		for (ShopOrderExchangeSelectedItem selectedItem : computation.selectedItemList()) {
			orderMapper.insertShopOrderChangeDetail(buildShopOrderExchangePickupDetailSavePO(clmNo, selectedItem, computation.initialChgDtlStatCd(), custNo));
			orderMapper.insertShopOrderChangeDetail(buildShopOrderExchangeDeliveryDetailSavePO(clmNo, selectedItem, custNo));
		}
		insertShopOrderExchangeAddressList(clmNo, pickupAddress, deliveryAddress, custNo);

		// 교환 배송비가 있으면 결제 준비 정보를 함께 생성합니다.
		ShopOrderPaymentPrepareVO paymentPrepare = null;
		if (computation.payDelvAmt() > 0L) {
			String paymentMethodCd = orderService.resolveRequiredShopOrderPaymentMethodCd(param.getPaymentMethodCd());
			paymentPrepare = prepareShopOrderExchangePayment(
				clmNo,
				ordNo,
				computation.payDelvAmt(),
				paymentMethodCd,
				custNo,
				deviceGbCd,
				shopOrigin
			);
		}

		// 교환 신청 결과를 응답 객체에 담아 반환합니다.
		ShopOrderExchangeResultVO result = new ShopOrderExchangeResultVO();
		result.setClmNo(clmNo);
		result.setOrdNo(ordNo);
		result.setPaymentRequiredYn(computation.payDelvAmt() > 0L);
		result.setPayDelvAmt(computation.payDelvAmt());
		result.setPaymentPrepare(paymentPrepare);
		return result;
	}

	// 쇼핑몰 마이페이지 교환 배송비 결제를 승인합니다.
	@Transactional
	public ShopOrderExchangePaymentConfirmVO confirmShopMypageOrderExchangePayment(
		ShopOrderExchangePaymentConfirmPO param,
		Long custNo
	) {
		// 로그인 고객번호와 승인 요청값을 검증합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}
		if (param == null || param.getPayNo() == null || isBlank(param.getClmNo()) || isBlank(param.getPaymentKey()) || param.getAmount() == null) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_EXCHANGE_PAYMENT_INVALID_MESSAGE);
		}

		// 결제번호와 클레임번호 기준으로 교환 배송비 결제 대상을 검증합니다.
		String clmNo = param.getClmNo().trim();
		ShopOrderPaymentVO payment = validateShopOrderExchangePayment(param.getPayNo(), clmNo, custNo, param.getAmount());
		ShopOrderExchangePaymentClaimVO claim = validateShopOrderExchangePaymentClaim(clmNo, custNo, true);
		if (SHOP_ORDER_PAY_STAT_DONE.equals(payment.getPayStatCd()) || SHOP_ORDER_PAY_STAT_WAITING_DEPOSIT.equals(payment.getPayStatCd())) {
			return buildShopOrderExchangePaymentConfirmResult(payment, claim);
		}

		// Toss 승인 API를 호출하고 실패하면 결제 실패와 교환 철회를 함께 반영합니다.
		String rawResponse;
		try {
			rawResponse = tossPaymentsClient.confirmPayment(param.getPaymentKey().trim(), clmNo, param.getAmount());
		} catch (TossPaymentClientException exception) {
			JsonNode errorNode = orderService.readShopOrderJsonNode(exception.getResponseBody());
			String errorCode = firstNonBlank(orderService.resolveJsonText(errorNode, "code"), "TOSS_CONFIRM_ERROR");
			String errorMessage = firstNonBlank(orderService.resolveJsonText(errorNode, "message"), SHOP_ORDER_PAYMENT_CONFIRM_MESSAGE);
			orderMapper.updateShopPaymentFailure(payment.getPayNo(), SHOP_ORDER_PAY_STAT_FAIL, errorCode, errorMessage, exception.getResponseBody(), custNo);
			withdrawShopOrderExchangeClaim(clmNo, custNo);
			throw new IllegalArgumentException(errorMessage);
		}

		// Toss 응답 상태에 따라 가상계좌 발급과 결제 완료를 나눠 저장합니다.
		JsonNode responseNode = orderService.readShopOrderJsonNode(rawResponse);
		String paymentStatus = orderService.resolveJsonText(responseNode, "status");
		String paymentKey = orderService.resolveJsonText(responseNode, "paymentKey");
		String paymentKeyHash = orderService.sha256Hex(paymentKey);
		String tradeNo = firstNonBlank(orderService.resolveJsonText(responseNode, "lastTransactionKey"), paymentKey);
		String approvedAt = orderService.normalizeShopOrderDateTime(orderService.resolveJsonText(responseNode, "approvedAt"));
		if (SHOP_ORDER_PAYMENT_METHOD_VIRTUAL_ACCOUNT.equals(payment.getPayMethodCd()) && "WAITING_FOR_DEPOSIT".equals(paymentStatus)) {
			String dueDate = orderService.normalizeShopOrderDateTime(orderService.resolveJsonText(responseNode.path("virtualAccount"), "dueDate"));
			orderMapper.updateShopPaymentWaitingDeposit(
				payment.getPayNo(),
				SHOP_ORDER_PAY_STAT_WAITING_DEPOSIT,
				param.getAmount(),
				paymentKey,
				paymentKeyHash,
				tradeNo,
				paymentStatus,
				"교환 배송비 무통장입금 발급 완료",
				orderService.resolveJsonText(responseNode.path("virtualAccount"), "bankCode"),
				orderService.resolveJsonText(responseNode.path("virtualAccount"), "accountNumber"),
				orderService.resolveJsonText(responseNode.path("virtualAccount"), "customerName"),
				dueDate,
				rawResponse,
				approvedAt,
				custNo
			);
			return buildShopOrderExchangePaymentConfirmResult(orderMapper.getShopPaymentByPayNo(payment.getPayNo()), claim);
		}
		if (!"DONE".equals(paymentStatus)) {
			throw new IllegalArgumentException(SHOP_ORDER_PAYMENT_CONFIRM_MESSAGE);
		}

		// 카드/퀵계좌이체 결제 완료 정보를 저장하고 교환 신청 상태로 전환합니다.
		orderMapper.updateShopPaymentSuccess(
			payment.getPayNo(),
			SHOP_ORDER_PAY_STAT_DONE,
			param.getAmount(),
			paymentKey,
			paymentKeyHash,
			tradeNo,
			orderService.resolveJsonText(responseNode.path("card"), "approveNo"),
			paymentStatus,
			"교환 배송비 결제 완료",
			firstNonBlank(orderService.resolveJsonText(responseNode.path("card"), "issuerCode"), orderService.resolveJsonText(responseNode.path("easyPay"), "provider")),
			orderService.resolveJsonText(responseNode.path("card"), "number"),
			rawResponse,
			approvedAt,
			custNo
		);
		applyShopOrderExchangeClaimPaymentComplete(clmNo, custNo);
		return buildShopOrderExchangePaymentConfirmResult(
			orderMapper.getShopPaymentByPayNo(payment.getPayNo()),
			validateShopOrderExchangePaymentClaim(clmNo, custNo, false)
		);
	}

	// 쇼핑몰 마이페이지 교환 배송비 결제 실패/취소를 반영합니다.
	@Transactional
	public void failShopMypageOrderExchangePayment(ShopOrderExchangePaymentFailPO param, Long custNo) {
		// 로그인 고객번호와 실패 요청값을 검증합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}
		if (param == null || param.getPayNo() == null || isBlank(param.getClmNo())) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_EXCHANGE_PAYMENT_INVALID_MESSAGE);
		}

		// 결제 row가 현재 고객의 교환 배송비 결제인지 확인합니다.
		String clmNo = param.getClmNo().trim();
		ShopOrderPaymentVO payment = validateShopOrderExchangePayment(param.getPayNo(), clmNo, custNo, null);
		if (SHOP_ORDER_PAY_STAT_DONE.equals(payment.getPayStatCd()) || SHOP_ORDER_PAY_STAT_WAITING_DEPOSIT.equals(payment.getPayStatCd())) {
			return;
		}

		// 결제 실패/취소 상태를 저장하고 교환 클레임을 자동 철회합니다.
		String payStatCd = isShopOrderCancelFailureCode(param.getCode()) ? SHOP_ORDER_PAY_STAT_CANCEL : SHOP_ORDER_PAY_STAT_FAIL;
		orderMapper.updateShopPaymentFailure(
			payment.getPayNo(),
			payStatCd,
			trimToNull(param.getCode()),
			trimToNull(param.getMessage()),
			buildShopOrderExchangePaymentFailureRawJson(param),
			custNo
		);
		withdrawShopOrderExchangeClaim(clmNo, custNo);
	}

	// 교환 신청 화면 진입 시 주문상세번호와 교환 가능 상태를 검증합니다.
	private void validateShopMypageOrderExchangeAccess(ShopMypageOrderGroupVO orderGroup, Integer ordDtlNo) {
		// 주문상세 목록이 없으면 교환 불가 예외를 반환합니다.
		if (orderGroup == null || orderGroup.getDetailList() == null || orderGroup.getDetailList().isEmpty()) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_EXCHANGE_UNAVAILABLE_MESSAGE);
		}

		// 교환 가능 상품 존재 여부와 요청 주문상세번호 유효성을 함께 확인합니다.
		boolean hasExchangeApplyableDetail = false;
		boolean matchedRequestedDetail = ordDtlNo == null;
		for (ShopMypageOrderDetailItemVO detailItem : orderGroup.getDetailList()) {
			if (detailItem == null) {
				continue;
			}
			boolean exchangeApplyable = isShopOrderExchangeApplyable(detailItem);
			if (exchangeApplyable) {
				hasExchangeApplyableDetail = true;
			}
			if (ordDtlNo != null && ordDtlNo.equals(detailItem.getOrdDtlNo())) {
				if (!exchangeApplyable) {
					throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_DETAIL_ITEM_INVALID_MESSAGE);
				}
				matchedRequestedDetail = true;
			}
		}
		if (!hasExchangeApplyableDetail) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_EXCHANGE_UNAVAILABLE_MESSAGE);
		}
		if (!matchedRequestedDetail) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_DETAIL_ITEM_INVALID_MESSAGE);
		}
	}

	// 주문상세 행이 교환신청 가능한 상태인지 반환합니다.
	private boolean isShopOrderExchangeApplyable(ShopMypageOrderDetailItemVO detailItem) {
		// 배송완료 상태, 잔여 수량, 진행중 클레임 차단 여부를 함께 확인합니다.
		if (detailItem == null) {
			return false;
		}
		return SHOP_ORDER_DTL_STAT_DELIVERY_COMPLETE.equals(detailItem.getOrdDtlStatCd())
			&& normalizeNonNegativeNumber(detailItem.getCancelableQty()) > 0
			&& Boolean.TRUE.equals(detailItem.getExchangeApplyableYn())
			&& !Boolean.TRUE.equals(detailItem.getActiveReturnClaimYn())
			&& !Boolean.TRUE.equals(detailItem.getActiveExchangeClaimYn());
	}

	// 교환 신청 화면의 상품별 사이즈 옵션 목록을 구성합니다.
	private List<ShopMypageOrderExchangeSizeOptionVO> buildShopMypageOrderExchangeSizeOptionList(ShopMypageOrderGroupVO orderGroup) {
		// 교환 가능 상품 기준으로 상품 사이즈 옵션을 주문상세번호와 연결합니다.
		List<ShopMypageOrderExchangeSizeOptionVO> result = new ArrayList<>();
		for (ShopMypageOrderDetailItemVO detailItem : orderGroup == null ? List.<ShopMypageOrderDetailItemVO>of() : orderGroup.getDetailList()) {
			if (!isShopOrderExchangeApplyable(detailItem) || isBlank(detailItem.getGoodsId())) {
				continue;
			}
			for (ShopGoodsSizeItemVO sizeItem : goodsMapper.getShopGoodsSizeList(detailItem.getGoodsId().trim())) {
				if (sizeItem == null || isBlank(sizeItem.getSizeId())) {
					continue;
				}
				ShopMypageOrderExchangeSizeOptionVO option = new ShopMypageOrderExchangeSizeOptionVO();
				option.setOrdDtlNo(detailItem.getOrdDtlNo());
				option.setGoodsId(detailItem.getGoodsId());
				option.setSizeId(sizeItem.getSizeId());
				option.setStockQty(normalizeNonNegativeNumber(sizeItem.getStockQty()));
				option.setAddAmt(normalizeNonNegativeNumber(sizeItem.getAddAmt()));
				option.setDispOrd(normalizeNonNegativeNumber(sizeItem.getDispOrd()));
				option.setSoldOut(normalizeNonNegativeNumber(sizeItem.getStockQty()) < 1);
				result.add(option);
			}
		}
		return result;
	}

	// 교환 요청 상품 목록을 주문상세번호 기준 맵으로 정규화합니다.
	private Map<Integer, ShopOrderExchangeItemPO> resolveShopOrderExchangeItemMap(List<ShopOrderExchangeItemPO> exchangeItemList) {
		// 선택 상품이 없으면 교환 신청을 진행하지 않습니다.
		if (exchangeItemList == null || exchangeItemList.isEmpty()) {
			throw new IllegalArgumentException("교환할 상품을 선택해주세요.");
		}

		// 주문상세번호 중복과 수량/사이즈 기본값을 함께 검증합니다.
		Map<Integer, ShopOrderExchangeItemPO> result = new LinkedHashMap<>();
		for (ShopOrderExchangeItemPO exchangeItem : exchangeItemList) {
			if (exchangeItem == null
				|| exchangeItem.getOrdDtlNo() == null
				|| exchangeItem.getOrdDtlNo() < 1
				|| exchangeItem.getExchangeQty() == null
				|| exchangeItem.getExchangeQty() < 1
				|| trimToNull(exchangeItem.getTargetSizeId()) == null) {
				throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_DETAIL_ITEM_INVALID_MESSAGE);
			}
			if (result.putIfAbsent(exchangeItem.getOrdDtlNo(), exchangeItem) != null) {
				throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_DETAIL_ITEM_INVALID_MESSAGE);
			}
		}
		return result;
	}

	// 교환 신청 기준 배송비와 저장 상세 목록을 계산합니다.
	private ShopOrderExchangeComputation buildShopOrderExchangeComputation(
		ShopMypageOrderGroupVO orderGroup,
		ShopCartSiteInfoVO siteInfo,
		List<ShopMypageOrderCancelReasonVO> reasonList,
		Map<Integer, ShopOrderExchangeItemPO> exchangeItemMap
	) {
		// 현재 주문상세를 순회하며 선택 교환 수량과 사유와 목표 사이즈를 검증합니다.
		boolean hasCompanyFaultReason = false;
		List<ShopOrderExchangeSelectedItem> selectedItemList = new ArrayList<>();
		Map<Integer, ShopOrderExchangeItemPO> unmatchedItemMap = new LinkedHashMap<>(exchangeItemMap);
		for (ShopMypageOrderDetailItemVO detailItem : orderGroup == null ? List.<ShopMypageOrderDetailItemVO>of() : orderGroup.getDetailList()) {
			if (detailItem == null || detailItem.getOrdDtlNo() == null) {
				continue;
			}
			ShopOrderExchangeItemPO requestItem = exchangeItemMap.get(detailItem.getOrdDtlNo());
			if (requestItem == null) {
				continue;
			}
			unmatchedItemMap.remove(detailItem.getOrdDtlNo());
			if (!isShopOrderExchangeApplyable(detailItem)) {
				throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_DETAIL_ITEM_INVALID_MESSAGE);
			}
			int currentRemainingQty = normalizeNonNegativeNumber(detailItem.getCancelableQty());
			int exchangeQty = normalizeNonNegativeNumber(requestItem.getExchangeQty());
			if (exchangeQty < 1 || exchangeQty > currentRemainingQty) {
				throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_DETAIL_ITEM_INVALID_MESSAGE);
			}

			String targetSizeId = trimToNull(requestItem.getTargetSizeId());
			ShopGoodsSizeItemVO targetSize = findShopGoodsSize(detailItem.getGoodsId(), targetSizeId);
			if (targetSize == null || normalizeNonNegativeNumber(targetSize.getStockQty()) < exchangeQty) {
				throw new IllegalArgumentException("교환 희망 사이즈의 재고를 확인해주세요.");
			}

			validateShopOrderExchangeReason(requestItem, reasonList);
			String reasonCd = trimToNull(requestItem.getReasonCd());
			String reasonDetail = trimToNull(requestItem.getReasonDetail());
			hasCompanyFaultReason = hasCompanyFaultReason || (reasonCd != null && reasonCd.startsWith(COMPANY_FAULT_REASON_PREFIX));
			selectedItemList.add(new ShopOrderExchangeSelectedItem(detailItem, requestItem, targetSize, exchangeQty, reasonCd, reasonDetail));
		}

		// 요청 주문상세번호가 실제 주문에 없거나 교환 불가 행이면 진행하지 않습니다.
		if (selectedItemList.isEmpty()) {
			throw new IllegalArgumentException("교환할 상품을 선택해주세요.");
		}
		if (!unmatchedItemMap.isEmpty()) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_DETAIL_ITEM_INVALID_MESSAGE);
		}

		// 회사 귀책이 하나라도 있으면 무료, 전부 고객 귀책이면 왕복 배송비를 결제합니다.
		long deliveryFee = resolveNonNegativeLong(siteInfo == null ? null : (long) normalizeNonNegativeNumber(siteInfo.getDeliveryFee()));
		long payDelvAmt = hasCompanyFaultReason ? 0L : deliveryFee * 2L;
		String initialChgDtlStatCd = payDelvAmt > 0L
			? SHOP_ORDER_CHANGE_DTL_STAT_EXCHANGE_PAYMENT_WAIT
			: SHOP_ORDER_CHANGE_DTL_STAT_EXCHANGE_APPLY;
		return new ShopOrderExchangeComputation(selectedItemList, hasCompanyFaultReason, payDelvAmt, initialChgDtlStatCd);
	}

	// 상품 사이즈 목록에서 요청한 사이즈를 찾습니다.
	private ShopGoodsSizeItemVO findShopGoodsSize(String goodsId, String sizeId) {
		// 상품코드와 사이즈가 비어 있으면 사이즈를 찾지 않습니다.
		String normalizedGoodsId = trimToNull(goodsId);
		String normalizedSizeId = trimToNull(sizeId);
		if (normalizedGoodsId == null || normalizedSizeId == null) {
			return null;
		}
		for (ShopGoodsSizeItemVO sizeItem : goodsMapper.getShopGoodsSizeList(normalizedGoodsId)) {
			if (sizeItem == null || isBlank(sizeItem.getSizeId())) {
				continue;
			}
			if (normalizedSizeId.equals(sizeItem.getSizeId().trim())) {
				return sizeItem;
			}
		}
		return null;
	}

	// 교환 사유 코드와 직접입력값을 검증합니다.
	private void validateShopOrderExchangeReason(ShopOrderExchangeItemPO exchangeItem, List<ShopMypageOrderCancelReasonVO> reasonList) {
		// 선택 상품별 교환 사유 코드가 비어 있거나 현재 사용 가능한 사유가 아니면 예외를 반환합니다.
		String reasonCd = trimToNull(exchangeItem == null ? null : exchangeItem.getReasonCd());
		if (reasonCd == null) {
			throw new IllegalArgumentException("교환 사유를 선택해주세요.");
		}
		if (!reasonCd.startsWith(CUSTOMER_FAULT_REASON_PREFIX) && !reasonCd.startsWith(COMPANY_FAULT_REASON_PREFIX)) {
			throw new IllegalArgumentException("교환 사유를 선택해주세요.");
		}

		ShopMypageOrderCancelReasonVO matchedReason = findShopOrderExchangeReason(reasonList, reasonCd);
		if (matchedReason == null) {
			throw new IllegalArgumentException("교환 사유를 선택해주세요.");
		}
		if (isShopOrderReasonDetailRequired(matchedReason) && trimToNull(exchangeItem.getReasonDetail()) == null) {
			throw new IllegalArgumentException("기타 사유를 입력해주세요.");
		}
	}

	// 교환 사유 목록에서 요청 사유 코드 1건을 찾습니다.
	private ShopMypageOrderCancelReasonVO findShopOrderExchangeReason(List<ShopMypageOrderCancelReasonVO> reasonList, String reasonCd) {
		// 현재 사용 가능한 사유 목록에서 코드가 일치하는 항목을 반환합니다.
		for (ShopMypageOrderCancelReasonVO reasonItem : reasonList == null ? List.<ShopMypageOrderCancelReasonVO>of() : reasonList) {
			if (reasonCd.equals(trimToNull(reasonItem == null ? null : reasonItem.getCd()))) {
				return reasonItem;
			}
		}
		return null;
	}

	// 클레임 사유가 직접입력값을 요구하는지 반환합니다.
	private boolean isShopOrderReasonDetailRequired(ShopMypageOrderCancelReasonVO reasonItem) {
		// 사유명에 기타가 포함되면 직접입력값을 필수로 처리합니다.
		String reasonName = trimToNull(reasonItem == null ? null : reasonItem.getCdNm());
		return reasonName != null && reasonName.contains("기타");
	}

	// 교환 주소 입력값을 정규화하고 길이를 검증합니다.
	private ShopOrderExchangeAddressPO normalizeShopOrderExchangeAddress(ShopOrderExchangeAddressPO address, String invalidMessage) {
		// 주소 정보가 없거나 필수값이 비어 있으면 진행하지 않습니다.
		String receiverName = trimToNull(address == null ? null : address.getRsvNm());
		String postNo = trimToNull(address == null ? null : address.getPostNo());
		String baseAddress = trimToNull(address == null ? null : address.getBaseAddress());
		String detailAddress = trimToNull(address == null ? null : address.getDetailAddress());
		validateShopOrderChangeAddress(invalidMessage, receiverName, postNo, baseAddress, detailAddress);

		// 저장에 사용할 주소 객체를 새로 구성합니다.
		ShopOrderExchangeAddressPO result = new ShopOrderExchangeAddressPO();
		result.setRsvNm(receiverName);
		result.setPostNo(postNo);
		result.setBaseAddress(baseAddress);
		result.setDetailAddress(detailAddress);
		return result;
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
		if (addrName.length() > ORDER_CHANGE_ADDRESS_NAME_MAX_LENGTH
			|| addrPostNo.length() > ORDER_CHANGE_ADDRESS_POST_NO_MAX_LENGTH
			|| addrBase.length() > ORDER_CHANGE_ADDRESS_BASE_MAX_LENGTH
			|| addrDtl.length() > ORDER_CHANGE_ADDRESS_DETAIL_MAX_LENGTH) {
			throw new IllegalArgumentException(invalidMessage);
		}
	}

	// 교환 클레임 주소 목록을 저장합니다.
	private void insertShopOrderExchangeAddressList(
		String clmNo,
		ShopOrderExchangeAddressPO pickupAddress,
		ShopOrderExchangeAddressPO deliveryAddress,
		Long auditNo
	) {
		// 고객 주소와 물류창고 주소를 교환 회수/배송 흐름 기준으로 저장합니다.
		ShopOrderExchangeAddressPO warehouseAddress = resolveShopOrderExchangeWarehouseAddress();
		orderMapper.insertShopOrderChangeExchangeAddress(buildShopOrderExchangeAddressSavePO(clmNo, SHOP_ORDER_CHANGE_ADDR_GB_EXCHANGE_PICKUP, pickupAddress, auditNo));
		orderMapper.insertShopOrderChangeExchangeAddress(buildShopOrderExchangeAddressSavePO(clmNo, SHOP_ORDER_CHANGE_ADDR_GB_EXCHANGE_PICKUP_DESTINATION, warehouseAddress, auditNo));
		orderMapper.insertShopOrderChangeExchangeAddress(buildShopOrderExchangeAddressSavePO(clmNo, SHOP_ORDER_CHANGE_ADDR_GB_EXCHANGE_DELIVERY_ORIGIN, warehouseAddress, auditNo));
		orderMapper.insertShopOrderChangeExchangeAddress(buildShopOrderExchangeAddressSavePO(clmNo, SHOP_ORDER_CHANGE_ADDR_GB_EXCHANGE_DELIVERY_DESTINATION, deliveryAddress, auditNo));
	}

	// 사이트 물류창고 주소를 교환 주소 형식으로 조회합니다.
	private ShopOrderExchangeAddressPO resolveShopOrderExchangeWarehouseAddress() {
		// 사이트 정보에서 창고 주소를 조회하고 필수값을 검증합니다.
		ShopSiteInfoVO siteInfo = siteInfoMapper.getShopSiteInfo(SHOP_SITE_ID);
		String addrName = trimToNull(siteInfo == null ? null : siteInfo.getSiteNm());
		String addrPostNo = trimToNull(siteInfo == null ? null : siteInfo.getWhPostNo());
		String addrBase = trimToNull(siteInfo == null ? null : siteInfo.getWhAddrBase());
		String addrDtl = trimToNull(siteInfo == null ? null : siteInfo.getWhAddrDtl());
		validateShopOrderChangeAddress("교환 물류지 정보를 확인해주세요.", addrName, addrPostNo, addrBase, addrDtl);

		// 저장에 사용할 창고 주소 객체를 구성합니다.
		ShopOrderExchangeAddressPO result = new ShopOrderExchangeAddressPO();
		result.setRsvNm(addrName);
		result.setPostNo(addrPostNo);
		result.setBaseAddress(addrBase);
		result.setDetailAddress(addrDtl);
		return result;
	}

	// 주문변경 주소 저장 파라미터를 생성합니다.
	private ShopOrderChangeExchangeAddressSavePO buildShopOrderExchangeAddressSavePO(
		String clmNo,
		String clmAddrGbCd,
		ShopOrderExchangeAddressPO address,
		Long auditNo
	) {
		// 교환 주소 1건을 주문변경 주소 테이블 형식으로 변환합니다.
		ShopOrderChangeExchangeAddressSavePO result = new ShopOrderChangeExchangeAddressSavePO();
		result.setClmNo(clmNo);
		result.setClmAddrGbCd(clmAddrGbCd);
		result.setAddrName(address.getRsvNm());
		result.setAddrPostNo(address.getPostNo());
		result.setAddrBase(address.getBaseAddress());
		result.setAddrDtl(address.getDetailAddress());
		result.setRegNo(auditNo);
		result.setUdtNo(auditNo);
		return result;
	}

	// 주문변경 마스터 저장 파라미터를 생성합니다.
	private ShopOrderChangeBaseSavePO buildShopOrderExchangeBaseSavePO(
		String clmNo,
		ShopOrderCancelOrderBaseVO orderBase,
		ShopOrderExchangeComputation computation,
		String exchangeDt,
		Long auditNo
	) {
		// 교환 신청 기준 변경구분과 상태와 교환 배송비를 저장합니다.
		ShopOrderChangeBaseSavePO result = new ShopOrderChangeBaseSavePO();
		result.setClmNo(clmNo);
		result.setOrdNo(orderBase == null ? null : orderBase.getOrdNo());
		result.setChgGbCd(SHOP_ORDER_CHANGE_GB_EXCHANGE);
		result.setChgDt(exchangeDt);
		result.setChgCompleteDt(null);
		result.setChgStatCd(SHOP_ORDER_CHANGE_STAT_PROGRESS);
		result.setPayDelvAmt(toSafeInt(computation == null ? 0L : computation.payDelvAmt()));
		result.setRegNo(auditNo);
		result.setUdtNo(auditNo);
		return result;
	}

	// 주문변경 회수 상세 저장 파라미터를 생성합니다.
	private ShopOrderChangeDetailSavePO buildShopOrderExchangePickupDetailSavePO(
		String clmNo,
		ShopOrderExchangeSelectedItem selectedItem,
		String chgDtlStatCd,
		Long auditNo
	) {
		// 선택한 주문상품의 기존 상품/사이즈 기준 회수 이력 한 건을 구성합니다.
		ShopMypageOrderDetailItemVO detailItem = selectedItem == null ? null : selectedItem.detailItem();
		ShopOrderChangeDetailSavePO result = new ShopOrderChangeDetailSavePO();
		result.setClmNo(clmNo);
		result.setOrdNo(detailItem == null ? null : detailItem.getOrdNo());
		result.setOrdDtlNo(detailItem == null ? null : detailItem.getOrdDtlNo());
		result.setChgDtlGbCd(SHOP_ORDER_CHANGE_DTL_GB_EXCHANGE_PICKUP);
		result.setChgDtlStatCd(chgDtlStatCd);
		result.setChgReasonCd(selectedItem == null ? null : selectedItem.reasonCd());
		result.setChgReasonDtl(selectedItem == null ? null : selectedItem.reasonDetail());
		result.setGoodsId(detailItem == null ? null : detailItem.getGoodsId());
		result.setSizeId(detailItem == null ? null : detailItem.getSizeId());
		result.setQty(selectedItem == null ? null : selectedItem.exchangeQty());
		result.setAddAmt(detailItem == null ? 0 : normalizeNonNegativeNumber(detailItem.getAddAmt()));
		result.setGoodsCpnDcAmt(0);
		result.setCartCpnDcAmt(0);
		result.setPointDcAmt(0);
		result.setChangeOrdDtlNo(null);
		result.setRegNo(auditNo);
		result.setUdtNo(auditNo);
		return result;
	}

	// 주문변경 배송 상세 저장 파라미터를 생성합니다.
	private ShopOrderChangeDetailSavePO buildShopOrderExchangeDeliveryDetailSavePO(
		String clmNo,
		ShopOrderExchangeSelectedItem selectedItem,
		Long auditNo
	) {
		// 선택한 교환 희망 사이즈 기준 배송 대기 이력 한 건을 구성합니다.
		ShopMypageOrderDetailItemVO detailItem = selectedItem == null ? null : selectedItem.detailItem();
		ShopGoodsSizeItemVO targetSize = selectedItem == null ? null : selectedItem.targetSize();
		ShopOrderChangeDetailSavePO result = new ShopOrderChangeDetailSavePO();
		result.setClmNo(clmNo);
		result.setOrdNo(detailItem == null ? null : detailItem.getOrdNo());
		result.setOrdDtlNo(detailItem == null ? null : detailItem.getOrdDtlNo());
		result.setChgDtlGbCd(SHOP_ORDER_CHANGE_DTL_GB_EXCHANGE_DELIVERY);
		result.setChgDtlStatCd(SHOP_ORDER_CHANGE_DTL_STAT_EXCHANGE_DELIVERY_WAIT);
		result.setChgReasonCd(selectedItem == null ? null : selectedItem.reasonCd());
		result.setChgReasonDtl(selectedItem == null ? null : selectedItem.reasonDetail());
		result.setGoodsId(detailItem == null ? null : detailItem.getGoodsId());
		result.setSizeId(targetSize == null ? null : targetSize.getSizeId());
		result.setQty(selectedItem == null ? null : selectedItem.exchangeQty());
		result.setAddAmt(targetSize == null ? 0 : normalizeNonNegativeNumber(targetSize.getAddAmt()));
		result.setGoodsCpnDcAmt(0);
		result.setCartCpnDcAmt(0);
		result.setPointDcAmt(0);
		result.setChangeOrdDtlNo(null);
		result.setRegNo(auditNo);
		result.setUdtNo(auditNo);
		return result;
	}

	// 교환 배송비 결제 준비 row를 생성하고 Toss 요청 정보를 반환합니다.
	private ShopOrderPaymentPrepareVO prepareShopOrderExchangePayment(
		String clmNo,
		String ordNo,
		long payDelvAmt,
		String paymentMethodCd,
		Long custNo,
		String deviceGbCd,
		String shopOrigin
	) {
		// 결제 준비 snapshot과 결제 row를 생성합니다.
		ShopOrderCustomerInfoVO customerInfo = orderService.resolveShopOrderCustomerInfo(custNo, deviceGbCd);
		String tossMethod = orderService.resolveTossMethodByPayMethodCd(paymentMethodCd);
		Map<String, Object> paymentSnapshot = new LinkedHashMap<>();
		paymentSnapshot.put("type", "exchangeDeliveryFee");
		paymentSnapshot.put("clmNo", clmNo);
		paymentSnapshot.put("ordNo", ordNo);
		paymentSnapshot.put("orderName", EXCHANGE_ORDER_NAME);
		paymentSnapshot.put("payDelvAmt", payDelvAmt);

		ShopOrderPaymentSavePO paymentSavePO = new ShopOrderPaymentSavePO();
		paymentSavePO.setOrdNo(ordNo);
		paymentSavePO.setCustNo(custNo);
		paymentSavePO.setPayStatCd(SHOP_ORDER_PAY_STAT_READY);
		paymentSavePO.setPayGbCd(SHOP_ORDER_PAY_GB_PAYMENT);
		paymentSavePO.setPayMethodCd(paymentMethodCd);
		paymentSavePO.setOrdGbCd(SHOP_ORDER_ORD_GB_EXCHANGE);
		paymentSavePO.setPgGbCd(SHOP_ORDER_PG_GB_TOSS);
		paymentSavePO.setClmNo(clmNo);
		paymentSavePO.setPayAmt(payDelvAmt);
		paymentSavePO.setDeviceGbCd(firstNonBlank(trimToNull(deviceGbCd), "PC"));
		paymentSavePO.setReqRawJson(orderService.writeShopOrderJson(paymentSnapshot));
		paymentSavePO.setRegNo(custNo);
		paymentSavePO.setUdtNo(custNo);
		orderMapper.insertShopPayment(paymentSavePO);
		if (paymentSavePO.getPayNo() == null || paymentSavePO.getPayNo() < 1L) {
			throw new IllegalStateException(SHOP_ORDER_PAYMENT_PREPARE_MESSAGE);
		}

		// Toss 결제창 성공/실패 URL과 고객 정보를 함께 응답합니다.
		String normalizedShopOrigin = orderService.requireShopOriginForPayment(shopOrigin);
		ShopOrderPaymentPrepareVO result = new ShopOrderPaymentPrepareVO();
		result.setOrdNo(ordNo);
		result.setPayNo(paymentSavePO.getPayNo());
		result.setClientKey(orderService.resolveShopOrderClientKey());
		result.setMethod(tossMethod);
		result.setOrderId(clmNo);
		result.setOrderName(EXCHANGE_ORDER_NAME);
		result.setAmount(payDelvAmt);
		result.setCustomerKey(customerInfo.getCustomerKey());
		result.setCustomerName(customerInfo.getCustNm());
		result.setCustomerEmail(customerInfo.getEmail());
		result.setCustomerMobilePhone(customerInfo.getPhoneNumber());
		result.setSuccessUrl(buildShopOrderExchangePaymentSuccessUrl(normalizedShopOrigin, paymentSavePO.getPayNo()));
		result.setFailUrl(buildShopOrderExchangePaymentFailUrl(normalizedShopOrigin, paymentSavePO.getPayNo(), ordNo));
		return result;
	}

	// 교환 배송비 결제 성공 URL을 생성합니다.
	private String buildShopOrderExchangePaymentSuccessUrl(String shopOrigin, Long payNo) {
		// 결제번호를 포함한 교환 결제 성공 URL을 반환합니다.
		return shopOrigin + "/mypage/order/exchange/success?payNo=" + payNo;
	}

	// 교환 배송비 결제 실패 URL을 생성합니다.
	private String buildShopOrderExchangePaymentFailUrl(String shopOrigin, Long payNo, String ordNo) {
		// 결제번호와 원 주문번호를 포함한 교환 결제 실패 URL을 반환합니다.
		return shopOrigin
			+ "/mypage/order/exchange/fail?payNo="
			+ payNo
			+ "&originOrdNo="
			+ encodeShopOrderExchangeUrlValue(safeValue(firstNonBlank(trimToNull(ordNo), "")));
	}

	// URL 쿼리값을 인코딩합니다.
	private String encodeShopOrderExchangeUrlValue(String value) {
		// UTF-8 기준으로 교환 결제 복귀 URL 쿼리값을 안전하게 인코딩합니다.
		return URLEncoder.encode(safeValue(firstNonBlank(value, "")), StandardCharsets.UTF_8);
	}

	// 교환 배송비 결제 row를 검증합니다.
	private ShopOrderPaymentVO validateShopOrderExchangePayment(Long payNo, String clmNo, Long custNo, Long amount) {
		// 결제번호 기준 결제 row와 고객 소유 여부를 확인합니다.
		ShopOrderPaymentVO payment = orderMapper.getShopPaymentByPayNo(payNo);
		if (payment == null
			|| payment.getCustNo() == null
			|| !custNo.equals(payment.getCustNo())
			|| !SHOP_ORDER_ORD_GB_EXCHANGE.equals(payment.getOrdGbCd())
			|| !clmNo.equals(trimToNull(payment.getClmNo()))) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_EXCHANGE_PAYMENT_INVALID_MESSAGE);
		}
		if (amount != null && (payment.getPayAmt() == null || payment.getPayAmt().longValue() != amount.longValue())) {
			throw new IllegalArgumentException("승인 금액을 확인해주세요.");
		}
		return payment;
	}

	// 교환 배송비 결제 대상 클레임 상태를 검증합니다.
	private ShopOrderExchangePaymentClaimVO validateShopOrderExchangePaymentClaim(String clmNo, Long custNo, boolean requirePaymentWait) {
		// 고객 소유 클레임과 현재 상태를 확인합니다.
		ShopOrderExchangePaymentClaimVO claim = orderMapper.getShopOrderExchangePaymentClaim(clmNo, custNo);
		if (claim == null || !SHOP_ORDER_CHANGE_STAT_PROGRESS.equals(claim.getChgStatCd())) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_EXCHANGE_PAYMENT_INVALID_MESSAGE);
		}
		if (requirePaymentWait
			&& (!SHOP_ORDER_CHANGE_DTL_STAT_EXCHANGE_PAYMENT_WAIT.equals(claim.getMinChgDtlStatCd())
				|| !SHOP_ORDER_CHANGE_DTL_STAT_EXCHANGE_PAYMENT_WAIT.equals(claim.getMaxChgDtlStatCd()))) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_EXCHANGE_PAYMENT_INVALID_MESSAGE);
		}
		return claim;
	}

	// 교환 클레임을 철회 상태로 변경합니다.
	void withdrawShopOrderExchangeClaim(String clmNo, Long auditNo) {
		// 교환 결제 실패/취소/만료 시 클레임 마스터와 상세를 철회 처리합니다.
		orderMapper.updateShopOrderChangeBaseStatus(clmNo, SHOP_ORDER_CHANGE_STAT_WITHDRAW, auditNo);
		orderMapper.updateShopOrderChangeDetailStatusByClaimAndGb(
			clmNo,
			SHOP_ORDER_CHANGE_DTL_GB_EXCHANGE_PICKUP,
			SHOP_ORDER_CHANGE_DTL_STAT_EXCHANGE_WITHDRAW,
			auditNo
		);
		orderMapper.updateShopOrderChangeDetailStatusByClaimAndGb(
			clmNo,
			SHOP_ORDER_CHANGE_DTL_GB_EXCHANGE_DELIVERY,
			SHOP_ORDER_CHANGE_DTL_STAT_EXCHANGE_DELIVERY_WITHDRAW,
			auditNo
		);
	}

	// 교환 클레임을 교환 신청 상태로 변경합니다.
	void applyShopOrderExchangeClaimPaymentComplete(String clmNo, Long auditNo) {
		// 교환 배송비 결제 완료 후 회수 상세만 교환 신청 상태로 승격합니다.
		orderMapper.updateShopOrderChangeDetailStatusByClaimAndGb(
			clmNo,
			SHOP_ORDER_CHANGE_DTL_GB_EXCHANGE_PICKUP,
			SHOP_ORDER_CHANGE_DTL_STAT_EXCHANGE_APPLY,
			auditNo
		);
	}

	// 교환 배송비 결제 승인 결과를 응답 객체로 구성합니다.
	private ShopOrderExchangePaymentConfirmVO buildShopOrderExchangePaymentConfirmResult(
		ShopOrderPaymentVO payment,
		ShopOrderExchangePaymentClaimVO claim
	) {
		// 결제 요약 정보와 가상계좌 정보를 응답 객체에 채웁니다.
		ShopOrderExchangePaymentConfirmVO result = new ShopOrderExchangePaymentConfirmVO();
		result.setClmNo(payment == null ? null : payment.getClmNo());
		result.setOrdNo(claim == null ? null : claim.getOrdNo());
		result.setPayNo(payment == null ? null : payment.getPayNo());
		result.setPayMethodCd(payment == null ? null : payment.getPayMethodCd());
		result.setPayStatCd(payment == null ? null : payment.getPayStatCd());
		result.setChgDtlStatCd(claim == null ? null : claim.getMinChgDtlStatCd());
		result.setOrderName(payment == null ? null : safeValue(firstNonBlank(readPaymentSnapshotValue(payment.getReqRawJson(), "orderName"), EXCHANGE_ORDER_NAME)));
		result.setAmount(payment == null ? null : payment.getPayAmt());
		result.setBankCd(payment == null ? null : payment.getBankCd());
		result.setBankNm(resolveShopOrderBankName(payment == null ? null : payment.getBankCd()));
		result.setBankNo(payment == null ? null : payment.getBankNo());
		result.setVactHolderNm(payment == null ? null : payment.getVactHolderNm());
		result.setVactDueDt(payment == null ? null : payment.getVactDueDt());
		return result;
	}

	// 결제 snapshot에서 문자열 필드 값을 조회합니다.
	private String readPaymentSnapshotValue(String snapshotJson, String fieldName) {
		// JSON 파싱 실패 시 빈 문자열을 반환합니다.
		return orderService.resolveJsonText(orderService.readShopOrderJsonNode(snapshotJson), fieldName);
	}

	// 가상계좌 은행코드를 공통코드 기준 은행명으로 변환합니다.
	private String resolveShopOrderBankName(String bankCd) {
		// 은행코드가 비어 있으면 빈 문자열을 반환합니다.
		String normalizedBankCd = trimToNull(bankCd);
		if (normalizedBankCd == null) {
			return "";
		}
		for (CommonCodeVO code : commonMapper.getCommonCodeList(SHOP_ORDER_BANK_GRP_CD)) {
			if (code != null && normalizedBankCd.equals(trimToNull(code.getCd()))) {
				return safeValue(firstNonBlank(trimToNull(code.getCdNm()), ""));
			}
		}
		return "";
	}

	// 결제 실패 요청 원본 JSON 문자열을 생성합니다.
	private String buildShopOrderExchangePaymentFailureRawJson(ShopOrderExchangePaymentFailPO param) {
		// Toss 실패 코드와 메시지를 JSON 형태로 저장합니다.
		Map<String, Object> rawMap = new LinkedHashMap<>();
		rawMap.put("code", safeValue(firstNonBlank(trimToNull(param == null ? null : param.getCode()), "")));
		rawMap.put("message", safeValue(firstNonBlank(trimToNull(param == null ? null : param.getMessage()), "")));
		return orderService.writeShopOrderJson(rawMap);
	}

	// 결제 실패 코드가 사용자 취소 계열인지 확인합니다.
	private boolean isShopOrderCancelFailureCode(String code) {
		// 코드에 CANCEL이 포함되면 사용자 취소 계열로 판단합니다.
		String normalizedCode = trimToNull(code);
		return normalizedCode != null && normalizedCode.toUpperCase().contains("CANCEL");
	}

	// 접두사와 고객번호와 년월일시분초밀리초를 조합해 클레임번호를 생성합니다.
	private String generateShopOrderClaimNo(Long custNo) {
		// 클레임번호는 주문번호와 구분되도록 C 접두사를 사용합니다.
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
		long safeCustNo = custNo == null ? 0L : Math.max(custNo, 0L);
		return "C" + safeCustNo + timestamp;
	}

	// 0 이상 숫자값을 안전한 int 값으로 보정합니다.
	private int normalizeNonNegativeNumber(Number value) {
		// null 또는 음수/비정상 값은 0으로 보정합니다.
		if (value == null) {
			return 0;
		}
		long longValue = value.longValue();
		if (longValue < 1L) {
			return 0;
		}
		return (int) Math.min(longValue, Integer.MAX_VALUE);
	}

	// 0 이상 Long 값을 안전하게 반환합니다.
	private long resolveNonNegativeLong(Long value) {
		// null 또는 음수 값은 0으로 보정합니다.
		return value == null ? 0L : Math.max(value, 0L);
	}

	// long 값을 int 범위로 보정합니다.
	private int toSafeInt(long value) {
		// ORDER_CHANGE_BASE.PAY_DELV_AMT 범위에 맞춰 안전한 int 값을 반환합니다.
		return (int) Math.max(Math.min(value, Integer.MAX_VALUE), Integer.MIN_VALUE);
	}
}
