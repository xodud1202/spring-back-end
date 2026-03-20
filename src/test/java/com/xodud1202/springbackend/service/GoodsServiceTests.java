package com.xodud1202.springbackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xodud1202.springbackend.domain.admin.category.CategoryGoodsSavePO;
import com.xodud1202.springbackend.domain.admin.category.CategoryVO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsCategoryItem;
import com.xodud1202.springbackend.domain.admin.goods.GoodsCategorySavePO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsSizeVO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsSavePO;
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
import com.xodud1202.springbackend.domain.shop.cart.ShopCartSavePO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartSiteInfoVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsBasicVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsCouponTargetVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsCouponVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsDescItemVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsDetailVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsGroupItemVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsImageVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsSiteInfoVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsSizeItemVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageCouponDownloadRequestPO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageCouponPageVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageCouponUnavailableGoodsVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageDownloadableCouponVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOwnedCouponVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderDetailItemVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderGroupVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderPageVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderStatusSummaryVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageWishGoodsItemVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageWishPageVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderAddressRegisterPO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderAddressSavePO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderAddressSaveResultVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderAddressSearchCommonVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderAddressSearchResponseVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderAddressUpdatePO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderCustomerInfoVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderAddressVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderDiscountSelectionVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderDiscountQuotePO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderDiscountQuoteVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderGoodsCouponSelectionVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderPageVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderPaymentConfirmVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderPaymentConfirmPO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderPaymentPreparePO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderPaymentVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderRestoreCartItemVO;
import com.xodud1202.springbackend.mapper.ExhibitionMapper;
import com.xodud1202.springbackend.mapper.GoodsMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// 상품 서비스의 관리자 상품 카테고리 동기화 로직을 검증합니다.
class GoodsServiceTests {
	@Mock
	private GoodsMapper goodsMapper;

	@Mock
	private ExhibitionMapper exhibitionMapper;

	@Mock
	private FtpFileService ftpFileService;

	@Mock
	private ShopAuthService shopAuthService;

	@Mock
	private JusoAddressApiClient jusoAddressApiClient;

	@Mock
	private TossPaymentsClient tossPaymentsClient;

	@Spy
	private ObjectMapper objectMapper = new ObjectMapper();

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
	@DisplayName("쇼핑몰 마이페이지 위시리스트 조회 시 10개 페이지 기준으로 최근 등록순 목록을 반환한다")
	// 위시리스트 페이지 조회 시 건수/페이지/이미지 URL 보정 결과를 검증합니다.
	void getShopMypageWishPage_returnsPagedWishGoods() {
		// 위시리스트 상품 테스트 데이터를 구성합니다.
		ShopMypageWishGoodsItemVO firstItem = new ShopMypageWishGoodsItemVO();
		firstItem.setCustNo(7L);
		firstItem.setGoodsId("GOODS200");
		firstItem.setGoodsNm("최근등록상품");
		firstItem.setSaleAmt(19900);
		firstItem.setImgPath("wish-main.png");

		ShopMypageWishGoodsItemVO secondItem = new ShopMypageWishGoodsItemVO();
		secondItem.setCustNo(7L);
		secondItem.setGoodsId("GOODS100");
		secondItem.setGoodsNm("이전등록상품");
		secondItem.setSaleAmt(9900);
		secondItem.setImgPath("");

		// 매퍼/FTP 목 응답을 설정합니다.
		when(goodsMapper.countShopMypageWishGoods(7L)).thenReturn(11);
		when(goodsMapper.getShopMypageWishGoodsList(7L, 0, 10)).thenReturn(List.of(firstItem, secondItem));
		when(ftpFileService.buildGoodsImageUrl("GOODS200", "wish-main.png")).thenReturn("https://image.test/goods/GOODS200/wish-main.png");

		// 위시리스트 페이지를 조회합니다.
		ShopMypageWishPageVO result = goodsService.getShopMypageWishPage(7L, 1);

		// 건수/페이지 정보와 이미지 URL 보정 결과를 검증합니다.
		assertThat(result).isNotNull();
		assertThat(result.getGoodsCount()).isEqualTo(11);
		assertThat(result.getPageNo()).isEqualTo(1);
		assertThat(result.getPageSize()).isEqualTo(10);
		assertThat(result.getTotalPageCount()).isEqualTo(2);
		assertThat(result.getGoodsList()).hasSize(2);
		assertThat(result.getGoodsList().get(0).getGoodsId()).isEqualTo("GOODS200");
		assertThat(result.getGoodsList().get(0).getImgUrl()).isEqualTo("https://image.test/goods/GOODS200/wish-main.png");
		assertThat(result.getGoodsList().get(1).getGoodsId()).isEqualTo("GOODS100");
		assertThat(result.getGoodsList().get(1).getImgUrl()).isEqualTo("");
	}

	@Test
	@DisplayName("쇼핑몰 마이페이지 위시리스트 삭제 시 고객번호와 상품코드로 삭제를 수행한다")
	// 위시리스트 삭제 요청 시 delete 매퍼가 호출되는지 검증합니다.
	void deleteShopMypageWishGoods_deletesWishItem() {
		// 삭제 대상 고객번호/상품코드를 준비합니다.
		String goodsId = " GOODS001 ";
		Long custNo = 7L;

		// 삭제 처리를 수행합니다.
		goodsService.deleteShopMypageWishGoods(goodsId, custNo);

		// trim 처리된 상품코드로 삭제 호출되는지 검증합니다.
		verify(goodsMapper, times(1)).deleteShopWishList(7L, "GOODS001");
	}

	@Test
	@DisplayName("쇼핑몰 마이페이지 쿠폰함 조회 시 탭별 페이징 정보와 쿠폰 사용 불가 상품 요약을 함께 반환한다")
	// 쿠폰함 페이지 조회 시 탭별 페이지 정보와 카드 툴팁용 사용 불가 상품 목록이 함께 구성되는지 검증합니다.
	void getShopMypageCouponPage_returnsPagedCouponListsWithUnavailableGoodsSummary() {
		// 사용 가능 보유 쿠폰과 다운로드 가능 쿠폰 테스트 데이터를 구성합니다.
		ShopMypageOwnedCouponVO ownedCoupon = new ShopMypageOwnedCouponVO();
		ownedCoupon.setCustCpnNo(1001L);
		ownedCoupon.setCpnNo(11L);
		ownedCoupon.setCpnNm("보유 상품 쿠폰");
		ownedCoupon.setCpnGbNm("상품 쿠폰");
		ownedCoupon.setCpnTargetCd("CPN_TARGET_01");

		ShopMypageDownloadableCouponVO downloadableCoupon = new ShopMypageDownloadableCouponVO();
		downloadableCoupon.setCpnNo(21L);
		downloadableCoupon.setCpnNm("다운로드 장바구니 쿠폰");
		downloadableCoupon.setCpnGbNm("장바구니 쿠폰");
		downloadableCoupon.setCpnTargetCd("CPN_TARGET_04");

		ShopGoodsCouponTargetVO ownedExcludeTarget = createCouponTarget("TARGET_GB_02", "GOODS001");
		ShopGoodsCouponTargetVO downloadableExcludeTarget = createCouponTarget("TARGET_GB_02", "1");
		ShopMypageCouponUnavailableGoodsVO ownedUnavailableGoods = new ShopMypageCouponUnavailableGoodsVO();
		ownedUnavailableGoods.setGoodsId("GOODS001");
		ownedUnavailableGoods.setGoodsNm("사용 불가 상품");
		ShopMypageCouponUnavailableGoodsVO downloadableUnavailableGoods = new ShopMypageCouponUnavailableGoodsVO();
		downloadableUnavailableGoods.setGoodsId("GOODS002");
		downloadableUnavailableGoods.setGoodsNm("브랜드 제외 상품");

		// 매퍼 응답을 탭별 건수/목록과 사용 불가 상품 요약으로 설정합니다.
		when(goodsMapper.countShopMypageOwnedCoupon(7L)).thenReturn(12);
		when(goodsMapper.countShopMypageDownloadableCoupon()).thenReturn(3);
		when(goodsMapper.getShopMypageOwnedCouponPageList(7L, 10, 10)).thenReturn(List.of(ownedCoupon));
		when(goodsMapper.getShopMypageDownloadableCouponPageList(0, 10)).thenReturn(List.of(downloadableCoupon));
		when(goodsMapper.getShopCouponTargetList(11L)).thenReturn(List.of(ownedExcludeTarget));
		when(goodsMapper.getShopCouponTargetList(21L)).thenReturn(List.of(downloadableExcludeTarget));
		when(goodsMapper.countShopMypageCouponUnavailableGoods("CPN_TARGET_01", List.of("GOODS001"))).thenReturn(1);
		when(goodsMapper.getShopMypageCouponUnavailableGoodsList("CPN_TARGET_01", List.of("GOODS001"), 10))
			.thenReturn(List.of(ownedUnavailableGoods));
		when(goodsMapper.countShopMypageCouponUnavailableGoods("CPN_TARGET_04", List.of("1"))).thenReturn(12);
		when(goodsMapper.getShopMypageCouponUnavailableGoodsList("CPN_TARGET_04", List.of("1"), 10))
			.thenReturn(List.of(downloadableUnavailableGoods));

		// 쿠폰함 페이지 데이터를 조회합니다.
		ShopMypageCouponPageVO result = goodsService.getShopMypageCouponPage(7L, 2, 1);

		// 탭별 페이지 정보와 사용 불가 상품 요약이 함께 반환되는지 검증합니다.
		assertThat(result).isNotNull();
		assertThat(result.getOwnedCouponCount()).isEqualTo(12);
		assertThat(result.getOwnedPageNo()).isEqualTo(2);
		assertThat(result.getOwnedTotalPageCount()).isEqualTo(2);
		assertThat(result.getOwnedCouponList()).hasSize(1);
		assertThat(result.getOwnedCouponList().get(0).getCustCpnNo()).isEqualTo(1001L);
		assertThat(result.getOwnedCouponList().get(0).getUnavailableGoodsCount()).isEqualTo(1);
		assertThat(result.getOwnedCouponList().get(0).getUnavailableGoodsList()).hasSize(1);
		assertThat(result.getOwnedCouponList().get(0).getUnavailableGoodsList().get(0).getGoodsId()).isEqualTo("GOODS001");
		assertThat(result.getDownloadableCouponCount()).isEqualTo(3);
		assertThat(result.getDownloadablePageNo()).isEqualTo(1);
		assertThat(result.getDownloadableTotalPageCount()).isEqualTo(1);
		assertThat(result.getDownloadableCouponList()).hasSize(1);
		assertThat(result.getDownloadableCouponList().get(0).getCpnNo()).isEqualTo(21L);
		assertThat(result.getDownloadableCouponList().get(0).getUnavailableGoodsCount()).isEqualTo(12);
		assertThat(result.getDownloadableCouponList().get(0).getUnavailableGoodsList()).hasSize(1);
		assertThat(result.getDownloadableCouponList().get(0).getUnavailableGoodsList().get(0).getGoodsId()).isEqualTo("GOODS002");
	}

