package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.common.mybatis.GeneratedLongKey;
import com.xodud1202.springbackend.domain.shop.auth.ShopCouponIssueRuleVO;
import com.xodud1202.springbackend.domain.shop.auth.ShopCustomerCouponSavePO;
import com.xodud1202.springbackend.domain.shop.auth.ShopCustomerGradeBenefitVO;
import com.xodud1202.springbackend.domain.shop.auth.ShopCustomerPointDetailSavePO;
import com.xodud1202.springbackend.domain.shop.auth.ShopCustomerPointSavePO;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// ShopAuthService의 구글 로그인/회원가입 로직을 검증합니다.
class ShopAuthServiceTests {
	@Mock
	private ShopAuthMapper shopAuthMapper;

	@InjectMocks
	private ShopAuthService shopAuthService;

	@Test
	@DisplayName("고객 등급명 조회: 공통코드에 등급명이 있으면 코드명을 반환한다")
	// 고객등급 코드가 공통코드에 존재하면 코드명을 반환합니다.
	void getCustomerGradeName_returnsCodeNameWhenCodeExists() {
		// 공통코드 조회 목 데이터를 구성합니다.
		when(shopAuthMapper.getCommonCodeName("CUST_GRADE", "CUST_GRADE_03")).thenReturn("GOLD");

		// 고객 등급명을 조회합니다.
		String custGradeNm = shopAuthService.getCustomerGradeName("CUST_GRADE_03");

		// 공통코드 코드명이 반환되는지 검증합니다.
		assertEquals("GOLD", custGradeNm);
	}

	@Test
	@DisplayName("고객 등급명 조회: 공통코드 미조회면 등급코드 값을 대체 반환한다")
	// 고객등급 코드에 매핑된 코드명이 없으면 코드값 자체를 반환합니다.
	void getCustomerGradeName_returnsCodeWhenCodeNameMissing() {
		// 공통코드 미조회 응답을 목으로 구성합니다.
		when(shopAuthMapper.getCommonCodeName("CUST_GRADE", "CUST_GRADE_03")).thenReturn(null);

		// 앞뒤 공백이 포함된 고객 등급명을 조회합니다.
		String custGradeNm = shopAuthService.getCustomerGradeName(" CUST_GRADE_03 ");

		// 공통코드 미조회 시 코드값 대체 반환을 검증합니다.
		assertEquals("CUST_GRADE_03", custGradeNm);
	}

	@Test
	@DisplayName("구글 로그인 판정: 기존 고객이 있으면 로그인 성공 응답을 반환한다")
	// 기존 고객이 CI로 조회되면 로그인 성공 응답을 반환합니다.
	void loginWithGoogle_returnsLoginSuccessWhenCustomerExists() {
		// 기존 고객 목 데이터를 구성합니다.
		ShopCustomerSessionVO customer = new ShopCustomerSessionVO(
			7L,
			"google_google-sub",
			"홍길동",
			"CUST_GRADE_03",
			"google-sub",
			"google-user@test.com"
		);
		when(shopAuthMapper.getShopCustomerByCi("google-sub")).thenReturn(customer);

		// 구글 로그인 판정을 수행합니다.
		ShopGoogleLoginResponse response = shopAuthService.loginWithGoogle(
			new ShopGoogleLoginRequest("google-sub", "google-user@test.com", "홍길동", null)
		);

		// 로그인 성공 응답을 검증합니다.
		assertTrue(response.loginSuccess());
		assertFalse(response.joinRequired());
		assertEquals(7L, response.custNo());
		assertEquals("홍길동", response.custNm());
		assertEquals("CUST_GRADE_03", response.custGradeCd());
		assertEquals("google_google-sub", response.loginId());
	}

	@Test
	@DisplayName("구글 로그인 판정: 기존 고객이 없으면 추가 정보 입력 응답을 반환한다")
	// 기존 고객이 없으면 추가 정보 입력이 필요한 응답을 반환합니다.
	void loginWithGoogle_returnsJoinRequiredWhenCustomerMissing() {
		// 고객 미조회 응답을 목으로 고정합니다.
		when(shopAuthMapper.getShopCustomerByCi("google-sub")).thenReturn(null);

		// 구글 로그인 판정을 수행합니다.
		ShopGoogleLoginResponse response = shopAuthService.loginWithGoogle(
			new ShopGoogleLoginRequest("google-sub", "google-user@test.com", "홍길동", null)
		);

		// 추가 정보 입력 필요 응답을 검증합니다.
		assertFalse(response.loginSuccess());
		assertTrue(response.joinRequired());
		assertEquals("google_google-sub", response.loginId());
	}

