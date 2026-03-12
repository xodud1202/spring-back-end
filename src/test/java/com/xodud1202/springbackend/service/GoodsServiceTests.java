package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.admin.category.CategoryGoodsSavePO;
import com.xodud1202.springbackend.domain.admin.category.CategoryVO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsCategoryItem;
import com.xodud1202.springbackend.domain.admin.goods.GoodsCategorySavePO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsSavePO;
import com.xodud1202.springbackend.domain.shop.category.ShopCategoryGoodsItemVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsBasicVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsCouponTargetVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsCouponVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsDescItemVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsDetailVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsGroupItemVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsImageVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsSiteInfoVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsSizeItemVO;
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

	@Test
	@DisplayName("쇼핑몰 상품상세 조회 시 가격/포인트/배송비/쿠폰/위시리스트를 조합해 반환한다")
	// 상품상세 상단 응답 조합 로직이 요구사항대로 계산되는지 검증합니다.
	void getShopGoodsDetail_returnsComposedShopGoodsDetail() {
		// 상품 기본/이미지/사이즈/그룹/설명 테스트 데이터를 구성합니다.
		ShopGoodsBasicVO goods = new ShopGoodsBasicVO();
		goods.setGoodsId("CAMEUEP02MG");
		goods.setGoodsNm("테스트 상품");
		goods.setGoodsGroupId("CAMEUEP0");
		goods.setBrandNo(1);
		goods.setBrandLogoPath("https://image.test/brand/logo.png");
		goods.setBrandNoti("<p>브랜드 공지</p>");
		goods.setSupplyAmt(89000);
		goods.setSaleAmt(39900);

		ShopGoodsImageVO image = new ShopGoodsImageVO();
		image.setGoodsId("CAMEUEP02MG");
		image.setImgPath("main.png");
		image.setDispOrd(1);

		ShopGoodsSizeItemVO soldOutSize = new ShopGoodsSizeItemVO();
		soldOutSize.setGoodsId("CAMEUEP02MG");
		soldOutSize.setSizeId("095");
		soldOutSize.setStockQty(0);
		soldOutSize.setDispOrd(1);

		ShopGoodsSizeItemVO normalSize = new ShopGoodsSizeItemVO();
		normalSize.setGoodsId("CAMEUEP02MG");
		normalSize.setSizeId("100");
		normalSize.setStockQty(5);
		normalSize.setDispOrd(2);

		ShopGoodsGroupItemVO groupItem = new ShopGoodsGroupItemVO();
		groupItem.setGoodsId("CAMEUEP01BL");
		groupItem.setFirstImgPath("group.png");

		ShopGoodsDescItemVO pcDesc = new ShopGoodsDescItemVO();
		pcDesc.setDeviceGbCd("PC");
		pcDesc.setGoodsDesc("<p>PC 상세</p>");

		ShopGoodsDescItemVO moDesc = new ShopGoodsDescItemVO();
		moDesc.setDeviceGbCd("MO");
		moDesc.setGoodsDesc("<p>MO 상세</p>");

		ShopGoodsSiteInfoVO siteInfo = new ShopGoodsSiteInfoVO();
		siteInfo.setSiteId("xodud1202");
		siteInfo.setDeliveryFee(3000);
		siteInfo.setDeliveryFeeLimit(30000);

		ShopGoodsCouponVO brandCoupon = new ShopGoodsCouponVO();
		brandCoupon.setCpnNo(1L);
		brandCoupon.setCpnTargetCd("CPN_TARGET_04");

		ShopGoodsCouponTargetVO brandApplyTarget = new ShopGoodsCouponTargetVO();
		brandApplyTarget.setCpnNo(1L);
		brandApplyTarget.setTargetGbCd("TARGET_GB_01");
		brandApplyTarget.setTargetValue("1");

		ShopGoodsCouponVO excludedCoupon = new ShopGoodsCouponVO();
		excludedCoupon.setCpnNo(2L);
		excludedCoupon.setCpnTargetCd("CPN_TARGET_01");

		ShopGoodsCouponTargetVO excludedApplyTarget = new ShopGoodsCouponTargetVO();
		excludedApplyTarget.setCpnNo(2L);
		excludedApplyTarget.setTargetGbCd("TARGET_GB_01");
		excludedApplyTarget.setTargetValue("CAMEUEP02MG");

		ShopGoodsCouponTargetVO excludedTarget = new ShopGoodsCouponTargetVO();
		excludedTarget.setCpnNo(2L);
		excludedTarget.setTargetGbCd("TARGET_GB_02");
		excludedTarget.setTargetValue("CAMEUEP02MG");

		// 매퍼/FTP 목 응답을 설정합니다.
		when(goodsMapper.getShopGoodsBasic("CAMEUEP02MG")).thenReturn(goods);
		when(goodsMapper.getShopGoodsImageList("CAMEUEP02MG")).thenReturn(List.of(image));
		when(goodsMapper.getShopGoodsSizeList("CAMEUEP02MG")).thenReturn(List.of(soldOutSize, normalSize));
		when(goodsMapper.getShopGoodsGroupItemList("CAMEUEP0")).thenReturn(List.of(groupItem));
		when(goodsMapper.getShopGoodsDescItemList("CAMEUEP02MG")).thenReturn(List.of(pcDesc, moDesc));
		when(goodsMapper.countShopWishList(1L, "CAMEUEP02MG")).thenReturn(1);
		when(goodsMapper.getShopGoodsSiteInfo("xodud1202")).thenReturn(siteInfo);
		when(goodsMapper.getShopPointSaveRateByCustGradeCd("CUST_GRADE_03")).thenReturn(5);
		when(goodsMapper.getShopActiveGoodsCouponList()).thenReturn(List.of(brandCoupon, excludedCoupon));
		when(goodsMapper.getShopGoodsCategoryIdList("CAMEUEP02MG")).thenReturn(List.of("2000020002"));
		when(goodsMapper.getShopGoodsExhibitionTabNoList("CAMEUEP02MG")).thenReturn(List.of());
		when(goodsMapper.getShopCouponTargetList(1L)).thenReturn(List.of(brandApplyTarget));
		when(goodsMapper.getShopCouponTargetList(2L)).thenReturn(List.of(excludedApplyTarget, excludedTarget));
		when(ftpFileService.buildGoodsImageUrl("CAMEUEP02MG", "main.png")).thenReturn("https://image.test/goods/CAMEUEP02MG/main.png");
		when(ftpFileService.buildGoodsImageUrl("CAMEUEP01BL", "group.png")).thenReturn("https://image.test/goods/CAMEUEP01BL/group.png");

		// 상품상세 조합 결과를 조회합니다.
		ShopGoodsDetailVO result = goodsService.getShopGoodsDetail("CAMEUEP02MG", 1L, "CUST_GRADE_03");

		// 가격/포인트/배송비/쿠폰/위시리스트 계산 결과를 검증합니다.
		assertThat(result).isNotNull();
		assertThat(result.getPriceSummary().getDiscountRate()).isEqualTo(55);
		assertThat(result.getPointSummary().getExpectedPoint()).isEqualTo(1995);
		assertThat(result.getPointSummary().getPointSaveRate()).isEqualTo(5);
		assertThat(result.getShippingSummary().isFreeDelivery()).isTrue();
		assertThat(result.getWishlist().isWished()).isTrue();
		assertThat(result.getImages().get(0).getImgUrl()).isEqualTo("https://image.test/goods/CAMEUEP02MG/main.png");
		assertThat(result.getGroupGoods().get(0).getFirstImgUrl()).isEqualTo("https://image.test/goods/CAMEUEP01BL/group.png");
		assertThat(result.getSizes().get(0).isSoldOut()).isTrue();
		assertThat(result.getSizes().get(1).isSoldOut()).isFalse();
		assertThat(result.getCoupons()).extracting(ShopGoodsCouponVO::getCpnNo).containsExactly(1L);
		assertThat(result.getDetailDesc().getPcDesc()).isEqualTo("<p>PC 상세</p>");
		assertThat(result.getDetailDesc().getMoDesc()).isEqualTo("<p>MO 상세</p>");
		assertThat(result.getGoods().getBrandLogoPath()).isEqualTo("https://image.test/brand/logo.png");
		assertThat(result.getGoods().getBrandNoti()).isEqualTo("<p>브랜드 공지</p>");
	}

	@Test
	@DisplayName("쇼핑몰 상품상세 조회 시 고객등급이 없으면 WELCOME 적립률을 사용한다")
	// 고객등급 쿠키가 없는 경우 기본 등급 기준 포인트가 계산되는지 검증합니다.
	void getShopGoodsDetail_usesWelcomeGradeWhenCustGradeMissing() {
		// 최소 상품 테스트 데이터를 구성합니다.
		ShopGoodsBasicVO goods = new ShopGoodsBasicVO();
		goods.setGoodsId("GOODS001");
		goods.setGoodsNm("기본 상품");
		goods.setGoodsGroupId("");
		goods.setSaleAmt(10000);
		goods.setSupplyAmt(10000);

		// 매퍼 기본 응답을 설정합니다.
		when(goodsMapper.getShopGoodsBasic("GOODS001")).thenReturn(goods);
		when(goodsMapper.getShopGoodsImageList("GOODS001")).thenReturn(List.of());
		when(goodsMapper.getShopGoodsSizeList("GOODS001")).thenReturn(List.of());
		when(goodsMapper.getShopGoodsDescItemList("GOODS001")).thenReturn(List.of());
		when(goodsMapper.getShopGoodsSiteInfo("xodud1202")).thenReturn(null);
		when(goodsMapper.getShopPointSaveRateByCustGradeCd("CUST_GRADE_01")).thenReturn(2);
		when(goodsMapper.getShopActiveGoodsCouponList()).thenReturn(List.of());

		// 고객등급 없이 상품상세를 조회합니다.
		ShopGoodsDetailVO result = goodsService.getShopGoodsDetail("GOODS001", null, null);

		// WELCOME 등급 기준 포인트 계산 결과를 검증합니다.
		assertThat(result).isNotNull();
		assertThat(result.getPointSummary().getCustGradeCd()).isEqualTo("CUST_GRADE_01");
		assertThat(result.getPointSummary().getPointSaveRate()).isEqualTo(2);
		assertThat(result.getPointSummary().getExpectedPoint()).isEqualTo(200);
		assertThat(result.getWishlist().isWished()).isFalse();
	}

	@Test
	@DisplayName("쇼핑몰 상품상세 조회 시 상품 기본 정보가 없으면 null을 반환한다")
	// 조회 가능한 상품이 없을 때 null 반환으로 404 처리 가능한지 검증합니다.
	void getShopGoodsDetail_returnsNullWhenGoodsMissing() {
		// 상품 기본 정보를 찾지 못한 상황을 목으로 설정합니다.
		when(goodsMapper.getShopGoodsBasic("NOT_FOUND")).thenReturn(null);

		// 상품상세 조회 결과가 null인지 확인합니다.
		assertThat(goodsService.getShopGoodsDetail("NOT_FOUND", null, null)).isNull();
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