	@Test
	@DisplayName("쇼핑몰 마이페이지 주문내역 조회 시 주문번호 5건 단위 페이징과 주문상세 묶음 데이터를 반환한다")
	// 주문내역 조회 시 주문번호 기준 페이징, 혼합 주문의 취소상세 포함, 상태 요약과 이미지 URL 보정이 함께 적용되는지 검증합니다.
	void getShopMypageOrderPage_returnsPagedOrderGroupsWithSummary() {
		// 주문번호 목록과 주문상세 목록 테스트 데이터를 구성합니다.
		ShopMypageOrderGroupVO secondPageFirstOrder = new ShopMypageOrderGroupVO();
		secondPageFirstOrder.setOrdNo("ORD-0007");
		secondPageFirstOrder.setOrderDt("2026-03-20 11:40:31");

		ShopMypageOrderGroupVO secondPageSecondOrder = new ShopMypageOrderGroupVO();
		secondPageSecondOrder.setOrdNo("ORD-0006");
		secondPageSecondOrder.setOrderDt("2026-03-19 17:09:15");

		ShopMypageOrderDetailItemVO firstDetail = new ShopMypageOrderDetailItemVO();
		firstDetail.setOrdNo("ORD-0007");
		firstDetail.setOrdDtlNo(1);
		firstDetail.setOrdDtlStatCd("ORD_DTL_STAT_02");
		firstDetail.setOrdDtlStatNm("결제 완료");
		firstDetail.setGoodsId("GOODS001");
		firstDetail.setGoodsNm("주문상품1");
		firstDetail.setSizeId("095");
		firstDetail.setOrdQty(2);
		firstDetail.setSaleAmt(12000);
		firstDetail.setAddAmt(500);
		firstDetail.setImgPath("goods-1.png");

		ShopMypageOrderDetailItemVO pendingDetail = new ShopMypageOrderDetailItemVO();
		pendingDetail.setOrdNo("ORD-0007");
		pendingDetail.setOrdDtlNo(0);
		pendingDetail.setOrdDtlStatCd("ORD_DTL_STAT_00");
		pendingDetail.setOrdDtlStatNm("주문 접수");
		pendingDetail.setGoodsId("GOODS009");
		pendingDetail.setGoodsNm("주문상품0");
		pendingDetail.setSizeId("090");
		pendingDetail.setOrdQty(1);
		pendingDetail.setSaleAmt(10000);
		pendingDetail.setAddAmt(0);
		pendingDetail.setImgPath("goods-0.png");

		ShopMypageOrderDetailItemVO cancelledDetail = new ShopMypageOrderDetailItemVO();
		cancelledDetail.setOrdNo("ORD-0007");
		cancelledDetail.setOrdDtlNo(2);
		cancelledDetail.setOrdDtlStatCd("ORD_DTL_STAT_99");
		cancelledDetail.setOrdDtlStatNm("주문 취소");
		cancelledDetail.setGoodsId("GOODS002");
		cancelledDetail.setGoodsNm("주문상품2");
		cancelledDetail.setSizeId("100");
		cancelledDetail.setOrdQty(1);
		cancelledDetail.setSaleAmt(18000);
		cancelledDetail.setAddAmt(0);
		cancelledDetail.setImgPath("goods-2.png");

		ShopMypageOrderDetailItemVO deliveredDetail = new ShopMypageOrderDetailItemVO();
		deliveredDetail.setOrdNo("ORD-0006");
		deliveredDetail.setOrdDtlNo(1);
		deliveredDetail.setOrdDtlStatCd("ORD_DTL_STAT_06");
		deliveredDetail.setOrdDtlStatNm("배송완료");
		deliveredDetail.setGoodsId("GOODS003");
		deliveredDetail.setGoodsNm("주문상품3");
		deliveredDetail.setSizeId("M");
		deliveredDetail.setOrdQty(3);
		deliveredDetail.setSaleAmt(21000);
		deliveredDetail.setAddAmt(1000);
		deliveredDetail.setImgPath("goods-3.png");

		ShopMypageOrderStatusSummaryVO summary = new ShopMypageOrderStatusSummaryVO();
		summary.setWaitingForDepositCount(2);
		summary.setPaymentCompletedCount(1);
		summary.setDeliveryCompletedCount(1);

		// 매퍼/FTP 목 응답을 주문내역 페이지 기준으로 설정합니다.
		when(goodsMapper.countShopMypageOrderGroup(7L, "2026-03-01 00:00:00", "2026-03-21 00:00:00")).thenReturn(7);
		when(goodsMapper.getShopMypageOrderGroupList(7L, "2026-03-01 00:00:00", "2026-03-21 00:00:00", 5, 5))
			.thenReturn(List.of(secondPageFirstOrder, secondPageSecondOrder));
		when(goodsMapper.getShopMypageOrderDetailList(List.of("ORD-0007", "ORD-0006")))
			.thenReturn(List.of(firstDetail, pendingDetail, cancelledDetail, deliveredDetail));
		when(goodsMapper.getShopMypageOrderStatusSummary(7L, "2026-03-01 00:00:00", "2026-03-21 00:00:00"))
			.thenReturn(summary);
		when(ftpFileService.buildGoodsImageUrl("GOODS001", "goods-1.png"))
			.thenReturn("https://image.test/goods/GOODS001/goods-1.png");
		when(ftpFileService.buildGoodsImageUrl("GOODS009", "goods-0.png"))
			.thenReturn("https://image.test/goods/GOODS009/goods-0.png");
		when(ftpFileService.buildGoodsImageUrl("GOODS002", "goods-2.png"))
			.thenReturn("https://image.test/goods/GOODS002/goods-2.png");
		when(ftpFileService.buildGoodsImageUrl("GOODS003", "goods-3.png"))
			.thenReturn("https://image.test/goods/GOODS003/goods-3.png");

		// 주문내역 페이지 데이터를 조회합니다.
		ShopMypageOrderPageVO result = goodsService.getShopMypageOrderPage(7L, 2, "2026-03-01", "2026-03-20");

		// 페이지 정보, 상태 요약, 주문번호별 주문상세 묶음 결과를 검증합니다.
		assertThat(result).isNotNull();
		assertThat(result.getOrderCount()).isEqualTo(7);
		assertThat(result.getPageNo()).isEqualTo(2);
		assertThat(result.getPageSize()).isEqualTo(5);
		assertThat(result.getTotalPageCount()).isEqualTo(2);
		assertThat(result.getStartDate()).isEqualTo("2026-03-01");
		assertThat(result.getEndDate()).isEqualTo("2026-03-20");
		assertThat(result.getStatusSummary()).isNotNull();
		assertThat(result.getStatusSummary().getWaitingForDepositCount()).isEqualTo(2);
		assertThat(result.getStatusSummary().getPaymentCompletedCount()).isEqualTo(1);
		assertThat(result.getStatusSummary().getDeliveryCompletedCount()).isEqualTo(1);
		assertThat(result.getStatusSummary().getProductPreparingCount()).isEqualTo(0);
		assertThat(result.getOrderList()).hasSize(2);
		assertThat(result.getOrderList().get(0).getOrdNo()).isEqualTo("ORD-0007");
		assertThat(result.getOrderList().get(0).getDetailList()).hasSize(2);
		assertThat(result.getOrderList().get(0).getDetailList())
			.noneMatch(detailItem -> "ORD_DTL_STAT_00".equals(detailItem.getOrdDtlStatCd()));
		assertThat(result.getOrderList().get(0).getDetailList().get(0).getImgUrl())
			.isEqualTo("https://image.test/goods/GOODS001/goods-1.png");
		assertThat(result.getOrderList().get(0).getDetailList().get(1).getOrdDtlStatCd()).isEqualTo("ORD_DTL_STAT_99");
		assertThat(result.getOrderList().get(1).getOrdNo()).isEqualTo("ORD-0006");
		assertThat(result.getOrderList().get(1).getDetailList()).hasSize(1);
		assertThat(result.getOrderList().get(1).getDetailList().get(0).getImgUrl())
			.isEqualTo("https://image.test/goods/GOODS003/goods-3.png");
	}

	@Test
	@DisplayName("쇼핑몰 마이페이지 주문내역 조회 시 기간이 없으면 최근 3개월 기본값과 5건 페이지 크기를 사용한다")
	// 기본 조회 시 최근 3개월 기간을 계산하고 주문번호 5건 단위로 조회하는지 검증합니다.
	void getShopMypageOrderPage_usesDefaultThreeMonthRange() {
		// 최근 3개월 기본 조회 시 전체 건수 0건과 빈 목록을 반환하도록 설정합니다.
		LocalDate today = LocalDate.now();
		String expectedStartDate = today.minusMonths(3L).toString();
		String expectedEndDate = today.toString();
		String expectedStartDateTime = today.minusMonths(3L).atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		String expectedEndExclusiveDateTime = today.plusDays(1L).atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		when(goodsMapper.countShopMypageOrderGroup(7L, expectedStartDateTime, expectedEndExclusiveDateTime)).thenReturn(0);
		when(goodsMapper.getShopMypageOrderGroupList(7L, expectedStartDateTime, expectedEndExclusiveDateTime, 0, 5)).thenReturn(List.of());
		when(goodsMapper.getShopMypageOrderStatusSummary(7L, expectedStartDateTime, expectedEndExclusiveDateTime)).thenReturn(null);

		// 기간 없이 주문내역 페이지를 조회합니다.
		ShopMypageOrderPageVO result = goodsService.getShopMypageOrderPage(7L, null, null, null);

		// 기본 조회 기간과 페이지 기본값이 적용되는지 검증합니다.
		assertThat(result.getStartDate()).isEqualTo(expectedStartDate);
		assertThat(result.getEndDate()).isEqualTo(expectedEndDate);
		assertThat(result.getPageNo()).isEqualTo(1);
		assertThat(result.getPageSize()).isEqualTo(5);
		assertThat(result.getTotalPageCount()).isEqualTo(0);
		assertThat(result.getOrderList()).isEmpty();
		assertThat(result.getStatusSummary()).isNotNull();
		assertThat(result.getStatusSummary().getWaitingForDepositCount()).isEqualTo(0);
		assertThat(result.getStatusSummary().getPaymentCompletedCount()).isEqualTo(0);
		verify(goodsMapper).getShopMypageOrderGroupList(7L, expectedStartDateTime, expectedEndExclusiveDateTime, 0, 5);
	}

