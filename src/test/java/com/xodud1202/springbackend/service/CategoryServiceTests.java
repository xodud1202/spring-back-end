package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.admin.category.CategoryVO;
import com.xodud1202.springbackend.domain.shop.header.ShopHeaderCategoryTreeVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// 카테고리 서비스의 쇼핑몰 헤더 트리 조합 로직을 검증합니다.
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
}
