package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.shop.auth.ShopCustomerSessionVO;
import com.xodud1202.springbackend.domain.shop.auth.ShopGoogleLoginRequest;
import com.xodud1202.springbackend.domain.shop.auth.ShopGoogleLoginResponse;
import com.xodud1202.springbackend.mapper.ShopAuthMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// ShopAuthService의 구글 로그인 판정 로직을 검증합니다.
class ShopAuthServiceTests {

	// 쇼핑몰 로그인 매퍼 목 객체입니다.
	@Mock
	private ShopAuthMapper shopAuthMapper;

	// 테스트 대상 서비스입니다.
	@InjectMocks
	private ShopAuthService shopAuthService;

	@Test
	@DisplayName("구글 로그인 판정: 기존 고객이 있으면 로그인 성공 응답을 반환한다")
	// 기존 고객이 CI로 조회되면 로그인 성공 응답을 반환합니다.
	void loginWithGoogle_returnsLoginSuccessWhenCustomerExists() {
		// 기존 고객 목 데이터를 구성합니다.
		ShopCustomerSessionVO customer = new ShopCustomerSessionVO();
		customer.setCustNo(7L);
		customer.setCustNm("홍길동");
		customer.setCustGradeCd("CUST_GRADE_03");
		when(shopAuthMapper.getShopCustomerByCi("google-sub")).thenReturn(customer);

		// 구글 로그인 판정을 수행합니다.
		ShopGoogleLoginRequest request = new ShopGoogleLoginRequest();
		request.setSub("google-sub");
		ShopGoogleLoginResponse response = shopAuthService.loginWithGoogle(request);

		// 로그인 성공 응답을 검증합니다.
		assertTrue(response.isLoginSuccess());
		assertFalse(response.isJoinRequired());
		assertEquals(7L, response.getCustNo());
		assertEquals("홍길동", response.getCustNm());
		assertEquals("CUST_GRADE_03", response.getCustGradeCd());
		assertEquals("google_google-sub", response.getLoginId());
	}

	@Test
	@DisplayName("구글 로그인 판정: 기존 고객이 없으면 추가 정보 입력 응답을 반환한다")
	// 기존 고객이 없으면 추가 정보 입력이 필요한 응답을 반환합니다.
	void loginWithGoogle_returnsJoinRequiredWhenCustomerMissing() {
		// 고객 미조회 응답을 목으로 고정합니다.
		when(shopAuthMapper.getShopCustomerByCi("google-sub")).thenReturn(null);

		// 구글 로그인 판정을 수행합니다.
		ShopGoogleLoginRequest request = new ShopGoogleLoginRequest();
		request.setSub("google-sub");
		ShopGoogleLoginResponse response = shopAuthService.loginWithGoogle(request);

		// 추가 정보 입력 필요 응답을 검증합니다.
		assertFalse(response.isLoginSuccess());
		assertTrue(response.isJoinRequired());
		assertEquals("google_google-sub", response.getLoginId());
	}

	@Test
	@DisplayName("구글 로그인 판정: sub 값이 없으면 예외를 반환한다")
	// sub 값이 없으면 유효성 예외를 반환합니다.
	void loginWithGoogle_throwsWhenSubMissing() {
		// 빈 요청으로 예외를 검증합니다.
		ShopGoogleLoginRequest request = new ShopGoogleLoginRequest();
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> shopAuthService.loginWithGoogle(request));
		assertEquals("구글 사용자 식별값을 확인해주세요.", exception.getMessage());
	}
}
