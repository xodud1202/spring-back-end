package com.xodud1202.springbackend.service;

import static com.xodud1202.springbackend.common.util.CommonTextUtils.*;

import com.xodud1202.springbackend.domain.admin.category.CategoryGoodsDeletePO;
import com.xodud1202.springbackend.domain.admin.category.CategoryGoodsOrderSavePO;
import com.xodud1202.springbackend.domain.admin.category.CategoryGoodsRegisterPO;
import com.xodud1202.springbackend.domain.admin.category.CategoryGoodsVO;
import com.xodud1202.springbackend.domain.admin.category.CategorySavePO;
import com.xodud1202.springbackend.domain.admin.category.CategoryVO;
import com.xodud1202.springbackend.domain.shop.category.ShopCategoryGoodsItemVO;
import com.xodud1202.springbackend.domain.shop.category.ShopCategoryPageVO;
import com.xodud1202.springbackend.domain.shop.header.ShopHeaderCategoryTreeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// 카테고리 도메인의 공통 비즈니스 로직을 처리합니다.
@Service
@RequiredArgsConstructor
public class CategoryService {
	private static final int SHOP_CATEGORY_PAGE_SIZE = 20;
	private static final String SHOP_CATEGORY_ROOT_PARENT_KEY = "__ROOT__";
	private final GoodsService goodsService;

	// 프로젝트 공통 카테고리 목록을 조회합니다.
	public List<CategoryVO> getCategoryList(Integer categoryLevel, String parentCategoryId) {
		// 상품 서비스의 카테고리 조회 기능을 재사용합니다.
		return goodsService.getCategoryList(categoryLevel, parentCategoryId);
	}

	// 관리자 카테고리 트리 목록을 조회합니다.
	public List<CategoryVO> getAdminCategoryTreeList() {
		// 관리자 카테고리 트리 목록을 반환합니다.
		return goodsService.getAdminCategoryTreeList();
	}

	// 관리자 카테고리 상세 정보를 조회합니다.
	public CategoryVO getAdminCategoryDetail(String categoryId) {
		// 관리자 카테고리 상세 정보를 반환합니다.
		return goodsService.getAdminCategoryDetail(categoryId);
	}

	// 관리자 카테고리 다음 코드를 조회합니다.
	public String getNextAdminCategoryId(String parentCategoryId) {
		// 상위 카테고리 기준 다음 카테고리 코드를 반환합니다.
		return goodsService.getNextAdminCategoryId(parentCategoryId);
	}

	// 관리자 카테고리 등록 요청을 검증합니다.
	public String validateAdminCategoryCreate(CategorySavePO param) {
		// 카테고리 등록 요청값 유효성을 검증합니다.
		return goodsService.validateAdminCategoryCreate(param);
	}

	// 관리자 카테고리 수정 요청을 검증합니다.
	public String validateAdminCategoryUpdate(CategorySavePO param) {
		// 카테고리 수정 요청값 유효성을 검증합니다.
		return goodsService.validateAdminCategoryUpdate(param);
	}

	// 관리자 카테고리 삭제 요청을 검증합니다.
	public String validateAdminCategoryDelete(CategorySavePO param) {
		// 카테고리 삭제 요청값 유효성을 검증합니다.
		return goodsService.validateAdminCategoryDelete(param);
	}

	// 관리자 카테고리를 등록합니다.
	public int createAdminCategory(CategorySavePO param) {
		// 카테고리 등록 처리를 수행합니다.
		return goodsService.createAdminCategory(param);
	}

	// 관리자 카테고리를 수정합니다.
	public int updateAdminCategory(CategorySavePO param) {
		// 카테고리 수정 처리를 수행합니다.
		return goodsService.updateAdminCategory(param);
	}

	// 관리자 카테고리를 삭제 처리합니다.
	public int deleteAdminCategory(CategorySavePO param) {
		// 카테고리 삭제 처리를 수행합니다.
		return goodsService.deleteAdminCategory(param);
	}