	@Test
	@DisplayName("쇼핑몰 마이페이지 주문내역 조회 시 시작일이 종료일보다 늦으면 예외를 반환한다")
	// 잘못된 조회 기간 요청 시 기간 검증 예외를 반환하는지 확인합니다.
	void getShopMypageOrderPage_throwsWhenStartDateAfterEndDate() {
		// 역전된 조회 기간으로 주문내역 조회를 요청합니다.
		assertThatThrownBy(() -> goodsService.getShopMypageOrderPage(7L, 1, "2026-03-21", "2026-03-20"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("조회 기간을 확인해주세요.");
	}

	@Test
	@DisplayName("쇼핑몰 마이페이지 쿠폰 다운로드 시 현재 다운로드 가능한 쿠폰이면 1건 발급한다")
	// 개별 쿠폰 다운로드 시 다운로드 가능 여부 확인 후 발급 서비스가 1건 호출되는지 검증합니다.
	void downloadShopMypageCoupon_issuesCouponWhenDownloadable() {
		// 다운로드 요청과 다운로드 가능 쿠폰 데이터를 구성합니다.
		ShopMypageCouponDownloadRequestPO param = new ShopMypageCouponDownloadRequestPO();
		param.setCpnNo(31L);

		ShopMypageDownloadableCouponVO downloadableCoupon = new ShopMypageDownloadableCouponVO();
		downloadableCoupon.setCpnNo(31L);
		downloadableCoupon.setCpnNm("다운로드 가능 쿠폰");

		// 다운로드 가능 목록과 발급 서비스 응답을 설정합니다.
		when(goodsMapper.getShopMypageDownloadableCouponList()).thenReturn(List.of(downloadableCoupon));
		when(shopAuthService.issueShopCustomerCoupon(7L, 31L, 1)).thenReturn(1);

		// 개별 쿠폰 다운로드를 수행합니다.
		goodsService.downloadShopMypageCoupon(param, 7L);

		// 발급 서비스가 1건 호출되는지 검증합니다.
		verify(shopAuthService).issueShopCustomerCoupon(7L, 31L, 1);
	}

	@Test
	@DisplayName("쇼핑몰 마이페이지 전체 쿠폰 다운로드 시 다운로드 가능 쿠폰별로 1건씩 발급한다")
	// 전체 다운로드 시 현재 다운로드 가능 쿠폰 종류별로 1건씩 발급되는지 검증합니다.
	void downloadAllShopMypageCoupon_issuesEachCouponOnce() {
		// 다운로드 가능 쿠폰 목록 데이터를 구성합니다.
		ShopMypageDownloadableCouponVO firstCoupon = new ShopMypageDownloadableCouponVO();
		firstCoupon.setCpnNo(41L);
		ShopMypageDownloadableCouponVO secondCoupon = new ShopMypageDownloadableCouponVO();
		secondCoupon.setCpnNo(42L);

		// 다운로드 가능 목록과 발급 서비스 응답을 설정합니다.
		when(goodsMapper.getShopMypageDownloadableCouponList()).thenReturn(List.of(firstCoupon, secondCoupon));
		when(shopAuthService.issueShopCustomerCoupon(7L, 41L, 1)).thenReturn(1);
		when(shopAuthService.issueShopCustomerCoupon(7L, 42L, 1)).thenReturn(1);

		// 전체 쿠폰 다운로드를 수행합니다.
		int result = goodsService.downloadAllShopMypageCoupon(7L);

		// 쿠폰별 발급 호출과 다운로드 건수를 검증합니다.
		assertThat(result).isEqualTo(2);
		verify(shopAuthService).issueShopCustomerCoupon(7L, 41L, 1);
		verify(shopAuthService).issueShopCustomerCoupon(7L, 42L, 1);
	}

	@Test
	@DisplayName("쇼핑몰 마이페이지 쿠폰 다운로드 시 현재 다운로드 가능한 쿠폰이 아니면 예외를 반환한다")
	// 다운로드 가능 목록에 없는 쿠폰번호를 요청하면 검증 예외를 반환하는지 확인합니다.
	void downloadShopMypageCoupon_throwsWhenCouponMissing() {
		// 존재하지 않는 쿠폰번호로 다운로드 요청 데이터를 구성합니다.
		ShopMypageCouponDownloadRequestPO param = new ShopMypageCouponDownloadRequestPO();
		param.setCpnNo(99L);

		// 다운로드 가능 목록을 비워둡니다.
		when(goodsMapper.getShopMypageDownloadableCouponList()).thenReturn(List.of());

		// 다운로드 가능 쿠폰 검증 예외 메시지를 확인합니다.
		assertThatThrownBy(() -> goodsService.downloadShopMypageCoupon(param, 7L))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("다운로드 가능한 쿠폰을 확인해주세요.");
	}

	@Test
	@DisplayName("쇼핑몰 상품상세 쿠폰 다운로드 시 현재 상품에 적용 가능한 상품쿠폰이면 1건 발급한다")
	// 상품상세 쿠폰 다운로드 요청 시 현재 상품에 노출 가능한 상품쿠폰만 발급되는지 검증합니다.
	void downloadShopGoodsCoupon_issuesCouponWhenApplicableToGoods() {
		// 다운로드 대상 상품과 상품쿠폰 데이터를 구성합니다.
		ShopGoodsBasicVO goods = new ShopGoodsBasicVO();
		goods.setGoodsId("GOODS001");
		goods.setBrandNo(1);

		ShopGoodsCouponVO goodsCoupon = new ShopGoodsCouponVO();
		goodsCoupon.setCpnNo(51L);
		goodsCoupon.setCpnGbCd("CPN_GB_01");
		goodsCoupon.setCpnTargetCd("CPN_TARGET_99");

		// 현재 상품에 노출 가능한 상품쿠폰과 발급 서비스 응답을 설정합니다.
		when(goodsMapper.getShopGoodsBasic("GOODS001")).thenReturn(goods);
		when(goodsMapper.getShopActiveGoodsCouponList()).thenReturn(List.of(goodsCoupon));
		when(goodsMapper.getShopGoodsCategoryIdList("GOODS001")).thenReturn(List.of());
		when(goodsMapper.getShopGoodsExhibitionTabNoList("GOODS001")).thenReturn(List.of());
		when(goodsMapper.getShopCouponTargetList(51L)).thenReturn(List.of());
		when(shopAuthService.issueShopCustomerCoupon(7L, 51L, 1)).thenReturn(1);

		// 상품상세 쿠폰 다운로드를 수행합니다.
		goodsService.downloadShopGoodsCoupon("GOODS001", 51L, 7L);

		// 발급 서비스가 1건 호출되는지 검증합니다.
		verify(shopAuthService).issueShopCustomerCoupon(7L, 51L, 1);
	}

	@Test
	@DisplayName("쇼핑몰 상품상세 쿠폰 다운로드 시 같은 상품쿠폰을 반복 요청해도 매번 발급을 시도한다")
	// 상품상세 쿠폰 다운로드는 동일 쿠폰을 이미 보유 중이어도 반복 다운로드를 허용하는지 검증합니다.
	void downloadShopGoodsCoupon_allowsDuplicateCouponDownload() {
		// 반복 다운로드 대상 상품과 상품쿠폰 데이터를 구성합니다.
		ShopGoodsBasicVO goods = new ShopGoodsBasicVO();
		goods.setGoodsId("GOODS001");
		goods.setBrandNo(1);

		ShopGoodsCouponVO goodsCoupon = new ShopGoodsCouponVO();
		goodsCoupon.setCpnNo(52L);
		goodsCoupon.setCpnGbCd("CPN_GB_01");
		goodsCoupon.setCpnTargetCd("CPN_TARGET_99");

		// 반복 호출해도 동일 쿠폰이 계속 발급 가능하도록 목 응답을 설정합니다.
		when(goodsMapper.getShopGoodsBasic("GOODS001")).thenReturn(goods);
		when(goodsMapper.getShopActiveGoodsCouponList()).thenReturn(List.of(goodsCoupon));
		when(goodsMapper.getShopGoodsCategoryIdList("GOODS001")).thenReturn(List.of());
		when(goodsMapper.getShopGoodsExhibitionTabNoList("GOODS001")).thenReturn(List.of());
		when(goodsMapper.getShopCouponTargetList(52L)).thenReturn(List.of());
		when(shopAuthService.issueShopCustomerCoupon(7L, 52L, 1)).thenReturn(1);

		// 동일 상품쿠폰 다운로드를 두 번 수행합니다.
		goodsService.downloadShopGoodsCoupon("GOODS001", 52L, 7L);
		goodsService.downloadShopGoodsCoupon("GOODS001", 52L, 7L);

		// 동일 쿠폰 발급 서비스가 요청 횟수만큼 호출되는지 검증합니다.
		verify(shopAuthService, times(2)).issueShopCustomerCoupon(7L, 52L, 1);
	}

	@Test
	@DisplayName("쇼핑몰 상품상세 쿠폰 다운로드 시 장바구니 쿠폰이면 예외를 반환한다")
	// 상품상세 다운로드 대상은 상품쿠폰만 허용되고 장바구니 쿠폰은 차단되는지 검증합니다.
	void downloadShopGoodsCoupon_throwsWhenCouponIsNotGoodsCoupon() {
		// 상품상세 조회 가능한 상품과 장바구니 쿠폰 데이터를 구성합니다.
		ShopGoodsBasicVO goods = new ShopGoodsBasicVO();
		goods.setGoodsId("GOODS001");
		goods.setBrandNo(1);

		ShopGoodsCouponVO cartCoupon = new ShopGoodsCouponVO();
		cartCoupon.setCpnNo(61L);
		cartCoupon.setCpnGbCd("CPN_GB_03");
		cartCoupon.setCpnTargetCd("CPN_TARGET_99");

		// 상품 기본 정보와 활성 쿠폰 목록을 설정합니다.
		when(goodsMapper.getShopGoodsBasic("GOODS001")).thenReturn(goods);
		when(goodsMapper.getShopActiveGoodsCouponList()).thenReturn(List.of(cartCoupon));
		when(goodsMapper.getShopGoodsCategoryIdList("GOODS001")).thenReturn(List.of());
		when(goodsMapper.getShopGoodsExhibitionTabNoList("GOODS001")).thenReturn(List.of());

		// 상품쿠폰 전용 검증 예외 메시지를 확인합니다.
		assertThatThrownBy(() -> goodsService.downloadShopGoodsCoupon("GOODS001", 61L, 7L))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("다운로드 가능한 상품쿠폰을 확인해주세요.");
	}

	@Test
	@DisplayName("쇼핑몰 상품상세 쿠폰 다운로드 시 제외상품이면 예외를 반환한다")
	// 현재 상품이 쿠폰 제외 대상으로 잡혀 있으면 다운로드를 차단하는지 검증합니다.
	void downloadShopGoodsCoupon_throwsWhenGoodsExcluded() {
		// 상품상세 조회 가능한 상품과 제외 대상 상품쿠폰 데이터를 구성합니다.
		ShopGoodsBasicVO goods = new ShopGoodsBasicVO();
		goods.setGoodsId("GOODS001");
		goods.setBrandNo(1);

		ShopGoodsCouponVO goodsCoupon = new ShopGoodsCouponVO();
		goodsCoupon.setCpnNo(71L);
		goodsCoupon.setCpnGbCd("CPN_GB_01");
		goodsCoupon.setCpnTargetCd("CPN_TARGET_99");

		// 상품 기본 정보와 제외 타겟 쿠폰 응답을 설정합니다.
		when(goodsMapper.getShopGoodsBasic("GOODS001")).thenReturn(goods);
		when(goodsMapper.getShopActiveGoodsCouponList()).thenReturn(List.of(goodsCoupon));
		when(goodsMapper.getShopGoodsCategoryIdList("GOODS001")).thenReturn(List.of());
		when(goodsMapper.getShopGoodsExhibitionTabNoList("GOODS001")).thenReturn(List.of());
		when(goodsMapper.getShopCouponTargetList(71L)).thenReturn(List.of(createCouponTarget("TARGET_GB_02", "GOODS001")));

		// 제외상품 검증 예외 메시지를 확인합니다.
		assertThatThrownBy(() -> goodsService.downloadShopGoodsCoupon("GOODS001", 71L, 7L))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("다운로드 가능한 상품쿠폰을 확인해주세요.");
	}

	@Test
	@DisplayName("쇼핑몰 상품상세 쿠폰 다운로드 시 상품이 없으면 예외를 반환한다")
	// 다운로드 대상 상품을 찾지 못하면 상품 미존재 예외를 반환하는지 검증합니다.
	void downloadShopGoodsCoupon_throwsWhenGoodsMissing() {
		// 상품 기본 정보를 찾지 못한 상황을 목으로 설정합니다.
		when(goodsMapper.getShopGoodsBasic("UNKNOWN")).thenReturn(null);

		// 상품 미존재 예외 메시지를 확인합니다.
		assertThatThrownBy(() -> goodsService.downloadShopGoodsCoupon("UNKNOWN", 81L, 7L))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("상품 정보를 찾을 수 없습니다.");
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
		brandCoupon.setCpnGbCd("CPN_GB_01");
		brandCoupon.setCpnTargetCd("CPN_TARGET_04");

		ShopGoodsCouponTargetVO brandApplyTarget = new ShopGoodsCouponTargetVO();
		brandApplyTarget.setCpnNo(1L);
		brandApplyTarget.setTargetGbCd("TARGET_GB_01");
		brandApplyTarget.setTargetValue("1");

		ShopGoodsCouponVO excludedCoupon = new ShopGoodsCouponVO();
		excludedCoupon.setCpnNo(2L);
		excludedCoupon.setCpnGbCd("CPN_GB_01");
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

	@Test
	@DisplayName("쇼핑몰 위시리스트 토글 시 미등록 상품은 등록 후 wished=true를 반환한다")
	// 위시 미등록 상태에서 토글하면 insert 후 true를 반환하는지 검증합니다.
	void toggleShopGoodsWishlist_insertsWhenNotWished() {
		// 위시 토글 대상 상품 기본 정보를 구성합니다.
		ShopGoodsBasicVO goods = new ShopGoodsBasicVO();
		goods.setGoodsId("GOODS001");

		// 미등록 상태 목 응답을 설정합니다.
		when(goodsMapper.getShopGoodsBasic("GOODS001")).thenReturn(goods);
		when(goodsMapper.countShopWishList(7L, "GOODS001")).thenReturn(0);
		when(goodsMapper.insertShopWishList(7L, "GOODS001", 7L)).thenReturn(1);

		// 위시 토글 후 등록 상태(true) 여부를 검증합니다.
		boolean result = goodsService.toggleShopGoodsWishlist("GOODS001", 7L);
		assertThat(result).isTrue();
		verify(goodsMapper).insertShopWishList(7L, "GOODS001", 7L);
	}

	@Test
	@DisplayName("쇼핑몰 위시리스트 토글 시 등록된 상품은 삭제 후 wished=false를 반환한다")
	// 위시 등록 상태에서 토글하면 delete 후 false를 반환하는지 검증합니다.
	void toggleShopGoodsWishlist_deletesWhenAlreadyWished() {
		// 위시 토글 대상 상품 기본 정보를 구성합니다.
		ShopGoodsBasicVO goods = new ShopGoodsBasicVO();
		goods.setGoodsId("GOODS002");

		// 등록 상태 목 응답을 설정합니다.
		when(goodsMapper.getShopGoodsBasic("GOODS002")).thenReturn(goods);
		when(goodsMapper.countShopWishList(8L, "GOODS002")).thenReturn(1);
		when(goodsMapper.deleteShopWishList(8L, "GOODS002")).thenReturn(1);

		// 위시 토글 후 해제 상태(false) 여부를 검증합니다.
		boolean result = goodsService.toggleShopGoodsWishlist("GOODS002", 8L);
		assertThat(result).isFalse();
		verify(goodsMapper).deleteShopWishList(8L, "GOODS002");
	}

	@Test
	@DisplayName("쇼핑몰 위시리스트 토글 시 조회 불가 상품이면 예외를 반환한다")
	// 조회 가능한 상품이 없을 때 상품 미존재 예외를 반환하는지 검증합니다.
	void toggleShopGoodsWishlist_throwsWhenGoodsMissing() {
		// 상품 기본 정보가 없는 상황을 목으로 설정합니다.
		when(goodsMapper.getShopGoodsBasic("UNKNOWN")).thenReturn(null);

		// 상품 미존재 예외 메시지를 검증합니다.
		assertThatThrownBy(() -> goodsService.toggleShopGoodsWishlist("UNKNOWN", 9L))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("상품 정보를 찾을 수 없습니다.");
	}

	@Test
	@DisplayName("쇼핑몰 장바구니 등록 시 미등록 상품은 신규 등록 후 최종 수량을 반환한다")
	// 장바구니 미등록 상태에서 등록하면 insert 후 최종 수량을 반환하는지 검증합니다.
	void addShopGoodsCart_insertsWhenNotExists() {
		// 장바구니 등록 대상 상품과 사이즈 목록을 구성합니다.
		ShopGoodsBasicVO goods = new ShopGoodsBasicVO();
		goods.setGoodsId("GOODS001");
		ShopGoodsSizeItemVO sizeItem = new ShopGoodsSizeItemVO();
		sizeItem.setGoodsId("GOODS001");
		sizeItem.setSizeId("095");
		sizeItem.setStockQty(10);

		// 미등록 상태 목 응답을 설정합니다.
		when(goodsMapper.getShopGoodsBasic("GOODS001")).thenReturn(goods);
		when(goodsMapper.getShopGoodsSizeList("GOODS001")).thenReturn(List.of(sizeItem));
		when(goodsMapper.countShopCart(7L, "GOODS001", "095")).thenReturn(0);
		when(exhibitionMapper.countShopVisibleExhibitionGoodsByGoodsId(99, "GOODS001")).thenReturn(0);
		when(goodsMapper.insertShopCart(any(ShopCartSavePO.class))).thenReturn(1);
		when(goodsMapper.getShopCartQty(7L, "GOODS001", "095")).thenReturn(2);

		// 장바구니 등록 후 최종 수량과 무효 기획전 번호 제거 여부를 검증합니다.
		int result = goodsService.addShopGoodsCart("GOODS001", "095", 2, 7L, 99);
		ArgumentCaptor<ShopCartSavePO> captor = ArgumentCaptor.forClass(ShopCartSavePO.class);
		assertThat(result).isEqualTo(2);
		verify(goodsMapper).insertShopCart(captor.capture());
		assertThat(captor.getValue().getCartGbCd()).isEqualTo("C");
		assertThat(captor.getValue().getCustNo()).isEqualTo(7L);
		assertThat(captor.getValue().getGoodsId()).isEqualTo("GOODS001");
		assertThat(captor.getValue().getSizeId()).isEqualTo("095");
		assertThat(captor.getValue().getQty()).isEqualTo(2);
		assertThat(captor.getValue().getExhibitionNo()).isNull();
	}

	@Test
	@DisplayName("쇼핑몰 장바구니 등록 시 유효한 기획전 상품이면 기획전 번호를 함께 저장한다")
	// 기획전 상세 유입 상품을 신규 장바구니 등록하면 검증된 기획전 번호가 저장되는지 확인합니다.
	void addShopGoodsCart_insertsValidatedExhibitionNoWhenExhibitionGoods() {
		// 장바구니 등록 대상 상품과 사이즈 목록을 구성합니다.
		ShopGoodsBasicVO goods = new ShopGoodsBasicVO();
		goods.setGoodsId("GOODS011");
		ShopGoodsSizeItemVO sizeItem = new ShopGoodsSizeItemVO();
		sizeItem.setGoodsId("GOODS011");
		sizeItem.setSizeId("090");
		sizeItem.setStockQty(5);

		// 기획전 노출 상품인 신규 장바구니 상태를 목으로 설정합니다.
		when(goodsMapper.getShopGoodsBasic("GOODS011")).thenReturn(goods);
		when(goodsMapper.getShopGoodsSizeList("GOODS011")).thenReturn(List.of(sizeItem));
		when(goodsMapper.countShopCart(17L, "GOODS011", "090")).thenReturn(0);
		when(exhibitionMapper.countShopVisibleExhibitionGoodsByGoodsId(2, "GOODS011")).thenReturn(1);
		when(goodsMapper.insertShopCart(any(ShopCartSavePO.class))).thenReturn(1);
		when(goodsMapper.getShopCartQty(17L, "GOODS011", "090")).thenReturn(1);

		// 유효한 기획전 번호가 저장되는지 검증합니다.
		int result = goodsService.addShopGoodsCart("GOODS011", "090", 1, 17L, 2);
		ArgumentCaptor<ShopCartSavePO> captor = ArgumentCaptor.forClass(ShopCartSavePO.class);
		assertThat(result).isEqualTo(1);
		verify(goodsMapper).insertShopCart(captor.capture());
		assertThat(captor.getValue().getExhibitionNo()).isEqualTo(2);
	}

	@Test
	@DisplayName("쇼핑몰 장바구니 등록 시 기존 상품은 수량을 가산하고 마지막 기획전 번호로 덮어쓴다")
	// 장바구니 등록 상태에서 동일 상품/사이즈를 기획전 경로로 다시 담으면 수량 가산과 기획전 번호 갱신이 함께 되는지 검증합니다.
	void addShopGoodsCart_updatesWhenExists() {
		// 장바구니 등록 대상 상품과 사이즈 목록을 구성합니다.
		ShopGoodsBasicVO goods = new ShopGoodsBasicVO();
		goods.setGoodsId("GOODS002");
		ShopGoodsSizeItemVO sizeItem = new ShopGoodsSizeItemVO();
		sizeItem.setGoodsId("GOODS002");
		sizeItem.setSizeId("100");
		sizeItem.setStockQty(20);

		// 기등록 상태 목 응답을 설정합니다.
		when(goodsMapper.getShopGoodsBasic("GOODS002")).thenReturn(goods);
		when(goodsMapper.getShopGoodsSizeList("GOODS002")).thenReturn(List.of(sizeItem));
		when(goodsMapper.countShopCart(8L, "GOODS002", "100")).thenReturn(1);
		when(exhibitionMapper.countShopVisibleExhibitionGoodsByGoodsId(2, "GOODS002")).thenReturn(1);
		when(goodsMapper.addShopCartQty(8L, "GOODS002", "100", 3, 2, 8L)).thenReturn(1);
		when(goodsMapper.getShopCartQty(8L, "GOODS002", "100")).thenReturn(5);

		// 장바구니 등록 후 최종 수량과 기획전 번호 갱신 여부를 검증합니다.
		int result = goodsService.addShopGoodsCart("GOODS002", "100", 3, 8L, 2);
		assertThat(result).isEqualTo(5);
		verify(goodsMapper).addShopCartQty(8L, "GOODS002", "100", 3, 2, 8L);
	}

	@Test
	@DisplayName("쇼핑몰 장바구니 등록 시 마지막 일반 경로 요청이면 기존 기획전 번호를 제거한다")
	// 동일 상품/사이즈를 일반 상품상세 경로로 다시 담으면 EXHIBITION_NO가 null로 덮이는지 검증합니다.
	void addShopGoodsCart_clearsExhibitionNoWhenLastPathIsNormal() {
		// 장바구니 등록 대상 상품과 사이즈 목록을 구성합니다.
		ShopGoodsBasicVO goods = new ShopGoodsBasicVO();
		goods.setGoodsId("GOODS012");
		ShopGoodsSizeItemVO sizeItem = new ShopGoodsSizeItemVO();
		sizeItem.setGoodsId("GOODS012");
		sizeItem.setSizeId("095");
		sizeItem.setStockQty(20);

		// 기존 장바구니가 있는 상태를 목으로 설정합니다.
		when(goodsMapper.getShopGoodsBasic("GOODS012")).thenReturn(goods);
		when(goodsMapper.getShopGoodsSizeList("GOODS012")).thenReturn(List.of(sizeItem));
		when(goodsMapper.countShopCart(18L, "GOODS012", "095")).thenReturn(1);
		when(goodsMapper.addShopCartQty(18L, "GOODS012", "095", 2, null, 18L)).thenReturn(1);
		when(goodsMapper.getShopCartQty(18L, "GOODS012", "095")).thenReturn(4);

		// 일반 경로 재등록 시 EXHIBITION_NO 제거와 수량 누적을 검증합니다.
		int result = goodsService.addShopGoodsCart("GOODS012", "095", 2, 18L, null);
		assertThat(result).isEqualTo(4);
		verify(goodsMapper).addShopCartQty(18L, "GOODS012", "095", 2, null, 18L);
	}

	@Test
	@DisplayName("쇼핑몰 장바구니 등록 시 존재하지 않는 사이즈면 예외를 반환한다")
	// 상품에 없는 사이즈 요청 시 사이즈 검증 예외를 반환하는지 확인합니다.
	void addShopGoodsCart_throwsWhenSizeMissing() {
		// 장바구니 등록 대상 상품을 구성합니다.
		ShopGoodsBasicVO goods = new ShopGoodsBasicVO();
		goods.setGoodsId("GOODS003");

		// 상품은 존재하지만 요청 사이즈가 없는 상태를 목으로 설정합니다.
		when(goodsMapper.getShopGoodsBasic("GOODS003")).thenReturn(goods);
		when(goodsMapper.getShopGoodsSizeList("GOODS003")).thenReturn(List.of());

		// 사이즈 검증 예외 메시지를 확인합니다.
		assertThatThrownBy(() -> goodsService.addShopGoodsCart("GOODS003", "110", 1, 9L, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("사이즈를 확인해주세요.");
	}

	@Test
	@DisplayName("쇼핑몰 바로구매 장바구니 등록 시 O 구분으로 신규 행을 만들고 cartId를 반환한다")
	// 바로구매 요청 시 CART_GB_CD=O 신규 등록과 생성된 장바구니 번호 반환 여부를 검증합니다.
	void addShopGoodsOrderNowCart_insertsOrderCartAndReturnsCartId() {
		// 바로구매 등록 대상 상품과 사이즈 목록을 구성합니다.
		ShopGoodsBasicVO goods = new ShopGoodsBasicVO();
		goods.setGoodsId("GOODS010");
		ShopGoodsSizeItemVO sizeItem = new ShopGoodsSizeItemVO();
		sizeItem.setGoodsId("GOODS010");
		sizeItem.setSizeId("090");
		sizeItem.setStockQty(3);

		// 상품/사이즈 조회 결과와 생성 키 반영 목 동작을 설정합니다.
		when(goodsMapper.getShopGoodsBasic("GOODS010")).thenReturn(goods);
		when(goodsMapper.getShopGoodsSizeList("GOODS010")).thenReturn(List.of(sizeItem));
		when(exhibitionMapper.countShopVisibleExhibitionGoodsByGoodsId(2, "GOODS010")).thenReturn(1);
		when(goodsMapper.insertShopCart(any(ShopCartSavePO.class))).thenAnswer(invocation -> {
			ShopCartSavePO savePO = invocation.getArgument(0);
			savePO.setCartId(21L);
			return 1;
		});

		// 바로구매 등록 후 생성된 cartId와 저장 기획전 번호를 검증합니다.
		Long result = goodsService.addShopGoodsOrderNowCart("GOODS010", "090", 1, 11L, 2);
		ArgumentCaptor<ShopCartSavePO> captor = ArgumentCaptor.forClass(ShopCartSavePO.class);
		assertThat(result).isEqualTo(21L);
		verify(goodsMapper).insertShopCart(captor.capture());
		assertThat(captor.getValue().getCartGbCd()).isEqualTo("O");
		assertThat(captor.getValue().getCustNo()).isEqualTo(11L);
		assertThat(captor.getValue().getGoodsId()).isEqualTo("GOODS010");
		assertThat(captor.getValue().getSizeId()).isEqualTo("090");
		assertThat(captor.getValue().getQty()).isEqualTo(1);
		assertThat(captor.getValue().getExhibitionNo()).isEqualTo(2);
	}

	@Test
	@DisplayName("쇼핑몰 장바구니 페이지 조회 시 이미지 URL, 사이즈 옵션, 배송비 기준 정보를 함께 반환한다")
	// 장바구니 페이지 조회 시 화면 렌더링에 필요한 조합 데이터가 정상 구성되는지 검증합니다.
	void getShopCartPage_returnsComposedCartPage() {
		// 장바구니 목록 테스트 데이터를 구성합니다.
		ShopCartItemVO cartItem = new ShopCartItemVO();
		cartItem.setCustNo(7L);
		cartItem.setGoodsId("GOODS001");
		cartItem.setGoodsNm("테스트 상품");
		cartItem.setSizeId("095");
		cartItem.setQty(2);
		cartItem.setSupplyAmt(20000);
		cartItem.setSaleAmt(15000);
		cartItem.setImgPath("cart-main.png");

		ShopGoodsSizeItemVO size095 = new ShopGoodsSizeItemVO();
		size095.setGoodsId("GOODS001");
		size095.setSizeId("095");
		size095.setStockQty(5);

		ShopGoodsSizeItemVO size100 = new ShopGoodsSizeItemVO();
		size100.setGoodsId("GOODS001");
		size100.setSizeId("100");
		size100.setStockQty(0);

		ShopCartSiteInfoVO siteInfo = new ShopCartSiteInfoVO();
		siteInfo.setSiteId("xodud1202");
		siteInfo.setDeliveryFee(3000);
		siteInfo.setDeliveryFeeLimit(30000);

		// 매퍼/FTP 목 응답을 설정합니다.
		when(goodsMapper.getShopCartItemList(7L)).thenReturn(List.of(cartItem));
		when(goodsMapper.getShopGoodsSizeList("GOODS001")).thenReturn(List.of(size095, size100));
		when(goodsMapper.getShopCartSiteInfo("xodud1202")).thenReturn(siteInfo);
		when(ftpFileService.buildGoodsImageUrl("GOODS001", "cart-main.png"))
			.thenReturn("https://image.test/goods/GOODS001/cart-main.png");

		// 장바구니 페이지 조회 결과를 확인합니다.
		ShopCartPageVO result = goodsService.getShopCartPage(7L);

		// 이미지 URL/사이즈 옵션/배송비 기준 정보가 정상 구성되었는지 검증합니다.
		assertThat(result).isNotNull();
		assertThat(result.getCartCount()).isEqualTo(1);
		assertThat(result.getSiteInfo().getDeliveryFee()).isEqualTo(3000);
		assertThat(result.getSiteInfo().getDeliveryFeeLimit()).isEqualTo(30000);
		assertThat(result.getCartList()).hasSize(1);
		assertThat(result.getCartList().get(0).getImgUrl()).isEqualTo("https://image.test/goods/GOODS001/cart-main.png");
		assertThat(result.getCartList().get(0).getSizeOptions()).hasSize(2);
		assertThat(result.getCartList().get(0).getSizeOptions().get(0).isSoldOut()).isFalse();
		assertThat(result.getCartList().get(0).getSizeOptions().get(1).isSoldOut()).isTrue();
	}

	@Test
	@DisplayName("쇼핑몰 주문서 페이지 조회 시 요청한 cartId가 모두 현재 고객 장바구니일 때만 데이터를 반환한다")
	// 주문서 페이지 조회 시 cartId 검증과 배송지/이미지/건수 구성 결과를 검증합니다.
	void getShopOrderPage_returnsOrderCartPageWhenCartIdsValid() {
		// 주문서 대상 장바구니 목록과 배송비 기준 정보를 구성합니다.
		ShopCartItemVO firstCartItem = new ShopCartItemVO();
		firstCartItem.setCartId(12L);
		firstCartItem.setCustNo(7L);
		firstCartItem.setGoodsId("GOODS100");
		firstCartItem.setBrandNo(1);
		firstCartItem.setGoodsNm("주문상품1");
		firstCartItem.setSizeId("095");
		firstCartItem.setQty(1);
		firstCartItem.setSupplyAmt(15000);
		firstCartItem.setSaleAmt(12000);
		firstCartItem.setImgPath("order-1.png");

		ShopCartItemVO secondCartItem = new ShopCartItemVO();
		secondCartItem.setCartId(15L);
		secondCartItem.setCustNo(7L);
		secondCartItem.setGoodsId("GOODS200");
		secondCartItem.setBrandNo(2);
		secondCartItem.setGoodsNm("주문상품2");
		secondCartItem.setSizeId("100");
		secondCartItem.setQty(2);
		secondCartItem.setSupplyAmt(25000);
		secondCartItem.setSaleAmt(20000);
		secondCartItem.setImgPath("order-2.png");

		ShopGoodsSizeItemVO goods100Size = new ShopGoodsSizeItemVO();
		goods100Size.setGoodsId("GOODS100");
		goods100Size.setSizeId("095");
		goods100Size.setStockQty(5);
		ShopGoodsSizeItemVO goods200Size = new ShopGoodsSizeItemVO();
		goods200Size.setGoodsId("GOODS200");
		goods200Size.setSizeId("100");
		goods200Size.setStockQty(7);

		GoodsSizeVO goods100SizeDetail = new GoodsSizeVO();
		goods100SizeDetail.setGoodsId("GOODS100");
		goods100SizeDetail.setSizeId("095");
		goods100SizeDetail.setStockQty(5);
		goods100SizeDetail.setDelYn("N");
		GoodsSizeVO goods200SizeDetail = new GoodsSizeVO();
		goods200SizeDetail.setGoodsId("GOODS200");
		goods200SizeDetail.setSizeId("100");
		goods200SizeDetail.setStockQty(7);
		goods200SizeDetail.setDelYn("N");

		ShopCartSiteInfoVO siteInfo = new ShopCartSiteInfoVO();
		siteInfo.setSiteId("xodud1202");
		siteInfo.setDeliveryFee(3000);
		siteInfo.setDeliveryFeeLimit(100000);

		ShopOrderAddressVO defaultAddress = new ShopOrderAddressVO();
		defaultAddress.setCustNo(7L);
		defaultAddress.setAddressNm("집");
		defaultAddress.setPostNo("06234");
		defaultAddress.setBaseAddress("서울특별시 강남구 테헤란로 1");
		defaultAddress.setDetailAddress("101동 1001호");
		defaultAddress.setPhoneNumber("010-1234-5678");
		defaultAddress.setRsvNm("홍길동");
		defaultAddress.setDefaultYn("Y");

		ShopCartCustomerCouponVO allGoodsCoupon = createCustomerCoupon(1001L, 101L, "CPN_GB_01", "CPN_TARGET_99", "CPN_DC_GB_01", 3000);
		ShopCartCustomerCouponVO goodsTargetCoupon = createCustomerCoupon(1003L, 103L, "CPN_GB_01", "CPN_TARGET_01", "CPN_DC_GB_01", 5000);
		ShopCartCustomerCouponVO cartCoupon = createCustomerCoupon(2001L, 201L, "CPN_GB_03", "CPN_TARGET_99", "CPN_DC_GB_01", 7000);
		ShopCartCustomerCouponVO deliveryCoupon = createCustomerCoupon(3001L, 301L, "CPN_GB_04", "CPN_TARGET_99", "CPN_DC_GB_01", 3000);
		ShopOrderCustomerInfoVO customerInfo = new ShopOrderCustomerInfoVO();
		customerInfo.setCustNo(7L);
		customerInfo.setCustNm("홍길동");
		customerInfo.setEmail("test@test.com");
		customerInfo.setPhoneNumber("010-1234-5678");
		customerInfo.setCustGradeCd("WELCOME");

		// 주문서 대상 cartId 조회와 이미지 URL/배송지 보정 목 응답을 설정합니다.
		when(goodsMapper.getShopOrderCartItemList(7L, List.of(12L, 15L))).thenReturn(List.of(firstCartItem, secondCartItem));
		when(goodsMapper.getShopOrderAddressList(7L)).thenReturn(List.of(defaultAddress));
		when(goodsMapper.getShopOrderCustomerInfo(7L)).thenReturn(customerInfo);
		when(goodsMapper.getShopPointSaveRateByCustGradeCd("WELCOME")).thenReturn(1);
		when(goodsMapper.getShopGoodsSizeList("GOODS100")).thenReturn(List.of(goods100Size));
		when(goodsMapper.getShopGoodsSizeList("GOODS200")).thenReturn(List.of(goods200Size));
		when(goodsMapper.getAdminGoodsSizeDetail("GOODS100", "095")).thenReturn(goods100SizeDetail);
		when(goodsMapper.getAdminGoodsSizeDetail("GOODS200", "100")).thenReturn(goods200SizeDetail);
		when(goodsMapper.getShopCartSiteInfo("xodud1202")).thenReturn(siteInfo);
		when(goodsMapper.getShopCustomerCouponList(7L)).thenReturn(List.of(allGoodsCoupon, goodsTargetCoupon, cartCoupon, deliveryCoupon));
		when(goodsMapper.getShopCouponTargetList(101L)).thenReturn(List.of());
		when(goodsMapper.getShopCouponTargetList(103L)).thenReturn(List.of(createCouponTarget("TARGET_GB_01", "GOODS200")));
		when(goodsMapper.getShopGoodsCategoryIdList("GOODS100")).thenReturn(List.of());
		when(goodsMapper.getShopGoodsCategoryIdList("GOODS200")).thenReturn(List.of());
		when(goodsMapper.getShopGoodsExhibitionTabNoList("GOODS100")).thenReturn(List.of());
		when(goodsMapper.getShopGoodsExhibitionTabNoList("GOODS200")).thenReturn(List.of());
		when(goodsMapper.getShopAvailablePointAmt(7L)).thenReturn(9000);
		when(ftpFileService.buildGoodsImageUrl("GOODS100", "order-1.png")).thenReturn("https://image.test/goods/GOODS100/order-1.png");
		when(ftpFileService.buildGoodsImageUrl("GOODS200", "order-2.png")).thenReturn("https://image.test/goods/GOODS200/order-2.png");

		// 유효한 cartId 요청 시 주문서 페이지 데이터가 구성되는지 검증합니다.
		ShopOrderPageVO result = goodsService.getShopOrderPage(List.of(12L, 15L), 7L, "PC", "http://127.0.0.1:3014");
		assertThat(result.getCartCount()).isEqualTo(2);
		assertThat(result.getCartList()).hasSize(2);
		assertThat(result.getCartList().get(0).getCartId()).isEqualTo(12L);
		assertThat(result.getCartList().get(0).getImgUrl()).isEqualTo("https://image.test/goods/GOODS100/order-1.png");
		assertThat(result.getCartList().get(1).getCartId()).isEqualTo(15L);
		assertThat(result.getSiteInfo().getDeliveryFee()).isEqualTo(3000);
		assertThat(result.getAddressList()).hasSize(1);
		assertThat(result.getDefaultAddress()).isNotNull();
		assertThat(result.getDefaultAddress().getAddressNm()).isEqualTo("집");
		assertThat(result.getAvailablePointAmt()).isEqualTo(9000);
		assertThat(result.getCouponOption()).isNotNull();
		assertThat(result.getCouponOption().getGoodsCouponGroupList()).hasSize(2);
		assertThat(result.getCouponOption().getGoodsCouponGroupList().get(0).getCouponList()).hasSize(1);
		assertThat(result.getCouponOption().getGoodsCouponGroupList().get(1).getCouponList()).hasSize(2);
		assertThat(result.getDiscountSelection()).isNotNull();
		assertThat(result.getDiscountSelection().getGoodsCouponSelectionList()).hasSize(2);
		assertThat(result.getDiscountSelection().getCartCouponCustCpnNo()).isEqualTo(2001L);
		assertThat(result.getDiscountSelection().getDeliveryCouponCustCpnNo()).isEqualTo(3001L);
		assertThat(result.getDiscountAmount()).isNotNull();
		assertThat(result.getDiscountAmount().getGoodsCouponDiscountAmt()).isEqualTo(8000);
		assertThat(result.getDiscountAmount().getCartCouponDiscountAmt()).isEqualTo(7000);
		assertThat(result.getDiscountAmount().getDeliveryCouponDiscountAmt()).isEqualTo(3000);
		assertThat(result.getDiscountAmount().getCouponDiscountAmt()).isEqualTo(18000);
		assertThat(result.getDiscountAmount().getMaxPointUseAmt()).isEqualTo(9000);
		assertThat(result.getPaymentConfig()).isNotNull();
		assertThat(result.getPaymentConfig().getApiVersion()).isEqualTo("2022-11-16");
		assertThat(result.getPaymentConfig().getSuccessUrlBase()).isEqualTo("http://127.0.0.1:3014/order/success");
		assertThat(result.getPaymentConfig().getFailUrlBase()).isEqualTo("http://127.0.0.1:3014/order/fail");
		assertThat(result.getCustomerInfo()).isNotNull();
		assertThat(result.getCustomerInfo().getCustomerKey()).isEqualTo("SHOP-CUST-7");
		assertThat(result.getCustomerInfo().getDeviceGbCd()).isEqualTo("PC");
		assertThat(result.getPointSaveSummary()).isNotNull();
		assertThat(result.getPointSaveSummary().getPointSaveRate()).isEqualTo(1);
		assertThat(result.getPointSaveSummary().getTotalExpectedPoint()).isEqualTo(520);
	}

	@Test
	@DisplayName("쇼핑몰 주문서 페이지 조회 시 요청 cartId 중 하나라도 누락되면 예외를 반환한다")
	// 주문서 페이지 조회 시 전달된 cartId가 모두 현재 고객 장바구니와 일치하지 않으면 실패하는지 검증합니다.
	void getShopOrderPage_throwsWhenCartIdMismatchExists() {
		// 요청한 cartId 중 1건만 조회되는 상태를 목으로 설정합니다.
		ShopCartItemVO firstCartItem = new ShopCartItemVO();
		firstCartItem.setCartId(12L);
		firstCartItem.setCustNo(7L);
		firstCartItem.setGoodsId("GOODS100");
		firstCartItem.setSizeId("095");
		when(goodsMapper.getShopOrderCartItemList(7L, List.of(12L, 19L))).thenReturn(List.of(firstCartItem));

		// cartId 누락 시 주문 정보 검증 예외 메시지를 확인합니다.
		assertThatThrownBy(() -> goodsService.getShopOrderPage(List.of(12L, 19L), 7L, "PC", "http://127.0.0.1:3014"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("주문 정보가 맞지 않습니다.");
	}

	@Test
	@DisplayName("쇼핑몰 주문서 페이지 조회 시 현재 재고보다 주문수량이 많으면 재고 부족 예외를 반환한다")
	// 주문서 진입 단계에서 상품/사이즈별 주문수량 합계가 현재 재고를 초과하면 재고 부족 메시지로 차단하는지 검증합니다.
	void getShopOrderPage_throwsWhenStockIsInsufficient() {
		// 주문 대상 장바구니와 현재 재고 부족 상태를 구성합니다.
		ShopCartItemVO cartItem = new ShopCartItemVO();
		cartItem.setCartId(21L);
		cartItem.setCustNo(7L);
		cartItem.setGoodsId("GOODS300");
		cartItem.setSizeId("090");
		cartItem.setQty(4);

		GoodsSizeVO goodsSize = new GoodsSizeVO();
		goodsSize.setGoodsId("GOODS300");
		goodsSize.setSizeId("090");
		goodsSize.setStockQty(3);
		goodsSize.setDelYn("N");

		// 주문 대상 cartId 조회 결과와 현재 재고 조회 결과를 목으로 설정합니다.
		when(goodsMapper.getShopOrderCartItemList(7L, List.of(21L))).thenReturn(List.of(cartItem));
		when(goodsMapper.getAdminGoodsSizeDetail("GOODS300", "090")).thenReturn(goodsSize);

		// 재고 부족이면 주문서 응답 구성 전에 예외가 발생하는지 검증합니다.
		assertThatThrownBy(() -> goodsService.getShopOrderPage(List.of(21L), 7L, "PC", "http://127.0.0.1:3014"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("재고가 부족한 상품이 있습니다.");
	}

	@Test
	@DisplayName("쇼핑몰 주문서 할인 재계산 시 선택한 상품쿠폰/장바구니쿠폰/배송비쿠폰 기준 금액과 최대 포인트를 반환한다")
	// 할인 재계산 요청 시 선택 상태 검증 후 정규화된 선택값과 할인 금액 요약을 반환하는지 검증합니다.
	void quoteShopOrderDiscount_returnsDiscountSummaryAndMaxPointUseAmt() {
		// 주문서 대상 장바구니와 할인 재계산 요청 데이터를 구성합니다.
		ShopCartItemVO firstCartItem = createCartItem("GOODS001", 1, "095", 2, 20000);
		firstCartItem.setCartId(12L);
		firstCartItem.setSupplyAmt(22000);
		ShopCartItemVO secondCartItem = createCartItem("GOODS002", 2, "100", 1, 10000);
		secondCartItem.setCartId(15L);
		secondCartItem.setSupplyAmt(12000);

		ShopOrderDiscountQuotePO param = new ShopOrderDiscountQuotePO();
		param.setCartIdList(List.of(12L, 15L));
		param.setGoodsCouponSelectionList(List.of(
			createShopOrderGoodsCouponSelection(12L, 1001L),
			createShopOrderGoodsCouponSelection(15L, 1003L)
		));
		param.setCartCouponCustCpnNo(2001L);
		param.setDeliveryCouponCustCpnNo(3001L);

		ShopCartCustomerCouponVO allGoodsCoupon = createCustomerCoupon(1001L, 101L, "CPN_GB_01", "CPN_TARGET_99", "CPN_DC_GB_01", 3000);
		ShopCartCustomerCouponVO goodsTargetCoupon = createCustomerCoupon(1003L, 103L, "CPN_GB_01", "CPN_TARGET_01", "CPN_DC_GB_01", 5000);
		ShopCartCustomerCouponVO cartCoupon = createCustomerCoupon(2001L, 201L, "CPN_GB_03", "CPN_TARGET_99", "CPN_DC_GB_01", 7000);
		ShopCartCustomerCouponVO deliveryCoupon = createCustomerCoupon(3001L, 301L, "CPN_GB_04", "CPN_TARGET_99", "CPN_DC_GB_01", 3000);

		ShopCartSiteInfoVO siteInfo = new ShopCartSiteInfoVO();
		siteInfo.setSiteId("xodud1202");
		siteInfo.setDeliveryFee(3000);
		siteInfo.setDeliveryFeeLimit(100000);

		// 주문 대상 장바구니/쿠폰/배송 기준 목 응답을 설정합니다.
		when(goodsMapper.getShopOrderCartItemList(7L, List.of(12L, 15L))).thenReturn(List.of(firstCartItem, secondCartItem));
		when(goodsMapper.getShopCustomerCouponList(7L))
			.thenReturn(List.of(allGoodsCoupon, goodsTargetCoupon, cartCoupon, deliveryCoupon));
		when(goodsMapper.getShopCouponTargetList(101L)).thenReturn(List.of());
		when(goodsMapper.getShopCouponTargetList(103L)).thenReturn(List.of(createCouponTarget("TARGET_GB_01", "GOODS002")));
		when(goodsMapper.getShopGoodsCategoryIdList("GOODS001")).thenReturn(List.of());
		when(goodsMapper.getShopGoodsCategoryIdList("GOODS002")).thenReturn(List.of());
		when(goodsMapper.getShopGoodsExhibitionTabNoList("GOODS001")).thenReturn(List.of());
		when(goodsMapper.getShopGoodsExhibitionTabNoList("GOODS002")).thenReturn(List.of());
		when(goodsMapper.getShopCartSiteInfo("xodud1202")).thenReturn(siteInfo);
		when(goodsMapper.getShopAvailablePointAmt(7L)).thenReturn(6000);

		// 할인 재계산 결과를 조회합니다.
		ShopOrderDiscountQuoteVO result = goodsService.quoteShopOrderDiscount(param, 7L);

		// 선택 상태 정규화와 할인 금액/최대 포인트 사용 가능 금액을 검증합니다.
		assertThat(result).isNotNull();
		assertThat(result.getDiscountSelection()).isNotNull();
		assertThat(result.getDiscountSelection().getGoodsCouponSelectionList()).hasSize(2);
		assertThat(result.getDiscountSelection().getCartCouponCustCpnNo()).isEqualTo(2001L);
		assertThat(result.getDiscountSelection().getDeliveryCouponCustCpnNo()).isEqualTo(3001L);
		assertThat(result.getDiscountAmount()).isNotNull();
		assertThat(result.getDiscountAmount().getGoodsCouponDiscountAmt()).isEqualTo(8000);
		assertThat(result.getDiscountAmount().getCartCouponDiscountAmt()).isEqualTo(7000);
		assertThat(result.getDiscountAmount().getDeliveryCouponDiscountAmt()).isEqualTo(3000);
		assertThat(result.getDiscountAmount().getCouponDiscountAmt()).isEqualTo(18000);
		assertThat(result.getDiscountAmount().getMaxPointUseAmt()).isEqualTo(6000);
	}

	@Test
	@DisplayName("쇼핑몰 주문서 할인 재계산 시 같은 상품쿠폰을 중복 선택하면 예외를 반환한다")
	// 할인 재계산 요청 시 상품쿠폰 1장을 여러 상품에 동시에 선택하면 정합성 예외를 반환하는지 검증합니다.
	void quoteShopOrderDiscount_throwsWhenGoodsCouponSelectedDuplicated() {
		// 주문서 대상 장바구니와 중복 상품쿠폰 선택 요청 데이터를 구성합니다.
		ShopCartItemVO firstCartItem = createCartItem("GOODS001", 1, "095", 2, 20000);
		firstCartItem.setCartId(12L);
		ShopCartItemVO secondCartItem = createCartItem("GOODS002", 2, "100", 1, 10000);
		secondCartItem.setCartId(15L);

		ShopOrderDiscountQuotePO param = new ShopOrderDiscountQuotePO();
		param.setCartIdList(List.of(12L, 15L));
		param.setGoodsCouponSelectionList(List.of(
			createShopOrderGoodsCouponSelection(12L, 1001L),
			createShopOrderGoodsCouponSelection(15L, 1001L)
		));
		param.setCartCouponCustCpnNo(null);
		param.setDeliveryCouponCustCpnNo(null);

		ShopCartCustomerCouponVO allGoodsCoupon = createCustomerCoupon(1001L, 101L, "CPN_GB_01", "CPN_TARGET_99", "CPN_DC_GB_01", 3000);

		ShopCartSiteInfoVO siteInfo = new ShopCartSiteInfoVO();
		siteInfo.setSiteId("xodud1202");
		siteInfo.setDeliveryFee(3000);
		siteInfo.setDeliveryFeeLimit(100000);

		// 주문 대상 장바구니와 상품쿠폰 후보 목 응답을 설정합니다.
		when(goodsMapper.getShopOrderCartItemList(7L, List.of(12L, 15L))).thenReturn(List.of(firstCartItem, secondCartItem));
		when(goodsMapper.getShopCustomerCouponList(7L)).thenReturn(List.of(allGoodsCoupon));
		when(goodsMapper.getShopCouponTargetList(101L)).thenReturn(List.of());
		when(goodsMapper.getShopGoodsCategoryIdList("GOODS001")).thenReturn(List.of());
		when(goodsMapper.getShopGoodsCategoryIdList("GOODS002")).thenReturn(List.of());
		when(goodsMapper.getShopGoodsExhibitionTabNoList("GOODS001")).thenReturn(List.of());
		when(goodsMapper.getShopGoodsExhibitionTabNoList("GOODS002")).thenReturn(List.of());
		when(goodsMapper.getShopCartSiteInfo("xodud1202")).thenReturn(siteInfo);
		when(goodsMapper.getShopAvailablePointAmt(7L)).thenReturn(0);

		// 중복 상품쿠폰 선택 예외 메시지를 확인합니다.
		assertThatThrownBy(() -> goodsService.quoteShopOrderDiscount(param, 7L))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("할인 혜택 정보를 확인해주세요.");
	}

	@Test
	@DisplayName("쇼핑몰 주문서 배송지 검색 시 검색어와 페이지 정보를 정규화해 Juso API 클라이언트를 호출한다")
	// 배송지 검색 요청 시 로그인 검증 후 정규화된 검색 조건으로 클라이언트를 호출하는지 검증합니다.
	void searchShopOrderAddress_callsJusoClientWithNormalizedPaging() {
		// 주소 검색 결과 응답을 구성합니다.
		ShopOrderAddressSearchCommonVO common = new ShopOrderAddressSearchCommonVO();
		common.setErrorCode("0");
		common.setErrorMessage("정상");
		common.setTotalCount(1);
		common.setCurrentPage(1);
		common.setCountPerPage(100);

		ShopOrderAddressSearchResponseVO response = new ShopOrderAddressSearchResponseVO();
		response.setCommon(common);
		response.setJusoList(List.of());
		when(jusoAddressApiClient.searchRoadAddress("테헤란로", 1, 100)).thenReturn(response);

		// 빈칸이 포함된 검색어와 초과 건수 요청을 정규화해 검색 결과를 조회합니다.
		ShopOrderAddressSearchResponseVO result = goodsService.searchShopOrderAddress(" 테헤란로 ", 0, 999, 7L);
		assertThat(result.getCommon().getErrorCode()).isEqualTo("0");
		verify(jusoAddressApiClient).searchRoadAddress("테헤란로", 1, 100);
	}

	@Test
	@DisplayName("쇼핑몰 주문 결제 웹훅은 DEPOSIT_CALLBACK DONE 본문을 주문번호 기준으로 처리한다")
	// DEPOSIT_CALLBACK 형식의 입금완료 웹훅이 들어오면 주문번호 기준으로 결제를 찾아 완료 상태로 반영하는지 검증합니다.
	void handleShopOrderPaymentWebhook_processesDepositCallbackDoneByOrderId() {
		// 무통장입금 대기 상태의 결제 정보를 구성합니다.
		ShopOrderPaymentVO payment = new ShopOrderPaymentVO();
		payment.setPayNo(901L);
		payment.setOrdNo("O220260319170925876");
		payment.setCustNo(7L);
		payment.setPayMethodCd("PAY_METHOD_02");
		payment.setPayStatCd("PAY_STAT_05");
		payment.setRspRawJson("""
			{"secret":"ps_LkKEypNArWdRengm2gYL8lmeaxYG"}
			""");

		String rawBody = """
			{
			  "createdAt": "2026-03-19T17:28:08.000000",
			  "secret": "ps_LkKEypNArWdRengm2gYL8lmeaxYG",
			  "orderId": "O220260319170925876",
			  "status": "DONE",
			  "transactionKey": "txrd_a01km2kb38p1zca4bh97qgvaseb"
			}
			""";

		// 주문번호 기준으로 결제 row를 찾도록 목 응답을 설정합니다.
		when(goodsMapper.getShopPaymentByOrdNo("O220260319170925876")).thenReturn(payment);

		// DEPOSIT_CALLBACK DONE 웹훅을 반영합니다.
		goodsService.handleShopOrderPaymentWebhook(rawBody);

		// 주문번호 기준 조회 후 결제/주문 상태를 완료로 갱신하는지 검증합니다.
		verify(goodsMapper).getShopPaymentByOrdNo("O220260319170925876");
		verify(goodsMapper).updateShopPaymentWebhook(
			901L,
			"PAY_STAT_02",
			"DONE",
			"무통장입금 완료",
			rawBody.trim(),
			"2026-03-19 17:28:08",
			7L
		);
		verify(goodsMapper).updateShopOrderBaseStatusAndDates(
			"O220260319170925876",
			"ORD_STAT_02",
			null,
			"2026-03-19 17:28:08",
			7L
		);
		verify(goodsMapper).updateShopOrderDetailStatus("O220260319170925876", "ORD_DTL_STAT_02", 7L);
	}

	@Test
	@DisplayName("쇼핑몰 주문 결제 준비 시 재고가 부족하면 결제창 요청을 생성하지 않는다")
	// 결제 준비 단계에서 현재 주문 수량보다 재고가 부족하면 주문/결제 준비 데이터를 만들지 않는지 검증합니다.
	void prepareShopOrderPayment_throwsWhenStockIsInsufficient() {
		// 최소 결제 준비 요청과 주문 대상 장바구니를 구성합니다.
		ShopOrderPaymentPreparePO param = new ShopOrderPaymentPreparePO();
		param.setFrom("cart");
		param.setCartIdList(List.of(11L));
		param.setAddressNm("집");
		param.setDiscountSelection(new ShopOrderDiscountSelectionVO());
		param.setPointUseAmt(0);
		param.setPaymentMethodCd("PAY_METHOD_01");

		ShopCartItemVO cartItem = new ShopCartItemVO();
		cartItem.setCartId(11L);
		cartItem.setGoodsId("GOODS001");
		cartItem.setSizeId("095");
		cartItem.setQty(2);

		GoodsSizeVO goodsSize = new GoodsSizeVO();
		goodsSize.setGoodsId("GOODS001");
		goodsSize.setSizeId("095");
		goodsSize.setStockQty(1);
		goodsSize.setDelYn("N");

		// 고객 장바구니 조회와 현재 재고 조회를 목으로 설정합니다.
		when(goodsMapper.getShopOrderCartItemList(7L, List.of(11L))).thenReturn(List.of(cartItem));
		when(goodsMapper.getAdminGoodsSizeDetail("GOODS001", "095")).thenReturn(goodsSize);

		// 재고 부족이면 주문/결제 준비 생성 없이 예외가 발생하는지 검증합니다.
		assertThatThrownBy(() -> goodsService.prepareShopOrderPayment(param, 7L, "PC", "http://127.0.0.1:3014"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("재고가 부족한 상품이 있습니다.");
		verify(goodsMapper, never()).insertShopOrderBase(any());
		verify(goodsMapper, never()).insertShopPayment(any());
	}

	@Test
	@DisplayName("쇼핑몰 주문 결제 승인은 재고 차감과 후처리를 끝낸 뒤 Toss 승인 API를 호출한다")
	// 카드 결제 승인 요청 시 재고 차감과 장바구니 삭제가 Toss 승인 호출보다 먼저 수행되는지 검증합니다.
	void confirmShopOrderPayment_reservesStockAndAppliesSideEffectsBeforeTossConfirm() {
		// 승인 요청과 결제 준비 상태의 PAYMENT 데이터를 구성합니다.
		ShopOrderPaymentConfirmPO param = new ShopOrderPaymentConfirmPO();
		param.setPayNo(501L);
		param.setOrdNo("O720260319180000111");
		param.setPaymentKey("pay_test_001");
		param.setAmount(10000L);

		ShopOrderPaymentVO payment = new ShopOrderPaymentVO();
		payment.setPayNo(501L);
		payment.setOrdNo("O720260319180000111");
		payment.setCustNo(7L);
		payment.setPayAmt(10000L);
		payment.setPayStatCd("PAY_STAT_01");
		payment.setPayMethodCd("PAY_METHOD_01");
		payment.setReqRawJson("""
			{
			  "cartIdList": [11],
			  "pointUseAmt": 0,
			  "orderName": "테스트 주문",
			  "discountSelection": {
			    "goodsCouponSelectionList": [],
			    "cartCouponCustCpnNo": null,
			    "deliveryCouponCustCpnNo": null
			  }
			}
			""");

		ShopOrderPaymentVO updatedPayment = new ShopOrderPaymentVO();
		updatedPayment.setPayNo(501L);
		updatedPayment.setOrdNo("O720260319180000111");
		updatedPayment.setCustNo(7L);
		updatedPayment.setPayAmt(10000L);
		updatedPayment.setPayStatCd("PAY_STAT_02");
		updatedPayment.setPayMethodCd("PAY_METHOD_01");
		updatedPayment.setReqRawJson(payment.getReqRawJson());

		ShopOrderRestoreCartItemVO stockItem = new ShopOrderRestoreCartItemVO();
		stockItem.setGoodsId("GOODS001");
		stockItem.setSizeId("095");
		stockItem.setOrdQty(2);

		// 재고 차감, Toss 승인 응답, 최종 PAYMENT 재조회 결과를 목으로 설정합니다.
		when(goodsMapper.getShopPaymentByPayNo(501L)).thenReturn(payment, updatedPayment);
		when(goodsMapper.getShopOrderRestoreCartItemList("O720260319180000111")).thenReturn(List.of(stockItem));
		when(goodsMapper.deductShopGoodsSizeStock("GOODS001", "095", 2, 7L)).thenReturn(1);
		when(tossPaymentsClient.confirmPayment("pay_test_001", "O720260319180000111", 10000L)).thenReturn("""
			{
			  "status": "DONE",
			  "paymentKey": "pay_test_001",
			  "lastTransactionKey": "tx_test_001",
			  "approvedAt": "2026-03-19T18:00:11+09:00",
			  "orderName": "테스트 주문",
			  "card": {
			    "approveNo": "APPROVE001",
			    "issuerCode": "88",
			    "number": "1234********5678"
			  }
			}
			""");

		// 승인 요청을 수행하고 재고 차감/장바구니 삭제가 Toss 승인 호출보다 먼저 수행되는지 검증합니다.
		goodsService.confirmShopOrderPayment(param, 7L);
		InOrder inOrder = inOrder(goodsMapper, tossPaymentsClient);
		inOrder.verify(goodsMapper).getShopPaymentByPayNo(501L);
		inOrder.verify(goodsMapper).getShopOrderRestoreCartItemList("O720260319180000111");
		inOrder.verify(goodsMapper).deductShopGoodsSizeStock("GOODS001", "095", 2, 7L);
		inOrder.verify(goodsMapper).deleteShopCartByCartIdList(7L, List.of(11L));
		inOrder.verify(tossPaymentsClient).confirmPayment("pay_test_001", "O720260319180000111", 10000L);
		verify(goodsMapper).updateShopOrderBaseStatusAndDates(
			"O720260319180000111",
			"ORD_STAT_02",
			"2026-03-19 18:00:11",
			"2026-03-19 18:00:11",
			7L
		);
		verify(goodsMapper).updateShopOrderDetailStatus("O720260319180000111", "ORD_DTL_STAT_02", 7L);
	}

	@Test
	@DisplayName("쇼핑몰 주문 결제 승인 시 가상계좌 발급이면 ORDER_DT만 저장한다")
	// 무통장입금 발급 승인 시 ORDER_DT에는 PG 승인 시간을 저장하고 ORDER_CONFIRM_DT는 비워두는지 검증합니다.
	void confirmShopOrderPayment_setsOrderDtOnlyWhenVirtualAccountIssued() {
		// 승인 요청과 무통장입금 결제 준비 상태의 PAYMENT 데이터를 구성합니다.
		ShopOrderPaymentConfirmPO param = new ShopOrderPaymentConfirmPO();
		param.setPayNo(601L);
		param.setOrdNo("O720260319180500333");
		param.setPaymentKey("pay_test_002");
		param.setAmount(12000L);

		ShopOrderPaymentVO payment = new ShopOrderPaymentVO();
		payment.setPayNo(601L);
		payment.setOrdNo("O720260319180500333");
		payment.setCustNo(7L);
		payment.setPayAmt(12000L);
		payment.setPayStatCd("PAY_STAT_01");
		payment.setPayMethodCd("PAY_METHOD_02");
		payment.setReqRawJson("""
			{
			  "cartIdList": [21],
			  "pointUseAmt": 0,
			  "orderName": "무통장 주문",
			  "discountSelection": {
			    "goodsCouponSelectionList": [],
			    "cartCouponCustCpnNo": null,
			    "deliveryCouponCustCpnNo": null
			  }
			}
			""");

		ShopOrderPaymentVO updatedPayment = new ShopOrderPaymentVO();
		updatedPayment.setPayNo(601L);
		updatedPayment.setOrdNo("O720260319180500333");
		updatedPayment.setCustNo(7L);
		updatedPayment.setPayAmt(12000L);
		updatedPayment.setPayStatCd("PAY_STAT_05");
		updatedPayment.setPayMethodCd("PAY_METHOD_02");
		updatedPayment.setReqRawJson(payment.getReqRawJson());
		updatedPayment.setBankCd("88");
		updatedPayment.setBankNo("12345678901234");
		updatedPayment.setVactHolderNm("홍길동");
		updatedPayment.setVactDueDt("2026-03-26 23:59:59");

		ShopOrderRestoreCartItemVO stockItem = new ShopOrderRestoreCartItemVO();
		stockItem.setGoodsId("GOODS001");
		stockItem.setSizeId("095");
		stockItem.setOrdQty(1);

		// 재고 차감, 무통장입금 발급 응답, 최종 PAYMENT 재조회 결과를 목으로 설정합니다.
		when(goodsMapper.getShopPaymentByPayNo(601L)).thenReturn(payment, updatedPayment);
		when(goodsMapper.getShopOrderRestoreCartItemList("O720260319180500333")).thenReturn(List.of(stockItem));
		when(goodsMapper.deductShopGoodsSizeStock("GOODS001", "095", 1, 7L)).thenReturn(1);
		when(shopAuthService.getCommonCodeName("BANK", "88")).thenReturn("신한은행");
		when(tossPaymentsClient.confirmPayment("pay_test_002", "O720260319180500333", 12000L)).thenReturn("""
			{
			  "status": "WAITING_FOR_DEPOSIT",
			  "paymentKey": "pay_test_002",
			  "lastTransactionKey": "tx_test_002",
			  "approvedAt": "2026-03-19T18:05:12+09:00",
			  "orderName": "무통장 주문",
			  "secret": "ps_test_002",
			  "virtualAccount": {
			    "dueDate": "2026-03-26T23:59:59+09:00",
			    "bankCode": "88",
			    "accountNumber": "12345678901234",
			    "customerName": "홍길동"
			  }
			}
			""");

		// 무통장입금 발급 승인 시 ORDER_DT만 저장하고 주문상태를 입금대기로 변경하는지 검증합니다.
		ShopOrderPaymentConfirmVO result = goodsService.confirmShopOrderPayment(param, 7L);
		verify(goodsMapper).updateShopOrderBaseStatusAndDates(
			"O720260319180500333",
			"ORD_STAT_01",
			"2026-03-19 18:05:12",
			null,
			7L
		);
		verify(goodsMapper).updateShopOrderDetailStatus("O720260319180500333", "ORD_DTL_STAT_01", 7L);
		assertThat(result.getBankNm()).isEqualTo("신한은행");
	}

	@Test
	@DisplayName("쇼핑몰 주문 결제 웹훅은 무통장입금 만료 시 재고를 복구한다")
	// 무통장입금 만료 웹훅이 들어오면 차감했던 재고와 후처리 자원을 함께 복구하는지 검증합니다.
	void handleShopOrderPaymentWebhook_restoresStockWhenDepositExpired() {
		// 무통장입금 대기 상태의 결제 정보와 주문 상세 재고 복구 대상을 구성합니다.
		ShopOrderPaymentVO payment = new ShopOrderPaymentVO();
		payment.setPayNo(777L);
		payment.setOrdNo("O720260319181500222");
		payment.setCustNo(7L);
		payment.setPayMethodCd("PAY_METHOD_02");
		payment.setPayStatCd("PAY_STAT_05");
		payment.setRspRawJson("""
			{"secret":"ps_LkKEypNArWdRengm2gYL8lmeaxYG"}
			""");

		ShopOrderRestoreCartItemVO stockItem = new ShopOrderRestoreCartItemVO();
		stockItem.setCustNo(7L);
		stockItem.setGoodsId("GOODS001");
		stockItem.setSizeId("095");
		stockItem.setOrdQty(2);
		stockItem.setExhibitionNo(10);

		String rawBody = """
			{
			  "createdAt": "2026-03-19T18:16:08.000000",
			  "secret": "ps_LkKEypNArWdRengm2gYL8lmeaxYG",
			  "orderId": "O720260319181500222",
			  "status": "EXPIRED",
			  "transactionKey": "txrd_a01km2kb38p1zca4bh97qgvaseb"
			}
			""";

		// 주문번호 기준 결제 조회와 포인트/장바구니 복구 조회를 목으로 설정합니다.
		when(goodsMapper.getShopPaymentByOrdNo("O720260319181500222")).thenReturn(payment);
		when(goodsMapper.getShopOrderPointDetailList("O720260319181500222")).thenReturn(List.of());
		when(goodsMapper.getShopOrderRestoreCartItemList("O720260319181500222")).thenReturn(List.of(stockItem));

		// 만료 웹훅 반영 시 재고 복구와 주문 취소 상태 갱신이 함께 수행되는지 검증합니다.
		goodsService.handleShopOrderPaymentWebhook(rawBody);
		verify(goodsMapper).restoreShopGoodsSizeStock("GOODS001", "095", 2, 7L);
		verify(goodsMapper).updateShopOrderBaseStatus("O720260319181500222", "ORD_STAT_99", 7L);
		verify(goodsMapper).updateShopOrderDetailStatus("O720260319181500222", "ORD_DTL_STAT_99", 7L);
	}

	@Test
	@DisplayName("쇼핑몰 주문서 배송지 등록 시 기본 배송지 저장 요청이면 기존 기본 배송지를 해제하고 최신 목록을 반환한다")
	// 배송지 등록 요청 시 기본 배송지 해제, 신규 등록, 최신 목록 구성 순서가 수행되는지 검증합니다.
	void registerShopOrderAddress_resetsDefaultAndReturnsLatestAddresses() {
		// 배송지 등록 요청과 최신 배송지 목록 데이터를 구성합니다.
		ShopOrderAddressRegisterPO param = new ShopOrderAddressRegisterPO();
		param.setAddressNm("회사");
		param.setPostNo("06234");
		param.setBaseAddress("서울특별시 강남구 테헤란로 1");
		param.setDetailAddress("101동 1001호");
		param.setPhoneNumber("010-1234-5678");
		param.setRsvNm("홍길동");
		param.setDefaultYn("Y");

		ShopOrderAddressVO savedAddress = new ShopOrderAddressVO();
		savedAddress.setCustNo(7L);
		savedAddress.setAddressNm("회사");
		savedAddress.setPostNo("06234");
		savedAddress.setBaseAddress("서울특별시 강남구 테헤란로 1");
		savedAddress.setDetailAddress("101동 1001호");
		savedAddress.setPhoneNumber("010-1234-5678");
		savedAddress.setRsvNm("홍길동");
		savedAddress.setDefaultYn("Y");

		// 중복 없음, 기본 배송지 해제, 등록 후 최신 목록 조회 흐름을 목으로 설정합니다.
		when(goodsMapper.countShopOrderAddressName(7L, "회사")).thenReturn(0);
		when(goodsMapper.updateShopOrderAddressDefaultYn(7L, "N", 7L)).thenReturn(1);
		when(goodsMapper.insertShopOrderAddress(any(ShopOrderAddressSavePO.class))).thenReturn(1);
		when(goodsMapper.getShopOrderAddressList(7L)).thenReturn(List.of(savedAddress));

		// 배송지를 등록한 뒤 기본 배송지 해제 호출과 최신 결과 구성을 검증합니다.
		ShopOrderAddressSaveResultVO result = goodsService.registerShopOrderAddress(param, 7L);
		ArgumentCaptor<ShopOrderAddressSavePO> captor = ArgumentCaptor.forClass(ShopOrderAddressSavePO.class);
		verify(goodsMapper).updateShopOrderAddressDefaultYn(7L, "N", 7L);
		verify(goodsMapper).insertShopOrderAddress(captor.capture());
		assertThat(captor.getValue().getAddressNm()).isEqualTo("회사");
		assertThat(captor.getValue().getDefaultYn()).isEqualTo("Y");
		assertThat(result.getSavedAddress()).isNotNull();
		assertThat(result.getSavedAddress().getAddressNm()).isEqualTo("회사");
		assertThat(result.getDefaultAddress()).isNotNull();
		assertThat(result.getDefaultAddress().getAddressNm()).isEqualTo("회사");
	}

	@Test
	@DisplayName("쇼핑몰 주문서 배송지 등록 시 같은 고객의 배송지명이 중복되면 예외를 반환한다")
	// 배송지 등록 요청 시 고객번호 기준 주소별칭이 이미 있으면 중복 예외를 반환하는지 검증합니다.
	void registerShopOrderAddress_throwsWhenAddressNameDuplicated() {
		// 중복 배송지명 등록 요청을 구성합니다.
		ShopOrderAddressRegisterPO param = new ShopOrderAddressRegisterPO();
		param.setAddressNm("회사");
		param.setPostNo("06234");
		param.setBaseAddress("서울특별시 강남구 테헤란로 1");
		param.setDetailAddress("101동 1001호");
		param.setPhoneNumber("010-1234-5678");
		param.setRsvNm("홍길동");
		param.setDefaultYn("N");

		// 같은 고객의 동일 배송지명이 이미 존재하는 상태를 목으로 설정합니다.
		when(goodsMapper.countShopOrderAddressName(7L, "회사")).thenReturn(1);

		// 중복 검증 예외 메시지를 확인합니다.
		assertThatThrownBy(() -> goodsService.registerShopOrderAddress(param, 7L))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("이미 사용 중인 배송지명입니다.");
	}

	@Test
	@DisplayName("쇼핑몰 주문서 배송지 수정 시 기본 배송지 저장 요청이면 기존 기본 배송지를 해제하고 최신 목록을 반환한다")
	// 배송지 수정 요청 시 대상 존재 검증, 기본 배송지 해제, 수정 반영, 최신 목록 구성이 수행되는지 검증합니다.
	void updateShopOrderAddress_resetsDefaultAndReturnsLatestAddresses() {
		// 배송지 수정 요청과 최신 배송지 목록 데이터를 구성합니다.
		ShopOrderAddressUpdatePO param = new ShopOrderAddressUpdatePO();
		param.setOriginAddressNm("집");
		param.setAddressNm("우리집");
		param.setPostNo("06234");
		param.setBaseAddress("서울특별시 강남구 테헤란로 1");
		param.setDetailAddress("201동 202호");
		param.setPhoneNumber("010-2222-3333");
		param.setRsvNm("홍길동");
		param.setDefaultYn("Y");

		ShopOrderAddressVO savedAddress = new ShopOrderAddressVO();
		savedAddress.setCustNo(7L);
		savedAddress.setAddressNm("우리집");
		savedAddress.setPostNo("06234");
		savedAddress.setBaseAddress("서울특별시 강남구 테헤란로 1");
		savedAddress.setDetailAddress("201동 202호");
		savedAddress.setPhoneNumber("010-2222-3333");
		savedAddress.setRsvNm("홍길동");
		savedAddress.setDefaultYn("Y");

		// 수정 대상 존재, 별칭 중복 없음, 기본 배송지 해제, 수정 후 최신 목록 조회 흐름을 목으로 설정합니다.
		when(goodsMapper.countShopOrderAddress(7L, "집")).thenReturn(1);
		when(goodsMapper.countShopOrderAddressName(7L, "우리집")).thenReturn(0);
		when(goodsMapper.updateShopOrderAddressDefaultYn(7L, "N", 7L)).thenReturn(1);
		when(goodsMapper.updateShopOrderAddress(
			7L,
			"집",
			"우리집",
			"06234",
			"서울특별시 강남구 테헤란로 1",
			"201동 202호",
			"010-2222-3333",
			"홍길동",
			"Y",
			7L
		)).thenReturn(1);
		when(goodsMapper.getShopOrderAddressList(7L)).thenReturn(List.of(savedAddress));

		// 배송지를 수정한 뒤 기본 배송지 해제 호출과 최신 결과 구성을 검증합니다.
		ShopOrderAddressSaveResultVO result = goodsService.updateShopOrderAddress(param, 7L);
		verify(goodsMapper).updateShopOrderAddressDefaultYn(7L, "N", 7L);
		verify(goodsMapper).updateShopOrderAddress(
			7L,
			"집",
			"우리집",
			"06234",
			"서울특별시 강남구 테헤란로 1",
			"201동 202호",
			"010-2222-3333",
			"홍길동",
			"Y",
			7L
		);
		assertThat(result.getSavedAddress()).isNotNull();
		assertThat(result.getSavedAddress().getAddressNm()).isEqualTo("우리집");
		assertThat(result.getDefaultAddress()).isNotNull();
		assertThat(result.getDefaultAddress().getAddressNm()).isEqualTo("우리집");
	}

	@Test
	@DisplayName("쇼핑몰 주문서 배송지 수정 시 바뀐 배송지명이 같은 고객에게 이미 있으면 예외를 반환한다")
	// 배송지 수정 요청 시 기존과 다른 배송지명으로 변경하면서 중복이 발생하면 예외를 반환하는지 검증합니다.
	void updateShopOrderAddress_throwsWhenRenamedAddressNameDuplicated() {
		// 중복 배송지명 변경 요청을 구성합니다.
		ShopOrderAddressUpdatePO param = new ShopOrderAddressUpdatePO();
		param.setOriginAddressNm("집");
		param.setAddressNm("회사");
		param.setPostNo("06234");
		param.setBaseAddress("서울특별시 강남구 테헤란로 1");
		param.setDetailAddress("101동 1001호");
		param.setPhoneNumber("010-1234-5678");
		param.setRsvNm("홍길동");
		param.setDefaultYn("N");

		// 수정 대상은 존재하지만 변경할 배송지명이 이미 존재하는 상태를 목으로 설정합니다.
		when(goodsMapper.countShopOrderAddress(7L, "집")).thenReturn(1);
		when(goodsMapper.countShopOrderAddressName(7L, "회사")).thenReturn(1);

		// 중복 검증 예외 메시지를 확인합니다.
		assertThatThrownBy(() -> goodsService.updateShopOrderAddress(param, 7L))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("이미 사용 중인 배송지명입니다.");
	}

	@Test
	@DisplayName("쇼핑몰 주문서 배송지 수정 시 수정 대상 배송지가 없으면 예외를 반환한다")
	// 배송지 수정 요청 시 origin 배송지 자체가 없으면 수정 대상 없음 예외를 반환하는지 검증합니다.
	void updateShopOrderAddress_throwsWhenOriginAddressMissing() {
		// 존재하지 않는 배송지 수정 요청을 구성합니다.
		ShopOrderAddressUpdatePO param = new ShopOrderAddressUpdatePO();
		param.setOriginAddressNm("집");
		param.setAddressNm("우리집");
		param.setPostNo("06234");
		param.setBaseAddress("서울특별시 강남구 테헤란로 1");
		param.setDetailAddress("201동 202호");
		param.setPhoneNumber("010-2222-3333");
		param.setRsvNm("홍길동");
		param.setDefaultYn("N");

		// 수정 대상 배송지가 없는 상태를 목으로 설정합니다.
		when(goodsMapper.countShopOrderAddress(7L, "집")).thenReturn(0);

		// 수정 대상 없음 예외 메시지를 확인합니다.
		assertThatThrownBy(() -> goodsService.updateShopOrderAddress(param, 7L))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("수정할 배송지를 찾을 수 없습니다.");
	}

	@Test
	@DisplayName("쇼핑몰 장바구니 쿠폰 예상 할인 계산 시 상품쿠폰 최적 매칭과 장바구니/배송비쿠폰 최대값을 합산한다")
	// 예상 할인 계산 시 상품쿠폰 최대 가중치 매칭과 후순위 쿠폰 합산 규칙이 올바르게 동작하는지 검증합니다.
	void getShopCartCouponEstimate_returnsMaximumCombinedDiscount() {
		// 선택 장바구니와 예상 할인 계산 요청 데이터를 구성합니다.
		ShopCartItemVO firstCartItem = createCartItem("GOODS001", 1, "095", 2, 20000);
		ShopCartItemVO secondCartItem = createCartItem("GOODS002", 2, "100", 1, 10000);
		ShopCartCouponEstimateRequestPO param = createCouponEstimateRequest(
			createCouponEstimateItem("GOODS001", "095"),
			createCouponEstimateItem("GOODS002", "100")
		);

		ShopCartCustomerCouponVO allGoodsCoupon = createCustomerCoupon(1001L, 101L, "CPN_GB_01", "CPN_TARGET_99", "CPN_DC_GB_01", 3000);
		ShopCartCustomerCouponVO brandGoodsCoupon = createCustomerCoupon(1002L, 102L, "CPN_GB_01", "CPN_TARGET_04", "CPN_DC_GB_02", 10);
		ShopCartCustomerCouponVO goodsTargetCoupon = createCustomerCoupon(1003L, 103L, "CPN_GB_01", "CPN_TARGET_01", "CPN_DC_GB_01", 5000);
		ShopCartCustomerCouponVO cartCoupon = createCustomerCoupon(2001L, 201L, "CPN_GB_03", "CPN_TARGET_99", "CPN_DC_GB_01", 7000);
		ShopCartCustomerCouponVO deliveryCoupon = createCustomerCoupon(3001L, 301L, "CPN_GB_04", "CPN_TARGET_99", "CPN_DC_GB_01", 3000);

		ShopCartSiteInfoVO siteInfo = new ShopCartSiteInfoVO();
		siteInfo.setSiteId("xodud1202");
		siteInfo.setDeliveryFee(3000);
		siteInfo.setDeliveryFeeLimit(100000);

		// 장바구니/쿠폰/쿠폰 타겟/배송비 기준 목 응답을 설정합니다.
		when(goodsMapper.getShopCartItemList(7L)).thenReturn(List.of(firstCartItem, secondCartItem));
		when(goodsMapper.getShopCustomerCouponList(7L))
			.thenReturn(List.of(allGoodsCoupon, brandGoodsCoupon, goodsTargetCoupon, cartCoupon, deliveryCoupon));
		when(goodsMapper.getShopCouponTargetList(101L)).thenReturn(List.of());
		when(goodsMapper.getShopCouponTargetList(102L)).thenReturn(List.of(createCouponTarget("TARGET_GB_01", "1")));
		when(goodsMapper.getShopCouponTargetList(103L)).thenReturn(List.of(createCouponTarget("TARGET_GB_01", "GOODS002")));
		when(goodsMapper.getShopGoodsCategoryIdList("GOODS001")).thenReturn(List.of());
		when(goodsMapper.getShopGoodsCategoryIdList("GOODS002")).thenReturn(List.of());
		when(goodsMapper.getShopGoodsExhibitionTabNoList("GOODS001")).thenReturn(List.of());
		when(goodsMapper.getShopGoodsExhibitionTabNoList("GOODS002")).thenReturn(List.of());
		when(goodsMapper.getShopCartSiteInfo("xodud1202")).thenReturn(siteInfo);

		// 예상 할인 계산 결과를 조회합니다.
		ShopCartCouponEstimateVO result = goodsService.getShopCartCouponEstimate(param, 7L);

		// 상품쿠폰 9,000원 + 장바구니쿠폰 7,000원 + 배송비쿠폰 3,000원 조합이 선택되는지 검증합니다.
		assertThat(result).isNotNull();
		assertThat(result.getGoodsCouponDiscountAmt()).isEqualTo(9000);
		assertThat(result.getCartCouponDiscountAmt()).isEqualTo(7000);
		assertThat(result.getDeliveryCouponDiscountAmt()).isEqualTo(3000);
		assertThat(result.getExpectedMaxDiscountAmt()).isEqualTo(19000);
	}

	@Test
	@DisplayName("쇼핑몰 장바구니 쿠폰 예상 할인 계산 시 선택 대상이 비어 있으면 0원 결과를 반환한다")
	// 빈 선택 요청에서는 장바구니/쿠폰 조회 없이 0원 결과를 반환하는지 검증합니다.
	void getShopCartCouponEstimate_returnsZeroWhenSelectionMissing() {
		// 빈 선택 요청 데이터를 구성합니다.
		ShopCartCouponEstimateRequestPO param = new ShopCartCouponEstimateRequestPO();
		param.setCartItemList(List.of());

		// 예상 할인 계산 결과를 조회합니다.
		ShopCartCouponEstimateVO result = goodsService.getShopCartCouponEstimate(param, 7L);

		// 모든 예상 할인 금액이 0원인지 확인합니다.
		assertThat(result).isNotNull();
		assertThat(result.getGoodsCouponDiscountAmt()).isEqualTo(0);
		assertThat(result.getCartCouponDiscountAmt()).isEqualTo(0);
		assertThat(result.getDeliveryCouponDiscountAmt()).isEqualTo(0);
		assertThat(result.getExpectedMaxDiscountAmt()).isEqualTo(0);
	}

	@Test
	@DisplayName("쇼핑몰 장바구니 쿠폰 예상 할인 계산 시 무료배송이면 배송비쿠폰 할인 금액은 0원이다")
	// 무료배송 조건 충족 시 배송비쿠폰이 있어도 예상 배송비 할인 금액이 0원으로 유지되는지 검증합니다.
	void getShopCartCouponEstimate_returnsZeroDeliveryDiscountWhenFreeDelivery() {
		// 무료배송 조건을 충족하는 장바구니와 배송비쿠폰 요청 데이터를 구성합니다.
		ShopCartItemVO cartItem = createCartItem("GOODS001", 1, "095", 1, 50000);
		ShopCartCouponEstimateRequestPO param = createCouponEstimateRequest(createCouponEstimateItem("GOODS001", "095"));
		ShopCartCustomerCouponVO deliveryCoupon = createCustomerCoupon(3001L, 301L, "CPN_GB_04", "CPN_TARGET_99", "CPN_DC_GB_01", 3000);

		ShopCartSiteInfoVO siteInfo = new ShopCartSiteInfoVO();
		siteInfo.setSiteId("xodud1202");
		siteInfo.setDeliveryFee(3000);
		siteInfo.setDeliveryFeeLimit(30000);

		// 장바구니/쿠폰/배송비 기준 목 응답을 설정합니다.
		when(goodsMapper.getShopCartItemList(7L)).thenReturn(List.of(cartItem));
		when(goodsMapper.getShopCustomerCouponList(7L)).thenReturn(List.of(deliveryCoupon));
		when(goodsMapper.getShopGoodsCategoryIdList("GOODS001")).thenReturn(List.of());
		when(goodsMapper.getShopGoodsExhibitionTabNoList("GOODS001")).thenReturn(List.of());
		when(goodsMapper.getShopCartSiteInfo("xodud1202")).thenReturn(siteInfo);

		// 예상 할인 계산 결과를 조회합니다.
		ShopCartCouponEstimateVO result = goodsService.getShopCartCouponEstimate(param, 7L);

		// 무료배송 조건 충족으로 배송비 할인과 총 예상 할인이 0원인지 검증합니다.
		assertThat(result).isNotNull();
		assertThat(result.getDeliveryCouponDiscountAmt()).isEqualTo(0);
		assertThat(result.getExpectedMaxDiscountAmt()).isEqualTo(0);
	}

	@Test
	@DisplayName("쇼핑몰 장바구니 옵션 변경 시 목표 옵션이 이미 존재하면 수량을 병합하고 기존 행을 삭제한다")
	// 장바구니 옵션 변경 시 동일 상품의 목표 사이즈 행이 있으면 병합 로직이 동작하는지 검증합니다.
	void updateShopCartOption_mergesWhenTargetSizeExists() {
		// 옵션 변경 요청 데이터를 구성합니다.
		ShopCartOptionUpdatePO param = new ShopCartOptionUpdatePO();
		param.setGoodsId("GOODS001");
		param.setSizeId("095");
		param.setTargetSizeId("100");
		param.setQty(2);

		ShopGoodsSizeItemVO targetSize = new ShopGoodsSizeItemVO();
		targetSize.setGoodsId("GOODS001");
		targetSize.setSizeId("100");
		targetSize.setStockQty(8);

		// 소스/목표 행 존재 및 기존 수량 목 응답을 설정합니다.
		when(goodsMapper.countShopCart(7L, "GOODS001", "095")).thenReturn(1);
		when(goodsMapper.getShopGoodsSizeList("GOODS001")).thenReturn(List.of(targetSize));
		when(goodsMapper.countShopCart(7L, "GOODS001", "100")).thenReturn(1);
		when(goodsMapper.getShopCartQty(7L, "GOODS001", "100")).thenReturn(3);
		when(goodsMapper.updateShopCartQty(7L, "GOODS001", "100", 5, 7L)).thenReturn(1);
		when(goodsMapper.deleteShopCartItem(7L, "GOODS001", "095")).thenReturn(1);

		// 옵션 변경을 수행합니다.
		goodsService.updateShopCartOption(param, 7L);

		// 목표 행 수량 병합과 기존 행 삭제 호출 여부를 검증합니다.
		verify(goodsMapper).updateShopCartQty(7L, "GOODS001", "100", 5, 7L);
		verify(goodsMapper).deleteShopCartItem(7L, "GOODS001", "095");
		verify(goodsMapper, times(0)).updateShopCartOption(7L, "GOODS001", "095", "100", 2, 7L);
	}

	@Test
	@DisplayName("쇼핑몰 장바구니 옵션 변경 시 동일 사이즈 선택이면 수량만 갱신한다")
	// 옵션 변경 요청의 기존/목표 사이즈가 같으면 수량 업데이트만 수행하는지 검증합니다.
	void updateShopCartOption_updatesQtyWhenSameSize() {
		// 옵션 변경 요청 데이터를 구성합니다.
		ShopCartOptionUpdatePO param = new ShopCartOptionUpdatePO();
		param.setGoodsId("GOODS002");
		param.setSizeId("100");
		param.setTargetSizeId("100");
		param.setQty(4);

		ShopGoodsSizeItemVO sameSize = new ShopGoodsSizeItemVO();
		sameSize.setGoodsId("GOODS002");
		sameSize.setSizeId("100");
		sameSize.setStockQty(10);

		// 소스 행 존재와 동일 사이즈 재고를 목으로 설정합니다.
		when(goodsMapper.countShopCart(8L, "GOODS002", "100")).thenReturn(1);
		when(goodsMapper.getShopGoodsSizeList("GOODS002")).thenReturn(List.of(sameSize));
		when(goodsMapper.updateShopCartQty(8L, "GOODS002", "100", 4, 8L)).thenReturn(1);

		// 옵션 변경을 수행합니다.
		goodsService.updateShopCartOption(param, 8L);

		// 동일 사이즈 수량 갱신만 호출되는지 검증합니다.
		verify(goodsMapper).updateShopCartQty(8L, "GOODS002", "100", 4, 8L);
		verify(goodsMapper, times(0)).deleteShopCartItem(8L, "GOODS002", "100");
	}

	@Test
	@DisplayName("쇼핑몰 장바구니 옵션 변경 시 수량이 1 미만이면 예외를 반환한다")
	// 옵션 변경 요청의 경계값 수량 검증(0 이하)을 수행하는지 확인합니다.
	void updateShopCartOption_throwsWhenQtyInvalid() {
		// 수량 0 경계값으로 옵션 변경 요청 데이터를 구성합니다.
		ShopCartOptionUpdatePO param = new ShopCartOptionUpdatePO();
		param.setGoodsId("GOODS003");
		param.setSizeId("095");
		param.setTargetSizeId("100");
		param.setQty(0);

		// 수량 검증 예외 메시지를 확인합니다.
		assertThatThrownBy(() -> goodsService.updateShopCartOption(param, 9L))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("수량을 확인해주세요.");
	}

	@Test
	@DisplayName("쇼핑몰 장바구니 선택 삭제 시 중복 선택 키는 한 번만 삭제한다")
	// 선택 삭제 요청에 동일 상품/사이즈가 중복되어도 단건 삭제가 중복 호출되지 않는지 검증합니다.
	void deleteShopCartItems_deletesUniqueSelectionOnlyOnce() {
		// 중복 키를 포함한 선택 삭제 요청 데이터를 구성합니다.
		ShopCartDeleteItemPO first = new ShopCartDeleteItemPO();
		first.setGoodsId("GOODS001");
		first.setSizeId("095");

		ShopCartDeleteItemPO duplicated = new ShopCartDeleteItemPO();
		duplicated.setGoodsId("GOODS001");
		duplicated.setSizeId("095");

		ShopCartDeleteItemPO second = new ShopCartDeleteItemPO();
		second.setGoodsId("GOODS002");
		second.setSizeId("100");

		ShopCartDeletePO param = new ShopCartDeletePO();
		param.setCartItemList(List.of(first, duplicated, second));

		// 장바구니 단건 삭제 목 응답을 설정합니다.
		when(goodsMapper.deleteShopCartItem(7L, "GOODS001", "095")).thenReturn(1);
		when(goodsMapper.deleteShopCartItem(7L, "GOODS002", "100")).thenReturn(1);

		// 선택 삭제를 수행합니다.
		int deletedCount = goodsService.deleteShopCartItems(param, 7L);

		// 중복 키를 제외한 2건만 삭제되었는지 검증합니다.
		assertThat(deletedCount).isEqualTo(2);
		verify(goodsMapper, times(1)).deleteShopCartItem(7L, "GOODS001", "095");
		verify(goodsMapper, times(1)).deleteShopCartItem(7L, "GOODS002", "100");
	}

	@Test
	@DisplayName("쇼핑몰 장바구니 선택 삭제 시 삭제 대상이 비어 있으면 예외를 반환한다")
	// 선택 삭제 요청에 대상 목록이 없을 때 필수값 예외를 반환하는지 검증합니다.
	void deleteShopCartItems_throwsWhenSelectionMissing() {
		// 빈 선택 삭제 요청 데이터를 구성합니다.
		ShopCartDeletePO param = new ShopCartDeletePO();
		param.setCartItemList(List.of());

		// 삭제 대상 누락 예외 메시지를 확인합니다.
		assertThatThrownBy(() -> goodsService.deleteShopCartItems(param, 7L))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("삭제할 상품을 선택해주세요.");
	}

	@Test
	@DisplayName("쇼핑몰 장바구니 전체 삭제 시 고객번호 기준으로 전체 삭제를 수행한다")
	// 전체 삭제 요청 시 cart 테이블의 고객번호 기준 delete가 호출되는지 검증합니다.
	void deleteShopCartAll_deletesByCustNo() {
		// 전체 삭제 건수를 4건으로 목 설정합니다.
		when(goodsMapper.deleteShopCartAll(10L)).thenReturn(4);

		// 전체 삭제를 수행합니다.
		int deletedCount = goodsService.deleteShopCartAll(10L);

		// 삭제 건수와 삭제 호출 여부를 검증합니다.
		assertThat(deletedCount).isEqualTo(4);
		verify(goodsMapper).deleteShopCartAll(10L);
	}

	// 테스트용 장바구니 상품 행 데이터를 생성합니다.
	private ShopCartItemVO createCartItem(String goodsId, Integer brandNo, String sizeId, Integer qty, Integer saleAmt) {
		// 상품/브랜드/수량/판매가 정보를 세팅합니다.
		ShopCartItemVO cartItem = new ShopCartItemVO();
		cartItem.setCustNo(7L);
		cartItem.setGoodsId(goodsId);
		cartItem.setBrandNo(brandNo);
		cartItem.setGoodsNm(goodsId + "_NAME");
		cartItem.setSizeId(sizeId);
		cartItem.setQty(qty);
		cartItem.setSaleAmt(saleAmt);
		cartItem.setSupplyAmt(saleAmt);
		return cartItem;
	}

	// 테스트용 장바구니 쿠폰 예상 할인 요청 행 데이터를 생성합니다.
	private ShopCartCouponEstimateItemPO createCouponEstimateItem(String goodsId, String sizeId) {
		// 선택 상품코드와 사이즈코드를 세팅합니다.
		ShopCartCouponEstimateItemPO cartItem = new ShopCartCouponEstimateItemPO();
		cartItem.setGoodsId(goodsId);
		cartItem.setSizeId(sizeId);
		return cartItem;
	}

	// 테스트용 장바구니 쿠폰 예상 할인 요청 객체를 생성합니다.
	private ShopCartCouponEstimateRequestPO createCouponEstimateRequest(ShopCartCouponEstimateItemPO... cartItemList) {
		// 가변 인자 행 목록을 요청 객체에 세팅합니다.
		ShopCartCouponEstimateRequestPO request = new ShopCartCouponEstimateRequestPO();
		request.setCartItemList(List.of(cartItemList));
		return request;
	}

	// 테스트용 주문서 상품쿠폰 선택 항목을 생성합니다.
	private ShopOrderGoodsCouponSelectionVO createShopOrderGoodsCouponSelection(Long cartId, Long custCpnNo) {
		// 장바구니 번호와 고객 보유 쿠폰 번호를 세팅합니다.
		ShopOrderGoodsCouponSelectionVO selection = new ShopOrderGoodsCouponSelectionVO();
		selection.setCartId(cartId);
		selection.setCustCpnNo(custCpnNo);
		return selection;
	}

	// 테스트용 고객 보유 쿠폰 정보를 생성합니다.
	private ShopCartCustomerCouponVO createCustomerCoupon(
		Long custCpnNo,
		Long cpnNo,
		String cpnGbCd,
		String cpnTargetCd,
		String cpnDcGbCd,
		Integer cpnDcVal
	) {
		// 고객 보유 쿠폰 번호와 할인 정보를 세팅합니다.
		ShopCartCustomerCouponVO coupon = new ShopCartCustomerCouponVO();
		coupon.setCustCpnNo(custCpnNo);
		coupon.setCustNo(7L);
		coupon.setCpnNo(cpnNo);
		coupon.setCpnGbCd(cpnGbCd);
		coupon.setCpnTargetCd(cpnTargetCd);
		coupon.setCpnDcGbCd(cpnDcGbCd);
		coupon.setCpnDcVal(cpnDcVal);
		return coupon;
	}

	// 테스트용 쿠폰 타겟 정보를 생성합니다.
	private ShopGoodsCouponTargetVO createCouponTarget(String targetGbCd, String targetValue) {
		// 타겟 구분 코드와 타겟 값을 세팅합니다.
		ShopGoodsCouponTargetVO target = new ShopGoodsCouponTargetVO();
		target.setTargetGbCd(targetGbCd);
		target.setTargetValue(targetValue);
		return target;
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
