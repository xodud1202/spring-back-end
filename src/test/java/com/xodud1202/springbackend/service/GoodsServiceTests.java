package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.admin.category.CategoryGoodsSavePO;
import com.xodud1202.springbackend.domain.admin.category.CategoryVO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsCategoryItem;
import com.xodud1202.springbackend.domain.admin.goods.GoodsCategorySavePO;
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
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageWishGoodsItemVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageWishPageVO;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

	@Mock
	private ShopAuthService shopAuthService;

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
		when(goodsMapper.insertShopCart(7L, "GOODS001", "095", 2, 7L, 7L)).thenReturn(1);
		when(goodsMapper.getShopCartQty(7L, "GOODS001", "095")).thenReturn(2);

		// 장바구니 등록 후 최종 수량과 insert 호출 여부를 검증합니다.
		int result = goodsService.addShopGoodsCart("GOODS001", "095", 2, 7L);
		assertThat(result).isEqualTo(2);
		verify(goodsMapper).insertShopCart(7L, "GOODS001", "095", 2, 7L, 7L);
	}

	@Test
	@DisplayName("쇼핑몰 장바구니 등록 시 기존 상품은 수량을 가산하고 최종 수량을 반환한다")
	// 장바구니 등록 상태에서 동일 상품/사이즈를 등록하면 수량 가산 여부를 검증합니다.
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
		when(goodsMapper.addShopCartQty(8L, "GOODS002", "100", 3, 8L)).thenReturn(1);
		when(goodsMapper.getShopCartQty(8L, "GOODS002", "100")).thenReturn(5);

		// 장바구니 등록 후 최종 수량과 update 호출 여부를 검증합니다.
		int result = goodsService.addShopGoodsCart("GOODS002", "100", 3, 8L);
		assertThat(result).isEqualTo(5);
		verify(goodsMapper).addShopCartQty(8L, "GOODS002", "100", 3, 8L);
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
		assertThatThrownBy(() -> goodsService.addShopGoodsCart("GOODS003", "110", 1, 9L))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("사이즈를 확인해주세요.");
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
