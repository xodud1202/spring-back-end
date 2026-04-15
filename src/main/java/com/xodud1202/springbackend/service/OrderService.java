package com.xodud1202.springbackend.service;

import static com.xodud1202.springbackend.common.util.CommonPaginationUtils.*;
import static com.xodud1202.springbackend.common.util.CommonTextUtils.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xodud1202.springbackend.config.properties.TossProperties;
import com.xodud1202.springbackend.domain.admin.goods.*;
import com.xodud1202.springbackend.domain.admin.order.*;
import com.xodud1202.springbackend.domain.common.CommonCodeVO;
import com.xodud1202.springbackend.entity.UserBaseEntity;
import com.xodud1202.springbackend.domain.shop.cart.*;
import com.xodud1202.springbackend.domain.shop.goods.*;
import com.xodud1202.springbackend.domain.shop.mypage.*;
import com.xodud1202.springbackend.domain.shop.order.*;
import com.xodud1202.springbackend.domain.shop.site.ShopSiteInfoVO;
import com.xodud1202.springbackend.mapper.CartMapper;
import com.xodud1202.springbackend.mapper.CommonMapper;
import com.xodud1202.springbackend.mapper.ExhibitionMapper;
import com.xodud1202.springbackend.mapper.GoodsMapper;
import com.xodud1202.springbackend.mapper.OrderMapper;
import com.xodud1202.springbackend.mapper.SiteInfoMapper;
import com.xodud1202.springbackend.service.order.support.ShopCartCouponEstimateRow;
import com.xodud1202.springbackend.service.order.support.ShopCartValidatedInput;
import com.xodud1202.springbackend.service.order.support.ShopMypageOrderDateRange;
import com.xodud1202.springbackend.service.order.support.ShopOrderDiscountContext;
import com.xodud1202.springbackend.service.order.support.ShopOrderGoodsCouponMatchResult;
import com.xodud1202.springbackend.service.order.support.ShopOrderRefundAccountInfo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import static com.xodud1202.springbackend.common.Constants.Common.*;
import static com.xodud1202.springbackend.common.Constants.Shop.*;

@Service
@RequiredArgsConstructor
// 주문 관련 비즈니스 로직을 처리합니다.
public class OrderService {
	private static final Logger log = LoggerFactory.getLogger(OrderService.class);

	private final CartMapper cartMapper;
	private final OrderMapper orderMapper;
	private final GoodsMapper goodsMapper;
	private final CommonMapper commonMapper;
	private final ExhibitionMapper exhibitionMapper;
	private final SiteInfoMapper siteInfoMapper;
	private final GoodsImageService goodsImageService;
	private final ShopAuthService shopAuthService;
	private final JusoAddressApiClient jusoAddressApiClient;
	private final TossPaymentsClient tossPaymentsClient;
	private final TossProperties tossProperties;
	private final ObjectMapper objectMapper;
	private static final int GOODS_IMAGE_MIN_SIZE = 500;
	private static final int GOODS_IMAGE_MAX_SIZE = 1500;
	private static final int ADMIN_ORDER_DEFAULT_PAGE = 1;
	private static final int ADMIN_ORDER_DEFAULT_PAGE_SIZE = 20;
	private static final int ADMIN_ORDER_MAX_PAGE_SIZE = 200;
	private static final String ADMIN_ORDER_START_DELIVERY_COMPANY_GRP_CD = "DELV_COMP";
	private static final int ADMIN_ORDER_START_DELIVERY_INVOICE_NO_MAX_LENGTH = 20;
	private static final Set<String> ADMIN_ORDER_START_DELIVERY_STATUS_SET = Set.of(
		SHOP_ORDER_DTL_STAT_PREPARING,
		SHOP_ORDER_DTL_STAT_DELIVERY_PREPARING,
		SHOP_ORDER_DTL_STAT_DELIVERING
	);

	// 관리자 주문 목록을 조회합니다.
	public AdminOrderListResponseVO getAdminOrderList(
		Integer page,
		Integer pageSize,
		String searchGb,
		String searchValue,
		String dateGb,
		String searchStartDt,
		String searchEndDt,
		String ordDtlStatCd,
		String chgDtlStatCd
	) {
		// 페이징 기본값을 계산합니다.
		int resolvedPage = normalizePage(page, ADMIN_ORDER_DEFAULT_PAGE);
		int resolvedPageSize = normalizePageSize(pageSize, ADMIN_ORDER_DEFAULT_PAGE_SIZE, ADMIN_ORDER_MAX_PAGE_SIZE);
		int offset = calculateOffset(resolvedPage, resolvedPageSize);

		// 검색 조건과 조회 기간을 정규화합니다.
		String normalizedSearchValue = trimToNull(searchValue);
		String normalizedSearchGb = normalizeAdminOrderSearchGb(searchGb);
		String normalizedDateGb = normalizeAdminOrderDateGb(dateGb);
		ShopMypageOrderDateRange dateRange = resolveShopMypageOrderDateRange(searchStartDt, searchEndDt);

		// 매퍼 조회용 파라미터를 구성합니다.
		AdminOrderPO param = new AdminOrderPO();
		param.setPage(resolvedPage);
		param.setPageSize(resolvedPageSize);
		param.setOffset(offset);
		param.setSearchGb(normalizedSearchGb);
		param.setSearchValue(normalizedSearchValue);
		param.setDateGb(normalizedDateGb);
		param.setSearchStartDt(dateRange.getStartDate());
		param.setSearchEndDt(dateRange.getEndDate());
		param.setStartDateTime(dateRange.getStartDateTime());
		param.setEndExclusiveDateTime(dateRange.getEndExclusiveDateTime());
		param.setOrdDtlStatCd(trimToNull(ordDtlStatCd));
		param.setChgDtlStatCd(trimToNull(chgDtlStatCd));

		// 목록과 건수를 조회합니다.
		List<AdminOrderListRowVO> list = orderMapper.getAdminOrderList(param);
		int totalCount = orderMapper.getAdminOrderCount(param);

		// 응답 객체를 구성합니다.
		AdminOrderListResponseVO result = new AdminOrderListResponseVO();
		result.setList(list == null ? List.of() : list);
		result.setTotalCount(totalCount);
		result.setPage(resolvedPage);
		result.setPageSize(resolvedPageSize);
		return result;
	}

	// 관리자 배송 시작 관리 목록을 조회합니다.
	AdminOrderStartDeliveryListResponseVO getAdminOrderStartDeliveryList(
		Integer page,
		Integer pageSize,
		String ordDtlStatCd
	) {
		// 페이징 기본값을 계산합니다.
		int resolvedPage = normalizePage(page, ADMIN_ORDER_DEFAULT_PAGE);
		int resolvedPageSize = normalizePageSize(pageSize, ADMIN_ORDER_DEFAULT_PAGE_SIZE, ADMIN_ORDER_MAX_PAGE_SIZE);
		int offset = calculateOffset(resolvedPage, resolvedPageSize);

		// 허용된 배송 상태만 조회 조건으로 사용합니다.
		String normalizedOrdDtlStatCd = normalizeAdminOrderStartDeliveryStatus(ordDtlStatCd);

		// 매퍼 조회용 파라미터를 구성합니다.
		AdminOrderStartDeliveryPO param = new AdminOrderStartDeliveryPO();
		param.setPage(resolvedPage);
		param.setPageSize(resolvedPageSize);
		param.setOffset(offset);
		param.setOrdDtlStatCd(normalizedOrdDtlStatCd);

		// 목록과 건수를 조회합니다.
		List<AdminOrderStartDeliveryListRowVO> list = orderMapper.getAdminOrderStartDeliveryList(param);
		int totalCount = orderMapper.getAdminOrderStartDeliveryCount(param);

		// 응답 객체를 구성합니다.
		AdminOrderStartDeliveryListResponseVO result = new AdminOrderStartDeliveryListResponseVO();
		result.setList(list == null ? List.of() : list);
		result.setTotalCount(totalCount);
		result.setPage(resolvedPage);
		result.setPageSize(resolvedPageSize);
		return result;
	}

	// 관리자 주문 상세 정보를 조회합니다.
	public AdminOrderDetailVO getAdminOrderDetail(String ordNo) {
		// 주문번호 필수 검증을 수행합니다.
		if (ordNo == null || ordNo.isBlank()) {
			throw new IllegalArgumentException("주문번호는 필수입니다.");
		}

		// 공백 제거된 주문번호를 재사용합니다.
		String trimmedOrdNo = ordNo.trim();

		// 주문 마스터 정보를 조회합니다.
		AdminOrderMasterVO master = orderMapper.getAdminOrderMaster(trimmedOrdNo);
		if (master == null) {
			throw new IllegalArgumentException("존재하지 않는 주문번호입니다: " + ordNo);
		}

		// 주문 상세 목록을 조회합니다.
		List<AdminOrderDetailRowVO> detailList = orderMapper.getAdminOrderDetailList(trimmedOrdNo);

		// 주문 클레임 목록을 조회합니다.
		List<AdminOrderClaimRowVO> claimList = orderMapper.getAdminOrderClaimList(trimmedOrdNo);

		// 주문 결제 목록을 조회합니다.
		List<AdminOrderPaymentRowVO> paymentList = orderMapper.getAdminOrderPaymentList(trimmedOrdNo);

		// 응답 객체를 구성합니다.
		AdminOrderDetailVO result = new AdminOrderDetailVO();
		result.setMaster(master);
		result.setList(detailList == null ? List.of() : detailList);
		result.setClaimList(claimList == null ? List.of() : claimList);
		result.setPaymentList(paymentList == null ? List.of() : paymentList);
		return result;
	}

