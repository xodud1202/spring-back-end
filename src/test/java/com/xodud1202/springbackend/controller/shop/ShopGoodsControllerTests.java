package com.xodud1202.springbackend.controller.shop;

import com.xodud1202.springbackend.domain.shop.cart.ShopCartPageVO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartCouponEstimateVO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartSiteInfoVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsBasicVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsDetailVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageCouponPageVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageDownloadableCouponVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderCancelPageVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderCancelReasonVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderAmountSummaryVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderDetailItemVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderDetailPageVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderGroupVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderPageVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderStatusSummaryVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOwnedCouponVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageWishGoodsItemVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageWishPageVO;
import com.xodud1202.springbackend.service.GoodsService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

@ExtendWith(MockitoExtension.class)
// 쇼핑몰 상품상세 컨트롤러 API 응답을 검증합니다.
class ShopGoodsControllerTests {
	@Mock
	private GoodsService goodsService;

	@InjectMocks
	private ShopGoodsController shopGoodsController;

	private MockMvc mockMvc;

	@BeforeEach
	// 컨트롤러 단위 검증용 MockMvc를 초기화합니다.
	void setUp() {
		// 단일 컨트롤러 기준 standalone MockMvc를 생성합니다.
		mockMvc = MockMvcBuilders.standaloneSetup(shopGoodsController).build();
	}

	@Test
	@DisplayName("상품상세 API는 정상 조회 시 200과 상품 기본 정보를 반환한다")
	// 상품상세 API 정상 응답 구조를 검증합니다.
	void getShopGoodsDetail_returnsOk() throws Exception {
		// 서비스 반환용 상품상세 응답 객체를 구성합니다.
		ShopGoodsBasicVO goods = new ShopGoodsBasicVO();
		goods.setGoodsId("CAMEUEP02MG");
		goods.setGoodsNm("테스트 상품");

		ShopGoodsDetailVO detail = new ShopGoodsDetailVO();
		detail.setGoods(goods);
		when(goodsService.getShopGoodsDetail(eq("CAMEUEP02MG"), isNull(), isNull())).thenReturn(detail);

		// 상품상세 API 요청 후 200 응답과 상품 필드를 검증합니다.
		mockMvc.perform(
				get("/api/shop/goods/detail")
					.param("goodsId", "CAMEUEP02MG")
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.goods.goodsId").value("CAMEUEP02MG"))
			.andExpect(jsonPath("$.goods.goodsNm").value("테스트 상품"));
	}

	@Test
	@DisplayName("상품상세 API는 상품코드가 없으면 400과 에러 메시지를 반환한다")
	// 필수 파라미터 누락 시 400 응답으로 변환되는지 검증합니다.
	void getShopGoodsDetail_returnsBadRequestWhenGoodsIdMissing() throws Exception {
		// goodsId 없이 요청했을 때 400 응답과 메시지를 검증합니다.
		mockMvc.perform(get("/api/shop/goods/detail").contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("상품코드를 확인해주세요."));
	}

	@Test
	@DisplayName("상품상세 API는 상품이 없으면 404와 에러 메시지를 반환한다")
	// 서비스 조회 결과가 없을 때 404 응답으로 변환되는지 검증합니다.
	void getShopGoodsDetail_returnsNotFoundWhenGoodsMissing() throws Exception {
		// 서비스 응답을 null로 고정합니다.
		when(goodsService.getShopGoodsDetail(eq("UNKNOWN"), isNull(), isNull())).thenReturn(null);

		// 상품상세 API 요청 후 404 응답과 메시지를 검증합니다.
		mockMvc.perform(
				get("/api/shop/goods/detail")
					.param("goodsId", "UNKNOWN")
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("상품 정보를 찾을 수 없습니다."));
	}

	@Test
	@DisplayName("상품상세 API는 예외 발생 시 500과 에러 메시지를 반환한다")
	// 서비스 예외 발생 시 500 응답으로 변환되는지 검증합니다.
	void getShopGoodsDetail_returnsInternalServerErrorWhenExceptionOccurs() throws Exception {
		// 서비스 예외를 발생하도록 목 동작을 설정합니다.
		when(goodsService.getShopGoodsDetail(eq("CAMEUEP02MG"), isNull(), isNull())).thenThrow(new IllegalStateException("boom"));

		// 상품상세 API 요청 후 500 응답과 메시지를 검증합니다.
		mockMvc.perform(
				get("/api/shop/goods/detail")
					.param("goodsId", "CAMEUEP02MG")
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.message").value("상품상세 조회에 실패했습니다."));
	}

	@Test
	@DisplayName("위시리스트 토글 API는 로그인 사용자가 요청하면 wished 상태를 반환한다")
	// 위시리스트 토글 성공 시 200 응답과 wished 값을 반환하는지 검증합니다.
	void toggleShopGoodsWishlist_returnsOk() throws Exception {
		// 토글 결과를 wished=true로 반환하도록 설정합니다.
		when(goodsService.toggleShopGoodsWishlist("CAMEUEP02MG", 1L)).thenReturn(true);

		// 로그인 쿠키와 함께 요청하면 200 응답과 wished=true를 검증합니다.
		mockMvc.perform(
				post("/api/shop/goods/wishlist/toggle")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"goodsId":"CAMEUEP02MG"}
						""")
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.wished").value(true));
	}

	@Test
	@DisplayName("상품상세 쿠폰 다운로드 API는 로그인 사용자가 요청하면 다운로드 메시지를 반환한다")
	// 상품상세 쿠폰 다운로드 성공 시 200 응답과 메시지 반환 여부를 검증합니다.
	void downloadShopGoodsCoupon_returnsOk() throws Exception {
		// 로그인 쿠키와 함께 요청하면 200 응답과 메시지를 검증합니다.
		mockMvc.perform(
				post("/api/shop/goods/coupon/download")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"goodsId":"CAMEUEP02MG","cpnNo":21}
						""")
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("쿠폰을 다운로드했습니다."));
	}

