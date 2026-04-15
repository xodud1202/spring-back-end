package com.xodud1202.springbackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xodud1202.springbackend.config.properties.TossProperties;
import com.xodud1202.springbackend.domain.admin.brand.BrandVO;
import com.xodud1202.springbackend.domain.admin.category.*;
import com.xodud1202.springbackend.domain.admin.goods.*;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderStartDeliveryPrepareItemPO;
import com.xodud1202.springbackend.domain.common.CommonCodeVO;
import com.xodud1202.springbackend.domain.shop.cart.*;
import com.xodud1202.springbackend.domain.shop.category.ShopCategoryGoodsItemVO;
import com.xodud1202.springbackend.domain.shop.goods.*;
import com.xodud1202.springbackend.domain.shop.mypage.*;
import com.xodud1202.springbackend.domain.shop.site.ShopSiteInfoVO;
import com.xodud1202.springbackend.entity.UserBaseEntity;
import com.xodud1202.springbackend.mapper.*;
import com.xodud1202.springbackend.service.goods.support.CategoryGoodsExcelRow;
import com.xodud1202.springbackend.service.goods.support.ShopMypageCouponUnavailableGoodsSummary;
import com.xodud1202.springbackend.service.order.support.ShopCartCouponEstimateRow;
import com.xodud1202.springbackend.service.order.support.ShopCartValidatedInput;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.xodud1202.springbackend.common.Constants.Common.NO;
import static com.xodud1202.springbackend.common.Constants.Common.YES;
import static com.xodud1202.springbackend.common.Constants.Shop.*;
import static com.xodud1202.springbackend.common.util.CommonPaginationUtils.*;
import static com.xodud1202.springbackend.common.util.CommonTextUtils.isBlank;
import static com.xodud1202.springbackend.common.util.CommonTextUtils.trimToNull;

@Service
@RequiredArgsConstructor
// 관리자 상품 관련 비즈니스 로직을 처리합니다.
public class GoodsService {
	private static final Logger log = LoggerFactory.getLogger(GoodsService.class);

	private final CartMapper cartMapper;
	private final GoodsMapper goodsMapper;
	private final CommonMapper commonMapper;
	private final ExhibitionMapper exhibitionMapper;
	private final SiteInfoMapper siteInfoMapper;
	private final FtpFileService ftpFileService;
	private final GoodsImageService goodsImageService;
	private final ShopCustomerCouponService shopCustomerCouponService;
	private final JusoAddressApiClient jusoAddressApiClient;
	private final TossPaymentsClient tossPaymentsClient;
	private final TossProperties tossProperties;
	private final ObjectMapper objectMapper;
	private final PlatformTransactionManager transactionManager;
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

	// 관리자 상품 목록을 페이징 조건으로 조회합니다.
	public Map<String, Object> getAdminGoodsList(GoodsPO param) {
		int page = normalizePage(param.getPage(), 1);
		// 페이지 사이즈 기본값과 최대값을 설정합니다.
		int pageSize = normalizePageSize(param.getPageSize(), 20, 200);
		int offset = calculateOffset(page, pageSize);

		param.setPage(page);
		param.setPageSize(pageSize);
		param.setOffset(offset);
		param.setSearchKeyword(buildGoodsNameSearchKeyword(param));

		List<GoodsVO> list = goodsMapper.getAdminGoodsList(param);
		// 상품 이미지 URL을 세팅합니다.
		goodsImageService.applyAdminGoodsListImageUrls(list);
		int totalCount = goodsMapper.getAdminGoodsCount(param);

		Map<String, Object> result = new HashMap<>();
		result.put("list", list);
		result.put("totalCount", totalCount);
		result.put("page", page);
		result.put("pageSize", pageSize);
		return result;
	}

	// 상품 분류 목록을 조회합니다.
	public List<GoodsMerchVO> getGoodsMerchList() {
		return goodsMapper.getGoodsMerchList();
	}

	// 브랜드 목록을 조회합니다.
	public List<BrandVO> getBrandList() {
		return goodsMapper.getBrandList();
	}

	// 관리자 상품 상세 정보를 조회합니다.
	public GoodsDetailVO getAdminGoodsDetail(String goodsId) {
		if (isBlank(goodsId)) {
			return null;
		}
		return goodsMapper.getAdminGoodsDetail(goodsId);
	}

	// 관리자 상품을 등록합니다.
	@Transactional
	public int insertAdminGoods(GoodsSavePO param) {
		if (param != null && param.getUdtNo() == null) {
			param.setUdtNo(param.getRegNo());
		}
		int result = goodsMapper.insertAdminGoods(param);
		saveAdminGoodsCategories(param);
		return result;
	}

	// 관리자 상품을 수정합니다.
	@Transactional
	public int updateAdminGoods(GoodsSavePO param) {
		int result = goodsMapper.updateAdminGoods(param);
		saveAdminGoodsCategories(param);
		return result;
	}

