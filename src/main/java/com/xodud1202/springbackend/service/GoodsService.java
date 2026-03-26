package com.xodud1202.springbackend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xodud1202.springbackend.config.properties.TossProperties;
import com.xodud1202.springbackend.domain.admin.brand.BrandVO;
import com.xodud1202.springbackend.domain.admin.category.*;
import com.xodud1202.springbackend.domain.admin.goods.*;
import com.xodud1202.springbackend.domain.admin.order.*;
import com.xodud1202.springbackend.domain.shop.cart.*;
import com.xodud1202.springbackend.domain.shop.category.ShopCategoryGoodsItemVO;
import com.xodud1202.springbackend.domain.shop.goods.*;
import com.xodud1202.springbackend.domain.shop.mypage.*;
import com.xodud1202.springbackend.domain.shop.order.*;
import com.xodud1202.springbackend.mapper.ExhibitionMapper;
import com.xodud1202.springbackend.mapper.GoodsMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
import java.util.function.Supplier;

import static com.xodud1202.springbackend.common.Constants.Common.*;
import static com.xodud1202.springbackend.common.Constants.Shop.*;

@Service
@RequiredArgsConstructor
// 관리자 상품 관련 비즈니스 로직을 처리합니다.
public class GoodsService {
	private static final Logger log = LoggerFactory.getLogger(GoodsService.class);

	private final GoodsMapper goodsMapper;
	private final ExhibitionMapper exhibitionMapper;
	private final FtpFileService ftpFileService;
	private final ShopAuthService shopAuthService;
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

	// 관리자 상품 목록을 페이징 조건으로 조회합니다.
	public Map<String, Object> getAdminGoodsList(GoodsPO param) {
		int page = param.getPage() == null || param.getPage() < 1 ? 1 : param.getPage();
		// 페이지 사이즈 기본값과 최대값을 설정합니다.
		int pageSize = 20;
		if (param.getPageSize() != null && param.getPageSize() > 0) {
			pageSize = Math.min(param.getPageSize(), 200);
		}
		int offset = (page - 1) * pageSize;

		param.setPage(page);
		param.setPageSize(pageSize);
		param.setOffset(offset);
		param.setSearchKeyword(buildGoodsNameSearchKeyword(param));

		List<GoodsVO> list = goodsMapper.getAdminGoodsList(param);
		// 상품 이미지 URL을 세팅합니다.
		applyGoodsImageUrls(list);
		int totalCount = goodsMapper.getAdminGoodsCount(param);

		Map<String, Object> result = new HashMap<>();
		result.put("list", list);
		result.put("totalCount", totalCount);
		result.put("page", page);
		result.put("pageSize", pageSize);
		return result;
	}

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
		int resolvedPage = page == null || page < 1 ? ADMIN_ORDER_DEFAULT_PAGE : page;
		int resolvedPageSize = pageSize == null || pageSize < 1
			? ADMIN_ORDER_DEFAULT_PAGE_SIZE
			: Math.min(pageSize, ADMIN_ORDER_MAX_PAGE_SIZE);
		int offset = (resolvedPage - 1) * resolvedPageSize;

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
		List<AdminOrderListRowVO> list = goodsMapper.getAdminOrderList(param);
		int totalCount = goodsMapper.getAdminOrderCount(param);

		// 응답 객체를 구성합니다.
		AdminOrderListResponseVO result = new AdminOrderListResponseVO();
		result.setList(list == null ? List.of() : list);
		result.setTotalCount(totalCount);
		result.setPage(resolvedPage);
		result.setPageSize(resolvedPageSize);
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
	public int insertAdminGoods(GoodsSavePO param) {
		if (param != null && param.getUdtNo() == null) {
			param.setUdtNo(param.getRegNo());
		}
		int result = goodsMapper.insertAdminGoods(param);
		saveAdminGoodsCategories(param);
		return result;
	}

