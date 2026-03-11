package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.shop.auth.ShopCustomerSessionVO;
import com.xodud1202.springbackend.domain.shop.auth.ShopGoogleJoinRequest;
import com.xodud1202.springbackend.domain.shop.auth.ShopGoogleJoinSavePO;
import com.xodud1202.springbackend.domain.shop.auth.ShopGoogleLoginRequest;
import com.xodud1202.springbackend.domain.shop.auth.ShopGoogleLoginResponse;
import com.xodud1202.springbackend.mapper.ShopAuthMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

	@Test
	@DisplayName("구글 회원가입: 선택값이 비어있으면 기본값으로 저장 후 로그인 성공 응답을 반환한다")
	// 선택값 미입력 요청에서 기본 저장값과 로그인 성공 응답을 검증합니다.
	void joinWithGoogle_registersCustomerWhenOptionalValuesAreBlank() {
		// 신규 등록 시나리오용 조회/등록 목 동작을 구성합니다.
		ShopCustomerSessionVO joinedCustomer = new ShopCustomerSessionVO();
		joinedCustomer.setCustNo(11L);
		joinedCustomer.setCustNm("홍길동");
		joinedCustomer.setCustGradeCd("CUST_GRADE_01");
		when(shopAuthMapper.getShopCustomerByCi("google-sub")).thenReturn(null, joinedCustomer);
		doAnswer(invocation -> {
			// insert 구문에서 생성키를 부여합니다.
			ShopGoogleJoinSavePO savePO = invocation.getArgument(0);
			savePO.setCustNo(11L);
			return 1;
		}).when(shopAuthMapper).insertShopGoogleCustomer(any(ShopGoogleJoinSavePO.class));

		// 선택값이 비어있는 회원가입 요청을 구성합니다.
		ShopGoogleJoinRequest request = createDefaultJoinRequest();
		request.setSex("X");
		request.setBirth("");
		request.setPhoneNumber("");
		request.setSmsRsvYn("N");
		request.setEmailRsvYn("N");
		request.setAppPushRsvYn("N");
		request.setDeviceType("WEB");

		// 회원가입을 수행합니다.
		ShopGoogleLoginResponse response = shopAuthService.joinWithGoogle(request);

		// 로그인 성공 응답을 검증합니다.
		assertTrue(response.isLoginSuccess());
		assertFalse(response.isJoinRequired());
		assertEquals(11L, response.getCustNo());
		assertEquals("홍길동", response.getCustNm());
		assertEquals("CUST_GRADE_01", response.getCustGradeCd());
		assertEquals("google_google-sub", response.getLoginId());

		// 저장 파라미터가 기본값으로 들어갔는지 검증합니다.
		ArgumentCaptor<ShopGoogleJoinSavePO> savePOCaptor = ArgumentCaptor.forClass(ShopGoogleJoinSavePO.class);
		verify(shopAuthMapper, times(1)).insertShopGoogleCustomer(savePOCaptor.capture());
		ShopGoogleJoinSavePO capturedSavePO = savePOCaptor.getValue();
		assertEquals("google_google-sub", capturedSavePO.getLoginId());
		assertEquals("X", capturedSavePO.getSex());
		assertNull(capturedSavePO.getBirth());
		assertNull(capturedSavePO.getPhoneNumber());
		assertEquals("N", capturedSavePO.getSmsRsvYn());
		assertEquals("N", capturedSavePO.getEmailRsvYn());
		assertEquals("N", capturedSavePO.getAppPushRsvYn());
		assertEquals("PC", capturedSavePO.getDeviceGbCd());
		assertEquals("GOOGLE", capturedSavePO.getJoinGb());
		assertEquals("CUST_GRADE_01", capturedSavePO.getCustGradeCd());
		assertEquals("CUST_STAT_01", capturedSavePO.getCustStatCd());
		assertEquals("google-sub", capturedSavePO.getCi());
		assertEquals("google-sub", capturedSavePO.getDi());

		// 등록 후 감사 컬럼 갱신 호출을 검증합니다.
		verify(shopAuthMapper, times(1)).updateShopGoogleCustomerAuditNo(11L, 11L);
	}

	@Test
	@DisplayName("구글 회원가입: 선택값이 모두 있으면 정규화 저장 후 로그인 성공 응답을 반환한다")
	// 선택값 입력 요청에서 저장 정규화(생년월일/휴대폰/디바이스)를 검증합니다.
	void joinWithGoogle_registersCustomerWithNormalizedOptionalValues() {
		// 신규 등록 시나리오용 조회/등록 목 동작을 구성합니다.
		ShopCustomerSessionVO joinedCustomer = new ShopCustomerSessionVO();
		joinedCustomer.setCustNo(17L);
		joinedCustomer.setCustNm("김구글");
		joinedCustomer.setCustGradeCd("CUST_GRADE_01");
		when(shopAuthMapper.getShopCustomerByCi("google-sub")).thenReturn(null, joinedCustomer);
		doAnswer(invocation -> {
			// insert 구문에서 생성키를 부여합니다.
			ShopGoogleJoinSavePO savePO = invocation.getArgument(0);
			savePO.setCustNo(17L);
			return 1;
		}).when(shopAuthMapper).insertShopGoogleCustomer(any(ShopGoogleJoinSavePO.class));

		// 선택값이 포함된 회원가입 요청을 구성합니다.
		ShopGoogleJoinRequest request = createDefaultJoinRequest();
		request.setCustNm("김구글");
		request.setSex("M");
		request.setBirth("1990-01-02");
		request.setPhoneNumber("010-1234-5678");
		request.setSmsRsvYn("Y");
		request.setEmailRsvYn("Y");
		request.setAppPushRsvYn("Y");
		request.setDeviceType("MOBILE");

		// 회원가입을 수행합니다.
		ShopGoogleLoginResponse response = shopAuthService.joinWithGoogle(request);

		// 로그인 성공 응답을 검증합니다.
		assertTrue(response.isLoginSuccess());
		assertFalse(response.isJoinRequired());
		assertEquals(17L, response.getCustNo());
		assertEquals("김구글", response.getCustNm());
		assertEquals("google_google-sub", response.getLoginId());

		// 저장 파라미터 정규화 결과를 검증합니다.
		ArgumentCaptor<ShopGoogleJoinSavePO> savePOCaptor = ArgumentCaptor.forClass(ShopGoogleJoinSavePO.class);
		verify(shopAuthMapper, times(1)).insertShopGoogleCustomer(savePOCaptor.capture());
		ShopGoogleJoinSavePO capturedSavePO = savePOCaptor.getValue();
		assertEquals("M", capturedSavePO.getSex());
		assertEquals("19900102", capturedSavePO.getBirth());
		assertEquals("01012345678", capturedSavePO.getPhoneNumber());
		assertEquals("Y", capturedSavePO.getSmsRsvYn());
		assertEquals("Y", capturedSavePO.getEmailRsvYn());
		assertEquals("Y", capturedSavePO.getAppPushRsvYn());
		assertEquals("MO", capturedSavePO.getDeviceGbCd());
	}

	@Test
	@DisplayName("구글 회원가입: 성별 값이 X/M/F가 아니면 예외를 반환한다")
	// 성별 코드가 허용값이 아니면 예외를 검증합니다.
	void joinWithGoogle_throwsWhenSexInvalid() {
		// 잘못된 성별 코드를 가진 요청을 구성합니다.
		ShopGoogleJoinRequest request = createDefaultJoinRequest();
		request.setSex("W");

		// 예외 메시지를 검증합니다.
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> shopAuthService.joinWithGoogle(request));
		assertEquals("성별 값이 올바르지 않습니다.", exception.getMessage());
	}

	@Test
	@DisplayName("구글 회원가입: 생년월일 형식이 YYYY-MM-DD가 아니면 예외를 반환한다")
	// 생년월일 형식 오류를 검증합니다.
	void joinWithGoogle_throwsWhenBirthFormatInvalid() {
		// 생년월일 형식이 잘못된 요청을 구성합니다.
		ShopGoogleJoinRequest request = createDefaultJoinRequest();
		request.setBirth("19900102");

		// 예외 메시지를 검증합니다.
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> shopAuthService.joinWithGoogle(request));
		assertEquals("생년월일은 YYYY-MM-DD 형식으로 입력해주세요.", exception.getMessage());
	}

	@Test
	@DisplayName("구글 회원가입: 생년월일이 현재 날짜를 초과하면 예외를 반환한다")
	// 미래 생년월일 입력 오류를 검증합니다.
	void joinWithGoogle_throwsWhenBirthIsFutureDate() {
		// 현재 날짜 +1일 값을 생년월일로 설정합니다.
		String tomorrow = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		ShopGoogleJoinRequest request = createDefaultJoinRequest();
		request.setBirth(tomorrow);

		// 예외 메시지를 검증합니다.
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> shopAuthService.joinWithGoogle(request));
		assertEquals("생년월일은 현재 날짜를 초과할 수 없습니다.", exception.getMessage());
	}

	@Test
	@DisplayName("구글 회원가입: 휴대폰번호 형식이 010-0000-0000이 아니면 예외를 반환한다")
	// 휴대폰번호 형식 오류를 검증합니다.
	void joinWithGoogle_throwsWhenPhoneNumberFormatInvalid() {
		// 하이픈 없는 휴대폰번호 요청을 구성합니다.
		ShopGoogleJoinRequest request = createDefaultJoinRequest();
		request.setPhoneNumber("01012345678");

		// 예외 메시지를 검증합니다.
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> shopAuthService.joinWithGoogle(request));
		assertEquals("휴대폰번호는 010-0000-0000 형식으로 입력해주세요.", exception.getMessage());
	}

	@Test
	@DisplayName("구글 회원가입: 필수 약관에 동의하지 않으면 예외를 반환한다")
	// 필수 약관 미동의 오류를 검증합니다.
	void joinWithGoogle_throwsWhenRequiredAgreementMissing() {
		// 개인정보 처리방침 미동의 요청을 구성합니다.
		ShopGoogleJoinRequest request = createDefaultJoinRequest();
		request.setPrivateAgreeYn("N");

		// 예외 메시지를 검증합니다.
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> shopAuthService.joinWithGoogle(request));
		assertEquals("개인정보 처리 방침 동의는 필수입니다.", exception.getMessage());
	}

	@Test
	@DisplayName("구글 회원가입: 디바이스 타입이 WEB/MOBILE/APP가 아니면 예외를 반환한다")
	// 디바이스 타입 허용값 검증을 확인합니다.
	void joinWithGoogle_throwsWhenDeviceTypeInvalid() {
		// 허용되지 않는 디바이스 타입 요청을 구성합니다.
		ShopGoogleJoinRequest request = createDefaultJoinRequest();
		request.setDeviceType("TABLET");

		// 예외 메시지를 검증합니다.
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> shopAuthService.joinWithGoogle(request));
		assertEquals("가입 디바이스 값이 올바르지 않습니다.", exception.getMessage());
	}

	@Test
	@DisplayName("구글 회원가입: 동일 CI 기존 고객이 있으면 신규 등록 없이 로그인 성공 응답을 반환한다")
	// 기존 고객 중복 가입 방지 로직을 검증합니다.
	void joinWithGoogle_returnsLoginSuccessWhenCustomerAlreadyExists() {
		// 기존 고객 조회 결과를 구성합니다.
		ShopCustomerSessionVO existingCustomer = new ShopCustomerSessionVO();
		existingCustomer.setCustNo(23L);
		existingCustomer.setCustNm("기존고객");
		existingCustomer.setCustGradeCd("CUST_GRADE_03");
		when(shopAuthMapper.getShopCustomerByCi("google-sub")).thenReturn(existingCustomer);

		// 회원가입을 수행합니다.
		ShopGoogleJoinRequest request = createDefaultJoinRequest();
		ShopGoogleLoginResponse response = shopAuthService.joinWithGoogle(request);

		// 중복 등록 없이 로그인 성공 응답을 검증합니다.
		assertTrue(response.isLoginSuccess());
		assertFalse(response.isJoinRequired());
		assertEquals(23L, response.getCustNo());
		assertEquals("기존고객", response.getCustNm());
		assertEquals("CUST_GRADE_03", response.getCustGradeCd());
		assertEquals("google_google-sub", response.getLoginId());

		// insert/update 호출이 없는지 검증합니다.
		verify(shopAuthMapper, never()).insertShopGoogleCustomer(any(ShopGoogleJoinSavePO.class));
		verify(shopAuthMapper, never()).updateShopGoogleCustomerAuditNo(any(Long.class), any(Long.class));
	}

	// 기본 구글 회원가입 요청 객체를 생성합니다.
	private ShopGoogleJoinRequest createDefaultJoinRequest() {
		// 필수값이 포함된 기본 요청 객체를 구성합니다.
		ShopGoogleJoinRequest request = new ShopGoogleJoinRequest();
		request.setSub("google-sub");
		request.setEmail("google-user@test.com");
		request.setCustNm("홍길동");
		request.setSex("X");
		request.setBirth("");
		request.setPhoneNumber("");
		request.setSmsRsvYn("N");
		request.setEmailRsvYn("N");
		request.setAppPushRsvYn("N");
		request.setPrivateAgreeYn("Y");
		request.setTermsAgreeYn("Y");
		request.setDeviceType("WEB");
		return request;
	}
}