	// 카테고리별 상품 목록을 조회합니다.
	public List<CategoryGoodsVO> getAdminCategoryGoodsList(String categoryId) {
		// 카테고리별 상품 목록을 반환합니다.
		return goodsService.getAdminCategoryGoodsList(categoryId);
	}

	// 카테고리 상품 정렬 순서 저장 요청을 검증합니다.
	public String validateCategoryGoodsOrderSave(CategoryGoodsOrderSavePO param) {
		// 정렬 순서 저장 요청값 유효성을 검증합니다.
		return goodsService.validateCategoryGoodsOrderSave(param);
	}

	// 카테고리 상품 정렬 순서를 저장합니다.
	public int saveCategoryGoodsOrder(CategoryGoodsOrderSavePO param) {
		// 카테고리 상품 정렬 순서를 저장합니다.
		return goodsService.saveCategoryGoodsOrder(param);
	}

	// 카테고리 상품 등록 요청을 검증합니다.
	public String validateCategoryGoodsRegister(CategoryGoodsRegisterPO param) {
		// 카테고리 상품 등록 요청값 유효성을 검증합니다.
		return goodsService.validateCategoryGoodsRegister(param);
	}

	// 카테고리 상품을 등록합니다.
	public int registerCategoryGoods(CategoryGoodsRegisterPO param) {
		// 카테고리 상품 등록 처리를 수행합니다.
		return goodsService.registerCategoryGoods(param);
	}

	// 카테고리 상품 삭제 요청을 검증합니다.
	public String validateCategoryGoodsDelete(CategoryGoodsDeletePO param) {
		// 카테고리 상품 삭제 요청값 유효성을 검증합니다.
		return goodsService.validateCategoryGoodsDelete(param);
	}

	// 카테고리 상품을 삭제합니다.
	public int deleteCategoryGoods(CategoryGoodsDeletePO param) {
		// 카테고리 상품 삭제 처리를 수행합니다.
		return goodsService.deleteCategoryGoods(param);
	}

	// 카테고리 상품 엑셀 업로드 요청을 검증합니다.
	public String validateCategoryGoodsExcelUpload(MultipartFile file, Long regNo, Long udtNo) {
		// 엑셀 업로드 요청값 유효성을 검증합니다.
		return goodsService.validateCategoryGoodsExcelUpload(file, regNo, udtNo);
	}

	// 카테고리 상품 엑셀 파일을 업로드합니다.
	public Map<String, Object> uploadCategoryGoodsExcel(MultipartFile file, Long regNo, Long udtNo) throws IOException {
		// 엑셀 업로드 결과를 반환합니다.
		return goodsService.uploadCategoryGoodsExcel(file, regNo, udtNo);
	}

	// 카테고리 상품 엑셀 파일 바이트 배열을 생성합니다.
	public byte[] buildCategoryGoodsExcel(String categoryId) throws IOException {
		// 카테고리 상품 엑셀 파일 바이트 배열을 반환합니다.
		return goodsService.buildCategoryGoodsExcel(categoryId);
	}

	// 쇼핑몰 헤더 카테고리 트리를 조회합니다.
	public List<ShopHeaderCategoryTreeVO> getShopHeaderCategoryTree() {
		// 노출 가능한 전체 카테고리를 한 번에 조회하고 빈 데이터일 경우 즉시 반환합니다.
		List<CategoryVO> visibleCategoryList = getShopVisibleCategoryList();
		if (visibleCategoryList.isEmpty()) {
			return List.of();
		}

		// 부모 카테고리 기준으로 하위 목록을 그룹화한 뒤 1/2/3차 트리로 조합합니다.
		Map<String, List<CategoryVO>> categoryListByParentIdMap = groupShopCategoryListByParentId(visibleCategoryList);
		return buildShopHeaderCategoryTreeList(categoryListByParentIdMap, SHOP_CATEGORY_ROOT_PARENT_KEY);
	}