	// 관리자 상품을 수정합니다.
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
			if (builder.length() > 0) {
				builder.append(' ');
			}
			builder.append('+').append(token).append('*');
		}
		return builder.length() == 0 ? null : builder.toString();
	}

	// 문자열 공백 여부를 확인합니다.
	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	// 문자열을 trim 처리하고 비어 있으면 null을 반환합니다.
	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
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
		Integer categoryLevel = 1;
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
		applyCategoryGoodsImageUrls(list);
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
		applyShopCategoryGoodsImageUrls(list);
		return list;
	}

	// 쇼핑몰 마이페이지 위시리스트 페이지 데이터를 조회합니다.
	public ShopMypageWishPageVO getShopMypageWishPage(Long custNo, Integer requestedPageNo) {
		// 고객번호가 없으면 로그인 예외를 반환합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}

		// 요청 페이지 번호를 1 이상으로 보정합니다.
		int resolvedRequestedPageNo = resolveRequestedPageNo(requestedPageNo);
		// 위시리스트 전체 건수를 조회합니다.
		int goodsCount = goodsMapper.countShopMypageWishGoods(custNo);
		// 전체 페이지 수를 계산합니다.
		int totalPageCount = calculateTotalPageCount(goodsCount, SHOP_MYPAGE_WISH_PAGE_SIZE);
		// 범위를 초과한 페이지 번호를 마지막 페이지로 보정합니다.
		int resolvedPageNo = totalPageCount == 0 ? 1 : Math.min(resolvedRequestedPageNo, totalPageCount);
		// 페이지 조회 오프셋을 계산합니다.
		int offset = calculateOffset(resolvedPageNo, SHOP_MYPAGE_WISH_PAGE_SIZE);
		// 위시리스트 상품 목록을 조회합니다.
		List<ShopMypageWishGoodsItemVO> goodsList = goodsMapper.getShopMypageWishGoodsList(custNo, offset, SHOP_MYPAGE_WISH_PAGE_SIZE);
		// 상품 이미지 URL을 세팅합니다.
		applyShopMypageWishGoodsImageUrls(goodsList);

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
		int resolvedRequestedOwnedPageNo = resolveRequestedPageNo(requestedOwnedPageNo);
		int resolvedRequestedDownloadablePageNo = resolveRequestedPageNo(requestedDownloadablePageNo);

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
		int resolvedRequestedPageNo = resolveRequestedPageNo(requestedPageNo);
		ShopMypageOrderDateRange orderDateRange = resolveShopMypageOrderDateRange(requestedStartDate, requestedEndDate);

		// 주문번호 기준 전체 건수와 전체 페이지 수를 계산합니다.
		int orderCount = goodsMapper.countShopMypageOrderGroup(
			custNo,
			orderDateRange.getStartDateTime(),
			orderDateRange.getEndExclusiveDateTime()
		);
		int totalPageCount = calculateTotalPageCount(orderCount, SHOP_MYPAGE_ORDER_PAGE_SIZE);
		int resolvedPageNo = resolvePageNoWithinRange(resolvedRequestedPageNo, totalPageCount);
		int offset = calculateOffset(resolvedPageNo, SHOP_MYPAGE_ORDER_PAGE_SIZE);

		// 현재 페이지의 주문번호 목록과 상태 요약을 조회합니다.
		List<ShopMypageOrderGroupVO> orderList = goodsMapper.getShopMypageOrderGroupList(
			custNo,
			orderDateRange.getStartDateTime(),
			orderDateRange.getEndExclusiveDateTime(),
			offset,
			SHOP_MYPAGE_ORDER_PAGE_SIZE
		);
		ShopMypageOrderStatusSummaryVO statusSummary = goodsMapper.getShopMypageOrderStatusSummary(
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
		ShopMypageOrderGroupVO orderGroup = getShopMypageOrderGroupWithDetail(custNo, resolvedOrdNo);
		validateShopMypageOrderCancelAccess(orderGroup, ordDtlNo);
		ShopMypageOrderAmountSummaryVO amountSummary = buildShopMypageOrderAmountSummary(custNo, orderGroup);
		List<ShopMypageOrderCancelReasonVO> reasonList = normalizeShopMypageOrderCancelReasonList(
			goodsMapper.getShopMypageOrderCancelReasonList()
		);
		ShopCartSiteInfoVO siteInfo = resolveShopCartSiteInfo();

		// 주문취소 신청 화면 응답 객체를 구성합니다.
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
		int resolvedRequestedPageNo = resolveRequestedPageNo(requestedPageNo);
		ShopMypageOrderDateRange dateRange = resolveShopMypageOrderDateRange(requestedStartDate, requestedEndDate);

		// 취소 클레임 전체 건수와 전체 페이지 수를 계산합니다.
		int cancelCount = goodsMapper.countShopMypageCancelHistory(
			custNo,
			dateRange.getStartDate(),
			dateRange.getEndDate()
		);
		int totalPageCount = calculateTotalPageCount(cancelCount, SHOP_MYPAGE_CANCEL_PAGE_SIZE);
		int resolvedPageNo = resolvePageNoWithinRange(resolvedRequestedPageNo, totalPageCount);
		int offset = calculateOffset(resolvedPageNo, SHOP_MYPAGE_CANCEL_PAGE_SIZE);

		// 현재 페이지의 취소 클레임 목록을 조회합니다.
		List<ShopMypageCancelHistoryVO> cancelList = goodsMapper.getShopMypageCancelHistoryList(
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
			List<ShopMypageCancelHistoryDetailVO> detailList = goodsMapper.getShopMypageCancelHistoryDetailList(clmNoList);
			Map<String, List<ShopMypageCancelHistoryDetailVO>> detailByClmNo = detailList.stream()
				.filter(d -> d != null && d.getClmNo() != null)
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

	// 쇼핑몰 마이페이지 취소상세 단건을 클레임번호로 조회합니다.
	public ShopMypageCancelHistoryVO getShopMypageCancelHistoryDetail(Long custNo, String clmNo) {
		// 고객번호가 없으면 로그인 예외를 반환합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}
		// 클레임번호가 없으면 요청값 예외를 반환합니다.
		if (clmNo == null || clmNo.isBlank()) {
			throw new IllegalArgumentException("클레임번호가 필요합니다.");
		}

		// 클레임 단건을 조회합니다.
		ShopMypageCancelHistoryVO cancelItem = goodsMapper.getShopMypageCancelHistoryItemByClmNo(custNo, clmNo);
		if (cancelItem == null) {
			return null;
		}

		// 취소 상품 상세 목록을 조회해 클레임에 매핑합니다.
		List<ShopMypageCancelHistoryDetailVO> detailList = goodsMapper.getShopMypageCancelHistoryDetailList(
			java.util.Collections.singletonList(clmNo)
		);
		cancelItem.setDetailList(detailList != null ? detailList : java.util.Collections.emptyList());
		return cancelItem;
	}

	// 쇼핑몰 마이페이지 포인트 내역 페이지 데이터를 조회합니다.
	public ShopMypagePointPageVO getShopMypagePointPage(Long custNo, Integer requestedPageNo) {
		// 고객번호가 없으면 로그인 예외를 반환합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}

		// 요청 페이지 번호를 1 이상으로 보정합니다.
		int resolvedRequestedPageNo = resolveRequestedPageNo(requestedPageNo);
		// 포인트 내역 전체 건수를 조회합니다.
		Integer pointCountResult = goodsMapper.getShopMypagePointItemCount(custNo);
		int pointCount = pointCountResult == null ? 0 : pointCountResult;
		// 전체 페이지 수를 계산합니다.
		int totalPageCount = calculateTotalPageCount(pointCount, SHOP_MYPAGE_POINT_PAGE_SIZE);
		// 범위를 초과한 페이지 번호를 마지막 페이지로 보정합니다.
		int resolvedPageNo = totalPageCount == 0 ? 1 : Math.min(resolvedRequestedPageNo, totalPageCount);
		// 페이지 조회 오프셋을 계산합니다.
		int offset = calculateOffset(resolvedPageNo, SHOP_MYPAGE_POINT_PAGE_SIZE);

		// 포인트 내역 목록을 조회합니다.
		List<ShopMypagePointItemVO> pointList = goodsMapper.getShopMypagePointItemList(custNo, SHOP_MYPAGE_POINT_PAGE_SIZE, offset);
		// 사용 가능 포인트 합계를 조회합니다.
		Integer availablePointAmt = goodsMapper.getShopAvailablePointAmt(custNo);
		// 7일 이내 만료 예정 포인트 합계를 조회합니다.
		Integer expiringPointAmt = goodsMapper.getShopMypageExpiringPointAmt(custNo);

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
		ShopMypageOrderGroupVO orderGroup = getShopMypageOrderGroupWithDetail(custNo, ordNo);
		ShopOrderCancelOrderBaseVO orderBase = resolveShopOrderCancelOrderBase(custNo, ordNo);
		ShopCartSiteInfoVO siteInfo = resolveShopCartSiteInfo();
		List<ShopMypageOrderCancelReasonVO> reasonList = normalizeShopMypageOrderCancelReasonList(
			goodsMapper.getShopMypageOrderCancelReasonList()
		);

		// 주문취소 요청을 현재 주문 기준으로 정규화하고 서버 금액을 다시 계산합니다.
		validateShopOrderCancelReason(param, reasonList);
		Map<Integer, Integer> cancelQtyMap = resolveShopOrderCancelQtyMap(param.getCancelItemList());
		ShopOrderCancelComputation cancelComputation = buildShopOrderCancelComputation(orderGroup, orderBase, siteInfo, cancelQtyMap);
		validateShopOrderCancelPreviewAmount(param.getPreviewAmount(), cancelComputation.getPreviewAmount());

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
				cancelQtyMap,
				cancelComputation
			));
		} catch (TossPaymentClientException exception) {
			// PG 취소 실패 시 환불 결제 row만 실패 상태로 남기고 주문 변경은 롤백합니다.
			handleShopOrderCancelPaymentFailure(refundPaymentSavePO.getPayNo(), exception, custNo);
			throw new IllegalArgumentException(resolveShopOrderCancelPgErrorMessage(exception));
		}
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
		int issuedCount = shopAuthService.issueShopCustomerCoupon(custNo, cpnNo, 1);
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
			int issuedCount = shopAuthService.issueShopCustomerCoupon(custNo, downloadableCoupon.getCpnNo(), 1);
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

	// 요청 페이지 번호를 1 이상 값으로 보정합니다.
	private int resolveRequestedPageNo(Integer requestedPageNo) {
		if (requestedPageNo == null || requestedPageNo < 1) {
			return 1;
		}
		return requestedPageNo;
	}

	// 전체 페이지 수 범위 안으로 현재 페이지 번호를 보정합니다.
	private int resolvePageNoWithinRange(int requestedPageNo, int totalPageCount) {
		if (totalPageCount <= 0) {
			return 1;
		}
		return Math.min(Math.max(requestedPageNo, 1), totalPageCount);
	}

	// 전체 건수와 페이지 크기를 기준으로 전체 페이지 수를 계산합니다.
	private int calculateTotalPageCount(int goodsCount, int pageSize) {
		if (goodsCount <= 0 || pageSize <= 0) {
			return 0;
		}
		return (goodsCount + pageSize - 1) / pageSize;
	}

	// 현재 페이지와 페이지 크기를 기준으로 조회 오프셋을 계산합니다.
	private int calculateOffset(int pageNo, int pageSize) {
		if (pageNo < 1 || pageSize <= 0) {
			return 0;
		}
		return (pageNo - 1) * pageSize;
	}

	// 마이페이지 주문내역 조회 기간을 기본값 포함 유효한 기간으로 보정합니다.
	private ShopMypageOrderDateRange resolveShopMypageOrderDateRange(String requestedStartDate, String requestedEndDate) {
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
		ShopMypageOrderDateRange result = new ShopMypageOrderDateRange();
		result.setStartDate(startDate.toString());
		result.setEndDate(endDate.toString());
		result.setStartDateTime(startDate.atStartOfDay().format(SHOP_MYPAGE_ORDER_DATE_TIME_FORMATTER));
		result.setEndExclusiveDateTime(endDate.plusDays(1L).atStartOfDay().format(SHOP_MYPAGE_ORDER_DATE_TIME_FORMATTER));
		return result;
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
	private ShopMypageOrderGroupVO getShopMypageOrderGroupWithDetail(Long custNo, String ordNo) {
		// 로그인 고객의 주문번호 1건을 조회합니다.
		ShopMypageOrderGroupVO orderGroup = goodsMapper.getShopMypageOrderGroup(custNo, ordNo);
		if (orderGroup == null) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_NOT_FOUND_MESSAGE);
		}

		// 주문상세 목록을 기존 묶음 로직으로 연결하고 노출 가능한 1건을 반환합니다.
		List<ShopMypageOrderGroupVO> resolvedOrderList = attachShopMypageOrderDetailList(List.of(orderGroup));
		if (resolvedOrderList.isEmpty()) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_NOT_FOUND_MESSAGE);
		}
		return resolvedOrderList.get(0);
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
		List<ShopMypageOrderDetailItemVO> detailList = goodsMapper.getShopMypageOrderDetailList(new ArrayList<>(orderGroupMap.keySet()));
		applyShopMypageOrderDetailImageUrls(detailList);

		// 주문번호별 detailList에 주문상세 행을 순서대로 연결합니다.
		for (ShopMypageOrderDetailItemVO detailItem : detailList == null ? List.<ShopMypageOrderDetailItemVO>of() : detailList) {
			if (detailItem == null || isBlank(detailItem.getOrdNo())) {
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
	private ShopMypageOrderAmountSummaryVO buildShopMypageOrderAmountSummary(Long custNo, ShopMypageOrderGroupVO orderGroup) {
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
	private ShopOrderCancelOrderBaseVO resolveShopOrderCancelOrderBase(Long custNo, String ordNo) {
		// 고객 주문번호 기준 주문 마스터가 없으면 주문 미존재 예외를 반환합니다.
		ShopOrderCancelOrderBaseVO orderBase = goodsMapper.getShopOrderCancelOrderBase(custNo, ordNo);
		if (orderBase == null) {
			throw new IllegalArgumentException(SHOP_MYPAGE_ORDER_NOT_FOUND_MESSAGE);
		}
		return orderBase;
	}

	// 현재 남아 있는 주문수량 기준으로 주문 금액 요약을 계산합니다.
	private ShopMypageOrderAmountSummaryVO buildShopOrderRemainingAmountSummary(
		ShopMypageOrderGroupVO orderGroup,
		ShopOrderCancelOrderBaseVO orderBase,
		ShopCartSiteInfoVO siteInfo
	) {
		// 주문상세별 남은 수량과 남은 쿠폰/포인트 배분 금액을 누적합니다.
		ShopMypageOrderAmountSummaryVO amountSummary = normalizeShopMypageOrderAmountSummary(new ShopMypageOrderAmountSummaryVO());
		long currentOrderAmt = 0L;
		for (ShopMypageOrderDetailItemVO detailItem : orderGroup == null ? List.<ShopMypageOrderDetailItemVO>of() : orderGroup.getDetailList()) {
			int originalQty = resolveShopOrderOriginalQty(detailItem);
			int remainingQty = resolveShopOrderRemainingQty(detailItem);
			if (originalQty < 1 || remainingQty < 1) {
				continue;
			}

			// 현재 남은 주문상품 금액과 남은 할인 배분 금액을 더합니다.
			long rowSupplyAmt = (long) normalizeNonNegativeNumber(detailItem.getSupplyAmt()) * remainingQty;
			long rowOrderAmt = (long) resolveShopOrderUnitOrderAmt(detailItem) * remainingQty;
			currentOrderAmt += rowOrderAmt;
			amountSummary.setTotalSupplyAmt(amountSummary.getTotalSupplyAmt() + rowSupplyAmt);
			amountSummary.setTotalOrderAmt(amountSummary.getTotalOrderAmt() + rowOrderAmt);
			amountSummary.setTotalGoodsCouponDiscountAmt(
				amountSummary.getTotalGoodsCouponDiscountAmt() + resolveShopOrderRemainingAllocatedAmt(detailItem.getGoodsCouponDiscountAmt(), originalQty, remainingQty)
			);
			amountSummary.setTotalCartCouponDiscountAmt(
				amountSummary.getTotalCartCouponDiscountAmt() + resolveShopOrderRemainingAllocatedAmt(detailItem.getCartCouponDiscountAmt(), originalQty, remainingQty)
			);
			amountSummary.setTotalPointUseAmt(
				amountSummary.getTotalPointUseAmt() + resolveShopOrderRemainingAllocatedAmt(detailItem.getPointUseAmt(), originalQty, remainingQty)
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

	// 주문취소 사유 코드와 직접입력값을 검증합니다.
	private void validateShopOrderCancelReason(ShopOrderCancelPO param, List<ShopMypageOrderCancelReasonVO> reasonList) {
		// 선택한 취소 사유 코드가 비어 있거나 현재 사용 가능한 사유가 아니면 예외를 반환합니다.
		String reasonCd = trimToNull(param == null ? null : param.getReasonCd());
		if (reasonCd == null) {
			throw new IllegalArgumentException("주문 취소 사유를 선택해주세요.");
		}
		ShopMypageOrderCancelReasonVO matchedReason = null;
		for (ShopMypageOrderCancelReasonVO reasonItem : reasonList == null ? List.<ShopMypageOrderCancelReasonVO>of() : reasonList) {
			if (reasonCd.equals(trimToNull(reasonItem.getCd()))) {
				matchedReason = reasonItem;
				break;
			}
		}
		if (matchedReason == null) {
			throw new IllegalArgumentException("주문 취소 사유를 선택해주세요.");
		}

		// 기타 사유는 직접입력값을 필수로 확인합니다.
		if ("C_03".equals(matchedReason.getCd()) && trimToNull(param.getReasonDetail()) == null) {
			throw new IllegalArgumentException("기타 사유를 입력해주세요.");
		}
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
			int originalQty = resolveShopOrderOriginalQty(detailItem);
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
			boolean paymentDoneCancelable = SHOP_ORDER_DTL_STAT_DONE.equals(detailItem.getOrdDtlStatCd());
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

			// 선택 행의 취소 수량 기준 상품/쿠폰/포인트 환급 금액을 누적합니다.
			selectedQtyCount += requestedCancelQty;
			if (remainingQty - requestedCancelQty < 1) {
				fullyCanceledRowCount += 1;
			}
			remainingOrderAmtAfterCancel += (long) resolveShopOrderUnitOrderAmt(detailItem) * (remainingQty - requestedCancelQty);
			accumulateShopOrderCancelPreviewAmount(previewSummary, detailItem, requestedCancelQty);
			selectedItemList.add(buildShopOrderCancelSelectedItem(detailItem, requestedCancelQty));
		}

		// 무통장입금은 전체취소만, 결제완료는 부분취소/전체취소를 허용합니다.
		if (selectedItemList.isEmpty() || selectedQtyCount < 1) {
			throw new IllegalArgumentException("취소할 상품을 선택해주세요.");
		}
		if (hasWaitingDepositSelection && hasPaymentDoneSelection) {
			throw new IllegalArgumentException("주문상품 정보를 확인해주세요.");
		}
		if (hasWaitingDepositSelection) {
			validateShopOrderCancelFullOnly(orderGroup, cancelQtyMap);
		}

		// 배송비 환급/차감과 취소 예정 금액, PG 현금 환불액을 현재 주문 금액 기준으로 계산합니다.
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
		// 주문상세 원수량과 이미 취소된 수량 기준으로 이번 취소분의 배분 금액을 계산합니다.
		int originalQty = resolveShopOrderOriginalQty(detailItem);
		int canceledBeforeQty = resolveShopOrderCanceledQty(detailItem);
		long supplyAmt = (long) normalizeNonNegativeNumber(detailItem.getSupplyAmt()) * cancelQty;
		long orderAmt = (long) resolveShopOrderUnitOrderAmt(detailItem) * cancelQty;
		long goodsCouponDiscountAmt = resolveShopOrderIncrementAllocatedAmt(detailItem.getGoodsCouponDiscountAmt(), originalQty, canceledBeforeQty, cancelQty);
		long cartCouponDiscountAmt = resolveShopOrderIncrementAllocatedAmt(detailItem.getCartCouponDiscountAmt(), originalQty, canceledBeforeQty, cancelQty);
		long pointRefundAmt = resolveShopOrderIncrementAllocatedAmt(detailItem.getPointUseAmt(), originalQty, canceledBeforeQty, cancelQty);

		// 공급가/상품가/상품할인과 상품쿠폰/장바구니쿠폰/포인트 환급 누계를 반영합니다.
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
		// 현재 주문의 승인/입금대기 원결제가 없으면 취소를 진행할 수 없습니다.
		ShopOrderPaymentVO payment = goodsMapper.getShopOrderPaymentForCancel(ordNo);
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
		refundSnapshot.put("reasonCd", trimToNull(param == null ? null : param.getReasonCd()));
		refundSnapshot.put("reasonDetail", trimToNull(param == null ? null : param.getReasonDetail()));
		refundSnapshot.put("cancelItemList", param == null ? List.of() : param.getCancelItemList());
		refundSnapshot.put("previewAmount", cancelComputation == null ? null : cancelComputation.getPreviewAmount());
		refundSnapshot.put("refundedCashAmt", cancelComputation == null ? 0L : cancelComputation.getRefundedCashAmt());

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
			refundPaymentSavePO.setPayAmt(resolveRefundPaymentAmt(cancelComputation == null ? null : cancelComputation.getRefundedCashAmt()));
			refundPaymentSavePO.setDeviceGbCd(orderBase == null ? "PC" : firstNonBlank(trimToNull(orderBase.getDeviceGbCd()), "PC"));
			refundPaymentSavePO.setReqRawJson(writeShopOrderJson(refundSnapshot));
			refundPaymentSavePO.setRegNo(custNo);
			refundPaymentSavePO.setUdtNo(custNo);
			goodsMapper.insertShopPayment(refundPaymentSavePO);
			if (refundPaymentSavePO.getPayNo() == null || refundPaymentSavePO.getPayNo() < 1L) {
				throw new IllegalStateException("환불 결제 준비에 실패했습니다.");
			}
			return refundPaymentSavePO;
		});
	}

	// 메인 주문취소 트랜잭션을 실행합니다.
	private <T> T executeInShopOrderTransaction(Supplier<T> action) {
		// 기본 전파속성의 트랜잭션 템플릿으로 주문취소 반영을 실행합니다.
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		return transactionTemplate.execute(status -> action.get());
	}

	// 별도 커밋이 필요한 주문취소 보조 트랜잭션을 실행합니다.
	private <T> T executeInNewShopOrderTransaction(Supplier<T> action) {
		// 환불 PAYMENT 선등록/실패 반영처럼 독립 커밋이 필요한 작업을 REQUIRES_NEW로 실행합니다.
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return transactionTemplate.execute(status -> action.get());
	}

	// 주문취소 성공 시 주문/클레임/포인트/재고/PAYMENT를 함께 반영합니다.
	private ShopOrderCancelResultVO applyShopOrderCancelSuccess(
		ShopOrderCancelPO param,
		Long custNo,
		ShopMypageOrderGroupVO orderGroup,
		ShopOrderCancelOrderBaseVO orderBase,
		ShopOrderPaymentVO originalPayment,
		ShopOrderPaymentSavePO refundPaymentSavePO,
		String clmNo,
		Map<Integer, Integer> cancelQtyMap,
		ShopOrderCancelComputation cancelComputation
	) {
		// 주문변경 마스터/상세와 주문상세 취소 수량을 먼저 반영합니다.
		String cancelDt = LocalDateTime.now().format(SHOP_MYPAGE_ORDER_DATE_TIME_FORMATTER);
		goodsMapper.insertShopOrderChangeBase(buildShopOrderChangeBaseSavePO(clmNo, orderBase, cancelComputation, cancelDt, custNo));
		for (ShopOrderCancelSelectedItem selectedItem : cancelComputation.getSelectedItemList()) {
			goodsMapper.insertShopOrderChangeDetail(buildShopOrderChangeDetailSavePO(clmNo, selectedItem, param, custNo));
			int updatedCount = goodsMapper.updateShopOrderDetailCancelQuantity(
				orderBase.getOrdNo(),
				selectedItem.getDetailItem().getOrdDtlNo(),
				selectedItem.getCancelQty(),
				selectedItem.getNextOrdDtlStatCd(),
				custNo
			);
			if (updatedCount < 1) {
				throw new IllegalArgumentException("주문상품 정보를 확인해주세요.");
			}
		}

		// 취소 수량만큼 재고와 포인트를 복구하고, 전체취소면 주문 쿠폰 적용 정보도 함께 원복합니다.
		restoreShopOrderCancelSelectedStock(cancelComputation.getSelectedItemList(), custNo);
		restoreShopOrderPointByAmount(custNo, orderBase.getOrdNo(), cancelComputation.getRestoredPointAmt());
		resetShopOrderCancelCouponDiscount(orderBase.getOrdNo(), cancelComputation, custNo);
		restoreShopOrderCancelCouponUse(custNo, orderBase.getOrdNo(), cancelComputation);

		// PG 취소 성공 응답을 저장하고, 전체취소면 주문 마스터도 완료 상태로 변경합니다.
		ShopOrderCancelPgResult cancelPgResult = cancelShopOrderPaymentWithPg(originalPayment, param, cancelComputation);
		goodsMapper.updateShopPaymentCancelSuccess(
			refundPaymentSavePO.getPayNo(),
			SHOP_ORDER_PAY_STAT_CANCEL,
			cancelPgResult.getCanceledAmount(),
			cancelPgResult.getTradeNo(),
			cancelPgResult.getRspCode(),
			cancelPgResult.getRspMsg(),
			cancelPgResult.getRawResponse(),
			cancelPgResult.getApprovedDt(),
			custNo
		);
		if (cancelComputation.isFullCancel()) {
			goodsMapper.updateShopOrderBaseStatus(orderBase.getOrdNo(), SHOP_ORDER_STAT_CANCEL, custNo);
		}

		// 주문취소 완료 응답 객체를 구성합니다.
		ShopOrderCancelResultVO result = new ShopOrderCancelResultVO();
		result.setClmNo(clmNo);
		result.setOrdNo(orderBase.getOrdNo());
		result.setRefundPayNo(refundPaymentSavePO.getPayNo());
		result.setPayStatCd(SHOP_ORDER_PAY_STAT_CANCEL);
		result.setRefundedCashAmt(cancelComputation.getRefundedCashAmt());
		result.setRestoredPointAmt(cancelComputation.getRestoredPointAmt());
		return result;
	}

	// 주문취소 완료 후 전체취소 여부에 맞춰 주문 쿠폰 할인 정보를 초기화합니다.
	private void resetShopOrderCancelCouponDiscount(String ordNo, ShopOrderCancelComputation cancelComputation, Long auditNo) {
		// 전체취소가 아니거나 주문번호가 비어 있으면 주문 쿠폰 할인 정보는 유지합니다.
		if (auditNo == null || auditNo < 1L || isBlank(ordNo) || cancelComputation == null || !cancelComputation.isFullCancel()) {
			return;
		}

		// 전체취소면 주문상세와 주문마스터의 쿠폰 적용 정보와 할인금액을 함께 초기화합니다.
		goodsMapper.resetShopOrderDetailCouponDiscount(ordNo, auditNo);
		goodsMapper.resetShopOrderBaseDeliveryCouponDiscount(ordNo, auditNo);
	}

	// 주문취소 완료 후 전체취소 여부에 맞춰 고객쿠폰 사용 상태를 원복합니다.
	private void restoreShopOrderCancelCouponUse(
		Long custNo,
		String ordNo,
		ShopOrderCancelComputation cancelComputation
	) {
		// 전체취소가 아니거나 주문번호가 비어 있으면 고객쿠폰 원복을 수행하지 않습니다.
		if (custNo == null || custNo < 1L || isBlank(ordNo) || cancelComputation == null || !cancelComputation.isFullCancel()) {
			return;
		}

		// 전체취소면 결제상태와 관계없이 주문번호 기준 사용 쿠폰 전체를 원복합니다.
		goodsMapper.restoreShopCustomerCouponUse(custNo, ordNo, custNo);
	}

	// 주문취소 대상 상품 수량만큼 재고를 복구합니다.
	private void restoreShopOrderCancelSelectedStock(List<ShopOrderCancelSelectedItem> selectedItemList, Long auditNo) {
		// 동일 상품/사이즈는 취소 수량을 합산해 재고를 복구합니다.
		Map<String, ShopOrderRestoreCartItemVO> stockItemMap = new LinkedHashMap<>();
		for (ShopOrderCancelSelectedItem selectedItem : selectedItemList == null ? List.<ShopOrderCancelSelectedItem>of() : selectedItemList) {
			if (selectedItem == null || selectedItem.getDetailItem() == null || isBlank(selectedItem.getDetailItem().getGoodsId()) || isBlank(selectedItem.getDetailItem().getSizeId())) {
				continue;
			}
			String stockKey = selectedItem.getDetailItem().getGoodsId().trim() + "|" + selectedItem.getDetailItem().getSizeId().trim();
			ShopOrderRestoreCartItemVO aggregateItem = stockItemMap.get(stockKey);
			if (aggregateItem == null) {
				aggregateItem = new ShopOrderRestoreCartItemVO();
				aggregateItem.setGoodsId(selectedItem.getDetailItem().getGoodsId().trim());
				aggregateItem.setSizeId(selectedItem.getDetailItem().getSizeId().trim());
				aggregateItem.setOrdQty(0);
				stockItemMap.put(stockKey, aggregateItem);
			}
			aggregateItem.setOrdQty(normalizeNonNegativeNumber(aggregateItem.getOrdQty()) + selectedItem.getCancelQty());
		}
		for (ShopOrderRestoreCartItemVO stockItem : stockItemMap.values()) {
			if (stockItem == null || normalizeNonNegativeNumber(stockItem.getOrdQty()) < 1) {
				continue;
			}
			goodsMapper.restoreShopGoodsSizeStock(stockItem.getGoodsId(), stockItem.getSizeId(), stockItem.getOrdQty(), auditNo);
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
		for (ShopOrderPointDetailVO pointDetail : goodsMapper.getShopOrderPointDetailBalanceList(ordNo)) {
			if (pointDetail == null || pointDetail.getPntNo() == null || remainingRestoreAmt < 1) {
				continue;
			}
			int restorableAmt = normalizeNonNegativeNumber(pointDetail.getPntAmt());
			if (restorableAmt < 1) {
				continue;
			}
			int appliedRestoreAmt = Math.min(restorableAmt, remainingRestoreAmt);
			goodsMapper.restoreShopCustomerPointUseAmt(pointDetail.getPntNo(), appliedRestoreAmt, custNo);
			ShopOrderPointDetailSavePO restoreDetail = new ShopOrderPointDetailSavePO();
			restoreDetail.setPntNo(pointDetail.getPntNo());
			restoreDetail.setPntAmt(appliedRestoreAmt);
			restoreDetail.setOrdNo(ordNo);
			restoreDetail.setBigo(SHOP_ORDER_POINT_RESTORE_MEMO);
			restoreDetail.setRegNo(custNo);
			goodsMapper.insertShopOrderPointDetail(restoreDetail);
			remainingRestoreAmt -= appliedRestoreAmt;
		}
		if (remainingRestoreAmt > 0) {
			throw new IllegalStateException("포인트 복구 처리에 실패했습니다.");
		}
	}

	// 주문취소용 PG 취소 API를 호출하고 성공 응답을 해석합니다.
	private ShopOrderCancelPgResult cancelShopOrderPaymentWithPg(
		ShopOrderPaymentVO originalPayment,
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
				: cancelComputation.getRefundedCashAmt() > 0L
					? cancelComputation.getRefundedCashAmt()
					: null;
		String rawResponse = tossPaymentsClient.cancelPayment(
			originalPayment.getTossPaymentKey().trim(),
			resolveShopOrderCancelPgReason(param),
			cancelAmount
		);
		JsonNode responseNode = readShopOrderJsonNode(rawResponse);
		String paymentStatus = firstNonBlank(resolveJsonText(responseNode, "status"), "");
		if (!"CANCELED".equals(paymentStatus) && !"PARTIAL_CANCELED".equals(paymentStatus)) {
			throw new IllegalArgumentException("주문취소 처리에 실패했습니다.");
		}

		// Toss 취소 응답에서 거래키, 취소일시, 실제 취소금액을 추출합니다.
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

	// 주문취소 PG 실패 시 환불 PAYMENT row만 실패 상태로 남깁니다.
	private void handleShopOrderCancelPaymentFailure(Long refundPayNo, TossPaymentClientException exception, Long custNo) {
		// 환불 결제번호가 없으면 별도 실패 반영을 진행하지 않습니다.
		if (refundPayNo == null || refundPayNo < 1L) {
			return;
		}

		// Toss 오류 응답을 저장해 실패 원인을 확인할 수 있게 합니다.
		executeInNewShopOrderTransaction(() -> {
			JsonNode errorNode = readShopOrderJsonNode(exception.getResponseBody());
			goodsMapper.updateShopPaymentCancelFailure(
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
		// Toss 오류 메시지가 있으면 우선 사용하고, 없으면 기본 문구를 반환합니다.
		JsonNode errorNode = readShopOrderJsonNode(exception == null ? null : exception.getResponseBody());
		return firstNonBlank(resolveJsonText(errorNode, "message"), "주문취소 처리에 실패했습니다.");
	}

	// PG 취소 사유 문자열을 Toss 전송용 텍스트로 구성합니다.
	private String resolveShopOrderCancelPgReason(ShopOrderCancelPO param) {
		// 사유 코드와 상세 입력을 조합해 최대한 읽기 쉬운 문구로 정리합니다.
		String reasonCd = trimToNull(param == null ? null : param.getReasonCd());
		String reasonDetail = trimToNull(param == null ? null : param.getReasonDetail());
		if (reasonCd == null) {
			return "주문 취소";
		}
		return reasonDetail == null ? reasonCd : reasonCd + " - " + reasonDetail;
	}

	// 주문변경 마스터 저장 파라미터를 생성합니다.
	private ShopOrderChangeBaseSavePO buildShopOrderChangeBaseSavePO(
		String clmNo,
		ShopOrderCancelOrderBaseVO orderBase,
		ShopOrderCancelComputation cancelComputation,
		String cancelDt,
		Long auditNo
	) {
		// 취소 즉시완료 기준 변경구분/상태/배송비 조정 금액을 채웁니다.
		ShopOrderChangeBaseSavePO result = new ShopOrderChangeBaseSavePO();
		result.setClmNo(clmNo);
		result.setOrdNo(orderBase == null ? null : orderBase.getOrdNo());
		result.setChgGbCd(SHOP_ORDER_CHANGE_GB_CANCEL);
		result.setChgDt(cancelDt);
		result.setChgCompleteDt(cancelDt);
		result.setChgStatCd(SHOP_ORDER_CHANGE_STAT_PROGRESS);
		result.setPayDelvAmt((int) Math.max(Math.min(cancelComputation == null ? 0L : cancelComputation.getShippingAdjustmentAmt(), Integer.MAX_VALUE), Integer.MIN_VALUE));
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
		ShopOrderChangeDetailSavePO result = new ShopOrderChangeDetailSavePO();
		result.setClmNo(clmNo);
		result.setOrdNo(selectedItem == null || selectedItem.getDetailItem() == null ? null : selectedItem.getDetailItem().getOrdNo());
		result.setOrdDtlNo(selectedItem == null || selectedItem.getDetailItem() == null ? null : selectedItem.getDetailItem().getOrdDtlNo());
		result.setChgDtlGbCd(SHOP_ORDER_CHANGE_DTL_GB_CANCEL);
		result.setChgDtlStatCd(SHOP_ORDER_CHANGE_DTL_STAT_DONE);
		result.setChgReasonCd(trimToNull(param == null ? null : param.getReasonCd()));
		result.setChgReasonDtl(trimToNull(param == null ? null : param.getReasonDetail()));
		result.setGoodsId(selectedItem == null || selectedItem.getDetailItem() == null ? null : selectedItem.getDetailItem().getGoodsId());
		result.setSizeId(selectedItem == null || selectedItem.getDetailItem() == null ? null : selectedItem.getDetailItem().getSizeId());
		result.setQty(selectedItem == null ? null : selectedItem.getCancelQty());
		result.setAddAmt(selectedItem == null || selectedItem.getDetailItem() == null ? null : normalizeNonNegativeNumber(selectedItem.getDetailItem().getAddAmt()));
		result.setChangeOrdDtlNo(null);
		result.setRegNo(auditNo);
		result.setUdtNo(auditNo);
		return result;
	}

	// 주문상세 1건과 취소수량으로 취소 반영 대상 행 정보를 생성합니다.
	private ShopOrderCancelSelectedItem buildShopOrderCancelSelectedItem(ShopMypageOrderDetailItemVO detailItem, int cancelQty) {
		// 취소 후 남은 수량에 따라 다음 주문상세 상태를 계산합니다.
		int remainingAfterCancelQty = Math.max(resolveShopOrderRemainingQty(detailItem) - Math.max(cancelQty, 0), 0);
		String nextOrdDtlStatCd = remainingAfterCancelQty < 1 ? SHOP_ORDER_DTL_STAT_CANCEL : detailItem.getOrdDtlStatCd();
		return new ShopOrderCancelSelectedItem(detailItem, cancelQty, remainingAfterCancelQty, nextOrdDtlStatCd);
	}

	// 주문상세의 원주문 수량을 안전한 정수로 반환합니다.
	private int resolveShopOrderOriginalQty(ShopMypageOrderDetailItemVO detailItem) {
		// 원주문 수량이 없거나 음수면 0으로 보정합니다.
		return normalizeNonNegativeNumber(detailItem == null ? null : detailItem.getOrdQty());
	}

	// 주문상세의 현재 남은 수량을 원주문 수량 범위 안에서 반환합니다.
	private int resolveShopOrderRemainingQty(ShopMypageOrderDetailItemVO detailItem) {
		// 현재 남은 수량은 원주문 수량을 넘지 않도록 보정합니다.
		int originalQty = resolveShopOrderOriginalQty(detailItem);
		int remainingQty = normalizeNonNegativeNumber(detailItem == null ? null : detailItem.getCancelableQty());
		return Math.min(originalQty, remainingQty);
	}

	// 주문상세의 이미 취소된 누적 수량을 반환합니다.
	private int resolveShopOrderCanceledQty(ShopMypageOrderDetailItemVO detailItem) {
		// 원주문 수량에서 현재 남은 수량을 빼 누적 취소 수량을 계산합니다.
		return Math.max(resolveShopOrderOriginalQty(detailItem) - resolveShopOrderRemainingQty(detailItem), 0);
	}

	// 주문상세의 상품 판매가 단가를 반환합니다.
	private int resolveShopOrderUnitOrderAmt(ShopMypageOrderDetailItemVO detailItem) {
		// 판매가와 추가금액을 더해 주문상세 1개당 주문금액을 계산합니다.
		return normalizeNonNegativeNumber(detailItem == null ? null : detailItem.getSaleAmt())
			+ normalizeNonNegativeNumber(detailItem == null ? null : detailItem.getAddAmt());
	}

	// 주문상세의 누적 배분 금액을 누적 수량 기준으로 계산합니다.
	private long resolveShopOrderCumulativeAllocatedAmt(Integer allocatedAmt, int originalQty, int cumulativeQty) {
		// 마지막 수량까지 모두 취소되면 남은 절삭 금액을 전부 배분합니다.
		long safeAllocatedAmt = normalizeNonNegativeNumber(allocatedAmt);
		int safeOriginalQty = Math.max(originalQty, 0);
		int safeCumulativeQty = Math.max(Math.min(cumulativeQty, safeOriginalQty), 0);
		if (safeAllocatedAmt < 1L || safeOriginalQty < 1 || safeCumulativeQty < 1) {
			return 0L;
		}
		if (safeCumulativeQty >= safeOriginalQty) {
			return safeAllocatedAmt;
		}
		return (safeAllocatedAmt * safeCumulativeQty) / safeOriginalQty;
	}

	// 주문상세의 이번 취소분 배분 금액을 계산합니다.
	private long resolveShopOrderIncrementAllocatedAmt(Integer allocatedAmt, int originalQty, int canceledBeforeQty, int cancelQty) {
		// 누적 취소 전/후 배분 차이만큼 이번 취소분 환급 금액을 계산합니다.
		long beforeAmt = resolveShopOrderCumulativeAllocatedAmt(allocatedAmt, originalQty, canceledBeforeQty);
		long afterAmt = resolveShopOrderCumulativeAllocatedAmt(allocatedAmt, originalQty, canceledBeforeQty + cancelQty);
		return Math.max(afterAmt - beforeAmt, 0L);
	}

	// 주문상세의 현재 남은 수량 기준 배분 금액을 계산합니다.
	private long resolveShopOrderRemainingAllocatedAmt(Integer allocatedAmt, int originalQty, int remainingQty) {
		// 전체 배분 금액에서 이미 취소된 누적 배분 금액을 제외해 현재 남은 금액을 계산합니다.
		long safeAllocatedAmt = normalizeNonNegativeNumber(allocatedAmt);
		long canceledAllocatedAmt = resolveShopOrderCumulativeAllocatedAmt(allocatedAmt, originalQty, Math.max(originalQty - remainingQty, 0));
		return Math.max(safeAllocatedAmt - canceledAllocatedAmt, 0L);
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

	// 주문취소 사유 코드 목록의 공백/null 값을 기본값으로 보정합니다.
	private List<ShopMypageOrderCancelReasonVO> normalizeShopMypageOrderCancelReasonList(
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
	private long resolveNonNegativeLong(Long value) {
		if (value == null || value < 0L) {
			return 0L;
		}
		return value;
	}

	// 환불 PAYMENT 저장금액을 음수 기준으로 변환합니다.
	private long resolveRefundPaymentAmt(Long refundedCashAmt) {
		// 환불 저장금액은 절댓값 기준으로 음수화합니다.
		long normalizedRefundedCashAmt = Math.abs(refundedCashAmt == null ? 0L : refundedCashAmt.longValue());
		return normalizedRefundedCashAmt * -1L;
	}

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
		applyShopGoodsImageUrlList(imageList);
		applyShopGoodsGroupItemImageUrlList(groupGoodsList);
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
		int issuedCount = shopAuthService.issueShopCustomerCoupon(custNo, cpnNo, 1);
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
	public int addShopGoodsCart(String goodsId, String sizeId, Integer qty, Long custNo, Integer exhibitionNo) {
		// 장바구니 등록 대상 상품/사이즈/수량을 검증하고 정규화합니다.
		ShopCartValidatedInput validatedInput = validateShopCartInput(goodsId, sizeId, qty, custNo);
		Integer validatedExhibitionNo = resolveValidatedShopCartExhibitionNo(validatedInput.getGoodsId(), exhibitionNo);

		// 기존 장바구니(C) 존재 여부에 따라 수량 가산 또는 신규 등록을 수행합니다.
		int existedCount = goodsMapper.countShopCart(custNo, validatedInput.getGoodsId(), validatedInput.getSizeId());
		if (existedCount > 0) {
			goodsMapper.addShopCartQty(custNo, validatedInput.getGoodsId(), validatedInput.getSizeId(), validatedInput.getQty(), validatedExhibitionNo, custNo);
		} else {
			goodsMapper.insertShopCart(createShopCartSavePO(SHOP_CART_GB_CART, custNo, validatedInput, validatedExhibitionNo));
		}

		// 저장 이후 장바구니 최종 수량을 조회해 반환합니다.
		Integer latestQty = goodsMapper.getShopCartQty(custNo, validatedInput.getGoodsId(), validatedInput.getSizeId());
		return latestQty == null ? validatedInput.getQty() : latestQty;
	}

	// 쇼핑몰 바로구매용 장바구니를 신규 등록하고 생성된 장바구니 번호를 반환합니다.
	@Transactional
	public Long addShopGoodsOrderNowCart(String goodsId, String sizeId, Integer qty, Long custNo, Integer exhibitionNo) {
		// 바로구매 등록 대상 상품/사이즈/수량을 검증하고 정규화합니다.
		ShopCartValidatedInput validatedInput = validateShopCartInput(goodsId, sizeId, qty, custNo);
		Integer validatedExhibitionNo = resolveValidatedShopCartExhibitionNo(validatedInput.getGoodsId(), exhibitionNo);
		ShopCartSavePO savePO = createShopCartSavePO(SHOP_CART_GB_ORDER, custNo, validatedInput, validatedExhibitionNo);

		// 바로구매(O) 행은 항상 신규 등록합니다.
		goodsMapper.insertShopCart(savePO);
		if (savePO.getCartId() == null || savePO.getCartId() < 1L) {
			throw new IllegalStateException("바로구매 장바구니 등록에 실패했습니다.");
		}
		return savePO.getCartId();
	}

	// 쇼핑몰 장바구니 페이지 데이터를 조회합니다.
	public ShopCartPageVO getShopCartPage(Long custNo) {
		// 로그인 고객번호 유효성을 확인합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}

		// 장바구니(C) 목록을 조회해 페이지 응답으로 구성합니다.
		return buildShopCartPage(goodsMapper.getShopCartItemList(custNo));
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
		int totalSaleAmt = calculateSelectedCartSaleAmt(discountContext.getEstimateRowList());
		int baseDeliveryFee = resolveCouponEstimateDeliveryFee(totalSaleAmt, discountContext.getSiteInfo());
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
			deviceGbCd
		);
		Map<String, Object> paymentSnapshot = buildShopOrderPaymentSnapshot(
			param.getFrom(),
			param.getGoodsId(),
			orderCartItemList,
			selectedAddress.getAddressNm(),
			discountQuote.getDiscountSelection(),
			normalizedPointUseAmt,
			orderName,
			finalPayAmt
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
		goodsMapper.insertShopPayment(paymentSavePO);
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
		ShopOrderPaymentVO payment = goodsMapper.getShopPaymentByPayNo(param.getPayNo());
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
			goodsMapper.updateShopPaymentFailure(
				payment.getPayNo(),
				SHOP_ORDER_PAY_STAT_FAIL,
				errorCode,
				errorMessage,
				exception.getResponseBody(),
				custNo
			);
			goodsMapper.updateShopOrderBaseStatus(param.getOrdNo().trim(), SHOP_ORDER_STAT_CANCEL, custNo);
			goodsMapper.updateShopOrderDetailStatus(param.getOrdNo().trim(), SHOP_ORDER_DTL_STAT_CANCEL, custNo);
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
			goodsMapper.updateShopPaymentWaitingDeposit(
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
			goodsMapper.updateShopOrderBaseStatusAndDates(
				param.getOrdNo().trim(),
				SHOP_ORDER_STAT_WAITING_DEPOSIT,
				approvedAt,
				null,
				custNo
			);
			goodsMapper.updateShopOrderDetailStatus(param.getOrdNo().trim(), SHOP_ORDER_DTL_STAT_WAITING_DEPOSIT, custNo);
			ShopOrderPaymentVO updatedPayment = goodsMapper.getShopPaymentByPayNo(payment.getPayNo());
			ShopOrderPaymentConfirmVO result = buildShopOrderPaymentConfirmResult(updatedPayment);
			result.setOrderName(orderName);
			return result;
		}

		if (!"DONE".equals(paymentStatus)) {
			throw new IllegalArgumentException(SHOP_ORDER_PAYMENT_CONFIRM_MESSAGE);
		}

		// 카드/퀵계좌이체 승인 성공 정보를 저장하고 후처리를 수행합니다.
		goodsMapper.updateShopPaymentSuccess(
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
		goodsMapper.updateShopOrderBaseStatusAndDates(
			param.getOrdNo().trim(),
			SHOP_ORDER_STAT_DONE,
			approvedAt,
			approvedAt,
			custNo
		);
		goodsMapper.updateShopOrderDetailStatus(param.getOrdNo().trim(), SHOP_ORDER_DTL_STAT_DONE, custNo);
		ShopOrderPaymentVO updatedPayment = goodsMapper.getShopPaymentByPayNo(payment.getPayNo());
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
		ShopOrderPaymentVO payment = goodsMapper.getShopPaymentByPayNo(param.getPayNo());
		if (payment == null || payment.getCustNo() == null || !custNo.equals(payment.getCustNo()) || !param.getOrdNo().trim().equals(payment.getOrdNo())) {
			throw new IllegalArgumentException(SHOP_ORDER_PAYMENT_INVALID_MESSAGE);
		}

		// 이미 성공 또는 입금대기 상태가 아니면 실패/취소 상태와 주문 취소 상태를 반영합니다.
		if (SHOP_ORDER_PAY_STAT_DONE.equals(payment.getPayStatCd()) || SHOP_ORDER_PAY_STAT_WAITING_DEPOSIT.equals(payment.getPayStatCd())) {
			return;
		}
		String payStatCd = isShopOrderCancelFailureCode(param.getCode()) ? SHOP_ORDER_PAY_STAT_CANCEL : SHOP_ORDER_PAY_STAT_FAIL;
		goodsMapper.updateShopPaymentFailure(
			payment.getPayNo(),
			payStatCd,
			trimToNull(param.getCode()),
			trimToNull(param.getMessage()),
			writeShopOrderJson(Map.of(
				"code", firstNonBlank(trimToNull(param.getCode()), ""),
				"message", firstNonBlank(trimToNull(param.getMessage()), "")
			)),
			custNo
		);
		goodsMapper.updateShopOrderBaseStatus(param.getOrdNo().trim(), SHOP_ORDER_STAT_CANCEL, custNo);
		goodsMapper.updateShopOrderDetailStatus(param.getOrdNo().trim(), SHOP_ORDER_DTL_STAT_CANCEL, custNo);
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
		String eventType = firstNonBlank(resolveJsonText(rootNode, "eventType"), "");
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
			goodsMapper.updateShopPaymentWebhook(
				payment.getPayNo(),
				SHOP_ORDER_PAY_STAT_DONE,
				paymentStatus,
				"무통장입금 완료",
				normalizedRawBody,
				normalizedWebhookDt,
				payment.getCustNo()
			);
			goodsMapper.updateShopOrderBaseStatusAndDates(
				payment.getOrdNo(),
				SHOP_ORDER_STAT_DONE,
				null,
				normalizedWebhookDt,
				payment.getCustNo()
			);
			goodsMapper.updateShopOrderDetailStatus(payment.getOrdNo(), SHOP_ORDER_DTL_STAT_DONE, payment.getCustNo());
			return;
		}

		if (!"EXPIRED".equals(paymentStatus) && !"CANCELED".equals(paymentStatus)) {
			return;
		}
		if (!SHOP_ORDER_PAY_STAT_WAITING_DEPOSIT.equals(payment.getPayStatCd())) {
			return;
		}
		String payStatCd = "CANCELED".equals(paymentStatus) ? SHOP_ORDER_PAY_STAT_CANCEL : SHOP_ORDER_PAY_STAT_FAIL;
		goodsMapper.updateShopPaymentWebhook(
			payment.getPayNo(),
			payStatCd,
			paymentStatus,
			"CANCELED".equals(paymentStatus) ? "무통장입금 취소" : "무통장입금 만료",
			normalizedRawBody,
			normalizedWebhookDt,
			payment.getCustNo()
		);
		goodsMapper.updateShopOrderBaseStatus(payment.getOrdNo(), SHOP_ORDER_STAT_CANCEL, payment.getCustNo());
		goodsMapper.updateShopOrderDetailStatus(payment.getOrdNo(), SHOP_ORDER_DTL_STAT_CANCEL, payment.getCustNo());
		restoreShopOrderSuccessSideEffects(payment, payment.getCustNo());
	}

	// 웹훅 본문에서 결제키 또는 주문번호 기준으로 현재 결제 row를 조회합니다.
	private ShopOrderPaymentVO resolveShopOrderWebhookPayment(String paymentKey, String ordNo) {
		// paymentKey가 있으면 우선 결제키 해시 기준으로 조회합니다.
		if (!isBlank(paymentKey)) {
			ShopOrderPaymentVO payment = goodsMapper.getShopPaymentByTossPaymentKeyHash(sha256Hex(paymentKey));
			if (payment != null) {
				return payment;
			}
		}

		// DEPOSIT_CALLBACK처럼 orderId만 전달되면 주문번호 기준으로 최신 결제 row를 조회합니다.
		if (isBlank(ordNo)) {
			return null;
		}
		return goodsMapper.getShopPaymentByOrdNo(ordNo.trim());
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
		if (goodsMapper.countShopOrderAddressName(custNo, savePO.getAddressNm()) > 0) {
			throw new IllegalArgumentException("이미 사용 중인 배송지명입니다.");
		}

		// 기본 배송지 저장 요청이면 기존 기본 배송지를 모두 해제합니다.
		if (YES.equals(savePO.getDefaultYn())) {
			goodsMapper.updateShopOrderAddressDefaultYn(custNo, NO, custNo);
		}

		// 배송지를 등록한 뒤 최신 배송지 목록과 기본 배송지를 구성합니다.
		goodsMapper.insertShopOrderAddress(savePO);
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
		if (goodsMapper.countShopOrderAddress(custNo, normalizedOriginAddressNm) < 1) {
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
		if (!normalizedOriginAddressNm.equals(savePO.getAddressNm()) && goodsMapper.countShopOrderAddressName(custNo, savePO.getAddressNm()) > 0) {
			throw new IllegalArgumentException("이미 사용 중인 배송지명입니다.");
		}

		// 기본 배송지 저장 요청이면 기존 기본 배송지를 모두 해제합니다.
		if (YES.equals(savePO.getDefaultYn())) {
			goodsMapper.updateShopOrderAddressDefaultYn(custNo, NO, custNo);
		}

		// 수정 대상 배송지를 갱신한 뒤 최신 목록과 수정 결과를 반환합니다.
		int updatedCount = goodsMapper.updateShopOrderAddress(
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
	public ShopCartCouponEstimateVO getShopCartCouponEstimate(ShopCartCouponEstimateRequestPO param, Long custNo) {
		// 로그인 고객번호 유효성을 확인합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}

		// 선택된 장바구니 행 정보가 없으면 0원 결과를 반환합니다.
		if (param == null || param.getCartItemList() == null || param.getCartItemList().isEmpty()) {
			return createEmptyShopCartCouponEstimate();
		}

		// 현재 장바구니와 선택 키 목록을 조회해 실제 계산 대상 행을 확정합니다.
		List<ShopCartItemVO> cartItemList = goodsMapper.getShopCartItemList(custNo);
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
		int safeBaseAmt = Math.max(baseAmt, 0);
		int cpnDcVal = normalizeNonNegativeNumber(coupon.getCpnDcVal());
		if (CPN_DC_GB_AMOUNT.equals(coupon.getCpnDcGbCd())) {
			return Math.min(safeBaseAmt, cpnDcVal);
		}
		if (CPN_DC_GB_RATE.equals(coupon.getCpnDcGbCd())) {
			return (int) Math.floor((double) safeBaseAmt * (double) cpnDcVal / 100.0d);
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
			for (int columnIndex = 0; columnIndex < columnCount; columnIndex += 1) {
				paddedMatrix[rowIndex + 1][columnIndex + 1] = weightMatrix[rowIndex][columnIndex];
			}
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
		List<ShopCartItemVO> orderCartItemList = goodsMapper.getShopOrderCartItemList(custNo, normalizedCartIdList);
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
		int availablePointAmt = normalizeNonNegativeNumber(goodsMapper.getShopAvailablePointAmt(custNo));
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
		result.setCartCouponList(buildShopOrderCouponItemList(discountContext.getCartCouponList()));
		result.setDeliveryCouponList(buildShopOrderCouponItemList(discountContext.getDeliveryCouponList()));
		return result;
	}

	// 주문 상품 행별 상품쿠폰 선택 후보 목록을 구성합니다.
	private List<ShopOrderGoodsCouponGroupVO> buildShopOrderGoodsCouponGroupList(ShopOrderDiscountContext discountContext) {
		// 상품 행이 없으면 빈 목록을 반환합니다.
		if (discountContext == null || discountContext.getEstimateRowList().isEmpty()) {
			return List.of();
		}

		// 주문 상품 행 순서대로 적용 가능한 상품쿠폰 목록을 구성합니다.
		List<ShopOrderGoodsCouponGroupVO> result = new ArrayList<>();
		for (int rowIndex = 0; rowIndex < discountContext.getEstimateRowList().size(); rowIndex += 1) {
			ShopCartCouponEstimateRow estimateRow = discountContext.getEstimateRowList().get(rowIndex);
			ShopCartItemVO cartItem = discountContext.getCartItemList().get(rowIndex);
			ShopOrderGoodsCouponGroupVO group = new ShopOrderGoodsCouponGroupVO();
			group.setCartId(estimateRow == null ? null : estimateRow.getCartId());
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
		if (estimateRow == null || discountContext == null || discountContext.getGoodsCouponList().isEmpty()) {
			return List.of();
		}

		// 적용 가능한 상품쿠폰만 선택 후보 목록으로 변환합니다.
		List<ShopOrderCouponItemVO> result = new ArrayList<>();
		for (ShopCartCustomerCouponVO coupon : discountContext.getGoodsCouponList()) {
			List<ShopGoodsCouponTargetVO> targetList = coupon == null || coupon.getCpnNo() == null
				? List.of()
				: discountContext.getCouponTargetMap().getOrDefault(coupon.getCpnNo(), List.of());
			if (!isMatchedShopCartGoodsCoupon(coupon, targetList, estimateRow)) {
				continue;
			}
			if (calculateCouponDiscountAmount(coupon, estimateRow.getRowSaleAmt()) < 1) {
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
		int selectedSaleAmt = calculateSelectedCartSaleAmt(discountContext.getEstimateRowList());
		int discountedSaleAmt = Math.max(selectedSaleAmt - goodsCouponMatchResult.getDiscountAmt(), 0);
		int deliveryFee = resolveCouponEstimateDeliveryFee(selectedSaleAmt, discountContext.getSiteInfo());

		// 장바구니/배송비 쿠폰은 최대 할인 1건을 자동 선택합니다.
		ShopCartCustomerCouponVO selectedCartCoupon = findMaximumDiscountCoupon(discountContext.getCartCouponList(), discountedSaleAmt);
		ShopCartCustomerCouponVO selectedDeliveryCoupon = findMaximumDiscountCoupon(discountContext.getDeliveryCouponList(), deliveryFee);

		// 정규화된 선택 상태와 할인 금액을 응답 객체로 조합합니다.
		ShopOrderDiscountSelectionVO selection = new ShopOrderDiscountSelectionVO();
		selection.setGoodsCouponSelectionList(goodsCouponMatchResult.getSelectionList());
		selection.setCartCouponCustCpnNo(selectedCartCoupon == null ? null : selectedCartCoupon.getCustCpnNo());
		selection.setDeliveryCouponCustCpnNo(selectedDeliveryCoupon == null ? null : selectedDeliveryCoupon.getCustCpnNo());
		return buildShopOrderDiscountQuote(
			selection,
			goodsCouponMatchResult.getDiscountAmt(),
			calculateCouponDiscountAmount(selectedCartCoupon, discountedSaleAmt),
			calculateCouponDiscountAmount(selectedDeliveryCoupon, deliveryFee),
			discountContext.getAvailablePointAmt(),
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
		int selectedSaleAmt = calculateSelectedCartSaleAmt(discountContext.getEstimateRowList());
		int discountedSaleAmt = Math.max(selectedSaleAmt - goodsCouponDiscountAmt, 0);
		int deliveryFee = resolveCouponEstimateDeliveryFee(selectedSaleAmt, discountContext.getSiteInfo());
		ShopCartCustomerCouponVO cartCoupon = findCustomerCouponByCustCpnNo(discountContext.getCartCouponList(), normalizedSelection.getCartCouponCustCpnNo());
		ShopCartCustomerCouponVO deliveryCoupon = findCustomerCouponByCustCpnNo(discountContext.getDeliveryCouponList(), normalizedSelection.getDeliveryCouponCustCpnNo());
		return buildShopOrderDiscountQuote(
			normalizedSelection,
			goodsCouponDiscountAmt,
			calculateCouponDiscountAmount(cartCoupon, discountedSaleAmt),
			calculateCouponDiscountAmount(deliveryCoupon, deliveryFee),
			discountContext.getAvailablePointAmt(),
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
		int maxPointUseAmt = Math.min(
			Math.max(availablePointAmt, 0),
			Math.max(discountedSaleAmt - normalizedCartCouponDiscountAmt, 0)
		);

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
		if (discountContext == null || discountContext.getEstimateRowList().isEmpty()) {
			return new ShopOrderGoodsCouponMatchResult(List.of(), 0);
		}

		// 기본 선택값은 모든 행 미적용 상태로 초기화합니다.
		List<ShopOrderGoodsCouponSelectionVO> selectionList = createDefaultShopOrderGoodsCouponSelectionList(discountContext.getCartItemList());
		if (discountContext.getGoodsCouponList().isEmpty()) {
			return new ShopOrderGoodsCouponMatchResult(selectionList, 0);
		}

		// 상품행-쿠폰행 조합 가중치 행렬을 구성합니다.
		int[][] weightMatrix = new int[discountContext.getEstimateRowList().size()][discountContext.getGoodsCouponList().size()];
		boolean hasPositiveWeight = false;
		for (int rowIndex = 0; rowIndex < discountContext.getEstimateRowList().size(); rowIndex += 1) {
			ShopCartCouponEstimateRow estimateRow = discountContext.getEstimateRowList().get(rowIndex);
			for (int couponIndex = 0; couponIndex < discountContext.getGoodsCouponList().size(); couponIndex += 1) {
				ShopCartCustomerCouponVO coupon = discountContext.getGoodsCouponList().get(couponIndex);
				List<ShopGoodsCouponTargetVO> targetList = coupon == null || coupon.getCpnNo() == null
					? List.of()
					: discountContext.getCouponTargetMap().getOrDefault(coupon.getCpnNo(), List.of());
				if (!isMatchedShopCartGoodsCoupon(coupon, targetList, estimateRow)) {
					continue;
				}
				int discountAmt = calculateCouponDiscountAmount(coupon, estimateRow == null ? 0 : estimateRow.getRowSaleAmt());
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
			if (couponIndex < 0 || couponIndex >= discountContext.getGoodsCouponList().size()) {
				continue;
			}
			selectionList.get(rowIndex).setCustCpnNo(discountContext.getGoodsCouponList().get(couponIndex).getCustCpnNo());
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
				if (coupon == null || coupon.getCustCpnNo() == null) {
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
		for (ShopCartItemVO cartItem : discountContext.getCartItemList()) {
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
			if (coupon == null || coupon.getCustCpnNo() == null) {
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
		for (ShopCartCouponEstimateRow estimateRow : discountContext.getEstimateRowList()) {
			if (estimateRow == null || estimateRow.getCartId() == null) {
				continue;
			}
			ShopCartCustomerCouponVO coupon = findCustomerCouponByCustCpnNo(discountContext.getGoodsCouponList(), goodsCouponSelectionMap.get(estimateRow.getCartId()));
			result += calculateCouponDiscountAmount(coupon, estimateRow.getRowSaleAmt());
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
		applyShopCartItemImageUrls(cartItemList);
		Map<String, List<ShopCartSizeOptionVO>> sizeOptionMap = buildShopCartSizeOptionMap(cartItemList);
		ShopCartSiteInfoVO siteInfo = resolveShopCartSiteInfo();

		// 장바구니 행별 사이즈 옵션 목록을 매핑합니다.
		for (ShopCartItemVO cartItem : cartItemList == null ? List.<ShopCartItemVO>of() : cartItemList) {
			if (cartItem == null || isBlank(cartItem.getGoodsId())) {
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
		result.setAvailablePointAmt(discountContext.getAvailablePointAmt());
		result.setCouponOption(buildShopOrderCouponOption(discountContext));
		result.setDiscountSelection(autoDiscountQuote.getDiscountSelection());
		result.setDiscountAmount(autoDiscountQuote.getDiscountAmount());
		result.setPaymentConfig(buildShopOrderPaymentConfig(shopOrigin));
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
		ShopOrderCustomerInfoVO customerInfo = goodsMapper.getShopOrderCustomerInfo(custNo);
		if (customerInfo == null || customerInfo.getCustNo() == null) {
			throw new IllegalArgumentException("주문 고객 정보를 찾을 수 없습니다.");
		}
		customerInfo.setCustomerKey("SHOP-CUST-" + customerInfo.getCustNo());
		customerInfo.setDeviceGbCd(firstNonBlank(trimToNull(deviceGbCd), "PC"));
		customerInfo.setCustNm(firstNonBlank(trimToNull(customerInfo.getCustNm()), "고객"));
		customerInfo.setEmail(firstNonBlank(trimToNull(customerInfo.getEmail()), ""));
		customerInfo.setPhoneNumber(firstNonBlank(trimToNull(customerInfo.getPhoneNumber()), ""));
		return customerInfo;
	}

	// 적립 예정 포인트 요약 정보를 구성합니다.
	private ShopOrderPointSaveSummaryVO buildShopOrderPointSaveSummary(List<ShopCartItemVO> cartItemList, String custGradeCd) {
		// 고객등급 적립률과 주문 전체 적립 예정 포인트를 계산합니다.
		int pointSaveRate = resolveShopPointSaveRate(resolveCustGradeCd(custGradeCd));
		int totalExpectedPoint = 0;
		for (ShopCartItemVO cartItem : cartItemList == null ? List.<ShopCartItemVO>of() : cartItemList) {
			if (cartItem == null) {
				continue;
			}
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
		String deviceGbCd
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
		result.setCartYn("goods".equalsIgnoreCase(firstNonBlank(trimToNull(from), "")) ? NO : YES);
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
		if (cartItemList == null || cartItemList.isEmpty() || isBlank(cartItemList.get(0).getGoodsNm())) {
			return "주문 상품";
		}
		String firstGoodsName = cartItemList.get(0).getGoodsNm().trim();
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
		goodsMapper.insertShopOrderBase(orderBaseSavePO);

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
			goodsMapper.insertShopOrderDetail(detailSavePO);
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
			buildShopOrderDiscountContext(List.of(cartItem), cartItem.getCustNo()).getGoodsCouponList(),
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
		int amount
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
		return result;
	}

	// 결제 스냅샷에서 장바구니 번호 목록을 추출합니다.
	private List<Long> extractShopOrderCartIdList(List<ShopCartItemVO> cartItemList) {
		// 주문 대상 장바구니 목록이 없으면 빈 목록을 반환합니다.
		List<Long> result = new ArrayList<>();
		for (ShopCartItemVO cartItem : cartItemList == null ? List.<ShopCartItemVO>of() : cartItemList) {
			if (cartItem == null || cartItem.getCartId() == null || cartItem.getCartId() < 1L) {
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
			goodsMapper.updateShopCustomerCouponUse(custNo, usedCouponList, payment.getOrdNo(), YES, custNo);
		}

		int pointUseAmt = extractShopOrderPointUseAmt(payment.getReqRawJson());
		if (pointUseAmt > 0) {
			consumeShopOrderPoint(custNo, payment.getOrdNo(), pointUseAmt);
		}

		List<Long> cartIdList = extractShopOrderCartIdListFromSnapshot(payment.getReqRawJson());
		if (!cartIdList.isEmpty()) {
			goodsMapper.deleteShopCartByCartIdList(custNo, cartIdList);
		}
	}

	// 무통장입금 만료/취소 시 쿠폰/포인트/장바구니를 원복합니다.
	private void restoreShopOrderSuccessSideEffects(ShopOrderPaymentVO payment, Long custNo) {
		// 주문번호 기준 사용 쿠폰과 포인트를 원복하고 장바구니를 다시 생성합니다.
		goodsMapper.restoreShopCustomerCouponUse(custNo, payment.getOrdNo(), custNo);
		restoreShopOrderPoint(custNo, payment.getOrdNo());
		restoreShopOrderStock(payment.getOrdNo(), custNo);

		for (ShopOrderRestoreCartItemVO restoreCartItem : goodsMapper.getShopOrderRestoreCartItemList(payment.getOrdNo())) {
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
			goodsMapper.insertShopCart(savePO);
		}
	}

	// 주문번호 기준 주문수량만큼 재고를 선차감합니다.
	private void reserveShopOrderStock(String ordNo, Long auditNo) {
		// 같은 상품/사이즈는 수량을 합산해 원자적으로 재고를 차감합니다.
		for (ShopOrderRestoreCartItemVO stockItem : aggregateShopOrderStockItemList(goodsMapper.getShopOrderRestoreCartItemList(ordNo))) {
			if (stockItem == null || isBlank(stockItem.getGoodsId()) || isBlank(stockItem.getSizeId())) {
				continue;
			}
			int requiredQty = normalizeNonNegativeNumber(stockItem.getOrdQty());
			if (requiredQty < 1) {
				continue;
			}
			int updatedCount = goodsMapper.deductShopGoodsSizeStock(stockItem.getGoodsId(), stockItem.getSizeId(), requiredQty, auditNo);
			if (updatedCount < 1) {
				throw new IllegalArgumentException(SHOP_ORDER_PAYMENT_STOCK_SHORTAGE_MESSAGE);
			}
		}
	}

	// 주문번호 기준 차감했던 재고를 다시 복구합니다.
	private void restoreShopOrderStock(String ordNo, Long auditNo) {
		// 같은 상품/사이즈는 수량을 합산해 재고를 복구합니다.
		for (ShopOrderRestoreCartItemVO stockItem : aggregateShopOrderStockItemList(goodsMapper.getShopOrderRestoreCartItemList(ordNo))) {
			if (stockItem == null || isBlank(stockItem.getGoodsId()) || isBlank(stockItem.getSizeId())) {
				continue;
			}
			int restoreQty = normalizeNonNegativeNumber(stockItem.getOrdQty());
			if (restoreQty < 1) {
				continue;
			}
			goodsMapper.restoreShopGoodsSizeStock(stockItem.getGoodsId(), stockItem.getSizeId(), restoreQty, auditNo);
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
		for (ShopOrderPointBaseVO pointBase : goodsMapper.getShopAvailablePointBaseList(custNo)) {
			if (pointBase == null || pointBase.getPntNo() == null || remainingAmt < 1) {
				continue;
			}
			int usableAmt = Math.min(normalizeNonNegativeNumber(pointBase.getRmnAmt()), remainingAmt);
			if (usableAmt < 1) {
				continue;
			}
			goodsMapper.updateShopCustomerPointUseAmt(pointBase.getPntNo(), usableAmt, custNo);
			ShopOrderPointDetailSavePO detailSavePO = new ShopOrderPointDetailSavePO();
			detailSavePO.setPntNo(pointBase.getPntNo());
			detailSavePO.setPntAmt(-usableAmt);
			detailSavePO.setOrdNo(ordNo);
			detailSavePO.setBigo(SHOP_ORDER_POINT_USE_MEMO);
			detailSavePO.setRegNo(custNo);
			goodsMapper.insertShopOrderPointDetail(detailSavePO);
			remainingAmt -= usableAmt;
		}
		if (remainingAmt > 0) {
			throw new IllegalStateException("포인트 사용 처리에 실패했습니다.");
		}
	}

	// 주문번호 기준 차감 포인트를 원복합니다.
	private void restoreShopOrderPoint(Long custNo, String ordNo) {
		// 주문번호 기준 음수 사용 상세 이력을 읽어 반대로 복구합니다.
		for (ShopOrderPointDetailVO pointDetail : goodsMapper.getShopOrderPointDetailList(ordNo)) {
			if (pointDetail == null || pointDetail.getPntNo() == null) {
				continue;
			}
			int restoreAmt = Math.abs(normalizeNonNegativeNumber(pointDetail.getPntAmt() == null ? null : Math.abs(pointDetail.getPntAmt())));
			if (restoreAmt < 1) {
				continue;
			}
			goodsMapper.restoreShopCustomerPointUseAmt(pointDetail.getPntNo(), restoreAmt, custNo);
			ShopOrderPointDetailSavePO restoreDetail = new ShopOrderPointDetailSavePO();
			restoreDetail.setPntNo(pointDetail.getPntNo());
			restoreDetail.setPntAmt(restoreAmt);
			restoreDetail.setOrdNo(ordNo);
			restoreDetail.setBigo(SHOP_ORDER_POINT_RESTORE_MEMO);
			restoreDetail.setRegNo(custNo);
			goodsMapper.insertShopOrderPointDetail(restoreDetail);
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
	private JsonNode readShopOrderJsonNode(String rawJson) {
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
	private String writeShopOrderJson(Object value) {
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
	private String resolveJsonText(JsonNode node, String fieldName) {
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
		return URLEncoder.encode(firstNonBlank(value, ""), StandardCharsets.UTF_8);
	}

	// Toss 클라이언트 키를 반환합니다.
	private String resolveShopOrderClientKey() {
		return firstNonBlank(trimToNull(tossProperties.clientKey()), "");
	}

	// 다양한 날짜 문자열을 주문 결제 저장용 형식으로 정규화합니다.
	private String normalizeShopOrderDateTime(String value) {
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

	// 첫 번째 비어 있지 않은 문자열을 반환합니다.
	private String firstNonBlank(String first, String second) {
		// 첫 번째 값이 비어 있으면 두 번째 값을 반환합니다.
		String normalizedFirst = trimToNull(first);
		if (normalizedFirst != null) {
			return normalizedFirst;
		}
		String normalizedSecond = trimToNull(second);
		return normalizedSecond == null ? "" : normalizedSecond;
	}

	// 현재 고객의 배송지 목록을 기본 배송지 우선 순서로 조회합니다.
	private List<ShopOrderAddressVO> resolveShopOrderAddressList(Long custNo) {
		List<ShopOrderAddressVO> addressList = goodsMapper.getShopOrderAddressList(custNo);
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
				if (sizeItem == null || isBlank(sizeItem.getSizeId())) {
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
	public void updateShopCartOption(ShopCartOptionUpdatePO param, Long custNo) {
		// 요청 데이터와 필수 입력값을 검증합니다.
		if (param == null) {
			throw new IllegalArgumentException("요청 데이터를 확인해주세요.");
		}
		if (isBlank(param.getGoodsId())) {
			throw new IllegalArgumentException("상품코드를 확인해주세요.");
		}
		if (isBlank(param.getSizeId())) {
			throw new IllegalArgumentException("변경 대상 사이즈를 확인해주세요.");
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
		String normalizedGoodsId = param.getGoodsId().trim();
		String normalizedSizeId = param.getSizeId().trim();
		String normalizedTargetSizeId = param.getTargetSizeId().trim();
		int resolvedQty = param.getQty();
		if (goodsMapper.countShopCart(custNo, normalizedGoodsId, normalizedSizeId) < 1) {
			throw new IllegalArgumentException("변경할 장바구니 상품을 찾을 수 없습니다.");
		}

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
			int updatedCount = goodsMapper.updateShopCartQty(custNo, normalizedGoodsId, normalizedTargetSizeId, resolvedQty, custNo);
			if (updatedCount < 1) {
				throw new IllegalArgumentException("변경할 장바구니 상품을 찾을 수 없습니다.");
			}
			return;
		}

		// 목표 사이즈가 이미 있으면 수량을 병합하고 기존 행은 삭제합니다.
		int existedTargetCount = goodsMapper.countShopCart(custNo, normalizedGoodsId, normalizedTargetSizeId);
		if (existedTargetCount > 0) {
			Integer existedTargetQty = goodsMapper.getShopCartQty(custNo, normalizedGoodsId, normalizedTargetSizeId);
			int mergedQty = Math.max(existedTargetQty == null ? 0 : existedTargetQty, 0) + resolvedQty;
			goodsMapper.updateShopCartQty(custNo, normalizedGoodsId, normalizedTargetSizeId, mergedQty, custNo);
			goodsMapper.deleteShopCartItem(custNo, normalizedGoodsId, normalizedSizeId);
			return;
		}

		// 목표 사이즈가 없으면 기존 행의 사이즈/수량을 직접 변경합니다.
		int updatedCount = goodsMapper.updateShopCartOption(
			custNo,
			normalizedGoodsId,
			normalizedSizeId,
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
	public int deleteShopCartItems(ShopCartDeletePO param, Long custNo) {
		// 로그인 고객번호와 삭제 요청 목록 유효성을 확인합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}
		if (param == null || param.getCartItemList() == null || param.getCartItemList().isEmpty()) {
			throw new IllegalArgumentException("삭제할 상품을 선택해주세요.");
		}

		// 중복 선택된 키를 제거하며 장바구니 항목을 삭제합니다.
		int deletedCount = 0;
		Set<String> deletedKeySet = new HashSet<>();
		for (ShopCartDeleteItemPO deleteItem : param.getCartItemList()) {
			if (deleteItem == null) {
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
			deletedCount += goodsMapper.deleteShopCartItem(custNo, normalizedGoodsId, normalizedSizeId);
		}
		if (deletedKeySet.isEmpty()) {
			throw new IllegalArgumentException("삭제할 상품을 선택해주세요.");
		}
		return deletedCount;
	}

	// 쇼핑몰 장바구니 전체 상품을 삭제합니다.
	@Transactional
	public int deleteShopCartAll(Long custNo) {
		// 로그인 고객번호 유효성을 확인합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}

		// 장바구니 전체 삭제를 수행합니다.
		return goodsMapper.deleteShopCartAll(custNo);
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
		ShopCartSiteInfoVO siteInfo = goodsMapper.getShopCartSiteInfo(SHOP_SITE_ID);
		if (siteInfo != null) {
			siteInfo.setSiteId(isBlank(siteInfo.getSiteId()) ? SHOP_SITE_ID : siteInfo.getSiteId());
			siteInfo.setDeliveryFee(siteInfo.getDeliveryFee() == null ? 0 : Math.max(siteInfo.getDeliveryFee(), 0));
			siteInfo.setDeliveryFeeLimit(siteInfo.getDeliveryFeeLimit() == null ? 0 : Math.max(siteInfo.getDeliveryFeeLimit(), 0));
			return siteInfo;
		}

		// 데이터가 없으면 기본값을 반환합니다.
		ShopCartSiteInfoVO fallback = new ShopCartSiteInfoVO();
		fallback.setSiteId(SHOP_SITE_ID);
		fallback.setDeliveryFee(0);
		fallback.setDeliveryFeeLimit(0);
		return fallback;
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
		ShopGoodsSiteInfoVO siteInfo = goodsMapper.getShopGoodsSiteInfo(SHOP_SITE_ID);
		if (siteInfo != null) {
			return siteInfo;
		}

		// 데이터가 없으면 기본값을 반환합니다.
		ShopGoodsSiteInfoVO fallback = new ShopGoodsSiteInfoVO();
		fallback.setSiteId(SHOP_SITE_ID);
		fallback.setSiteNm("");
		fallback.setDeliveryFee(0);
		fallback.setDeliveryFeeLimit(0);
		return fallback;
	}

	// 쇼핑몰 상품상세 가격 요약 정보를 계산합니다.
	private ShopGoodsPriceSummaryVO buildShopGoodsPriceSummary(ShopGoodsBasicVO goods) {
		// 공급가/판매가를 안전한 값으로 보정합니다.
		int supplyAmt = goods != null && goods.getSupplyAmt() != null ? Math.max(goods.getSupplyAmt(), 0) : 0;
		int saleAmt = goods != null && goods.getSaleAmt() != null ? Math.max(goods.getSaleAmt(), 0) : 0;
		boolean showSupplyStrike = supplyAmt > saleAmt;

		// 할인율은 소수점 버림 정수로 계산합니다.
		int discountRate = 0;
		if (showSupplyStrike && supplyAmt > 0) {
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

	// 상품상세 이미지 목록의 표시용 URL을 보정합니다.
	private void applyShopGoodsImageUrlList(List<ShopGoodsImageVO> imageList) {
		if (imageList == null || imageList.isEmpty()) {
			return;
		}
		for (ShopGoodsImageVO item : imageList) {
			if (item == null) {
				continue;
			}
			item.setImgUrl(resolveShopGoodsImageUrl(item.getGoodsId(), item.getImgPath()));
		}
	}

	// 그룹상품 목록의 대표 이미지 URL을 보정합니다.
	private void applyShopGoodsGroupItemImageUrlList(List<ShopGoodsGroupItemVO> groupGoodsList) {
		if (groupGoodsList == null || groupGoodsList.isEmpty()) {
			return;
		}
		for (ShopGoodsGroupItemVO item : groupGoodsList) {
			if (item == null) {
				continue;
			}
			item.setFirstImgUrl(resolveShopGoodsImageUrl(item.getGoodsId(), item.getFirstImgPath()));
		}
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

	// 상품 목록에 이미지 URL을 세팅합니다.
	private void applyGoodsImageUrls(List<GoodsVO> list) {
		if (list == null || list.isEmpty()) {
			return;
		}
		for (GoodsVO item : list) {
			if (item == null) {
				continue;
			}
			String imgPath = item.getImgPath();
			if (isBlank(imgPath)) {
				continue;
			}
			if (imgPath.startsWith("http://") || imgPath.startsWith("https://")) {
				item.setImgUrl(imgPath);
				continue;
			}
			item.setImgUrl(ftpFileService.buildGoodsImageUrl(item.getGoodsId(), imgPath));
		}
	}

	// 카테고리 상품 목록에 이미지 URL을 세팅합니다.
	private void applyCategoryGoodsImageUrls(List<CategoryGoodsVO> list) {
		if (list == null || list.isEmpty()) {
			return;
		}
		for (CategoryGoodsVO item : list) {
			if (item == null) {
				continue;
			}
			String imgPath = item.getImgPath();
			if (isBlank(imgPath)) {
				continue;
			}
			if (imgPath.startsWith("http://") || imgPath.startsWith("https://")) {
				item.setImgUrl(imgPath);
				continue;
			}
			item.setImgUrl(ftpFileService.buildGoodsImageUrl(item.getGoodsId(), imgPath));
		}
	}


	// 쇼핑몰 카테고리 상품 목록에 이미지 URL을 세팅합니다.
	private void applyShopCategoryGoodsImageUrls(List<ShopCategoryGoodsItemVO> list) {
		if (list == null || list.isEmpty()) {
			return;
		}
		for (ShopCategoryGoodsItemVO item : list) {
			if (item == null) {
				continue;
			}
			item.setImgUrl(resolveShopGoodsImageUrl(item.getGoodsId(), item.getImgPath()));
			item.setSecondaryImgUrl(resolveShopGoodsImageUrl(item.getGoodsId(), item.getSecondaryImgPath()));
		}
	}

	// 쇼핑몰 마이페이지 위시리스트 상품 목록에 이미지 URL을 세팅합니다.
	private void applyShopMypageWishGoodsImageUrls(List<ShopMypageWishGoodsItemVO> list) {
		if (list == null || list.isEmpty()) {
			return;
		}
		for (ShopMypageWishGoodsItemVO item : list) {
			if (item == null) {
				continue;
			}
			item.setImgUrl(resolveShopGoodsImageUrl(item.getGoodsId(), item.getImgPath()));
		}
	}

	// 쇼핑몰 마이페이지 주문내역 주문상세 목록에 이미지 URL을 세팅합니다.
	private void applyShopMypageOrderDetailImageUrls(List<ShopMypageOrderDetailItemVO> list) {
		if (list == null || list.isEmpty()) {
			return;
		}
		for (ShopMypageOrderDetailItemVO item : list) {
			if (item == null) {
				continue;
			}
			item.setImgUrl(resolveShopGoodsImageUrl(item.getGoodsId(), item.getImgPath()));
		}
	}

	// 쇼핑몰 장바구니 상품 목록에 이미지 URL을 세팅합니다.
	private void applyShopCartItemImageUrls(List<ShopCartItemVO> list) {
		if (list == null || list.isEmpty()) {
			return;
		}
		for (ShopCartItemVO item : list) {
			if (item == null) {
				continue;
			}
			item.setImgUrl(resolveShopGoodsImageUrl(item.getGoodsId(), item.getImgPath()));
		}
	}

	// 쇼핑몰 상품 이미지 경로를 UI 조회용 URL로 변환합니다.
	private String resolveShopGoodsImageUrl(String goodsId, String imgPath) {
		// 이미지 경로가 없으면 빈 문자열을 반환합니다.
		if (isBlank(imgPath)) {
			return "";
		}
		// 절대 URL은 그대로 반환합니다.
		if (imgPath.startsWith("http://") || imgPath.startsWith("https://")) {
			return imgPath;
		}
		// 파일명만 있는 경우 FTP 규칙으로 URL을 생성합니다.
		return ftpFileService.buildGoodsImageUrl(goodsId, imgPath);
	}

	// 장바구니 저장 검증 완료 입력값을 전달합니다.
	private static class ShopCartValidatedInput {
		// 상품코드입니다.
		private final String goodsId;
		// 사이즈코드입니다.
		private final String sizeId;
		// 수량입니다.
		private final int qty;

		// 검증 완료된 장바구니 입력값을 생성합니다.
		private ShopCartValidatedInput(String goodsId, String sizeId, int qty) {
			this.goodsId = goodsId;
			this.sizeId = sizeId;
			this.qty = qty;
		}

		// 상품코드를 반환합니다.
		private String getGoodsId() {
			return goodsId;
		}

		// 사이즈코드를 반환합니다.
		private String getSizeId() {
			return sizeId;
		}

		// 수량을 반환합니다.
		private int getQty() {
			return qty;
		}
	}

	// 장바구니 쿠폰 예상 할인 계산용 상품 행 컨텍스트를 전달합니다.
	private static class ShopCartCouponEstimateRow {
		// 장바구니 번호입니다.
		private final Long cartId;
		// 상품코드입니다.
		private final String goodsId;
		// 사이즈코드입니다.
		private final String sizeId;
		// 행 단위 판매가 합계입니다.
		private final int rowSaleAmt;
		// 브랜드 번호 문자열입니다.
		private final String brandNoValue;
		// 상품 카테고리 코드 목록입니다.
		private final Set<String> categoryIdSet;
		// 상품 기획전 탭 번호 목록입니다.
		private final Set<String> exhibitionTabNoSet;

		// 장바구니 쿠폰 예상 할인 계산용 상품 행 컨텍스트를 생성합니다.
		private ShopCartCouponEstimateRow(
			Long cartId,
			String goodsId,
			String sizeId,
			int rowSaleAmt,
			String brandNoValue,
			Set<String> categoryIdSet,
			Set<String> exhibitionTabNoSet
		) {
			// 생성자 입력값을 그대로 불변 필드에 보관합니다.
			this.cartId = cartId;
			this.goodsId = goodsId;
			this.sizeId = sizeId;
			this.rowSaleAmt = rowSaleAmt;
			this.brandNoValue = brandNoValue;
			this.categoryIdSet = categoryIdSet == null ? Set.of() : categoryIdSet;
			this.exhibitionTabNoSet = exhibitionTabNoSet == null ? Set.of() : exhibitionTabNoSet;
		}

		// 장바구니 번호를 반환합니다.
		private Long getCartId() {
			return cartId;
		}

		// 상품코드를 반환합니다.
		private String getGoodsId() {
			return goodsId;
		}

		// 사이즈코드를 반환합니다.
		private String getSizeId() {
			return sizeId;
		}

		// 행 단위 판매가 합계를 반환합니다.
		private int getRowSaleAmt() {
			return rowSaleAmt;
		}

		// 브랜드 번호 문자열을 반환합니다.
		private String getBrandNoValue() {
			return brandNoValue;
		}

		// 상품 카테고리 코드 목록을 반환합니다.
		private Set<String> getCategoryIdSet() {
			return categoryIdSet;
		}

		// 상품 기획전 탭 번호 목록을 반환합니다.
		private Set<String> getExhibitionTabNoSet() {
			return exhibitionTabNoSet;
		}
	}

	// 주문서 할인 계산에 필요한 컨텍스트를 전달합니다.
	private static class ShopOrderDiscountContext {
		// 주문 대상 장바구니 목록입니다.
		private final List<ShopCartItemVO> cartItemList;
		// 쿠폰 계산용 행 목록입니다.
		private final List<ShopCartCouponEstimateRow> estimateRowList;
		// 현재 사용 가능한 전체 보유 쿠폰 목록입니다.
		private final List<ShopCartCustomerCouponVO> customerCouponList;
		// 상품쿠폰 목록입니다.
		private final List<ShopCartCustomerCouponVO> goodsCouponList;
		// 장바구니 쿠폰 목록입니다.
		private final List<ShopCartCustomerCouponVO> cartCouponList;
		// 배송비 쿠폰 목록입니다.
		private final List<ShopCartCustomerCouponVO> deliveryCouponList;
		// 쿠폰 번호별 적용 대상 목록입니다.
		private final Map<Long, List<ShopGoodsCouponTargetVO>> couponTargetMap;
		// 배송비 기준 사이트 정보입니다.
		private final ShopCartSiteInfoVO siteInfo;
		// 현재 사용 가능한 보유 포인트입니다.
		private final int availablePointAmt;

		// 주문서 할인 계산 컨텍스트를 생성합니다.
		private ShopOrderDiscountContext(
			List<ShopCartItemVO> cartItemList,
			List<ShopCartCouponEstimateRow> estimateRowList,
			List<ShopCartCustomerCouponVO> customerCouponList,
			List<ShopCartCustomerCouponVO> goodsCouponList,
			List<ShopCartCustomerCouponVO> cartCouponList,
			List<ShopCartCustomerCouponVO> deliveryCouponList,
			Map<Long, List<ShopGoodsCouponTargetVO>> couponTargetMap,
			ShopCartSiteInfoVO siteInfo,
			int availablePointAmt
		) {
			this.cartItemList = cartItemList == null ? List.of() : cartItemList;
			this.estimateRowList = estimateRowList == null ? List.of() : estimateRowList;
			this.customerCouponList = customerCouponList == null ? List.of() : customerCouponList;
			this.goodsCouponList = goodsCouponList == null ? List.of() : goodsCouponList;
			this.cartCouponList = cartCouponList == null ? List.of() : cartCouponList;
			this.deliveryCouponList = deliveryCouponList == null ? List.of() : deliveryCouponList;
			this.couponTargetMap = couponTargetMap == null ? Map.of() : couponTargetMap;
			this.siteInfo = siteInfo;
			this.availablePointAmt = Math.max(availablePointAmt, 0);
		}

		// 주문 대상 장바구니 목록을 반환합니다.
		private List<ShopCartItemVO> getCartItemList() {
			return cartItemList;
		}

		// 쿠폰 계산용 행 목록을 반환합니다.
		private List<ShopCartCouponEstimateRow> getEstimateRowList() {
			return estimateRowList;
		}

		// 현재 사용 가능한 전체 보유 쿠폰 목록을 반환합니다.
		private List<ShopCartCustomerCouponVO> getCustomerCouponList() {
			return customerCouponList;
		}

		// 상품쿠폰 목록을 반환합니다.
		private List<ShopCartCustomerCouponVO> getGoodsCouponList() {
			return goodsCouponList;
		}

		// 장바구니 쿠폰 목록을 반환합니다.
		private List<ShopCartCustomerCouponVO> getCartCouponList() {
			return cartCouponList;
		}

		// 배송비 쿠폰 목록을 반환합니다.
		private List<ShopCartCustomerCouponVO> getDeliveryCouponList() {
			return deliveryCouponList;
		}

		// 쿠폰 번호별 적용 대상 목록을 반환합니다.
		private Map<Long, List<ShopGoodsCouponTargetVO>> getCouponTargetMap() {
			return couponTargetMap;
		}

		// 배송비 기준 사이트 정보를 반환합니다.
		private ShopCartSiteInfoVO getSiteInfo() {
			return siteInfo;
		}

		// 현재 사용 가능한 보유 포인트를 반환합니다.
		private int getAvailablePointAmt() {
			return availablePointAmt;
		}
	}

	// 주문서 상품쿠폰 자동 선택 결과를 전달합니다.
	private static class ShopOrderGoodsCouponMatchResult {
		// 상품행별 선택 목록입니다.
		private final List<ShopOrderGoodsCouponSelectionVO> selectionList;
		// 상품쿠폰 할인 합계입니다.
		private final int discountAmt;

		// 주문서 상품쿠폰 자동 선택 결과를 생성합니다.
		private ShopOrderGoodsCouponMatchResult(List<ShopOrderGoodsCouponSelectionVO> selectionList, int discountAmt) {
			this.selectionList = selectionList == null ? List.of() : selectionList;
			this.discountAmt = Math.max(discountAmt, 0);
		}

		// 상품행별 선택 목록을 반환합니다.
		private List<ShopOrderGoodsCouponSelectionVO> getSelectionList() {
			return selectionList;
		}

		// 상품쿠폰 할인 합계를 반환합니다.
		private int getDiscountAmt() {
			return discountAmt;
		}
	}

	// 쿠폰 사용 불가 상품 요약 정보를 내부 계산용으로 전달합니다.
	private static class ShopMypageCouponUnavailableGoodsSummary {
		// 쿠폰 사용 불가 상품 전체 건수입니다.
		private final int unavailableGoodsCount;
		// 쿠폰 사용 불가 상품 목록입니다.
		private final List<ShopMypageCouponUnavailableGoodsVO> unavailableGoodsList;

		// 쿠폰 사용 불가 상품 요약 정보를 생성합니다.
		private ShopMypageCouponUnavailableGoodsSummary(
			int unavailableGoodsCount,
			List<ShopMypageCouponUnavailableGoodsVO> unavailableGoodsList
		) {
			this.unavailableGoodsCount = Math.max(unavailableGoodsCount, 0);
			this.unavailableGoodsList = unavailableGoodsList == null ? List.of() : unavailableGoodsList;
		}

		// 빈 요약 정보를 반환합니다.
		private static ShopMypageCouponUnavailableGoodsSummary empty() {
			return new ShopMypageCouponUnavailableGoodsSummary(0, List.of());
		}

		// 쿠폰 사용 불가 상품 전체 건수를 반환합니다.
		private int getUnavailableGoodsCount() {
			return unavailableGoodsCount;
		}

		// 쿠폰 사용 불가 상품 목록을 반환합니다.
		private List<ShopMypageCouponUnavailableGoodsVO> getUnavailableGoodsList() {
			return unavailableGoodsList;
		}
	}

	// 엑셀 업로드 행 정보를 전달합니다.
	private static class CategoryGoodsExcelRow {
		// 카테고리코드입니다.
		private final String categoryId;
		// 상품코드입니다.
		private final String goodsId;
		// 정렬순서입니다.
		private final Integer dispOrd;

		// 엑셀 업로드 행을 생성합니다.
		private CategoryGoodsExcelRow(String categoryId, String goodsId, Integer dispOrd) {
			this.categoryId = categoryId;
			this.goodsId = goodsId;
			this.dispOrd = dispOrd;
		}

		// 카테고리코드를 반환합니다.
		public String categoryId() {
			return categoryId;
		}

		// 상품코드를 반환합니다.
		public String goodsId() {
			return goodsId;
		}

		// 정렬순서를 반환합니다.
		public Integer dispOrd() {
			return dispOrd;
		}
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
	@Transactional
	public void saveAdminGoodsCategories(GoodsSavePO param) {
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
		for (GoodsImageVO item : list) {
			if (item == null) {
				continue;
			}
			String imgPath = item.getImgPath();
			if (imgPath != null && (imgPath.startsWith("http://") || imgPath.startsWith("https://"))) {
				item.setImgUrl(imgPath);
				continue;
			}
			item.setImgUrl(ftpFileService.buildGoodsImageUrl(goodsId, imgPath));
		}
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

	// 접두사/고객번호/년월일시분초밀리초를 조합해 클레임번호를 생성합니다.
	private String generateShopOrderClaimNo(Long custNo) {
		// 클레임번호는 주문번호와 구분되도록 C 접두사를 사용합니다.
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
		long safeCustNo = custNo == null ? 0L : Math.max(custNo, 0L);
		return "C" + safeCustNo + timestamp;
	}

	// 주문취소 계산 결과를 내부적으로 전달합니다.
	private static class ShopOrderCancelComputation {
		// 선택된 주문취소 대상 행 목록입니다.
		private final List<ShopOrderCancelSelectedItem> selectedItemList;
		// 프론트와 비교할 취소 예정 금액 요약입니다.
		private final ShopOrderCancelPreviewAmountPO previewAmount;
		// 전체취소 여부입니다.
		private final boolean fullCancel;
		// PG 현금 환불 금액입니다.
		private final long refundedCashAmt;
		// 복구할 포인트 금액입니다.
		private final long restoredPointAmt;
		// 배송비 조정 금액입니다.
		private final long shippingAdjustmentAmt;

		// 계산 결과 객체를 생성합니다.
		private ShopOrderCancelComputation(
			List<ShopOrderCancelSelectedItem> selectedItemList,
			ShopOrderCancelPreviewAmountPO previewAmount,
			boolean fullCancel,
			long refundedCashAmt,
			long restoredPointAmt,
			long shippingAdjustmentAmt
		) {
			this.selectedItemList = selectedItemList;
			this.previewAmount = previewAmount;
			this.fullCancel = fullCancel;
			this.refundedCashAmt = refundedCashAmt;
			this.restoredPointAmt = restoredPointAmt;
			this.shippingAdjustmentAmt = shippingAdjustmentAmt;
		}

		// 선택된 주문취소 대상 행 목록을 반환합니다.
		private List<ShopOrderCancelSelectedItem> getSelectedItemList() {
			return selectedItemList;
		}

		// 프론트 비교용 취소 예정 금액 요약을 반환합니다.
		private ShopOrderCancelPreviewAmountPO getPreviewAmount() {
			return previewAmount;
		}

		// 전체취소 여부를 반환합니다.
		private boolean isFullCancel() {
			return fullCancel;
		}

		// PG 현금 환불 금액을 반환합니다.
		private long getRefundedCashAmt() {
			return refundedCashAmt;
		}

		// 복구 포인트 금액을 반환합니다.
		private long getRestoredPointAmt() {
			return restoredPointAmt;
		}

		// 배송비 조정 금액을 반환합니다.
		private long getShippingAdjustmentAmt() {
			return shippingAdjustmentAmt;
		}
	}

	// 주문취소 화면 내부 계산용 금액 요약을 전달합니다.
	private static class ShopOrderCancelPreviewSummary {
		// 공급가 합계입니다.
		private long totalSupplyAmt;
		// 상품 판매가 합계입니다.
		private long totalOrderAmt;
		// 상품쿠폰 환급 합계입니다.
		private long totalGoodsCouponDiscountAmt;
		// 장바구니쿠폰 환급 합계입니다.
		private long totalCartCouponDiscountAmt;
		// 포인트 환급 합계입니다.
		private long totalPointRefundAmt;

		// 공급가 합계를 반환합니다.
		private long getTotalSupplyAmt() {
			return totalSupplyAmt;
		}

		// 공급가 합계를 저장합니다.
		private void setTotalSupplyAmt(long totalSupplyAmt) {
			this.totalSupplyAmt = totalSupplyAmt;
		}

		// 상품 판매가 합계를 반환합니다.
		private long getTotalOrderAmt() {
			return totalOrderAmt;
		}

		// 상품 판매가 합계를 저장합니다.
		private void setTotalOrderAmt(long totalOrderAmt) {
			this.totalOrderAmt = totalOrderAmt;
		}

		// 상품쿠폰 환급 합계를 반환합니다.
		private long getTotalGoodsCouponDiscountAmt() {
			return totalGoodsCouponDiscountAmt;
		}

		// 상품쿠폰 환급 합계를 저장합니다.
		private void setTotalGoodsCouponDiscountAmt(long totalGoodsCouponDiscountAmt) {
			this.totalGoodsCouponDiscountAmt = totalGoodsCouponDiscountAmt;
		}

		// 장바구니쿠폰 환급 합계를 반환합니다.
		private long getTotalCartCouponDiscountAmt() {
			return totalCartCouponDiscountAmt;
		}

		// 장바구니쿠폰 환급 합계를 저장합니다.
		private void setTotalCartCouponDiscountAmt(long totalCartCouponDiscountAmt) {
			this.totalCartCouponDiscountAmt = totalCartCouponDiscountAmt;
		}

		// 포인트 환급 합계를 반환합니다.
		private long getTotalPointRefundAmt() {
			return totalPointRefundAmt;
		}

		// 포인트 환급 합계를 저장합니다.
		private void setTotalPointRefundAmt(long totalPointRefundAmt) {
			this.totalPointRefundAmt = totalPointRefundAmt;
		}
	}

	// 주문취소 대상 주문상세 1건과 취소수량 정보를 전달합니다.
	private static class ShopOrderCancelSelectedItem {
		// 취소 대상 주문상세 행입니다.
		private final ShopMypageOrderDetailItemVO detailItem;
		// 취소 수량입니다.
		private final int cancelQty;
		// 취소 후 남는 수량입니다.
		private final int remainingAfterCancelQty;
		// 취소 반영 후 주문상세 상태코드입니다.
		private final String nextOrdDtlStatCd;

		// 선택 행 정보를 생성합니다.
		private ShopOrderCancelSelectedItem(
			ShopMypageOrderDetailItemVO detailItem,
			int cancelQty,
			int remainingAfterCancelQty,
			String nextOrdDtlStatCd
		) {
			this.detailItem = detailItem;
			this.cancelQty = cancelQty;
			this.remainingAfterCancelQty = remainingAfterCancelQty;
			this.nextOrdDtlStatCd = nextOrdDtlStatCd;
		}

		// 취소 대상 주문상세 행을 반환합니다.
		private ShopMypageOrderDetailItemVO getDetailItem() {
			return detailItem;
		}

		// 취소 수량을 반환합니다.
		private int getCancelQty() {
			return cancelQty;
		}

		// 취소 후 남는 수량을 반환합니다.
		private int getRemainingAfterCancelQty() {
			return remainingAfterCancelQty;
		}

		// 취소 반영 후 주문상세 상태코드를 반환합니다.
		private String getNextOrdDtlStatCd() {
			return nextOrdDtlStatCd;
		}
	}

	// PG 취소 성공 응답에서 필요한 요약값을 전달합니다.
	private static class ShopOrderCancelPgResult {
		// PG 원본 응답 문자열입니다.
		private final String rawResponse;
		// PG 응답 코드입니다.
		private final String rspCode;
		// PG 응답 메시지입니다.
		private final String rspMsg;
		// PG 거래키입니다.
		private final String tradeNo;
		// 취소 완료 일시입니다.
		private final String approvedDt;
		// 실제 취소 금액입니다.
		private final long canceledAmount;

		// PG 취소 요약 결과를 생성합니다.
		private ShopOrderCancelPgResult(
			String rawResponse,
			String rspCode,
			String rspMsg,
			String tradeNo,
			String approvedDt,
			long canceledAmount
		) {
			this.rawResponse = rawResponse;
			this.rspCode = rspCode;
			this.rspMsg = rspMsg;
			this.tradeNo = tradeNo;
			this.approvedDt = approvedDt;
			this.canceledAmount = canceledAmount;
		}

		// PG 원본 응답 문자열을 반환합니다.
		private String getRawResponse() {
			return rawResponse;
		}

		// PG 응답 코드를 반환합니다.
		private String getRspCode() {
			return rspCode;
		}

		// PG 응답 메시지를 반환합니다.
		private String getRspMsg() {
			return rspMsg;
		}

		// PG 거래키를 반환합니다.
		private String getTradeNo() {
			return tradeNo;
		}

		// 취소 완료 일시를 반환합니다.
		private String getApprovedDt() {
			return approvedDt;
		}

		// 실제 취소 금액을 반환합니다.
		private long getCanceledAmount() {
			return canceledAmount;
		}
	}

	// 마이페이지 주문내역 조회 기간 보정 결과를 내부적으로 전달합니다.
	private static class ShopMypageOrderDateRange {
		// 조회 시작일입니다.
		private String startDate;
		// 조회 종료일입니다.
		private String endDate;
		// 조회 시작일시입니다.
		private String startDateTime;
		// 조회 종료일 다음날 00시 기준 비교 일시입니다.
		private String endExclusiveDateTime;

		// 조회 시작일을 반환합니다.
		private String getStartDate() {
			return startDate;
		}

		// 조회 시작일을 저장합니다.
		private void setStartDate(String startDate) {
			this.startDate = startDate;
		}

		// 조회 종료일을 반환합니다.
		private String getEndDate() {
			return endDate;
		}

		// 조회 종료일을 저장합니다.
		private void setEndDate(String endDate) {
			this.endDate = endDate;
		}

		// 조회 시작일시를 반환합니다.
		private String getStartDateTime() {
			return startDateTime;
		}

		// 조회 시작일시를 저장합니다.
		private void setStartDateTime(String startDateTime) {
			this.startDateTime = startDateTime;
		}

		// 조회 종료일 다음날 00시 비교 일시를 반환합니다.
		private String getEndExclusiveDateTime() {
			return endExclusiveDateTime;
		}

		// 조회 종료일 다음날 00시 비교 일시를 저장합니다.
		private void setEndExclusiveDateTime(String endExclusiveDateTime) {
			this.endExclusiveDateTime = endExclusiveDateTime;
		}
	}
}
