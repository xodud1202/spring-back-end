package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.admin.category.CategoryVO;
import com.xodud1202.springbackend.domain.shop.category.ShopCategoryGoodsItemVO;
import com.xodud1202.springbackend.domain.shop.category.ShopCategoryPageVO;
import com.xodud1202.springbackend.domain.shop.header.ShopHeaderCategoryTreeVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// 카테고리 서비스의 쇼핑몰 헤더 트리/카테고리 페이지 조합 로직을 검증합니다.
class CategoryServiceTests {
	@Mock
	private GoodsService goodsService;

	@InjectMocks
	private CategoryService categoryService;

	@Test
	@DisplayName("카테고리 트리 조회 시 1차-2차-3차 구조로 반환한다")
	// 레벨별 카테고리 조회 결과를 트리 구조로 조합하는지 확인합니다.
	void getShopHeaderCategoryTree_returnsThreeDepthStructure() {
		// 레벨별 카테고리 테스트 데이터를 구성합니다.
		CategoryVO level1 = new CategoryVO();
		level1.setCategoryId("10");
		level1.setCategoryLevel(1);
		level1.setCategoryNm("남성");
		level1.setDispOrd(1);
		level1.setShowYn("Y");

		CategoryVO level2 = new CategoryVO();
		level2.setCategoryId("100001");
		level2.setParentCategoryId("10");
		level2.setCategoryLevel(2);
		level2.setCategoryNm("아우터");
		level2.setDispOrd(1);
		level2.setShowYn("Y");

		CategoryVO level3 = new CategoryVO();
		level3.setCategoryId("1000010001");
		level3.setParentCategoryId("100001");
		level3.setCategoryLevel(3);
		level3.setCategoryNm("롱패딩");
		level3.setDispOrd(1);
		level3.setShowYn("Y");

		// 레벨별 카테고리 조회 결과를 목으로 설정합니다.
		when(goodsService.getCategoryList(1, null)).thenReturn(List.of(level1));
		when(goodsService.getCategoryList(2, "10")).thenReturn(List.of(level2));
		when(goodsService.getCategoryList(3, "100001")).thenReturn(List.of(level3));

		// 카테고리 트리 조회 결과의 계층 구조를 검증합니다.
		List<ShopHeaderCategoryTreeVO> result = categoryService.getShopHeaderCategoryTree();
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getCategoryNm()).isEqualTo("남성");
		assertThat(result.get(0).getChildren()).hasSize(1);
		assertThat(result.get(0).getChildren().get(0).getCategoryNm()).isEqualTo("아우터");
		assertThat(result.get(0).getChildren().get(0).getChildren()).hasSize(1);
		assertThat(result.get(0).getChildren().get(0).getChildren().get(0).getCategoryNm()).isEqualTo("롱패딩");
	}

	@Test
	@DisplayName("카테고리 트리 조회 시 노출 여부가 N인 데이터는 제외한다")
	// 노출 여부 필터와 정렬 규칙이 적용되는지 확인합니다.
	void getShopHeaderCategoryTree_filtersHiddenCategoryAndSorts() {
		// 정렬/노출 테스트용 1차 카테고리 데이터를 구성합니다.
		CategoryVO categoryA = new CategoryVO();
		categoryA.setCategoryId("20");
		categoryA.setCategoryNm("여성");
		categoryA.setDispOrd(2);
		categoryA.setShowYn("Y");

		CategoryVO categoryB = new CategoryVO();
		categoryB.setCategoryId("10");
		categoryB.setCategoryNm("남성");
		categoryB.setDispOrd(1);
		categoryB.setShowYn("Y");

		CategoryVO hiddenCategory = new CategoryVO();
		hiddenCategory.setCategoryId("99");
		hiddenCategory.setCategoryNm("숨김");
		hiddenCategory.setDispOrd(99);
		hiddenCategory.setShowYn("N");

		// 1차 카테고리만 반환하도록 목 데이터를 설정합니다.
		when(goodsService.getCategoryList(1, null)).thenReturn(List.of(categoryA, hiddenCategory, categoryB));
		when(goodsService.getCategoryList(2, "10")).thenReturn(List.of());
		when(goodsService.getCategoryList(2, "20")).thenReturn(List.of());

		// 숨김 카테고리 제외 및 정렬 결과를 검증합니다.
		List<ShopHeaderCategoryTreeVO> result = categoryService.getShopHeaderCategoryTree();
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getCategoryId()).isEqualTo("10");
		assertThat(result.get(1).getCategoryId()).isEqualTo("20");
	}

	@Test
	@DisplayName("카테고리 페이지 조회 시 선택 카테고리 상품 페이징 데이터를 반환한다")
	// 선택 카테고리 기준으로 상품 목록/총건수/페이지 메타가 조합되는지 확인합니다.
	void getShopCategoryPage_returnsSelectedCategoryGoodsPageData() {
		// 카테고리 트리 테스트 데이터를 구성합니다.
		CategoryVO level1 = new CategoryVO();
		level1.setCategoryId("10");
		level1.setCategoryLevel(1);
		level1.setCategoryNm("남성");
		level1.setDispOrd(1);
		level1.setShowYn("Y");

		CategoryVO level2 = new CategoryVO();
		level2.setCategoryId("100001");
		level2.setParentCategoryId("10");
		level2.setCategoryLevel(2);
		level2.setCategoryNm("아우터");
		level2.setDispOrd(1);
		level2.setShowYn("Y");

		when(goodsService.getCategoryList(1, null)).thenReturn(List.of(level1));
		when(goodsService.getCategoryList(2, "10")).thenReturn(List.of(level2));
		when(goodsService.getCategoryList(3, "100001")).thenReturn(List.of());

		// 선택 카테고리 상품 페이징 목 데이터를 설정합니다.
		ShopCategoryGoodsItemVO goodsItem = new ShopCategoryGoodsItemVO();
		goodsItem.setCategoryId("100001");
		goodsItem.setGoodsId("CAMEUEP01BL");
		goodsItem.setGoodsNm("테스트상품");
		goodsItem.setBrandNm("테스트브랜드");
		goodsItem.setSaleAmt(14900);
		when(goodsService.countShopCategoryGoods("100001")).thenReturn(25);
		when(goodsService.getShopCategoryGoodsList("100001", 0, 20)).thenReturn(List.of(goodsItem));

		// 카테고리 페이지 조회 결과를 검증합니다.
		ShopCategoryPageVO result = categoryService.getShopCategoryPage("100001", 1);
		assertThat(result.getSelectedCategoryId()).isEqualTo("100001");
		assertThat(result.getSelectedCategoryNm()).isEqualTo("아우터");
		assertThat(result.getGoodsCount()).isEqualTo(25);
		assertThat(result.getPageNo()).isEqualTo(1);
		assertThat(result.getPageSize()).isEqualTo(20);
		assertThat(result.getTotalPageCount()).isEqualTo(2);
		assertThat(result.getGoodsList()).hasSize(1);
		assertThat(result.getGoodsList().get(0).getGoodsId()).isEqualTo("CAMEUEP01BL");
		verify(goodsService).getShopCategoryGoodsList("100001", 0, 20);
	}

	@Test
	@DisplayName("카테고리 페이지 조회 시 선택 카테고리가 없으면 첫 카테고리와 1페이지로 보정한다")
	// 잘못된 categoryId/pageNo 요청 시 기본 카테고리/1페이지로 보정되는지 확인합니다.
	void getShopCategoryPage_fallsBackToFirstCategoryAndFirstPageWhenInvalidRequest() {
		// 정렬 검증용 1차 카테고리 데이터를 구성합니다.
		CategoryVO first = new CategoryVO();
		first.setCategoryId("10");
		first.setCategoryLevel(1);
		first.setCategoryNm("남성");
		first.setDispOrd(1);
		first.setShowYn("Y");

		CategoryVO second = new CategoryVO();
		second.setCategoryId("20");
		second.setCategoryLevel(1);
		second.setCategoryNm("여성");
		second.setDispOrd(2);
		second.setShowYn("Y");

		when(goodsService.getCategoryList(1, null)).thenReturn(List.of(second, first));
		when(goodsService.getCategoryList(2, "10")).thenReturn(List.of());
		when(goodsService.getCategoryList(2, "20")).thenReturn(List.of());
		when(goodsService.countShopCategoryGoods("10")).thenReturn(0);
		when(goodsService.getShopCategoryGoodsList("10", 0, 20)).thenReturn(List.of());

		// 잘못된 categoryId/null pageNo 전달 시 보정 결과를 검증합니다.
		ShopCategoryPageVO result = categoryService.getShopCategoryPage("999999", null);
		assertThat(result.getSelectedCategoryId()).isEqualTo("10");
		assertThat(result.getSelectedCategoryNm()).isEqualTo("남성");
		assertThat(result.getGoodsCount()).isEqualTo(0);
		assertThat(result.getPageNo()).isEqualTo(1);
		assertThat(result.getTotalPageCount()).isEqualTo(0);
		verify(goodsService).getShopCategoryGoodsList("10", 0, 20);
	}

	@Test
	@DisplayName("카테고리 페이지 조회 시 요청 페이지가 범위를 초과하면 마지막 페이지로 보정한다")
	// 총 페이지 수를 넘어선 페이지 요청 시 마지막 페이지 오프셋이 적용되는지 확인합니다.
	void getShopCategoryPage_clampsPageNoToLastPageWhenOutOfRange() {
		// 카테고리 트리 테스트 데이터를 구성합니다.
		CategoryVO level1 = new CategoryVO();
		level1.setCategoryId("10");
		level1.setCategoryLevel(1);
		level1.setCategoryNm("남성");
		level1.setDispOrd(1);
		level1.setShowYn("Y");

		CategoryVO level2 = new CategoryVO();
		level2.setCategoryId("100001");
		level2.setParentCategoryId("10");
		level2.setCategoryLevel(2);
		level2.setCategoryNm("아우터");
		level2.setDispOrd(1);
		level2.setShowYn("Y");

		when(goodsService.getCategoryList(1, null)).thenReturn(List.of(level1));
		when(goodsService.getCategoryList(2, "10")).thenReturn(List.of(level2));
		when(goodsService.getCategoryList(3, "100001")).thenReturn(List.of());
		when(goodsService.countShopCategoryGoods("100001")).thenReturn(45);
		when(goodsService.getShopCategoryGoodsList("100001", 40, 20)).thenReturn(List.of());

		// 총 3페이지에서 9페이지 요청 시 3페이지로 보정되는지 검증합니다.
		ShopCategoryPageVO result = categoryService.getShopCategoryPage("100001", 9);
		assertThat(result.getPageNo()).isEqualTo(3);
		assertThat(result.getTotalPageCount()).isEqualTo(3);
		assertThat(result.getPageSize()).isEqualTo(20);
		verify(goodsService).getShopCategoryGoodsList("100001", 40, 20);
	}
}