	@Test
	@DisplayName("구글 로그인 판정: sub 값이 없으면 예외를 반환한다")
	// sub 값이 없으면 유효성 예외를 반환합니다.
	void loginWithGoogle_throwsWhenSubMissing() {
		// 빈 요청으로 예외를 검증합니다.
		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> shopAuthService.loginWithGoogle(new ShopGoogleLoginRequest(null, null, null, null))
		);
		assertEquals("구글 사용자 식별값을 확인해주세요.", exception.getMessage());
	}

	@Test
	@DisplayName("구글 회원가입: 선택값이 비어있으면 기본값으로 저장 후 로그인 성공 응답을 반환한다")
	// 선택값 미입력 요청에서 기본 저장값과 로그인 성공 응답을 검증합니다.
	void joinWithGoogle_registersCustomerWhenOptionalValuesAreBlank() {
		// 신규 등록 시나리오용 조회/등록 목 동작을 구성합니다.
		ShopCustomerSessionVO joinedCustomer = new ShopCustomerSessionVO(
			11L,
			"google_google-sub",
			"홍길동",
			"CUST_GRADE_01",
			"google-sub",
			"google-user@test.com"
		);
		when(shopAuthMapper.getShopCustomerByCi("google-sub")).thenReturn(null, joinedCustomer);
		doAnswer(invocation -> {
			GeneratedLongKey generatedKey = invocation.getArgument(1);
			generatedKey.setValue(11L);
			return 1;
		}).when(shopAuthMapper).insertShopGoogleCustomer(any(ShopGoogleJoinSavePO.class), any(GeneratedLongKey.class));

		// 회원가입을 수행합니다.
		ShopGoogleLoginResponse response = shopAuthService.joinWithGoogle(createDefaultJoinRequest());

		// 로그인 성공 응답을 검증합니다.
		assertTrue(response.loginSuccess());
		assertFalse(response.joinRequired());
		assertEquals(11L, response.custNo());
		assertEquals("홍길동", response.custNm());
		assertEquals("CUST_GRADE_01", response.custGradeCd());
		assertEquals("google_google-sub", response.loginId());

		// 저장 파라미터가 기본값으로 들어갔는지 검증합니다.
		ArgumentCaptor<ShopGoogleJoinSavePO> savePOCaptor = ArgumentCaptor.forClass(ShopGoogleJoinSavePO.class);
		verify(shopAuthMapper, times(1)).insertShopGoogleCustomer(savePOCaptor.capture(), any(GeneratedLongKey.class));
		ShopGoogleJoinSavePO capturedSavePO = savePOCaptor.getValue();
		assertEquals("google_google-sub", capturedSavePO.loginId());
		assertEquals("X", capturedSavePO.sex());
		assertEquals(null, capturedSavePO.birth());
		assertEquals(null, capturedSavePO.phoneNumber());
		assertEquals("N", capturedSavePO.smsRsvYn());
		assertEquals("N", capturedSavePO.emailRsvYn());
		assertEquals("N", capturedSavePO.appPushRsvYn());
		assertEquals("PC", capturedSavePO.deviceGbCd());
		assertEquals("GOOGLE", capturedSavePO.joinGb());
		assertEquals("CUST_GRADE_01", capturedSavePO.custGradeCd());
		assertEquals("CUST_STAT_01", capturedSavePO.custStatCd());
		assertEquals("google-sub", capturedSavePO.ci());
		assertEquals("google-sub", capturedSavePO.di());

		// 등록 후 감사 컬럼 갱신 호출을 검증합니다.
		verify(shopAuthMapper, times(1)).updateShopGoogleCustomerAuditNo(11L, 11L);
	}

	@Test
	@DisplayName("구글 회원가입: 선택값이 모두 있으면 정규화 저장 후 로그인 성공 응답을 반환한다")
	// 선택값 입력 요청에서 저장 정규화(생년월일/휴대폰/디바이스)를 검증합니다.
	void joinWithGoogle_registersCustomerWithNormalizedOptionalValues() {
		// 신규 등록 시나리오용 조회/등록 목 동작을 구성합니다.
		ShopCustomerSessionVO joinedCustomer = new ShopCustomerSessionVO(
			17L,
			"google_google-sub",
			"김구글",
			"CUST_GRADE_01",
			"google-sub",
			"google-user@test.com"
		);
		when(shopAuthMapper.getShopCustomerByCi("google-sub")).thenReturn(null, joinedCustomer);
		doAnswer(invocation -> {
			GeneratedLongKey generatedKey = invocation.getArgument(1);
			generatedKey.setValue(17L);
			return 1;
		}).when(shopAuthMapper).insertShopGoogleCustomer(any(ShopGoogleJoinSavePO.class), any(GeneratedLongKey.class));

		// 선택값이 포함된 회원가입 요청을 구성합니다.
		ShopGoogleJoinRequest request = new ShopGoogleJoinRequest(
			"google-sub",
			"google-user@test.com",
			"김구글",
			"M",
			"1990-01-02",
			"010-1234-5678",
			"Y",
			"Y",
			"Y",
			"Y",
			"Y",
			"MOBILE"
		);

		// 회원가입을 수행합니다.
		ShopGoogleLoginResponse response = shopAuthService.joinWithGoogle(request);

		// 로그인 성공 응답을 검증합니다.
		assertTrue(response.loginSuccess());
		assertFalse(response.joinRequired());
		assertEquals(17L, response.custNo());
		assertEquals("김구글", response.custNm());
		assertEquals("google_google-sub", response.loginId());

		// 저장 파라미터 정규화 결과를 검증합니다.
		ArgumentCaptor<ShopGoogleJoinSavePO> savePOCaptor = ArgumentCaptor.forClass(ShopGoogleJoinSavePO.class);
		verify(shopAuthMapper, times(1)).insertShopGoogleCustomer(savePOCaptor.capture(), any(GeneratedLongKey.class));
		ShopGoogleJoinSavePO capturedSavePO = savePOCaptor.getValue();
		assertEquals("M", capturedSavePO.sex());
		assertEquals("19900102", capturedSavePO.birth());
		assertEquals("01012345678", capturedSavePO.phoneNumber());
		assertEquals("Y", capturedSavePO.smsRsvYn());
		assertEquals("Y", capturedSavePO.emailRsvYn());
		assertEquals("Y", capturedSavePO.appPushRsvYn());
		assertEquals("MO", capturedSavePO.deviceGbCd());
	}

	@Test
	@DisplayName("구글 회원가입: 성별 값이 X/M/F가 아니면 예외를 반환한다")
	// 성별 코드가 허용값이 아니면 예외를 검증합니다.
	void joinWithGoogle_throwsWhenSexInvalid() {
		// 잘못된 성별 코드를 가진 요청을 구성합니다.
		ShopGoogleJoinRequest request = new ShopGoogleJoinRequest(
			"google-sub",
			"google-user@test.com",
			"홍길동",
			"W",
			"",
			"",
			"N",
			"N",
			"N",
			"Y",
			"Y",
			"WEB"
		);

		// 예외 메시지를 검증합니다.
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> shopAuthService.joinWithGoogle(request));
		assertEquals("성별 값이 올바르지 않습니다.", exception.getMessage());
	}

	@Test
	@DisplayName("구글 회원가입: 생년월일 형식이 YYYY-MM-DD가 아니면 예외를 반환한다")
	// 생년월일 형식 오류를 검증합니다.
	void joinWithGoogle_throwsWhenBirthFormatInvalid() {
		// 생년월일 형식이 잘못된 요청을 구성합니다.
		ShopGoogleJoinRequest request = new ShopGoogleJoinRequest(
			"google-sub",
			"google-user@test.com",
			"홍길동",
			"X",
			"19900102",
			"",
			"N",
			"N",
			"N",
			"Y",
			"Y",
			"WEB"
		);

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
		ShopGoogleJoinRequest request = new ShopGoogleJoinRequest(
			"google-sub",
			"google-user@test.com",
			"홍길동",
			"X",
			tomorrow,
			"",
			"N",
			"N",
			"N",
			"Y",
			"Y",
			"WEB"
		);

		// 예외 메시지를 검증합니다.
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> shopAuthService.joinWithGoogle(request));
		assertEquals("생년월일은 현재 날짜를 초과할 수 없습니다.", exception.getMessage());
	}

	@Test
	@DisplayName("구글 회원가입: 휴대폰번호 형식이 010-0000-0000이 아니면 예외를 반환한다")
	// 휴대폰번호 형식 오류를 검증합니다.
	void joinWithGoogle_throwsWhenPhoneNumberFormatInvalid() {
		// 하이픈 없는 휴대폰번호 요청을 구성합니다.
		ShopGoogleJoinRequest request = new ShopGoogleJoinRequest(
			"google-sub",
			"google-user@test.com",
			"홍길동",
			"X",
			"",
			"01012345678",
			"N",
			"N",
			"N",
			"Y",
			"Y",
			"WEB"
		);

		// 예외 메시지를 검증합니다.
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> shopAuthService.joinWithGoogle(request));
		assertEquals("휴대폰번호는 010-0000-0000 형식으로 입력해주세요.", exception.getMessage());
	}

	@Test
	@DisplayName("구글 회원가입: 필수 약관에 동의하지 않으면 예외를 반환한다")
	// 필수 약관 미동의 오류를 검증합니다.
	void joinWithGoogle_throwsWhenRequiredAgreementMissing() {
		// 개인정보 처리방침 미동의 요청을 구성합니다.
		ShopGoogleJoinRequest request = new ShopGoogleJoinRequest(
			"google-sub",
			"google-user@test.com",
			"홍길동",
			"X",
			"",
			"",
			"N",
			"N",
			"N",
			"N",
			"Y",
			"WEB"
		);

		// 예외 메시지를 검증합니다.
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> shopAuthService.joinWithGoogle(request));
		assertEquals("개인정보 처리 방침 동의는 필수입니다.", exception.getMessage());
	}

	@Test
	@DisplayName("구글 회원가입: 디바이스 타입이 WEB/MOBILE/APP가 아니면 예외를 반환한다")
	// 디바이스 타입 허용값 검증을 확인합니다.
	void joinWithGoogle_throwsWhenDeviceTypeInvalid() {
		// 허용되지 않는 디바이스 타입 요청을 구성합니다.
		ShopGoogleJoinRequest request = new ShopGoogleJoinRequest(
			"google-sub",
			"google-user@test.com",
			"홍길동",
			"X",
			"",
			"",
			"N",
			"N",
			"N",
			"Y",
			"Y",
			"TABLET"
		);

		// 예외 메시지를 검증합니다.
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> shopAuthService.joinWithGoogle(request));
		assertEquals("가입 디바이스 값이 올바르지 않습니다.", exception.getMessage());
	}

	@Test
	@DisplayName("구글 회원가입: 동일 CI 기존 고객이 있으면 신규 등록 없이 로그인 성공 응답을 반환한다")
	// 기존 고객 중복 가입 방지 로직을 검증합니다.
	void joinWithGoogle_returnsLoginSuccessWhenCustomerAlreadyExists() {
		// 기존 고객 조회 결과를 구성합니다.
		ShopCustomerSessionVO existingCustomer = new ShopCustomerSessionVO(
			23L,
			"google_google-sub",
			"기존고객",
			"CUST_GRADE_03",
			"google-sub",
			"google-user@test.com"
		);
		when(shopAuthMapper.getShopCustomerByCi("google-sub")).thenReturn(existingCustomer);

		// 회원가입을 수행합니다.
		ShopGoogleLoginResponse response = shopAuthService.joinWithGoogle(createDefaultJoinRequest());

		// 중복 등록 없이 로그인 성공 응답을 검증합니다.
		assertTrue(response.loginSuccess());
		assertFalse(response.joinRequired());
		assertEquals(23L, response.custNo());
		assertEquals("기존고객", response.custNm());
		assertEquals("CUST_GRADE_03", response.custGradeCd());
		assertEquals("google_google-sub", response.loginId());

		// insert/update 호출이 없는지 검증합니다.
		verify(shopAuthMapper, never()).insertShopGoogleCustomer(any(ShopGoogleJoinSavePO.class), any(GeneratedLongKey.class));
		verify(shopAuthMapper, never()).updateShopGoogleCustomerAuditNo(any(Long.class), any(Long.class));
	}

	@Test
	@DisplayName("구글 회원가입: 사이트 가입 포인트가 0보다 크면 고객 포인트를 지급한다")
	// 가입 포인트 지급 로직을 검증합니다.
	void joinWithGoogle_grantsJoinPointWhenSiteJoinPointIsPositive() {
		// 신규 등록 시나리오용 조회/등록 목 동작을 구성합니다.
		ShopCustomerSessionVO joinedCustomer = new ShopCustomerSessionVO(
			31L,
			"google_google-sub",
			"포인트회원",
			"CUST_GRADE_01",
			"google-sub",
			"google-user@test.com"
		);
		when(shopAuthMapper.getShopCustomerByCi("google-sub")).thenReturn(null, joinedCustomer);
		doAnswer(invocation -> {
			GeneratedLongKey generatedKey = invocation.getArgument(1);
			generatedKey.setValue(31L);
			return 1;
		}).when(shopAuthMapper).insertShopGoogleCustomer(any(ShopGoogleJoinSavePO.class), any(GeneratedLongKey.class));
		when(shopAuthMapper.getShopJoinPoint("xodud1202")).thenReturn(2000);
		doAnswer(invocation -> {
			GeneratedLongKey generatedKey = invocation.getArgument(1);
			generatedKey.setValue(41L);
			return 1;
		}).when(shopAuthMapper).insertCustomerPointBase(any(ShopCustomerPointSavePO.class), any(GeneratedLongKey.class));
		when(shopAuthMapper.getCustomerGradeBenefitByCustGradeCd("CUST_GRADE_01")).thenReturn(null);

		// 회원가입을 수행합니다.
		shopAuthService.joinWithGoogle(
			new ShopGoogleJoinRequest(
				"google-sub",
				"google-user@test.com",
				"포인트회원",
				"X",
				"",
				"",
				"N",
				"N",
				"N",
				"Y",
				"Y",
				"WEB"
			)
		);

		// 포인트 마스터 저장값을 검증합니다.
		ArgumentCaptor<ShopCustomerPointSavePO> pointSaveCaptor = ArgumentCaptor.forClass(ShopCustomerPointSavePO.class);
		verify(shopAuthMapper, times(1)).insertCustomerPointBase(pointSaveCaptor.capture(), any(GeneratedLongKey.class));
		ShopCustomerPointSavePO capturedPointSavePO = pointSaveCaptor.getValue();
		assertEquals(31L, capturedPointSavePO.custNo());
		assertEquals("JOIN_POINT", capturedPointSavePO.pntGiveGbCd());
		assertEquals(2000, capturedPointSavePO.saveAmt());
		assertEquals(31L, capturedPointSavePO.regNo());
		assertEquals(31L, capturedPointSavePO.udtNo());

		// 포인트 상세 저장값을 검증합니다.
		ArgumentCaptor<ShopCustomerPointDetailSavePO> pointDetailCaptor = ArgumentCaptor.forClass(ShopCustomerPointDetailSavePO.class);
		verify(shopAuthMapper, times(1)).insertCustomerPointDetail(pointDetailCaptor.capture());
		ShopCustomerPointDetailSavePO capturedPointDetailPO = pointDetailCaptor.getValue();
		assertEquals(41L, capturedPointDetailPO.pntNo());
		assertEquals(2000, capturedPointDetailPO.pntAmt());
		assertEquals("회원가입 포인트 지급", capturedPointDetailPO.bigo());
		assertEquals(31L, capturedPointDetailPO.regNo());

		// 등급혜택이 없는 경우 쿠폰 지급은 호출되지 않는지 검증합니다.
		verify(shopAuthMapper, never()).insertCustomerCoupon(any(ShopCustomerCouponSavePO.class));
	}

	@Test
	@DisplayName("구글 회원가입: 사이트 가입 포인트가 0이면 포인트 미지급, 정상 상태 쿠폰만 CNT만큼 지급한다")
	// 포인트 미지급과 쿠폰 지급 수량/상태 조건을 함께 검증합니다.
	void joinWithGoogle_skipsJoinPointWhenZeroAndIssuesCouponsByCountForActiveCouponOnly() {
		// 신규 등록 시나리오용 조회/등록 목 동작을 구성합니다.
		ShopCustomerSessionVO joinedCustomer = new ShopCustomerSessionVO(
			51L,
			"google_google-sub",
			"쿠폰회원",
			"CUST_GRADE_01",
			"google-sub",
			"google-user@test.com"
		);
		when(shopAuthMapper.getShopCustomerByCi("google-sub")).thenReturn(null, joinedCustomer);
		doAnswer(invocation -> {
			GeneratedLongKey generatedKey = invocation.getArgument(1);
			generatedKey.setValue(51L);
			return 1;
		}).when(shopAuthMapper).insertShopGoogleCustomer(any(ShopGoogleJoinSavePO.class), any(GeneratedLongKey.class));
		when(shopAuthMapper.getShopJoinPoint("xodud1202")).thenReturn(0);

		// 고객등급 혜택(상품 쿠폰 2장, 장바구니 쿠폰 1장)을 구성합니다.
		ShopCustomerGradeBenefitVO benefitVO = new ShopCustomerGradeBenefitVO("CUST_GRADE_01", 1L, 2, 2L, 1, null, null);
		when(shopAuthMapper.getCustomerGradeBenefitByCustGradeCd("CUST_GRADE_01")).thenReturn(benefitVO);

		// 정상 상태 쿠폰 1번만 발급 가능하도록 규칙을 구성합니다.
		ShopCouponIssueRuleVO goodsCouponRule = new ShopCouponIssueRuleVO(
			1L,
			"CPN_STAT_02",
			"CPN_USE_DT_02",
			null,
			LocalDateTime.of(2026, 3, 1, 0, 0, 0),
			LocalDateTime.of(2026, 3, 31, 23, 59, 59)
		);
		when(shopAuthMapper.getIssuableCouponIssueRule(1L)).thenReturn(goodsCouponRule);
		when(shopAuthMapper.getIssuableCouponIssueRule(2L)).thenReturn(null);

		// 회원가입을 수행합니다.
		shopAuthService.joinWithGoogle(
			new ShopGoogleJoinRequest(
				"google-sub",
				"google-user@test.com",
				"쿠폰회원",
				"X",
				"",
				"",
				"N",
				"N",
				"N",
				"Y",
				"Y",
				"WEB"
			)
		);

		// 가입 포인트가 0이면 포인트 지급 호출이 없어야 합니다.
		verify(shopAuthMapper, never()).insertCustomerPointBase(any(ShopCustomerPointSavePO.class), any(GeneratedLongKey.class));
		verify(shopAuthMapper, never()).insertCustomerPointDetail(any(ShopCustomerPointDetailSavePO.class));

		// 정상 상태 쿠폰(1번)만 수량 2건 지급되는지 검증합니다.
		ArgumentCaptor<ShopCustomerCouponSavePO> couponSaveCaptor = ArgumentCaptor.forClass(ShopCustomerCouponSavePO.class);
		verify(shopAuthMapper, times(2)).insertCustomerCoupon(couponSaveCaptor.capture());
		for (ShopCustomerCouponSavePO couponSavePO : couponSaveCaptor.getAllValues()) {
			assertEquals(51L, couponSavePO.custNo());
			assertEquals(1L, couponSavePO.cpnNo());
			assertEquals(LocalDateTime.of(2026, 3, 1, 0, 0, 0), couponSavePO.cpnUsableStartDt());
			assertEquals(LocalDateTime.of(2026, 3, 31, 23, 59, 59), couponSavePO.cpnUsableEndDt());
			assertEquals(51L, couponSavePO.regNo());
			assertEquals(51L, couponSavePO.udtNo());
		}
	}

	@Test
	@DisplayName("구글 회원가입: 기간형 쿠폰(CPN_USE_DT_01)은 현재 시점 기준으로 사용기간을 계산해 CNT만큼 지급한다")
	// 기간형 쿠폰 지급 시 사용 시작/종료 일시 계산 로직을 검증합니다.
	void joinWithGoogle_issuesPeriodCouponWithNowBasedUsableWindow() {
		// 신규 등록 시나리오용 조회/등록 목 동작을 구성합니다.
		ShopCustomerSessionVO joinedCustomer = new ShopCustomerSessionVO(
			61L,
			"google_google-sub",
			"기간쿠폰회원",
			"CUST_GRADE_01",
			"google-sub",
			"google-user@test.com"
		);
		when(shopAuthMapper.getShopCustomerByCi("google-sub")).thenReturn(null, joinedCustomer);
		doAnswer(invocation -> {
			GeneratedLongKey generatedKey = invocation.getArgument(1);
			generatedKey.setValue(61L);
			return 1;
		}).when(shopAuthMapper).insertShopGoogleCustomer(any(ShopGoogleJoinSavePO.class), any(GeneratedLongKey.class));
		when(shopAuthMapper.getShopJoinPoint("xodud1202")).thenReturn(0);

		// 기간형 쿠폰 2장 지급 혜택을 구성합니다.
		ShopCustomerGradeBenefitVO benefitVO = new ShopCustomerGradeBenefitVO("CUST_GRADE_01", null, null, 2L, 2, null, null);
		when(shopAuthMapper.getCustomerGradeBenefitByCustGradeCd("CUST_GRADE_01")).thenReturn(benefitVO);

		// 기간형 쿠폰 규칙(CPN_USE_DT_01, 30일)을 구성합니다.
		ShopCouponIssueRuleVO periodCouponRule = new ShopCouponIssueRuleVO(2L, "CPN_STAT_02", "CPN_USE_DT_01", 30, null, null);
		when(shopAuthMapper.getIssuableCouponIssueRule(2L)).thenReturn(periodCouponRule);

		// 회원가입 직전/직후 시간을 저장해 사용 시작 시점 범위를 검증합니다.
		LocalDateTime beforeJoin = LocalDateTime.now();
		shopAuthService.joinWithGoogle(
			new ShopGoogleJoinRequest(
				"google-sub",
				"google-user@test.com",
				"기간쿠폰회원",
				"X",
				"",
				"",
				"N",
				"N",
				"N",
				"Y",
				"Y",
				"WEB"
			)
		);
		LocalDateTime afterJoin = LocalDateTime.now();

		// 기간형 쿠폰이 요청 수량(2건)만큼 지급되는지 검증합니다.
		ArgumentCaptor<ShopCustomerCouponSavePO> couponSaveCaptor = ArgumentCaptor.forClass(ShopCustomerCouponSavePO.class);
		verify(shopAuthMapper, times(2)).insertCustomerCoupon(couponSaveCaptor.capture());
		for (ShopCustomerCouponSavePO couponSavePO : couponSaveCaptor.getAllValues()) {
			assertEquals(61L, couponSavePO.custNo());
			assertEquals(2L, couponSavePO.cpnNo());
			assertEquals(61L, couponSavePO.regNo());
			assertEquals(61L, couponSavePO.udtNo());
			assertNotNull(couponSavePO.cpnUsableStartDt());
			assertNotNull(couponSavePO.cpnUsableEndDt());
			assertTrue(!couponSavePO.cpnUsableStartDt().isBefore(beforeJoin) && !couponSavePO.cpnUsableStartDt().isAfter(afterJoin));
			assertEquals(couponSavePO.cpnUsableStartDt().plusDays(30), couponSavePO.cpnUsableEndDt());
		}
	}

	@Test
	@DisplayName("고객 쿠폰 직접 발급: 고정일시형 쿠폰은 설정된 시작/종료 일시로 1건 지급한다")
	// 고정일시형 다운로드 쿠폰 발급 시 설정된 사용 가능 기간으로 고객 쿠폰이 저장되는지 검증합니다.
	void issueShopCustomerCoupon_issuesFixedDateCouponWithConfiguredWindow() {
		// 고정일시형 쿠폰 규칙을 구성합니다.
		ShopCouponIssueRuleVO fixedDateCouponRule = new ShopCouponIssueRuleVO(
			11L,
			"CPN_STAT_02",
			"CPN_USE_DT_02",
			null,
			LocalDateTime.of(2026, 3, 1, 0, 0, 0),
			LocalDateTime.of(2026, 3, 31, 23, 59, 59)
		);
		when(shopAuthMapper.getIssuableCouponIssueRule(11L)).thenReturn(fixedDateCouponRule);
		when(shopAuthMapper.insertCustomerCoupon(any(ShopCustomerCouponSavePO.class))).thenReturn(1);

		// 고객 쿠폰 1건 발급을 수행합니다.
		int issuedCount = shopAuthService.issueShopCustomerCoupon(71L, 11L, 1);

		// 고정일시형 쿠폰이 지정 기간으로 1건 저장되는지 검증합니다.
		assertEquals(1, issuedCount);
		ArgumentCaptor<ShopCustomerCouponSavePO> couponSaveCaptor = ArgumentCaptor.forClass(ShopCustomerCouponSavePO.class);
		verify(shopAuthMapper).insertCustomerCoupon(couponSaveCaptor.capture());
		assertEquals(71L, couponSaveCaptor.getValue().custNo());
		assertEquals(11L, couponSaveCaptor.getValue().cpnNo());
		assertEquals(LocalDateTime.of(2026, 3, 1, 0, 0, 0), couponSaveCaptor.getValue().cpnUsableStartDt());
		assertEquals(LocalDateTime.of(2026, 3, 31, 23, 59, 59), couponSaveCaptor.getValue().cpnUsableEndDt());
	}

	@Test
	@DisplayName("고객 쿠폰 직접 발급: 기간형 쿠폰은 현재 시점 기준으로 사용 시작/종료 일시를 계산한다")
	// 기간형 다운로드 쿠폰 발급 시 현재 시각 기준 사용 가능 기간 계산이 적용되는지 검증합니다.
	void issueShopCustomerCoupon_issuesPeriodCouponWithNowBasedWindow() {
		// 기간형 쿠폰 규칙을 구성합니다.
		ShopCouponIssueRuleVO periodCouponRule = new ShopCouponIssueRuleVO(12L, "CPN_STAT_02", "CPN_USE_DT_01", 30, null, null);
		when(shopAuthMapper.getIssuableCouponIssueRule(12L)).thenReturn(periodCouponRule);
		when(shopAuthMapper.insertCustomerCoupon(any(ShopCustomerCouponSavePO.class))).thenReturn(1);

		// 발급 직전/직후 시각을 저장해 시작일 검증 범위를 확보합니다.
		LocalDateTime beforeIssue = LocalDateTime.now();
		int issuedCount = shopAuthService.issueShopCustomerCoupon(72L, 12L, 1);
		LocalDateTime afterIssue = LocalDateTime.now();

		// 기간형 쿠폰이 1건 저장되고 현재 시점 기준으로 종료일이 계산되는지 검증합니다.
		assertEquals(1, issuedCount);
		ArgumentCaptor<ShopCustomerCouponSavePO> couponSaveCaptor = ArgumentCaptor.forClass(ShopCustomerCouponSavePO.class);
		verify(shopAuthMapper).insertCustomerCoupon(couponSaveCaptor.capture());
		assertTrue(!couponSaveCaptor.getValue().cpnUsableStartDt().isBefore(beforeIssue));
		assertTrue(!couponSaveCaptor.getValue().cpnUsableStartDt().isAfter(afterIssue));
		assertEquals(couponSaveCaptor.getValue().cpnUsableStartDt().plusDays(30), couponSaveCaptor.getValue().cpnUsableEndDt());
	}

	// 기본 구글 회원가입 요청 객체를 생성합니다.
	private ShopGoogleJoinRequest createDefaultJoinRequest() {
		// 필수값이 포함된 기본 요청 객체를 구성합니다.
		return new ShopGoogleJoinRequest(
			"google-sub",
			"google-user@test.com",
			"홍길동",
			"X",
			"",
			"",
			"N",
			"N",
			"N",
			"Y",
			"Y",
			"WEB"
		);
	}
}