	// 상품 등록 필수값을 검증합니다.
	public String validateGoodsSave(GoodsSavePO param) {
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}
		if (isBlank(param.getGoodsId())) {
			return "상품코드를 입력해주세요.";
		}
		if (param.getBrandNo() == null) {
			return "브랜드를 선택해주세요.";
		}
		if (isBlank(param.getGoodsDivCd())) {
			return "상품구분을 선택해주세요.";
		}
		if (isBlank(param.getGoodsStatCd())) {
			return "상품상태를 선택해주세요.";
		}
		if (isBlank(param.getGoodsNm())) {
			return "상품명을 입력해주세요.";
		}
		if (isBlank(param.getGoodsGroupId())) {
			return "상품그룹코드를 입력해주세요.";
		}
		if (isBlank(param.getGoodsMerchId())) {
			return "상품분류를 선택해주세요.";
		}
		if (param.getSupplyAmt() == null) {
			return "공급가를 입력해주세요.";
		}
		if (param.getSaleAmt() == null) {
			return "판매가를 입력해주세요.";
		}
		if (param.getErpSupplyAmt() == null) {
			return "ERP 공급가를 입력해주세요.";
		}
		if (param.getErpCostAmt() == null) {
			return "ERP 원가를 입력해주세요.";
		}
		if (isBlank(param.getErpStyleCd())) {
			return "ERP 품번코드를 입력해주세요.";
		}
		if (isBlank(param.getErpColorCd())) {
			return "ERP 컬러코드를 입력해주세요.";
		}
		if (isBlank(param.getErpMerchCd())) {
			return "ERP 상품구분코드를 입력해주세요.";
		}
		if (param.getRegNo() == null) {
			return "등록자 정보를 확인해주세요.";
		}
		return null;
	}

	// 상품 수정 필수값을 검증합니다.
	public String validateGoodsUpdate(GoodsSavePO param) {
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}
		if (isBlank(param.getGoodsId())) {
			return "상품코드를 확인해주세요.";
		}
		if (isBlank(param.getGoodsDivCd())) {
			return "상품구분을 선택해주세요.";
		}
		if (isBlank(param.getGoodsStatCd())) {
			return "상품상태를 선택해주세요.";
		}
		if (isBlank(param.getGoodsNm())) {
			return "상품명을 입력해주세요.";
		}
		if (isBlank(param.getGoodsGroupId())) {
			return "상품그룹코드를 입력해주세요.";
		}
		if (isBlank(param.getGoodsMerchId())) {
			return "상품분류를 선택해주세요.";
		}
		if (param.getSupplyAmt() == null) {
			return "공급가를 입력해주세요.";
		}
		if (param.getSaleAmt() == null) {
			return "판매가를 입력해주세요.";
		}
		if (param.getErpSupplyAmt() == null) {
			return "ERP 공급가를 입력해주세요.";
		}
		if (param.getErpCostAmt() == null) {
			return "ERP 원가를 입력해주세요.";
		}
		if (isBlank(param.getErpStyleCd())) {
			return "ERP 품번코드를 입력해주세요.";
		}
		if (isBlank(param.getErpColorCd())) {
			return "ERP 컬러코드를 입력해주세요.";
		}
		if (isBlank(param.getErpMerchCd())) {
			return "ERP 상품구분코드를 입력해주세요.";
		}
		if (param.getUdtNo() == null) {
			return "수정자 정보를 확인해주세요.";
		}
		return null;
	}

	// 상품명 FULLTEXT 검색용 키워드를 생성합니다.
	private String buildGoodsNameSearchKeyword(GoodsPO param) {
		if (param == null) {
			return null;
		}

		String searchGb = param.getSearchGb();
		String searchValue = param.getSearchValue();
		if (!"goodsNm".equals(searchGb) || searchValue == null) {
			return null;
		}

		String trimmed = searchValue.trim();
		if (trimmed.isEmpty()) {
			return null;
		}

		String[] tokens = trimmed.split("\\s+");
		StringBuilder builder = new StringBuilder();
		for (String token : tokens) {
			if (token == null || token.isEmpty()) {
				continue;
			}
			if (!builder.isEmpty()) {
				builder.append(' ');
			}
			builder.append('+').append(token).append('*');
		}
		return builder.isEmpty() ? null : builder.toString();
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

	// 관리자 상품 사이즈 목록을 조회합니다.
	public List<GoodsSizeVO> getAdminGoodsSizeList(String goodsId) {
		if (isBlank(goodsId)) {
			return List.of();
		}
		return goodsMapper.getAdminGoodsSizeList(goodsId);
	}

	// 관리자 상품 사이즈 단건을 조회합니다.
	public GoodsSizeVO getAdminGoodsSizeDetail(String goodsId, String sizeId) {
		if (isBlank(goodsId) || isBlank(sizeId)) {
			return null;
		}
		return goodsMapper.getAdminGoodsSizeDetail(goodsId, sizeId);
	}

	// 관리자 상품 사이즈 저장 요청을 검증합니다.
	public String validateGoodsSizeSave(GoodsSizeSavePO param, boolean isNew) {
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}
		if (isBlank(param.getGoodsId())) {
			return "상품코드를 확인해주세요.";
		}
		if (isBlank(param.getSizeId())) {
			return "사이즈코드를 입력해주세요.";
		}
		if (param.getStockQty() == null) {
			return "재고를 입력해주세요.";
		}
		if (param.getAddAmt() == null) {
			return "추가 금액을 입력해주세요.";
		}
		if (isBlank(param.getErpSyncYn())) {
			return "ERP 연동 여부를 선택해주세요.";
		}
		if (isNew && isBlank(param.getErpSizeCd())) {
			return "ERP 사이즈코드를 입력해주세요.";
		}
		if (isNew && param.getRegNo() == null) {
			return "등록자 정보를 확인해주세요.";
		}
		if (!isNew && param.getUdtNo() == null) {
			return "수정자 정보를 확인해주세요.";
		}
		return null;
	}

	// 관리자 상품 사이즈를 저장합니다.
	public int saveAdminGoodsSize(GoodsSizeSavePO param) {
		String lookupSizeId = isBlank(param.getOriginSizeId()) ? param.getSizeId() : param.getOriginSizeId();
		GoodsSizeVO current = goodsMapper.getAdminGoodsSizeDetail(param.getGoodsId(), lookupSizeId);
		boolean isNew = current == null;
		if (param.getDispOrd() == null) {
			param.setDispOrd(0);
		}
		if (isBlank(param.getDelYn())) {
			param.setDelYn("N");
		}
		if (!isNew && isBlank(param.getOriginSizeId())) {
			param.setOriginSizeId(lookupSizeId);
		}
		if (isNew && param.getUdtNo() == null) {
			param.setUdtNo(param.getRegNo());
		}
		if (!isNew && "Y".equalsIgnoreCase(param.getErpSyncYn())) {
			param.setStockQty(current.getStockQty());
		}
		return isNew ? goodsMapper.insertAdminGoodsSize(param) : goodsMapper.updateAdminGoodsSize(param);
	}

	// 카테고리 목록을 조회합니다.
	public List<CategoryVO> getCategoryList(Integer categoryLevel, String parentCategoryId) {
		return goodsMapper.getCategoryList(categoryLevel, parentCategoryId);
	}

	// 관리자 카테고리 트리 목록을 조회합니다.
	public List<CategoryVO> getAdminCategoryTreeList() {
		// 전체 카테고리 목록을 조회합니다.
		return goodsMapper.getAdminCategoryTreeList();
	}

	// 관리자 카테고리 상세를 조회합니다.
	public CategoryVO getAdminCategoryDetail(String categoryId) {
		// 카테고리 아이디가 없으면 조회하지 않습니다.
		if (isBlank(categoryId)) {
			return null;
		}
		// 카테고리 상세 정보를 조회합니다.
		return goodsMapper.getAdminCategoryDetail(categoryId);
	}

	// 관리자 카테고리 등록 요청을 검증합니다.
	public String validateAdminCategoryCreate(CategorySavePO param) {
		// 요청 데이터 유효성을 확인합니다.
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}
		if (isBlank(param.getCategoryId())) {
			return "카테고리 코드를 입력해주세요.";
		}
		if (isBlank(param.getCategoryNm())) {
			return "카테고리명을 입력해주세요.";
		}
		if (param.getRegNo() == null) {
			return "등록자 정보를 확인해주세요.";
		}
		int exists = goodsMapper.countAdminCategoryById(param.getCategoryId());
		if (exists > 0) {
			return "이미 등록된 카테고리 코드입니다.";
		}
		if (!isBlank(param.getParentCategoryId())) {
			CategoryVO parent = goodsMapper.getAdminCategoryDetail(param.getParentCategoryId());
			if (parent == null) {
				return "상위 카테고리를 확인해주세요.";
			}
			if (parent.getCategoryLevel() != null && parent.getCategoryLevel() >= 3) {
				return "3레벨 카테고리에는 하위 카테고리를 추가할 수 없습니다.";
			}
		}
		return null;
	}

	// 관리자 카테고리 다음 코드를 생성합니다.
	public String getNextAdminCategoryId(String parentCategoryId) {
		// 상위 카테고리 공백값을 정리합니다.
		String resolvedParentId = isBlank(parentCategoryId) ? "0" : parentCategoryId;
		// 최상위 카테고리 여부를 판단합니다.
		boolean isRootCategory = "0".equals(resolvedParentId);
		// 동일 상위 카테고리 기준 최대 코드를 조회합니다.
		String maxCategoryId = goodsMapper.getAdminCategoryMaxId(resolvedParentId);
		if (isBlank(maxCategoryId)) {
			// 최상위는 2자리, 하위는 4자리로 시작합니다.
			return isRootCategory ? "01" : resolvedParentId + "0001";
		}
		if (isRootCategory) {
			// 최상위 카테고리는 마지막 2자리 기준으로 증가합니다.
			String suffix = maxCategoryId.length() >= 2 ? maxCategoryId.substring(maxCategoryId.length() - 2) : maxCategoryId;
			int nextNumber;
			try {
				nextNumber = Integer.parseInt(suffix) + 1;
			} catch (NumberFormatException e) {
				nextNumber = 1;
			}
			return String.format("%02d", nextNumber);
		}
		// 하위 카테고리는 4자리 단위로 증가합니다.
		String prefix = maxCategoryId.length() > 4 ? maxCategoryId.substring(0, maxCategoryId.length() - 4) : "";
		String suffix = maxCategoryId.length() >= 4 ? maxCategoryId.substring(maxCategoryId.length() - 4) : maxCategoryId;
		int nextNumber;
		try {
			nextNumber = Integer.parseInt(suffix) + 1;
		} catch (NumberFormatException e) {
			nextNumber = 1;
		}
		String nextSuffix = String.format("%04d", nextNumber);
		return prefix + nextSuffix;
	}

	// 관리자 카테고리 수정 요청을 검증합니다.
	public String validateAdminCategoryUpdate(CategorySavePO param) {
		// 요청 데이터 유효성을 확인합니다.
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}
		if (isBlank(param.getCategoryId())) {
			return "카테고리 코드를 확인해주세요.";
		}
		if (isBlank(param.getCategoryNm())) {
			return "카테고리명을 입력해주세요.";
		}
		if (param.getUdtNo() == null) {
			return "수정자 정보를 확인해주세요.";
		}
		CategoryVO current = goodsMapper.getAdminCategoryDetail(param.getCategoryId());
		if (current == null) {
			return "카테고리 정보를 확인해주세요.";
		}
		return null;
	}

	// 관리자 카테고리 삭제 요청을 검증합니다.
	public String validateAdminCategoryDelete(CategorySavePO param) {
		// 요청 데이터 유효성을 확인합니다.
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}
		if (isBlank(param.getCategoryId())) {
			return "카테고리 코드를 확인해주세요.";
		}
		if (param.getUdtNo() == null) {
			return "수정자 정보를 확인해주세요.";
		}
		int childCount = goodsMapper.countAdminCategoryChildren(param.getCategoryId());
		if (childCount > 0) {
			return "하위 카테고리가 존재하여 삭제할 수 없습니다.";
		}
		int goodsCount = goodsMapper.countAdminCategoryGoods(param.getCategoryId());
		if (goodsCount > 0) {
			return "상품에 연결된 카테고리는 삭제할 수 없습니다.";
		}
		return null;
	}

	// 관리자 카테고리를 등록합니다.
	public int createAdminCategory(CategorySavePO param) {
		// 상위 카테고리 공백값을 정리합니다.
		if (isBlank(param.getParentCategoryId())) {
			param.setParentCategoryId("0");
		}
		// 상위 카테고리에 따라 레벨을 계산합니다.
		int categoryLevel = 1;
		if (!isBlank(param.getParentCategoryId())) {
			CategoryVO parent = goodsMapper.getAdminCategoryDetail(param.getParentCategoryId());
			categoryLevel = parent != null && parent.getCategoryLevel() != null ? parent.getCategoryLevel() + 1 : 1;
		}
		param.setCategoryLevel(categoryLevel);
		// 정렬 순서를 설정합니다.
		if (param.getDispOrd() == null) {
			Integer maxDispOrd = goodsMapper.getAdminCategoryMaxDispOrd(param.getParentCategoryId());
			param.setDispOrd(maxDispOrd != null ? maxDispOrd + 1 : 1);
		}
		// 노출 여부 기본값을 설정합니다.
		if (isBlank(param.getShowYn())) {
			param.setShowYn("Y");
		}
		// 수정자를 등록자로 설정합니다.
		if (param.getUdtNo() == null) {
			param.setUdtNo(param.getRegNo());
		}
		return goodsMapper.insertAdminCategory(param);
	}

	// 관리자 카테고리를 수정합니다.
	public int updateAdminCategory(CategorySavePO param) {
		// 정렬 순서 기본값을 유지합니다.
		if (param.getDispOrd() == null) {
			CategoryVO current = goodsMapper.getAdminCategoryDetail(param.getCategoryId());
			param.setDispOrd(current != null ? current.getDispOrd() : 0);
		}
		// 노출 여부 기본값을 설정합니다.
		if (isBlank(param.getShowYn())) {
			param.setShowYn("Y");
		}
		return goodsMapper.updateAdminCategory(param);
	}

	// 관리자 카테고리를 삭제 처리합니다.
	public int deleteAdminCategory(CategorySavePO param) {
		// 삭제 처리를 수행합니다.
		return goodsMapper.deleteAdminCategory(param);
	}

	// 카테고리별 상품 목록을 조회합니다.
	public List<CategoryGoodsVO> getAdminCategoryGoodsList(String categoryId) {
		// 카테고리 코드가 없으면 빈 목록을 반환합니다.
		if (isBlank(categoryId)) {
			return List.of();
		}
		// 카테고리별 상품 목록을 조회합니다.
		List<CategoryGoodsVO> list = goodsMapper.getAdminCategoryGoodsList(categoryId);
		// 상품 이미지 URL을 세팅합니다.
		goodsImageService.applyCategoryGoodsImageUrls(list);
		return list;
	}

	// 쇼핑몰 카테고리 화면의 선택 카테고리 전체 상품 건수를 조회합니다.
	public int countShopCategoryGoods(String categoryId) {
		// 카테고리 코드가 없으면 0건을 반환합니다.
		if (isBlank(categoryId)) {
			return 0;
		}
		// 선택 카테고리 상품 건수를 조회합니다.
		return goodsMapper.countShopCategoryGoods(categoryId);
	}

	// 쇼핑몰 카테고리 화면의 선택 카테고리 상품 목록을 페이징 조회합니다.
	public List<ShopCategoryGoodsItemVO> getShopCategoryGoodsList(String categoryId, int offset, int pageSize) {
		// 카테고리 코드가 없으면 빈 목록을 반환합니다.
		if (isBlank(categoryId)) {
			return List.of();
		}
		// 오프셋과 페이지 크기를 안전한 범위로 보정합니다.
		int resolvedOffset = Math.max(offset, 0);
		int resolvedPageSize = pageSize <= 0 ? 20 : pageSize;
		// 선택 카테고리 상품 목록을 조회합니다.
		List<ShopCategoryGoodsItemVO> list = goodsMapper.getShopCategoryGoodsList(categoryId, resolvedOffset, resolvedPageSize);
		// 상품 이미지 URL을 세팅합니다.
		goodsImageService.applyShopCategoryGoodsImageUrls(list);
		return list;
	}

	// 쇼핑몰 마이페이지 위시리스트 페이지 데이터를 조회합니다.
	public ShopMypageWishPageVO getShopMypageWishPage(Long custNo, Integer requestedPageNo) {
		// 고객번호가 없으면 로그인 예외를 반환합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}

		// 요청 페이지 번호를 1 이상으로 보정합니다.
		int resolvedRequestedPageNo = normalizePage(requestedPageNo, 1);
		// 위시리스트 전체 건수를 조회합니다.
		int goodsCount = goodsMapper.countShopMypageWishGoods(custNo);
		// 전체 페이지 수를 계산합니다.
		int totalPageCount = calculateTotalPageCount(goodsCount, SHOP_MYPAGE_WISH_PAGE_SIZE);
		// 범위를 초과한 페이지 번호를 마지막 페이지로 보정합니다.
		int resolvedPageNo = resolvePageNoWithinRange(resolvedRequestedPageNo, totalPageCount);
		// 페이지 조회 오프셋을 계산합니다.
		int offset = calculateOffset(resolvedPageNo, SHOP_MYPAGE_WISH_PAGE_SIZE);
		// 위시리스트 상품 목록을 조회합니다.
		List<ShopMypageWishGoodsItemVO> goodsList = goodsMapper.getShopMypageWishGoodsList(custNo, offset, SHOP_MYPAGE_WISH_PAGE_SIZE);
		// 상품 이미지 URL을 세팅합니다.
		goodsImageService.applyShopMypageWishGoodsImageUrls(goodsList);

		// 위시리스트 페이지 응답 객체를 구성합니다.
		ShopMypageWishPageVO result = new ShopMypageWishPageVO();
		result.setGoodsList(goodsList == null ? List.of() : goodsList);
		result.setGoodsCount(goodsCount);
		result.setPageNo(resolvedPageNo);
		result.setPageSize(SHOP_MYPAGE_WISH_PAGE_SIZE);
		result.setTotalPageCount(totalPageCount);
		return result;
	}

	// 쇼핑몰 마이페이지 위시리스트 상품을 삭제합니다.
	public void deleteShopMypageWishGoods(String goodsId, Long custNo) {
		// 필수 입력값(goodsId/custNo) 유효성을 확인합니다.
		if (isBlank(goodsId)) {
			throw new IllegalArgumentException("상품코드를 확인해주세요.");
		}
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}

		// 위시리스트에서 고객/상품 기준으로 삭제합니다.
		goodsMapper.deleteShopWishList(custNo, goodsId.trim());
	}

	// 쇼핑몰 마이페이지 쿠폰함 페이지 데이터를 조회합니다.
	public ShopMypageCouponPageVO getShopMypageCouponPage(Long custNo, Integer requestedOwnedPageNo, Integer requestedDownloadablePageNo) {
		// 고객번호가 없으면 로그인 예외를 반환합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}

		// 보유 쿠폰/다운로드 쿠폰 요청 페이지 번호를 각각 보정합니다.
		int resolvedRequestedOwnedPageNo = normalizePage(requestedOwnedPageNo, 1);
		int resolvedRequestedDownloadablePageNo = normalizePage(requestedDownloadablePageNo, 1);

		// 보유 쿠폰/다운로드 쿠폰 전체 건수와 전체 페이지 수를 계산합니다.
		int ownedCouponCount = goodsMapper.countShopMypageOwnedCoupon(custNo);
		int downloadableCouponCount = goodsMapper.countShopMypageDownloadableCoupon();
		int ownedTotalPageCount = calculateTotalPageCount(ownedCouponCount, SHOP_MYPAGE_COUPON_PAGE_SIZE);
		int downloadableTotalPageCount = calculateTotalPageCount(downloadableCouponCount, SHOP_MYPAGE_COUPON_PAGE_SIZE);
		int resolvedOwnedPageNo = resolvePageNoWithinRange(resolvedRequestedOwnedPageNo, ownedTotalPageCount);
		int resolvedDownloadablePageNo = resolvePageNoWithinRange(resolvedRequestedDownloadablePageNo, downloadableTotalPageCount);

		// 현재 페이지의 보유 쿠폰/다운로드 쿠폰 목록을 각각 조회합니다.
		int ownedOffset = calculateOffset(resolvedOwnedPageNo, SHOP_MYPAGE_COUPON_PAGE_SIZE);
		int downloadableOffset = calculateOffset(resolvedDownloadablePageNo, SHOP_MYPAGE_COUPON_PAGE_SIZE);
		List<ShopMypageOwnedCouponVO> ownedCouponList =
			goodsMapper.getShopMypageOwnedCouponPageList(custNo, ownedOffset, SHOP_MYPAGE_COUPON_PAGE_SIZE);
		List<ShopMypageDownloadableCouponVO> downloadableCouponList =
			goodsMapper.getShopMypageDownloadableCouponPageList(downloadableOffset, SHOP_MYPAGE_COUPON_PAGE_SIZE);

		// 쿠폰 번호 기준 사용 불가 상품 요약을 캐시하여 카드별 부가정보를 채웁니다.
		Map<Long, ShopMypageCouponUnavailableGoodsSummary> unavailableGoodsSummaryMap = new HashMap<>();
		applyShopMypageCouponUnavailableGoodsToOwnedCouponList(ownedCouponList, unavailableGoodsSummaryMap);
		applyShopMypageCouponUnavailableGoodsToDownloadableCouponList(downloadableCouponList, unavailableGoodsSummaryMap);

		// 쿠폰 페이지 응답 객체를 구성합니다.
		ShopMypageCouponPageVO result = new ShopMypageCouponPageVO();
		result.setOwnedCouponList(ownedCouponList == null ? List.of() : ownedCouponList);
		result.setOwnedCouponCount(ownedCouponCount);
		result.setOwnedPageNo(resolvedOwnedPageNo);
		result.setOwnedPageSize(SHOP_MYPAGE_COUPON_PAGE_SIZE);
		result.setOwnedTotalPageCount(ownedTotalPageCount);
		result.setDownloadableCouponList(downloadableCouponList == null ? List.of() : downloadableCouponList);
		result.setDownloadableCouponCount(downloadableCouponCount);
		result.setDownloadablePageNo(resolvedDownloadablePageNo);
		result.setDownloadablePageSize(SHOP_MYPAGE_COUPON_PAGE_SIZE);
		result.setDownloadableTotalPageCount(downloadableTotalPageCount);
		return result;
	}

	// 쇼핑몰 마이페이지에서 쿠폰 1건을 다운로드합니다.
	@Transactional
	public void downloadShopMypageCoupon(ShopMypageCouponDownloadRequestPO param, Long custNo) {
		// 로그인 고객번호와 다운로드 쿠폰번호를 검증합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}
		Long cpnNo = param == null ? null : param.getCpnNo();
		if (cpnNo == null || cpnNo < 1L) {
			throw new IllegalArgumentException("쿠폰번호를 확인해주세요.");
		}

		// 현재 다운로드 가능한 쿠폰인지 확인합니다.
		ShopMypageDownloadableCouponVO downloadableCoupon = findShopMypageDownloadableCoupon(cpnNo);
		if (downloadableCoupon == null) {
			throw new IllegalArgumentException("다운로드 가능한 쿠폰을 확인해주세요.");
		}

		// 쿠폰 1건을 고객에게 발급합니다.
		int issuedCount = shopCustomerCouponService.issueShopCustomerCoupon(custNo, cpnNo, 1);
		if (issuedCount < 1) {
			throw new IllegalArgumentException("쿠폰 다운로드에 실패했습니다.");
		}
	}

	// 쇼핑몰 마이페이지에서 현재 다운로드 가능한 쿠폰을 전체 다운로드합니다.
	@Transactional
	public int downloadAllShopMypageCoupon(Long custNo) {
		// 로그인 고객번호를 검증합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}

		// 현재 다운로드 가능한 쿠폰 목록을 조회합니다.
		List<ShopMypageDownloadableCouponVO> downloadableCouponList = goodsMapper.getShopMypageDownloadableCouponList();
		if (downloadableCouponList == null || downloadableCouponList.isEmpty()) {
			return 0;
		}

		// 쿠폰별로 1장씩만 전체 다운로드를 수행합니다.
		int downloadedCount = 0;
		Set<Long> issuedCouponNoSet = new HashSet<>();
		for (ShopMypageDownloadableCouponVO downloadableCoupon : downloadableCouponList) {
			if (downloadableCoupon == null || downloadableCoupon.getCpnNo() == null) {
				continue;
			}
			if (!issuedCouponNoSet.add(downloadableCoupon.getCpnNo())) {
				continue;
			}

			// 개별 쿠폰 발급 실패 시 전체 다운로드를 실패 처리합니다.
			int issuedCount = shopCustomerCouponService.issueShopCustomerCoupon(custNo, downloadableCoupon.getCpnNo(), 1);
			if (issuedCount < 1) {
				throw new IllegalArgumentException("쿠폰 다운로드에 실패했습니다.");
			}
			downloadedCount += issuedCount;
		}
		return downloadedCount;
	}

	// 보유 쿠폰 목록에 쿠폰 사용 불가 상품 요약 정보를 채웁니다.
	private void applyShopMypageCouponUnavailableGoodsToOwnedCouponList(
		List<ShopMypageOwnedCouponVO> ownedCouponList,
		Map<Long, ShopMypageCouponUnavailableGoodsSummary> unavailableGoodsSummaryMap
	) {
		// 보유 쿠폰 목록이 없으면 처리하지 않습니다.
		if (ownedCouponList == null || ownedCouponList.isEmpty()) {
			return;
		}

		// 동일 쿠폰번호는 캐시를 재사용하여 사용 불가 상품 요약을 채웁니다.
		for (ShopMypageOwnedCouponVO ownedCoupon : ownedCouponList) {
			if (ownedCoupon == null) {
				continue;
			}
			ShopMypageCouponUnavailableGoodsSummary unavailableGoodsSummary = resolveShopMypageCouponUnavailableGoodsSummary(
				unavailableGoodsSummaryMap,
				ownedCoupon.getCpnNo(),
				ownedCoupon.getCpnTargetCd()
			);
			ownedCoupon.setUnavailableGoodsCount(unavailableGoodsSummary.getUnavailableGoodsCount());
			ownedCoupon.setUnavailableGoodsList(unavailableGoodsSummary.getUnavailableGoodsList());
		}
	}

	// 다운로드 가능 쿠폰 목록에 쿠폰 사용 불가 상품 요약 정보를 채웁니다.
	private void applyShopMypageCouponUnavailableGoodsToDownloadableCouponList(
		List<ShopMypageDownloadableCouponVO> downloadableCouponList,
		Map<Long, ShopMypageCouponUnavailableGoodsSummary> unavailableGoodsSummaryMap
	) {
		// 다운로드 가능 쿠폰 목록이 없으면 처리하지 않습니다.
		if (downloadableCouponList == null || downloadableCouponList.isEmpty()) {
			return;
		}

		// 동일 쿠폰번호는 캐시를 재사용하여 사용 불가 상품 요약을 채웁니다.
		for (ShopMypageDownloadableCouponVO downloadableCoupon : downloadableCouponList) {
			if (downloadableCoupon == null) {
				continue;
			}
			ShopMypageCouponUnavailableGoodsSummary unavailableGoodsSummary = resolveShopMypageCouponUnavailableGoodsSummary(
				unavailableGoodsSummaryMap,
				downloadableCoupon.getCpnNo(),
				downloadableCoupon.getCpnTargetCd()
			);
			downloadableCoupon.setUnavailableGoodsCount(unavailableGoodsSummary.getUnavailableGoodsCount());
			downloadableCoupon.setUnavailableGoodsList(unavailableGoodsSummary.getUnavailableGoodsList());
		}
	}

	// 쿠폰번호 기준 사용 불가 상품 요약 캐시를 조회하거나 새로 계산합니다.
	private ShopMypageCouponUnavailableGoodsSummary resolveShopMypageCouponUnavailableGoodsSummary(
		Map<Long, ShopMypageCouponUnavailableGoodsSummary> unavailableGoodsSummaryMap,
		Long cpnNo,
		String cpnTargetCd
	) {
		// 쿠폰번호가 없으면 빈 요약을 반환합니다.
		if (cpnNo == null || cpnNo < 1L) {
			return ShopMypageCouponUnavailableGoodsSummary.empty();
		}
		if (unavailableGoodsSummaryMap != null && unavailableGoodsSummaryMap.containsKey(cpnNo)) {
			return unavailableGoodsSummaryMap.get(cpnNo);
		}

		// 쿠폰 타겟 목록과 제외 대상 값을 기준으로 사용 불가 상품 요약을 계산합니다.
		List<ShopGoodsCouponTargetVO> targetList = goodsMapper.getShopCouponTargetList(cpnNo);
		List<String> excludeTargetValueList = extractCouponTargetValueList(targetList, TARGET_GB_EXCLUDE);
		ShopMypageCouponUnavailableGoodsSummary unavailableGoodsSummary =
			buildShopMypageCouponUnavailableGoodsSummary(cpnTargetCd, excludeTargetValueList);
		if (unavailableGoodsSummaryMap != null) {
			unavailableGoodsSummaryMap.put(cpnNo, unavailableGoodsSummary);
		}
		return unavailableGoodsSummary;
	}

	// 쿠폰 대상 코드와 제외 대상값 목록을 기준으로 사용 불가 상품 요약을 계산합니다.
	private ShopMypageCouponUnavailableGoodsSummary buildShopMypageCouponUnavailableGoodsSummary(
		String cpnTargetCd,
		List<String> excludeTargetValueList
	) {
		// 제외 대상이 없거나 해석할 수 없는 쿠폰 타겟이면 빈 요약을 반환합니다.
		if (excludeTargetValueList == null || excludeTargetValueList.isEmpty() || isBlank(cpnTargetCd)) {
			return ShopMypageCouponUnavailableGoodsSummary.empty();
		}

		// 현재 쿠폰 타겟 유형에 맞는 사용 불가 상품 목록과 건수를 조회합니다.
		if (!CPN_TARGET_GOODS.equals(cpnTargetCd)
			&& !CPN_TARGET_ALL.equals(cpnTargetCd)
			&& !CPN_TARGET_BRAND.equals(cpnTargetCd)
			&& !CPN_TARGET_CATEGORY.equals(cpnTargetCd)
			&& !CPN_TARGET_EXHIBITION.equals(cpnTargetCd)) {
			return ShopMypageCouponUnavailableGoodsSummary.empty();
		}
		int unavailableGoodsCount = goodsMapper.countShopMypageCouponUnavailableGoods(cpnTargetCd, excludeTargetValueList);
		List<ShopMypageCouponUnavailableGoodsVO> unavailableGoodsList = goodsMapper.getShopMypageCouponUnavailableGoodsList(
			cpnTargetCd,
			excludeTargetValueList,
			SHOP_MYPAGE_COUPON_TOOLTIP_LIMIT
		);
		return new ShopMypageCouponUnavailableGoodsSummary(
			unavailableGoodsCount,
			unavailableGoodsList == null ? List.of() : unavailableGoodsList
		);
	}

	// 쿠폰 타겟 목록에서 지정 대상 구분의 대상값 목록을 중복 없이 추출합니다.
	private List<String> extractCouponTargetValueList(List<ShopGoodsCouponTargetVO> targetList, String targetGbCd) {
		// 타겟 목록 또는 요청 대상 구분이 없으면 빈 목록을 반환합니다.
		if (targetList == null || targetList.isEmpty() || isBlank(targetGbCd)) {
			return List.of();
		}

		// 대상 구분이 일치하는 대상값만 중복 없이 순서대로 수집합니다.
		Set<String> targetValueSet = new LinkedHashSet<>();
		for (ShopGoodsCouponTargetVO target : targetList) {
			if (target == null || !targetGbCd.equals(target.getTargetGbCd()) || isBlank(target.getTargetValue())) {
				continue;
			}
			targetValueSet.add(target.getTargetValue().trim());
		}
		return targetValueSet.isEmpty() ? List.of() : new ArrayList<>(targetValueSet);
	}

	// 주문번호 1건을 조회하고 주문상세 목록과 이미지 URL을 연결합니다.
	// 현재 다운로드 가능한 쿠폰 목록에서 지정 쿠폰번호 1건을 조회합니다.
	private ShopMypageDownloadableCouponVO findShopMypageDownloadableCoupon(Long cpnNo) {
		// 쿠폰번호가 없으면 조회하지 않습니다.
		if (cpnNo == null || cpnNo < 1L) {
			return null;
		}

		// 현재 다운로드 가능 목록에서 동일 쿠폰번호를 찾습니다.
		List<ShopMypageDownloadableCouponVO> downloadableCouponList = goodsMapper.getShopMypageDownloadableCouponList();
		for (ShopMypageDownloadableCouponVO downloadableCoupon : downloadableCouponList == null ? List.<ShopMypageDownloadableCouponVO>of() : downloadableCouponList) {
			if (downloadableCoupon.getCpnNo() == null) {
				continue;
			}
			if (cpnNo.equals(downloadableCoupon.getCpnNo())) {
				return downloadableCoupon;
			}
		}
		return null;
	}

	// 쇼핑몰 상품상세 상단 렌더링에 필요한 데이터를 조회합니다.
	public ShopGoodsDetailVO getShopGoodsDetail(String goodsId, Long custNo, String custGradeCd) {
		// 상품 코드가 없으면 조회하지 않습니다.
		if (isBlank(goodsId)) {
			return null;
		}

		// 판매중/노출중 상품 기본 정보를 조회합니다.
		ShopGoodsBasicVO goods = goodsMapper.getShopGoodsBasic(goodsId.trim());
		if (goods == null) {
			return null;
		}

		// 상품 이미지/사이즈/그룹상품/상세설명 데이터를 조회합니다.
		List<ShopGoodsImageVO> imageList = goodsMapper.getShopGoodsImageList(goods.getGoodsId());
		List<ShopGoodsSizeItemVO> sizeList = goodsMapper.getShopGoodsSizeList(goods.getGoodsId());
		List<ShopGoodsGroupItemVO> groupGoodsList = isBlank(goods.getGoodsGroupId())
			? List.of()
			: goodsMapper.getShopGoodsGroupItemList(goods.getGoodsGroupId());
		List<ShopGoodsDescItemVO> goodsDescItemList = goodsMapper.getShopGoodsDescItemList(goods.getGoodsId());

		// 화면에서 바로 사용할 수 있도록 URL/품절 상태를 보정합니다.
		goodsImageService.applyShopGoodsImageUrlList(imageList);
		goodsImageService.applyShopGoodsGroupItemImageUrlList(groupGoodsList);
		applyShopGoodsSizeSoldOutState(sizeList);

		// 위시리스트/사이트정보/가격/포인트/배송비/상품쿠폰 정보를 계산합니다.
		ShopGoodsWishlistVO wishlist = buildShopGoodsWishlist(custNo, goods.getGoodsId());
		ShopGoodsSiteInfoVO siteInfo = resolveShopGoodsSiteInfo();
		ShopGoodsPriceSummaryVO priceSummary = buildShopGoodsPriceSummary(goods);
		ShopGoodsPointSummaryVO pointSummary = buildShopGoodsPointSummary(goods, custGradeCd);
		ShopGoodsShippingSummaryVO shippingSummary = buildShopGoodsShippingSummary(goods, siteInfo);
		List<ShopGoodsCouponVO> couponList = getAvailableShopGoodsCouponList(goods.getGoodsId(), goods.getBrandNo());

		// 응답 객체를 구성해 반환합니다.
		ShopGoodsDetailVO result = new ShopGoodsDetailVO();
		result.setGoods(goods);
		result.setImages(imageList == null ? List.of() : imageList);
		result.setGroupGoods(groupGoodsList == null ? List.of() : groupGoodsList);
		result.setSizes(sizeList == null ? List.of() : sizeList);
		result.setDetailDesc(buildShopGoodsDesc(goodsDescItemList));
		result.setWishlist(wishlist);
		result.setSiteInfo(siteInfo);
		result.setCoupons(couponList);
		result.setPriceSummary(priceSummary);
		result.setPointSummary(pointSummary);
		result.setShippingSummary(shippingSummary);
		return result;
	}

	// 쇼핑몰 상품상세에서 현재 상품에 다운로드 가능한 상품쿠폰 1건을 발급합니다.
	@Transactional
	public void downloadShopGoodsCoupon(String goodsId, Long cpnNo, Long custNo) {
		// 로그인 고객번호와 필수 요청값을 검증합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}
		if (isBlank(goodsId)) {
			throw new IllegalArgumentException("상품코드를 확인해주세요.");
		}
		if (cpnNo == null || cpnNo < 1L) {
			throw new IllegalArgumentException("쿠폰번호를 확인해주세요.");
		}

		// 현재 조회 가능한 상품인지 먼저 확인합니다.
		String normalizedGoodsId = goodsId.trim();
		ShopGoodsBasicVO goods = goodsMapper.getShopGoodsBasic(normalizedGoodsId);
		if (goods == null) {
			throw new IllegalArgumentException("상품 정보를 찾을 수 없습니다.");
		}

		// 상품상세에 실제 노출 가능한 상품쿠폰인지 동일 기준으로 다시 검증합니다.
		ShopGoodsCouponVO downloadableCoupon = findAvailableShopGoodsCoupon(normalizedGoodsId, goods.getBrandNo(), cpnNo);
		if (downloadableCoupon == null) {
			throw new IllegalArgumentException("다운로드 가능한 상품쿠폰을 확인해주세요.");
		}

		// 검증된 상품쿠폰 1건을 고객에게 발급합니다.
		int issuedCount = shopCustomerCouponService.issueShopCustomerCoupon(custNo, cpnNo, 1);
		if (issuedCount < 1) {
			throw new IllegalArgumentException("쿠폰 다운로드에 실패했습니다.");
		}
	}

	// 쇼핑몰 상품 위시리스트를 토글(등록/삭제)하고 최종 상태를 반환합니다.
	public boolean toggleShopGoodsWishlist(String goodsId, Long custNo) {
		// 필수 입력값(goodsId/custNo) 유효성을 확인합니다.
		if (isBlank(goodsId)) {
			throw new IllegalArgumentException("상품코드를 확인해주세요.");
		}
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}

		// 현재 조회 가능한 상품인지 확인합니다.
		String normalizedGoodsId = goodsId.trim();
		ShopGoodsBasicVO goods = goodsMapper.getShopGoodsBasic(normalizedGoodsId);
		if (goods == null) {
			throw new IllegalArgumentException("상품 정보를 찾을 수 없습니다.");
		}

		// 기존 등록 여부에 따라 삭제 또는 등록 처리합니다.
		int existedCount = goodsMapper.countShopWishList(custNo, normalizedGoodsId);
		if (existedCount > 0) {
			goodsMapper.deleteShopWishList(custNo, normalizedGoodsId);
			return false;
		}

		// 위시리스트 신규 등록 시 등록자는 고객번호로 기록합니다.
		goodsMapper.insertShopWishList(custNo, normalizedGoodsId, custNo);
		return true;
	}

	// 쇼핑몰 상품 장바구니를 등록(기존 건은 수량 가산)하고 최종 수량을 반환합니다.
	@Transactional
	int addShopGoodsCart(String goodsId, String sizeId, Integer qty, Long custNo, Integer exhibitionNo) {
		// 장바구니 등록 대상 상품/사이즈/수량을 검증하고 정규화합니다.
		ShopCartValidatedInput validatedInput = validateShopCartInput(goodsId, sizeId, qty, custNo);
		Integer validatedExhibitionNo = resolveValidatedShopCartExhibitionNo(validatedInput.getGoodsId(), exhibitionNo);

		// 기존 장바구니(C) 존재 여부에 따라 수량 가산 또는 신규 등록을 수행합니다.
		int existedCount = cartMapper.countShopCartByGoodsIdAndSizeId(custNo, validatedInput.getGoodsId(), validatedInput.getSizeId());
		if (existedCount > 0) {
			cartMapper.addShopCartQtyByGoodsIdAndSizeId(
				custNo,
				validatedInput.getGoodsId(),
				validatedInput.getSizeId(),
				validatedInput.getQty(),
				validatedExhibitionNo,
				custNo
			);
		} else {
			cartMapper.insertShopCart(createShopCartSavePO(SHOP_CART_GB_CART, custNo, validatedInput, validatedExhibitionNo));
		}

		// 저장 이후 장바구니 최종 수량을 조회해 반환합니다.
		Integer latestQty = cartMapper.getShopCartQtyByGoodsIdAndSizeId(custNo, validatedInput.getGoodsId(), validatedInput.getSizeId());
		return latestQty == null ? validatedInput.getQty() : latestQty;
	}

	// 쇼핑몰 바로구매용 장바구니를 신규 등록하고 생성된 장바구니 번호를 반환합니다.
	@Transactional
	Long addShopGoodsOrderNowCart(String goodsId, String sizeId, Integer qty, Long custNo, Integer exhibitionNo) {
		// 바로구매 등록 대상 상품/사이즈/수량을 검증하고 정규화합니다.
		ShopCartValidatedInput validatedInput = validateShopCartInput(goodsId, sizeId, qty, custNo);
		Integer validatedExhibitionNo = resolveValidatedShopCartExhibitionNo(validatedInput.getGoodsId(), exhibitionNo);
		ShopCartSavePO savePO = createShopCartSavePO(SHOP_CART_GB_ORDER, custNo, validatedInput, validatedExhibitionNo);

		// 바로구매(O) 행은 항상 신규 등록합니다.
		cartMapper.insertShopCart(savePO);
		if (savePO.getCartId() == null || savePO.getCartId() < 1L) {
			throw new IllegalStateException("바로구매 장바구니 등록에 실패했습니다.");
		}
		return savePO.getCartId();
	}

	// 쇼핑몰 장바구니 페이지 데이터를 조회합니다.
	ShopCartPageVO getShopCartPage(Long custNo) {
		// 로그인 고객번호 유효성을 확인합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}

		// 장바구니(C) 목록을 조회해 페이지 응답으로 구성합니다.
		return buildShopCartPage(cartMapper.getShopCartItemList(custNo));
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
				if (!isMatchedShopCartGoodsCoupon(coupon, targetList, estimateRow)) {
					continue;
				}
				int discountAmt = calculateCouponDiscountAmount(coupon, estimateRow.getRowSaleAmt());
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
			result += estimateRow.getRowSaleAmt();
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
	private boolean isMatchedShopCartGoodsCoupon(
		ShopCartCustomerCouponVO coupon,
		List<ShopGoodsCouponTargetVO> targetList,
		ShopCartCouponEstimateRow estimateRow
	) {
		// 비교할 쿠폰 또는 행 정보가 없으면 미적용 처리합니다.
		if (coupon == null || estimateRow == null || isBlank(estimateRow.getGoodsId())) {
			return false;
		}
		return isMatchedShopCouponTarget(
			coupon.getCpnTargetCd(),
			targetList,
			estimateRow.getGoodsId(),
			estimateRow.getBrandNoValue(),
			estimateRow.getCategoryIdSet(),
			estimateRow.getExhibitionTabNoSet()
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
	private int normalizeNonNegativeNumber(Integer value) {
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

	// 쇼핑몰 장바구니 상품 옵션(사이즈/수량)을 변경합니다.
	@Transactional
	void updateShopCartOption(ShopCartOptionUpdatePO param, Long custNo) {
		// 요청 데이터와 필수 입력값을 검증합니다.
		if (param == null) {
			throw new IllegalArgumentException("요청 데이터를 확인해주세요.");
		}
		if (isBlank(param.getTargetSizeId())) {
			throw new IllegalArgumentException("변경할 사이즈를 선택해주세요.");
		}
		if (param.getQty() == null || param.getQty() < 1) {
			throw new IllegalArgumentException("수량을 확인해주세요.");
		}
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}

		// 변경 대상 장바구니 존재 여부를 확인합니다.
		ShopCartItemVO currentCartItem = resolveShopCartItemForOptionUpdate(param, custNo);
		String normalizedGoodsId = currentCartItem.getGoodsId().trim();
		String normalizedSizeId = currentCartItem.getSizeId().trim();
		String normalizedTargetSizeId = param.getTargetSizeId().trim();
		int resolvedQty = param.getQty();
		Long currentCartId = currentCartItem.getCartId();

		// 변경할 사이즈의 유효성/품절 상태를 확인합니다.
		List<ShopGoodsSizeItemVO> sizeList = goodsMapper.getShopGoodsSizeList(normalizedGoodsId);
		ShopGoodsSizeItemVO targetSize = findShopGoodsSizeBySizeId(sizeList, normalizedTargetSizeId);
		if (targetSize == null) {
			throw new IllegalArgumentException("사이즈를 확인해주세요.");
		}
		int stockQty = targetSize.getStockQty() == null ? 0 : targetSize.getStockQty();
		if (stockQty < 1) {
			throw new IllegalArgumentException("품절된 사이즈입니다.");
		}

		// 동일 사이즈 변경이면 수량만 수정합니다.
		if (normalizedSizeId.equals(normalizedTargetSizeId)) {
			int updatedCount = cartMapper.updateShopCartQtyByCartId(custNo, currentCartId, resolvedQty, custNo);
			if (updatedCount < 1) {
				throw new IllegalArgumentException("변경할 장바구니 상품을 찾을 수 없습니다.");
			}
			return;
		}

		// 목표 사이즈가 이미 있으면 수량을 병합하고 현재 행은 삭제합니다.
		Long targetCartId = cartMapper.getShopCartIdByGoodsIdAndSizeId(custNo, normalizedGoodsId, normalizedTargetSizeId);
		if (targetCartId != null && targetCartId > 0L && !targetCartId.equals(currentCartId)) {
			Integer existedTargetQty = cartMapper.getShopCartQtyByGoodsIdAndSizeId(custNo, normalizedGoodsId, normalizedTargetSizeId);
			int mergedQty = Math.max(existedTargetQty == null ? 0 : existedTargetQty, 0) + resolvedQty;
			cartMapper.updateShopCartQtyByCartId(custNo, targetCartId, mergedQty, custNo);
			cartMapper.deleteShopCartByCartId(custNo, currentCartId);
			return;
		}

		// 목표 사이즈가 없으면 현재 행의 사이즈/수량을 직접 변경합니다.
		int updatedCount = cartMapper.updateShopCartOptionByCartId(
			custNo,
			currentCartId,
			normalizedTargetSizeId,
			resolvedQty,
			custNo
		);
		if (updatedCount < 1) {
			throw new IllegalArgumentException("변경할 장바구니 상품을 찾을 수 없습니다.");
		}
	}

	// 쇼핑몰 장바구니 선택 상품을 삭제합니다.
	@Transactional
	int deleteShopCartItems(ShopCartDeletePO param, Long custNo) {
		// 로그인 고객번호와 삭제 요청 목록 유효성을 확인합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}
		if (param == null || param.getCartItemList() == null || param.getCartItemList().isEmpty()) {
			throw new IllegalArgumentException("삭제할 상품을 선택해주세요.");
		}

		// 중복 선택된 키를 제거하며 장바구니 항목을 삭제합니다.
		int deletedCount = 0;
		Set<Long> deletedCartIdSet = new HashSet<>();
		Set<String> deletedKeySet = new HashSet<>();
		for (ShopCartDeleteItemPO deleteItem : param.getCartItemList()) {
			if (deleteItem == null) {
				continue;
			}

			// 장바구니번호가 있으면 해당 키를 우선 사용해 단건 삭제합니다.
			Long cartId = deleteItem.getCartId();
			if (cartId != null && cartId > 0L) {
				if (!deletedCartIdSet.add(cartId)) {
					continue;
				}
				deletedCount += cartMapper.deleteShopCartByCartId(custNo, cartId);
				continue;
			}
			String normalizedGoodsId = deleteItem.getGoodsId() == null ? "" : deleteItem.getGoodsId().trim();
			String normalizedSizeId = deleteItem.getSizeId() == null ? "" : deleteItem.getSizeId().trim();
			if (normalizedGoodsId.isEmpty() || normalizedSizeId.isEmpty()) {
				throw new IllegalArgumentException("삭제할 상품 정보를 확인해주세요.");
			}

			String deletedKey = normalizedGoodsId + "|" + normalizedSizeId;
			if (deletedKeySet.contains(deletedKey)) {
				continue;
			}
			deletedKeySet.add(deletedKey);
			deletedCount += cartMapper.deleteShopCartByGoodsIdAndSizeId(custNo, normalizedGoodsId, normalizedSizeId);
		}
		if (deletedCartIdSet.isEmpty() && deletedKeySet.isEmpty()) {
			throw new IllegalArgumentException("삭제할 상품을 선택해주세요.");
		}
		return deletedCount;
	}

	// 쇼핑몰 장바구니 전체 상품을 삭제합니다.
	@Transactional
	int deleteShopCartAll(Long custNo) {
		// 로그인 고객번호 유효성을 확인합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}

		// 장바구니 전체 삭제를 수행합니다.
		return cartMapper.deleteShopCartAll(custNo);
	}

	// 옵션 변경 대상 장바구니 1건을 cartId 우선 기준으로 조회합니다.
	private ShopCartItemVO resolveShopCartItemForOptionUpdate(ShopCartOptionUpdatePO param, Long custNo) {
		// 장바구니번호가 있으면 해당 행을 우선 조회합니다.
		Long cartId = param == null ? null : param.getCartId();
		if (cartId != null && cartId > 0L) {
			ShopCartItemVO cartItem = cartMapper.getShopCartItem(custNo, cartId);
			if (cartItem != null && !isBlank(cartItem.getGoodsId()) && !isBlank(cartItem.getSizeId())) {
				return cartItem;
			}
			throw new IllegalArgumentException("변경할 장바구니 상품을 찾을 수 없습니다.");
		}

		// 하위 호환을 위해 상품/사이즈 기준 조회도 유지합니다.
		if (isBlank(param == null ? null : param.getGoodsId())) {
			throw new IllegalArgumentException("상품코드를 확인해주세요.");
		}
		if (isBlank(param == null ? null : param.getSizeId())) {
			throw new IllegalArgumentException("변경 대상 사이즈를 확인해주세요.");
		}

		String normalizedGoodsId = Objects.requireNonNull(param).getGoodsId().trim();
		String normalizedSizeId = param.getSizeId().trim();
		Long fallbackCartId = cartMapper.getShopCartIdByGoodsIdAndSizeId(custNo, normalizedGoodsId, normalizedSizeId);
		if (fallbackCartId == null || fallbackCartId < 1L) {
			throw new IllegalArgumentException("변경할 장바구니 상품을 찾을 수 없습니다.");
		}
		ShopCartItemVO cartItem = cartMapper.getShopCartItem(custNo, fallbackCartId);
		if (cartItem == null || isBlank(cartItem.getGoodsId()) || isBlank(cartItem.getSizeId())) {
			throw new IllegalArgumentException("변경할 장바구니 상품을 찾을 수 없습니다.");
		}
		return cartItem;
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
	private ShopCartSiteInfoVO resolveShopCartSiteInfo() {
		// 사이트 아이디 기준 배송 기준 정보를 조회합니다.
		return createShopCartSiteInfo(siteInfoMapper.getShopSiteInfo(SHOP_SITE_ID));
	}

	// 쇼핑몰 상품 위시리스트 상태를 계산합니다.
	private ShopGoodsWishlistVO buildShopGoodsWishlist(Long custNo, String goodsId) {
		// 기본값은 비등록 상태로 반환합니다.
		ShopGoodsWishlistVO result = new ShopGoodsWishlistVO();
		result.setWished(false);
		if (custNo == null || custNo < 1 || isBlank(goodsId)) {
			return result;
		}

		// 로그인 고객의 위시리스트 등록 여부를 조회합니다.
		result.setWished(goodsMapper.countShopWishList(custNo, goodsId) > 0);
		return result;
	}

	// 쇼핑몰 상품상세의 사이트 배송 기준 정보를 조회합니다.
	private ShopGoodsSiteInfoVO resolveShopGoodsSiteInfo() {
		// 사이트 아이디 기준 배송 기준 정보를 조회합니다.
		return createShopGoodsSiteInfo(siteInfoMapper.getShopSiteInfo(SHOP_SITE_ID));
	}

	// 사이트 배송 기준 정보를 장바구니 응답 형식으로 변환합니다.
	private ShopCartSiteInfoVO createShopCartSiteInfo(ShopSiteInfoVO siteInfo) {
		// 조회 결과가 없으면 0원 기본값을 반환합니다.
		ShopCartSiteInfoVO result = new ShopCartSiteInfoVO();
		result.setSiteId(isBlank(siteInfo == null ? null : siteInfo.getSiteId()) ? SHOP_SITE_ID : Objects.requireNonNull(siteInfo).getSiteId());
		result.setDeliveryFee(siteInfo == null || siteInfo.getDeliveryFee() == null ? 0 : Math.max(siteInfo.getDeliveryFee(), 0));
		result.setDeliveryFeeLimit(
			siteInfo == null || siteInfo.getDeliveryFeeLimit() == null ? 0 : Math.max(siteInfo.getDeliveryFeeLimit(), 0)
		);
		return result;
	}

	// 사이트 배송 기준 정보를 상품상세 응답 형식으로 변환합니다.
	private ShopGoodsSiteInfoVO createShopGoodsSiteInfo(ShopSiteInfoVO siteInfo) {
		// 조회 결과가 없으면 0원 기본값을 반환합니다.
		ShopGoodsSiteInfoVO result = new ShopGoodsSiteInfoVO();
		result.setSiteId(isBlank(siteInfo == null ? null : siteInfo.getSiteId()) ? SHOP_SITE_ID : Objects.requireNonNull(siteInfo).getSiteId());
		result.setSiteNm(siteInfo == null || siteInfo.getSiteNm() == null ? "" : siteInfo.getSiteNm());
		result.setDeliveryFee(siteInfo == null || siteInfo.getDeliveryFee() == null ? 0 : Math.max(siteInfo.getDeliveryFee(), 0));
		result.setDeliveryFeeLimit(
			siteInfo == null || siteInfo.getDeliveryFeeLimit() == null ? 0 : Math.max(siteInfo.getDeliveryFeeLimit(), 0)
		);
		return result;
	}

	// 쇼핑몰 상품상세 가격 요약 정보를 계산합니다.
	private ShopGoodsPriceSummaryVO buildShopGoodsPriceSummary(ShopGoodsBasicVO goods) {
		// 공급가/판매가를 안전한 값으로 보정합니다.
		int supplyAmt = goods != null && goods.getSupplyAmt() != null ? Math.max(goods.getSupplyAmt(), 0) : 0;
		int saleAmt = goods != null && goods.getSaleAmt() != null ? Math.max(goods.getSaleAmt(), 0) : 0;
		boolean showSupplyStrike = supplyAmt > saleAmt;

		// 할인율은 소수점 버림 정수로 계산합니다.
		int discountRate = 0;
		if (showSupplyStrike) {
			discountRate = (int) Math.floor(((double) (supplyAmt - saleAmt) / (double) supplyAmt) * 100.0d);
		}

		// 가격 요약 정보를 구성합니다.
		ShopGoodsPriceSummaryVO result = new ShopGoodsPriceSummaryVO();
		result.setSupplyAmt(supplyAmt);
		result.setSaleAmt(saleAmt);
		result.setShowSupplyStrike(showSupplyStrike);
		result.setDiscountRate(Math.max(discountRate, 0));
		return result;
	}

	// 쇼핑몰 상품상세 예정 포인트 정보를 계산합니다.
	private ShopGoodsPointSummaryVO buildShopGoodsPointSummary(ShopGoodsBasicVO goods, String custGradeCd) {
		// 고객등급 기준 적립률을 조회합니다.
		String resolvedCustGradeCd = resolveCustGradeCd(custGradeCd);
		int pointSaveRate = resolveShopPointSaveRate(resolvedCustGradeCd);

		// 판매가 기준 예정 포인트를 계산합니다.
		int saleAmt = goods != null && goods.getSaleAmt() != null ? Math.max(goods.getSaleAmt(), 0) : 0;
		int expectedPoint = (int) ((long) saleAmt * (long) pointSaveRate / 100L);

		// 포인트 요약 정보를 구성합니다.
		ShopGoodsPointSummaryVO result = new ShopGoodsPointSummaryVO();
		result.setCustGradeCd(resolvedCustGradeCd);
		result.setPointSaveRate(pointSaveRate);
		result.setExpectedPoint(Math.max(expectedPoint, 0));
		return result;
	}

	// 쇼핑몰 상품상세 배송비 요약 정보를 계산합니다.
	private ShopGoodsShippingSummaryVO buildShopGoodsShippingSummary(ShopGoodsBasicVO goods, ShopGoodsSiteInfoVO siteInfo) {
		// 판매가/배송비/무료배송 기준 금액을 안전한 값으로 보정합니다.
		int saleAmt = goods != null && goods.getSaleAmt() != null ? Math.max(goods.getSaleAmt(), 0) : 0;
		int deliveryFee = siteInfo != null && siteInfo.getDeliveryFee() != null ? Math.max(siteInfo.getDeliveryFee(), 0) : 0;
		int deliveryFeeLimit = siteInfo != null && siteInfo.getDeliveryFeeLimit() != null ? Math.max(siteInfo.getDeliveryFeeLimit(), 0) : 0;

		// 판매가가 무료배송 기준보다 큰 경우 무료배송으로 계산합니다.
		boolean isFreeDelivery = saleAmt > deliveryFeeLimit;
		String shippingMessage = isFreeDelivery
			? "무료배송"
			: String.format("%,d원 (%,d원 이상 구매시 무료 배송)", deliveryFee, deliveryFeeLimit);

		// 배송비 요약 정보를 구성합니다.
		ShopGoodsShippingSummaryVO result = new ShopGoodsShippingSummaryVO();
		result.setFreeDelivery(isFreeDelivery);
		result.setDeliveryFee(deliveryFee);
		result.setDeliveryFeeLimit(deliveryFeeLimit);
		result.setShippingMessage(shippingMessage);
		return result;
	}

	// 쇼핑몰 상품상세에 노출 가능한 상품쿠폰 목록을 조회합니다.
	private List<ShopGoodsCouponVO> getAvailableShopGoodsCouponList(String goodsId, Integer brandNo) {
		// 활성 상품쿠폰 원본 목록이 없으면 빈 목록을 반환합니다.
		List<ShopGoodsCouponVO> sourceCouponList = goodsMapper.getShopActiveGoodsCouponList();
		if (sourceCouponList == null || sourceCouponList.isEmpty() || isBlank(goodsId)) {
			return List.of();
		}

		// 쿠폰 타겟 매칭용 상품 기준값을 조회합니다.
		String brandNoValue = brandNo == null ? null : String.valueOf(brandNo);
		Set<String> categoryIdSet = toSafeStringSet(goodsMapper.getShopGoodsCategoryIdList(goodsId));
		Set<String> exhibitionTabNoSet = toSafeStringSet(goodsMapper.getShopGoodsExhibitionTabNoList(goodsId));

		// 쿠폰 타겟(적용/제외) 매칭 결과를 기준으로 목록을 필터링합니다.
		List<ShopGoodsCouponVO> resultList = new ArrayList<>();
		for (ShopGoodsCouponVO coupon : sourceCouponList) {
			if (coupon == null || coupon.getCpnNo() == null) {
				continue;
			}
			if (!CPN_GB_GOODS.equals(coupon.getCpnGbCd())) {
				continue;
			}
			List<ShopGoodsCouponTargetVO> targetList = goodsMapper.getShopCouponTargetList(coupon.getCpnNo());
			if (isMatchedShopGoodsCoupon(coupon, targetList, goodsId, brandNoValue, categoryIdSet, exhibitionTabNoSet)) {
				resultList.add(coupon);
			}
		}
		return resultList;
	}

	// 쇼핑몰 상품상세에 노출 가능한 상품쿠폰 목록에서 지정 쿠폰번호 1건을 찾습니다.
	private ShopGoodsCouponVO findAvailableShopGoodsCoupon(String goodsId, Integer brandNo, Long cpnNo) {
		// 비교할 쿠폰번호가 없으면 null을 반환합니다.
		if (cpnNo == null || cpnNo < 1L) {
			return null;
		}

		// 현재 상품에 노출 가능한 쿠폰 목록에서 동일 쿠폰번호를 탐색합니다.
		List<ShopGoodsCouponVO> availableCouponList = getAvailableShopGoodsCouponList(goodsId, brandNo);
		for (ShopGoodsCouponVO coupon : availableCouponList) {
			if (coupon == null || coupon.getCpnNo() == null) {
				continue;
			}
			if (cpnNo.equals(coupon.getCpnNo())) {
				return coupon;
			}
		}
		return null;
	}

	// 쿠폰 1건이 현재 상품에 적용 가능한지 판정합니다.
	private boolean isMatchedShopGoodsCoupon(
		ShopGoodsCouponVO coupon,
		List<ShopGoodsCouponTargetVO> targetList,
		String goodsId,
		String brandNoValue,
		Set<String> categoryIdSet,
		Set<String> exhibitionTabNoSet
	) {
		// 상품상세 쿠폰 판정은 공통 타겟 매칭 로직을 재사용합니다.
		if (coupon == null) {
			return false;
		}
		return isMatchedShopCouponTarget(
			coupon.getCpnTargetCd(),
			targetList,
			goodsId,
			brandNoValue,
			categoryIdSet,
			exhibitionTabNoSet
		);
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

	// 기기별 상세설명 원본 목록을 PC/MO 응답 형태로 변환합니다.
	private ShopGoodsDescVO buildShopGoodsDesc(List<ShopGoodsDescItemVO> descItemList) {
		// 기본값은 빈 문자열로 반환합니다.
		ShopGoodsDescVO result = new ShopGoodsDescVO();
		result.setPcDesc("");
		result.setMoDesc("");
		if (descItemList == null || descItemList.isEmpty()) {
			return result;
		}

		// 디바이스 구분 코드 기준으로 상세설명을 매핑합니다.
		for (ShopGoodsDescItemVO item : descItemList) {
			if (item == null || isBlank(item.getDeviceGbCd())) {
				continue;
			}
			if ("PC".equals(item.getDeviceGbCd())) {
				result.setPcDesc(item.getGoodsDesc() == null ? "" : item.getGoodsDesc());
				continue;
			}
			if ("MO".equals(item.getDeviceGbCd())) {
				result.setMoDesc(item.getGoodsDesc() == null ? "" : item.getGoodsDesc());
			}
		}
		return result;
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

	// 사이즈 목록의 품절 여부 상태를 계산합니다.
	private void applyShopGoodsSizeSoldOutState(List<ShopGoodsSizeItemVO> sizeList) {
		if (sizeList == null || sizeList.isEmpty()) {
			return;
		}
		for (ShopGoodsSizeItemVO size : sizeList) {
			if (size == null) {
				continue;
			}
			int stockQty = size.getStockQty() == null ? 0 : size.getStockQty();
			size.setSoldOut(stockQty <= 0);
		}
	}

	// 문자열 목록을 null 안전한 Set으로 변환합니다.
	private Set<String> toSafeStringSet(List<String> sourceList) {
		// 원본 목록이 없으면 빈 Set을 반환합니다.
		if (sourceList == null || sourceList.isEmpty()) {
			return Set.of();
		}
		return new HashSet<>(sourceList);
	}

	// 카테고리 상품 정렬 순서 저장 요청을 검증합니다.
	public String validateCategoryGoodsOrderSave(CategoryGoodsOrderSavePO param) {
		// 요청 데이터 유효성을 확인합니다.
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}
		if (isBlank(param.getCategoryId())) {
			return "카테고리 코드를 확인해주세요.";
		}
		if (param.getUdtNo() == null) {
			return "수정자 정보를 확인해주세요.";
		}
		List<CategoryGoodsOrderItem> orders = param.getOrders();
		if (orders == null || orders.isEmpty()) {
			return "저장할 순서 정보가 없습니다.";
		}
		for (CategoryGoodsOrderItem item : orders) {
			if (item == null || isBlank(item.getGoodsId()) || item.getDispOrd() == null) {
				return "순서 정보가 올바르지 않습니다.";
			}
		}
		return null;
	}

	// 카테고리 상품 정렬 순서를 저장합니다.
	@Transactional
	public int saveCategoryGoodsOrder(CategoryGoodsOrderSavePO param) {
		// 요청 데이터가 없으면 처리하지 않습니다.
		if (param == null) {
			return 0;
		}
		int updated = 0;
		// 정렬 순서를 순회하며 저장합니다.
		for (CategoryGoodsOrderItem item : param.getOrders()) {
			if (item == null) {
				continue;
			}
			CategoryGoodsSavePO savePO = new CategoryGoodsSavePO();
			savePO.setCategoryId(param.getCategoryId());
			savePO.setGoodsId(item.getGoodsId());
			savePO.setDispOrd(item.getDispOrd());
			savePO.setUdtNo(param.getUdtNo());
			updated += goodsMapper.updateCategoryGoodsDispOrd(savePO);
		}
		return updated;
	}

	// 카테고리 상품 등록 요청을 검증합니다.
	public String validateCategoryGoodsRegister(CategoryGoodsRegisterPO param) {
		// 요청 데이터 유효성을 확인합니다.
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}
		if (isBlank(param.getCategoryId())) {
			return "카테고리를 선택해주세요.";
		}
		if (param.getRegNo() == null) {
			return "등록자 정보를 확인해주세요.";
		}
		if (param.getUdtNo() == null) {
			return "수정자 정보를 확인해주세요.";
		}
		if (param.getGoodsIds() == null || param.getGoodsIds().isEmpty()) {
			return "등록할 상품을 선택해주세요.";
		}
		return null;
	}

	// 카테고리 상품을 등록합니다.
	@Transactional
	public int registerCategoryGoods(CategoryGoodsRegisterPO param) {
		// 요청 데이터가 없으면 처리하지 않습니다.
		if (param == null) {
			return 0;
		}
		// 카테고리 정보를 조회합니다.
		CategoryVO category = goodsMapper.getAdminCategoryDetail(param.getCategoryId());
		if (category == null) {
			return 0;
		}
		int inserted = 0;
		// 저장일자 기반 정렬 순서를 생성합니다.
		Integer dispOrd = buildTodayDispOrd();
		// 상위 카테고리 목록을 조회합니다.
		List<CategoryVO> parents = getCategoryParents(category.getCategoryId());
		// 상품 목록을 순회하며 등록합니다.
		for (String goodsId : param.getGoodsIds()) {
			if (isBlank(goodsId)) {
				continue;
			}
			// 대상 카테고리에 상품을 등록합니다.
			if (insertCategoryGoodsIfNotExists(category.getCategoryId(), goodsId, dispOrd, param.getRegNo(), param.getUdtNo())) {
				inserted += 1;
			}
			// 상위 카테고리에도 등록합니다.
			for (CategoryVO parent : parents) {
				if (parent == null) {
					continue;
				}
				if (insertCategoryGoodsIfNotExists(parent.getCategoryId(), goodsId, dispOrd, param.getRegNo(), param.getUdtNo())) {
					inserted += 1;
				}
			}
		}
		return inserted;
	}

	// 카테고리 상품 삭제 요청을 검증합니다.
	public String validateCategoryGoodsDelete(CategoryGoodsDeletePO param) {
		// 요청 데이터 유효성을 확인합니다.
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}
		if (isBlank(param.getCategoryId())) {
			return "카테고리를 선택해주세요.";
		}
		if (param.getUdtNo() == null) {
			return "수정자 정보를 확인해주세요.";
		}
		if (param.getGoodsIds() == null || param.getGoodsIds().isEmpty()) {
			return "삭제할 상품을 선택해주세요.";
		}
		return null;
	}

	// 카테고리 상품을 삭제합니다.
	@Transactional
	public int deleteCategoryGoods(CategoryGoodsDeletePO param) {
		// 요청 데이터가 없으면 처리하지 않습니다.
		if (param == null) {
			return 0;
		}
		int deleted = 0;
		// 삭제 대상 카테고리를 조회합니다.
		CategoryVO category = goodsMapper.getAdminCategoryDetail(param.getCategoryId());
		if (category == null) {
			return 0;
		}
		// 상품 목록을 순회하며 삭제합니다.
		for (String goodsId : param.getGoodsIds()) {
			if (isBlank(goodsId)) {
				continue;
			}
			deleted += deleteCategoryGoodsWithHierarchy(category, goodsId, param.getUdtNo());
		}
		return deleted;
	}

	// 카테고리 상품 엑셀 업로드 요청을 검증합니다.
	public String validateCategoryGoodsExcelUpload(MultipartFile file, Long regNo, Long udtNo) {
		// 요청 데이터 유효성을 확인합니다.
		if (file == null || file.isEmpty()) {
			return "업로드할 엑셀 파일을 선택해주세요.";
		}
		if (regNo == null) {
			return "등록자 정보를 확인해주세요.";
		}
		if (udtNo == null) {
			return "수정자 정보를 확인해주세요.";
		}
		return null;
	}

	// 카테고리 상품 엑셀 업로드를 처리합니다.
	@Transactional
	public Map<String, Object> uploadCategoryGoodsExcel(MultipartFile file, Long regNo, Long udtNo) throws IOException {
		// 업로드 데이터를 파싱합니다.
		List<CategoryGoodsExcelRow> rows = parseCategoryGoodsExcel(file);
		int inserted = 0;
		int updated = 0;
		// 업로드 데이터를 순회하며 저장합니다.
		for (CategoryGoodsExcelRow row : rows) {
			if (row == null) {
				continue;
			}
			// 카테고리 정보를 확인합니다.
			CategoryVO category = goodsMapper.getAdminCategoryDetail(row.categoryId());
			if (category == null) {
				throw new IllegalArgumentException("카테고리 정보를 확인해주세요.");
			}
			// 상품 정보를 확인합니다.
			GoodsDetailVO goods = goodsMapper.getAdminGoodsDetail(row.goodsId());
			if (goods == null) {
				throw new IllegalArgumentException("상품 정보를 확인해주세요.");
			}
			// 대상 카테고리의 정렬 순서를 저장합니다.
			boolean exists = goodsMapper.countCategoryGoods(row.categoryId(), row.goodsId()) > 0;
			if (exists) {
				CategoryGoodsSavePO updatePO = new CategoryGoodsSavePO();
				updatePO.setCategoryId(row.categoryId());
				updatePO.setGoodsId(row.goodsId());
				updatePO.setDispOrd(row.dispOrd());
				updatePO.setUdtNo(udtNo);
				updated += goodsMapper.updateCategoryGoodsDispOrd(updatePO);
			} else {
				if (insertCategoryGoodsIfNotExists(row.categoryId(), row.goodsId(), row.dispOrd(), regNo, udtNo)) {
					inserted += 1;
				}
			}
			// 상위 카테고리에는 신규 등록만 수행합니다.
			List<CategoryVO> parents = getCategoryParents(row.categoryId());
			for (CategoryVO parent : parents) {
				if (parent == null) {
					continue;
				}
				insertCategoryGoodsIfNotExists(parent.getCategoryId(), row.goodsId(), row.dispOrd(), regNo, udtNo);
			}
		}
		Map<String, Object> result = new HashMap<>();
		result.put("inserted", inserted);
		result.put("updated", updated);
		result.put("total", rows.size());
		return result;
	}

	// 카테고리 상품 엑셀 다운로드 데이터를 생성합니다.
	public byte[] buildCategoryGoodsExcel(String categoryId) throws IOException {
		// 카테고리별 상품 목록을 조회합니다.
		List<CategoryGoodsVO> list = goodsMapper.getAdminCategoryGoodsList(categoryId);
		// 엑셀 워크북을 생성합니다.
		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("category_goods");
		// 헤더 행을 생성합니다.
		Row header = sheet.createRow(0);
		header.createCell(0).setCellValue("카테고리코드");
		header.createCell(1).setCellValue("상품코드");
		header.createCell(2).setCellValue("노출순서");
		// 데이터 행을 생성합니다.
		int rowIndex = 1;
		for (CategoryGoodsVO item : list) {
			Row row = sheet.createRow(rowIndex);
			row.createCell(0).setCellValue(item.getCategoryId());
			row.createCell(1).setCellValue(item.getGoodsId());
			row.createCell(2).setCellValue(item.getDispOrd() != null ? item.getDispOrd() : 0);
			rowIndex += 1;
		}
		// 엑셀 데이터를 바이트 배열로 반환합니다.
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			workbook.write(outputStream);
			workbook.close();
			return outputStream.toByteArray();
		}
	}

	// 카테고리 상위 목록을 조회합니다.
	private List<CategoryVO> getCategoryParents(String categoryId) {
		List<CategoryVO> parents = new ArrayList<>();
		// 상위 카테고리를 순차적으로 조회합니다.
		String nextParentId = categoryId;
		for (int index = 0; index < 5; index += 1) {
			if (isBlank(nextParentId) || "0".equals(nextParentId)) {
				break;
			}
			CategoryVO current = goodsMapper.getAdminCategoryDetail(nextParentId);
			if (current == null || isBlank(current.getParentCategoryId()) || "0".equals(current.getParentCategoryId())) {
				break;
			}
			CategoryVO parent = goodsMapper.getAdminCategoryDetail(current.getParentCategoryId());
			if (parent == null) {
				break;
			}
			parents.add(parent);
			nextParentId = parent.getCategoryId();
		}
		return parents;
	}

	// 카테고리 하위 목록을 재귀적으로 조회합니다.
	private List<CategoryVO> getCategoryChildrenRecursive(String categoryId) {
		List<CategoryVO> children = new ArrayList<>();
		// 하위 카테고리를 조회합니다.
		List<CategoryVO> directChildren = goodsMapper.getCategoryChildren(categoryId);
		for (CategoryVO child : directChildren) {
			if (child == null) {
				continue;
			}
			children.add(child);
			children.addAll(getCategoryChildrenRecursive(child.getCategoryId()));
		}
		return children;
	}

	// 카테고리 상품 등록 여부를 확인하고 등록합니다.
	private boolean insertCategoryGoodsIfNotExists(String categoryId, String goodsId, Integer dispOrd, Long regNo, Long udtNo) {
		// 중복 여부를 확인합니다.
		if (goodsMapper.countCategoryGoods(categoryId, goodsId) > 0) {
			return false;
		}
		// 등록 정보를 생성합니다.
		CategoryGoodsSavePO savePO = new CategoryGoodsSavePO();
		savePO.setCategoryId(categoryId);
		savePO.setGoodsId(goodsId);
		savePO.setDispOrd(dispOrd != null ? dispOrd : 0);
		savePO.setRegNo(regNo);
		savePO.setUdtNo(udtNo);
		// 등록을 수행합니다.
		goodsMapper.insertCategoryGoods(savePO);
		return true;
	}

	// 카테고리 상품과 하위/상위 연관 정보를 함께 삭제합니다.
	private int deleteCategoryGoodsWithHierarchy(CategoryVO category, String goodsId, Long udtNo) {
		int deleted = 0;
		// 기본 삭제를 수행합니다.
		CategoryGoodsSavePO deletePO = new CategoryGoodsSavePO();
		deletePO.setCategoryId(category.getCategoryId());
		deletePO.setGoodsId(goodsId);
		deletePO.setUdtNo(udtNo);
		deleted += goodsMapper.deleteCategoryGoods(deletePO);
		Integer level = category.getCategoryLevel();
		if (level == null) {
			return deleted;
		}
		// 레벨별 삭제 정책을 분기 처리합니다.
		if (level >= 3) {
			// 3레벨 삭제 시 상위 카테고리 처리
			handleParentDeletionForLevel3(category, goodsId, udtNo);
		} else if (level == 2) {
			// 2레벨 삭제 시 하위 3레벨 전체 삭제
			deleted += deleteChildrenGoods(category.getCategoryId(), goodsId, udtNo);
			// 1레벨 삭제 여부를 확인합니다.
			handleParentDeletionForLevel2(category, goodsId, udtNo);
		} else if (level == 1) {
			// 1레벨 삭제 시 하위 전체 삭제
			deleted += deleteChildrenGoods(category.getCategoryId(), goodsId, udtNo);
		}
		return deleted;
	}

	// 3레벨 삭제 시 상위 카테고리를 정리합니다.
	private void handleParentDeletionForLevel3(CategoryVO category, String goodsId, Long udtNo) {
		// 2레벨 상위 카테고리를 조회합니다.
		CategoryVO parent = goodsMapper.getAdminCategoryDetail(category.getParentCategoryId());
		if (parent == null) {
			return;
		}
		// 동일 2레벨 하위에 다른 상품이 없으면 2레벨에서도 삭제합니다.
		if (goodsMapper.countCategoryGoodsInChildren(parent.getCategoryId(), goodsId) == 0) {
			CategoryGoodsSavePO deletePO = new CategoryGoodsSavePO();
			deletePO.setCategoryId(parent.getCategoryId());
			deletePO.setGoodsId(goodsId);
			deletePO.setUdtNo(udtNo);
			goodsMapper.deleteCategoryGoods(deletePO);
		}
		// 1레벨 상위 카테고리를 조회합니다.
		CategoryVO grandParent = goodsMapper.getAdminCategoryDetail(parent.getParentCategoryId());
		if (grandParent == null) {
			return;
		}
		// 동일 1레벨 하위에 다른 상품이 없으면 1레벨에서도 삭제합니다.
		if (goodsMapper.countCategoryGoodsInChildren(grandParent.getCategoryId(), goodsId) == 0) {
			CategoryGoodsSavePO deletePO = new CategoryGoodsSavePO();
			deletePO.setCategoryId(grandParent.getCategoryId());
			deletePO.setGoodsId(goodsId);
			deletePO.setUdtNo(udtNo);
			goodsMapper.deleteCategoryGoods(deletePO);
		}
	}

	// 2레벨 삭제 시 1레벨 삭제 여부를 확인합니다.
	private void handleParentDeletionForLevel2(CategoryVO category, String goodsId, Long udtNo) {
		// 1레벨 상위 카테고리를 조회합니다.
		CategoryVO parent = goodsMapper.getAdminCategoryDetail(category.getParentCategoryId());
		if (parent == null) {
			return;
		}
		// 동일 1레벨 하위에 다른 상품이 없으면 1레벨에서도 삭제합니다.
		if (goodsMapper.countCategoryGoodsInChildren(parent.getCategoryId(), goodsId) == 0) {
			CategoryGoodsSavePO deletePO = new CategoryGoodsSavePO();
			deletePO.setCategoryId(parent.getCategoryId());
			deletePO.setGoodsId(goodsId);
			deletePO.setUdtNo(udtNo);
			goodsMapper.deleteCategoryGoods(deletePO);
		}
	}

	// 하위 카테고리의 상품 정보를 삭제합니다.
	private int deleteChildrenGoods(String parentCategoryId, String goodsId, Long udtNo) {
		int deleted = 0;
		// 하위 카테고리를 재귀적으로 조회합니다.
		List<CategoryVO> children = getCategoryChildrenRecursive(parentCategoryId);
		for (CategoryVO child : children) {
			if (child == null) {
				continue;
			}
			CategoryGoodsSavePO deletePO = new CategoryGoodsSavePO();
			deletePO.setCategoryId(child.getCategoryId());
			deletePO.setGoodsId(goodsId);
			deletePO.setUdtNo(udtNo);
			deleted += goodsMapper.deleteCategoryGoods(deletePO);
		}
		return deleted;
	}

	// 오늘 기준 정렬 순서를 생성합니다.
	private Integer buildTodayDispOrd() {
		// YYYYMMDD 형식으로 정렬 순서를 생성합니다.
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
		String formatted = LocalDate.now().format(formatter);
		return Integer.parseInt(formatted);
	}

	// 카테고리 상품 엑셀 파일을 파싱합니다.
	private List<CategoryGoodsExcelRow> parseCategoryGoodsExcel(MultipartFile file) throws IOException {
		List<CategoryGoodsExcelRow> rows = new ArrayList<>();
		// 엑셀 워크북을 로드합니다.
		try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
			Sheet sheet = workbook.getSheetAt(0);
			if (sheet == null) {
				throw new IllegalArgumentException("엑셀 데이터를 확인해주세요.");
			}
			// 헤더 정보를 확인합니다.
			Row header = sheet.getRow(0);
			if (header == null
				|| !"카테고리코드".equals(getCellString(header.getCell(0)))
				|| !"상품코드".equals(getCellString(header.getCell(1)))
				|| !"노출순서".equals(getCellString(header.getCell(2)))) {
				throw new IllegalArgumentException("엑셀 헤더 형식을 확인해주세요.");
			}
			// 데이터 행을 파싱합니다.
			int lastRow = sheet.getLastRowNum();
			for (int rowIndex = 1; rowIndex <= lastRow; rowIndex += 1) {
				Row row = sheet.getRow(rowIndex);
				if (row == null) {
					throw new IllegalArgumentException("엑셀 데이터에 빈 행이 존재합니다.");
				}
				String categoryId = getCellString(row.getCell(0));
				String goodsId = getCellString(row.getCell(1));
				Integer dispOrd = getCellInteger(row.getCell(2));
				if (isBlank(categoryId) || isBlank(goodsId) || dispOrd == null) {
					throw new IllegalArgumentException("엑셀 데이터에 빈값이 존재합니다.");
				}
				rows.add(new CategoryGoodsExcelRow(categoryId, goodsId, dispOrd));
			}
		}
		return rows;
	}

	// 엑셀 셀 문자열 값을 반환합니다.
	private String getCellString(Cell cell) {
		if (cell == null) {
			return "";
		}
		String value;
		try {
			value = cell.getStringCellValue();
		} catch (Exception e) {
			try {
				value = String.valueOf((long) cell.getNumericCellValue());
			} catch (Exception ex) {
				value = "";
			}
		}
		return value != null ? value.trim() : "";
	}

	// 엑셀 셀 정수 값을 반환합니다.
	private Integer getCellInteger(Cell cell) {
		if (cell == null) {
			return null;
		}
		try {
			return (int) cell.getNumericCellValue();
		} catch (Exception e) {
			String text = getCellString(cell);
			if (isBlank(text)) {
				return null;
			}
			try {
				return Integer.parseInt(text);
			} catch (NumberFormatException ex) {
				return null;
			}
		}
	}

	// 관리자 상품 카테고리 목록을 조회합니다.
	public List<GoodsCategoryVO> getAdminGoodsCategoryList(String goodsId) {
		if (isBlank(goodsId)) {
			return List.of();
		}
		return goodsMapper.getAdminGoodsCategoryList(goodsId);
	}

	// 관리자 상품 카테고리 단건 저장 요청을 검증합니다.
	public String validateGoodsCategorySave(GoodsCategorySavePO param) {
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}
		if (isBlank(param.getGoodsId())) {
			return "상품코드를 확인해주세요.";
		}
		if (isBlank(param.getCategoryId())) {
			return "카테고리를 선택해주세요.";
		}
		if (param.getUdtNo() == null) {
			return "수정자 정보를 확인해주세요.";
		}
		int childCount = goodsMapper.countCategoryChildren(param.getCategoryId());
		if (childCount > 0) {
			return "카테고리를 선택해주세요.";
		}
		return null;
	}

	// 관리자 상품 카테고리를 단건 저장합니다.
	@Transactional
	public int saveAdminGoodsCategory(GoodsCategorySavePO param) {
		if (param.getDispOrd() == null) {
			param.setDispOrd(0);
		}
		if (param.getRegNo() == null) {
			param.setRegNo(param.getUdtNo());
		}
		String originCategoryId = param.getOriginCategoryId();
		if (!isBlank(originCategoryId) && !originCategoryId.equals(param.getCategoryId())) {
			deleteAdminGoodsCategoryHierarchy(originCategoryId, param.getGoodsId(), param.getUdtNo());
		}
		return saveCategoryGoodsHierarchy(param.getCategoryId(), param.getGoodsId(), param.getDispOrd(), param.getRegNo(), param.getUdtNo());
	}

	// 관리자 상품 카테고리 단건 삭제 요청을 검증합니다.
	public String validateGoodsCategoryDelete(GoodsCategorySavePO param) {
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}
		if (isBlank(param.getGoodsId())) {
			return "상품코드를 확인해주세요.";
		}
		if (isBlank(param.getCategoryId())) {
			return "카테고리를 선택해주세요.";
		}
		if (param.getUdtNo() == null) {
			return "수정자 정보를 확인해주세요.";
		}
		return null;
	}

	// 관리자 상품 카테고리를 단건 삭제합니다.
	@Transactional
	public int deleteAdminGoodsCategory(GoodsCategorySavePO param) {
		return deleteAdminGoodsCategoryHierarchy(param.getCategoryId(), param.getGoodsId(), param.getUdtNo());
	}

	// 관리자 상품 카테고리를 저장합니다.
	private void saveAdminGoodsCategories(GoodsSavePO param) {
		if (param == null || param.getCategoryList() == null) {
			return;
		}
		String goodsId = param.getGoodsId();
		if (isBlank(goodsId)) {
			return;
		}
		Long writerNo = param.getUdtNo() != null ? param.getUdtNo() : param.getRegNo();
		if (writerNo == null) {
			return;
		}
		List<GoodsCategoryItem> categoryList = param.getCategoryList();
		goodsMapper.deleteCategoryGoodsByGoodsId(goodsId);
		if (categoryList.isEmpty()) {
			return;
		}
		for (int index = 0; index < categoryList.size(); index += 1) {
			GoodsCategoryItem item = categoryList.get(index);
			if (item == null || isBlank(item.getCategoryId())) {
				continue;
			}
			Integer dispOrd = item.getDispOrd() != null ? item.getDispOrd() : index + 1;
			saveCategoryGoodsHierarchy(item.getCategoryId(), goodsId, dispOrd, writerNo, writerNo);
		}
	}

	// 관리자 상품 카테고리와 상위 카테고리 매핑을 함께 저장합니다.
	private int saveCategoryGoodsHierarchy(String categoryId, String goodsId, Integer dispOrd, Long regNo, Long udtNo) {
		// 대상 카테고리 정보를 확인합니다.
		CategoryVO category = goodsMapper.getAdminCategoryDetail(categoryId);
		if (category == null || isBlank(goodsId)) {
			return 0;
		}

		int affected = upsertCategoryGoodsDispOrd(category.getCategoryId(), goodsId, dispOrd, regNo, udtNo);
		// 상위 카테고리 매핑을 누락 없이 보정합니다.
		List<CategoryVO> parents = getCategoryParents(category.getCategoryId());
		for (CategoryVO parent : parents) {
			if (parent == null || isBlank(parent.getCategoryId())) {
				continue;
			}
			if (goodsMapper.countCategoryGoods(parent.getCategoryId(), goodsId) > 0) {
				continue;
			}
			CategoryGoodsSavePO savePO = new CategoryGoodsSavePO();
			savePO.setCategoryId(parent.getCategoryId());
			savePO.setGoodsId(goodsId);
			savePO.setDispOrd(dispOrd != null ? dispOrd : 0);
			savePO.setRegNo(regNo);
			savePO.setUdtNo(udtNo);
			affected += goodsMapper.insertCategoryGoods(savePO);
		}
		return affected;
	}

	// 관리자 상품 카테고리 매핑을 단건 저장하거나 정렬 순서를 갱신합니다.
	private int upsertCategoryGoodsDispOrd(String categoryId, String goodsId, Integer dispOrd, Long regNo, Long udtNo) {
		// 기존 매핑 존재 여부를 확인합니다.
		CategoryGoodsSavePO savePO = new CategoryGoodsSavePO();
		savePO.setCategoryId(categoryId);
		savePO.setGoodsId(goodsId);
		savePO.setDispOrd(dispOrd != null ? dispOrd : 0);
		savePO.setRegNo(regNo);
		savePO.setUdtNo(udtNo);
		if (goodsMapper.countCategoryGoods(categoryId, goodsId) > 0) {
			return goodsMapper.updateCategoryGoodsDispOrd(savePO);
		}
		return goodsMapper.insertCategoryGoods(savePO);
	}

	// 관리자 상품 카테고리와 계층 매핑을 함께 삭제합니다.
	private int deleteAdminGoodsCategoryHierarchy(String categoryId, String goodsId, Long udtNo) {
		// 삭제 대상 카테고리 정보를 확인합니다.
		CategoryVO category = goodsMapper.getAdminCategoryDetail(categoryId);
		if (category == null || isBlank(goodsId)) {
			return 0;
		}
		return deleteCategoryGoodsWithHierarchy(category, goodsId, udtNo);
	}

	// 관리자 상품 사이즈 삭제 요청을 검증합니다.
	public String validateGoodsSizeDelete(GoodsSizeSavePO param) {
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}
		if (isBlank(param.getGoodsId())) {
			return "상품코드를 확인해주세요.";
		}
		if (isBlank(param.getSizeId())) {
			return "사이즈코드를 확인해주세요.";
		}
		if (param.getUdtNo() == null) {
			return "수정자 정보를 확인해주세요.";
		}
		return null;
	}

	// 관리자 상품 사이즈를 삭제 처리합니다.
	public int deleteAdminGoodsSize(GoodsSizeSavePO param) {
		return goodsMapper.deleteAdminGoodsSize(param);
	}

	// 관리자 상품 사이즈 순서 저장 요청을 검증합니다.
	public String validateGoodsSizeOrderSave(GoodsSizeOrderSavePO param) {
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}
		if (isBlank(param.getGoodsId())) {
			return "상품코드를 확인해주세요.";
		}
		if (param.getUdtNo() == null) {
			return "수정자 정보를 확인해주세요.";
		}
		List<GoodsSizeOrderItem> orders = param.getOrders();
		if (orders == null || orders.isEmpty()) {
			return "저장할 순서 정보가 없습니다.";
		}
		for (GoodsSizeOrderItem item : orders) {
			if (item == null || isBlank(item.getSizeId()) || item.getDispOrd() == null) {
				return "순서 정보가 올바르지 않습니다.";
			}
		}
		return null;
	}

	// 관리자 상품 사이즈 순서를 저장합니다.
	public int updateAdminGoodsSizeOrder(GoodsSizeOrderSavePO param) {
		return goodsMapper.updateAdminGoodsSizeOrder(param);
	}

	// 관리자 상품 이미지 목록을 조회합니다.
	public List<GoodsImageVO> getAdminGoodsImageList(String goodsId) {
		if (isBlank(goodsId)) {
			return List.of();
		}
		List<GoodsImageVO> list = goodsMapper.getAdminGoodsImageList(goodsId);
		goodsImageService.applyAdminGoodsImageUrls(list, goodsId);
		return list;
	}

	// 상품 이미지 업로드 요청을 검증합니다.
	public String validateGoodsImageUpload(String goodsId, MultipartFile image, Long regNo) {
		if (isBlank(goodsId)) {
			return "상품코드를 확인해주세요.";
		}
		if (regNo == null) {
			return "등록자 정보를 확인해주세요.";
		}
		if (image == null || image.isEmpty()) {
			return "이미지를 선택해주세요.";
		}
		String contentType = image.getContentType();
		if (contentType == null || !contentType.startsWith("image/")) {
			return "이미지 파일만 업로드할 수 있습니다.";
		}
		try {
			BufferedImage bufferedImage = ImageIO.read(image.getInputStream());
			if (bufferedImage == null) {
				return "이미지 파일을 확인해주세요.";
			}
			int width = bufferedImage.getWidth();
			int height = bufferedImage.getHeight();
			if (width != height) {
				return "정사각형 이미지만 업로드할 수 있습니다.";
			}
			if (width < GOODS_IMAGE_MIN_SIZE || width > GOODS_IMAGE_MAX_SIZE) {
				return "이미지 크기는 500x500 ~ 1500x1500px만 가능합니다.";
			}
		} catch (IOException e) {
			return "이미지 파일을 확인해주세요.";
		}
		return null;
	}

	// 관리자 상품 이미지를 등록합니다.
	public GoodsImageVO uploadAdminGoodsImage(String goodsId, MultipartFile image, Long regNo) throws IOException {
		String imageUrl = ftpFileService.uploadGoodsImage(image, goodsId, String.valueOf(regNo));

		int maxDispOrd = goodsMapper.getAdminGoodsImageMaxDispOrd(goodsId);
		GoodsImageSavePO savePO = new GoodsImageSavePO();
		savePO.setGoodsId(goodsId);
		savePO.setDispOrd(maxDispOrd + 1);
		savePO.setImgPath(imageUrl);
		savePO.setRegNo(regNo);
		savePO.setUdtNo(regNo);
		goodsMapper.insertAdminGoodsImage(savePO);

		GoodsImageVO result = new GoodsImageVO();
		result.setGoodsId(goodsId);
		result.setImgPath(imageUrl);
		result.setDispOrd(maxDispOrd + 1);
		result.setImgUrl(imageUrl);
		return result;
	}

	// 관리자 상품 이미지 삭제 요청을 검증합니다.
	public String validateGoodsImageDelete(GoodsImageSavePO param) {
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}
		if (isBlank(param.getGoodsId())) {
			return "상품코드를 확인해주세요.";
		}
		if (param.getImgNo() == null) {
			return "이미지 정보를 확인해주세요.";
		}
		if (param.getUdtNo() == null) {
			return "수정자 정보를 확인해주세요.";
		}
		return null;
	}

	// 관리자 상품 이미지를 삭제합니다.
	public int deleteAdminGoodsImage(GoodsImageSavePO param) {
		GoodsImageVO current = goodsMapper.getAdminGoodsImageByNo(param.getImgNo());
		if (current == null || isBlank(current.getGoodsId())) {
			return 0;
		}
		if (!current.getGoodsId().equals(param.getGoodsId())) {
			return 0;
		}
		int result = goodsMapper.deleteAdminGoodsImage(param.getImgNo());
		String fileName = extractFileName(current.getImgPath());
		if (!isBlank(fileName)) {
			try {
				ftpFileService.deleteGoodsImage(current.getGoodsId(), fileName);
			} catch (IOException e) {
				// FTP 삭제 실패는 무시합니다.
			}
		}
		return result;
	}

	// 관리자 상품 이미지 순서 저장 요청을 검증합니다.
	public String validateGoodsImageOrderSave(GoodsImageOrderSavePO param) {
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}
		if (isBlank(param.getGoodsId())) {
			return "상품코드를 확인해주세요.";
		}
		if (param.getUdtNo() == null) {
			return "수정자 정보를 확인해주세요.";
		}
		List<GoodsImageOrderItem> orders = param.getOrders();
		if (orders == null || orders.isEmpty()) {
			return "저장할 순서 정보가 없습니다.";
		}
		for (GoodsImageOrderItem item : orders) {
			if (item == null || item.getImgNo() == null || item.getDispOrd() == null) {
				return "순서 정보가 올바르지 않습니다.";
			}
		}
		return null;
	}

	// 관리자 상품 이미지 순서를 저장합니다.
	public int updateAdminGoodsImageOrder(GoodsImageOrderSavePO param) {
		return goodsMapper.updateAdminGoodsImageOrder(param);
	}

	// 관리자 상품 상세 설명 목록을 조회합니다.
	public List<GoodsDescVO> getAdminGoodsDescList(String goodsId) {
		if (isBlank(goodsId)) {
			return List.of();
		}
		return goodsMapper.getAdminGoodsDescList(goodsId);
	}

	// 관리자 상품 상세 설명 저장 요청을 검증합니다.
	public String validateGoodsDescSave(GoodsDescSavePO param) {
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}
		if (isBlank(param.getGoodsId())) {
			return "상품코드를 확인해주세요.";
		}
		if (param.getUdtNo() == null) {
			return "수정자 정보를 확인해주세요.";
		}
		List<GoodsDescSaveItem> list = param.getList();
		if (list == null || list.isEmpty()) {
			return "저장할 상세 정보가 없습니다.";
		}
		for (GoodsDescSaveItem item : list) {
			if (item == null || isBlank(item.getDeviceGbCd())) {
				return "디바이스 구분을 확인해주세요.";
			}
		}
		return null;
	}

	// 관리자 상품 상세 설명을 저장합니다.
	public int saveAdminGoodsDesc(GoodsDescSavePO param) {
		int result = 0;
		Long regNo = param.getRegNo() != null ? param.getRegNo() : param.getUdtNo();
		for (GoodsDescSaveItem item : param.getList()) {
			if (item == null || isBlank(item.getDeviceGbCd())) {
				continue;
			}
			item.setGoodsId(param.getGoodsId());
			item.setUdtNo(param.getUdtNo());
			item.setRegNo(regNo);
			if (item.getGoodsDesc() == null) {
				item.setGoodsDesc("");
			}
			int count = goodsMapper.countAdminGoodsDesc(param.getGoodsId(), item.getDeviceGbCd());
			if (count > 0) {
				result += goodsMapper.updateAdminGoodsDesc(item);
			} else {
				result += goodsMapper.insertAdminGoodsDesc(item);
			}
		}
		return result;
	}

	// 상품 이미지 URL에서 파일명을 추출합니다.
	private String extractFileName(String imgPath) {
		if (isBlank(imgPath)) {
			return null;
		}
		int index = imgPath.lastIndexOf('/');
		if (index < 0 || index >= imgPath.length() - 1) {
			return imgPath;
		}
		return imgPath.substring(index + 1);
	}

}
