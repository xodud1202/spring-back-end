package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.admin.category.CategoryGoodsSavePO;
import com.xodud1202.springbackend.domain.admin.category.CategoryVO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsCategoryItem;
import com.xodud1202.springbackend.domain.admin.goods.GoodsCategorySavePO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsSavePO;
import com.xodud1202.springbackend.domain.shop.category.ShopCategoryGoodsItemVO;
import com.xodud1202.springbackend.mapper.GoodsMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// 상품 서비스의 관리자 상품 카테고리 동기화 로직을 검증합니다.
class GoodsServiceTests {
	@Mock
	private GoodsMapper goodsMapper;

	@Mock
	private FtpFileService ftpFileService;

	@InjectMocks
	private GoodsService goodsService;

	@Test
	@DisplayName("관리자 상품 카테고리 단건 저장 시 리프와 상위 카테고리를 category_goods에 함께 저장한다")
	// 상품 상세 팝업 저장 시 선택한 리프 카테고리와 상위 카테고리 매핑을 함께 생성하는지 확인합니다.
	void saveAdminGoodsCategory_insertsLeafAndParentCategoryGoods() {
		// 저장 요청 테스트 데이터를 구성합니다.
		GoodsCategorySavePO param = new GoodsCategorySavePO();
		param.setGoodsId("G-001");
		param.setCategoryId("1000010001");
		param.setDispOrd(7);
		param.setRegNo(10L);
		param.setUdtNo(10L);

		// 카테고리 계층 조회 결과를 목으로 설정합니다.
		mockCategoryHierarchy();
		when(goodsMapper.countCategoryGoods("1000010001", "G-001")).thenReturn(0);
		when(goodsMapper.countCategoryGoods("100001", "G-001")).thenReturn(0);
		when(goodsMapper.countCategoryGoods("10", "G-001")).thenReturn(0);
		when(goodsMapper.insertCategoryGoods(any(CategoryGoodsSavePO.class))).thenReturn(1);

		// 카테고리 저장 시 리프/상위 카테고리 등록 여부를 검증합니다.
		int result = goodsService.saveAdminGoodsCategory(param);
		ArgumentCaptor<CategoryGoodsSavePO> captor = ArgumentCaptor.forClass(CategoryGoodsSavePO.class);
		verify(goodsMapper, times(3)).insertCategoryGoods(captor.capture());
		assertThat(result).isEqualTo(3);
		assertThat(captor.getAllValues())
			.extracting(CategoryGoodsSavePO::getCategoryId)
			.containsExactly("1000010001", "100001", "10");
		assertThat(captor.getAllValues())
			.extracting(CategoryGoodsSavePO::getDispOrd)
			.containsOnly(7);
	}

	@Test
	@DisplayName("관리자 상품 카테고리 일괄 저장 시 기존 category_goods를 비우고 공통 상위 카테고리는 중복 저장하지 않는다")
	// 상품 저장 시 category_goods 전체를 재구성하면서 공유 상위 카테고리를 중복 등록하지 않는지 확인합니다.
	void saveAdminGoodsCategories_rebuildsCategoryGoodsWithoutDuplicateParents() {
		// 일괄 저장 테스트 데이터를 구성합니다.
		GoodsSavePO param = new GoodsSavePO();
		param.setGoodsId("G-001");
		param.setRegNo(99L);
		param.setUdtNo(99L);

		GoodsCategoryItem first = new GoodsCategoryItem();
		first.setCategoryId("1000010001");
		first.setDispOrd(2);

		GoodsCategoryItem second = new GoodsCategoryItem();
		second.setCategoryId("1000010002");
		second.setDispOrd(3);

		param.setCategoryList(List.of(first, second));

		// 카테고리 계층/중복 조회 결과를 목으로 설정합니다.
		mockCategoryHierarchy();
		when(goodsMapper.getAdminCategoryDetail(eq("1000010002"))).thenReturn(createCategory("1000010002", "100001", 3));
		when(goodsMapper.countCategoryGoods("1000010001", "G-001")).thenReturn(0);
		when(goodsMapper.countCategoryGoods("1000010002", "G-001")).thenReturn(0);
		when(goodsMapper.countCategoryGoods("100001", "G-001")).thenReturn(0, 1);
		when(goodsMapper.countCategoryGoods("10", "G-001")).thenReturn(0, 1);
		when(goodsMapper.insertCategoryGoods(any(CategoryGoodsSavePO.class))).thenReturn(1);

		// category_goods 재구성 결과를 검증합니다.
		goodsService.saveAdminGoodsCategories(param);
		ArgumentCaptor<CategoryGoodsSavePO> captor = ArgumentCaptor.forClass(CategoryGoodsSavePO.class);
		verify(goodsMapper).deleteCategoryGoodsByGoodsId("G-001");
		verify(goodsMapper, times(4)).insertCategoryGoods(captor.capture());
		assertThat(captor.getAllValues())
			.extracting(CategoryGoodsSavePO::getCategoryId)
			.containsExactly("1000010001", "100001", "10", "1000010002");
	}

