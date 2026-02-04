package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.admin.brand.BrandVO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsPO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsDetailVO;
import com.xodud1202.springbackend.domain.admin.category.CategorySavePO;
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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
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
		int pageSize = 20;
		int offset = (page - 1) * pageSize;

		param.setPage(page);
		param.setPageSize(pageSize);
		param.setOffset(offset);
		param.setSearchKeyword(buildGoodsNameSearchKeyword(param));

		List<GoodsVO> list = goodsMapper.getAdminGoodsList(param);
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
		String maxCategoryId = goodsMapper.getAdminCategoryMaxId(resolvedParentId);
		if (isBlank(maxCategoryId)) {
			// 하위 카테고리가 없으면 상위 코드 + 0001을 반환합니다.
			return "0".equals(resolvedParentId) ? "0001" : resolvedParentId + "0001";
		}
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
