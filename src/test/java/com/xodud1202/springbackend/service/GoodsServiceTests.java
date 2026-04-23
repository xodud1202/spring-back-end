package com.xodud1202.springbackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xodud1202.springbackend.config.properties.TossProperties;
import com.xodud1202.springbackend.domain.admin.category.CategoryGoodsSavePO;
import com.xodud1202.springbackend.domain.admin.category.CategoryVO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsCategorySavePO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsBasicVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsCouponVO;
import com.xodud1202.springbackend.mapper.CartMapper;
import com.xodud1202.springbackend.mapper.CommonMapper;
import com.xodud1202.springbackend.mapper.ExhibitionMapper;
import com.xodud1202.springbackend.mapper.GoodsMapper;
import com.xodud1202.springbackend.mapper.SiteInfoMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

import static com.xodud1202.springbackend.common.Constants.Shop.CPN_GB_GOODS;
import static com.xodud1202.springbackend.common.Constants.Shop.CPN_TARGET_ALL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// 상품 서비스의 현재 상품/쿠폰 책임을 검증합니다.
class GoodsServiceTests {
	@Mock
	private CartMapper cartMapper;

	@Mock
	private GoodsMapper goodsMapper;

	@Mock
	private CommonMapper commonMapper;

	@Mock
	private ExhibitionMapper exhibitionMapper;

	@Mock
	private SiteInfoMapper siteInfoMapper;

	@Mock
	private FtpFileService ftpFileService;

	@Mock
	private GoodsImageService goodsImageService;

	@Mock
	private ShopCustomerCouponService shopCustomerCouponService;

	@Mock
	private JusoAddressApiClient jusoAddressApiClient;

	@Mock
	private TossPaymentsClient tossPaymentsClient;

	@Spy
	private TossProperties tossProperties = new TossProperties("test-client-key", "test-secret-key");

	@Spy
	private ObjectMapper objectMapper = new ObjectMapper();

	@Mock
	private PlatformTransactionManager transactionManager;

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
		verify(goodsMapper, org.mockito.Mockito.times(3)).insertCategoryGoods(captor.capture());
		assertThat(result).isEqualTo(3);
		assertThat(captor.getAllValues())
			.extracting(CategoryGoodsSavePO::getCategoryId)
			.containsExactly("1000010001", "100001", "10");
	}

	@Test
	@DisplayName("상품상세 쿠폰 다운로드는 노출 가능한 상품쿠폰을 고객 쿠폰으로 발급한다")
	// 상품상세 기준 쿠폰 검증 후 ShopCustomerCouponService로 발급 책임을 위임하는지 확인합니다.
	void downloadShopGoodsCoupon_issuesCouponWhenApplicableToGoods() {
		// 조회 가능한 상품과 전체 대상 상품쿠폰을 목으로 구성합니다.
		ShopGoodsBasicVO goods = new ShopGoodsBasicVO();
		goods.setGoodsId("GOODS001");
		goods.setBrandNo(3);

		ShopGoodsCouponVO coupon = new ShopGoodsCouponVO();
		coupon.setCpnNo(51L);
		coupon.setCpnGbCd(CPN_GB_GOODS);
		coupon.setCpnTargetCd(CPN_TARGET_ALL);

		when(goodsMapper.getShopGoodsBasic("GOODS001")).thenReturn(goods);
		when(goodsMapper.getShopActiveGoodsCouponList()).thenReturn(List.of(coupon));
		when(shopCustomerCouponService.issueShopCustomerCoupon(7L, 51L, 1)).thenReturn(1);

		// 상품 쿠폰 다운로드를 수행합니다.
		goodsService.downloadShopGoodsCoupon(" GOODS001 ", 51L, 7L);

		// 실제 고객 쿠폰 발급 서비스 호출 여부를 검증합니다.
		verify(shopCustomerCouponService).issueShopCustomerCoupon(7L, 51L, 1);
	}

	@Test
	@DisplayName("상품상세 쿠폰 다운로드는 조회 불가 상품이면 예외를 반환한다")
	// 상품 기본 정보가 없으면 쿠폰 발급을 시도하지 않는지 확인합니다.
	void downloadShopGoodsCoupon_throwsWhenGoodsMissing() {
		// 상품 미조회 응답을 목으로 구성합니다.
		when(goodsMapper.getShopGoodsBasic("UNKNOWN")).thenReturn(null);

		// 조회 불가 상품이면 명확한 예외 메시지를 반환합니다.
		assertThatThrownBy(() -> goodsService.downloadShopGoodsCoupon("UNKNOWN", 81L, 7L))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("상품 정보를 찾을 수 없습니다.");
	}

	// 테스트용 카테고리 계층을 구성합니다.
	private void mockCategoryHierarchy() {
		// 리프 -> 중분류 -> 대분류 순으로 상위 카테고리를 조회할 수 있게 설정합니다.
		CategoryVO leaf = createCategory("1000010001", "100001", 3);
		CategoryVO parent = createCategory("100001", "10", 2);
		CategoryVO root = createCategory("10", "0", 1);
		when(goodsMapper.getAdminCategoryDetail("1000010001")).thenReturn(leaf);
		when(goodsMapper.getAdminCategoryDetail("100001")).thenReturn(parent);
		when(goodsMapper.getAdminCategoryDetail("10")).thenReturn(root);
	}

	// 테스트용 카테고리 VO를 생성합니다.
	private CategoryVO createCategory(String categoryId, String parentCategoryId, Integer categoryLevel) {
		// 계층 조회에 필요한 최소 필드만 설정합니다.
		CategoryVO category = new CategoryVO();
		category.setCategoryId(categoryId);
		category.setParentCategoryId(parentCategoryId);
		category.setCategoryLevel(categoryLevel);
		category.setCategoryNm("카테고리 " + categoryId);
		return category;
	}
}