	@Test
	@DisplayName("상품상세 쿠폰 다운로드 API는 비로그인 요청이면 401을 반환한다")
	// 비로그인 상태에서 상품상세 쿠폰 다운로드 요청 시 401 응답을 검증합니다.
	void downloadShopGoodsCoupon_returnsUnauthorizedWhenNotLoggedIn() throws Exception {
		// 로그인 쿠키 없이 요청하면 401 응답과 메시지를 검증합니다.
		mockMvc.perform(
				post("/api/shop/goods/coupon/download")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"goodsId":"CAMEUEP02MG","cpnNo":21}
						""")
			)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
	}

	@Test
	@DisplayName("상품상세 쿠폰 다운로드 API는 잘못된 상품/쿠폰 조합이면 400을 반환한다")
	// 서비스에서 상품상세 기준 쿠폰 검증 예외를 반환하면 400 응답으로 변환되는지 검증합니다.
	void downloadShopGoodsCoupon_returnsBadRequestWhenCouponInvalidForGoods() throws Exception {
		// 상품상세 기준 쿠폰 검증 예외를 발생하도록 목 동작을 설정합니다.
		org.mockito.Mockito.doThrow(new IllegalArgumentException("다운로드 가능한 상품쿠폰을 확인해주세요."))
			.when(goodsService)
			.downloadShopGoodsCoupon("CAMEUEP02MG", 21L, 1L);

		// 로그인 쿠키와 함께 요청하면 400 응답과 메시지를 검증합니다.
		mockMvc.perform(
				post("/api/shop/goods/coupon/download")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"goodsId":"CAMEUEP02MG","cpnNo":21}
						""")
			)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("다운로드 가능한 상품쿠폰을 확인해주세요."));
	}

	@Test
	@DisplayName("위시리스트 토글 API는 비로그인 요청이면 401을 반환한다")
	// 비로그인 상태에서 위시리스트 토글 요청 시 401 응답을 검증합니다.
	void toggleShopGoodsWishlist_returnsUnauthorizedWhenNotLoggedIn() throws Exception {
		// 로그인 쿠키 없이 요청하면 401 응답과 메시지를 검증합니다.
		mockMvc.perform(
				post("/api/shop/goods/wishlist/toggle")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"goodsId":"CAMEUEP02MG"}
						""")
			)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
	}

	@Test
	@DisplayName("위시리스트 토글 API는 상품코드가 없으면 400을 반환한다")
	// 필수 파라미터(goodsId) 누락 시 400 응답을 검증합니다.
	void toggleShopGoodsWishlist_returnsBadRequestWhenGoodsIdMissing() throws Exception {
		// goodsId 없이 요청하면 400 응답과 메시지를 검증합니다.
		mockMvc.perform(
				post("/api/shop/goods/wishlist/toggle")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}")
			)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("상품코드를 확인해주세요."));
	}

	@Test
	@DisplayName("위시리스트 토글 API는 조회 불가 상품이면 404를 반환한다")
	// 서비스에서 상품 미존재 예외를 반환하면 404 응답으로 변환되는지 검증합니다.
	void toggleShopGoodsWishlist_returnsNotFoundWhenGoodsMissing() throws Exception {
		// 상품 미존재 예외를 발생하도록 목 동작을 설정합니다.
		when(goodsService.toggleShopGoodsWishlist("UNKNOWN", 1L))
			.thenThrow(new IllegalArgumentException("상품 정보를 찾을 수 없습니다."));

		// 토글 요청 후 404 응답과 메시지를 검증합니다.
		mockMvc.perform(
				post("/api/shop/goods/wishlist/toggle")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"goodsId":"UNKNOWN"}
						""")
			)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("상품 정보를 찾을 수 없습니다."));
	}

	@Test
	@DisplayName("마이페이지 위시리스트 조회 API는 로그인 사용자가 요청하면 페이지 데이터를 반환한다")
	// 마이페이지 위시리스트 조회 성공 시 200 응답과 페이지 필드를 반환하는지 검증합니다.
	void getShopMypageWishPage_returnsOk() throws Exception {
		// 위시리스트 페이지 응답 객체를 구성합니다.
		ShopMypageWishGoodsItemVO wishItem = new ShopMypageWishGoodsItemVO();
		wishItem.setGoodsId("GOODS001");
		wishItem.setGoodsNm("테스트 위시상품");
		wishItem.setSaleAmt(19900);

		ShopMypageWishPageVO page = new ShopMypageWishPageVO();
		page.setGoodsList(List.of(wishItem));
		page.setGoodsCount(1);
		page.setPageNo(1);
		page.setPageSize(10);
		page.setTotalPageCount(1);
		when(goodsService.getShopMypageWishPage(1L, 1)).thenReturn(page);

		// 로그인 쿠키와 함께 요청하면 200 응답과 페이지 필드를 검증합니다.
		mockMvc.perform(
				get("/api/shop/mypage/wish/page")
					.cookie(new Cookie("cust_no", "1"))
					.param("pageNo", "1")
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.goodsCount").value(1))
			.andExpect(jsonPath("$.pageSize").value(10))
			.andExpect(jsonPath("$.goodsList[0].goodsId").value("GOODS001"));
	}

	@Test
	@DisplayName("마이페이지 위시리스트 조회 API는 비로그인 요청이면 401을 반환한다")
	// 비로그인 상태에서 마이페이지 위시리스트 조회 요청 시 401 응답을 검증합니다.
	void getShopMypageWishPage_returnsUnauthorizedWhenNotLoggedIn() throws Exception {
		// 로그인 쿠키 없이 요청하면 401 응답과 메시지를 검증합니다.
		mockMvc.perform(
				get("/api/shop/mypage/wish/page")
					.param("pageNo", "1")
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
	}

	@Test
	@DisplayName("마이페이지 위시리스트 삭제 API는 로그인 사용자가 요청하면 삭제 메시지를 반환한다")
	// 마이페이지 위시리스트 삭제 성공 시 200 응답과 메시지를 반환하는지 검증합니다.
	void deleteShopMypageWish_returnsOk() throws Exception {
		// 삭제 서비스 호출을 no-op으로 설정합니다.
		doNothing().when(goodsService).deleteShopMypageWishGoods("GOODS001", 1L);

		// 로그인 쿠키와 함께 요청하면 200 응답과 메시지를 검증합니다.
		mockMvc.perform(
				post("/api/shop/mypage/wish/delete")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"goodsId":"GOODS001"}
						""")
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("위시리스트에서 삭제했습니다."));
	}

	@Test
	@DisplayName("마이페이지 쿠폰함 조회 API는 로그인 사용자가 요청하면 쿠폰함 목록을 반환한다")
	// 쿠폰함 조회 성공 시 200 응답과 탭별 페이지 정보/쿠폰 목록 필드를 반환하는지 검증합니다.
	void getShopMypageCouponPage_returnsOk() throws Exception {
		// 쿠폰함 페이지 응답 객체를 구성합니다.
		ShopMypageOwnedCouponVO ownedCoupon = new ShopMypageOwnedCouponVO();
		ownedCoupon.setCustCpnNo(101L);
		ownedCoupon.setCpnNo(11L);
		ownedCoupon.setCpnNm("보유 쿠폰");

		ShopMypageDownloadableCouponVO downloadableCoupon = new ShopMypageDownloadableCouponVO();
		downloadableCoupon.setCpnNo(21L);
		downloadableCoupon.setCpnNm("다운로드 쿠폰");

		ShopMypageCouponPageVO page = new ShopMypageCouponPageVO();
		page.setOwnedCouponList(List.of(ownedCoupon));
		page.setOwnedCouponCount(11);
		page.setOwnedPageNo(2);
		page.setOwnedPageSize(10);
		page.setOwnedTotalPageCount(2);
		page.setDownloadableCouponList(List.of(downloadableCoupon));
		page.setDownloadableCouponCount(4);
		page.setDownloadablePageNo(1);
		page.setDownloadablePageSize(10);
		page.setDownloadableTotalPageCount(1);
		when(goodsService.getShopMypageCouponPage(1L, 2, 1)).thenReturn(page);

		// 로그인 쿠키와 함께 요청하면 200 응답과 쿠폰 목록 필드를 검증합니다.
		mockMvc.perform(
				get("/api/shop/mypage/coupon/page")
					.param("ownedPageNo", "2")
					.param("downloadablePageNo", "1")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.ownedCouponCount").value(11))
			.andExpect(jsonPath("$.ownedPageNo").value(2))
			.andExpect(jsonPath("$.ownedCouponList[0].custCpnNo").value(101))
			.andExpect(jsonPath("$.downloadableCouponCount").value(4))
			.andExpect(jsonPath("$.downloadableCouponList[0].cpnNo").value(21));
	}

	@Test
	@DisplayName("마이페이지 쿠폰함 조회 API는 비로그인 요청이면 401을 반환한다")
	// 비로그인 상태에서 쿠폰함 조회 요청 시 401 응답을 검증합니다.
	void getShopMypageCouponPage_returnsUnauthorizedWhenNotLoggedIn() throws Exception {
		// 로그인 쿠키 없이 요청하면 401 응답과 메시지를 검증합니다.
		mockMvc.perform(get("/api/shop/mypage/coupon/page").contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
	}

	@Test
	@DisplayName("마이페이지 주문내역 조회 API는 로그인 사용자가 요청하면 주문목록과 상태 요약을 반환한다")
	// 주문내역 조회 성공 시 200 응답과 주문번호/상태 요약 필드가 함께 반환되는지 검증합니다.
	void getShopMypageOrderPage_returnsOk() throws Exception {
		// 서비스 반환용 주문내역 페이지 응답 객체를 구성합니다.
		ShopMypageOrderDetailItemVO detailItem = new ShopMypageOrderDetailItemVO();
		detailItem.setOrdNo("ORD-0001");
		detailItem.setOrdDtlNo(1);
		detailItem.setOrdDtlStatCd("ORD_DTL_STAT_02");
		detailItem.setOrdDtlStatNm("결제 완료");
		detailItem.setGoodsId("GOODS001");
		detailItem.setGoodsNm("테스트 상품");

		ShopMypageOrderGroupVO orderGroup = new ShopMypageOrderGroupVO();
		orderGroup.setOrdNo("ORD-0001");
		orderGroup.setOrderDt("2026-03-20 11:40:31");
		orderGroup.setDetailList(List.of(detailItem));

		ShopMypageOrderStatusSummaryVO statusSummary = new ShopMypageOrderStatusSummaryVO();
		statusSummary.setWaitingForDepositCount(2);
		statusSummary.setPaymentCompletedCount(1);

		ShopMypageOrderPageVO page = new ShopMypageOrderPageVO();
		page.setOrderList(List.of(orderGroup));
		page.setOrderCount(1);
		page.setPageNo(1);
		page.setPageSize(5);
		page.setTotalPageCount(1);
		page.setStartDate("2026-03-01");
		page.setEndDate("2026-03-20");
		page.setStatusSummary(statusSummary);
		when(goodsService.getShopMypageOrderPage(1L, 1, "2026-03-01", "2026-03-20")).thenReturn(page);

		// 로그인 쿠키와 함께 요청하면 200 응답과 주문내역 필드를 검증합니다.
		mockMvc.perform(
				get("/api/shop/mypage/order/page")
					.param("pageNo", "1")
					.param("startDate", "2026-03-01")
					.param("endDate", "2026-03-20")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.orderCount").value(1))
			.andExpect(jsonPath("$.pageSize").value(5))
			.andExpect(jsonPath("$.orderList[0].ordNo").value("ORD-0001"))
			.andExpect(jsonPath("$.orderList[0].detailList[0].goodsNm").value("테스트 상품"))
			.andExpect(jsonPath("$.statusSummary.waitingForDepositCount").value(2))
			.andExpect(jsonPath("$.statusSummary.paymentCompletedCount").value(1));
	}

	@Test
	@DisplayName("마이페이지 주문내역 조회 API는 비로그인 요청이면 401을 반환한다")
	// 비로그인 상태에서 주문내역 조회 요청 시 401 응답을 검증합니다.
	void getShopMypageOrderPage_returnsUnauthorizedWhenNotLoggedIn() throws Exception {
		// 로그인 쿠키 없이 요청하면 401 응답과 메시지를 검증합니다.
		mockMvc.perform(get("/api/shop/mypage/order/page").contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
	}

	@Test
	@DisplayName("마이페이지 주문내역 조회 API는 조회 기간 예외가 발생하면 400을 반환한다")
	// 기간 검증 예외 발생 시 400 응답과 에러 메시지를 반환하는지 검증합니다.
	void getShopMypageOrderPage_returnsBadRequestWhenDateRangeInvalid() throws Exception {
		// 서비스에서 기간 검증 예외를 반환하도록 설정합니다.
		when(goodsService.getShopMypageOrderPage(1L, 1, "2026-03-21", "2026-03-20"))
			.thenThrow(new IllegalArgumentException("조회 기간을 확인해주세요."));

		// 로그인 쿠키와 함께 잘못된 기간으로 요청하면 400 응답과 메시지를 검증합니다.
		mockMvc.perform(
				get("/api/shop/mypage/order/page")
					.param("pageNo", "1")
					.param("startDate", "2026-03-21")
					.param("endDate", "2026-03-20")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("조회 기간을 확인해주세요."));
	}

	@Test
	@DisplayName("마이페이지 주문상세 조회 API는 로그인 사용자가 요청하면 주문상품과 금액 요약을 반환한다")
	// 주문상세 조회 성공 시 200 응답과 주문번호/금액 요약 필드가 함께 반환되는지 검증합니다.
	void getShopMypageOrderDetailPage_returnsOk() throws Exception {
		// 서비스 반환용 주문상세 페이지 응답 객체를 구성합니다.
		ShopMypageOrderDetailItemVO detailItem = new ShopMypageOrderDetailItemVO();
		detailItem.setOrdNo("ORD-0001");
		detailItem.setOrdDtlNo(1);
		detailItem.setOrdDtlStatCd("ORD_DTL_STAT_06");
		detailItem.setOrdDtlStatNm("배송완료");
		detailItem.setGoodsId("GOODS001");
		detailItem.setGoodsNm("테스트 상품");

		ShopMypageOrderGroupVO orderGroup = new ShopMypageOrderGroupVO();
		orderGroup.setOrdNo("ORD-0001");
		orderGroup.setOrderDt("2026-03-20 11:40:31");
		orderGroup.setDetailList(List.of(detailItem));

		ShopMypageOrderAmountSummaryVO amountSummary = new ShopMypageOrderAmountSummaryVO();
		amountSummary.setTotalSupplyAmt(50000L);
		amountSummary.setTotalOrderAmt(42000L);
		amountSummary.setTotalGoodsDiscountAmt(8000L);
		amountSummary.setTotalGoodsCouponDiscountAmt(1000L);
		amountSummary.setTotalCartCouponDiscountAmt(2000L);
		amountSummary.setTotalCouponDiscountAmt(3000L);
		amountSummary.setTotalPointUseAmt(2000L);
		amountSummary.setDeliveryFeeAmt(3000L);
		amountSummary.setDeliveryCouponDiscountAmt(1500L);
		amountSummary.setFinalPayAmt(40000L);

		ShopMypageOrderDetailPageVO page = new ShopMypageOrderDetailPageVO();
		page.setOrder(orderGroup);
		page.setAmountSummary(amountSummary);
		when(goodsService.getShopMypageOrderDetailPage(1L, "ORD-0001")).thenReturn(page);

		// 로그인 쿠키와 함께 요청하면 200 응답과 주문상세 필드를 검증합니다.
		mockMvc.perform(
				get("/api/shop/mypage/order/detail")
					.param("ordNo", "ORD-0001")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.order.ordNo").value("ORD-0001"))
			.andExpect(jsonPath("$.order.detailList[0].goodsNm").value("테스트 상품"))
			.andExpect(jsonPath("$.amountSummary.totalSupplyAmt").value(50000))
			.andExpect(jsonPath("$.amountSummary.totalGoodsDiscountAmt").value(8000))
			.andExpect(jsonPath("$.amountSummary.totalGoodsCouponDiscountAmt").value(1000))
			.andExpect(jsonPath("$.amountSummary.totalCartCouponDiscountAmt").value(2000))
			.andExpect(jsonPath("$.amountSummary.deliveryCouponDiscountAmt").value(1500))
			.andExpect(jsonPath("$.amountSummary.finalPayAmt").value(40000));
	}

	@Test
	@DisplayName("마이페이지 주문상세 조회 API는 비로그인 요청이면 401을 반환한다")
	// 비로그인 상태에서 주문상세 조회 요청 시 401 응답을 검증합니다.
	void getShopMypageOrderDetailPage_returnsUnauthorizedWhenNotLoggedIn() throws Exception {
		// 로그인 쿠키 없이 요청하면 401 응답과 메시지를 검증합니다.
		mockMvc.perform(get("/api/shop/mypage/order/detail").param("ordNo", "ORD-0001").contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
	}

	@Test
	@DisplayName("마이페이지 주문상세 조회 API는 주문번호가 비어 있으면 400을 반환한다")
	// 필수 주문번호 누락 시 400 응답과 에러 메시지를 반환하는지 검증합니다.
	void getShopMypageOrderDetailPage_returnsBadRequestWhenOrdNoMissing() throws Exception {
		// 공백 주문번호로 요청하면 400 응답과 메시지를 검증합니다.
		mockMvc.perform(
				get("/api/shop/mypage/order/detail")
					.param("ordNo", " ")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("주문번호를 확인해주세요."));
	}

	@Test
	@DisplayName("마이페이지 주문상세 조회 API는 주문이 없으면 404를 반환한다")
	// 주문 미존재 예외 발생 시 404 응답과 에러 메시지를 반환하는지 검증합니다.
	void getShopMypageOrderDetailPage_returnsNotFoundWhenOrderMissing() throws Exception {
		// 서비스에서 주문 미존재 예외를 반환하도록 설정합니다.
		when(goodsService.getShopMypageOrderDetailPage(1L, "ORD-NOT-FOUND"))
			.thenThrow(new IllegalArgumentException("주문 정보를 찾을 수 없습니다."));

		// 로그인 쿠키와 함께 없는 주문번호로 요청하면 404 응답과 메시지를 검증합니다.
		mockMvc.perform(
				get("/api/shop/mypage/order/detail")
					.param("ordNo", "ORD-NOT-FOUND")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("주문 정보를 찾을 수 없습니다."));
	}

	@Test
	@DisplayName("마이페이지 주문취소 신청 화면 조회 API는 로그인 사용자가 요청하면 주문상품, 사유 코드, 배송비 기준 정보를 반환한다")
	// 주문취소 신청 화면 조회 성공 시 200 응답과 주문상품/취소사유/배송비 기준 필드가 함께 반환되는지 검증합니다.
	void getShopMypageOrderCancelPage_returnsOk() throws Exception {
		// 서비스 반환용 주문취소 신청 화면 응답 객체를 구성합니다.
		ShopMypageOrderDetailItemVO detailItem = new ShopMypageOrderDetailItemVO();
		detailItem.setOrdNo("ORD-0001");
		detailItem.setOrdDtlNo(1);
		detailItem.setOrdDtlStatCd("ORD_DTL_STAT_02");
		detailItem.setOrdDtlStatNm("결제 완료");
		detailItem.setGoodsId("GOODS001");
		detailItem.setGoodsNm("테스트 상품");
		detailItem.setCancelableQty(2);
		detailItem.setGoodsCouponDiscountAmt(1000);
		detailItem.setCartCouponDiscountAmt(2000);
		detailItem.setPointUseAmt(500);

		ShopMypageOrderGroupVO orderGroup = new ShopMypageOrderGroupVO();
		orderGroup.setOrdNo("ORD-0001");
		orderGroup.setOrderDt("2026-03-20 11:40:31");
		orderGroup.setDetailList(List.of(detailItem));

		ShopMypageOrderAmountSummaryVO amountSummary = new ShopMypageOrderAmountSummaryVO();
		amountSummary.setTotalSupplyAmt(50000L);
		amountSummary.setTotalOrderAmt(42000L);
		amountSummary.setDeliveryFeeAmt(3000L);
		amountSummary.setFinalPayAmt(40000L);

		ShopMypageOrderCancelReasonVO reason = new ShopMypageOrderCancelReasonVO();
		reason.setCd("C_01");
		reason.setCdNm("단순 변심");

		ShopCartSiteInfoVO siteInfo = new ShopCartSiteInfoVO();
		siteInfo.setSiteId("xodud1202");
		siteInfo.setDeliveryFee(3000);
		siteInfo.setDeliveryFeeLimit(50000);

		ShopMypageOrderCancelPageVO page = new ShopMypageOrderCancelPageVO();
		page.setOrder(orderGroup);
		page.setAmountSummary(amountSummary);
		page.setReasonList(List.of(reason));
		page.setSiteInfo(siteInfo);
		when(goodsService.getShopMypageOrderCancelPage(1L, "ORD-0001", 1)).thenReturn(page);

		// 로그인 쿠키와 함께 요청하면 200 응답과 주문취소 신청 화면 필드를 검증합니다.
		mockMvc.perform(
				get("/api/shop/mypage/order/cancel/page")
					.param("ordNo", "ORD-0001")
					.param("ordDtlNo", "1")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.order.ordNo").value("ORD-0001"))
			.andExpect(jsonPath("$.order.detailList[0].cancelableQty").value(2))
			.andExpect(jsonPath("$.order.detailList[0].goodsCouponDiscountAmt").value(1000))
			.andExpect(jsonPath("$.reasonList[0].cd").value("C_01"))
			.andExpect(jsonPath("$.siteInfo.deliveryFee").value(3000))
			.andExpect(jsonPath("$.siteInfo.deliveryFeeLimit").value(50000));
	}

	@Test
	@DisplayName("마이페이지 주문취소 신청 화면 조회 API는 비로그인 요청이면 401을 반환한다")
	// 비로그인 상태에서 주문취소 신청 화면 조회 요청 시 401 응답을 검증합니다.
	void getShopMypageOrderCancelPage_returnsUnauthorizedWhenNotLoggedIn() throws Exception {
		// 로그인 쿠키 없이 요청하면 401 응답과 메시지를 검증합니다.
		mockMvc.perform(
				get("/api/shop/mypage/order/cancel/page")
					.param("ordNo", "ORD-0001")
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
	}

	@Test
	@DisplayName("마이페이지 주문취소 신청 화면 조회 API는 주문번호가 비어 있으면 400을 반환한다")
	// 필수 주문번호 누락 시 400 응답과 에러 메시지를 반환하는지 검증합니다.
	void getShopMypageOrderCancelPage_returnsBadRequestWhenOrdNoMissing() throws Exception {
		// 공백 주문번호로 요청하면 400 응답과 메시지를 검증합니다.
		mockMvc.perform(
				get("/api/shop/mypage/order/cancel/page")
					.param("ordNo", " ")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("주문번호를 확인해주세요."));
	}

	@Test
	@DisplayName("마이페이지 주문취소 신청 화면 조회 API는 주문상세번호가 잘못되면 400을 반환한다")
	// 주문취소 화면 진입용 주문상세번호가 유효하지 않을 때 400 응답과 메시지를 반환하는지 검증합니다.
	void getShopMypageOrderCancelPage_returnsBadRequestWhenOrdDtlNoInvalid() throws Exception {
		// 서비스에서 주문상세번호 검증 예외를 반환하도록 설정합니다.
		when(goodsService.getShopMypageOrderCancelPage(1L, "ORD-0001", 999))
			.thenThrow(new IllegalArgumentException("주문상품 정보를 확인해주세요."));

		// 로그인 쿠키와 함께 잘못된 주문상세번호로 요청하면 400 응답과 메시지를 검증합니다.
		mockMvc.perform(
				get("/api/shop/mypage/order/cancel/page")
					.param("ordNo", "ORD-0001")
					.param("ordDtlNo", "999")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("주문상품 정보를 확인해주세요."));
	}

	@Test
	@DisplayName("마이페이지 주문취소 신청 화면 조회 API는 주문이 없으면 404를 반환한다")
	// 주문 미존재 예외 발생 시 404 응답과 에러 메시지를 반환하는지 검증합니다.
	void getShopMypageOrderCancelPage_returnsNotFoundWhenOrderMissing() throws Exception {
		// 서비스에서 주문 미존재 예외를 반환하도록 설정합니다.
		when(goodsService.getShopMypageOrderCancelPage(1L, "ORD-NOT-FOUND", null))
			.thenThrow(new IllegalArgumentException("주문 정보를 찾을 수 없습니다."));

		// 로그인 쿠키와 함께 없는 주문번호로 요청하면 404 응답과 메시지를 검증합니다.
		mockMvc.perform(
				get("/api/shop/mypage/order/cancel/page")
					.param("ordNo", "ORD-NOT-FOUND")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("주문 정보를 찾을 수 없습니다."));
	}

	@Test
	@DisplayName("마이페이지 쿠폰 다운로드 API는 로그인 사용자가 요청하면 다운로드 메시지를 반환한다")
	// 개별 쿠폰 다운로드 성공 시 200 응답과 메시지를 반환하는지 검증합니다.
	void downloadShopMypageCoupon_returnsOk() throws Exception {
		// 로그인 쿠키와 함께 요청하면 200 응답과 메시지를 검증합니다.
		mockMvc.perform(
				post("/api/shop/mypage/coupon/download")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"cpnNo":21}
						""")
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("쿠폰을 다운로드했습니다."));
	}

	@Test
	@DisplayName("마이페이지 쿠폰 다운로드 API는 비로그인 요청이면 401을 반환한다")
	// 비로그인 상태에서 개별 쿠폰 다운로드 요청 시 401 응답을 검증합니다.
	void downloadShopMypageCoupon_returnsUnauthorizedWhenNotLoggedIn() throws Exception {
		// 로그인 쿠키 없이 요청하면 401 응답과 메시지를 검증합니다.
		mockMvc.perform(
				post("/api/shop/mypage/coupon/download")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"cpnNo":21}
						""")
			)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
	}

	@Test
	@DisplayName("마이페이지 전체 쿠폰 다운로드 API는 다운로드 건수를 반환한다")
	// 전체 다운로드 성공 시 200 응답과 다운로드 건수 반환 여부를 검증합니다.
	void downloadAllShopMypageCoupon_returnsOk() throws Exception {
		// 전체 다운로드 건수를 3건으로 반환하도록 설정합니다.
		when(goodsService.downloadAllShopMypageCoupon(1L)).thenReturn(3);

		// 로그인 쿠키와 함께 요청하면 200 응답과 다운로드 결과를 검증합니다.
		mockMvc.perform(
				post("/api/shop/mypage/coupon/download/all")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.downloadedCount").value(3))
			.andExpect(jsonPath("$.message").value("전체 쿠폰을 다운로드했습니다."));
	}

	@Test
	@DisplayName("장바구니 등록 API는 로그인 사용자가 요청하면 최종 수량을 반환한다")
	// 장바구니 등록 성공 시 200 응답과 최종 수량 반환 여부를 검증합니다.
	void addShopGoodsCart_returnsOk() throws Exception {
		// 장바구니 최종 수량을 5로 반환하도록 설정합니다.
		when(goodsService.addShopGoodsCart("CAMEUEP02MG", "095", 2, 1L, 2)).thenReturn(5);

		// 로그인 쿠키와 함께 요청하면 200 응답과 수량/메시지를 검증합니다.
		mockMvc.perform(
				post("/api/shop/goods/cart/add")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"goodsId":"CAMEUEP02MG","sizeId":"095","qty":2,"exhibitionNo":2}
						""")
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.qty").value(5))
			.andExpect(jsonPath("$.message").value("장바구니에 담았습니다."));
	}

	@Test
	@DisplayName("바로구매 장바구니 등록 API는 로그인 사용자가 요청하면 cartId와 goodsId를 반환한다")
	// 바로구매 장바구니 등록 성공 시 200 응답과 생성된 장바구니 번호 반환 여부를 검증합니다.
	void addShopGoodsOrderNow_returnsOk() throws Exception {
		// 바로구매 장바구니 번호를 21번으로 반환하도록 설정합니다.
		when(goodsService.addShopGoodsOrderNowCart("CAMEUEP02MG", "095", 1, 1L, 2)).thenReturn(21L);

		// 로그인 쿠키와 함께 요청하면 200 응답과 cartId/goodsId를 검증합니다.
		mockMvc.perform(
				post("/api/shop/goods/order-now")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"goodsId":"CAMEUEP02MG","sizeId":"095","qty":1,"exhibitionNo":2}
						""")
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.cartId").value(21))
			.andExpect(jsonPath("$.goodsId").value("CAMEUEP02MG"))
			.andExpect(jsonPath("$.message").value("바로구매 정보를 등록했습니다."));
	}

	@Test
	@DisplayName("장바구니 등록 API는 잘못된 기획전 번호 형식이면 null로 정규화해 처리한다")
	// exhibitionNo 형식이 잘못되어도 요청 실패 없이 일반 경로(null)로 서비스에 전달되는지 검증합니다.
	void addShopGoodsCart_normalizesInvalidExhibitionNoToNull() throws Exception {
		// 잘못된 기획전 번호는 null로 전달되도록 장바구니 수량을 반환하도록 설정합니다.
		when(goodsService.addShopGoodsCart("CAMEUEP02MG", "095", 2, 1L, null)).thenReturn(5);

		// 문자열 기획전 번호로 요청해도 200 응답과 수량/메시지를 검증합니다.
		mockMvc.perform(
				post("/api/shop/goods/cart/add")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"goodsId":"CAMEUEP02MG","sizeId":"095","qty":2,"exhibitionNo":"abc"}
						""")
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.qty").value(5))
			.andExpect(jsonPath("$.message").value("장바구니에 담았습니다."));
	}

	@Test
	@DisplayName("바로구매 장바구니 등록 API는 비로그인 요청이면 401을 반환한다")
	// 비로그인 상태에서 바로구매 장바구니 등록 요청 시 401 응답을 검증합니다.
	void addShopGoodsOrderNow_returnsUnauthorizedWhenNotLoggedIn() throws Exception {
		// 로그인 쿠키 없이 요청하면 401 응답과 메시지를 검증합니다.
		mockMvc.perform(
				post("/api/shop/goods/order-now")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"goodsId":"CAMEUEP02MG","sizeId":"095","qty":1}
						""")
			)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
	}

	@Test
	@DisplayName("장바구니 등록 API는 비로그인 요청이면 401을 반환한다")
	// 비로그인 상태에서 장바구니 등록 요청 시 401 응답을 검증합니다.
	void addShopGoodsCart_returnsUnauthorizedWhenNotLoggedIn() throws Exception {
		// 로그인 쿠키 없이 요청하면 401 응답과 메시지를 검증합니다.
		mockMvc.perform(
				post("/api/shop/goods/cart/add")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"goodsId":"CAMEUEP02MG","sizeId":"095","qty":1}
						""")
			)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
	}

	@Test
	@DisplayName("장바구니 등록 API는 사이즈가 없으면 400을 반환한다")
	// 필수 파라미터(sizeId) 누락 시 400 응답을 검증합니다.
	void addShopGoodsCart_returnsBadRequestWhenSizeMissing() throws Exception {
		// sizeId 없이 요청하면 400 응답과 메시지를 검증합니다.
		mockMvc.perform(
				post("/api/shop/goods/cart/add")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"goodsId":"CAMEUEP02MG","qty":1}
						""")
			)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("사이즈를 선택해주세요."));
	}

	@Test
	@DisplayName("장바구니 등록 API는 조회 불가 상품이면 404를 반환한다")
	// 서비스에서 상품 미존재 예외를 반환하면 404 응답으로 변환되는지 검증합니다.
	void addShopGoodsCart_returnsNotFoundWhenGoodsMissing() throws Exception {
		// 상품 미존재 예외를 발생하도록 목 동작을 설정합니다.
		when(goodsService.addShopGoodsCart("UNKNOWN", "095", 1, 1L, null))
			.thenThrow(new IllegalArgumentException("상품 정보를 찾을 수 없습니다."));

		// 장바구니 등록 요청 후 404 응답과 메시지를 검증합니다.
		mockMvc.perform(
				post("/api/shop/goods/cart/add")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"goodsId":"UNKNOWN","sizeId":"095","qty":1}
						""")
			)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("상품 정보를 찾을 수 없습니다."));
	}

	@Test
	@DisplayName("장바구니 페이지 조회 API는 로그인 사용자가 요청하면 장바구니 데이터를 반환한다")
	// 장바구니 페이지 조회 성공 시 200 응답과 장바구니 필드를 반환하는지 검증합니다.
	void getShopCartPage_returnsOk() throws Exception {
		// 장바구니 페이지 응답 객체를 구성합니다.
		ShopCartSiteInfoVO siteInfo = new ShopCartSiteInfoVO();
		siteInfo.setSiteId("xodud1202");
		siteInfo.setDeliveryFee(3000);
		siteInfo.setDeliveryFeeLimit(30000);

		ShopCartPageVO page = new ShopCartPageVO();
		page.setCartList(List.of());
		page.setCartCount(0);
		page.setSiteInfo(siteInfo);
		when(goodsService.getShopCartPage(1L)).thenReturn(page);

		// 로그인 쿠키와 함께 요청하면 200 응답과 장바구니 필드를 검증합니다.
		mockMvc.perform(
				get("/api/shop/cart/page")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.cartCount").value(0))
			.andExpect(jsonPath("$.siteInfo.deliveryFee").value(3000))
			.andExpect(jsonPath("$.siteInfo.deliveryFeeLimit").value(30000));
	}

	@Test
	@DisplayName("장바구니 페이지 조회 API는 비로그인 요청이면 401을 반환한다")
	// 비로그인 상태에서 장바구니 페이지 조회 요청 시 401 응답을 검증합니다.
	void getShopCartPage_returnsUnauthorizedWhenNotLoggedIn() throws Exception {
		// 로그인 쿠키 없이 요청하면 401 응답과 메시지를 검증합니다.
		mockMvc.perform(get("/api/shop/cart/page").contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
	}

	@Test
	@DisplayName("장바구니 쿠폰 예상 할인 API는 로그인 사용자가 요청하면 할인 금액 필드를 반환한다")
	// 예상 할인 계산 성공 시 200 응답과 할인 금액 필드 반환 여부를 검증합니다.
	void estimateShopCartCoupon_returnsOk() throws Exception {
		// 예상 할인 응답 객체를 구성합니다.
		ShopCartCouponEstimateVO result = new ShopCartCouponEstimateVO();
		result.setExpectedMaxDiscountAmt(19000);
		result.setGoodsCouponDiscountAmt(9000);
		result.setCartCouponDiscountAmt(7000);
		result.setDeliveryCouponDiscountAmt(3000);
		when(goodsService.getShopCartCouponEstimate(any(), eq(1L))).thenReturn(result);

		// 로그인 쿠키와 함께 요청하면 200 응답과 할인 금액 필드를 검증합니다.
		mockMvc.perform(
				post("/api/shop/cart/coupon/estimate")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"cartItemList":[{"goodsId":"GOODS001","sizeId":"095"}]}
						""")
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.expectedMaxDiscountAmt").value(19000))
			.andExpect(jsonPath("$.goodsCouponDiscountAmt").value(9000))
			.andExpect(jsonPath("$.cartCouponDiscountAmt").value(7000))
			.andExpect(jsonPath("$.deliveryCouponDiscountAmt").value(3000));
	}

	@Test
	@DisplayName("장바구니 쿠폰 예상 할인 API는 비로그인 요청이면 401을 반환한다")
	// 비로그인 상태에서 예상 할인 계산 요청 시 401 응답을 검증합니다.
	void estimateShopCartCoupon_returnsUnauthorizedWhenNotLoggedIn() throws Exception {
		// 로그인 쿠키 없이 요청하면 401 응답과 메시지를 검증합니다.
		mockMvc.perform(
				post("/api/shop/cart/coupon/estimate")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"cartItemList":[{"goodsId":"GOODS001","sizeId":"095"}]}
						""")
			)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
	}

	@Test
	@DisplayName("장바구니 쿠폰 예상 할인 API는 빈 선택 요청이면 0원 결과를 반환한다")
	// 빈 선택 요청 시 200 응답과 0원 할인 결과를 반환하는지 검증합니다.
	void estimateShopCartCoupon_returnsZeroWhenSelectionMissing() throws Exception {
		// 빈 선택 결과 응답 객체를 구성합니다.
		ShopCartCouponEstimateVO result = new ShopCartCouponEstimateVO();
		result.setExpectedMaxDiscountAmt(0);
		result.setGoodsCouponDiscountAmt(0);
		result.setCartCouponDiscountAmt(0);
		result.setDeliveryCouponDiscountAmt(0);
		when(goodsService.getShopCartCouponEstimate(any(), eq(1L))).thenReturn(result);

		// 로그인 쿠키와 함께 빈 선택 요청 시 0원 필드를 검증합니다.
		mockMvc.perform(
				post("/api/shop/cart/coupon/estimate")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"cartItemList":[]}
						""")
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.expectedMaxDiscountAmt").value(0))
			.andExpect(jsonPath("$.goodsCouponDiscountAmt").value(0))
			.andExpect(jsonPath("$.cartCouponDiscountAmt").value(0))
			.andExpect(jsonPath("$.deliveryCouponDiscountAmt").value(0));
	}

	@Test
	@DisplayName("장바구니 옵션 변경 API는 로그인 사용자가 요청하면 변경 완료 메시지를 반환한다")
	// 장바구니 옵션 변경 성공 시 200 응답과 메시지 반환 여부를 검증합니다.
	void updateShopCartOption_returnsOk() throws Exception {
		// 로그인 쿠키와 함께 요청하면 200 응답과 메시지를 검증합니다.
		mockMvc.perform(
				post("/api/shop/cart/option/update")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"goodsId":"GOODS001","sizeId":"095","targetSizeId":"100","qty":2}
						""")
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("장바구니 옵션을 변경했습니다."));
	}

	@Test
	@DisplayName("장바구니 옵션 변경 API는 변경 대상이 없으면 404를 반환한다")
	// 서비스에서 장바구니 대상 미존재 예외를 반환하면 404 응답으로 변환되는지 검증합니다.
	void updateShopCartOption_returnsNotFoundWhenCartItemMissing() throws Exception {
		// 장바구니 대상 미존재 예외를 발생하도록 목 동작을 설정합니다.
		org.mockito.Mockito.doThrow(new IllegalArgumentException("변경할 장바구니 상품을 찾을 수 없습니다."))
			.when(goodsService)
			.updateShopCartOption(any(), eq(1L));

		// 옵션 변경 요청 후 404 응답과 메시지를 검증합니다.
		mockMvc.perform(
				post("/api/shop/cart/option/update")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"goodsId":"GOODS001","sizeId":"095","targetSizeId":"100","qty":2}
						""")
			)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("변경할 장바구니 상품을 찾을 수 없습니다."));
	}

	@Test
	@DisplayName("장바구니 선택 삭제 API는 로그인 사용자가 요청하면 삭제 건수를 반환한다")
	// 장바구니 선택 삭제 성공 시 200 응답과 삭제 건수 반환 여부를 검증합니다.
	void deleteShopCartItems_returnsOk() throws Exception {
		// 선택 삭제 건수를 2건으로 반환하도록 설정합니다.
		when(goodsService.deleteShopCartItems(any(), eq(1L))).thenReturn(2);

		// 로그인 쿠키와 함께 요청하면 200 응답과 삭제 결과를 검증합니다.
		mockMvc.perform(
				post("/api/shop/cart/delete")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"cartItemList":[{"goodsId":"GOODS001","sizeId":"095"},{"goodsId":"GOODS002","sizeId":"100"}]}
						""")
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.deletedCount").value(2))
			.andExpect(jsonPath("$.message").value("선택한 장바구니 상품을 삭제했습니다."));
	}

	@Test
	@DisplayName("장바구니 전체 삭제 API는 로그인 사용자가 요청하면 삭제 건수를 반환한다")
	// 장바구니 전체 삭제 성공 시 200 응답과 삭제 건수 반환 여부를 검증합니다.
	void deleteShopCartAll_returnsOk() throws Exception {
		// 전체 삭제 건수를 3건으로 반환하도록 설정합니다.
		when(goodsService.deleteShopCartAll(1L)).thenReturn(3);

		// 로그인 쿠키와 함께 요청하면 200 응답과 삭제 결과를 검증합니다.
		mockMvc.perform(
				post("/api/shop/cart/delete/all")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.deletedCount").value(3))
			.andExpect(jsonPath("$.message").value("장바구니 상품을 전체 삭제했습니다."));
	}
}
