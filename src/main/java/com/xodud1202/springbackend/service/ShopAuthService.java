package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.shop.auth.ShopCustomerSessionVO;
import com.xodud1202.springbackend.domain.shop.auth.ShopGoogleJoinRequest;
import com.xodud1202.springbackend.domain.shop.auth.ShopGoogleJoinSavePO;
import com.xodud1202.springbackend.domain.shop.auth.ShopGoogleLoginRequest;
import com.xodud1202.springbackend.domain.shop.auth.ShopGoogleLoginResponse;
import com.xodud1202.springbackend.mapper.ShopAuthMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 쇼핑몰 고객 로그인 관련 비즈니스 로직을 처리합니다.
@Service
@RequiredArgsConstructor
public class ShopAuthService {
	private static final String DEFAULT_CUST_GRADE_CD = "CUST_GRADE_01";
	private static final String DEFAULT_CUST_STAT_CD = "CUST_STAT_01";
	private static final String GOOGLE_JOIN_GB = "GOOGLE";
	private static final String SEX_UNSELECTED = "X";
	private static final String SEX_MALE = "M";
	private static final String SEX_FEMALE = "F";
	private static final String AGREEMENT_Y = "Y";
	private static final String AGREEMENT_N = "N";
	private static final String DEVICE_TYPE_WEB = "WEB";
	private static final String DEVICE_TYPE_MOBILE = "MOBILE";
	private static final String DEVICE_TYPE_APP = "APP";
	private static final String DEVICE_GB_PC = "PC";
	private static final String DEVICE_GB_MO = "MO";
	private static final String DEVICE_GB_APP = "APP";
	private static final int CUSTOMER_NAME_MAX_LENGTH = 20;
	private static final Pattern BIRTH_PATTERN = Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})$");
	private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile("^\\d{3}-\\d{4}-\\d{4}$");

	private final ShopAuthMapper shopAuthMapper;

	// 구글 로그인 식별값으로 기존 회원 여부를 판정합니다.
	public ShopGoogleLoginResponse loginWithGoogle(ShopGoogleLoginRequest request) {
		// 요청 데이터 유효성을 확인합니다.
		if (request == null || isBlank(request.getSub())) {
			throw new IllegalArgumentException("구글 사용자 식별값을 확인해주세요.");
		}

		// CI에 저장된 구글 sub 기준으로 기존 고객 정보를 조회합니다.
		String normalizedSub = request.getSub().trim();
		ShopCustomerSessionVO customer = shopAuthMapper.getShopCustomerByCi(normalizedSub);

		// 기존 고객이 있으면 즉시 로그인 가능한 응답을 구성합니다.
		if (customer != null && customer.getCustNo() != null) {
			return buildLoginSuccessResponse(customer, buildGoogleLoginId(normalizedSub));
		}

		// 기존 고객이 없으면 추가 정보 입력이 필요한 응답을 반환합니다.
		ShopGoogleLoginResponse response = new ShopGoogleLoginResponse();
		response.setLoginSuccess(false);
		response.setJoinRequired(true);
		response.setLoginId(buildGoogleLoginId(normalizedSub));
		return response;
	}

	// 구글 신규 회원가입을 처리하고 로그인 응답을 반환합니다.
	@Transactional
	public ShopGoogleLoginResponse joinWithGoogle(ShopGoogleJoinRequest request) {
		// 요청 데이터 유효성을 먼저 검증합니다.
		validateJoinRequest(request);

		// 요청값을 저장 전 규칙에 맞게 정규화합니다.
		String normalizedSub = request.getSub().trim();
		String normalizedEmail = request.getEmail().trim();
		String normalizedCustNm = normalizeCustomerName(request.getCustNm());
		String normalizedSex = normalizeSex(request.getSex());
		String normalizedBirth = normalizeBirthForSave(request.getBirth());
		String normalizedPhoneNumber = normalizePhoneNumberForSave(request.getPhoneNumber());
		String normalizedSmsRsvYn = normalizeYn(request.getSmsRsvYn());
		String normalizedEmailRsvYn = normalizeYn(request.getEmailRsvYn());
		String normalizedAppPushRsvYn = normalizeYn(request.getAppPushRsvYn());
		String resolvedDeviceGbCd = resolveDeviceGbCd(request.getDeviceType());

		// 동일 CI 기존 회원이 있으면 중복 등록 없이 즉시 로그인 응답을 반환합니다.
		ShopCustomerSessionVO existingCustomer = shopAuthMapper.getShopCustomerByCi(normalizedSub);
		if (existingCustomer != null && existingCustomer.getCustNo() != null) {
			return buildLoginSuccessResponse(existingCustomer, buildGoogleLoginId(normalizedSub));
		}

		// 신규 회원 등록 파라미터를 구성합니다.
		ShopGoogleJoinSavePO savePO = new ShopGoogleJoinSavePO();
		savePO.setLoginId(buildGoogleLoginId(normalizedSub));
		savePO.setPassword(null);
		savePO.setCustNm(normalizedCustNm);
		savePO.setCustGradeCd(DEFAULT_CUST_GRADE_CD);
		savePO.setCustStatCd(DEFAULT_CUST_STAT_CD);
		savePO.setJoinGb(GOOGLE_JOIN_GB);
		savePO.setSex(normalizedSex);
		savePO.setBirth(normalizedBirth);
		savePO.setPhoneNumber(normalizedPhoneNumber);
		savePO.setEmail(normalizedEmail);
		savePO.setSmsRsvYn(normalizedSmsRsvYn);
		savePO.setEmailRsvYn(normalizedEmailRsvYn);
		savePO.setAppPushRsvYn(normalizedAppPushRsvYn);
		savePO.setDeviceGbCd(resolvedDeviceGbCd);
		savePO.setCi(normalizedSub);
		savePO.setDi(normalizedSub);
		savePO.setRegNo(0);
		savePO.setUdtNo(0);

		// CUSTOMER_BASE 신규 회원을 등록합니다.
		shopAuthMapper.insertShopGoogleCustomer(savePO);
		if (savePO.getCustNo() == null) {
			throw new IllegalArgumentException("회원가입 처리 중 고객번호 생성에 실패했습니다.");
		}

		// REG_NO/UDT_NO를 가입자 고객번호로 갱신합니다.
		shopAuthMapper.updateShopGoogleCustomerAuditNo(savePO.getCustNo(), savePO.getCustNo());

		// 등록된 회원 정보를 다시 조회해 로그인 응답을 구성합니다.
		ShopCustomerSessionVO joinedCustomer = shopAuthMapper.getShopCustomerByCi(normalizedSub);
		if (joinedCustomer == null || joinedCustomer.getCustNo() == null) {
			joinedCustomer = new ShopCustomerSessionVO();
			joinedCustomer.setCustNo(savePO.getCustNo());
			joinedCustomer.setCustNm(savePO.getCustNm());
			joinedCustomer.setCustGradeCd(savePO.getCustGradeCd());
		}
		return buildLoginSuccessResponse(joinedCustomer, savePO.getLoginId());
	}

	// 회원가입 요청의 필수값/약관 동의 여부를 확인합니다.
	private void validateJoinRequest(ShopGoogleJoinRequest request) {
		// 필수 식별값을 확인합니다.
		if (request == null || isBlank(request.getSub())) {
			throw new IllegalArgumentException("구글 사용자 식별값을 확인해주세요.");
		}

		// 필수 이메일을 확인합니다.
		if (isBlank(request.getEmail())) {
			throw new IllegalArgumentException("이메일을 확인해주세요.");
		}

		// 필수 고객명을 확인합니다.
		if (isBlank(request.getCustNm())) {
			throw new IllegalArgumentException("고객명을 입력해주세요.");
		}

		// 필수 약관 동의 여부를 확인합니다.
		if (!AGREEMENT_Y.equals(normalizeYn(request.getPrivateAgreeYn()))) {
			throw new IllegalArgumentException("개인정보 처리 방침 동의는 필수입니다.");
		}
		if (!AGREEMENT_Y.equals(normalizeYn(request.getTermsAgreeYn()))) {
			throw new IllegalArgumentException("서비스 이용약관 동의는 필수입니다.");
		}
	}

	// 로그인 성공 응답을 공통으로 구성합니다.
	private ShopGoogleLoginResponse buildLoginSuccessResponse(ShopCustomerSessionVO customer, String loginId) {
		// 로그인 성공 상태를 응답 객체에 채웁니다.
		ShopGoogleLoginResponse response = new ShopGoogleLoginResponse();
		response.setLoginSuccess(true);
		response.setJoinRequired(false);
		response.setCustNo(customer.getCustNo());
		response.setCustNm(customer.getCustNm());
		response.setCustGradeCd(customer.getCustGradeCd());
		response.setLoginId(loginId);
		return response;
	}

	// 구글 사용자 식별값으로 내부 로그인 아이디를 생성합니다.
	private String buildGoogleLoginId(String sub) {
		// sub 값이 없으면 빈 문자열을 반환합니다.
		if (isBlank(sub)) {
			return "";
		}
		return "google_" + sub.trim();
	}

	// 고객명을 저장 가능한 값으로 정규화합니다.
	private String normalizeCustomerName(String custNm) {
		// 공백을 제거한 뒤 길이 제한을 검증합니다.
		String normalizedCustNm = custNm.trim();
		if (normalizedCustNm.length() > CUSTOMER_NAME_MAX_LENGTH) {
			throw new IllegalArgumentException("고객명은 20자 이내로 입력해주세요.");
		}
		return normalizedCustNm;
	}

	// 성별 코드를 저장 규칙(X/M/F)에 맞게 정규화합니다.
	private String normalizeSex(String sex) {
		// 미입력은 미선택 코드(X)로 처리합니다.
		if (isBlank(sex)) {
			return SEX_UNSELECTED;
		}

		// 허용된 성별 코드만 통과시킵니다.
		String normalizedSex = sex.trim().toUpperCase();
		if (SEX_UNSELECTED.equals(normalizedSex) || SEX_MALE.equals(normalizedSex) || SEX_FEMALE.equals(normalizedSex)) {
			return normalizedSex;
		}
		throw new IllegalArgumentException("성별 값이 올바르지 않습니다.");
	}

	// 생년월일 입력값을 저장 포맷(YYYYMMDD)으로 정규화합니다.
	private String normalizeBirthForSave(String birth) {
		// 미입력은 null로 저장합니다.
		if (isBlank(birth)) {
			return null;
		}

		// YYYY-MM-DD 형식인지 정규식으로 확인합니다.
		Matcher birthMatcher = BIRTH_PATTERN.matcher(birth.trim());
		if (!birthMatcher.matches()) {
			throw new IllegalArgumentException("생년월일은 YYYY-MM-DD 형식으로 입력해주세요.");
		}

		// 날짜 유효성과 현재 날짜 초과 여부를 검증합니다.
		int year = Integer.parseInt(birthMatcher.group(1));
		int month = Integer.parseInt(birthMatcher.group(2));
		int day = Integer.parseInt(birthMatcher.group(3));
		LocalDate birthDate;
		try {
			birthDate = LocalDate.of(year, month, day);
		} catch (DateTimeException exception) {
			throw new IllegalArgumentException("생년월일이 올바르지 않습니다.");
		}
		if (birthDate.isAfter(LocalDate.now())) {
			throw new IllegalArgumentException("생년월일은 현재 날짜를 초과할 수 없습니다.");
		}

		// DB 저장 포맷(YYYYMMDD)으로 변환합니다.
		return birthDate.format(DateTimeFormatter.BASIC_ISO_DATE);
	}

	// 휴대폰번호 입력값을 저장 포맷(숫자 11자리)으로 정규화합니다.
	private String normalizePhoneNumberForSave(String phoneNumber) {
		// 미입력은 null로 저장합니다.
		if (isBlank(phoneNumber)) {
			return null;
		}

		// 010-0000-0000 형식인지 검증합니다.
		String normalizedPhoneNumber = phoneNumber.trim();
		if (!PHONE_NUMBER_PATTERN.matcher(normalizedPhoneNumber).matches()) {
			throw new IllegalArgumentException("휴대폰번호는 010-0000-0000 형식으로 입력해주세요.");
		}

		// 하이픈을 제거한 숫자 문자열로 저장합니다.
		return normalizedPhoneNumber.replace("-", "");
	}

	// Y/N 입력값을 저장 규칙으로 정규화합니다.
	private String normalizeYn(String value) {
		// 미입력은 N으로 처리합니다.
		if (isBlank(value)) {
			return AGREEMENT_N;
		}

		// 대문자 기준 Y/N만 허용합니다.
		String normalizedValue = value.trim().toUpperCase();
		if (AGREEMENT_Y.equals(normalizedValue) || AGREEMENT_N.equals(normalizedValue)) {
			return normalizedValue;
		}
		throw new IllegalArgumentException("수신 동의 값이 올바르지 않습니다.");
	}

	// 회원가입 디바이스 타입을 DEVICE_GB_CD 코드로 매핑합니다.
	private String resolveDeviceGbCd(String deviceType) {
		// 디바이스 타입 필수값을 확인합니다.
		if (isBlank(deviceType)) {
			throw new IllegalArgumentException("가입 디바이스 정보를 확인해주세요.");
		}

		// WEB/MOBILE/APP 값을 공통코드 값으로 변환합니다.
		String normalizedDeviceType = deviceType.trim().toUpperCase();
		if (DEVICE_TYPE_WEB.equals(normalizedDeviceType)) {
			return DEVICE_GB_PC;
		}
		if (DEVICE_TYPE_MOBILE.equals(normalizedDeviceType)) {
			return DEVICE_GB_MO;
		}
		if (DEVICE_TYPE_APP.equals(normalizedDeviceType)) {
			return DEVICE_GB_APP;
		}
		throw new IllegalArgumentException("가입 디바이스 값이 올바르지 않습니다.");
	}

	// 문자열 공백 여부를 확인합니다.
	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
}