	// 쇼핑몰 카테고리 페이지 데이터를 조회합니다.
	public ShopCategoryPageVO getShopCategoryPage(String selectedCategoryId, Integer requestedPageNo) {
		// 쇼핑몰 카테고리 트리를 조회합니다.
		List<ShopHeaderCategoryTreeVO> categoryTree = getShopHeaderCategoryTree();
		// 카테고리 아이디/명 매핑을 생성합니다.
		Map<String, String> categoryNameByIdMap = new LinkedHashMap<>();
		collectCategoryNameById(categoryTree, categoryNameByIdMap);

		// 요청 카테고리 아이디를 유효한 선택값으로 보정합니다.
		String resolvedSelectedCategoryId = resolveSelectedCategoryId(selectedCategoryId, categoryNameByIdMap);
		// 요청 페이지 번호를 1 이상으로 보정합니다.
		int resolvedRequestedPageNo = resolveRequestedPageNo(requestedPageNo);
		// 선택 카테고리의 전체 상품 건수를 조회합니다.
		int goodsCount = goodsService.countShopCategoryGoods(resolvedSelectedCategoryId);
		// 전체 페이지 수를 계산합니다.
		int totalPageCount = calculateTotalPageCount(goodsCount, SHOP_CATEGORY_PAGE_SIZE);
		// 범위를 초과한 페이지 번호를 마지막 페이지로 보정합니다.
		int resolvedPageNo = totalPageCount == 0 ? 1 : Math.min(resolvedRequestedPageNo, totalPageCount);
		// 페이지 조회 오프셋을 계산합니다.
		int offset = calculateOffset(resolvedPageNo, SHOP_CATEGORY_PAGE_SIZE);
		// 선택 카테고리의 페이징 상품 목록을 조회합니다.
		List<ShopCategoryGoodsItemVO> goodsList = goodsService.getShopCategoryGoodsList(resolvedSelectedCategoryId, offset, SHOP_CATEGORY_PAGE_SIZE);

		// 카테고리 페이지 응답 객체를 구성합니다.
		ShopCategoryPageVO result = new ShopCategoryPageVO();
		result.setCategoryTree(categoryTree);
		result.setSelectedCategoryId(resolvedSelectedCategoryId);
		result.setSelectedCategoryNm(categoryNameByIdMap.getOrDefault(resolvedSelectedCategoryId, ""));
		result.setGoodsList(goodsList);
		result.setGoodsCount(goodsCount);
		result.setPageNo(resolvedPageNo);
		result.setPageSize(SHOP_CATEGORY_PAGE_SIZE);
		result.setTotalPageCount(totalPageCount);
		return result;
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

	// 선택 카테고리 아이디를 유효한 값으로 보정합니다.
	private String resolveSelectedCategoryId(String selectedCategoryId, Map<String, String> categoryNameByIdMap) {
		// 요청 카테고리가 존재하면 그대로 사용합니다.
		if (!isBlank(selectedCategoryId) && categoryNameByIdMap.containsKey(selectedCategoryId)) {
			return selectedCategoryId;
		}
		// 요청 카테고리가 없거나 유효하지 않으면 첫 번째 카테고리를 기본값으로 사용합니다.
		for (String categoryId : categoryNameByIdMap.keySet()) {
			return categoryId;
		}
		// 카테고리가 없으면 빈 문자열을 반환합니다.
		return "";
	}

	// 카테고리 트리를 순회해 카테고리 아이디/명을 수집합니다.
	private void collectCategoryNameById(List<ShopHeaderCategoryTreeVO> categoryTree, Map<String, String> categoryNameByIdMap) {
		if (categoryTree == null || categoryTree.isEmpty()) {
			return;
		}
		for (ShopHeaderCategoryTreeVO category : categoryTree) {
			if (category == null || isBlank(category.getCategoryId())) {
				continue;
			}
			categoryNameByIdMap.put(category.getCategoryId(), category.getCategoryNm() == null ? "" : category.getCategoryNm());
			collectCategoryNameById(category.getChildren(), categoryNameByIdMap);
		}
	}
	
	// 쇼핑몰 헤더 노출용 전체 카테고리를 한 번에 조회해 정렬 후 반환합니다.
	private List<CategoryVO> getShopVisibleCategoryList() {
		// 전체 카테고리 목록을 한 번에 조회하고 빈 데이터일 경우 즉시 반환합니다.
		List<CategoryVO> sourceList = this.getCategoryList(null, null);
		if (sourceList == null || sourceList.isEmpty()) {
			return List.of();
		}

		// 노출 여부가 Y인 데이터만 정렬 기준에 맞춰 반환합니다.
		return sourceList.stream()
			.filter(Objects::nonNull)
			.filter(item -> "Y".equals(item.getShowYn()))
			.sorted(
				Comparator.comparing(
					CategoryVO::getDispOrd,
					Comparator.nullsLast(Integer::compareTo)
				).thenComparing(CategoryVO::getCategoryId, Comparator.nullsLast(String::compareTo))
			)
			.toList();
	}

	// 부모 카테고리 기준으로 카테고리 목록을 그룹화합니다.
	private Map<String, List<CategoryVO>> groupShopCategoryListByParentId(List<CategoryVO> categoryList) {
		Map<String, List<CategoryVO>> result = new LinkedHashMap<>();
		if (categoryList == null || categoryList.isEmpty()) {
			return result;
		}

		// 상위 카테고리 아이디별로 정렬된 카테고리 목록을 유지하며 묶습니다.
		for (CategoryVO category : categoryList) {
			if (category == null || isBlank(category.getCategoryId())) {
				continue;
			}
			String parentKey = resolveShopCategoryParentKey(category.getParentCategoryId());
			result.computeIfAbsent(parentKey, key -> new ArrayList<>()).add(category);
		}
		return result;
	}

	// 부모 카테고리 그룹 맵을 재귀 순회해 쇼핑몰 헤더 트리를 생성합니다.
	private List<ShopHeaderCategoryTreeVO> buildShopHeaderCategoryTreeList(
		Map<String, List<CategoryVO>> categoryListByParentIdMap,
		String parentKey
	) {
		List<CategoryVO> childCategoryList = categoryListByParentIdMap.getOrDefault(parentKey, List.of());
		if (childCategoryList.isEmpty()) {
			return List.of();
		}

		// 현재 부모의 하위 카테고리를 순회하며 최대 3차까지 자식 트리를 연결합니다.
		List<ShopHeaderCategoryTreeVO> result = new ArrayList<>();
		for (CategoryVO category : childCategoryList) {
			if (category == null || category.getCategoryLevel() == null || category.getCategoryLevel() < 1 || category.getCategoryLevel() > 3) {
				continue;
			}

			ShopHeaderCategoryTreeVO node = toShopHeaderCategoryTree(category);
			if (category.getCategoryLevel() < 3) {
				node.setChildren(buildShopHeaderCategoryTreeList(categoryListByParentIdMap, category.getCategoryId()));
			}
			result.add(node);
		}
		return result;
	}

	// 상위 카테고리 아이디를 헤더 트리 조합용 그룹 키로 변환합니다.
	private String resolveShopCategoryParentKey(String parentCategoryId) {
		if (isBlank(parentCategoryId)) {
			return SHOP_CATEGORY_ROOT_PARENT_KEY;
		}

		// 실제 DB에서 1차 카테고리 상위값은 "0"으로 관리되므로 루트 키로 취급합니다.
		String trimmedParentCategoryId = parentCategoryId.trim();
		return "0".equals(trimmedParentCategoryId) ? SHOP_CATEGORY_ROOT_PARENT_KEY : trimmedParentCategoryId;
	}

	// 카테고리 도메인 객체를 쇼핑몰 헤더 트리 응답 객체로 변환합니다.
	private ShopHeaderCategoryTreeVO toShopHeaderCategoryTree(CategoryVO source) {
		// 쇼핑몰 헤더 트리 응답 객체를 생성하고 필수 필드를 매핑합니다.
		ShopHeaderCategoryTreeVO target = new ShopHeaderCategoryTreeVO();
		target.setCategoryId(source.getCategoryId());
		target.setCategoryNm(source.getCategoryNm());
		target.setCategoryLevel(source.getCategoryLevel());
		target.setDispOrd(source.getDispOrd());
		target.setChildren(List.of());
		return target;
	}
}