	// 관리자 주문상세를 상품 준비중 상태로 변경합니다.
	@Transactional
	AdminOrderDetailStatusUpdateVO prepareAdminOrderDetail(AdminOrderDetailStatusUpdatePO param) {
		// 요청 데이터와 주문번호를 검증합니다.
		if (param == null) {
			throw new IllegalArgumentException("결제 완료 주문건만 선택해주세요.");
		}
		String ordNo = trimToNull(param.getOrdNo());
		if (ordNo == null) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_NO_INVALID_MESSAGE);
		}

		// 선택 주문상세번호 목록을 중복 제거된 양수 목록으로 정규화합니다.
		List<Integer> ordDtlNoList = normalizeAdminOrderDetailNoList(param.getOrdDtlNoList());
		if (ordDtlNoList.isEmpty()) {
			throw new IllegalArgumentException("결제 완료 주문건만 선택해주세요.");
		}

		// 주문번호로 고객번호를 조회해 감사 컬럼 갱신값으로 재사용합니다.
		Long custNo = orderMapper.getOrderCustNo(ordNo);
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_NO_INVALID_MESSAGE);
		}

		// 결제 완료 상태의 선택 주문상세만 상품 준비중으로 변경합니다.
		int updatedCount = orderMapper.updateAdminOrderDetailStatusByOrdDtlNoList(
			ordNo,
			ordDtlNoList,
			SHOP_ORDER_DTL_STAT_DONE,
			SHOP_ORDER_DTL_STAT_PREPARING,
			custNo
		);
		if (updatedCount != ordDtlNoList.size()) {
			throw new IllegalArgumentException("결제 완료 주문건만 선택해주세요.");
		}

		// 변경 건수를 응답 객체에 담아 반환합니다.
		AdminOrderDetailStatusUpdateVO result = new AdminOrderDetailStatusUpdateVO();
		result.setUpdatedCount(updatedCount);
		return result;
	}

	// 관리자 상품 준비중 주문을 배송 준비중 상태로 변경합니다.
	@Transactional
	AdminOrderStartDeliveryStatusUpdateVO prepareAdminOrderStartDelivery(AdminOrderStartDeliveryPreparePO param) {
		// 요청 데이터와 배송업체 공통코드를 검증 준비합니다.
		if (param == null) {
			throw new IllegalArgumentException("상품 준비중 주문건만 선택해주세요.");
		}
		Set<String> deliveryCompanyCodeSet = getAdminOrderStartDeliveryCompanyCodeSet();

		// 요청 상품 목록을 정규화하고 배송정보를 검증합니다.
		List<AdminOrderStartDeliveryPrepareItemPO> itemList = normalizeAdminOrderStartDeliveryPrepareItemList(
			param.getItemList(),
			deliveryCompanyCodeSet
		);
		if (itemList.isEmpty()) {
			throw new IllegalArgumentException("상품 준비중 주문건만 선택해주세요.");
		}

		// 현재 로그인 관리자 번호를 감사 컬럼에 반영합니다.
		Long udtNo = resolveCurrentAdminUserNo();

		// 상품 준비중 상태의 선택 상품만 배송 준비중으로 변경합니다.
		int updatedCount = orderMapper.updateAdminOrderStartDeliveryPrepareList(
			itemList,
			SHOP_ORDER_DTL_STAT_PREPARING,
			SHOP_ORDER_DTL_STAT_DELIVERY_PREPARING,
			udtNo
		);
		if (updatedCount != itemList.size()) {
			throw new IllegalArgumentException("상품 준비중 주문건만 선택해주세요.");
		}

		// 변경 건수를 응답 객체에 담아 반환합니다.
		AdminOrderStartDeliveryStatusUpdateVO result = new AdminOrderStartDeliveryStatusUpdateVO();
		result.setUpdatedCount(updatedCount);
		return result;
	}

	// 관리자 배송 준비중 주문을 배송중 상태로 변경합니다.
	@Transactional
	AdminOrderStartDeliveryStatusUpdateVO startAdminOrderStartDelivery(AdminOrderStartDeliveryStatusPO param) {
		// 배송 준비중 상품 키 목록만 정규화해 상태 변경을 위임합니다.
		List<AdminOrderStartDeliveryKeyItemPO> itemList = normalizeAdminOrderStartDeliveryKeyItemList(
			param == null ? null : param.getItemList()
		);
		return applyAdminOrderStartDeliveryStatusChange(
			itemList,
			SHOP_ORDER_DTL_STAT_DELIVERY_PREPARING,
			SHOP_ORDER_DTL_STAT_DELIVERING,
			"배송 준비중 주문건만 선택해주세요.",
			false
		);
	}

	// 관리자 배송중 주문을 배송완료 상태로 변경합니다.
	@Transactional
	AdminOrderStartDeliveryStatusUpdateVO completeAdminOrderStartDelivery(AdminOrderStartDeliveryStatusPO param) {
		// 배송중 상품 키 목록만 정규화해 상태 변경을 위임합니다.
		List<AdminOrderStartDeliveryKeyItemPO> itemList = normalizeAdminOrderStartDeliveryKeyItemList(
			param == null ? null : param.getItemList()
		);
		return applyAdminOrderStartDeliveryStatusChange(
			itemList,
			SHOP_ORDER_DTL_STAT_DELIVERING,
			SHOP_ORDER_DTL_STAT_DELIVERY_COMPLETE,
			"배송중 주문건만 선택해주세요.",
			true
		);
	}

	// 관리자 주문 검색 구분값을 허용 범위로 보정합니다.
	private String normalizeAdminOrderSearchGb(String searchGb) {
		// 상품코드 검색만 허용하고 나머지는 주문번호 검색으로 고정합니다.
		return "goodsId".equalsIgnoreCase(trimToNull(searchGb)) ? "goodsId" : "ordNo";
	}

	// 관리자 주문 기간 구분값을 허용 범위로 보정합니다.
	private String normalizeAdminOrderDateGb(String dateGb) {
		// 결제기간 검색만 별도로 허용하고 나머지는 주문기간 검색으로 고정합니다.
		return "PAY_DT".equalsIgnoreCase(trimToNull(dateGb)) ? "PAY_DT" : "ORDER_DT";
	}

	// 관리자 배송 시작 관리 상태값을 허용 범위로 보정합니다.
	private String normalizeAdminOrderStartDeliveryStatus(String ordDtlStatCd) {
		// 값이 비어 있으면 기본 조회 상태를 상품 준비중으로 고정합니다.
		String normalizedOrdDtlStatCd = trimToNull(ordDtlStatCd);
		if (normalizedOrdDtlStatCd == null) {
			return SHOP_ORDER_DTL_STAT_PREPARING;
		}
		if (!ADMIN_ORDER_START_DELIVERY_STATUS_SET.contains(normalizedOrdDtlStatCd)) {
			throw new IllegalArgumentException("상품 상태를 확인해주세요.");
		}
		return normalizedOrdDtlStatCd;
	}

	// 관리자 배송업체 공통코드 목록을 조회해 코드 집합으로 반환합니다.
	private Set<String> getAdminOrderStartDeliveryCompanyCodeSet() {
		// 공통코드 조회 결과에서 실제 사용 가능한 배송업체 코드만 추출합니다.
		Set<String> result = new LinkedHashSet<>();
		for (CommonCodeVO item : commonMapper.getCommonCodeList(ADMIN_ORDER_START_DELIVERY_COMPANY_GRP_CD)) {
			String code = trimToNull(item == null ? null : item.getCd());
			if (code != null) {
				result.add(code);
			}
		}
		return Set.copyOf(result);
	}

	// 관리자 배송 준비중 요청 상품 목록을 정규화합니다.
	private List<AdminOrderStartDeliveryPrepareItemPO> normalizeAdminOrderStartDeliveryPrepareItemList(
		List<AdminOrderStartDeliveryPrepareItemPO> itemList,
		Set<String> deliveryCompanyCodeSet
	) {
		// null/중복/비정상 값을 제거하지 않고 즉시 검증 오류를 반환합니다.
		if (itemList == null || itemList.isEmpty()) {
			return List.of();
		}

		Map<String, AdminOrderStartDeliveryPrepareItemPO> normalizedMap = new LinkedHashMap<>();
		for (AdminOrderStartDeliveryPrepareItemPO item : itemList) {
			String ordNo = trimToNull(item == null ? null : item.getOrdNo());
			Integer ordDtlNo = item == null ? null : item.getOrdDtlNo();
			String delvCompCd = trimToNull(item == null ? null : item.getDelvCompCd());
			String invoiceNo = normalizeAdminOrderStartDeliveryInvoiceNo(item == null ? null : item.getInvoiceNo());

			// 복합키와 배송정보의 기본 형식을 검증합니다.
			if (ordNo == null || ordDtlNo == null || ordDtlNo < 1) {
				throw new IllegalArgumentException("선택 주문건 정보를 확인해주세요.");
			}
			if (delvCompCd == null || !deliveryCompanyCodeSet.contains(delvCompCd)) {
				throw new IllegalArgumentException("배송업체를 선택해주세요.");
			}
			if (!isValidAdminOrderStartDeliveryInvoiceNo(invoiceNo)) {
				throw new IllegalArgumentException("송장번호는 숫자 20자리 이하로 입력해주세요.");
			}

			// 검증된 값만 새 객체로 복사해 이후 로직에서 재사용합니다.
			AdminOrderStartDeliveryPrepareItemPO normalizedItem = new AdminOrderStartDeliveryPrepareItemPO();
			normalizedItem.setOrdNo(ordNo);
			normalizedItem.setOrdDtlNo(ordDtlNo);
			normalizedItem.setDelvCompCd(delvCompCd);
			normalizedItem.setInvoiceNo(invoiceNo);
			normalizedMap.putIfAbsent(buildAdminOrderStartDeliveryItemKey(ordNo, ordDtlNo), normalizedItem);
		}
		return List.copyOf(normalizedMap.values());
	}

	// 관리자 배송 상태 변경 대상 키 목록을 정규화합니다.
	private List<AdminOrderStartDeliveryKeyItemPO> normalizeAdminOrderStartDeliveryKeyItemList(
		List<AdminOrderStartDeliveryKeyItemPO> itemList
	) {
		// null/음수/중복 값을 제거하고 유효한 복합키만 유지합니다.
		if (itemList == null || itemList.isEmpty()) {
			return List.of();
		}

		Map<String, AdminOrderStartDeliveryKeyItemPO> normalizedMap = new LinkedHashMap<>();
		for (AdminOrderStartDeliveryKeyItemPO item : itemList) {
			String ordNo = trimToNull(item == null ? null : item.getOrdNo());
			Integer ordDtlNo = item == null ? null : item.getOrdDtlNo();
			if (ordNo == null || ordDtlNo == null || ordDtlNo < 1) {
				throw new IllegalArgumentException("선택 주문건 정보를 확인해주세요.");
			}

			// 검증된 복합키만 새 객체로 복사해 상태 변경 요청으로 사용합니다.
			AdminOrderStartDeliveryKeyItemPO normalizedItem = new AdminOrderStartDeliveryKeyItemPO();
			normalizedItem.setOrdNo(ordNo);
			normalizedItem.setOrdDtlNo(ordDtlNo);
			normalizedMap.putIfAbsent(buildAdminOrderStartDeliveryItemKey(ordNo, ordDtlNo), normalizedItem);
		}
		return List.copyOf(normalizedMap.values());
	}

	// 관리자 배송 상태 변경 공통 로직을 수행합니다.
	private AdminOrderStartDeliveryStatusUpdateVO applyAdminOrderStartDeliveryStatusChange(
		List<AdminOrderStartDeliveryKeyItemPO> itemList,
		String fromOrdDtlStatCd,
		String toOrdDtlStatCd,
		String invalidMessage,
		boolean updateDelvCompleteDt
	) {
		// 선택 목록이 없으면 상태 변경을 진행하지 않습니다.
		if (itemList.isEmpty()) {
			throw new IllegalArgumentException(invalidMessage);
		}

		// 현재 로그인 관리자 번호를 감사 컬럼에 반영합니다.
		Long udtNo = resolveCurrentAdminUserNo();

		// 현재 상태와 일치하는 선택 상품만 다음 상태로 변경합니다.
		int updatedCount = updateOrderDetailStatusByKeyList(
			itemList,
			fromOrdDtlStatCd,
			toOrdDtlStatCd,
			udtNo,
			updateDelvCompleteDt
		);
		if (updatedCount != itemList.size()) {
			throw new IllegalArgumentException(invalidMessage);
		}

		// 변경 건수를 응답 객체에 담아 반환합니다.
		AdminOrderStartDeliveryStatusUpdateVO result = new AdminOrderStartDeliveryStatusUpdateVO();
		result.setUpdatedCount(updatedCount);
		return result;
	}

	// 관리자 배송 시작 관리 송장번호를 정규화합니다.
	private String normalizeAdminOrderStartDeliveryInvoiceNo(String invoiceNo) {
		// 공백만 제거하고 서버에서는 숫자 형식 여부를 별도로 검증합니다.
		return trimToNull(invoiceNo);
	}

	// 관리자 배송 시작 관리 송장번호 형식을 검증합니다.
	private boolean isValidAdminOrderStartDeliveryInvoiceNo(String invoiceNo) {
		// 숫자만 허용하고 DB 컬럼 길이 20자를 초과하지 않아야 합니다.
		return invoiceNo != null
			&& invoiceNo.length() <= ADMIN_ORDER_START_DELIVERY_INVOICE_NO_MAX_LENGTH
			&& invoiceNo.chars().allMatch(Character::isDigit);
	}

	// 관리자 배송 시작 관리 요청 항목의 복합키 문자열을 생성합니다.
	private String buildAdminOrderStartDeliveryItemKey(String ordNo, Integer ordDtlNo) {
		return ordNo + ":" + ordDtlNo;
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

	// Y/N 값을 보정하고 그 외 값은 N으로 반환합니다.
	private String normalizeYesNo(String value) {
		return YES.equalsIgnoreCase(trimToNull(value)) ? YES : NO;
	}

	// 쇼핑몰 마이페이지 주문내역 페이지 데이터를 조회합니다.
	public ShopMypageOrderPageVO getShopMypageOrderPage(
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
		ShopMypageOrderDateRange orderDateRange = resolveShopMypageOrderDateRange(requestedStartDate, requestedEndDate);

		// 주문번호 기준 전체 건수와 전체 페이지 수를 계산합니다.
		int orderCount = orderMapper.countShopMypageOrderGroup(
			custNo,
			orderDateRange.getStartDateTime(),
			orderDateRange.getEndExclusiveDateTime()
		);
		int totalPageCount = calculateTotalPageCount(orderCount, SHOP_MYPAGE_ORDER_PAGE_SIZE);
		int resolvedPageNo = resolvePageNoWithinRange(resolvedRequestedPageNo, totalPageCount);
		int offset = calculateOffset(resolvedPageNo, SHOP_MYPAGE_ORDER_PAGE_SIZE);

		// 현재 페이지의 주문번호 목록과 상태 요약을 조회합니다.
		List<ShopMypageOrderGroupVO> orderList = orderMapper.getShopMypageOrderGroupList(
			custNo,
			orderDateRange.getStartDateTime(),
			orderDateRange.getEndExclusiveDateTime(),
			offset,
			SHOP_MYPAGE_ORDER_PAGE_SIZE
		);
		ShopMypageOrderStatusSummaryVO statusSummary = orderMapper.getShopMypageOrderStatusSummary(
			custNo,
			orderDateRange.getStartDateTime(),
			orderDateRange.getEndExclusiveDateTime()
		);
		statusSummary = normalizeShopMypageOrderStatusSummary(statusSummary);

		// 주문번호별 주문상세 목록을 묶고 상품 이미지 URL을 세팅합니다.
		List<ShopMypageOrderGroupVO> resolvedOrderList = attachShopMypageOrderDetailList(orderList);

		// 주문내역 페이지 응답 객체를 구성합니다.
		ShopMypageOrderPageVO result = new ShopMypageOrderPageVO();
		result.setOrderList(resolvedOrderList);
		result.setOrderCount(orderCount);
		result.setPageNo(resolvedPageNo);
		result.setPageSize(SHOP_MYPAGE_ORDER_PAGE_SIZE);
		result.setTotalPageCount(totalPageCount);
		result.setStartDate(orderDateRange.getStartDate());
		result.setEndDate(orderDateRange.getEndDate());
		result.setStatusSummary(statusSummary);
		return result;
	}

	// 쇼핑몰 마이페이지 주문상세 페이지 데이터를 조회합니다.
	public ShopMypageOrderDetailPageVO getShopMypageOrderDetailPage(Long custNo, String ordNo) {
		// 고객번호와 주문번호를 검증합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}
		String resolvedOrdNo = trimToNull(ordNo);
		if (resolvedOrdNo == null) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_NO_INVALID_MESSAGE);
		}

		// 현재 로그인 고객의 주문번호 1건과 금액 요약을 조회합니다.
		ShopMypageOrderGroupVO orderGroup = getShopMypageOrderGroupWithDetail(custNo, resolvedOrdNo);
		ShopMypageOrderAmountSummaryVO amountSummary = buildShopMypageOrderAmountSummary(custNo, orderGroup);

		// 주문상세 페이지 응답 객체를 구성합니다.
		ShopMypageOrderDetailPageVO result = new ShopMypageOrderDetailPageVO();
		result.setOrder(orderGroup);
		result.setAmountSummary(amountSummary);
		return result;
	}

	// 쇼핑몰 마이페이지 반품 신청 화면 데이터를 조회합니다.
	ShopMypageOrderReturnPageVO getShopMypageOrderReturnPage(Long custNo, String ordNo, Integer ordDtlNo) {
		// 고객번호와 주문번호를 검증합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}
		String resolvedOrdNo = trimToNull(ordNo);
		if (resolvedOrdNo == null) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_NO_INVALID_MESSAGE);
		}

		// 현재 로그인 고객의 주문번호 1건과 금액/사유/배송 기준 정보를 조회합니다.
		ShopMypageOrderGroupVO orderGroup = getShopMypageOrderGroupWithDetail(custNo, resolvedOrdNo);
		validateShopMypageOrderReturnAccess(orderGroup, ordDtlNo);
		ShopOrderCancelOrderBaseVO orderBase = resolveShopOrderCancelOrderBase(custNo, resolvedOrdNo);
		List<ShopOrderAddressVO> addressList = resolveShopOrderAddressList(custNo);
		List<ShopMypageOrderCancelReasonVO> reasonList = normalizeShopMypageOrderCancelReasonList(
			orderMapper.getShopMypageOrderReturnReasonList()
		);
		ShopCartSiteInfoVO siteInfo = resolveShopCartSiteInfo();
		ShopMypageOrderAmountSummaryVO amountSummary = buildShopOrderRemainingAmountSummary(orderGroup, orderBase, siteInfo);
		ShopMypageOrderReturnFeeContextVO returnFeeContext = buildShopMypageOrderReturnFeeContext(
			resolvedOrdNo,
			orderBase,
			amountSummary
		);
		ShopOrderCustomerInfoVO customerInfo = orderMapper.getShopOrderCustomerInfo(custNo);
		ShopOrderAddressVO pickupAddress = createShopOrderPickupAddress(custNo, orderBase);

		// 반품 신청 화면 응답 객체를 구성합니다.
		ShopMypageOrderReturnPageVO result = new ShopMypageOrderReturnPageVO();
		result.setOrder(orderGroup);
		result.setAmountSummary(amountSummary);
		result.setReasonList(reasonList);
		result.setSiteInfo(siteInfo);
		result.setReturnFeeContext(returnFeeContext);
		result.setAddressList(addressList);
		result.setPickupAddress(pickupAddress);
		result.setCustomerPhoneNumber(safeValue(firstNonBlank(trimToNull(customerInfo == null ? null : customerInfo.getPhoneNumber()), "")));
		return result;
	}

	// 쇼핑몰 마이페이지 반품 배송비 계산 컨텍스트를 구성합니다.
	private ShopMypageOrderReturnFeeContextVO buildShopMypageOrderReturnFeeContext(
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

		// 이력 여부와 현재 잔여 결제금액을 프론트 계산용 기본값으로 보정합니다.
		result.setHasPriorCompanyFaultReturnOrExchange(Boolean.TRUE.equals(result.getHasPriorCompanyFaultReturnOrExchange()));
		result.setHasPriorCustomerFaultReturnDeduction(Boolean.TRUE.equals(result.getHasPriorCustomerFaultReturnDeduction()));
		result.setCurrentRemainingFinalPayAmt((int) Math.min(
			resolveNonNegativeLong(amountSummary == null ? null : amountSummary.getFinalPayAmt()),
			Integer.MAX_VALUE
		));
		return result;
	}

	// 쇼핑몰 마이페이지 포인트 내역 페이지 데이터를 조회합니다.
	public ShopMypagePointPageVO getShopMypagePointPage(Long custNo, Integer requestedPageNo) {
		// 고객번호가 없으면 로그인 예외를 반환합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}

		// 요청 페이지 번호를 1 이상으로 보정합니다.
		int resolvedRequestedPageNo = normalizePage(requestedPageNo, 1);
		// 포인트 내역 전체 건수를 조회합니다.
		Integer pointCountResult = orderMapper.getShopMypagePointItemCount(custNo);
		int pointCount = pointCountResult == null ? 0 : pointCountResult;
		// 전체 페이지 수를 계산합니다.
		int totalPageCount = calculateTotalPageCount(pointCount, SHOP_MYPAGE_POINT_PAGE_SIZE);
		// 범위를 초과한 페이지 번호를 마지막 페이지로 보정합니다.
		int resolvedPageNo = resolvePageNoWithinRange(resolvedRequestedPageNo, totalPageCount);
		// 페이지 조회 오프셋을 계산합니다.
		int offset = calculateOffset(resolvedPageNo, SHOP_MYPAGE_POINT_PAGE_SIZE);

		// 포인트 내역 목록을 조회합니다.
		List<ShopMypagePointItemVO> pointList = orderMapper.getShopMypagePointItemList(custNo, SHOP_MYPAGE_POINT_PAGE_SIZE, offset);
		// 사용 가능 포인트 합계를 조회합니다.
		Integer availablePointAmt = orderMapper.getShopAvailablePointAmt(custNo);
		// 7일 이내 만료 예정 포인트 합계를 조회합니다.
		Integer expiringPointAmt = orderMapper.getShopMypageExpiringPointAmt(custNo);

		// 포인트 내역 페이지 응답 객체를 구성합니다.
		ShopMypagePointPageVO result = new ShopMypagePointPageVO();
		result.setAvailablePointAmt(availablePointAmt == null ? 0 : availablePointAmt);
		result.setExpiringPointAmt(expiringPointAmt == null ? 0 : expiringPointAmt);
		result.setPointList(pointList == null ? java.util.Collections.emptyList() : pointList);
		result.setPointCount(pointCount);
		result.setPageNo(resolvedPageNo);
		result.setPageSize(SHOP_MYPAGE_POINT_PAGE_SIZE);
		result.setTotalPageCount(totalPageCount);
		return result;
	}

	// 쇼핑몰 마이페이지 배송중 주문상품을 배송완료 처리합니다.
	@Transactional
	ShopOrderDetailStatusUpdateVO completeShopMypageOrderDelivery(ShopOrderDetailStatusUpdatePO param, Long custNo) {
		// 배송중 상품만 배송완료로 변경합니다.
		return applyShopMypageOrderDetailStatusChange(
			param,
			custNo,
			SHOP_ORDER_DTL_STAT_DELIVERING,
			SHOP_ORDER_DTL_STAT_DELIVERY_COMPLETE,
			SHOP_MYPAGE_ORDER_DELIVERY_COMPLETE_UNAVAILABLE_MESSAGE,
			true
		);
	}

	// 쇼핑몰 마이페이지 배송완료 주문상품을 구매확정 처리합니다.
	@Transactional
	public ShopOrderDetailStatusUpdateVO confirmShopMypageOrderPurchase(ShopOrderDetailStatusUpdatePO param, Long custNo) {
		// 배송완료 상품만 구매확정으로 변경합니다.
		return applyShopMypageOrderDetailStatusChange(
			param,
			custNo,
			SHOP_ORDER_DTL_STAT_DELIVERY_COMPLETE,
			SHOP_ORDER_DTL_STAT_PURCHASE_CONFIRM,
			SHOP_MYPAGE_ORDER_PURCHASE_CONFIRM_UNAVAILABLE_MESSAGE,
			false
		);
	}

	// 마이페이지 주문내역 조회 기간을 기본값 포함 유효한 기간으로 보정합니다.
	ShopMypageOrderDateRange resolveShopMypageOrderDateRange(String requestedStartDate, String requestedEndDate) {
		// 요청값이 없으면 기본 3개월 기간을 계산합니다.
		LocalDate today = LocalDate.now();
		LocalDate defaultStartDate = today.minusMonths(3L);
		LocalDate startDate = parseShopMypageOrderDate(firstNonBlank(trimToNull(requestedStartDate), defaultStartDate.toString()));
		LocalDate endDate = parseShopMypageOrderDate(firstNonBlank(trimToNull(requestedEndDate), today.toString()));

		// 시작일이 종료일보다 늦으면 조회 기간 예외를 반환합니다.
		if (startDate.isAfter(endDate)) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_DATE_INVALID_MESSAGE);
		}

		// 일자 문자열과 SQL 비교용 시작/종료 일시 문자열을 함께 구성합니다.
		return new ShopMypageOrderDateRange(
			startDate.toString(),
			endDate.toString(),
			startDate.atStartOfDay().format(SHOP_MYPAGE_ORDER_DATE_TIME_FORMATTER),
			endDate.plusDays(1L).atStartOfDay().format(SHOP_MYPAGE_ORDER_DATE_TIME_FORMATTER)
		);
	}

	// 마이페이지 주문내역 조회일 문자열을 LocalDate로 파싱합니다.
	private LocalDate parseShopMypageOrderDate(String value) {
		// 날짜 문자열이 비어 있으면 조회 기간 예외를 반환합니다.
		if (isBlank(value)) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_DATE_INVALID_MESSAGE);
		}
		try {
			return LocalDate.parse(value.trim());
		} catch (DateTimeParseException exception) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_DATE_INVALID_MESSAGE);
		}
	}

	// 주문번호 1건을 조회하고 주문상세 목록과 이미지 URL을 연결합니다.
	ShopMypageOrderGroupVO getShopMypageOrderGroupWithDetail(Long custNo, String ordNo) {
		// 로그인 고객의 주문번호 1건을 조회합니다.
		ShopMypageOrderGroupVO orderGroup = orderMapper.getShopMypageOrderGroup(custNo, ordNo);
		if (orderGroup == null) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_NOT_FOUND_MESSAGE);
		}

		// 주문상세 목록을 기존 묶음 로직으로 연결하고 노출 가능한 1건을 반환합니다.
		List<ShopMypageOrderGroupVO> resolvedOrderList = attachShopMypageOrderDetailList(List.of(orderGroup));
		if (resolvedOrderList.isEmpty()) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_NOT_FOUND_MESSAGE);
		}
		
		return resolvedOrderList.getFirst();
	}

	// 반품 신청 화면 진입 시 주문상세번호와 반품 가능 상태를 검증합니다.
	private void validateShopMypageOrderReturnAccess(ShopMypageOrderGroupVO orderGroup, Integer ordDtlNo) {
		// 주문상세 목록이 없으면 반품 불가 예외를 반환합니다.
		if (orderGroup == null || orderGroup.getDetailList() == null || orderGroup.getDetailList().isEmpty()) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_RETURN_UNAVAILABLE_MESSAGE);
		}

		// 반품 가능 상품 존재 여부와 요청 주문상세번호 유효성을 함께 확인합니다.
		boolean hasReturnApplyableDetail = false;
		boolean matchedRequestedDetail = ordDtlNo == null;
		for (ShopMypageOrderDetailItemVO detailItem : orderGroup.getDetailList()) {
			if (detailItem == null) {
				continue;
			}

			// 배송완료 상태이면서 진행중 클레임이 없는 상품만 반품 대상 후보로 인정합니다.
			boolean returnApplyable = isShopMypageOrderReturnApplyable(detailItem);
			if (returnApplyable) {
				hasReturnApplyableDetail = true;
			}

			// 요청 주문상세번호가 있으면 현재 주문의 반품 가능 상품과 일치하는지 확인합니다.
			if (ordDtlNo != null && ordDtlNo.equals(detailItem.getOrdDtlNo())) {
				if (!returnApplyable) {
					throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_DETAIL_ITEM_INVALID_MESSAGE);
				}
				matchedRequestedDetail = true;
			}
		}

		// 반품 가능한 상품이 없으면 반품 신청 화면 진입을 막습니다.
		if (!hasReturnApplyableDetail) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_RETURN_UNAVAILABLE_MESSAGE);
		}

		// 요청 주문상세번호가 현재 주문에 없으면 잘못된 접근으로 처리합니다.
		if (!matchedRequestedDetail) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_DETAIL_ITEM_INVALID_MESSAGE);
		}
	}

	// 주문상세 행이 반품신청 가능한 상태인지 반환합니다.
	private boolean isShopMypageOrderReturnApplyable(ShopMypageOrderDetailItemVO detailItem) {
		// 배송완료 상태, 잔여 수량, 진행중 클레임 차단 여부를 함께 확인합니다.
		if (detailItem == null) {
			return false;
		}
		return SHOP_ORDER_DTL_STAT_DELIVERY_COMPLETE.equals(detailItem.getOrdDtlStatCd())
			&& normalizeNonNegativeNumber(detailItem.getCancelableQty()) > 0
			&& Boolean.TRUE.equals(detailItem.getReturnApplyableYn());
	}

	// 마이페이지 주문상태 변경 요청을 검증하고 대상 주문상세를 반환합니다.
	private ShopMypageOrderDetailItemVO validateShopMypageOrderStatusActionTarget(
		ShopOrderDetailStatusUpdatePO param,
		Long custNo,
		String expectedOrdDtlStatCd,
		String invalidMessage
	) {
		// 로그인 고객번호와 요청 본문을 먼저 검증합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}
		if (param == null) {
			throw new IllegalArgumentException(invalidMessage);
		}

		// 주문번호와 주문상세번호를 정규화하고 현재 고객 주문인지 확인합니다.
		String ordNo = trimToNull(param.getOrdNo());
		Integer ordDtlNo = param.getOrdDtlNo();
		if (ordNo == null) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_NO_INVALID_MESSAGE);
		}
		if (ordDtlNo == null || ordDtlNo < 1) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_DETAIL_ITEM_INVALID_MESSAGE);
		}

		// 현재 로그인 고객의 주문상세 목록에서 대상 행을 찾고 상태를 검증합니다.
		ShopMypageOrderGroupVO orderGroup = getShopMypageOrderGroupWithDetail(custNo, ordNo);
		ShopMypageOrderDetailItemVO detailItem = findShopMypageOrderDetailItem(orderGroup, ordDtlNo);
		if (detailItem == null) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_DETAIL_ITEM_INVALID_MESSAGE);
		}
		if (!expectedOrdDtlStatCd.equals(detailItem.getOrdDtlStatCd())) {
			throw new IllegalArgumentException(invalidMessage);
		}
		return detailItem;
	}

	// 마이페이지 주문상태 변경 공통 로직을 수행합니다.
	private ShopOrderDetailStatusUpdateVO applyShopMypageOrderDetailStatusChange(
		ShopOrderDetailStatusUpdatePO param,
		Long custNo,
		String fromOrdDtlStatCd,
		String toOrdDtlStatCd,
		String invalidMessage,
		boolean updateDelvCompleteDt
	) {
		// 대상 주문상세를 검증하고 복합키 기준 상태 변경 대상을 구성합니다.
		ShopMypageOrderDetailItemVO detailItem = validateShopMypageOrderStatusActionTarget(
			param,
			custNo,
			fromOrdDtlStatCd,
			invalidMessage
		);
		AdminOrderStartDeliveryKeyItemPO statusChangeTarget = new AdminOrderStartDeliveryKeyItemPO();
		statusChangeTarget.setOrdNo(detailItem.getOrdNo());
		statusChangeTarget.setOrdDtlNo(detailItem.getOrdDtlNo());

		// 관리자 배송 상태 변경과 동일한 쿼리를 재사용해 실제 상태를 반영합니다.
		int updatedCount = updateOrderDetailStatusByKeyList(
			List.of(statusChangeTarget),
			fromOrdDtlStatCd,
			toOrdDtlStatCd,
			custNo,
			updateDelvCompleteDt
		);
		if (updatedCount != 1) {
			throw new IllegalArgumentException(invalidMessage);
		}

		// 변경 결과를 응답 객체에 담아 반환합니다.
		ShopOrderDetailStatusUpdateVO result = new ShopOrderDetailStatusUpdateVO();
		result.setOrdNo(detailItem.getOrdNo());
		result.setOrdDtlNo(detailItem.getOrdDtlNo());
		result.setOrdDtlStatCd(toOrdDtlStatCd);
		result.setUpdatedCount(updatedCount);
		return result;
	}

	// 관리자 주문반품 신청 화면 데이터를 조회합니다.
	AdminOrderReturnPageVO getAdminOrderReturnPage(String ordNo) {
		// 주문번호 필수 검증을 수행합니다.
		String resolvedOrdNo = trimToNull(ordNo);
		if (resolvedOrdNo == null) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_NO_INVALID_MESSAGE);
		}

		// 주문번호 기준 고객번호와 주문 그룹 정보를 조회합니다.
		Long custNo = orderMapper.getOrderCustNo(resolvedOrdNo);
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_NO_INVALID_MESSAGE);
		}
		ShopMypageOrderGroupVO orderGroup = getShopMypageOrderGroupWithDetail(custNo, resolvedOrdNo);
		if (orderGroup == null) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_NO_INVALID_MESSAGE);
		}

		// 관리자 반품 화면 구성에 필요한 금액/사유/배송 기준/회수지 데이터를 조합합니다.
		ShopOrderCancelOrderBaseVO orderBase = resolveShopOrderCancelOrderBase(custNo, resolvedOrdNo);
		List<ShopMypageOrderCancelReasonVO> reasonList = normalizeShopMypageOrderCancelReasonList(
			orderMapper.getShopMypageOrderReturnReasonList()
		);
		ShopCartSiteInfoVO siteInfo = resolveShopCartSiteInfo();
		ShopMypageOrderAmountSummaryVO amountSummary = buildShopOrderRemainingAmountSummary(orderGroup, orderBase, siteInfo);
		ShopMypageOrderReturnFeeContextVO returnFeeContext = buildShopMypageOrderReturnFeeContext(
			resolvedOrdNo,
			orderBase,
			amountSummary
		);
		ShopOrderAddressVO pickupAddress = createShopOrderPickupAddress(custNo, orderBase);

		// 관리자 반품 신청 화면 응답 객체를 구성합니다.
		AdminOrderReturnPageVO result = new AdminOrderReturnPageVO();
		result.setOrder(orderGroup);
		result.setAmountSummary(amountSummary);
		result.setReasonList(reasonList);
		result.setSiteInfo(siteInfo);
		result.setReturnFeeContext(returnFeeContext);
		result.setPickupAddress(pickupAddress);
		return result;
	}

	// 주문 그룹에서 지정한 주문상세번호 1건을 찾습니다.
	private ShopMypageOrderDetailItemVO findShopMypageOrderDetailItem(ShopMypageOrderGroupVO orderGroup, Integer ordDtlNo) {
		// 주문상세 목록이 없거나 주문상세번호가 없으면 조회하지 않습니다.
		if (orderGroup == null || orderGroup.getDetailList() == null || ordDtlNo == null) {
			return null;
		}

		// 동일 주문상세번호를 가진 행 1건을 반환합니다.
		for (ShopMypageOrderDetailItemVO detailItem : orderGroup.getDetailList()) {
			if (detailItem == null || detailItem.getOrdDtlNo() == null) {
				continue;
			}
			if (ordDtlNo.equals(detailItem.getOrdDtlNo())) {
				return detailItem;
			}
		}
		return null;
	}

	// 주문상세 복합키 목록 기준 상태 변경 쿼리를 재사용합니다.
	private int updateOrderDetailStatusByKeyList(
		List<AdminOrderStartDeliveryKeyItemPO> itemList,
		String fromOrdDtlStatCd,
		String toOrdDtlStatCd,
		Long udtNo,
		boolean updateDelvCompleteDt
	) {
		// 복합키 목록이 비어 있으면 변경할 대상이 없습니다.
		if (itemList == null || itemList.isEmpty()) {
			return 0;
		}
		return orderMapper.updateAdminOrderStartDeliveryStatusList(
			itemList,
			fromOrdDtlStatCd,
			toOrdDtlStatCd,
			udtNo,
			updateDelvCompleteDt
		);
	}

	// 주문번호 목록에 주문상세 목록을 묶어 주문내역 응답 구조를 완성합니다.
	private List<ShopMypageOrderGroupVO> attachShopMypageOrderDetailList(List<ShopMypageOrderGroupVO> orderList) {
		// 주문번호 목록이 없으면 빈 목록을 반환합니다.
		if (orderList == null || orderList.isEmpty()) {
			return List.of();
		}

		// 주문번호 순서를 유지하는 그룹 맵을 먼저 구성합니다.
		Map<String, ShopMypageOrderGroupVO> orderGroupMap = new LinkedHashMap<>();
		for (ShopMypageOrderGroupVO orderGroup : orderList) {
			if (orderGroup == null || isBlank(orderGroup.getOrdNo())) {
				continue;
			}
			orderGroup.setDetailList(new ArrayList<>());
			orderGroupMap.put(orderGroup.getOrdNo(), orderGroup);
		}
		if (orderGroupMap.isEmpty()) {
			return List.of();
		}

		// 주문번호 목록으로 주문상세를 조회한 뒤 이미지 URL을 보정합니다.
		List<ShopMypageOrderDetailItemVO> detailList = orderMapper.getShopMypageOrderDetailList(new ArrayList<>(orderGroupMap.keySet()));
		goodsImageService.applyShopMypageOrderDetailImageUrls(detailList);

		// 주문번호별 detailList에 주문상세 행을 순서대로 연결합니다.
		for (ShopMypageOrderDetailItemVO detailItem : detailList == null ? List.<ShopMypageOrderDetailItemVO>of() : detailList) {
			if (isBlank(detailItem.getOrdNo())) {
				continue;
			}
			if ("ORD_DTL_STAT_00".equals(detailItem.getOrdDtlStatCd())) {
				continue;
			}
			ShopMypageOrderGroupVO orderGroup = orderGroupMap.get(detailItem.getOrdNo());
			if (orderGroup == null) {
				continue;
			}
			orderGroup.getDetailList().add(detailItem);
		}

		// 조회 결과에 노출할 주문상세가 1건 이상 있는 주문번호만 반환합니다.
		List<ShopMypageOrderGroupVO> result = new ArrayList<>();
		for (ShopMypageOrderGroupVO orderGroup : orderGroupMap.values()) {
			if (orderGroup == null || orderGroup.getDetailList() == null || orderGroup.getDetailList().isEmpty()) {
				continue;
			}
			result.add(orderGroup);
		}
		return result;
	}

	// 마이페이지 주문상세 금액 요약을 현재 남은 주문 기준으로 계산합니다.
	ShopMypageOrderAmountSummaryVO buildShopMypageOrderAmountSummary(Long custNo, ShopMypageOrderGroupVO orderGroup) {
		// 주문 마스터/배송 기준과 현재 남은 주문상세 목록으로 금액 요약을 계산합니다.
		ShopOrderCancelOrderBaseVO orderBase = resolveShopOrderCancelOrderBase(custNo, orderGroup == null ? null : orderGroup.getOrdNo());
		ShopCartSiteInfoVO siteInfo = resolveShopCartSiteInfo();
		return buildShopOrderRemainingAmountSummary(orderGroup, orderBase, siteInfo);
	}

	// 마이페이지 주문상세 금액 요약 응답의 null 값을 0 이상으로 보정합니다.
	private ShopMypageOrderAmountSummaryVO normalizeShopMypageOrderAmountSummary(ShopMypageOrderAmountSummaryVO amountSummary) {
		// 금액 요약 응답이 없으면 0 기본값 객체를 생성합니다.
		ShopMypageOrderAmountSummaryVO result = amountSummary == null ? new ShopMypageOrderAmountSummaryVO() : amountSummary;
		result.setTotalSupplyAmt(resolveNonNegativeLong(result.getTotalSupplyAmt()));
		result.setTotalOrderAmt(resolveNonNegativeLong(result.getTotalOrderAmt()));
		result.setTotalGoodsDiscountAmt(resolveNonNegativeLong(result.getTotalGoodsDiscountAmt()));
		result.setTotalGoodsCouponDiscountAmt(resolveNonNegativeLong(result.getTotalGoodsCouponDiscountAmt()));
		result.setTotalCartCouponDiscountAmt(resolveNonNegativeLong(result.getTotalCartCouponDiscountAmt()));
		result.setTotalCouponDiscountAmt(resolveNonNegativeLong(result.getTotalCouponDiscountAmt()));
		result.setTotalPointUseAmt(resolveNonNegativeLong(result.getTotalPointUseAmt()));
		result.setDeliveryFeeAmt(resolveNonNegativeLong(result.getDeliveryFeeAmt()));
		result.setDeliveryCouponDiscountAmt(resolveNonNegativeLong(result.getDeliveryCouponDiscountAmt()));
		result.setFinalPayAmt(resolveNonNegativeLong(result.getFinalPayAmt()));
		return result;
	}

	// 주문취소 처리용 주문 마스터 정보를 조회합니다.
	ShopOrderCancelOrderBaseVO resolveShopOrderCancelOrderBase(Long custNo, String ordNo) {
		// 고객 주문번호 기준 주문 마스터가 없으면 주문 미존재 예외를 반환합니다.
		ShopOrderCancelOrderBaseVO orderBase = orderMapper.getShopOrderCancelOrderBase(custNo, ordNo);
		if (orderBase == null) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_NOT_FOUND_MESSAGE);
		}
		return orderBase;
	}

	// 주문 당시 배송지 정보를 반품 회수지 기본값 형식으로 변환합니다.
	private ShopOrderAddressVO createShopOrderPickupAddress(Long custNo, ShopOrderCancelOrderBaseVO orderBase) {
		// 주문 배송지 정보가 없으면 기본 회수지를 생성하지 않습니다.
		if (orderBase == null) {
			return null;
		}
		String receiverName = trimToNull(orderBase.getRcvNm());
		String postNo = trimToNull(orderBase.getRcvPostNo());
		String baseAddress = trimToNull(orderBase.getRcvAddrBase());
		String detailAddress = trimToNull(orderBase.getRcvAddrDtl());
		if (receiverName == null && postNo == null && baseAddress == null && detailAddress == null) {
			return null;
		}

		// 주문 배송지 노출용 회수지 객체를 구성합니다.
		ShopOrderAddressVO result = new ShopOrderAddressVO();
		result.setCustNo(custNo);
		result.setAddressNm("주문 배송지");
		result.setPostNo(postNo == null ? "" : postNo);
		result.setBaseAddress(baseAddress == null ? "" : baseAddress);
		result.setDetailAddress(detailAddress == null ? "" : detailAddress);
		result.setPhoneNumber("");
		result.setRsvNm(receiverName == null ? "" : receiverName);
		result.setDefaultYn(NO);
		return result;
	}

	// 현재 남아 있는 주문수량 기준으로 주문 금액 요약을 계산합니다.
	public ShopMypageOrderAmountSummaryVO buildShopOrderRemainingAmountSummary(
		ShopMypageOrderGroupVO orderGroup,
		ShopOrderCancelOrderBaseVO orderBase,
		ShopCartSiteInfoVO siteInfo
	) {
		// 주문상세별 남은 수량과 ORDER_DETAIL에 남아 있는 현재 할인금액을 누적합니다.
		ShopMypageOrderAmountSummaryVO amountSummary = normalizeShopMypageOrderAmountSummary(new ShopMypageOrderAmountSummaryVO());
		long currentOrderAmt = 0L;
		for (ShopMypageOrderDetailItemVO detailItem : orderGroup == null ? List.<ShopMypageOrderDetailItemVO>of() : orderGroup.getDetailList()) {
			int remainingQty = resolveShopOrderRemainingQty(detailItem);
			if (remainingQty < 1) {
				continue;
			}

			// 현재 남은 주문상품 금액과 현재 잔여 할인금액을 더합니다.
			long rowSupplyAmt = (long) normalizeNonNegativeNumber(detailItem.getSupplyAmt()) * remainingQty;
			long rowOrderAmt = (long) resolveShopOrderUnitOrderAmt(detailItem) * remainingQty;
			currentOrderAmt += rowOrderAmt;
			amountSummary.setTotalSupplyAmt(amountSummary.getTotalSupplyAmt() + rowSupplyAmt);
			amountSummary.setTotalOrderAmt(amountSummary.getTotalOrderAmt() + rowOrderAmt);
			amountSummary.setTotalGoodsCouponDiscountAmt(
				amountSummary.getTotalGoodsCouponDiscountAmt() + normalizeNonNegativeNumber(detailItem.getGoodsCouponDiscountAmt())
			);
			amountSummary.setTotalCartCouponDiscountAmt(
				amountSummary.getTotalCartCouponDiscountAmt() + normalizeNonNegativeNumber(detailItem.getCartCouponDiscountAmt())
			);
			amountSummary.setTotalPointUseAmt(
				amountSummary.getTotalPointUseAmt() + normalizeNonNegativeNumber(detailItem.getPointUseAmt())
			);
		}

		// 주문 마스터 배송비와 무료배송 해제 규칙을 반영해 현재 남은 주문 금액을 계산합니다.
		long totalGoodsDiscountAmt = Math.max(amountSummary.getTotalSupplyAmt() - amountSummary.getTotalOrderAmt(), 0L);
		long totalCouponDiscountAmt = amountSummary.getTotalGoodsCouponDiscountAmt() + amountSummary.getTotalCartCouponDiscountAmt();
		long originalBaseDeliveryFee = resolveNonNegativeLong(orderBase == null ? null : (long) normalizeNonNegativeNumber(orderBase.getOrdDelvAmt()));
		long originalDeliveryCouponDiscountAmt = resolveNonNegativeLong(orderBase == null ? null : (long) normalizeNonNegativeNumber(orderBase.getDelvCpnDcAmt()));
		long remainingBaseDeliveryFee =
			currentOrderAmt < 1L
				? 0L
				: originalBaseDeliveryFee > 0L
					? originalBaseDeliveryFee
					: currentOrderAmt < resolveNonNegativeLong(siteInfo == null ? null : (long) normalizeNonNegativeNumber(siteInfo.getDeliveryFeeLimit()))
						? resolveNonNegativeLong(siteInfo == null ? null : (long) normalizeNonNegativeNumber(siteInfo.getDeliveryFee()))
						: 0L;
		long remainingDeliveryCouponDiscountAmt =
			currentOrderAmt < 1L || remainingBaseDeliveryFee < 1L
				? 0L
				: Math.min(originalDeliveryCouponDiscountAmt, remainingBaseDeliveryFee);

		// 화면에 표시할 현재 남은 주문 금액 요약 필드를 완성합니다.
		amountSummary.setTotalGoodsDiscountAmt(totalGoodsDiscountAmt);
		amountSummary.setTotalCouponDiscountAmt(totalCouponDiscountAmt);
		amountSummary.setDeliveryFeeAmt(remainingBaseDeliveryFee);
		amountSummary.setDeliveryCouponDiscountAmt(remainingDeliveryCouponDiscountAmt);
		amountSummary.setFinalPayAmt(
			Math.max(
				amountSummary.getTotalOrderAmt()
					+ remainingBaseDeliveryFee
					- totalCouponDiscountAmt
					- remainingDeliveryCouponDiscountAmt
					- amountSummary.getTotalPointUseAmt(),
				0L
			)
		);
		return normalizeShopMypageOrderAmountSummary(amountSummary);
	}

	// 관리자 주문상세번호 목록을 중복 제거된 양수 목록으로 정규화합니다.
	private List<Integer> normalizeAdminOrderDetailNoList(List<Integer> ordDtlNoList) {
		// null/음수/0 값은 제거하고 입력 순서는 유지합니다.
		if (ordDtlNoList == null || ordDtlNoList.isEmpty()) {
			return List.of();
		}

		Set<Integer> normalizedSet = new LinkedHashSet<>();
		for (Integer ordDtlNo : ordDtlNoList) {
			if (ordDtlNo != null && ordDtlNo > 0) {
				normalizedSet.add(ordDtlNo);
			}
		}
		return List.copyOf(normalizedSet);
	}

	// 주문상세의 원주문 수량을 안전한 정수로 반환합니다.
	private int resolveShopOrderOriginalQty(ShopMypageOrderDetailItemVO detailItem) {
		// 원주문 수량이 없거나 음수면 0으로 보정합니다.
		return normalizeNonNegativeNumber(detailItem == null ? null : detailItem.getOrdQty());
	}

	// 주문상세의 현재 남은 수량을 원주문 수량 범위 안에서 반환합니다.
	int resolveShopOrderRemainingQty(ShopMypageOrderDetailItemVO detailItem) {
		// 현재 남은 수량은 원주문 수량을 넘지 않도록 보정합니다.
		int originalQty = resolveShopOrderOriginalQty(detailItem);
		int remainingQty = normalizeNonNegativeNumber(detailItem == null ? null : detailItem.getCancelableQty());
		return Math.min(originalQty, remainingQty);
	}

	// 주문상세의 상품 판매가 단가를 반환합니다.
	int resolveShopOrderUnitOrderAmt(ShopMypageOrderDetailItemVO detailItem) {
		// 판매가와 추가금액을 더해 주문상세 1개당 주문금액을 계산합니다.
		return normalizeNonNegativeNumber(detailItem == null ? null : detailItem.getSaleAmt())
			+ normalizeNonNegativeNumber(detailItem == null ? null : detailItem.getAddAmt());
	}

	// 주문상세의 누적 배분 금액을 누적 수량 기준으로 계산합니다.
	private long resolveShopOrderCumulativeAllocatedAmt(Integer allocatedAmt, int originalQty, int cumulativeQty) {
		// 마지막 수량까지 모두 취소되면 남은 절삭 금액을 전부 배분합니다.
		long safeAllocatedAmt = normalizeNonNegativeNumber(allocatedAmt);
		int safeOriginalQty = Math.max(originalQty, 0);
		int safeCumulativeQty = Math.clamp(cumulativeQty, 0, safeOriginalQty);
		if (safeAllocatedAmt < 1L || safeOriginalQty < 1 || safeCumulativeQty < 1) {
			return 0L;
		}
		if (safeCumulativeQty >= safeOriginalQty) {
			return safeAllocatedAmt;
		}
		return (safeAllocatedAmt * safeCumulativeQty) / safeOriginalQty;
	}

	// 주문상세의 이번 취소분 배분 금액을 계산합니다.
	long resolveShopOrderIncrementAllocatedAmt(Integer allocatedAmt, int originalQty, int canceledBeforeQty, int cancelQty) {
		// 누적 취소 전/후 배분 차이만큼 이번 취소분 환급 금액을 계산합니다.
		long beforeAmt = resolveShopOrderCumulativeAllocatedAmt(allocatedAmt, originalQty, canceledBeforeQty);
		long afterAmt = resolveShopOrderCumulativeAllocatedAmt(allocatedAmt, originalQty, canceledBeforeQty + cancelQty);
		return Math.max(afterAmt - beforeAmt, 0L);
	}

	// 주문취소 사유 코드 목록의 공백/null 값을 기본값으로 보정합니다.
	List<ShopMypageOrderCancelReasonVO> normalizeShopMypageOrderCancelReasonList(
		List<ShopMypageOrderCancelReasonVO> reasonList
	) {
		// 조회 결과가 없으면 빈 목록을 반환합니다.
		if (reasonList == null || reasonList.isEmpty()) {
			return List.of();
		}

		// 공백 코드와 코드명은 제외하고 안전한 목록만 반환합니다.
		List<ShopMypageOrderCancelReasonVO> result = new ArrayList<>();
		for (ShopMypageOrderCancelReasonVO reason : reasonList) {
			if (reason == null || isBlank(reason.getCd()) || isBlank(reason.getCdNm())) {
				continue;
			}
			ShopMypageOrderCancelReasonVO normalizedReason = new ShopMypageOrderCancelReasonVO();
			normalizedReason.setCd(reason.getCd().trim());
			normalizedReason.setCdNm(reason.getCdNm().trim());
			result.add(normalizedReason);
		}
		return result;
	}

	// 마이페이지 주문내역 상태 요약 응답의 null 값을 0으로 보정합니다.
	private ShopMypageOrderStatusSummaryVO normalizeShopMypageOrderStatusSummary(ShopMypageOrderStatusSummaryVO statusSummary) {
		// 상태 요약 응답이 없으면 0 기본값 객체를 생성합니다.
		ShopMypageOrderStatusSummaryVO result = statusSummary == null ? new ShopMypageOrderStatusSummaryVO() : statusSummary;
		result.setWaitingForDepositCount(result.getWaitingForDepositCount() == null ? 0 : result.getWaitingForDepositCount());
		result.setPaymentCompletedCount(result.getPaymentCompletedCount() == null ? 0 : result.getPaymentCompletedCount());
		result.setProductPreparingCount(result.getProductPreparingCount() == null ? 0 : result.getProductPreparingCount());
		result.setDeliveryPreparingCount(result.getDeliveryPreparingCount() == null ? 0 : result.getDeliveryPreparingCount());
		result.setShippingCount(result.getShippingCount() == null ? 0 : result.getShippingCount());
		result.setDeliveryCompletedCount(result.getDeliveryCompletedCount() == null ? 0 : result.getDeliveryCompletedCount());
		result.setPurchaseConfirmedCount(result.getPurchaseConfirmedCount() == null ? 0 : result.getPurchaseConfirmedCount());
		return result;
	}

	// 마이페이지 주문상세 최종 결제 금액을 결제정보 우선 규칙으로 계산합니다.
	private long resolveShopMypageOrderFinalPayAmt(
		ShopMypageOrderAmountSummaryVO amountSummary,
		ShopOrderPaymentVO payment
	) {
		// 승인금액 또는 결제금액이 있으면 우선 사용합니다.
		if (payment != null) {
			if (payment.getAprvAmt() != null) {
				return resolveNonNegativeLong(payment.getAprvAmt());
			}
			if (payment.getPayAmt() != null) {
				return resolveNonNegativeLong(payment.getPayAmt());
			}
		}

		// 결제 정보가 없으면 계산식 기준으로 최종 결제 금액을 보정합니다.
		long calculatedFinalPayAmt =
			amountSummary.getTotalOrderAmt()
				+ amountSummary.getDeliveryFeeAmt()
				- amountSummary.getTotalCouponDiscountAmt()
				- amountSummary.getDeliveryCouponDiscountAmt()
				- amountSummary.getTotalPointUseAmt();
		return Math.max(calculatedFinalPayAmt, 0L);
	}

	// Long 금액값을 0 이상의 안전한 값으로 보정합니다.
	long resolveNonNegativeLong(Long value) {
		if (value == null || value < 0L) {
			return 0L;
		}
		return value;
	}

	// 쇼핑몰 주문서 페이지 데이터를 cartId 기준으로 조회합니다.
	public ShopOrderPageVO getShopOrderPage(List<Long> cartIdList, Long custNo, String deviceGbCd, String shopOrigin) {
		// 로그인 고객번호 유효성을 확인합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}

		// 현재 로그인 고객의 유효한 주문 대상 장바구니 목록을 조회합니다.
		List<ShopCartItemVO> orderCartItemList = resolveValidatedShopOrderCartItemList(cartIdList, custNo);
		validateShopOrderStock(orderCartItemList);
		return buildShopOrderPage(orderCartItemList, custNo, deviceGbCd, shopOrigin);
	}

	// 쇼핑몰 주문서 할인 금액을 재계산합니다.
	public ShopOrderDiscountQuoteVO quoteShopOrderDiscount(ShopOrderDiscountQuotePO param, Long custNo) {
		// 로그인 고객번호와 재계산 입력값 유효성을 확인합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}
		if (param == null) {
			throw new IllegalArgumentException(SHOP_ORDER_DISCOUNT_INVALID_MESSAGE);
		}

		// 주문 대상 장바구니와 할인 계산 컨텍스트를 조회합니다.
		List<ShopCartItemVO> orderCartItemList = resolveValidatedShopOrderCartItemList(param.getCartIdList(), custNo);
		ShopOrderDiscountContext discountContext = buildShopOrderDiscountContext(orderCartItemList, custNo);
		return buildShopOrderDiscountQuoteFromSelection(discountContext, param.getGoodsCouponSelectionList(), param.getCartCouponCustCpnNo(), param.getDeliveryCouponCustCpnNo());
	}

	// 쇼핑몰 주문 결제 준비 데이터를 생성하고 Toss 결제 요청 정보를 반환합니다.
	@Transactional
	public ShopOrderPaymentPrepareVO prepareShopOrderPayment(
		ShopOrderPaymentPreparePO param,
		Long custNo,
		String deviceGbCd,
		String shopOrigin
	) {
		// 로그인 고객번호와 결제 준비 요청값을 검증합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}
		if (param == null) {
			throw new IllegalArgumentException(SHOP_ORDER_PAYMENT_INVALID_MESSAGE);
		}

		// 현재 유효한 주문 대상 장바구니와 고객/배송지 정보를 조회합니다.
		List<ShopCartItemVO> orderCartItemList = resolveValidatedShopOrderCartItemList(param.getCartIdList(), custNo);
		validateShopOrderStock(orderCartItemList);
		ShopOrderAddressVO selectedAddress = resolveRequiredShopOrderAddress(custNo, param.getAddressNm());
		ShopOrderCustomerInfoVO customerInfo = resolveShopOrderCustomerInfo(custNo, deviceGbCd);
		ShopOrderDiscountContext discountContext = buildShopOrderDiscountContext(orderCartItemList, custNo);

		// 현재 장바구니 기준 할인 선택을 다시 검증하고 포인트 사용 금액을 보정합니다.
		ShopOrderDiscountSelectionVO requestSelection = param.getDiscountSelection() == null ? new ShopOrderDiscountSelectionVO() : param.getDiscountSelection();
		ShopOrderDiscountQuoteVO discountQuote = buildShopOrderDiscountQuoteFromSelection(
			discountContext,
			requestSelection.getGoodsCouponSelectionList(),
			requestSelection.getCartCouponCustCpnNo(),
			requestSelection.getDeliveryCouponCustCpnNo()
		);
		int normalizedPointUseAmt = clampShopOrderPointUseAmt(param.getPointUseAmt(), discountQuote.getDiscountAmount().getMaxPointUseAmt());

		// 결제금액, 주문명, 결제수단, 적립 예정 포인트를 최종 확정합니다.
		int totalSaleAmt = calculateSelectedCartSaleAmt(discountContext.estimateRowList());
		int baseDeliveryFee = resolveCouponEstimateDeliveryFee(totalSaleAmt, discountContext.siteInfo());
		int deliveryCouponDiscountAmt = normalizeNonNegativeNumber(discountQuote.getDiscountAmount().getDeliveryCouponDiscountAmt());
		int finalDeliveryFee = Math.max(baseDeliveryFee - deliveryCouponDiscountAmt, 0);
		int finalPayAmt = Math.max(
			totalSaleAmt + baseDeliveryFee - normalizeNonNegativeNumber(discountQuote.getDiscountAmount().getCouponDiscountAmt()) - normalizedPointUseAmt,
			0
		);
		if (finalPayAmt < 1) {
			throw new IllegalArgumentException("결제 금액을 확인해주세요.");
		}
		String resolvedPaymentMethodCd = resolveRequiredShopOrderPaymentMethodCd(param.getPaymentMethodCd());
		ShopOrderRefundAccountInfo refundAccountInfo = resolveShopOrderRefundAccountInfo(param, resolvedPaymentMethodCd);
		String tossMethod = resolveTossMethodByPayMethodCd(resolvedPaymentMethodCd);
		String ordNo = generateShopOrderNo(custNo);
		String orderName = buildShopOrderName(orderCartItemList);
		ShopOrderPointSaveSummaryVO pointSaveSummary = buildShopOrderPointSaveSummary(orderCartItemList, customerInfo.getCustGradeCd());

		// 주문 마스터/상세와 결제 준비 스냅샷을 생성합니다.
		ShopOrderBaseSavePO orderBaseSavePO = buildShopOrderBaseSavePO(
			param.getFrom(),
			ordNo,
			custNo,
			selectedAddress,
			discountQuote.getDiscountSelection().getDeliveryCouponCustCpnNo(),
			baseDeliveryFee,
			deliveryCouponDiscountAmt,
			deviceGbCd,
			refundAccountInfo
		);
		Map<String, Object> paymentSnapshot = buildShopOrderPaymentSnapshot(
			param.getFrom(),
			param.getGoodsId(),
			orderCartItemList,
			selectedAddress.getAddressNm(),
			discountQuote.getDiscountSelection(),
			normalizedPointUseAmt,
			orderName,
			finalPayAmt,
			refundAccountInfo
		);
		String paymentSnapshotJson = writeShopOrderJson(paymentSnapshot);
		insertShopOrderBaseAndDetail(
			orderBaseSavePO,
			custNo,
			orderCartItemList,
			discountQuote,
			normalizedPointUseAmt,
			pointSaveSummary.getPointSaveRate(),
			orderBaseSavePO.getRegNo()
		);

		// 결제 준비 row를 생성하고 Toss 결제창 요청 응답을 반환합니다.
		ShopOrderPaymentSavePO paymentSavePO = new ShopOrderPaymentSavePO();
		paymentSavePO.setOrdNo(ordNo);
		paymentSavePO.setCustNo(custNo);
		paymentSavePO.setPayStatCd(SHOP_ORDER_PAY_STAT_READY);
		paymentSavePO.setPayGbCd(SHOP_ORDER_PAY_GB_PAYMENT);
		paymentSavePO.setPayMethodCd(resolvedPaymentMethodCd);
		paymentSavePO.setOrdGbCd(SHOP_ORDER_ORD_GB_ORDER);
		paymentSavePO.setPgGbCd(SHOP_ORDER_PG_GB_TOSS);
		paymentSavePO.setPayAmt((long) finalPayAmt);
		paymentSavePO.setDeviceGbCd(deviceGbCd);
		paymentSavePO.setReqRawJson(paymentSnapshotJson);
		paymentSavePO.setRegNo(custNo);
		paymentSavePO.setUdtNo(custNo);
		orderMapper.insertShopPayment(paymentSavePO);
		if (paymentSavePO.getPayNo() == null || paymentSavePO.getPayNo() < 1L) {
			throw new IllegalStateException(SHOP_ORDER_PAYMENT_PREPARE_MESSAGE);
		}

		// 결제창 성공/실패 URL과 고객 정보를 함께 응답합니다.
		String normalizedShopOrigin = normalizeShopOrigin(shopOrigin);
		ShopOrderPaymentPrepareVO result = new ShopOrderPaymentPrepareVO();
		result.setOrdNo(ordNo);
		result.setPayNo(paymentSavePO.getPayNo());
		result.setClientKey(resolveShopOrderClientKey());
		result.setMethod(tossMethod);
		result.setOrderId(ordNo);
		result.setOrderName(orderName);
		result.setAmount((long) finalPayAmt);
		result.setCustomerKey(customerInfo.getCustomerKey());
		result.setCustomerName(customerInfo.getCustNm());
		result.setCustomerEmail(customerInfo.getEmail());
		result.setCustomerMobilePhone(customerInfo.getPhoneNumber());
		result.setSuccessUrl(buildShopOrderSuccessUrl(normalizedShopOrigin, paymentSavePO.getPayNo()));
		result.setFailUrl(buildShopOrderFailUrl(normalizedShopOrigin, paymentSavePO.getPayNo(), param.getFrom(), param.getGoodsId(), orderCartItemList));
		return result;
	}

	// 쇼핑몰 주문 결제 승인을 완료하고 주문/결제 상태를 갱신합니다.
	@Transactional
	public ShopOrderPaymentConfirmVO confirmShopOrderPayment(ShopOrderPaymentConfirmPO param, Long custNo) {
		// 로그인 고객번호와 승인 요청값을 검증합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}
		if (param == null || param.getPayNo() == null || isBlank(param.getOrdNo()) || isBlank(param.getPaymentKey()) || param.getAmount() == null) {
			throw new IllegalArgumentException(SHOP_ORDER_PAYMENT_INVALID_MESSAGE);
		}

		// 현재 결제번호 기준 결제 준비 상태와 고객 소유 여부를 확인합니다.
		ShopOrderPaymentVO payment = orderMapper.getShopPaymentByPayNo(param.getPayNo());
		if (payment == null || payment.getCustNo() == null || !custNo.equals(payment.getCustNo()) || !param.getOrdNo().trim().equals(payment.getOrdNo())) {
			throw new IllegalArgumentException(SHOP_ORDER_PAYMENT_INVALID_MESSAGE);
		}
		if (payment.getPayAmt() == null || payment.getPayAmt().longValue() != param.getAmount().longValue()) {
			throw new IllegalArgumentException("승인 금액을 확인해주세요.");
		}

		// 이미 승인 또는 입금대기 처리된 결제는 저장된 결과를 그대로 반환합니다.
		if (SHOP_ORDER_PAY_STAT_DONE.equals(payment.getPayStatCd()) || SHOP_ORDER_PAY_STAT_WAITING_DEPOSIT.equals(payment.getPayStatCd())) {
			return buildShopOrderPaymentConfirmResult(payment);
		}

		// 승인 요청 전 재고 차감과 쿠폰/포인트/장바구니 후처리를 먼저 수행합니다.
		reserveShopOrderStock(payment.getOrdNo(), custNo);
		applyShopOrderSuccessSideEffects(payment, custNo);

		// Toss 승인 API를 호출하고 상태/수단별 후속 처리를 수행합니다.
		String rawResponse;
		try {
			rawResponse = confirmTossPayment(param);
		} catch (TossPaymentClientException exception) {
			JsonNode errorNode = readShopOrderJsonNode(exception.getResponseBody());
			String errorCode = firstNonBlank(resolveJsonText(errorNode, "code"), "TOSS_CONFIRM_ERROR");
			String errorMessage = firstNonBlank(resolveJsonText(errorNode, "message"), SHOP_ORDER_PAYMENT_CONFIRM_MESSAGE);
			orderMapper.updateShopPaymentFailure(
				payment.getPayNo(),
				SHOP_ORDER_PAY_STAT_FAIL,
				errorCode,
				errorMessage,
				exception.getResponseBody(),
				custNo
			);
			orderMapper.updateShopOrderBaseStatus(param.getOrdNo().trim(), SHOP_ORDER_STAT_CANCEL, custNo);
			orderMapper.updateShopOrderDetailStatus(param.getOrdNo().trim(), SHOP_ORDER_DTL_STAT_CANCEL, custNo);
			throw new IllegalArgumentException(errorMessage);
		}
		JsonNode responseNode = readShopOrderJsonNode(rawResponse);
		String paymentStatus = resolveJsonText(responseNode, "status");
		String paymentKey = resolveJsonText(responseNode, "paymentKey");
		String paymentKeyHash = sha256Hex(paymentKey);
		String tradeNo = firstNonBlank(resolveJsonText(responseNode, "lastTransactionKey"), paymentKey);
		String approvedAt = normalizeShopOrderDateTime(resolveJsonText(responseNode, "approvedAt"));
		String orderName = firstNonBlank(resolveJsonText(responseNode, "orderName"), readShopOrderSnapshotValue(payment.getReqRawJson(), "orderName"));

		// 무통장입금 발급 성공과 일반 승인 성공을 분기 처리합니다.
		if (SHOP_ORDER_PAYMENT_METHOD_VIRTUAL_ACCOUNT.equals(payment.getPayMethodCd()) && "WAITING_FOR_DEPOSIT".equals(paymentStatus)) {
			String dueDate = normalizeShopOrderDateTime(resolveJsonText(responseNode.path("virtualAccount"), "dueDate"));
			orderMapper.updateShopPaymentWaitingDeposit(
				payment.getPayNo(),
				SHOP_ORDER_PAY_STAT_WAITING_DEPOSIT,
				param.getAmount(),
				paymentKey,
				paymentKeyHash,
				tradeNo,
				paymentStatus,
				"무통장입금 발급 완료",
				resolveJsonText(responseNode.path("virtualAccount"), "bankCode"),
				resolveJsonText(responseNode.path("virtualAccount"), "accountNumber"),
				resolveJsonText(responseNode.path("virtualAccount"), "customerName"),
				dueDate,
				rawResponse,
				approvedAt,
				custNo
			);
			orderMapper.updateShopOrderBaseStatusAndDates(
				param.getOrdNo().trim(),
				SHOP_ORDER_STAT_WAITING_DEPOSIT,
				approvedAt,
				null,
				custNo
			);
			orderMapper.updateShopOrderDetailStatus(param.getOrdNo().trim(), SHOP_ORDER_DTL_STAT_WAITING_DEPOSIT, custNo);
			ShopOrderPaymentVO updatedPayment = orderMapper.getShopPaymentByPayNo(payment.getPayNo());
			ShopOrderPaymentConfirmVO result = buildShopOrderPaymentConfirmResult(updatedPayment);
			result.setOrderName(orderName);
			return result;
		}

		if (!"DONE".equals(paymentStatus)) {
			throw new IllegalArgumentException(SHOP_ORDER_PAYMENT_CONFIRM_MESSAGE);
		}

		// 카드/퀵계좌이체 승인 성공 정보를 저장하고 후처리를 수행합니다.
		orderMapper.updateShopPaymentSuccess(
			payment.getPayNo(),
			SHOP_ORDER_PAY_STAT_DONE,
			param.getAmount(),
			paymentKey,
			paymentKeyHash,
			tradeNo,
			resolveJsonText(responseNode.path("card"), "approveNo"),
			paymentStatus,
			"결제 승인 완료",
			firstNonBlank(resolveJsonText(responseNode.path("card"), "issuerCode"), resolveJsonText(responseNode.path("easyPay"), "provider")),
			resolveJsonText(responseNode.path("card"), "number"),
			rawResponse,
			approvedAt,
			custNo
		);
		orderMapper.updateShopOrderBaseStatusAndDates(
			param.getOrdNo().trim(),
			SHOP_ORDER_STAT_DONE,
			approvedAt,
			approvedAt,
			custNo
		);
		orderMapper.updateShopOrderDetailStatus(param.getOrdNo().trim(), SHOP_ORDER_DTL_STAT_DONE, custNo);
		ShopOrderPaymentVO updatedPayment = orderMapper.getShopPaymentByPayNo(payment.getPayNo());
		ShopOrderPaymentConfirmVO result = buildShopOrderPaymentConfirmResult(updatedPayment);
		result.setOrderName(orderName);
		return result;
	}

	// 쇼핑몰 주문 결제 실패/취소 결과를 저장합니다.
	@Transactional
	public void failShopOrderPayment(ShopOrderPaymentFailPO param, Long custNo) {
		// 로그인 고객번호와 실패 요청값을 검증합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}
		if (param == null || param.getPayNo() == null || isBlank(param.getOrdNo())) {
			throw new IllegalArgumentException(SHOP_ORDER_PAYMENT_INVALID_MESSAGE);
		}

		// 현재 결제 준비 row가 로그인 고객 소유인지 확인합니다.
		ShopOrderPaymentVO payment = orderMapper.getShopPaymentByPayNo(param.getPayNo());
		if (payment == null || payment.getCustNo() == null || !custNo.equals(payment.getCustNo()) || !param.getOrdNo().trim().equals(payment.getOrdNo())) {
			throw new IllegalArgumentException(SHOP_ORDER_PAYMENT_INVALID_MESSAGE);
		}

		// 이미 성공 또는 입금대기 상태가 아니면 실패/취소 상태와 주문 취소 상태를 반영합니다.
		if (SHOP_ORDER_PAY_STAT_DONE.equals(payment.getPayStatCd()) || SHOP_ORDER_PAY_STAT_WAITING_DEPOSIT.equals(payment.getPayStatCd())) {
			return;
		}
		String payStatCd = isShopOrderCancelFailureCode(param.getCode()) ? SHOP_ORDER_PAY_STAT_CANCEL : SHOP_ORDER_PAY_STAT_FAIL;
		orderMapper.updateShopPaymentFailure(
			payment.getPayNo(),
			payStatCd,
			trimToNull(param.getCode()),
			trimToNull(param.getMessage()),
			writeShopOrderJson(Map.of(
				"code", safeValue(firstNonBlank(trimToNull(param.getCode()), "")),
				"message", safeValue(firstNonBlank(trimToNull(param.getMessage()), ""))
			)),
			custNo
		);
		orderMapper.updateShopOrderBaseStatus(param.getOrdNo().trim(), SHOP_ORDER_STAT_CANCEL, custNo);
		orderMapper.updateShopOrderDetailStatus(param.getOrdNo().trim(), SHOP_ORDER_DTL_STAT_CANCEL, custNo);
	}

	// Toss 웹훅 결과를 반영해 무통장입금 입금완료/만료/취소 후속 처리를 수행합니다.
	@Transactional
	public void handleShopOrderPaymentWebhook(String rawBody) {
		// 웹훅 본문이 비어 있으면 처리하지 않습니다.
		String normalizedRawBody = trimToNull(rawBody);
		if (normalizedRawBody == null) {
			return;
		}

		// 결제키 또는 주문번호와 상태를 읽어 현재 결제 row를 조회합니다.
		JsonNode rootNode = readShopOrderJsonNode(normalizedRawBody);
		JsonNode dataNode = rootNode.path("data");
		String eventType = safeValue(firstNonBlank(resolveJsonText(rootNode, "eventType"), ""));
		String paymentKey = firstNonBlank(resolveJsonText(dataNode, "paymentKey"), resolveJsonText(rootNode, "paymentKey"));
		String ordNo = firstNonBlank(resolveJsonText(rootNode, "orderId"), resolveJsonText(dataNode, "orderId"));
		String paymentStatus = firstNonBlank(resolveJsonText(dataNode, "status"), resolveJsonText(rootNode, "status"));
		if (isBlank(paymentKey) && isBlank(ordNo)) {
			return;
		}
		if (isBlank(paymentStatus)) {
			return;
		}

		// PAYMENT_STATUS_CHANGED, DEPOSIT_CALLBACK 모두 처리할 수 있도록 결제 row를 조회합니다.
		ShopOrderPaymentVO payment = resolveShopOrderWebhookPayment(paymentKey, ordNo);
		if (payment == null || !SHOP_ORDER_PAYMENT_METHOD_VIRTUAL_ACCOUNT.equals(payment.getPayMethodCd())) {
			return;
		}
		log.info(
			"쇼핑몰 주문 결제 웹훅 수신 eventType={} ordNo={} paymentKey={} payNo={} status={}",
			eventType,
			firstNonBlank(ordNo, payment.getOrdNo()),
			firstNonBlank(paymentKey, payment.getTossPaymentKey()),
			payment.getPayNo(),
			paymentStatus
		);

		// DEPOSIT_CALLBACK은 secret 값을 비교해 토스 승인 응답과 일치하는지 확인합니다.
		if ("DEPOSIT_CALLBACK".equals(eventType) || (isBlank(paymentKey) && !isBlank(ordNo))) {
			validateShopOrderDepositWebhookSecret(payment, trimToNull(resolveJsonText(rootNode, "secret")));
		}

		// 무통장입금 완료는 결제 완료 상태로 승격하고, 만료/취소는 원복 처리합니다.
		String normalizedWebhookDt = normalizeShopOrderDateTime(firstNonBlank(resolveJsonText(rootNode, "createdAt"), resolveJsonText(dataNode, "approvedAt")));
		if ("DONE".equals(paymentStatus)) {
			if (SHOP_ORDER_PAY_STAT_DONE.equals(payment.getPayStatCd())) {
				return;
			}
			orderMapper.updateShopPaymentWebhook(
				payment.getPayNo(),
				SHOP_ORDER_PAY_STAT_DONE,
				paymentStatus,
				"무통장입금 완료",
				normalizedRawBody,
				normalizedWebhookDt,
				payment.getCustNo()
			);
			orderMapper.updateShopOrderBaseStatusAndDates(
				payment.getOrdNo(),
				SHOP_ORDER_STAT_DONE,
				null,
				normalizedWebhookDt,
				payment.getCustNo()
			);
			orderMapper.updateShopOrderDetailStatus(payment.getOrdNo(), SHOP_ORDER_DTL_STAT_DONE, payment.getCustNo());
			return;
		}

		if (!"EXPIRED".equals(paymentStatus) && !"CANCELED".equals(paymentStatus)) {
			return;
		}
		if (!SHOP_ORDER_PAY_STAT_WAITING_DEPOSIT.equals(payment.getPayStatCd())) {
			return;
		}
		String payStatCd = "CANCELED".equals(paymentStatus) ? SHOP_ORDER_PAY_STAT_CANCEL : SHOP_ORDER_PAY_STAT_FAIL;
		orderMapper.updateShopPaymentWebhook(
			payment.getPayNo(),
			payStatCd,
			paymentStatus,
			"CANCELED".equals(paymentStatus) ? "무통장입금 취소" : "무통장입금 만료",
			normalizedRawBody,
			normalizedWebhookDt,
			payment.getCustNo()
		);
		orderMapper.updateShopOrderBaseStatus(payment.getOrdNo(), SHOP_ORDER_STAT_CANCEL, payment.getCustNo());
		orderMapper.updateShopOrderDetailStatus(payment.getOrdNo(), SHOP_ORDER_DTL_STAT_CANCEL, payment.getCustNo());
		restoreShopOrderSuccessSideEffects(payment, payment.getCustNo());
	}

	// 웹훅 본문에서 결제키 또는 주문번호 기준으로 현재 결제 row를 조회합니다.
	private ShopOrderPaymentVO resolveShopOrderWebhookPayment(String paymentKey, String ordNo) {
		// paymentKey가 있으면 우선 결제키 해시 기준으로 조회합니다.
		if (!isBlank(paymentKey)) {
			ShopOrderPaymentVO payment = orderMapper.getShopPaymentByTossPaymentKeyHash(sha256Hex(paymentKey));
			if (payment != null) {
				return payment;
			}
		}

		// DEPOSIT_CALLBACK처럼 orderId만 전달되면 주문번호 기준으로 최신 결제 row를 조회합니다.
		if (isBlank(ordNo)) {
			return null;
		}
		return orderMapper.getShopPaymentByOrdNo(ordNo.trim());
	}

	// DEPOSIT_CALLBACK secret 값이 승인 응답의 secret과 같은지 확인합니다.
	private void validateShopOrderDepositWebhookSecret(ShopOrderPaymentVO payment, String webhookSecret) {
		// secret이 비어 있으면 비교하지 않습니다.
		if (payment == null || isBlank(webhookSecret)) {
			return;
		}

		// 승인 응답 원본 JSON에 저장된 secret 값을 꺼내 비교합니다.
		String savedSecret = trimToNull(resolveJsonText(readShopOrderJsonNode(payment.getRspRawJson()), "secret"));
		if (savedSecret == null) {
			return;
		}
		if (!savedSecret.equals(webhookSecret.trim())) {
			log.warn(
				"쇼핑몰 주문 결제 웹훅 secret 불일치 ordNo={} payNo={} webhookSecret={} savedSecret={}",
				payment.getOrdNo(),
				payment.getPayNo(),
				webhookSecret,
				savedSecret
			);
			throw new IllegalArgumentException("웹훅 검증에 실패했습니다.");
		}
	}

	// 쇼핑몰 주문서 배송지 검색 결과를 조회합니다.
	public ShopOrderAddressSearchResponseVO searchShopOrderAddress(String keyword, Integer currentPage, Integer countPerPage, Long custNo) {
		// 로그인 고객번호와 검색어 유효성을 확인합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}
		return searchOrderAddress(keyword, currentPage, countPerPage);
	}

	// 관리자 주문 반품 회수지 우편번호 검색 결과를 조회합니다.
	public ShopOrderAddressSearchResponseVO searchAdminOrderAddress(String keyword, Integer currentPage, Integer countPerPage) {
		// 관리자 화면에서 사용할 주소 검색 결과를 공통 로직으로 조회합니다.
		return searchOrderAddress(keyword, currentPage, countPerPage);
	}

	// 주문 주소 검색 공통 로직으로 도로명 검색 결과를 조회합니다.
	private ShopOrderAddressSearchResponseVO searchOrderAddress(String keyword, Integer currentPage, Integer countPerPage) {
		// 검색어 유효성을 확인합니다.
		String normalizedKeyword = trimToNull(keyword);
		if (normalizedKeyword == null) {
			throw new IllegalArgumentException("주소 검색어를 입력해주세요.");
		}

		// 주소 검색 페이지 번호와 페이지당 건수를 허용 범위로 정규화합니다.
		int resolvedCurrentPage = normalizeShopOrderAddressSearchCurrentPage(currentPage);
		int resolvedCountPerPage = normalizeShopOrderAddressSearchCountPerPage(countPerPage);
		return jusoAddressApiClient.searchRoadAddress(normalizedKeyword, resolvedCurrentPage, resolvedCountPerPage);
	}

	// 쇼핑몰 주문서 배송지를 등록하고 최신 목록을 반환합니다.
	@Transactional
	public ShopOrderAddressSaveResultVO registerShopOrderAddress(ShopOrderAddressRegisterPO param, Long custNo) {
		// 로그인 고객번호와 배송지 등록 입력값을 검증합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}
		ShopOrderAddressSavePO savePO = validateShopOrderAddressRegisterInput(param, custNo);

		// 배송지 별칭 중복 여부를 확인합니다.
		if (orderMapper.countShopOrderAddressName(custNo, savePO.getAddressNm()) > 0) {
			throw new IllegalArgumentException("이미 사용 중인 배송지명입니다.");
		}

		// 기본 배송지 저장 요청이면 기존 기본 배송지를 모두 해제합니다.
		if (YES.equals(savePO.getDefaultYn())) {
			orderMapper.updateShopOrderAddressDefaultYn(custNo, NO, custNo);
		}

		// 배송지를 등록한 뒤 최신 배송지 목록과 기본 배송지를 구성합니다.
		orderMapper.insertShopOrderAddress(savePO);
		List<ShopOrderAddressVO> addressList = resolveShopOrderAddressList(custNo);
		ShopOrderAddressVO defaultAddress = findShopOrderDefaultAddress(addressList);
		ShopOrderAddressVO savedAddress = findShopOrderAddressByName(addressList, savePO.getAddressNm());
		if (savedAddress == null) {
			throw new IllegalStateException("등록된 배송지 정보를 찾을 수 없습니다.");
		}

		// 등록 결과 응답 객체를 구성합니다.
		ShopOrderAddressSaveResultVO result = new ShopOrderAddressSaveResultVO();
		result.setAddressList(addressList);
		result.setDefaultAddress(defaultAddress);
		result.setSavedAddress(savedAddress);
		return result;
	}

	// 쇼핑몰 주문서 배송지를 수정하고 최신 목록을 반환합니다.
	@Transactional
	public ShopOrderAddressSaveResultVO updateShopOrderAddress(ShopOrderAddressUpdatePO param, Long custNo) {
		// 로그인 고객번호와 배송지 수정 입력값을 검증합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}
		if (param == null || isBlank(param.getOriginAddressNm())) {
			throw new IllegalArgumentException("수정할 배송지 정보를 확인해주세요.");
		}
		String normalizedOriginAddressNm = param.getOriginAddressNm().trim();
		if (orderMapper.countShopOrderAddress(custNo, normalizedOriginAddressNm) < 1) {
			throw new IllegalArgumentException("수정할 배송지를 찾을 수 없습니다.");
		}

		// 수정 입력값을 등록 검증과 동일한 기준으로 검증합니다.
		ShopOrderAddressRegisterPO registerPO = new ShopOrderAddressRegisterPO();
		registerPO.setAddressNm(param.getAddressNm());
		registerPO.setPostNo(param.getPostNo());
		registerPO.setBaseAddress(param.getBaseAddress());
		registerPO.setDetailAddress(param.getDetailAddress());
		registerPO.setPhoneNumber(param.getPhoneNumber());
		registerPO.setRsvNm(param.getRsvNm());
		registerPO.setDefaultYn(param.getDefaultYn());
		ShopOrderAddressSavePO savePO = validateShopOrderAddressRegisterInput(registerPO, custNo);

		// 배송지 별칭이 변경될 때만 중복 여부를 확인합니다.
		if (!normalizedOriginAddressNm.equals(savePO.getAddressNm()) && orderMapper.countShopOrderAddressName(custNo, savePO.getAddressNm()) > 0) {
			throw new IllegalArgumentException("이미 사용 중인 배송지명입니다.");
		}

		// 기본 배송지 저장 요청이면 기존 기본 배송지를 모두 해제합니다.
		if (YES.equals(savePO.getDefaultYn())) {
			orderMapper.updateShopOrderAddressDefaultYn(custNo, NO, custNo);
		}

		// 수정 대상 배송지를 갱신한 뒤 최신 목록과 수정 결과를 반환합니다.
		int updatedCount = orderMapper.updateShopOrderAddress(
			custNo,
			normalizedOriginAddressNm,
			savePO.getAddressNm(),
			savePO.getPostNo(),
			savePO.getBaseAddress(),
			savePO.getDetailAddress(),
			savePO.getPhoneNumber(),
			savePO.getRsvNm(),
			savePO.getDefaultYn(),
			custNo
		);
		if (updatedCount < 1) {
			throw new IllegalArgumentException("수정할 배송지를 찾을 수 없습니다.");
		}

		// 최신 배송지 목록과 수정된 배송지/기본 배송지를 조합합니다.
		List<ShopOrderAddressVO> addressList = resolveShopOrderAddressList(custNo);
		ShopOrderAddressVO defaultAddress = findShopOrderDefaultAddress(addressList);
		ShopOrderAddressVO savedAddress = findShopOrderAddressByName(addressList, savePO.getAddressNm());
		if (savedAddress == null) {
			throw new IllegalStateException("수정된 배송지 정보를 찾을 수 없습니다.");
		}
		ShopOrderAddressSaveResultVO result = new ShopOrderAddressSaveResultVO();
		result.setAddressList(addressList);
		result.setDefaultAddress(defaultAddress);
		result.setSavedAddress(savedAddress);
		return result;
	}

	// 쇼핑몰 장바구니 선택 상품 기준 예상 최대 쿠폰 할인 금액을 계산합니다.
	ShopCartCouponEstimateVO getShopCartCouponEstimate(ShopCartCouponEstimateRequestPO param, Long custNo) {
		// 로그인 고객번호 유효성을 확인합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}

		// 선택된 장바구니 행 정보가 없으면 0원 결과를 반환합니다.
		if (param == null || param.getCartItemList() == null || param.getCartItemList().isEmpty()) {
			return createEmptyShopCartCouponEstimate();
		}

		// 현재 장바구니와 선택 키 목록을 조회해 실제 계산 대상 행을 확정합니다.
		List<ShopCartItemVO> cartItemList = cartMapper.getShopCartItemList(custNo);
		Set<String> selectedCartItemKeySet = buildShopCartCouponEstimateKeySet(param.getCartItemList());
		List<ShopCartItemVO> selectedCartItemList = resolveSelectedCartItemListForCouponEstimate(cartItemList, selectedCartItemKeySet);
		if (selectedCartItemList.isEmpty()) {
			return createEmptyShopCartCouponEstimate();
		}

		// 선택 장바구니/보유 쿠폰/배송비 기준 정보를 조회합니다.
		List<ShopCartCouponEstimateRow> estimateRowList = buildShopCartCouponEstimateRowList(selectedCartItemList);
		List<ShopCartCustomerCouponVO> customerCouponList = goodsMapper.getShopCustomerCouponList(custNo);
		ShopCartSiteInfoVO siteInfo = resolveShopCartSiteInfo();
		if (estimateRowList.isEmpty() || customerCouponList == null || customerCouponList.isEmpty()) {
			return createEmptyShopCartCouponEstimate();
		}

		// 상품쿠폰은 행 단위 최대 가중치 매칭으로 최적 할인액을 계산합니다.
		List<ShopCartCustomerCouponVO> goodsCouponList = filterShopCustomerCouponListByKind(customerCouponList, CPN_GB_GOODS);
		Map<Long, List<ShopGoodsCouponTargetVO>> couponTargetMap = buildShopCouponTargetMap(goodsCouponList);
		int goodsCouponDiscountAmt = calculateMaximumGoodsCouponDiscount(estimateRowList, goodsCouponList, couponTargetMap);

		// 장바구니쿠폰과 배송비쿠폰은 각 1장 최대값으로 계산합니다.
		int selectedSaleAmt = calculateSelectedCartSaleAmt(estimateRowList);
		int discountedSaleAmt = Math.max(selectedSaleAmt - goodsCouponDiscountAmt, 0);
		int cartCouponDiscountAmt = calculateMaximumSingleCouponDiscount(
			filterShopCustomerCouponListByKind(customerCouponList, CPN_GB_CART),
			discountedSaleAmt
		);
		int deliveryFee = resolveCouponEstimateDeliveryFee(selectedSaleAmt, siteInfo);
		int deliveryCouponDiscountAmt = calculateMaximumSingleCouponDiscount(
			filterShopCustomerCouponListByKind(customerCouponList, CPN_GB_DELIVERY),
			deliveryFee
		);

		// 예상 최대 할인 결과를 응답 객체로 구성합니다.
		ShopCartCouponEstimateVO result = new ShopCartCouponEstimateVO();
		result.setGoodsCouponDiscountAmt(goodsCouponDiscountAmt);
		result.setCartCouponDiscountAmt(cartCouponDiscountAmt);
		result.setDeliveryCouponDiscountAmt(deliveryCouponDiscountAmt);
		result.setExpectedMaxDiscountAmt(goodsCouponDiscountAmt + cartCouponDiscountAmt + deliveryCouponDiscountAmt);
		return result;
	}

	// 빈 장바구니 쿠폰 예상 할인 응답을 생성합니다.
	private ShopCartCouponEstimateVO createEmptyShopCartCouponEstimate() {
		// 모든 할인 금액을 0원으로 초기화합니다.
		ShopCartCouponEstimateVO result = new ShopCartCouponEstimateVO();
		result.setExpectedMaxDiscountAmt(0);
		result.setGoodsCouponDiscountAmt(0);
		result.setCartCouponDiscountAmt(0);
		result.setDeliveryCouponDiscountAmt(0);
		return result;
	}

	// 장바구니 쿠폰 예상 할인 계산용 선택 키 목록을 구성합니다.
	private Set<String> buildShopCartCouponEstimateKeySet(List<ShopCartCouponEstimateItemPO> cartItemList) {
		// 요청 목록이 없으면 빈 Set을 반환합니다.
		Set<String> result = new HashSet<>();
		if (cartItemList == null || cartItemList.isEmpty()) {
			return result;
		}

		// 상품코드/사이즈코드 조합으로 중복 제거된 키를 구성합니다.
		for (ShopCartCouponEstimateItemPO cartItem : cartItemList) {
			if (cartItem == null || isBlank(cartItem.getGoodsId()) || isBlank(cartItem.getSizeId())) {
				continue;
			}
			result.add(buildShopCartItemKey(cartItem.getGoodsId(), cartItem.getSizeId()));
		}
		return result;
	}

	// 현재 장바구니에서 요청한 선택 행만 추출합니다.
	private List<ShopCartItemVO> resolveSelectedCartItemListForCouponEstimate(
		List<ShopCartItemVO> cartItemList,
		Set<String> selectedCartItemKeySet
	) {
		// 비교할 장바구니 또는 선택 키가 없으면 빈 목록을 반환합니다.
		if (cartItemList == null || cartItemList.isEmpty() || selectedCartItemKeySet == null || selectedCartItemKeySet.isEmpty()) {
			return List.of();
		}

		// 장바구니 현재 상태 기준으로 실제 계산 대상 행을 구성합니다.
		List<ShopCartItemVO> result = new ArrayList<>();
		for (ShopCartItemVO cartItem : cartItemList) {
			if (cartItem == null || isBlank(cartItem.getGoodsId()) || isBlank(cartItem.getSizeId())) {
				continue;
			}
			if (selectedCartItemKeySet.contains(buildShopCartItemKey(cartItem.getGoodsId(), cartItem.getSizeId()))) {
				result.add(cartItem);
			}
		}
		return result;
	}

	// 장바구니 선택 행을 쿠폰 계산용 컨텍스트 목록으로 변환합니다.
	private List<ShopCartCouponEstimateRow> buildShopCartCouponEstimateRowList(List<ShopCartItemVO> selectedCartItemList) {
		// 선택 행이 없으면 빈 목록을 반환합니다.
		if (selectedCartItemList == null || selectedCartItemList.isEmpty()) {
			return List.of();
		}

		// 상품별 카테고리/기획전 타겟 비교값을 한 번씩만 조회합니다.
		Map<String, Set<String>> goodsCategoryIdSetMap = new HashMap<>();
		Map<String, Set<String>> goodsExhibitionTabNoSetMap = new HashMap<>();
		List<ShopCartCouponEstimateRow> result = new ArrayList<>();
		for (ShopCartItemVO cartItem : selectedCartItemList) {
			if (cartItem == null || isBlank(cartItem.getGoodsId())) {
				continue;
			}

			// 행 단위 판매가 합계와 상품 타겟 비교 정보를 계산합니다.
			String normalizedGoodsId = cartItem.getGoodsId().trim();
			goodsCategoryIdSetMap.computeIfAbsent(
				normalizedGoodsId,
				key -> toSafeStringSet(goodsMapper.getShopGoodsCategoryIdList(key))
			);
			goodsExhibitionTabNoSetMap.computeIfAbsent(
				normalizedGoodsId,
				key -> toSafeStringSet(goodsMapper.getShopGoodsExhibitionTabNoList(key))
			);
			int rowSaleAmt = normalizeNonNegativeNumber(cartItem.getSaleAmt()) * normalizeQuantity(cartItem.getQty());
			ShopCartCouponEstimateRow estimateRow = new ShopCartCouponEstimateRow(
				cartItem.getCartId(),
				normalizedGoodsId,
				isBlank(cartItem.getSizeId()) ? "" : cartItem.getSizeId().trim(),
				rowSaleAmt,
				cartItem.getBrandNo() == null ? null : String.valueOf(cartItem.getBrandNo()),
				goodsCategoryIdSetMap.getOrDefault(normalizedGoodsId, Set.of()),
				goodsExhibitionTabNoSetMap.getOrDefault(normalizedGoodsId, Set.of())
			);
			result.add(estimateRow);
		}
		return result;
	}

	// 쿠폰 번호별 적용/제외 대상 목록을 조회합니다.
	private Map<Long, List<ShopGoodsCouponTargetVO>> buildShopCouponTargetMap(List<ShopCartCustomerCouponVO> couponList) {
		// 쿠폰 목록이 없으면 빈 매핑을 반환합니다.
		Map<Long, List<ShopGoodsCouponTargetVO>> result = new HashMap<>();
		if (couponList == null || couponList.isEmpty()) {
			return result;
		}

		// 동일 쿠폰 번호는 한 번만 조회해 재사용합니다.
		for (ShopCartCustomerCouponVO coupon : couponList) {
			if (coupon == null || coupon.getCpnNo() == null || result.containsKey(coupon.getCpnNo())) {
				continue;
			}
			List<ShopGoodsCouponTargetVO> targetList = goodsMapper.getShopCouponTargetList(coupon.getCpnNo());
			result.put(coupon.getCpnNo(), targetList == null ? List.of() : targetList);
		}
		return result;
	}

	// 쿠폰 종류 코드 기준으로 보유 쿠폰 목록을 필터링합니다.
	private List<ShopCartCustomerCouponVO> filterShopCustomerCouponListByKind(
		List<ShopCartCustomerCouponVO> customerCouponList,
		String cpnGbCd
	) {
		// 필터링 대상이 없으면 빈 목록을 반환합니다.
		if (customerCouponList == null || customerCouponList.isEmpty() || isBlank(cpnGbCd)) {
			return List.of();
		}

		// 요청한 쿠폰 종류만 별도 목록으로 반환합니다.
		List<ShopCartCustomerCouponVO> result = new ArrayList<>();
		for (ShopCartCustomerCouponVO coupon : customerCouponList) {
			if (coupon == null || !cpnGbCd.equals(coupon.getCpnGbCd())) {
				continue;
			}
			result.add(coupon);
		}
		return result;
	}

	// 선택 장바구니 행 목록에 대한 상품쿠폰 최대 할인 합계를 계산합니다.
	private int calculateMaximumGoodsCouponDiscount(
		List<ShopCartCouponEstimateRow> estimateRowList,
		List<ShopCartCustomerCouponVO> goodsCouponList,
		Map<Long, List<ShopGoodsCouponTargetVO>> couponTargetMap
	) {
		// 상품 행 또는 상품쿠폰이 없으면 0원을 반환합니다.
		if (estimateRowList == null || estimateRowList.isEmpty() || goodsCouponList == null || goodsCouponList.isEmpty()) {
			return 0;
		}

		// 행-쿠폰 조합 할인액을 가중치 행렬로 구성합니다.
		int[][] weightMatrix = new int[estimateRowList.size()][goodsCouponList.size()];
		boolean hasPositiveWeight = false;
		for (int rowIndex = 0; rowIndex < estimateRowList.size(); rowIndex += 1) {
			ShopCartCouponEstimateRow estimateRow = estimateRowList.get(rowIndex);
			for (int couponIndex = 0; couponIndex < goodsCouponList.size(); couponIndex += 1) {
				ShopCartCustomerCouponVO coupon = goodsCouponList.get(couponIndex);
				List<ShopGoodsCouponTargetVO> targetList = coupon == null || coupon.getCpnNo() == null
					? List.of()
					: couponTargetMap.getOrDefault(coupon.getCpnNo(), List.of());
				if (isUnmatchedShopCartGoodsCoupon(coupon, targetList, estimateRow)) {
					continue;
				}
				int discountAmt = calculateCouponDiscountAmount(coupon, estimateRow.rowSaleAmt());
				weightMatrix[rowIndex][couponIndex] = discountAmt;
				if (discountAmt > 0) {
					hasPositiveWeight = true;
				}
			}
		}
		if (!hasPositiveWeight) {
			return 0;
		}

		// 최대 가중치 이분 매칭으로 최적 할인 합계를 계산합니다.
		return calculateAssignmentDiscountAmount(weightMatrix, solveMaximumWeightAssignmentColumns(weightMatrix));
	}

	// 장바구니쿠폰/배송비쿠폰처럼 한 장만 적용하는 쿠폰 최대 할인액을 계산합니다.
	private int calculateMaximumSingleCouponDiscount(List<ShopCartCustomerCouponVO> couponList, int baseAmt) {
		// 기준 금액 또는 쿠폰 목록이 없으면 0원을 반환합니다.
		if (baseAmt < 1 || couponList == null || couponList.isEmpty()) {
			return 0;
		}

		// 각 쿠폰 할인액 중 가장 큰 값을 선택합니다.
		int maxDiscountAmt = 0;
		for (ShopCartCustomerCouponVO coupon : couponList) {
			maxDiscountAmt = Math.max(maxDiscountAmt, calculateCouponDiscountAmount(coupon, baseAmt));
		}
		return maxDiscountAmt;
	}

	// 쿠폰 1건의 기준 금액 대비 할인액을 계산합니다.
	private int calculateCouponDiscountAmount(ShopCartCustomerCouponVO coupon, int baseAmt) {
		// 기준 금액 또는 쿠폰 정보가 없으면 0원을 반환합니다.
		if (coupon == null || baseAmt < 1) {
			return 0;
		}

		// 할인 구분 코드에 따라 금액/정률 할인액을 계산합니다.
		int cpnDcVal = normalizeNonNegativeNumber(coupon.getCpnDcVal());
		if (CPN_DC_GB_AMOUNT.equals(coupon.getCpnDcGbCd())) {
			return Math.min(baseAmt, cpnDcVal);
		}
		if (CPN_DC_GB_RATE.equals(coupon.getCpnDcGbCd())) {
			return (int) Math.floor((double) baseAmt * (double) cpnDcVal / 100.0d);
		}
		return 0;
	}

	// 선택 장바구니 행 목록의 판매가 합계를 계산합니다.
	private int calculateSelectedCartSaleAmt(List<ShopCartCouponEstimateRow> estimateRowList) {
		// 계산 대상 행이 없으면 0원을 반환합니다.
		if (estimateRowList == null || estimateRowList.isEmpty()) {
			return 0;
		}

		// 행 단위 판매가 합계를 모두 누적합니다.
		int result = 0;
		for (ShopCartCouponEstimateRow estimateRow : estimateRowList) {
			if (estimateRow == null) {
				continue;
			}
			result += estimateRow.rowSaleAmt();
		}
		return result;
	}

	// 장바구니 현재 합계 기준 배송비를 계산합니다.
	private int resolveCouponEstimateDeliveryFee(int selectedSaleAmt, ShopCartSiteInfoVO siteInfo) {
		// 사이트 배송 기준 금액을 안전한 값으로 보정합니다.
		int deliveryFee = siteInfo != null ? normalizeNonNegativeNumber(siteInfo.getDeliveryFee()) : 0;
		int deliveryFeeLimit = siteInfo != null ? normalizeNonNegativeNumber(siteInfo.getDeliveryFeeLimit()) : 0;
		if (selectedSaleAmt >= deliveryFeeLimit) {
			return 0;
		}
		return deliveryFee;
	}

	// 행별 상품쿠폰 할인 가중치 행렬에서 최대 할인 합계를 계산합니다.
	private int solveMaximumWeightAssignment(int[][] weightMatrix) {
		// 행별 최적 할당 열 목록을 구한 뒤 할인 합계를 계산합니다.
		return calculateAssignmentDiscountAmount(weightMatrix, solveMaximumWeightAssignmentColumns(weightMatrix));
	}

	// 행별 상품쿠폰 할인 가중치 행렬에서 최대 할인 열 할당 결과를 계산합니다.
	private int[] solveMaximumWeightAssignmentColumns(int[][] weightMatrix) {
		// 유효한 행렬이 없으면 빈 할당 결과를 반환합니다.
		if (weightMatrix == null || weightMatrix.length == 0 || weightMatrix[0].length == 0) {
			return new int[0];
		}

		// 직사각형 행렬을 정사각형으로 패딩해 헝가리안 알고리즘 입력으로 변환합니다.
		int rowCount = weightMatrix.length;
		int columnCount = weightMatrix[0].length;
		int matrixSize = Math.max(rowCount, columnCount);
		int[][] paddedMatrix = new int[matrixSize + 1][matrixSize + 1];
		for (int rowIndex = 0; rowIndex < rowCount; rowIndex += 1) {
			System.arraycopy(weightMatrix[rowIndex], 0, paddedMatrix[rowIndex + 1], 1, columnCount);
		}

		// 최소 비용 문제 형태로 변환한 뒤 최적 할당을 계산합니다.
		int[] u = new int[matrixSize + 1];
		int[] v = new int[matrixSize + 1];
		int[] p = new int[matrixSize + 1];
		int[] way = new int[matrixSize + 1];
		for (int row = 1; row <= matrixSize; row += 1) {
			p[0] = row;
			int column = 0;
			int[] minValue = new int[matrixSize + 1];
			boolean[] used = new boolean[matrixSize + 1];
			Arrays.fill(minValue, Integer.MAX_VALUE);

			// 현재 행을 기준으로 최소 비용 증가분을 반복 갱신합니다.
			do {
				used[column] = true;
				int currentRow = p[column];
				int delta = Integer.MAX_VALUE;
				int nextColumn = 0;
				for (int candidateColumn = 1; candidateColumn <= matrixSize; candidateColumn += 1) {
					if (used[candidateColumn]) {
						continue;
					}
					int cost = -paddedMatrix[currentRow][candidateColumn] - u[currentRow] - v[candidateColumn];
					if (cost < minValue[candidateColumn]) {
						minValue[candidateColumn] = cost;
						way[candidateColumn] = column;
					}
					if (minValue[candidateColumn] < delta) {
						delta = minValue[candidateColumn];
						nextColumn = candidateColumn;
					}
				}

				// 잠재치와 잔여 최소 비용을 함께 업데이트합니다.
				for (int candidateColumn = 0; candidateColumn <= matrixSize; candidateColumn += 1) {
					if (used[candidateColumn]) {
						u[p[candidateColumn]] += delta;
						v[candidateColumn] -= delta;
						continue;
					}
					minValue[candidateColumn] -= delta;
				}
				column = nextColumn;
			} while (p[column] != 0);

			// 역추적으로 현재 행의 최적 할당 경로를 반영합니다.
			do {
				int previousColumn = way[column];
				p[column] = p[previousColumn];
				column = previousColumn;
			} while (column != 0);
		}

		// 실제 입력 행렬 범위만 합산해 최대 할인 합계를 반환합니다.
		int[] assignment = new int[matrixSize + 1];
		for (int column = 1; column <= matrixSize; column += 1) {
			assignment[p[column]] = column;
		}
		int[] result = new int[rowCount];
		Arrays.fill(result, -1);
		for (int row = 1; row <= rowCount; row += 1) {
			int assignedColumn = assignment[row];
			if (assignedColumn < 1 || assignedColumn > columnCount) {
				continue;
			}
			if (paddedMatrix[row][assignedColumn] < 1) {
				continue;
			}
			result[row - 1] = assignedColumn - 1;
		}
		return result;
	}

	// 행-열 할당 결과 기준 할인 합계를 계산합니다.
	private int calculateAssignmentDiscountAmount(int[][] weightMatrix, int[] assignmentColumns) {
		// 유효한 행렬 또는 할당 결과가 없으면 0원을 반환합니다.
		if (weightMatrix == null || weightMatrix.length == 0 || assignmentColumns == null || assignmentColumns.length == 0) {
			return 0;
		}

		// 실제 할당된 할인액만 누적합니다.
		int result = 0;
		for (int rowIndex = 0; rowIndex < weightMatrix.length; rowIndex += 1) {
			if (rowIndex >= assignmentColumns.length) {
				break;
			}
			int assignedColumnIndex = assignmentColumns[rowIndex];
			if (assignedColumnIndex < 0 || assignedColumnIndex >= weightMatrix[rowIndex].length) {
				continue;
			}
			result += Math.max(weightMatrix[rowIndex][assignedColumnIndex], 0);
		}
		return Math.max(result, 0);
	}

	// 상품쿠폰 1건이 특정 장바구니 행에 적용 가능한지 확인합니다.
	private boolean isUnmatchedShopCartGoodsCoupon(
		ShopCartCustomerCouponVO coupon,
		List<ShopGoodsCouponTargetVO> targetList,
		ShopCartCouponEstimateRow estimateRow
	) {
		// 비교할 쿠폰 또는 행 정보가 없으면 미적용 처리합니다.
		if (coupon == null || estimateRow == null || isBlank(estimateRow.goodsId())) {
			return true;
		}
		return !isMatchedShopCouponTarget(
			coupon.getCpnTargetCd(),
			targetList,
			estimateRow.goodsId(),
			estimateRow.brandNoValue(),
			estimateRow.categoryIdSet(),
			estimateRow.exhibitionTabNoSet()
		);
	}

	// 상품코드와 사이즈코드 조합 기준 장바구니 키를 생성합니다.
	private String buildShopCartItemKey(String goodsId, String sizeId) {
		// 빈 값은 trim 후 그대로 연결해 비교 키를 생성합니다.
		String normalizedGoodsId = goodsId == null ? "" : goodsId.trim();
		String normalizedSizeId = sizeId == null ? "" : sizeId.trim();
		return normalizedGoodsId + "||" + normalizedSizeId;
	}

	// 정수 값을 0 이상으로 보정합니다.
	int normalizeNonNegativeNumber(Integer value) {
		// 값이 없거나 음수면 0으로 보정합니다.
		if (value == null || value < 0) {
			return 0;
		}
		return value;
	}

	// 수량 값을 1 이상으로 보정합니다.
	private int normalizeQuantity(Integer value) {
		// 값이 없거나 1 미만이면 최소 주문 수량 1을 반환합니다.
		if (value == null || value < 1) {
			return 1;
		}
		return value;
	}

	// 장바구니/바로구매 저장 입력값을 검증하고 정규화합니다.
	private ShopCartValidatedInput validateShopCartInput(String goodsId, String sizeId, Integer qty, Long custNo) {
		// 필수 입력값(goodsId/sizeId/qty/custNo) 유효성을 확인합니다.
		if (isBlank(goodsId)) {
			throw new IllegalArgumentException("상품코드를 확인해주세요.");
		}
		if (isBlank(sizeId)) {
			throw new IllegalArgumentException("사이즈를 선택해주세요.");
		}
		if (qty == null || qty < 1) {
			throw new IllegalArgumentException("수량을 확인해주세요.");
		}
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}

		// 현재 조회 가능한 상품인지 확인합니다.
		String normalizedGoodsId = goodsId.trim();
		String normalizedSizeId = sizeId.trim();
		ShopGoodsBasicVO goods = goodsMapper.getShopGoodsBasic(normalizedGoodsId);
		if (goods == null) {
			throw new IllegalArgumentException("상품 정보를 찾을 수 없습니다.");
		}

		// 상품 사이즈 목록에서 요청 사이즈의 유효성과 품절 상태를 확인합니다.
		List<ShopGoodsSizeItemVO> sizeList = goodsMapper.getShopGoodsSizeList(normalizedGoodsId);
		ShopGoodsSizeItemVO targetSize = findShopGoodsSizeBySizeId(sizeList, normalizedSizeId);
		if (targetSize == null) {
			throw new IllegalArgumentException("사이즈를 확인해주세요.");
		}
		int stockQty = targetSize.getStockQty() == null ? 0 : targetSize.getStockQty();
		if (stockQty < 1) {
			throw new IllegalArgumentException("품절된 사이즈입니다.");
		}
		return new ShopCartValidatedInput(normalizedGoodsId, normalizedSizeId, qty);
	}

	// 장바구니 저장 PO를 생성합니다.
	private ShopCartSavePO createShopCartSavePO(String cartGbCd, Long custNo, ShopCartValidatedInput validatedInput, Integer exhibitionNo) {
		// 장바구니 구분/상품/사이즈/수량 정보를 저장 PO에 반영합니다.
		ShopCartSavePO savePO = new ShopCartSavePO();
		savePO.setCartGbCd(cartGbCd);
		savePO.setCustNo(custNo);
		savePO.setGoodsId(validatedInput.getGoodsId());
		savePO.setSizeId(validatedInput.getSizeId());
		savePO.setQty(validatedInput.getQty());
		savePO.setExhibitionNo(exhibitionNo);
		savePO.setRegNo(custNo);
		savePO.setUdtNo(custNo);
		return savePO;
	}

	// 장바구니 저장 대상 기획전 번호를 현재 노출 가능한 기획전 상품 기준으로 재검증합니다.
	private Integer resolveValidatedShopCartExhibitionNo(String goodsId, Integer exhibitionNo) {
		// 기획전 번호가 없거나 1 미만이면 일반 경로로 판단해 null을 반환합니다.
		if (exhibitionNo == null || exhibitionNo < 1) {
			return null;
		}

		// 현재 노출 가능한 기획전 상품이면 해당 번호를 유지하고 아니면 제거합니다.
		int visibleGoodsCount = exhibitionMapper.countShopVisibleExhibitionGoodsByGoodsId(exhibitionNo, goodsId);
		return visibleGoodsCount > 0 ? exhibitionNo : null;
	}

	// 주문서 대상 장바구니 번호 목록을 중복 없이 정규화합니다.
	private List<Long> normalizeShopOrderCartIdList(List<Long> cartIdList) {
		// 장바구니 번호 목록이 없으면 빈 목록을 반환합니다.
		if (cartIdList == null || cartIdList.isEmpty()) {
			return List.of();
		}

		// 1 이상 번호만 순서 보존 중복 제거합니다.
		Set<Long> distinctCartIdSet = new LinkedHashSet<>();
		for (Long cartId : cartIdList) {
			if (cartId == null || cartId < 1L) {
				continue;
			}
			distinctCartIdSet.add(cartId);
		}
		return new ArrayList<>(distinctCartIdSet);
	}

	// 현재 로그인 고객 기준으로 유효한 주문 대상 장바구니 목록을 조회합니다.
	private List<ShopCartItemVO> resolveValidatedShopOrderCartItemList(List<Long> cartIdList, Long custNo) {
		// 주문서 대상 cartId 목록을 중복 없이 정규화합니다.
		List<Long> normalizedCartIdList = normalizeShopOrderCartIdList(cartIdList);
		if (normalizedCartIdList.isEmpty()) {
			throw new IllegalArgumentException("주문 정보가 맞지 않습니다.");
		}

		// 현재 로그인 고객의 cartId 목록을 조회해 모두 유효한지 검증합니다.
		List<ShopCartItemVO> orderCartItemList = cartMapper.getShopOrderCartItemList(custNo, normalizedCartIdList);
		if (orderCartItemList == null || orderCartItemList.size() != normalizedCartIdList.size()) {
			throw new IllegalArgumentException("주문 정보가 맞지 않습니다.");
		}
		return orderCartItemList;
	}

	// 주문 대상 상품의 현재 재고가 충분한지 확인합니다.
	private void validateShopOrderStock(List<ShopCartItemVO> orderCartItemList) {
		// 같은 상품/사이즈는 수량을 합산해 현재 재고와 비교합니다.
		for (ShopOrderRestoreCartItemVO stockItem : aggregateShopOrderStockItemListFromCart(orderCartItemList)) {
			if (stockItem == null || isBlank(stockItem.getGoodsId()) || isBlank(stockItem.getSizeId())) {
				continue;
			}
			int requiredQty = normalizeNonNegativeNumber(stockItem.getOrdQty());
			if (requiredQty < 1) {
				continue;
			}
			GoodsSizeVO goodsSize = goodsMapper.getAdminGoodsSizeDetail(stockItem.getGoodsId(), stockItem.getSizeId());
			int stockQty = goodsSize == null || YES.equalsIgnoreCase(goodsSize.getDelYn())
				? 0
				: normalizeNonNegativeNumber(goodsSize.getStockQty());
			if (stockQty < requiredQty) {
				throw new IllegalArgumentException(SHOP_ORDER_PAYMENT_STOCK_SHORTAGE_MESSAGE);
			}
		}
	}

	// 주문 대상 장바구니 목록 기준 할인 계산 컨텍스트를 생성합니다.
	private ShopOrderDiscountContext buildShopOrderDiscountContext(List<ShopCartItemVO> cartItemList, Long custNo) {
		// 쿠폰 계산용 행 목록과 현재 보유 쿠폰/포인트/배송비 기준 정보를 함께 조회합니다.
		List<ShopCartCouponEstimateRow> estimateRowList = buildShopCartCouponEstimateRowList(cartItemList);
		List<ShopCartCustomerCouponVO> customerCouponList = goodsMapper.getShopCustomerCouponList(custNo);
		Map<Long, List<ShopGoodsCouponTargetVO>> couponTargetMap = buildShopCouponTargetMap(customerCouponList);
		ShopCartSiteInfoVO siteInfo = resolveShopCartSiteInfo();
		int availablePointAmt = normalizeNonNegativeNumber(orderMapper.getShopAvailablePointAmt(custNo));
		return new ShopOrderDiscountContext(
			cartItemList == null ? List.of() : cartItemList,
			estimateRowList,
			customerCouponList == null ? List.of() : customerCouponList,
			filterShopCustomerCouponListByKind(customerCouponList, CPN_GB_GOODS),
			filterShopCustomerCouponListByKind(customerCouponList, CPN_GB_CART),
			filterShopCustomerCouponListByKind(customerCouponList, CPN_GB_DELIVERY),
			couponTargetMap,
			siteInfo,
			availablePointAmt
		);
	}

	// 주문서 쿠폰 선택 후보 정보를 구성합니다.
	private ShopOrderCouponOptionVO buildShopOrderCouponOption(ShopOrderDiscountContext discountContext) {
		// 상품쿠폰/장바구니쿠폰/배송비쿠폰 후보 목록을 응답 객체로 조합합니다.
		ShopOrderCouponOptionVO result = new ShopOrderCouponOptionVO();
		result.setGoodsCouponGroupList(buildShopOrderGoodsCouponGroupList(discountContext));
		result.setCartCouponList(buildShopOrderCouponItemList(discountContext.cartCouponList()));
		result.setDeliveryCouponList(buildShopOrderCouponItemList(discountContext.deliveryCouponList()));
		return result;
	}

	// 주문 상품 행별 상품쿠폰 선택 후보 목록을 구성합니다.
	private List<ShopOrderGoodsCouponGroupVO> buildShopOrderGoodsCouponGroupList(ShopOrderDiscountContext discountContext) {
		// 상품 행이 없으면 빈 목록을 반환합니다.
		if (discountContext == null || discountContext.estimateRowList().isEmpty()) {
			return List.of();
		}

		// 주문 상품 행 순서대로 적용 가능한 상품쿠폰 목록을 구성합니다.
		List<ShopOrderGoodsCouponGroupVO> result = new ArrayList<>();
		for (int rowIndex = 0; rowIndex < discountContext.estimateRowList().size(); rowIndex += 1) {
			ShopCartCouponEstimateRow estimateRow = discountContext.estimateRowList().get(rowIndex);
			ShopCartItemVO cartItem = discountContext.cartItemList().get(rowIndex);
			ShopOrderGoodsCouponGroupVO group = new ShopOrderGoodsCouponGroupVO();
			group.setCartId(estimateRow == null ? null : estimateRow.cartId());
			group.setGoodsId(cartItem == null ? null : cartItem.getGoodsId());
			group.setGoodsNm(cartItem == null ? null : cartItem.getGoodsNm());
			group.setSizeId(cartItem == null ? null : cartItem.getSizeId());
			group.setCouponList(buildMatchedShopOrderGoodsCouponItemList(estimateRow, discountContext));
			result.add(group);
		}
		return result;
	}

	// 특정 주문 상품 행에 적용 가능한 상품쿠폰 후보 목록을 구성합니다.
	private List<ShopOrderCouponItemVO> buildMatchedShopOrderGoodsCouponItemList(
		ShopCartCouponEstimateRow estimateRow,
		ShopOrderDiscountContext discountContext
	) {
		// 비교할 행이 없으면 빈 목록을 반환합니다.
		if (estimateRow == null || discountContext == null || discountContext.goodsCouponList().isEmpty()) {
			return List.of();
		}

		// 적용 가능한 상품쿠폰만 선택 후보 목록으로 변환합니다.
		List<ShopOrderCouponItemVO> result = new ArrayList<>();
		for (ShopCartCustomerCouponVO coupon : discountContext.goodsCouponList()) {
			List<ShopGoodsCouponTargetVO> targetList = coupon == null || coupon.getCpnNo() == null
				? List.of()
				: discountContext.couponTargetMap().getOrDefault(coupon.getCpnNo(), List.of());
			if (isUnmatchedShopCartGoodsCoupon(coupon, targetList, estimateRow)) {
				continue;
			}
			if (calculateCouponDiscountAmount(coupon, estimateRow.rowSaleAmt()) < 1) {
				continue;
			}
			result.add(buildShopOrderCouponItem(coupon));
		}
		return result;
	}

	// 보유 쿠폰 목록을 주문서 쿠폰 선택 항목 목록으로 변환합니다.
	private List<ShopOrderCouponItemVO> buildShopOrderCouponItemList(List<ShopCartCustomerCouponVO> couponList) {
		// 쿠폰 목록이 없으면 빈 목록을 반환합니다.
		if (couponList == null || couponList.isEmpty()) {
			return List.of();
		}

		// 보유 쿠폰 정보만 주문서 선택 항목 형태로 변환합니다.
		List<ShopOrderCouponItemVO> result = new ArrayList<>();
		for (ShopCartCustomerCouponVO coupon : couponList) {
			if (coupon == null) {
				continue;
			}
			result.add(buildShopOrderCouponItem(coupon));
		}
		return result;
	}

	// 보유 쿠폰 1건을 주문서 쿠폰 선택 항목으로 변환합니다.
	private ShopOrderCouponItemVO buildShopOrderCouponItem(ShopCartCustomerCouponVO coupon) {
		// 쿠폰 식별/할인 정보를 주문서 쿠폰 항목 객체에 매핑합니다.
		ShopOrderCouponItemVO result = new ShopOrderCouponItemVO();
		result.setCustCpnNo(coupon == null ? null : coupon.getCustCpnNo());
		result.setCpnNo(coupon == null ? null : coupon.getCpnNo());
		result.setCpnNm(coupon == null ? null : coupon.getCpnNm());
		result.setCpnGbCd(coupon == null ? null : coupon.getCpnGbCd());
		result.setCpnTargetCd(coupon == null ? null : coupon.getCpnTargetCd());
		result.setCpnDcGbCd(coupon == null ? null : coupon.getCpnDcGbCd());
		result.setCpnDcVal(coupon == null ? null : coupon.getCpnDcVal());
		result.setCpnUsableStartDt(coupon == null ? null : coupon.getCpnUsableStartDt());
		result.setCpnUsableEndDt(coupon == null ? null : coupon.getCpnUsableEndDt());
		return result;
	}

	// 주문서 자동 최대 할인 선택 결과를 계산합니다.
	private ShopOrderDiscountQuoteVO buildShopOrderAutoDiscountQuote(ShopOrderDiscountContext discountContext) {
		// 상품쿠폰 자동 선택 결과를 먼저 계산합니다.
		ShopOrderGoodsCouponMatchResult goodsCouponMatchResult = calculateShopOrderAutoGoodsCouponMatch(discountContext);
		int selectedSaleAmt = calculateSelectedCartSaleAmt(discountContext.estimateRowList());
		int discountedSaleAmt = Math.max(selectedSaleAmt - goodsCouponMatchResult.discountAmt(), 0);
		int deliveryFee = resolveCouponEstimateDeliveryFee(selectedSaleAmt, discountContext.siteInfo());

		// 장바구니/배송비 쿠폰은 최대 할인 1건을 자동 선택합니다.
		ShopCartCustomerCouponVO selectedCartCoupon = findMaximumDiscountCoupon(discountContext.cartCouponList(), discountedSaleAmt);
		ShopCartCustomerCouponVO selectedDeliveryCoupon = findMaximumDiscountCoupon(discountContext.deliveryCouponList(), deliveryFee);

		// 정규화된 선택 상태와 할인 금액을 응답 객체로 조합합니다.
		ShopOrderDiscountSelectionVO selection = new ShopOrderDiscountSelectionVO();
		selection.setGoodsCouponSelectionList(goodsCouponMatchResult.selectionList());
		selection.setCartCouponCustCpnNo(selectedCartCoupon == null ? null : selectedCartCoupon.getCustCpnNo());
		selection.setDeliveryCouponCustCpnNo(selectedDeliveryCoupon == null ? null : selectedDeliveryCoupon.getCustCpnNo());
		return buildShopOrderDiscountQuote(
			selection,
			goodsCouponMatchResult.discountAmt(),
			calculateCouponDiscountAmount(selectedCartCoupon, discountedSaleAmt),
			calculateCouponDiscountAmount(selectedDeliveryCoupon, deliveryFee),
			discountContext.availablePointAmt(),
			discountedSaleAmt
		);
	}

	// 주문서 명시 선택 기준 할인 재계산 결과를 계산합니다.
	private ShopOrderDiscountQuoteVO buildShopOrderDiscountQuoteFromSelection(
		ShopOrderDiscountContext discountContext,
		List<ShopOrderGoodsCouponSelectionVO> goodsCouponSelectionList,
		Long cartCouponCustCpnNo,
		Long deliveryCouponCustCpnNo
	) {
		// 선택 후보 범위 안의 쿠폰만 남기도록 입력 선택을 정규화합니다.
		ShopOrderDiscountSelectionVO normalizedSelection = normalizeShopOrderDiscountSelection(
			discountContext,
			goodsCouponSelectionList,
			cartCouponCustCpnNo,
			deliveryCouponCustCpnNo
		);

		// 정규화된 선택 상태 기준으로 할인 금액을 계산합니다.
		int goodsCouponDiscountAmt = calculateSelectedShopOrderGoodsCouponDiscount(discountContext, normalizedSelection.getGoodsCouponSelectionList());
		int selectedSaleAmt = calculateSelectedCartSaleAmt(discountContext.estimateRowList());
		int discountedSaleAmt = Math.max(selectedSaleAmt - goodsCouponDiscountAmt, 0);
		int deliveryFee = resolveCouponEstimateDeliveryFee(selectedSaleAmt, discountContext.siteInfo());
		ShopCartCustomerCouponVO cartCoupon = findCustomerCouponByCustCpnNo(discountContext.cartCouponList(), normalizedSelection.getCartCouponCustCpnNo());
		ShopCartCustomerCouponVO deliveryCoupon = findCustomerCouponByCustCpnNo(discountContext.deliveryCouponList(), normalizedSelection.getDeliveryCouponCustCpnNo());
		return buildShopOrderDiscountQuote(
			normalizedSelection,
			goodsCouponDiscountAmt,
			calculateCouponDiscountAmount(cartCoupon, discountedSaleAmt),
			calculateCouponDiscountAmount(deliveryCoupon, deliveryFee),
			discountContext.availablePointAmt(),
			discountedSaleAmt
		);
	}

	// 주문서 할인 재계산 응답 객체를 구성합니다.
	private ShopOrderDiscountQuoteVO buildShopOrderDiscountQuote(
		ShopOrderDiscountSelectionVO selection,
		int goodsCouponDiscountAmt,
		int cartCouponDiscountAmt,
		int deliveryCouponDiscountAmt,
		int availablePointAmt,
		int discountedSaleAmt
	) {
		// 최대 포인트 사용 가능 금액과 전체 쿠폰 할인 금액을 계산합니다.
		int normalizedGoodsCouponDiscountAmt = Math.max(goodsCouponDiscountAmt, 0);
		int normalizedCartCouponDiscountAmt = Math.max(cartCouponDiscountAmt, 0);
		int normalizedDeliveryCouponDiscountAmt = Math.max(deliveryCouponDiscountAmt, 0);
		int maxPointUseAmt = Math.clamp(availablePointAmt, 0, Math.max(discountedSaleAmt - normalizedCartCouponDiscountAmt, 0));

		// 선택 상태와 할인 금액 요약을 응답 객체로 설정합니다.
		ShopOrderDiscountAmountVO discountAmount = new ShopOrderDiscountAmountVO();
		discountAmount.setGoodsCouponDiscountAmt(normalizedGoodsCouponDiscountAmt);
		discountAmount.setCartCouponDiscountAmt(normalizedCartCouponDiscountAmt);
		discountAmount.setDeliveryCouponDiscountAmt(normalizedDeliveryCouponDiscountAmt);
		discountAmount.setCouponDiscountAmt(normalizedGoodsCouponDiscountAmt + normalizedCartCouponDiscountAmt + normalizedDeliveryCouponDiscountAmt);
		discountAmount.setMaxPointUseAmt(maxPointUseAmt);

		ShopOrderDiscountQuoteVO result = new ShopOrderDiscountQuoteVO();
		result.setDiscountSelection(selection);
		result.setDiscountAmount(discountAmount);
		return result;
	}

	// 상품쿠폰 자동 최대 할인 선택 결과를 계산합니다.
	private ShopOrderGoodsCouponMatchResult calculateShopOrderAutoGoodsCouponMatch(ShopOrderDiscountContext discountContext) {
		// 상품 행이 없으면 빈 선택 결과를 반환합니다.
		if (discountContext == null || discountContext.estimateRowList().isEmpty()) {
			return new ShopOrderGoodsCouponMatchResult(List.of(), 0);
		}

		// 기본 선택값은 모든 행 미적용 상태로 초기화합니다.
		List<ShopOrderGoodsCouponSelectionVO> selectionList = createDefaultShopOrderGoodsCouponSelectionList(discountContext.cartItemList());
		if (discountContext.goodsCouponList().isEmpty()) {
			return new ShopOrderGoodsCouponMatchResult(selectionList, 0);
		}

		// 상품행-쿠폰행 조합 가중치 행렬을 구성합니다.
		int[][] weightMatrix = new int[discountContext.estimateRowList().size()][discountContext.goodsCouponList().size()];
		boolean hasPositiveWeight = false;
		for (int rowIndex = 0; rowIndex < discountContext.estimateRowList().size(); rowIndex += 1) {
			ShopCartCouponEstimateRow estimateRow = discountContext.estimateRowList().get(rowIndex);
			for (int couponIndex = 0; couponIndex < discountContext.goodsCouponList().size(); couponIndex += 1) {
				ShopCartCustomerCouponVO coupon = discountContext.goodsCouponList().get(couponIndex);
				List<ShopGoodsCouponTargetVO> targetList = coupon == null || coupon.getCpnNo() == null
					? List.of()
					: discountContext.couponTargetMap().getOrDefault(coupon.getCpnNo(), List.of());
				if (isUnmatchedShopCartGoodsCoupon(coupon, targetList, estimateRow)) {
					continue;
				}
				int discountAmt = calculateCouponDiscountAmount(coupon, estimateRow.rowSaleAmt());
				weightMatrix[rowIndex][couponIndex] = discountAmt;
				if (discountAmt > 0) {
					hasPositiveWeight = true;
				}
			}
		}
		if (!hasPositiveWeight) {
			return new ShopOrderGoodsCouponMatchResult(selectionList, 0);
		}

		// 최대 가중치 할당 결과를 선택 목록과 할인 합계로 변환합니다.
		int[] assignmentColumns = solveMaximumWeightAssignmentColumns(weightMatrix);
		for (int rowIndex = 0; rowIndex < assignmentColumns.length && rowIndex < selectionList.size(); rowIndex += 1) {
			int couponIndex = assignmentColumns[rowIndex];
			if (couponIndex < 0 || couponIndex >= discountContext.goodsCouponList().size()) {
				continue;
			}
			selectionList.get(rowIndex).setCustCpnNo(discountContext.goodsCouponList().get(couponIndex).getCustCpnNo());
		}
		return new ShopOrderGoodsCouponMatchResult(selectionList, calculateAssignmentDiscountAmount(weightMatrix, assignmentColumns));
	}

	// 주문 상품 행 기준 기본 상품쿠폰 선택 목록을 생성합니다.
	private List<ShopOrderGoodsCouponSelectionVO> createDefaultShopOrderGoodsCouponSelectionList(List<ShopCartItemVO> cartItemList) {
		// 주문 상품 행이 없으면 빈 목록을 반환합니다.
		if (cartItemList == null || cartItemList.isEmpty()) {
			return List.of();
		}

		// 각 행별 장바구니 번호만 세팅한 미적용 선택 목록을 생성합니다.
		List<ShopOrderGoodsCouponSelectionVO> result = new ArrayList<>();
		for (ShopCartItemVO cartItem : cartItemList) {
			ShopOrderGoodsCouponSelectionVO selection = new ShopOrderGoodsCouponSelectionVO();
			selection.setCartId(cartItem == null ? null : cartItem.getCartId());
			selection.setCustCpnNo(null);
			result.add(selection);
		}
		return result;
	}

	// 주문서 쿠폰 선택 요청을 현재 주문 대상과 후보 목록 기준으로 정규화합니다.
	private ShopOrderDiscountSelectionVO normalizeShopOrderDiscountSelection(
		ShopOrderDiscountContext discountContext,
		List<ShopOrderGoodsCouponSelectionVO> goodsCouponSelectionList,
		Long cartCouponCustCpnNo,
		Long deliveryCouponCustCpnNo
	) {
		// 비교에 사용할 상품쿠폰 후보 목록을 cartId 기준으로 매핑합니다.
		ShopOrderCouponOptionVO couponOption = buildShopOrderCouponOption(discountContext);
		Map<Long, Set<Long>> goodsCouponCandidateMap = new HashMap<>();
		for (ShopOrderGoodsCouponGroupVO group : couponOption.getGoodsCouponGroupList()) {
			if (group == null || group.getCartId() == null) {
				continue;
			}
			Set<Long> candidateSet = new HashSet<>();
			for (ShopOrderCouponItemVO coupon : group.getCouponList() == null ? List.<ShopOrderCouponItemVO>of() : group.getCouponList()) {
				if (coupon.getCustCpnNo() == null) {
					continue;
				}
				candidateSet.add(coupon.getCustCpnNo());
			}
			goodsCouponCandidateMap.put(group.getCartId(), candidateSet);
		}

		// 요청 상품쿠폰 선택값을 cartId 기준으로 정리합니다.
		Map<Long, Long> requestedGoodsSelectionMap = new HashMap<>();
		for (ShopOrderGoodsCouponSelectionVO selection : goodsCouponSelectionList == null ? List.<ShopOrderGoodsCouponSelectionVO>of() : goodsCouponSelectionList) {
			if (selection.getCartId() == null) {
				continue;
			}
			if (!goodsCouponCandidateMap.containsKey(selection.getCartId())) {
				throw new IllegalArgumentException(SHOP_ORDER_DISCOUNT_INVALID_MESSAGE);
			}
			requestedGoodsSelectionMap.put(selection.getCartId(), selection.getCustCpnNo());
		}

		// 행 순서대로 선택 목록을 정규화하고 상품쿠폰 중복 사용 여부를 검증합니다.
		List<ShopOrderGoodsCouponSelectionVO> normalizedGoodsSelectionList = new ArrayList<>();
		Set<Long> usedGoodsCouponSet = new HashSet<>();
		for (ShopCartItemVO cartItem : discountContext.cartItemList()) {
			Long cartId = cartItem == null ? null : cartItem.getCartId();
			Long selectedCustCpnNo = cartId == null ? null : requestedGoodsSelectionMap.get(cartId);
			ShopOrderGoodsCouponSelectionVO selection = new ShopOrderGoodsCouponSelectionVO();
			selection.setCartId(cartId);
			selection.setCustCpnNo(null);
			if (selectedCustCpnNo == null) {
				normalizedGoodsSelectionList.add(selection);
				continue;
			}
			Set<Long> candidateSet = goodsCouponCandidateMap.getOrDefault(cartId, Set.of());
			if (!candidateSet.contains(selectedCustCpnNo) || !usedGoodsCouponSet.add(selectedCustCpnNo)) {
				throw new IllegalArgumentException(SHOP_ORDER_DISCOUNT_INVALID_MESSAGE);
			}
			selection.setCustCpnNo(selectedCustCpnNo);
			normalizedGoodsSelectionList.add(selection);
		}

		// 장바구니/배송비 쿠폰 선택값도 후보 목록 기준으로 정규화합니다.
		ShopOrderDiscountSelectionVO result = new ShopOrderDiscountSelectionVO();
		result.setGoodsCouponSelectionList(normalizedGoodsSelectionList);
		result.setCartCouponCustCpnNo(normalizeSelectedCouponCustCpnNo(couponOption.getCartCouponList(), cartCouponCustCpnNo));
		result.setDeliveryCouponCustCpnNo(normalizeSelectedCouponCustCpnNo(couponOption.getDeliveryCouponList(), deliveryCouponCustCpnNo));
		return result;
	}

	// 선택된 고객 보유 쿠폰 번호가 후보 목록 안에 있는지 검증합니다.
	private Long normalizeSelectedCouponCustCpnNo(List<ShopOrderCouponItemVO> couponList, Long custCpnNo) {
		// 미선택이면 그대로 null을 반환합니다.
		if (custCpnNo == null) {
			return null;
		}

		// 후보 목록에 없는 쿠폰이면 예외를 반환합니다.
		for (ShopOrderCouponItemVO coupon : couponList == null ? List.<ShopOrderCouponItemVO>of() : couponList) {
			if (coupon.getCustCpnNo() == null) {
				continue;
			}
			if (custCpnNo.equals(coupon.getCustCpnNo())) {
				return custCpnNo;
			}
		}
		throw new IllegalArgumentException(SHOP_ORDER_DISCOUNT_INVALID_MESSAGE);
	}

	// 정규화된 상품쿠폰 선택 목록 기준 할인 합계를 계산합니다.
	private int calculateSelectedShopOrderGoodsCouponDiscount(
		ShopOrderDiscountContext discountContext,
		List<ShopOrderGoodsCouponSelectionVO> goodsCouponSelectionList
	) {
		// 선택 목록이 없으면 0원을 반환합니다.
		if (discountContext == null || goodsCouponSelectionList == null || goodsCouponSelectionList.isEmpty()) {
			return 0;
		}

		// cartId 기준으로 쿠폰 선택값을 찾아 각 행 할인액을 누적합니다.
		Map<Long, Long> goodsCouponSelectionMap = new HashMap<>();
		for (ShopOrderGoodsCouponSelectionVO selection : goodsCouponSelectionList) {
			if (selection == null || selection.getCartId() == null || selection.getCustCpnNo() == null) {
				continue;
			}
			goodsCouponSelectionMap.put(selection.getCartId(), selection.getCustCpnNo());
		}
		int result = 0;
		for (ShopCartCouponEstimateRow estimateRow : discountContext.estimateRowList()) {
			if (estimateRow == null || estimateRow.cartId() == null) {
				continue;
			}
			ShopCartCustomerCouponVO coupon = findCustomerCouponByCustCpnNo(discountContext.goodsCouponList(), goodsCouponSelectionMap.get(estimateRow.cartId()));
			result += calculateCouponDiscountAmount(coupon, estimateRow.rowSaleAmt());
		}
		return Math.max(result, 0);
	}

	// 기준 금액에 대해 최대 할인 금액을 주는 쿠폰 1건을 반환합니다.
	private ShopCartCustomerCouponVO findMaximumDiscountCoupon(List<ShopCartCustomerCouponVO> couponList, int baseAmt) {
		// 기준 금액 또는 쿠폰 목록이 없으면 null을 반환합니다.
		if (baseAmt < 1 || couponList == null || couponList.isEmpty()) {
			return null;
		}

		// 각 쿠폰 할인액 중 가장 큰 쿠폰을 선택합니다.
		ShopCartCustomerCouponVO result = null;
		int maxDiscountAmt = 0;
		for (ShopCartCustomerCouponVO coupon : couponList) {
			int discountAmt = calculateCouponDiscountAmount(coupon, baseAmt);
			if (discountAmt <= maxDiscountAmt) {
				continue;
			}
			maxDiscountAmt = discountAmt;
			result = coupon;
		}
		return result;
	}

	// 고객 보유 쿠폰 번호 기준으로 보유 쿠폰 목록에서 단건을 조회합니다.
	private ShopCartCustomerCouponVO findCustomerCouponByCustCpnNo(List<ShopCartCustomerCouponVO> couponList, Long custCpnNo) {
		// 미선택 또는 목록 없음이면 null을 반환합니다.
		if (custCpnNo == null || couponList == null || couponList.isEmpty()) {
			return null;
		}

		// 고객 보유 쿠폰 번호가 일치하는 첫 쿠폰을 반환합니다.
		for (ShopCartCustomerCouponVO coupon : couponList) {
			if (coupon == null || coupon.getCustCpnNo() == null) {
				continue;
			}
			if (custCpnNo.equals(coupon.getCustCpnNo())) {
				return coupon;
			}
		}
		return null;
	}

	// 장바구니 목록을 페이지 응답 형식으로 조합합니다.
	private ShopCartPageVO buildShopCartPage(List<ShopCartItemVO> cartItemList) {
		// 장바구니 이미지 URL/사이즈 옵션/배송비 기준 정보를 조합합니다.
		goodsImageService.applyShopCartItemImageUrls(cartItemList);
		Map<String, List<ShopCartSizeOptionVO>> sizeOptionMap = buildShopCartSizeOptionMap(cartItemList);
		ShopCartSiteInfoVO siteInfo = resolveShopCartSiteInfo();

		// 장바구니 행별 사이즈 옵션 목록을 매핑합니다.
		for (ShopCartItemVO cartItem : cartItemList == null ? List.<ShopCartItemVO>of() : cartItemList) {
			if (isBlank(cartItem.getGoodsId())) {
				continue;
			}
			String normalizedGoodsId = cartItem.getGoodsId().trim();
			cartItem.setSizeOptions(sizeOptionMap.getOrDefault(normalizedGoodsId, List.of()));
		}

		// 장바구니 페이지 응답 객체를 구성합니다.
		List<ShopCartItemVO> safeCartItemList = cartItemList == null ? List.of() : cartItemList;
		ShopCartPageVO result = new ShopCartPageVO();
		result.setCartList(safeCartItemList);
		result.setCartCount(safeCartItemList.size());
		result.setSiteInfo(siteInfo);
		return result;
	}

	// 주문 대상 장바구니 목록과 배송지 목록을 주문서 페이지 응답 형식으로 조합합니다.
	private ShopOrderPageVO buildShopOrderPage(List<ShopCartItemVO> cartItemList, Long custNo, String deviceGbCd, String shopOrigin) {
		// 기존 장바구니 조합 결과에 배송지/쿠폰/포인트/결제 기본 정보를 추가합니다.
		ShopCartPageVO cartPage = buildShopCartPage(cartItemList);
		List<ShopOrderAddressVO> addressList = resolveShopOrderAddressList(custNo);
		ShopOrderDiscountContext discountContext = buildShopOrderDiscountContext(cartPage.getCartList(), custNo);
		ShopOrderDiscountQuoteVO autoDiscountQuote = buildShopOrderAutoDiscountQuote(discountContext);
		ShopOrderCustomerInfoVO customerInfo = resolveShopOrderCustomerInfo(custNo, deviceGbCd);
		ShopOrderPointSaveSummaryVO pointSaveSummary = buildShopOrderPointSaveSummary(cartPage.getCartList(), customerInfo.getCustGradeCd());

		// 주문서 응답 객체를 구성합니다.
		ShopOrderPageVO result = new ShopOrderPageVO();
		result.setCartList(cartPage.getCartList());
		result.setCartCount(cartPage.getCartCount());
		result.setSiteInfo(cartPage.getSiteInfo());
		result.setAddressList(addressList);
		result.setDefaultAddress(findShopOrderDefaultAddress(addressList));
		result.setAvailablePointAmt(discountContext.availablePointAmt());
		result.setCouponOption(buildShopOrderCouponOption(discountContext));
		result.setDiscountSelection(autoDiscountQuote.getDiscountSelection());
		result.setDiscountAmount(autoDiscountQuote.getDiscountAmount());
		result.setPaymentConfig(buildShopOrderPaymentConfig(shopOrigin));
		result.setRefundBankList(resolveShopOrderRefundBankList());
		result.setCustomerInfo(customerInfo);
		result.setPointSaveSummary(pointSaveSummary);
		return result;
	}

	// 주문서 결제 환경 정보를 구성합니다.
	private ShopOrderPaymentConfigVO buildShopOrderPaymentConfig(String shopOrigin) {
		// 성공/실패 URL 베이스와 클라이언트 키를 응답 객체에 채웁니다.
		String normalizedShopOrigin = normalizeShopOrigin(shopOrigin);
		ShopOrderPaymentConfigVO result = new ShopOrderPaymentConfigVO();
		result.setClientKey(resolveShopOrderClientKey());
		result.setApiVersion(SHOP_ORDER_PAYMENT_API_VERSION);
		result.setSuccessUrlBase(buildShopOrderSuccessBaseUrl(normalizedShopOrigin));
		result.setFailUrlBase(buildShopOrderFailBaseUrl(normalizedShopOrigin));
		return result;
	}

	// 현재 주문 고객의 기본 결제 정보를 조회합니다.
	private ShopOrderCustomerInfoVO resolveShopOrderCustomerInfo(Long custNo, String deviceGbCd) {
		// 고객번호 기준 기본 정보를 조회하고 Toss 고객 식별키를 보정합니다.
		ShopOrderCustomerInfoVO customerInfo = orderMapper.getShopOrderCustomerInfo(custNo);
		if (customerInfo == null || customerInfo.getCustNo() == null) {
			throw new IllegalArgumentException("주문 고객 정보를 찾을 수 없습니다.");
		}
		customerInfo.setCustomerKey("SHOP-CUST-" + customerInfo.getCustNo());
		customerInfo.setDeviceGbCd(firstNonBlank(trimToNull(deviceGbCd), "PC"));
		customerInfo.setCustNm(firstNonBlank(trimToNull(customerInfo.getCustNm()), "고객"));
		customerInfo.setEmail(safeValue(firstNonBlank(trimToNull(customerInfo.getEmail()), "")));
		customerInfo.setPhoneNumber(safeValue(firstNonBlank(trimToNull(customerInfo.getPhoneNumber()), "")));
		return customerInfo;
	}

	// 적립 예정 포인트 요약 정보를 구성합니다.
	private ShopOrderPointSaveSummaryVO buildShopOrderPointSaveSummary(List<ShopCartItemVO> cartItemList, String custGradeCd) {
		// 고객등급 적립률과 주문 전체 적립 예정 포인트를 계산합니다.
		int pointSaveRate = resolveShopPointSaveRate(resolveCustGradeCd(custGradeCd));
		int totalExpectedPoint = 0;
		for (ShopCartItemVO cartItem : cartItemList == null ? List.<ShopCartItemVO>of() : cartItemList) {
			totalExpectedPoint += resolveShopOrderPointSaveAmt(cartItem, pointSaveRate);
		}

		// 적립률과 적립 예정 합계를 응답 객체에 설정합니다.
		ShopOrderPointSaveSummaryVO result = new ShopOrderPointSaveSummaryVO();
		result.setPointSaveRate(pointSaveRate);
		result.setTotalExpectedPoint(Math.max(totalExpectedPoint, 0));
		return result;
	}

	// 주문 행 기준 적립 예정 포인트를 계산합니다.
	private int resolveShopOrderPointSaveAmt(ShopCartItemVO cartItem, int pointSaveRate) {
		// 판매가 합계 기준으로 적립 예정 포인트를 계산합니다.
		if (cartItem == null) {
			return 0;
		}
		int rowSaleAmt = normalizeNonNegativeNumber(cartItem.getSaleAmt()) * normalizeQuantity(cartItem.getQty());
		return (int) ((long) rowSaleAmt * (long) Math.max(pointSaveRate, 0) / 100L);
	}

	// 주문 마스터 저장 파라미터를 생성합니다.
	private ShopOrderBaseSavePO buildShopOrderBaseSavePO(
		String from,
		String ordNo,
		Long custNo,
		ShopOrderAddressVO selectedAddress,
		Long deliveryCouponCustCpnNo,
		int baseDeliveryFee,
		int deliveryCouponDiscountAmt,
		String deviceGbCd,
		ShopOrderRefundAccountInfo refundAccountInfo
	) {
		// 주문 출처와 배송지 기준으로 주문 마스터 저장 파라미터를 구성합니다.
		ShopOrderBaseSavePO result = new ShopOrderBaseSavePO();
		result.setOrdNo(ordNo);
		result.setCustNo(custNo);
		result.setOrdStatCd(SHOP_ORDER_STAT_READY);
		result.setRcvNm(selectedAddress.getRsvNm());
		result.setRcvPostNo(selectedAddress.getPostNo());
		result.setRcvAddrBase(selectedAddress.getBaseAddress());
		result.setRcvAddrDtl(selectedAddress.getDetailAddress());
		result.setDelvCpnNo(deliveryCouponCustCpnNo);
		result.setDelvCpnDcAmt(deliveryCouponDiscountAmt);
		result.setOrdDelvAmt(baseDeliveryFee);
		result.setRefundBankCd(refundAccountInfo == null ? null : refundAccountInfo.refundBankCd());
		result.setRefundBankNo(refundAccountInfo == null ? null : refundAccountInfo.refundBankNo());
		result.setRefundHolderNm(refundAccountInfo == null ? null : refundAccountInfo.refundHolderNm());
		result.setCartYn("goods".equalsIgnoreCase(safeValue(firstNonBlank(trimToNull(from), ""))) ? NO : YES);
		result.setDeviceGbCd(firstNonBlank(trimToNull(deviceGbCd), "PC"));
		result.setRegNo(custNo);
		result.setUdtNo(custNo);
		return result;
	}

	// 주문서에서 선택한 배송지명을 현재 고객 배송지 목록에서 조회합니다.
	private ShopOrderAddressVO resolveRequiredShopOrderAddress(Long custNo, String addressNm) {
		// 배송지명이 비어 있으면 주문 진행을 막습니다.
		if (custNo == null || custNo < 1L || isBlank(addressNm)) {
			throw new IllegalArgumentException("배송지를 선택해주세요.");
		}

		// 고객 배송지 목록에서 일치하는 배송지를 찾고, 없으면 예외를 반환합니다.
		ShopOrderAddressVO selectedAddress = findShopOrderAddressByName(resolveShopOrderAddressList(custNo), addressNm);
		if (selectedAddress == null) {
			throw new IllegalArgumentException("배송지를 선택해주세요.");
		}
		return selectedAddress;
	}

	// 주문서에서 노출할 무통장입금 환불은행 목록을 조회합니다.
	private List<CommonCodeVO> resolveShopOrderRefundBankList() {
		// BANK 공통코드가 없으면 빈 목록으로 정규화해 반환합니다.
		List<CommonCodeVO> refundBankList = commonMapper.getCommonCodeList("BANK");
		return refundBankList == null ? List.of() : refundBankList;
	}

	// 결제수단 코드를 필수값 기준으로 검증합니다.
	private String resolveRequiredShopOrderPaymentMethodCd(String paymentMethodCd) {
		// 지원하는 결제수단 코드만 허용합니다.
		String normalizedPaymentMethodCd = trimToNull(paymentMethodCd);
		if (SHOP_ORDER_PAYMENT_METHOD_CARD.equals(normalizedPaymentMethodCd)
			|| SHOP_ORDER_PAYMENT_METHOD_VIRTUAL_ACCOUNT.equals(normalizedPaymentMethodCd)
			|| SHOP_ORDER_PAYMENT_METHOD_TRANSFER.equals(normalizedPaymentMethodCd)) {
			return normalizedPaymentMethodCd;
		}
		throw new IllegalArgumentException("결제수단을 선택해주세요.");
	}

	// 결제 준비 요청의 환불계좌 정보를 결제수단 기준으로 검증/정규화합니다.
	private ShopOrderRefundAccountInfo resolveShopOrderRefundAccountInfo(
		ShopOrderPaymentPreparePO param,
		String paymentMethodCd
	) {
		// 무통장입금이 아니면 환불계좌 저장값을 모두 제거합니다.
		if (!SHOP_ORDER_PAYMENT_METHOD_VIRTUAL_ACCOUNT.equals(paymentMethodCd)) {
			return new ShopOrderRefundAccountInfo(null, null, null);
		}

		// 무통장입금은 환불은행/계좌번호/예금주를 모두 필수로 검증합니다.
		String refundBankCd = trimToNull(param == null ? null : param.getRefundBankCd());
		String refundBankNo = trimToNull(param == null ? null : param.getRefundBankNo());
		String refundHolderNm = trimToNull(param == null ? null : param.getRefundHolderNm());
		if (refundBankCd == null) {
			throw new IllegalArgumentException("환불 은행을 선택해주세요.");
		}
		if (refundBankNo == null) {
			throw new IllegalArgumentException("환불 계좌번호를 입력해주세요.");
		}
		if (!refundBankNo.matches("\\d+")) {
			throw new IllegalArgumentException("환불 계좌번호는 숫자만 입력해주세요.");
		}
		if (refundBankNo.length() > 50) {
			throw new IllegalArgumentException("환불 계좌번호를 확인해주세요.");
		}
		if (refundHolderNm == null) {
			throw new IllegalArgumentException("환불 예금주명을 입력해주세요.");
		}
		if (refundHolderNm.length() > 20) {
			throw new IllegalArgumentException("환불 예금주명을 확인해주세요.");
		}
		validateShopOrderRefundBankCode(refundBankCd);
		return new ShopOrderRefundAccountInfo(refundBankCd, refundBankNo, refundHolderNm);
	}

	// 환불 은행코드가 사용 가능한 BANK 공통코드인지 검증합니다.
	void validateShopOrderRefundBankCode(String refundBankCd) {
		// 은행코드 목록에 존재하지 않으면 주문 결제를 중단합니다.
		boolean exists =
			resolveShopOrderRefundBankList().stream()
				.filter(Objects::nonNull)
				.map(CommonCodeVO::getCd)
				.filter(Objects::nonNull)
				.anyMatch(refundBankCd::equals);
		if (!exists) {
			throw new IllegalArgumentException("환불 은행을 확인해주세요.");
		}
	}

	// 결제수단 코드 기준 Toss method 값을 반환합니다.
	private String resolveTossMethodByPayMethodCd(String paymentMethodCd) {
		// 내부 결제수단 코드와 Toss 결제수단 코드를 매핑합니다.
		if (SHOP_ORDER_PAYMENT_METHOD_CARD.equals(paymentMethodCd)) {
			return SHOP_ORDER_TOSS_METHOD_CARD;
		}
		if (SHOP_ORDER_PAYMENT_METHOD_VIRTUAL_ACCOUNT.equals(paymentMethodCd)) {
			return SHOP_ORDER_TOSS_METHOD_VIRTUAL_ACCOUNT;
		}
		if (SHOP_ORDER_PAYMENT_METHOD_TRANSFER.equals(paymentMethodCd)) {
			return SHOP_ORDER_TOSS_METHOD_TRANSFER;
		}
		throw new IllegalArgumentException("결제수단을 확인해주세요.");
	}

	// 입력 포인트 사용 금액을 최대 사용 가능 금액 기준으로 보정합니다.
	private int clampShopOrderPointUseAmt(Integer pointUseAmt, Integer maxPointUseAmt) {
		// 음수/비정상 입력은 0원으로 보정한 뒤 최대 사용 가능 금액을 적용합니다.
		return Math.min(
			normalizeNonNegativeNumber(pointUseAmt),
			normalizeNonNegativeNumber(maxPointUseAmt)
		);
	}

	// 주문번호를 생성합니다.
	private String generateShopOrderNo(Long custNo) {
		// 접두사/고객번호/년월일시분초밀리초를 조합해 주문번호를 생성합니다.
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
		long safeCustNo = custNo == null ? 0L : Math.max(custNo, 0L);
		return "O" + safeCustNo + timestamp;
	}

	// Toss 주문명을 생성합니다.
	private String buildShopOrderName(List<ShopCartItemVO> cartItemList) {
		// 첫 상품명을 기준으로 다건 여부를 반영한 주문명을 생성합니다.
		if (cartItemList == null || cartItemList.isEmpty() || isBlank(cartItemList.getFirst().getGoodsNm())) {
			return "주문 상품";
		}
		String firstGoodsName = cartItemList.getFirst().getGoodsNm().trim();
		if (cartItemList.size() < 2) {
			return firstGoodsName;
		}
		return firstGoodsName + " 외 " + (cartItemList.size() - 1) + "건";
	}

	// 주문 마스터/상세를 함께 등록합니다.
	private void insertShopOrderBaseAndDetail(
		ShopOrderBaseSavePO orderBaseSavePO,
		Long custNo,
		List<ShopCartItemVO> orderCartItemList,
		ShopOrderDiscountQuoteVO discountQuote,
		int pointUseAmt,
		int pointSaveRate,
		Long auditNo
	) {
		// 주문 마스터를 먼저 등록합니다.
		orderMapper.insertShopOrderBase(orderBaseSavePO);

		// 주문 공통 할인 금액을 판매가 비율 기준으로 행별 배분합니다.
		Map<Long, Long> goodsCouponSelectionMap = buildShopOrderGoodsCouponSelectionMap(discountQuote.getDiscountSelection());
		List<Integer> cartCouponDistribution = distributeShopOrderSharedAmount(
			orderCartItemList,
			normalizeNonNegativeNumber(discountQuote.getDiscountAmount().getCartCouponDiscountAmt())
		);
		List<Integer> pointUseDistribution = distributeShopOrderSharedAmount(orderCartItemList, pointUseAmt);

		// 각 주문 행별 상세 정보를 생성합니다.
		for (int rowIndex = 0; rowIndex < orderCartItemList.size(); rowIndex += 1) {
			ShopCartItemVO cartItem = orderCartItemList.get(rowIndex);
			ShopOrderDetailSavePO detailSavePO = new ShopOrderDetailSavePO();
			detailSavePO.setOrdNo(orderBaseSavePO.getOrdNo());
			detailSavePO.setOrdDtlNo(rowIndex + 1);
			detailSavePO.setOrdGbCd(SHOP_ORDER_ORD_GB_ORDER);
			detailSavePO.setOrdDtlStatCd(SHOP_ORDER_DTL_STAT_READY);
			detailSavePO.setCustNo(custNo);
			detailSavePO.setGoodsId(cartItem.getGoodsId());
			detailSavePO.setSizeId(cartItem.getSizeId());
			detailSavePO.setSupplyAmt(normalizeNonNegativeNumber(cartItem.getSupplyAmt()));
			detailSavePO.setSaleAmt(normalizeNonNegativeNumber(cartItem.getSaleAmt()));
			detailSavePO.setAddAmt(normalizeNonNegativeNumber(cartItem.getAddAmt()));
			detailSavePO.setOrdQty(normalizeQuantity(cartItem.getQty()));
			detailSavePO.setCncQty(0);
			detailSavePO.setRmnQty(normalizeQuantity(cartItem.getQty()));
			detailSavePO.setGoodsCpnNo(goodsCouponSelectionMap.get(cartItem.getCartId()));
			detailSavePO.setGoodsCpnDcAmt(resolveShopOrderGoodsCouponDiscountAmt(cartItem, discountQuote, goodsCouponSelectionMap));
			detailSavePO.setCartCpnNo(discountQuote.getDiscountSelection().getCartCouponCustCpnNo());
			detailSavePO.setCartCpnDcAmt(cartCouponDistribution.get(rowIndex));
			detailSavePO.setPointUseAmt(pointUseDistribution.get(rowIndex));
			detailSavePO.setPointSaveAmt(resolveShopOrderPointSaveAmt(cartItem, pointSaveRate));
			detailSavePO.setExhibitionNo(cartItem.getExhibitionNo());
			detailSavePO.setRegNo(auditNo);
			detailSavePO.setUdtNo(auditNo);
			orderMapper.insertShopOrderDetail(detailSavePO);
		}
	}

	// 상품쿠폰 선택 상태를 cartId 기준 매핑으로 변환합니다.
	private Map<Long, Long> buildShopOrderGoodsCouponSelectionMap(ShopOrderDiscountSelectionVO discountSelection) {
		// 상품쿠폰 선택이 없으면 빈 매핑을 반환합니다.
		Map<Long, Long> result = new HashMap<>();
		if (discountSelection == null || discountSelection.getGoodsCouponSelectionList() == null) {
			return result;
		}

		// cartId 기준 상품쿠폰 고객쿠폰번호 매핑을 구성합니다.
		for (ShopOrderGoodsCouponSelectionVO selection : discountSelection.getGoodsCouponSelectionList()) {
			if (selection == null || selection.getCartId() == null || selection.getCustCpnNo() == null) {
				continue;
			}
			result.put(selection.getCartId(), selection.getCustCpnNo());
		}
		return result;
	}

	// 행별 상품쿠폰 할인 금액을 계산합니다.
	private int resolveShopOrderGoodsCouponDiscountAmt(
		ShopCartItemVO cartItem,
		ShopOrderDiscountQuoteVO discountQuote,
		Map<Long, Long> goodsCouponSelectionMap
	) {
		// 상품쿠폰 미적용 행이면 0원을 반환합니다.
		if (cartItem == null || cartItem.getCartId() == null || goodsCouponSelectionMap == null || goodsCouponSelectionMap.get(cartItem.getCartId()) == null) {
			return 0;
		}

		// 자동/선택 검증이 끝난 쿠폰 항목을 기준으로 행 할인액을 계산합니다.
		Long selectedCustCpnNo = goodsCouponSelectionMap.get(cartItem.getCartId());
		ShopCartCustomerCouponVO coupon = findCustomerCouponByCustCpnNo(
			buildShopOrderDiscountContext(List.of(cartItem), cartItem.getCustNo()).goodsCouponList(),
			selectedCustCpnNo
		);
		int rowSaleAmt = normalizeNonNegativeNumber(cartItem.getSaleAmt()) * normalizeQuantity(cartItem.getQty());
		return calculateCouponDiscountAmount(coupon, rowSaleAmt);
	}

	// 공통 할인 금액을 판매가 비율 기준으로 행별 배분합니다.
	private List<Integer> distributeShopOrderSharedAmount(List<ShopCartItemVO> cartItemList, int totalAmt) {
		// 배분 대상이 없거나 총 금액이 0원이면 같은 길이의 0원 목록을 반환합니다.
		List<Integer> result = new ArrayList<>();
		if (cartItemList == null || cartItemList.isEmpty()) {
			return result;
		}
		for (int index = 0; index < cartItemList.size(); index += 1) {
			result.add(0);
		}
		int normalizedTotalAmt = Math.max(totalAmt, 0);
		if (normalizedTotalAmt < 1) {
			return result;
		}

		// 각 행 판매가 비율 기준으로 배분 후 잔여 금액은 마지막 행에 반영합니다.
		int totalSaleAmt = 0;
		for (ShopCartItemVO cartItem : cartItemList) {
			totalSaleAmt += normalizeNonNegativeNumber(cartItem.getSaleAmt()) * normalizeQuantity(cartItem.getQty());
		}
		if (totalSaleAmt < 1) {
			result.set(result.size() - 1, normalizedTotalAmt);
			return result;
		}
		int distributedAmt = 0;
		for (int index = 0; index < cartItemList.size(); index += 1) {
			ShopCartItemVO cartItem = cartItemList.get(index);
			int rowSaleAmt = normalizeNonNegativeNumber(cartItem.getSaleAmt()) * normalizeQuantity(cartItem.getQty());
			int amount = index == cartItemList.size() - 1
				? normalizedTotalAmt - distributedAmt
				: (int) ((long) normalizedTotalAmt * (long) rowSaleAmt / (long) totalSaleAmt);
			result.set(index, Math.max(amount, 0));
			distributedAmt += Math.max(amount, 0);
		}
		return result;
	}

	// 결제 준비 요청 스냅샷을 PAYMENT.REQ_RAW_JSON 저장용 객체로 구성합니다.
	private Map<String, Object> buildShopOrderPaymentSnapshot(
		String from,
		String goodsId,
		List<ShopCartItemVO> cartItemList,
		String addressNm,
		ShopOrderDiscountSelectionVO discountSelection,
		int pointUseAmt,
		String orderName,
		int amount,
		ShopOrderRefundAccountInfo refundAccountInfo
	) {
		// 재시도 복귀와 후처리에 필요한 최소 정보를 스냅샷으로 저장합니다.
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("from", firstNonBlank(trimToNull(from), isBlank(goodsId) ? "cart" : "goods"));
		result.put("goodsId", trimToNull(goodsId));
		result.put("cartIdList", extractShopOrderCartIdList(cartItemList));
		result.put("addressNm", trimToNull(addressNm));
		result.put("discountSelection", discountSelection);
		result.put("pointUseAmt", pointUseAmt);
		result.put("orderName", orderName);
		result.put("amount", amount);
		result.put("refundBankCd", refundAccountInfo == null ? null : refundAccountInfo.refundBankCd());
		result.put("refundBankNo", refundAccountInfo == null ? null : refundAccountInfo.refundBankNo());
		result.put("refundHolderNm", refundAccountInfo == null ? null : refundAccountInfo.refundHolderNm());
		return result;
	}

	// 결제 스냅샷에서 장바구니 번호 목록을 추출합니다.
	private List<Long> extractShopOrderCartIdList(List<ShopCartItemVO> cartItemList) {
		// 주문 대상 장바구니 목록이 없으면 빈 목록을 반환합니다.
		List<Long> result = new ArrayList<>();
		for (ShopCartItemVO cartItem : cartItemList == null ? List.<ShopCartItemVO>of() : cartItemList) {
			if (cartItem.getCartId() == null || cartItem.getCartId() < 1L) {
				continue;
			}
			result.add(cartItem.getCartId());
		}
		return result;
	}

	// 주문 결제 승인 성공 후 쿠폰/포인트/장바구니 후처리를 수행합니다.
	private void applyShopOrderSuccessSideEffects(ShopOrderPaymentVO payment, Long custNo) {
		// 결제 준비 스냅샷에서 사용 쿠폰/포인트/장바구니 정보를 읽어 후처리를 수행합니다.
		List<Long> usedCouponList = extractShopOrderUsedCouponList(payment.getReqRawJson());
		if (!usedCouponList.isEmpty()) {
			orderMapper.updateShopCustomerCouponUse(custNo, usedCouponList, payment.getOrdNo(), YES, custNo);
		}

		int pointUseAmt = extractShopOrderPointUseAmt(payment.getReqRawJson());
		if (pointUseAmt > 0) {
			consumeShopOrderPoint(custNo, payment.getOrdNo(), pointUseAmt);
		}

		List<Long> cartIdList = extractShopOrderCartIdListFromSnapshot(payment.getReqRawJson());
		if (!cartIdList.isEmpty()) {
			cartMapper.deleteShopCartByCartIdList(custNo, cartIdList);
		}
	}

	// 무통장입금 만료/취소 시 쿠폰/포인트/장바구니를 원복합니다.
	private void restoreShopOrderSuccessSideEffects(ShopOrderPaymentVO payment, Long custNo) {
		// 주문번호 기준 사용 쿠폰과 포인트를 원복하고 장바구니를 다시 생성합니다.
		orderMapper.restoreShopCustomerCouponUse(custNo, payment.getOrdNo(), custNo);
		restoreShopOrderPoint(custNo, payment.getOrdNo());
		restoreShopOrderStock(payment.getOrdNo(), custNo);

		for (ShopOrderRestoreCartItemVO restoreCartItem : orderMapper.getShopOrderRestoreCartItemList(payment.getOrdNo())) {
			if (restoreCartItem == null) {
				continue;
			}
			ShopCartSavePO savePO = new ShopCartSavePO();
			savePO.setCartGbCd(SHOP_CART_GB_CART);
			savePO.setCustNo(restoreCartItem.getCustNo());
			savePO.setGoodsId(restoreCartItem.getGoodsId());
			savePO.setSizeId(restoreCartItem.getSizeId());
			savePO.setQty(restoreCartItem.getOrdQty());
			savePO.setExhibitionNo(restoreCartItem.getExhibitionNo());
			savePO.setRegNo(custNo);
			savePO.setUdtNo(custNo);
			cartMapper.insertShopCart(savePO);
		}
	}

	// 주문번호 기준 주문수량만큼 재고를 선차감합니다.
	private void reserveShopOrderStock(String ordNo, Long auditNo) {
		// 같은 상품/사이즈는 수량을 합산해 원자적으로 재고를 차감합니다.
		for (ShopOrderRestoreCartItemVO stockItem : aggregateShopOrderStockItemList(orderMapper.getShopOrderRestoreCartItemList(ordNo))) {
			if (stockItem == null || isBlank(stockItem.getGoodsId()) || isBlank(stockItem.getSizeId())) {
				continue;
			}
			int requiredQty = normalizeNonNegativeNumber(stockItem.getOrdQty());
			if (requiredQty < 1) {
				continue;
			}
			int updatedCount = orderMapper.deductShopGoodsSizeStock(stockItem.getGoodsId(), stockItem.getSizeId(), requiredQty, auditNo);
			if (updatedCount < 1) {
				throw new IllegalArgumentException(SHOP_ORDER_PAYMENT_STOCK_SHORTAGE_MESSAGE);
			}
		}
	}

	// 주문번호 기준 차감했던 재고를 다시 복구합니다.
	private void restoreShopOrderStock(String ordNo, Long auditNo) {
		// 같은 상품/사이즈는 수량을 합산해 재고를 복구합니다.
		for (ShopOrderRestoreCartItemVO stockItem : aggregateShopOrderStockItemList(orderMapper.getShopOrderRestoreCartItemList(ordNo))) {
			if (stockItem == null || isBlank(stockItem.getGoodsId()) || isBlank(stockItem.getSizeId())) {
				continue;
			}
			int restoreQty = normalizeNonNegativeNumber(stockItem.getOrdQty());
			if (restoreQty < 1) {
				continue;
			}
			orderMapper.restoreShopGoodsSizeStock(stockItem.getGoodsId(), stockItem.getSizeId(), restoreQty, auditNo);
		}
	}

	// 장바구니 목록을 상품/사이즈별 재고 처리 수량으로 합산합니다.
	private List<ShopOrderRestoreCartItemVO> aggregateShopOrderStockItemListFromCart(List<ShopCartItemVO> cartItemList) {
		// 장바구니 행을 재고 처리 공통 구조로 바꾼 뒤 상품/사이즈별로 묶습니다.
		List<ShopOrderRestoreCartItemVO> sourceList = new ArrayList<>();
		if (cartItemList == null) {
			return sourceList;
		}
		for (ShopCartItemVO cartItem : cartItemList) {
			if (cartItem == null) {
				continue;
			}
			ShopOrderRestoreCartItemVO stockItem = new ShopOrderRestoreCartItemVO();
			stockItem.setGoodsId(cartItem.getGoodsId());
			stockItem.setSizeId(cartItem.getSizeId());
			stockItem.setOrdQty(cartItem.getQty());
			sourceList.add(stockItem);
		}
		return aggregateShopOrderStockItemList(sourceList);
	}

	// 주문 상세 목록을 상품/사이즈별 재고 처리 수량으로 합산합니다.
	private List<ShopOrderRestoreCartItemVO> aggregateShopOrderStockItemList(List<ShopOrderRestoreCartItemVO> sourceList) {
		// 동일 상품/사이즈는 하나의 재고 처리 건으로 묶어 반환합니다.
		Map<String, ShopOrderRestoreCartItemVO> stockItemMap = new LinkedHashMap<>();
		if (sourceList == null) {
			return new ArrayList<>();
		}
		for (ShopOrderRestoreCartItemVO sourceItem : sourceList) {
			if (sourceItem == null || isBlank(sourceItem.getGoodsId()) || isBlank(sourceItem.getSizeId())) {
				continue;
			}
			String stockKey = sourceItem.getGoodsId().trim() + "|" + sourceItem.getSizeId().trim();
			ShopOrderRestoreCartItemVO aggregateItem = stockItemMap.get(stockKey);
			if (aggregateItem == null) {
				aggregateItem = new ShopOrderRestoreCartItemVO();
				aggregateItem.setGoodsId(sourceItem.getGoodsId().trim());
				aggregateItem.setSizeId(sourceItem.getSizeId().trim());
				aggregateItem.setOrdQty(0);
				stockItemMap.put(stockKey, aggregateItem);
			}
			aggregateItem.setOrdQty(normalizeNonNegativeNumber(aggregateItem.getOrdQty()) + normalizeNonNegativeNumber(sourceItem.getOrdQty()));
		}
		return new ArrayList<>(stockItemMap.values());
	}

	// 고객 포인트를 주문 결제 금액만큼 차감합니다.
	private void consumeShopOrderPoint(Long custNo, String ordNo, int pointUseAmt) {
		// 사용 가능한 포인트 행을 오래된 순서로 소진합니다.
		int remainingAmt = Math.max(pointUseAmt, 0);
		for (ShopOrderPointBaseVO pointBase : orderMapper.getShopAvailablePointBaseList(custNo)) {
			if (pointBase == null || pointBase.getPntNo() == null || remainingAmt < 1) {
				continue;
			}
			int usableAmt = Math.min(normalizeNonNegativeNumber(pointBase.getRmnAmt()), remainingAmt);
			if (usableAmt < 1) {
				continue;
			}
			orderMapper.updateShopCustomerPointUseAmt(pointBase.getPntNo(), usableAmt, custNo);
			ShopOrderPointDetailSavePO detailSavePO = new ShopOrderPointDetailSavePO();
			detailSavePO.setPntNo(pointBase.getPntNo());
			detailSavePO.setPntAmt(-usableAmt);
			detailSavePO.setOrdNo(ordNo);
			detailSavePO.setBigo(SHOP_ORDER_POINT_USE_MEMO);
			detailSavePO.setRegNo(custNo);
			orderMapper.insertShopOrderPointDetail(detailSavePO);
			remainingAmt -= usableAmt;
		}
		if (remainingAmt > 0) {
			throw new IllegalStateException("포인트 사용 처리에 실패했습니다.");
		}
	}

	// 주문번호 기준 차감 포인트를 원복합니다.
	private void restoreShopOrderPoint(Long custNo, String ordNo) {
		// 주문번호 기준 음수 사용 상세 이력을 읽어 반대로 복구합니다.
		for (ShopOrderPointDetailVO pointDetail : orderMapper.getShopOrderPointDetailList(ordNo)) {
			if (pointDetail == null || pointDetail.getPntNo() == null) {
				continue;
			}
			int restoreAmt = Math.abs(normalizeNonNegativeNumber(pointDetail.getPntAmt() == null ? null : Math.abs(pointDetail.getPntAmt())));
			if (restoreAmt < 1) {
				continue;
			}
			orderMapper.restoreShopCustomerPointUseAmt(pointDetail.getPntNo(), restoreAmt, custNo);
			ShopOrderPointDetailSavePO restoreDetail = new ShopOrderPointDetailSavePO();
			restoreDetail.setPntNo(pointDetail.getPntNo());
			restoreDetail.setPntAmt(restoreAmt);
			restoreDetail.setOrdNo(ordNo);
			restoreDetail.setBigo(SHOP_ORDER_POINT_RESTORE_MEMO);
			restoreDetail.setRegNo(custNo);
			orderMapper.insertShopOrderPointDetail(restoreDetail);
		}
	}

	// 결제 완료 응답 객체를 PAYMENT 기준으로 구성합니다.
	private ShopOrderPaymentConfirmVO buildShopOrderPaymentConfirmResult(ShopOrderPaymentVO payment) {
		// 결제 요약 정보와 가상계좌 정보를 응답 객체에 채웁니다.
		ShopOrderPaymentConfirmVO result = new ShopOrderPaymentConfirmVO();
		result.setOrdNo(payment == null ? null : payment.getOrdNo());
		result.setPayNo(payment == null ? null : payment.getPayNo());
		result.setPayMethodCd(payment == null ? null : payment.getPayMethodCd());
		result.setPayStatCd(payment == null ? null : payment.getPayStatCd());
		result.setOrdStatCd(resolveShopOrderStatusByPayment(payment));
		result.setOrderName(payment == null ? null : readShopOrderSnapshotValue(payment.getReqRawJson(), "orderName"));
		result.setAmount(payment == null ? null : payment.getPayAmt());
		result.setBankCd(payment == null ? null : payment.getBankCd());
		result.setBankNm(resolveShopOrderBankName(payment == null ? null : payment.getBankCd()));
		result.setBankNo(payment == null ? null : payment.getBankNo());
		result.setVactHolderNm(payment == null ? null : payment.getVactHolderNm());
		result.setVactDueDt(payment == null ? null : payment.getVactDueDt());
		return result;
	}

	// 가상계좌 은행코드를 공통코드 기준 은행명으로 변환합니다.
	private String resolveShopOrderBankName(String bankCd) {
		// 은행코드가 비어 있으면 빈 문자열을 반환합니다.
		if (isBlank(bankCd)) {
			return "";
		}

		// 공통코드 BANK 그룹에서 은행명을 조회합니다.
		return shopAuthService.getCommonCodeName(SHOP_ORDER_BANK_GRP_CD, bankCd);
	}

	// PAYMENT 상태 기준 주문상태 코드를 계산합니다.
	private String resolveShopOrderStatusByPayment(ShopOrderPaymentVO payment) {
		// 결제상태 기준으로 주문상태 코드를 맞춰 반환합니다.
		if (payment == null || isBlank(payment.getPayStatCd())) {
			return SHOP_ORDER_STAT_READY;
		}
		if (SHOP_ORDER_PAY_STAT_DONE.equals(payment.getPayStatCd())) {
			return SHOP_ORDER_STAT_DONE;
		}
		if (SHOP_ORDER_PAY_STAT_WAITING_DEPOSIT.equals(payment.getPayStatCd())) {
			return SHOP_ORDER_STAT_WAITING_DEPOSIT;
		}
		if (SHOP_ORDER_PAY_STAT_FAIL.equals(payment.getPayStatCd()) || SHOP_ORDER_PAY_STAT_CANCEL.equals(payment.getPayStatCd())) {
			return SHOP_ORDER_STAT_CANCEL;
		}
		return SHOP_ORDER_STAT_READY;
	}

	// 선택 고객쿠폰 번호 목록을 결제 스냅샷에서 추출합니다.
	private List<Long> extractShopOrderUsedCouponList(String snapshotJson) {
		// 상품/장바구니/배송비 쿠폰 선택값을 하나의 목록으로 모읍니다.
		List<Long> result = new ArrayList<>();
		JsonNode rootNode = readShopOrderJsonNode(snapshotJson);
		JsonNode selectionNode = rootNode.path("discountSelection");
		for (JsonNode goodsCouponNode : selectionNode.path("goodsCouponSelectionList")) {
			long custCpnNo = goodsCouponNode.path("custCpnNo").asLong(0L);
			if (custCpnNo > 0L) {
				result.add(custCpnNo);
			}
		}
		long cartCouponCustCpnNo = selectionNode.path("cartCouponCustCpnNo").asLong(0L);
		long deliveryCouponCustCpnNo = selectionNode.path("deliveryCouponCustCpnNo").asLong(0L);
		if (cartCouponCustCpnNo > 0L) {
			result.add(cartCouponCustCpnNo);
		}
		if (deliveryCouponCustCpnNo > 0L) {
			result.add(deliveryCouponCustCpnNo);
		}
		return result;
	}

	// 결제 스냅샷에서 포인트 사용 금액을 추출합니다.
	private int extractShopOrderPointUseAmt(String snapshotJson) {
		// pointUseAmt 필드를 정수로 반환합니다.
		return readShopOrderJsonNode(snapshotJson).path("pointUseAmt").asInt(0);
	}

	// 결제 스냅샷에서 장바구니 번호 목록을 추출합니다.
	private List<Long> extractShopOrderCartIdListFromSnapshot(String snapshotJson) {
		// cartIdList 배열을 Long 목록으로 변환합니다.
		List<Long> result = new ArrayList<>();
		for (JsonNode cartIdNode : readShopOrderJsonNode(snapshotJson).path("cartIdList")) {
			long cartId = cartIdNode.asLong(0L);
			if (cartId > 0L) {
				result.add(cartId);
			}
		}
		return result;
	}

	// 결제 스냅샷에서 지정한 문자열 필드 값을 읽습니다.
	private String readShopOrderSnapshotValue(String snapshotJson, String fieldName) {
		// 스냅샷이 비어 있으면 빈 문자열을 반환합니다.
		return resolveJsonText(readShopOrderJsonNode(snapshotJson), fieldName);
	}

	// JSON 문자열을 JsonNode로 변환합니다.
	JsonNode readShopOrderJsonNode(String rawJson) {
		// 빈 문자열이면 빈 객체 노드를 반환합니다.
		String normalizedRawJson = trimToNull(rawJson);
		if (normalizedRawJson == null) {
			return objectMapper.createObjectNode();
		}
		try {
			return objectMapper.readTree(normalizedRawJson);
		} catch (JsonProcessingException exception) {
			return objectMapper.createObjectNode();
		}
	}

	// 객체를 JSON 문자열로 직렬화합니다.
	String writeShopOrderJson(Object value) {
		// 직렬화에 실패해도 빈 JSON 객체 문자열을 반환합니다.
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException exception) {
			return "{}";
		}
	}

	// Toss 승인 API를 호출하고 원본 응답 문자열을 반환합니다.
	private String confirmTossPayment(ShopOrderPaymentConfirmPO param) {
		// 결제키/주문번호/금액 기준으로 Toss 승인 API를 호출합니다.
		return tossPaymentsClient.confirmPayment(param.getPaymentKey().trim(), param.getOrdNo().trim(), param.getAmount());
	}

	// JSON 노드에서 지정한 필드 문자열 값을 안전하게 조회합니다.
	String resolveJsonText(JsonNode node, String fieldName) {
		// 노드 또는 필드명이 없으면 빈 문자열을 반환합니다.
		if (node == null || isBlank(fieldName)) {
			return "";
		}
		JsonNode targetNode = node.path(fieldName);
		if (targetNode.isMissingNode() || targetNode.isNull()) {
			return "";
		}
		return targetNode.asText("");
	}

	// 주문 결제용 URL Origin 값을 정규화합니다.
	private String normalizeShopOrigin(String shopOrigin) {
		// 끝 슬래시를 제거해 절대 URL 베이스로 정리합니다.
		String normalizedShopOrigin = trimToNull(shopOrigin);
		if (normalizedShopOrigin == null) {
			return "";
		}
		return normalizedShopOrigin.endsWith("/") ? normalizedShopOrigin.substring(0, normalizedShopOrigin.length() - 1) : normalizedShopOrigin;
	}

	// 결제 성공 URL 베이스를 반환합니다.
	private String buildShopOrderSuccessBaseUrl(String shopOrigin) {
		// 절대 URL Origin이 있으면 절대 경로를, 없으면 상대 경로를 반환합니다.
		return isBlank(shopOrigin) ? "/order/success" : shopOrigin + "/order/success";
	}

	// 결제 실패 URL 베이스를 반환합니다.
	private String buildShopOrderFailBaseUrl(String shopOrigin) {
		// 절대 URL Origin이 있으면 절대 경로를, 없으면 상대 경로를 반환합니다.
		return isBlank(shopOrigin) ? "/order/fail" : shopOrigin + "/order/fail";
	}

	// 결제 성공 URL을 생성합니다.
	private String buildShopOrderSuccessUrl(String shopOrigin, Long payNo) {
		// 결제번호를 포함한 success URL을 반환합니다.
		return buildShopOrderSuccessBaseUrl(shopOrigin) + "?payNo=" + payNo;
	}

	// 결제 실패 URL을 생성합니다.
	private String buildShopOrderFailUrl(
		String shopOrigin,
		Long payNo,
		String from,
		String goodsId,
		List<ShopCartItemVO> cartItemList
	) {
		// 원래 주문서 복귀에 필요한 출처/상품/장바구니 번호를 fail URL에 담습니다.
		StringBuilder builder = new StringBuilder(buildShopOrderFailBaseUrl(shopOrigin));
		builder.append("?payNo=").append(payNo);
		builder.append("&from=").append(encodeShopOrderUrlValue(firstNonBlank(trimToNull(from), isBlank(goodsId) ? "cart" : "goods")));
		if (!isBlank(goodsId)) {
			builder.append("&goodsId=").append(encodeShopOrderUrlValue(goodsId.trim()));
		}
		for (Long cartId : extractShopOrderCartIdList(cartItemList)) {
			builder.append("&cartId=").append(cartId);
		}
		return builder.toString();
	}

	// URL 쿼리값을 인코딩합니다.
	private String encodeShopOrderUrlValue(String value) {
		// UTF-8 기준으로 안전하게 인코딩합니다.
		return URLEncoder.encode(safeValue(firstNonBlank(value, "")), StandardCharsets.UTF_8);
	}

	// Toss 클라이언트 키를 반환합니다.
	private String resolveShopOrderClientKey() {
		return safeValue(firstNonBlank(trimToNull(tossProperties.clientKey()), ""));
	}

	// 다양한 날짜 문자열을 주문 결제 저장용 형식으로 정규화합니다.
	String normalizeShopOrderDateTime(String value) {
		// 빈 문자열이면 null을 반환합니다.
		String normalizedValue = trimToNull(value);
		if (normalizedValue == null) {
			return null;
		}
		try {
			return OffsetDateTime.parse(normalizedValue).toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		} catch (Exception ignored) {
			// OffsetDateTime 파싱이 실패하면 다음 형식으로 재시도합니다.
		}
		try {
			return LocalDateTime.parse(normalizedValue).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		} catch (Exception ignored) {
			return normalizedValue.replace('T', ' ');
		}
	}

	// SHA-256 해시 문자열을 생성합니다.
	private String sha256Hex(String value) {
		// 입력 문자열이 비어 있으면 빈 문자열을 반환합니다.
		String normalizedValue = trimToNull(value);
		if (normalizedValue == null) {
			return "";
		}
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
			byte[] digest = messageDigest.digest(normalizedValue.getBytes(StandardCharsets.UTF_8));
			StringBuilder builder = new StringBuilder();
			for (byte currentByte : digest) {
				builder.append(String.format("%02x", currentByte));
			}
			return builder.toString();
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("결제키 해시 생성에 실패했습니다.", exception);
		}
	}

	// 결제 실패 코드가 사용자 취소 계열인지 확인합니다.
	private boolean isShopOrderCancelFailureCode(String code) {
		// 코드에 CANCEL이 포함되면 사용자 취소 계열로 판단합니다.
		String normalizedCode = trimToNull(code);
		return normalizedCode != null && normalizedCode.toUpperCase().contains("CANCEL");
	}

	// 현재 고객의 배송지 목록을 기본 배송지 우선 순서로 조회합니다.
	private List<ShopOrderAddressVO> resolveShopOrderAddressList(Long custNo) {
		List<ShopOrderAddressVO> addressList = orderMapper.getShopOrderAddressList(custNo);
		return addressList == null ? List.of() : addressList;
	}

	// 배송지 목록에서 기본 배송지를 조회합니다.
	private ShopOrderAddressVO findShopOrderDefaultAddress(List<ShopOrderAddressVO> addressList) {
		if (addressList == null || addressList.isEmpty()) {
			return null;
		}

		// 기본 배송지 표시가 Y인 첫 행을 반환합니다.
		for (ShopOrderAddressVO address : addressList) {
			if (address == null || !YES.equals(address.getDefaultYn())) {
				continue;
			}
			return address;
		}
		return null;
	}

	// 배송지 목록에서 배송지 별칭으로 단건을 조회합니다.
	private ShopOrderAddressVO findShopOrderAddressByName(List<ShopOrderAddressVO> addressList, String addressNm) {
		if (addressList == null || addressList.isEmpty() || isBlank(addressNm)) {
			return null;
		}

		// 고객 기준 복합키의 두 번째 값인 주소별칭으로 등록 결과를 찾습니다.
		String normalizedAddressNm = addressNm.trim();
		for (ShopOrderAddressVO address : addressList) {
			if (address == null || isBlank(address.getAddressNm())) {
				continue;
			}
			if (normalizedAddressNm.equals(address.getAddressNm().trim())) {
				return address;
			}
		}
		return null;
	}

	// 주문서 배송지 등록 입력값을 검증하고 저장 PO로 변환합니다.
	private ShopOrderAddressSavePO validateShopOrderAddressRegisterInput(ShopOrderAddressRegisterPO param, Long custNo) {
		// 요청 객체와 필수 문자열 입력값을 검증합니다.
		if (param == null) {
			throw new IllegalArgumentException("배송지 정보를 확인해주세요.");
		}
		String normalizedAddressNm = trimToNull(param.getAddressNm());
		String normalizedPostNo = trimToNull(param.getPostNo());
		String normalizedBaseAddress = trimToNull(param.getBaseAddress());
		String normalizedDetailAddress = trimToNull(param.getDetailAddress());
		String normalizedPhoneNumber = trimToNull(param.getPhoneNumber());
		String normalizedRsvNm = trimToNull(param.getRsvNm());
		String normalizedDefaultYn = normalizeYesNo(param.getDefaultYn());
		if (normalizedAddressNm == null) {
			throw new IllegalArgumentException("배송지명을 입력해주세요.");
		}
		if (normalizedAddressNm.length() > 20) {
			throw new IllegalArgumentException("배송지명은 20자 이내로 입력해주세요.");
		}
		if (normalizedPostNo == null || !normalizedPostNo.matches("^\\d{5,6}$")) {
			throw new IllegalArgumentException("검색한 주소를 확인해주세요.");
		}
		if (normalizedBaseAddress == null) {
			throw new IllegalArgumentException("검색한 주소를 확인해주세요.");
		}
		if (normalizedBaseAddress.length() > 125) {
			throw new IllegalArgumentException("기본주소 길이를 확인해주세요.");
		}
		if (normalizedDetailAddress == null) {
			throw new IllegalArgumentException("상세주소를 입력해주세요.");
		}
		if (normalizedDetailAddress.length() > 125) {
			throw new IllegalArgumentException("상세주소는 125자 이내로 입력해주세요.");
		}
		if (normalizedPhoneNumber == null || !isValidShopOrderPhoneNumber(normalizedPhoneNumber)) {
			throw new IllegalArgumentException("연락처 형식을 확인해주세요.");
		}
		if (normalizedPhoneNumber.length() > 20) {
			throw new IllegalArgumentException("연락처 길이를 확인해주세요.");
		}
		if (normalizedRsvNm == null) {
			throw new IllegalArgumentException("받는 사람을 입력해주세요.");
		}
		if (normalizedRsvNm.length() > 20) {
			throw new IllegalArgumentException("받는 사람은 20자 이내로 입력해주세요.");
		}

		// 검증 완료된 값을 저장 PO에 채워 반환합니다.
		ShopOrderAddressSavePO savePO = new ShopOrderAddressSavePO();
		savePO.setCustNo(custNo);
		savePO.setAddressNm(normalizedAddressNm);
		savePO.setPostNo(normalizedPostNo);
		savePO.setBaseAddress(normalizedBaseAddress);
		savePO.setDetailAddress(normalizedDetailAddress);
		savePO.setPhoneNumber(normalizedPhoneNumber);
		savePO.setRsvNm(normalizedRsvNm);
		savePO.setDefaultYn(normalizedDefaultYn);
		savePO.setRegNo(custNo);
		savePO.setUdtNo(custNo);
		return savePO;
	}

	// 주문서 주소 검색 현재 페이지를 1 이상으로 보정합니다.
	private int normalizeShopOrderAddressSearchCurrentPage(Integer currentPage) {
		if (currentPage == null || currentPage < 1) {
			return SHOP_ORDER_ADDRESS_SEARCH_DEFAULT_PAGE;
		}
		return currentPage;
	}

	// 주문서 주소 검색 페이지당 건수를 1~100 범위로 보정합니다.
	private int normalizeShopOrderAddressSearchCountPerPage(Integer countPerPage) {
		if (countPerPage == null || countPerPage < 1) {
			return SHOP_ORDER_ADDRESS_SEARCH_DEFAULT_COUNT;
		}
		return Math.min(countPerPage, SHOP_ORDER_ADDRESS_SEARCH_MAX_COUNT);
	}

	// 주문서 연락처 형식을 확인합니다.
	private boolean isValidShopOrderPhoneNumber(String phoneNumber) {
		if (isBlank(phoneNumber)) {
			return false;
		}
		return phoneNumber.matches("^\\d{2,3}-\\d{3,4}-\\d{4}$");
	}

	// 쇼핑몰 장바구니 상품별 사이즈 옵션 목록을 구성합니다.
	private Map<String, List<ShopCartSizeOptionVO>> buildShopCartSizeOptionMap(List<ShopCartItemVO> cartItemList) {
		// 장바구니 목록이 없으면 빈 매핑을 반환합니다.
		Map<String, List<ShopCartSizeOptionVO>> sizeOptionMap = new HashMap<>();
		if (cartItemList == null || cartItemList.isEmpty()) {
			return sizeOptionMap;
		}

		// 상품코드별로 한 번씩만 사이즈 목록을 조회해 옵션으로 변환합니다.
		for (ShopCartItemVO cartItem : cartItemList) {
			if (cartItem == null || isBlank(cartItem.getGoodsId())) {
				continue;
			}
			String normalizedGoodsId = cartItem.getGoodsId().trim();
			if (sizeOptionMap.containsKey(normalizedGoodsId)) {
				continue;
			}

			// 상품 사이즈 목록을 장바구니 옵션 응답 형식으로 변환합니다.
			List<ShopGoodsSizeItemVO> sizeList = goodsMapper.getShopGoodsSizeList(normalizedGoodsId);
			List<ShopCartSizeOptionVO> optionList = new ArrayList<>();
			for (ShopGoodsSizeItemVO sizeItem : sizeList == null ? List.<ShopGoodsSizeItemVO>of() : sizeList) {
				if (isBlank(sizeItem.getSizeId())) {
					continue;
				}
				int stockQty = sizeItem.getStockQty() == null ? 0 : Math.max(sizeItem.getStockQty(), 0);
				ShopCartSizeOptionVO option = new ShopCartSizeOptionVO();
				option.setSizeId(sizeItem.getSizeId());
				option.setStockQty(stockQty);
				option.setSoldOut(stockQty < 1);
				optionList.add(option);
			}
			sizeOptionMap.put(normalizedGoodsId, optionList);
		}
		return sizeOptionMap;
	}

	// 상품 사이즈 목록에서 지정한 사이즈코드와 일치하는 항목을 찾습니다.
	private ShopGoodsSizeItemVO findShopGoodsSizeBySizeId(List<ShopGoodsSizeItemVO> sizeList, String sizeId) {
		// 비교할 사이즈코드가 없으면 null을 반환합니다.
		if (isBlank(sizeId) || sizeList == null || sizeList.isEmpty()) {
			return null;
		}

		// 사이즈 목록을 순회하며 일치 항목을 반환합니다.
		for (ShopGoodsSizeItemVO sizeItem : sizeList) {
			if (sizeItem == null || isBlank(sizeItem.getSizeId())) {
				continue;
			}
			if (sizeId.equals(sizeItem.getSizeId().trim())) {
				return sizeItem;
			}
		}
		return null;
	}

	// 쇼핑몰 장바구니 배송 기준 정보를 조회합니다.
	ShopCartSiteInfoVO resolveShopCartSiteInfo() {
		// 사이트 아이디 기준 배송 기준 정보를 조회합니다.
		return createShopCartSiteInfo(siteInfoMapper.getShopSiteInfo(SHOP_SITE_ID));
	}

	// 사이트 배송 기준 정보를 장바구니 응답 형식으로 변환합니다.
	private ShopCartSiteInfoVO createShopCartSiteInfo(ShopSiteInfoVO siteInfo) {
		// 조회 결과가 없으면 0원 기본값을 반환합니다.
		ShopCartSiteInfoVO result = new ShopCartSiteInfoVO();
		result.setSiteId(isBlank(siteInfo == null ? null : siteInfo.getSiteId()) ? SHOP_SITE_ID : siteInfo.getSiteId());
		result.setDeliveryFee(siteInfo == null || siteInfo.getDeliveryFee() == null ? 0 : Math.max(siteInfo.getDeliveryFee(), 0));
		result.setDeliveryFeeLimit(
			siteInfo == null || siteInfo.getDeliveryFeeLimit() == null ? 0 : Math.max(siteInfo.getDeliveryFeeLimit(), 0)
		);
		return result;
	}

	// 쿠폰 타겟 코드와 상품 비교값 기준으로 적용 가능 여부를 계산합니다.
	private boolean isMatchedShopCouponTarget(
		String cpnTargetCd,
		List<ShopGoodsCouponTargetVO> targetList,
		String goodsId,
		String brandNoValue,
		Set<String> categoryIdSet,
		Set<String> exhibitionTabNoSet
	) {
		// 쿠폰 타겟 기준으로 적용 여부를 계산합니다.
		boolean applied = false;
		if (CPN_TARGET_ALL.equals(cpnTargetCd)) {
			applied = true;
		} else if (CPN_TARGET_GOODS.equals(cpnTargetCd)) {
			applied = hasCouponApplyTarget(targetList, goodsId);
		} else if (CPN_TARGET_BRAND.equals(cpnTargetCd)) {
			applied = hasCouponApplyTarget(targetList, brandNoValue);
		} else if (CPN_TARGET_CATEGORY.equals(cpnTargetCd)) {
			applied = hasCouponApplyAnyTarget(targetList, categoryIdSet);
		} else if (CPN_TARGET_EXHIBITION.equals(cpnTargetCd)) {
			applied = hasCouponApplyAnyTarget(targetList, exhibitionTabNoSet);
		}
		if (!applied) {
			return false;
		}

		// 상품 제외 타겟이 존재하면 미적용 처리합니다.
		return !hasCouponExcludeTarget(targetList, goodsId);
	}

	// 쿠폰 적용 타겟이 단일 값과 일치하는지 확인합니다.
	private boolean hasCouponApplyTarget(List<ShopGoodsCouponTargetVO> targetList, String expectedValue) {
		// 비교값이 없으면 매칭하지 않습니다.
		if (isBlank(expectedValue) || targetList == null || targetList.isEmpty()) {
			return false;
		}
		for (ShopGoodsCouponTargetVO target : targetList) {
			if (target == null) {
				continue;
			}
			if (TARGET_GB_APPLY.equals(target.getTargetGbCd()) && expectedValue.equals(target.getTargetValue())) {
				return true;
			}
		}
		return false;
	}

	// 쿠폰 적용 타겟이 다중 후보값 중 하나와 일치하는지 확인합니다.
	private boolean hasCouponApplyAnyTarget(List<ShopGoodsCouponTargetVO> targetList, Set<String> candidateValueSet) {
		// 비교 후보가 없으면 매칭하지 않습니다.
		if (candidateValueSet == null || candidateValueSet.isEmpty() || targetList == null || targetList.isEmpty()) {
			return false;
		}
		for (ShopGoodsCouponTargetVO target : targetList) {
			if (target == null || !TARGET_GB_APPLY.equals(target.getTargetGbCd())) {
				continue;
			}
			if (candidateValueSet.contains(target.getTargetValue())) {
				return true;
			}
		}
		return false;
	}

	// 쿠폰 제외 타겟에 현재 상품코드가 포함되는지 확인합니다.
	private boolean hasCouponExcludeTarget(List<ShopGoodsCouponTargetVO> targetList, String goodsId) {
		// 상품코드가 없으면 제외 판정을 수행하지 않습니다.
		if (isBlank(goodsId) || targetList == null || targetList.isEmpty()) {
			return false;
		}
		for (ShopGoodsCouponTargetVO target : targetList) {
			if (target == null) {
				continue;
			}
			if (TARGET_GB_EXCLUDE.equals(target.getTargetGbCd()) && goodsId.equals(target.getTargetValue())) {
				return true;
			}
		}
		return false;
	}

	// 고객등급 코드를 화면 계산용 기본값 규칙으로 보정합니다.
	private String resolveCustGradeCd(String custGradeCd) {
		// 쿠키 고객등급이 비어있으면 WELCOME 등급을 기본값으로 사용합니다.
		if (isBlank(custGradeCd)) {
			return DEFAULT_CUST_GRADE_CD;
		}
		return custGradeCd.trim();
	}

	// 고객등급별 포인트 적립률을 조회하고 기본값으로 보정합니다.
	private int resolveShopPointSaveRate(String custGradeCd) {
		// 지정 등급의 적립률을 조회합니다.
		Integer pointSaveRate = goodsMapper.getShopPointSaveRateByCustGradeCd(custGradeCd);
		if (pointSaveRate != null && pointSaveRate >= 0) {
			return pointSaveRate;
		}

		// 지정 등급이 없으면 WELCOME 등급 적립률로 재조회합니다.
		Integer defaultPointSaveRate = goodsMapper.getShopPointSaveRateByCustGradeCd(DEFAULT_CUST_GRADE_CD);
		if (defaultPointSaveRate != null && defaultPointSaveRate >= 0) {
			return defaultPointSaveRate;
		}
		return 0;
	}

	// 문자열 목록을 null 안전한 Set으로 변환합니다.
	private Set<String> toSafeStringSet(List<String> sourceList) {
		// 원본 목록이 없으면 빈 Set을 반환합니다.
		if (sourceList == null || sourceList.isEmpty()) {
			return Set.of();
		}
		return new HashSet<>(sourceList);
	}
}
