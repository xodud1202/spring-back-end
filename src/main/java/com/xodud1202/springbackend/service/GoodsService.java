package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.admin.brand.BrandVO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsPO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsDetailVO;
import com.xodud1202.springbackend.domain.admin.category.CategorySavePO;
import com.xodud1202.springbackend.domain.admin.category.CategoryGoodsDeletePO;
import com.xodud1202.springbackend.domain.admin.category.CategoryGoodsOrderItem;
import com.xodud1202.springbackend.domain.admin.category.CategoryGoodsOrderSavePO;
import com.xodud1202.springbackend.domain.admin.category.CategoryGoodsRegisterPO;
import com.xodud1202.springbackend.domain.admin.category.CategoryGoodsSavePO;
import com.xodud1202.springbackend.domain.admin.category.CategoryGoodsVO;
import com.xodud1202.springbackend.domain.admin.category.CategoryVO;
import com.xodud1202.springbackend.domain.shop.category.ShopCategoryGoodsItemVO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartCouponEstimateItemPO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartCouponEstimateRequestPO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartCouponEstimateVO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartCustomerCouponVO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartDeleteItemPO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartDeletePO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartItemVO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartOptionUpdatePO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartPageVO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartSiteInfoVO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartSizeOptionVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsBasicVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsCouponTargetVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsCouponVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsDescItemVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsDescVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsDetailVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsGroupItemVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsImageVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsPointSummaryVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsPriceSummaryVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsShippingSummaryVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsSiteInfoVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsSizeItemVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsWishlistVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageWishGoodsItemVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageWishPageVO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsCategoryItem;
import com.xodud1202.springbackend.domain.admin.goods.GoodsCategorySavePO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsCategoryVO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsDescSaveItem;
import com.xodud1202.springbackend.domain.admin.goods.GoodsDescSavePO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsDescVO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsImageSavePO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsImageVO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsImageOrderItem;
import com.xodud1202.springbackend.domain.admin.goods.GoodsImageOrderSavePO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsMerchVO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsSavePO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsSizeOrderItem;
import com.xodud1202.springbackend.domain.admin.goods.GoodsSizeOrderSavePO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsSizeSavePO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsSizeVO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsVO;
import com.xodud1202.springbackend.mapper.GoodsMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
// 관리자 상품 관련 비즈니스 로직을 처리합니다.
public class GoodsService {
	private final GoodsMapper goodsMapper;
	private final FtpFileService ftpFileService;
	private static final int GOODS_IMAGE_MIN_SIZE = 500;
	private static final int GOODS_IMAGE_MAX_SIZE = 1500;
	private static final String SHOP_SITE_ID = "xodud1202";
	private static final String DEFAULT_CUST_GRADE_CD = "CUST_GRADE_01";
	private static final String TARGET_GB_APPLY = "TARGET_GB_01";
	private static final String TARGET_GB_EXCLUDE = "TARGET_GB_02";
	private static final String CPN_TARGET_ALL = "CPN_TARGET_99";
	private static final String CPN_TARGET_GOODS = "CPN_TARGET_01";
	private static final String CPN_TARGET_BRAND = "CPN_TARGET_04";
	private static final String CPN_TARGET_CATEGORY = "CPN_TARGET_03";
	private static final String CPN_TARGET_EXHIBITION = "CPN_TARGET_02";
	private static final String CPN_GB_GOODS = "CPN_GB_01";
	private static final String CPN_GB_CART = "CPN_GB_03";
	private static final String CPN_GB_DELIVERY = "CPN_GB_04";
	private static final String CPN_DC_GB_AMOUNT = "CPN_DC_GB_01";
	private static final String CPN_DC_GB_RATE = "CPN_DC_GB_02";
	private static final int SHOP_MYPAGE_WISH_PAGE_SIZE = 10;

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
			String nextSuffix = String.format("%02d", nextNumber);
			return nextSuffix;
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

