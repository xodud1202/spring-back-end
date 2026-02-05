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
import com.xodud1202.springbackend.domain.admin.goods.GoodsCategoryItem;
import com.xodud1202.springbackend.domain.admin.goods.GoodsCategorySavePO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsCategorySaveItem;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
// 관리자 상품 관련 비즈니스 로직을 처리합니다.
public class GoodsService {
	private final GoodsMapper goodsMapper;
	private final FtpFileService ftpFileService;
	private static final int GOODS_IMAGE_MIN_SIZE = 500;
	private static final int GOODS_IMAGE_MAX_SIZE = 1500;

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
	public int saveAdminGoodsCategory(GoodsCategorySavePO param) {
		if (param.getDispOrd() == null) {
			param.setDispOrd(0);
		}
		if (param.getRegNo() == null) {
			param.setRegNo(param.getUdtNo());
		}
		String originCategoryId = param.getOriginCategoryId();
		if (!isBlank(originCategoryId) && !originCategoryId.equals(param.getCategoryId())) {
			GoodsCategorySavePO deleteParam = new GoodsCategorySavePO();
			deleteParam.setGoodsId(param.getGoodsId());
			deleteParam.setCategoryId(originCategoryId);
			goodsMapper.deleteAdminGoodsCategory(deleteParam);
		}
		int exists = goodsMapper.countAdminGoodsCategory(param.getGoodsId(), param.getCategoryId());
		if (exists > 0) {
			return goodsMapper.updateAdminGoodsCategory(param);
		}
		return goodsMapper.insertAdminGoodsCategory(param);
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
	public int deleteAdminGoodsCategory(GoodsCategorySavePO param) {
		return goodsMapper.deleteAdminGoodsCategory(param);
	}

	// 관리자 상품 카테고리를 저장합니다.
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
		goodsMapper.deleteAdminGoodsCategoryByGoodsId(goodsId);
		if (categoryList.isEmpty()) {
			return;
		}
		List<GoodsCategorySaveItem> saveItems = new java.util.ArrayList<>();
		for (int index = 0; index < categoryList.size(); index += 1) {
			GoodsCategoryItem item = categoryList.get(index);
			if (item == null || isBlank(item.getCategoryId())) {
				continue;
			}
			GoodsCategorySaveItem saveItem = new GoodsCategorySaveItem();
			saveItem.setGoodsId(goodsId);
			saveItem.setCategoryId(item.getCategoryId());
			saveItem.setDispOrd(item.getDispOrd() != null ? item.getDispOrd() : index + 1);
			saveItem.setRegNo(writerNo);
			saveItem.setUdtNo(writerNo);
			saveItems.add(saveItem);
		}
		if (!saveItems.isEmpty()) {
			goodsMapper.insertAdminGoodsCategoryList(saveItems);
		}
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