	@Test
	@DisplayName("관리자 상품 카테고리 삭제 시 리프 카테고리와 비어 있는 상위 카테고리를 함께 삭제한다")
	// 상품 상세 팝업 삭제 시 선택한 리프 카테고리 삭제 후 상위 카테고리도 정리되는지 확인합니다.
	void deleteAdminGoodsCategory_deletesLeafAndEmptyParents() {
		// 삭제 요청 테스트 데이터를 구성합니다.
		GoodsCategorySavePO param = new GoodsCategorySavePO();
		param.setGoodsId("G-001");
		param.setCategoryId("1000010001");
		param.setUdtNo(88L);

		// 카테고리 계층/하위 존재 여부를 목으로 설정합니다.
		mockCategoryHierarchy();
		when(goodsMapper.countCategoryGoodsInChildren("100001", "G-001")).thenReturn(0);
		when(goodsMapper.countCategoryGoodsInChildren("10", "G-001")).thenReturn(0);
		when(goodsMapper.deleteCategoryGoods(any(CategoryGoodsSavePO.class))).thenReturn(1);

		// 리프/상위 카테고리 삭제 호출 여부를 검증합니다.
		goodsService.deleteAdminGoodsCategory(param);
		ArgumentCaptor<CategoryGoodsSavePO> captor = ArgumentCaptor.forClass(CategoryGoodsSavePO.class);
		verify(goodsMapper, times(3)).deleteCategoryGoods(captor.capture());
		assertThat(captor.getAllValues())
			.extracting(CategoryGoodsSavePO::getCategoryId)
			.containsExactly("1000010001", "100001", "10");
	}

	@Test
	@DisplayName("쇼핑몰 카테고리 상품 조회 시 첫 번째와 두 번째 이미지 URL을 함께 보정한다")
	// 카테고리 상품 목록 조회 시 primary/secondary 이미지 URL을 모두 응답 객체에 반영하는지 확인합니다.
	void getShopCategoryGoodsList_mapsPrimaryAndSecondaryImageUrls() {
		// 카테고리 상품 테스트 데이터를 구성합니다.
		ShopCategoryGoodsItemVO item = new ShopCategoryGoodsItemVO();
		item.setCategoryId("100001");
		item.setGoodsId("GOODS001");
		item.setGoodsNm("테스트상품");
		item.setImgPath("main.png");
		item.setSecondaryImgPath("sub.png");

		// 매퍼/FTP 응답을 목으로 설정합니다.
		when(goodsMapper.getShopCategoryGoodsList("100001", 0, 20)).thenReturn(List.of(item));
		when(ftpFileService.buildGoodsImageUrl("GOODS001", "main.png")).thenReturn("https://image.test/goods/GOODS001/main.png");
		when(ftpFileService.buildGoodsImageUrl("GOODS001", "sub.png")).thenReturn("https://image.test/goods/GOODS001/sub.png");

		// 이미지 URL 보정 결과를 검증합니다.
		List<ShopCategoryGoodsItemVO> result = goodsService.getShopCategoryGoodsList("100001", 0, 20);
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getImgUrl()).isEqualTo("https://image.test/goods/GOODS001/main.png");
		assertThat(result.get(0).getSecondaryImgUrl()).isEqualTo("https://image.test/goods/GOODS001/sub.png");
	}

	// 테스트용 카테고리 계층 목 데이터를 구성합니다.
	private void mockCategoryHierarchy() {
		// 공통으로 사용하는 1/2/3차 카테고리 계층을 반환하도록 설정합니다.
		when(goodsMapper.getAdminCategoryDetail(eq("10"))).thenReturn(createCategory("10", "0", 1));
		when(goodsMapper.getAdminCategoryDetail(eq("100001"))).thenReturn(createCategory("100001", "10", 2));
		when(goodsMapper.getAdminCategoryDetail(eq("1000010001"))).thenReturn(createCategory("1000010001", "100001", 3));
	}

	// 테스트용 카테고리 객체를 생성합니다.
	private CategoryVO createCategory(String categoryId, String parentCategoryId, Integer categoryLevel) {
		// 카테고리 식별자와 계층 정보를 세팅합니다.
		CategoryVO category = new CategoryVO();
		category.setCategoryId(categoryId);
		category.setParentCategoryId(parentCategoryId);
		category.setCategoryLevel(categoryLevel);
		return category;
	}
}