	// 요청 페이지 번호를 1 이상 값으로 보정합니다.
	private int resolveRequestedPageNo(Integer requestedPageNo) {
		if (requestedPageNo == null || requestedPageNo < 1) {
			return 1;
		}
		return requestedPageNo;
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
		List<ShopGoodsCouponVO> couponList = filterAvailableShopGoodsCouponList(goods.getGoodsId(), goods.getBrandNo());

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
	public int addShopGoodsCart(String goodsId, String sizeId, Integer qty, Long custNo) {
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
		int resolvedQty = qty;
		ShopGoodsBasicVO goods = goodsMapper.getShopGoodsBasic(normalizedGoodsId);
		if (goods == null) {
			throw new IllegalArgumentException("상품 정보를 찾을 수 없습니다.");
		}

		// 상품 사이즈 목록에서 요청 사이즈의 유효성과 품절 상태를 확인합니다.
		List<ShopGoodsSizeItemVO> sizeList = goodsMapper.getShopGoodsSizeList(normalizedGoodsId);
		ShopGoodsSizeItemVO targetSize = null;
		for (ShopGoodsSizeItemVO sizeItem : sizeList) {
			if (sizeItem == null) {
				continue;
			}
			if (!normalizedSizeId.equals(sizeItem.getSizeId())) {
				continue;
			}
			targetSize = sizeItem;
			break;
		}
		if (targetSize == null) {
			throw new IllegalArgumentException("사이즈를 확인해주세요.");
		}
		int stockQty = targetSize.getStockQty() == null ? 0 : targetSize.getStockQty();
		if (stockQty < 1) {
			throw new IllegalArgumentException("품절된 사이즈입니다.");
		}

		// 기존 장바구니 존재 여부에 따라 수량 가산 또는 신규 등록을 수행합니다.
		int existedCount = goodsMapper.countShopCart(custNo, normalizedGoodsId, normalizedSizeId);
		if (existedCount > 0) {
			goodsMapper.addShopCartQty(custNo, normalizedGoodsId, normalizedSizeId, resolvedQty, custNo);
		} else {
			goodsMapper.insertShopCart(custNo, normalizedGoodsId, normalizedSizeId, resolvedQty, custNo, custNo);
		}

		// 저장 이후 장바구니 최종 수량을 조회해 반환합니다.
		Integer latestQty = goodsMapper.getShopCartQty(custNo, normalizedGoodsId, normalizedSizeId);
		return latestQty == null ? resolvedQty : latestQty;
	}

	// 쇼핑몰 장바구니 페이지 데이터를 조회합니다.
	public ShopCartPageVO getShopCartPage(Long custNo) {
		// 로그인 고객번호 유효성을 확인합니다.
		if (custNo == null || custNo < 1L) {
			throw new IllegalArgumentException("로그인이 필요합니다.");
		}

		// 장바구니 목록과 배송비 기준 정보를 조회합니다.
		List<ShopCartItemVO> cartItemList = goodsMapper.getShopCartItemList(custNo);
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
		return solveMaximumWeightAssignment(weightMatrix);
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
		// 유효한 행렬이 없으면 0원을 반환합니다.
		if (weightMatrix == null || weightMatrix.length == 0 || weightMatrix[0].length == 0) {
			return 0;
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
		int result = 0;
		for (int row = 1; row <= rowCount; row += 1) {
			int assignedColumn = assignment[row];
			if (assignedColumn < 1 || assignedColumn > columnCount) {
				continue;
			}
			result += paddedMatrix[row][assignedColumn];
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

	// 쇼핑몰 상품상세 쿠폰 목록에서 사용 가능한 상품쿠폰만 필터링합니다.
	private List<ShopGoodsCouponVO> filterAvailableShopGoodsCouponList(String goodsId, Integer brandNo) {
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
			List<ShopGoodsCouponTargetVO> targetList = goodsMapper.getShopCouponTargetList(coupon.getCpnNo());
			if (isMatchedShopGoodsCoupon(coupon, targetList, goodsId, brandNoValue, categoryIdSet, exhibitionTabNoSet)) {
				resultList.add(coupon);
			}
		}
		return resultList;
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

	// 장바구니 쿠폰 예상 할인 계산용 상품 행 컨텍스트를 전달합니다.
	private static class ShopCartCouponEstimateRow {
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
			String goodsId,
			String sizeId,
			int rowSaleAmt,
			String brandNoValue,
			Set<String> categoryIdSet,
			Set<String> exhibitionTabNoSet
		) {
			// 생성자 입력값을 그대로 불변 필드에 보관합니다.
			this.goodsId = goodsId;
			this.sizeId = sizeId;
			this.rowSaleAmt = rowSaleAmt;
			this.brandNoValue = brandNoValue;
			this.categoryIdSet = categoryIdSet == null ? Set.of() : categoryIdSet;
			this.exhibitionTabNoSet = exhibitionTabNoSet == null ? Set.of() : exhibitionTabNoSet;
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
}
