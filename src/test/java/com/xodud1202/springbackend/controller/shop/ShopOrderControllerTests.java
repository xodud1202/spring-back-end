package com.xodud1202.springbackend.controller.shop;

import com.xodud1202.springbackend.domain.shop.cart.ShopCartSiteInfoVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderAddressSaveResultVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderAddressSearchCommonVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderAddressSearchItemVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderAddressSearchResponseVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderAddressVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderPageVO;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
// 쇼핑몰 주문서 컨트롤러 API 응답을 검증합니다.
class ShopOrderControllerTests {
	@Mock
	private GoodsService goodsService;

	@InjectMocks
	private ShopOrderController shopOrderController;

	private MockMvc mockMvc;

	@BeforeEach
	// 컨트롤러 단위 검증용 MockMvc를 초기화합니다.
	void setUp() {
		// 단일 컨트롤러 기준 standalone MockMvc를 생성합니다.
		mockMvc = MockMvcBuilders.standaloneSetup(shopOrderController).build();
	}

	@Test
	@DisplayName("주문서 페이지 조회 API는 로그인 사용자가 유효한 cartId를 전달하면 데이터를 반환한다")
	// 주문서 페이지 조회 성공 시 200 응답과 페이지 필드 반환 여부를 검증합니다.
	void getShopOrderPage_returnsOk() throws Exception {
		// 주문서 페이지 응답 객체를 구성합니다.
		ShopCartSiteInfoVO siteInfo = new ShopCartSiteInfoVO();
		siteInfo.setSiteId("xodud1202");
		siteInfo.setDeliveryFee(3000);
		siteInfo.setDeliveryFeeLimit(30000);

		ShopOrderAddressVO defaultAddress = new ShopOrderAddressVO();
		defaultAddress.setCustNo(1L);
		defaultAddress.setAddressNm("집");
		defaultAddress.setDefaultYn("Y");

		ShopOrderPageVO page = new ShopOrderPageVO();
		page.setCartList(List.of());
		page.setCartCount(0);
		page.setSiteInfo(siteInfo);
		page.setAddressList(List.of(defaultAddress));
		page.setDefaultAddress(defaultAddress);
		when(goodsService.getShopOrderPage(List.of(12L, 15L), 1L)).thenReturn(page);

		// 로그인 쿠키와 함께 요청하면 200 응답과 주문서 기본 필드를 검증합니다.
		mockMvc.perform(
				get("/api/shop/order/page")
					.cookie(new Cookie("cust_no", "1"))
					.param("cartId", "12")
					.param("cartId", "15")
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.cartCount").value(0))
			.andExpect(jsonPath("$.siteInfo.deliveryFee").value(3000))
			.andExpect(jsonPath("$.siteInfo.deliveryFeeLimit").value(30000))
			.andExpect(jsonPath("$.addressList[0].addressNm").value("집"))
			.andExpect(jsonPath("$.defaultAddress.addressNm").value("집"));
	}

	@Test
	@DisplayName("주문서 페이지 조회 API는 비로그인 요청이면 401을 반환한다")
	// 비로그인 상태에서 주문서 페이지 조회 요청 시 401 응답을 검증합니다.
	void getShopOrderPage_returnsUnauthorizedWhenNotLoggedIn() throws Exception {
		// 로그인 쿠키 없이 요청하면 401 응답과 메시지를 검증합니다.
		mockMvc.perform(
				get("/api/shop/order/page")
					.param("cartId", "12")
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
	}

	@Test
	@DisplayName("주문서 페이지 조회 API는 유효하지 않은 cartId가 포함되면 400을 반환한다")
	// 주문서 페이지 조회 시 cartId 검증 예외가 발생하면 400 응답으로 변환되는지 검증합니다.
	void getShopOrderPage_returnsBadRequestWhenCartIdInvalid() throws Exception {
		// 주문 정보 검증 예외를 발생하도록 목 동작을 설정합니다.
		when(goodsService.getShopOrderPage(List.of(12L, 19L), 1L))
			.thenThrow(new IllegalArgumentException("주문 정보가 맞지 않습니다."));

		// 로그인 쿠키와 함께 잘못된 cartId를 요청하면 400 응답과 메시지를 검증합니다.
		mockMvc.perform(
				get("/api/shop/order/page")
					.cookie(new Cookie("cust_no", "1"))
					.param("cartId", "12")
					.param("cartId", "19")
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("주문 정보가 맞지 않습니다."));
	}

	@Test
	@DisplayName("배송지 검색 API는 로그인 사용자가 요청하면 주소 검색 결과를 반환한다")
	// 배송지 검색 성공 시 200 응답과 공통 필드/주소 목록 반환 여부를 검증합니다.
	void searchShopOrderAddress_returnsOk() throws Exception {
		// 주소 검색 응답 객체를 구성합니다.
		ShopOrderAddressSearchCommonVO common = new ShopOrderAddressSearchCommonVO();
		common.setErrorCode("0");
		common.setErrorMessage("정상");
		common.setTotalCount(1);
		common.setCurrentPage(1);
		common.setCountPerPage(10);

		ShopOrderAddressSearchItemVO jusoItem = new ShopOrderAddressSearchItemVO();
		jusoItem.setRoadAddr("서울특별시 강남구 테헤란로 1");
		jusoItem.setZipNo("06234");

		ShopOrderAddressSearchResponseVO result = new ShopOrderAddressSearchResponseVO();
		result.setCommon(common);
		result.setJusoList(List.of(jusoItem));
		when(goodsService.searchShopOrderAddress("테헤란로", 1, 10, 1L)).thenReturn(result);

		// 로그인 쿠키와 함께 요청하면 200 응답과 검색 결과 필드를 검증합니다.
		mockMvc.perform(
				get("/api/shop/order/address/search")
					.cookie(new Cookie("cust_no", "1"))
					.param("keyword", "테헤란로")
					.param("currentPage", "1")
					.param("countPerPage", "10")
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.common.errorCode").value("0"))
			.andExpect(jsonPath("$.common.totalCount").value(1))
			.andExpect(jsonPath("$.jusoList[0].roadAddr").value("서울특별시 강남구 테헤란로 1"));
	}

	@Test
	@DisplayName("배송지 검색 API는 비로그인 요청이면 401을 반환한다")
	// 비로그인 상태에서 배송지 검색 요청 시 401 응답을 검증합니다.
	void searchShopOrderAddress_returnsUnauthorizedWhenNotLoggedIn() throws Exception {
		// 로그인 쿠키 없이 요청하면 401 응답과 메시지를 검증합니다.
		mockMvc.perform(
				get("/api/shop/order/address/search")
					.param("keyword", "테헤란로")
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
	}

	@Test
	@DisplayName("배송지 등록 API는 로그인 사용자가 요청하면 최신 배송지 목록을 반환한다")
	// 배송지 등록 성공 시 200 응답과 등록 결과 필드 반환 여부를 검증합니다.
	void registerShopOrderAddress_returnsOk() throws Exception {
		// 배송지 등록 결과 응답 객체를 구성합니다.
		ShopOrderAddressVO savedAddress = new ShopOrderAddressVO();
		savedAddress.setCustNo(1L);
		savedAddress.setAddressNm("회사");
		savedAddress.setPostNo("06234");
		savedAddress.setBaseAddress("서울특별시 강남구 테헤란로 1");
		savedAddress.setDetailAddress("101동 1001호");
		savedAddress.setPhoneNumber("010-1234-5678");
		savedAddress.setRsvNm("홍길동");
		savedAddress.setDefaultYn("Y");

		ShopOrderAddressSaveResultVO result = new ShopOrderAddressSaveResultVO();
		result.setAddressList(List.of(savedAddress));
		result.setDefaultAddress(savedAddress);
		result.setSavedAddress(savedAddress);
		when(goodsService.registerShopOrderAddress(any(), eq(1L))).thenReturn(result);

		// 로그인 쿠키와 함께 요청하면 200 응답과 등록 결과 필드를 검증합니다.
		mockMvc.perform(
				post("/api/shop/order/address")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"addressNm":"회사","postNo":"06234","baseAddress":"서울특별시 강남구 테헤란로 1","detailAddress":"101동 1001호","phoneNumber":"010-1234-5678","rsvNm":"홍길동","defaultYn":"Y"}
						""")
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.addressList[0].addressNm").value("회사"))
			.andExpect(jsonPath("$.savedAddress.addressNm").value("회사"))
			.andExpect(jsonPath("$.defaultAddress.defaultYn").value("Y"));
	}

	@Test
	@DisplayName("배송지 등록 API는 비로그인 요청이면 401을 반환한다")
	// 비로그인 상태에서 배송지 등록 요청 시 401 응답을 검증합니다.
	void registerShopOrderAddress_returnsUnauthorizedWhenNotLoggedIn() throws Exception {
		// 로그인 쿠키 없이 요청하면 401 응답과 메시지를 검증합니다.
		mockMvc.perform(
				post("/api/shop/order/address")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"addressNm":"회사","postNo":"06234","baseAddress":"서울특별시 강남구 테헤란로 1","detailAddress":"101동 1001호","phoneNumber":"010-1234-5678","rsvNm":"홍길동","defaultYn":"Y"}
						""")
			)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
	}

	@Test
	@DisplayName("배송지 등록 API는 배송지명 중복이면 400을 반환한다")
	// 서비스에서 중복 배송지명 예외를 반환하면 400 응답으로 변환되는지 검증합니다.
	void registerShopOrderAddress_returnsBadRequestWhenAddressNameDuplicated() throws Exception {
		// 중복 배송지명 예외를 발생하도록 목 동작을 설정합니다.
		when(goodsService.registerShopOrderAddress(any(), eq(1L)))
			.thenThrow(new IllegalArgumentException("이미 사용 중인 배송지명입니다."));

		// 로그인 쿠키와 함께 요청하면 400 응답과 메시지를 검증합니다.
		mockMvc.perform(
				post("/api/shop/order/address")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"addressNm":"회사","postNo":"06234","baseAddress":"서울특별시 강남구 테헤란로 1","detailAddress":"101동 1001호","phoneNumber":"010-1234-5678","rsvNm":"홍길동","defaultYn":"N"}
						""")
			)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("이미 사용 중인 배송지명입니다."));
	}

	@Test
	@DisplayName("배송지 수정 API는 로그인 사용자가 요청하면 최신 배송지 목록을 반환한다")
	// 배송지 수정 성공 시 200 응답과 수정 결과 필드 반환 여부를 검증합니다.
	void updateShopOrderAddress_returnsOk() throws Exception {
		// 배송지 수정 결과 응답 객체를 구성합니다.
		ShopOrderAddressVO savedAddress = new ShopOrderAddressVO();
		savedAddress.setCustNo(1L);
		savedAddress.setAddressNm("우리집");
		savedAddress.setPostNo("06234");
		savedAddress.setBaseAddress("서울특별시 강남구 테헤란로 1");
		savedAddress.setDetailAddress("201동 202호");
		savedAddress.setPhoneNumber("010-2222-3333");
		savedAddress.setRsvNm("홍길동");
		savedAddress.setDefaultYn("Y");

		ShopOrderAddressSaveResultVO result = new ShopOrderAddressSaveResultVO();
		result.setAddressList(List.of(savedAddress));
		result.setDefaultAddress(savedAddress);
		result.setSavedAddress(savedAddress);
		when(goodsService.updateShopOrderAddress(any(), eq(1L))).thenReturn(result);

		// 로그인 쿠키와 함께 요청하면 200 응답과 수정 결과 필드를 검증합니다.
		mockMvc.perform(
				put("/api/shop/order/address")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"originAddressNm":"집","addressNm":"우리집","postNo":"06234","baseAddress":"서울특별시 강남구 테헤란로 1","detailAddress":"201동 202호","phoneNumber":"010-2222-3333","rsvNm":"홍길동","defaultYn":"Y"}
						""")
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.addressList[0].addressNm").value("우리집"))
			.andExpect(jsonPath("$.savedAddress.addressNm").value("우리집"))
			.andExpect(jsonPath("$.defaultAddress.defaultYn").value("Y"));
	}

	@Test
	@DisplayName("배송지 수정 API는 비로그인 요청이면 401을 반환한다")
	// 비로그인 상태에서 배송지 수정 요청 시 401 응답을 검증합니다.
	void updateShopOrderAddress_returnsUnauthorizedWhenNotLoggedIn() throws Exception {
		// 로그인 쿠키 없이 요청하면 401 응답과 메시지를 검증합니다.
		mockMvc.perform(
				put("/api/shop/order/address")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"originAddressNm":"집","addressNm":"우리집","postNo":"06234","baseAddress":"서울특별시 강남구 테헤란로 1","detailAddress":"201동 202호","phoneNumber":"010-2222-3333","rsvNm":"홍길동","defaultYn":"Y"}
						""")
			)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
	}

	@Test
	@DisplayName("배송지 수정 API는 수정 대상이 없으면 400을 반환한다")
	// 서비스에서 수정 대상 없음 예외를 반환하면 400 응답으로 변환되는지 검증합니다.
	void updateShopOrderAddress_returnsBadRequestWhenAddressMissing() throws Exception {
		// 수정 대상 없음 예외를 발생하도록 목 동작을 설정합니다.
		when(goodsService.updateShopOrderAddress(any(), eq(1L)))
			.thenThrow(new IllegalArgumentException("수정할 배송지를 찾을 수 없습니다."));

		// 로그인 쿠키와 함께 요청하면 400 응답과 메시지를 검증합니다.
		mockMvc.perform(
				put("/api/shop/order/address")
					.cookie(new Cookie("cust_no", "1"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{"originAddressNm":"집","addressNm":"우리집","postNo":"06234","baseAddress":"서울특별시 강남구 테헤란로 1","detailAddress":"201동 202호","phoneNumber":"010-2222-3333","rsvNm":"홍길동","defaultYn":"N"}
						""")
			)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("수정할 배송지를 찾을 수 없습니다."));
	}
}
