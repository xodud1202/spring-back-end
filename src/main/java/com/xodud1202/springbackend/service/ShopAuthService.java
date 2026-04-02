package com.xodud1202.springbackend.service;

import static com.xodud1202.springbackend.common.Constants.Shop.CPN_USE_DT_DATETIME;
import static com.xodud1202.springbackend.common.Constants.Shop.CPN_USE_DT_PERIOD;
import static com.xodud1202.springbackend.common.Constants.Shop.AGREEMENT_N;
import static com.xodud1202.springbackend.common.Constants.Shop.AGREEMENT_Y;
import static com.xodud1202.springbackend.common.Constants.Shop.CUST_GRADE_GRP_CD;
import static com.xodud1202.springbackend.common.Constants.Shop.DEFAULT_CUST_GRADE_CD;
import static com.xodud1202.springbackend.common.Constants.Shop.DEFAULT_CUST_STAT_CD;
import static com.xodud1202.springbackend.common.Constants.Shop.DEVICE_GB_APP;
import static com.xodud1202.springbackend.common.Constants.Shop.DEVICE_GB_MO;
import static com.xodud1202.springbackend.common.Constants.Shop.DEVICE_GB_PC;
import static com.xodud1202.springbackend.common.Constants.Shop.DEVICE_TYPE_APP;
import static com.xodud1202.springbackend.common.Constants.Shop.DEVICE_TYPE_MOBILE;
import static com.xodud1202.springbackend.common.Constants.Shop.DEVICE_TYPE_WEB;
import static com.xodud1202.springbackend.common.Constants.Shop.GOOGLE_JOIN_GB;
import static com.xodud1202.springbackend.common.Constants.Shop.JOIN_POINT_GIVE_GB_CD;
import static com.xodud1202.springbackend.common.Constants.Shop.JOIN_POINT_GIVE_MEMO;
import static com.xodud1202.springbackend.common.Constants.Shop.SEX_FEMALE;
import static com.xodud1202.springbackend.common.Constants.Shop.SEX_MALE;
import static com.xodud1202.springbackend.common.Constants.Shop.SEX_UNSELECTED;
import static com.xodud1202.springbackend.common.Constants.Shop.SHOP_SITE_ID;

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
import com.xodud1202.springbackend.domain.shop.site.ShopSiteInfoVO;
import com.xodud1202.springbackend.mapper.ShopAuthMapper;
import com.xodud1202.springbackend.mapper.SiteInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
// 쇼핑몰 고객 로그인 관련 비즈니스 로직을 처리합니다.
public class ShopAuthService {
	private static final int CUSTOMER_NAME_MAX_LENGTH = 20;
	private static final long JOIN_POINT_EXPIRE_MONTHS = 1L;
	private static final Pattern BIRTH_PATTERN = Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})$");
	private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile("^\\d{3}-\\d{4}-\\d{4}$");

	private final ShopAuthMapper shopAuthMapper;
	private final SiteInfoMapper siteInfoMapper;

	// 공통코드 코드값으로 코드명을 조회합니다.
	public String getCommonCodeName(String grpCd, String cd) {
		// 그룹코드 또는 코드값이 비어 있으면 빈 문자열을 반환합니다.
		if (isBlank(grpCd) || isBlank(cd)) {
			return "";
		}

		// 공통코드명을 조회하고 미조회 시 코드값을 대체 반환합니다.
		String normalizedGrpCd = grpCd.trim();
		String normalizedCd = cd.trim();
		String codeName = shopAuthMapper.getCommonCodeName(normalizedGrpCd, normalizedCd);
		if (isBlank(codeName)) {
			return normalizedCd;
		}
		return codeName.trim();
	}

	// 고객 등급코드에 해당하는 등급명을 조회합니다.
	public String getCustomerGradeName(String custGradeCd) {
		// 등급코드가 비어 있으면 빈 문자열을 반환합니다.
		if (isBlank(custGradeCd)) {
			return "";
		}

		// 고객 등급 공통코드명을 조회합니다.
		return getCommonCodeName(CUST_GRADE_GRP_CD, custGradeCd);
	}

	// 구글 로그인 식별값으로 기존 회원 여부를 판정합니다.
	public ShopGoogleLoginResponse loginWithGoogle(ShopGoogleLoginRequest request) {
		// 요청 데이터 유효성을 확인합니다.
		String normalizedSub = normalizeRequiredText(request.sub(), "구글 사용자 식별값을 확인해주세요.");

		// CI에 저장된 구글 sub 기준으로 기존 고객 정보를 조회합니다.
		ShopCustomerSessionVO customer = shopAuthMapper.getShopCustomerByCi(normalizedSub);

		// 기존 고객이 있으면 즉시 로그인 가능한 응답을 구성합니다.
		if (customer != null && customer.custNo() != null) {
			return buildLoginSuccessResponse(customer, buildGoogleLoginId(normalizedSub));
		}

		// 기존 고객이 없으면 추가 정보 입력이 필요한 응답을 반환합니다.
		return ShopGoogleLoginResponse.joinRequired(buildGoogleLoginId(normalizedSub));
	}

	@Transactional
	// 구글 신규 회원가입을 처리하고 로그인 응답을 반환합니다.
	public ShopGoogleLoginResponse joinWithGoogle(ShopGoogleJoinRequest request) {
		// 요청 데이터 유효성을 먼저 검증합니다.
		validateJoinRequest(request);

		// 요청값을 저장 전 규칙에 맞게 정규화합니다.
		String normalizedSub = request.sub().trim();
		String normalizedEmail = request.email().trim();
		String normalizedCustNm = normalizeCustomerName(request.custNm());
		String normalizedSex = normalizeSex(request.sex());
		String normalizedBirth = normalizeBirthForSave(request.birth());
		String normalizedPhoneNumber = normalizePhoneNumberForSave(request.phoneNumber());
		String normalizedSmsRsvYn = normalizeYn(request.smsRsvYn());
		String normalizedEmailRsvYn = normalizeYn(request.emailRsvYn());
		String normalizedAppPushRsvYn = normalizeYn(request.appPushRsvYn());
		String resolvedDeviceGbCd = resolveDeviceGbCd(request.deviceType());

		// 동일 CI 기존 회원이 있으면 중복 등록 없이 즉시 로그인 응답을 반환합니다.
		ShopCustomerSessionVO existingCustomer = shopAuthMapper.getShopCustomerByCi(normalizedSub);
		if (existingCustomer != null && existingCustomer.custNo() != null) {
			return buildLoginSuccessResponse(existingCustomer, buildGoogleLoginId(normalizedSub));
		}

		// 신규 회원 등록 명령을 구성합니다.
		ShopGoogleJoinSavePO saveCommand = new ShopGoogleJoinSavePO(
			buildGoogleLoginId(normalizedSub),
			null,
			normalizedCustNm,
			DEFAULT_CUST_GRADE_CD,
			DEFAULT_CUST_STAT_CD,
			GOOGLE_JOIN_GB,
			normalizedSex,
			normalizedBirth,
			normalizedPhoneNumber,
			normalizedEmail,
			normalizedSmsRsvYn,
			normalizedEmailRsvYn,
			normalizedAppPushRsvYn,
			resolvedDeviceGbCd,
			normalizedSub,
			normalizedSub,
			0,
			0
		);

		// CUSTOMER_BASE 신규 회원을 등록하고 생성키를 수집합니다.
		GeneratedLongKey customerGeneratedKey = new GeneratedLongKey();
		shopAuthMapper.insertShopGoogleCustomer(saveCommand, customerGeneratedKey);
		Long createdCustNo = customerGeneratedKey.getValue();
		if (createdCustNo == null) {
			throw new IllegalArgumentException("회원가입 처리 중 고객번호 생성에 실패했습니다.");
		}

		// REG_NO/UDT_NO를 가입자 고객번호로 갱신합니다.
		shopAuthMapper.updateShopGoogleCustomerAuditNo(createdCustNo, createdCustNo);

		// 회원가입 기본 포인트/등급 쿠폰 혜택을 지급합니다.
		grantJoinBenefits(createdCustNo, saveCommand.custGradeCd());

		// 등록된 회원 정보를 다시 조회해 로그인 응답을 구성합니다.
		ShopCustomerSessionVO joinedCustomer = shopAuthMapper.getShopCustomerByCi(normalizedSub);
		if (joinedCustomer == null || joinedCustomer.custNo() == null) {
			joinedCustomer = new ShopCustomerSessionVO(
				createdCustNo,
				saveCommand.loginId(),
				saveCommand.custNm(),
				saveCommand.custGradeCd(),
				saveCommand.ci(),
				saveCommand.email()
			);
		}
		return buildLoginSuccessResponse(joinedCustomer, saveCommand.loginId());
	}

	// 회원가입 완료 후 포인트/쿠폰 혜택을 지급합니다.
	private void grantJoinBenefits(Long custNo, String custGradeCd) {
		// 가입 포인트가 설정되어 있으면 고객 포인트를 지급합니다.
		grantJoinPointIfNeeded(custNo);

		// 고객등급별 혜택 쿠폰이 설정되어 있으면 고객 쿠폰을 지급합니다.
		grantJoinCouponsByGrade(custNo, custGradeCd);
	}

	// 사이트 기본 가입 포인트를 고객에게 지급합니다.
	private void grantJoinPointIfNeeded(Long custNo) {
		// 사이트 기본 회원가입 포인트를 조회합니다.
		Integer joinPoint = resolveShopJoinPoint();
		if (joinPoint == null || joinPoint <= 0) {
			return;
		}

		// 회원가입 포인트 만료일시를 가입 시점 기준 1개월 후로 계산합니다.
		LocalDateTime joinPointExpireDt = resolveJoinPointExpireDateTime();

		// 포인트 마스터 지급 이력을 등록합니다.
		ShopCustomerPointSavePO pointSaveCommand = new ShopCustomerPointSavePO(
			custNo,
			JOIN_POINT_GIVE_GB_CD,
			JOIN_POINT_GIVE_MEMO,
			joinPoint,
			joinPointExpireDt,
			custNo,
			custNo
		);
		GeneratedLongKey pointGeneratedKey = new GeneratedLongKey();
		shopAuthMapper.insertCustomerPointBase(pointSaveCommand, pointGeneratedKey);

		// 생성된 포인트 번호가 없으면 처리 실패로 간주합니다.
		Long createdPointNo = pointGeneratedKey.getValue();
		if (createdPointNo == null) {
			throw new IllegalArgumentException("회원가입 포인트 지급 처리에 실패했습니다.");
		}

		// 포인트 상세 이력을 등록합니다.
		ShopCustomerPointDetailSavePO pointDetailCommand = new ShopCustomerPointDetailSavePO(
			createdPointNo,
			joinPoint,
			JOIN_POINT_GIVE_MEMO,
			custNo
		);
		shopAuthMapper.insertCustomerPointDetail(pointDetailCommand);
	}

	// 사이트 설정의 회원가입 포인트 값을 조회합니다.
	private Integer resolveShopJoinPoint() {
		// 사이트 통합 정보에서 회원가입 포인트 값을 읽고 null 안전하게 반환합니다.
		ShopSiteInfoVO siteInfo = siteInfoMapper.getShopSiteInfo(SHOP_SITE_ID);
		return siteInfo == null ? null : siteInfo.getJoinPoint();
	}

	// 회원가입 포인트 만료 일시를 계산합니다.
	private LocalDateTime resolveJoinPointExpireDateTime() {
		// 가입 시점 기준 1개월 후 만료되도록 초 단위 기준으로 계산합니다.
		return LocalDateTime.now().withNano(0).plusMonths(JOIN_POINT_EXPIRE_MONTHS);
	}

	// 고객등급별 혜택 쿠폰을 고객에게 지급합니다.
	private void grantJoinCouponsByGrade(Long custNo, String custGradeCd) {
		// 고객등급별 혜택 정보를 조회합니다.
		ShopCustomerGradeBenefitVO benefitVO = shopAuthMapper.getCustomerGradeBenefitByCustGradeCd(custGradeCd);
		if (benefitVO == null) {
			return;
		}

		// 혜택 쿠폰 번호/수량이 설정된 항목을 고객에게 발급합니다.
		issueShopCustomerCoupon(custNo, benefitVO.goodsCpnNo(), benefitVO.goodsCpnCnt());
		issueShopCustomerCoupon(custNo, benefitVO.cartCpnNo(), benefitVO.cartCpnCnt());
		issueShopCustomerCoupon(custNo, benefitVO.deliveryCpnNo(), benefitVO.deliveryCpnCnt());
	}

	@Transactional
	// 단일 쿠폰을 지정 수량만큼 고객에게 지급하고 실제 지급 건수를 반환합니다.
	public int issueShopCustomerCoupon(Long custNo, Long cpnNo, Integer issueCount) {
		// 쿠폰번호/수량이 유효하지 않으면 지급을 생략합니다.
		if (custNo == null || custNo < 1L || cpnNo == null || issueCount == null || issueCount < 1) {
			return 0;
		}

		// 정상 상태(CPN_STAT_02) 쿠폰만 발급 규칙을 조회합니다.
		ShopCouponIssueRuleVO couponRule = shopAuthMapper.getIssuableCouponIssueRule(cpnNo);
		if (couponRule == null) {
			return 0;
		}

		// 쿠폰 사용 가능 시작/종료 일시를 계산합니다.
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime usableStartDt = resolveCouponUsableStartDateTime(couponRule, now);
		LocalDateTime usableEndDt = resolveCouponUsableEndDateTime(couponRule, now);
		if (usableStartDt == null || usableEndDt == null || usableStartDt.isAfter(usableEndDt)) {
			return 0;
		}

		// 요청 수량만큼 고객 쿠폰을 반복 지급합니다.
		int issuedCount = 0;
		for (int issueIndex = 0; issueIndex < issueCount; issueIndex += 1) {
			ShopCustomerCouponSavePO couponSaveCommand = new ShopCustomerCouponSavePO(
				custNo,
				cpnNo,
				usableStartDt,
				usableEndDt,
				custNo,
				custNo
			);
			issuedCount += shopAuthMapper.insertCustomerCoupon(couponSaveCommand);
		}
		return issuedCount;
	}

	// 쿠폰 사용 가능 시작 일시를 계산합니다.
	private LocalDateTime resolveCouponUsableStartDateTime(ShopCouponIssueRuleVO couponRule, LocalDateTime now) {
		// 기간형 쿠폰은 발급 시점을 시작일시로 사용합니다.
		if (CPN_USE_DT_PERIOD.equals(couponRule.cpnUseDtGb())) {
			return now;
		}

		// 고정일시형 쿠폰은 쿠폰 기본 시작일시를 사용합니다.
		if (CPN_USE_DT_DATETIME.equals(couponRule.cpnUseDtGb())) {
			return couponRule.cpnUseStartDt();
		}
		return null;
	}

	// 쿠폰 사용 가능 종료 일시를 계산합니다.
	private LocalDateTime resolveCouponUsableEndDateTime(ShopCouponIssueRuleVO couponRule, LocalDateTime now) {
		// 기간형 쿠폰은 사용 가능 일수 기준으로 종료일시를 계산합니다.
		if (CPN_USE_DT_PERIOD.equals(couponRule.cpnUseDtGb())) {
			if (couponRule.cpnUsableDt() == null || couponRule.cpnUsableDt() < 1) {
				return null;
			}
			return now.plusDays(couponRule.cpnUsableDt());
		}

		// 고정일시형 쿠폰은 쿠폰 기본 종료일시를 사용합니다.
		if (CPN_USE_DT_DATETIME.equals(couponRule.cpnUseDtGb())) {
			return couponRule.cpnUseEndDt();
		}
		return null;
	}

	// 회원가입 요청의 필수값/약관 동의 여부를 확인합니다.
	private void validateJoinRequest(ShopGoogleJoinRequest request) {
		// 필수 식별값을 확인합니다.
		normalizeRequiredText(request.sub(), "구글 사용자 식별값을 확인해주세요.");

		// 필수 이메일을 확인합니다.
		normalizeRequiredText(request.email(), "이메일을 확인해주세요.");

		// 필수 고객명을 확인합니다.
		normalizeRequiredText(request.custNm(), "고객명을 입력해주세요.");

		// 필수 약관 동의 여부를 확인합니다.
		if (!AGREEMENT_Y.equals(normalizeYn(request.privateAgreeYn()))) {
			throw new IllegalArgumentException("개인정보 처리 방침 동의는 필수입니다.");
		}
		if (!AGREEMENT_Y.equals(normalizeYn(request.termsAgreeYn()))) {
			throw new IllegalArgumentException("서비스 이용약관 동의는 필수입니다.");
		}
	}

	// 로그인 성공 응답을 공통으로 구성합니다.
	private ShopGoogleLoginResponse buildLoginSuccessResponse(ShopCustomerSessionVO customer, String loginId) {
		// 로그인 성공 상태를 응답 객체에 채웁니다.
		return ShopGoogleLoginResponse.loginSuccess(customer.custNo(), customer.custNm(), customer.custGradeCd(), loginId);
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

	// 필수 문자열을 trim 처리하고 비어 있으면 예외를 반환합니다.
	private String normalizeRequiredText(String value, String errorMessage) {
		String normalizedValue = trimToNull(value);
		if (normalizedValue == null) {
			throw new IllegalArgumentException(errorMessage);
		}
		return normalizedValue;
	}

	// 문자열을 trim 처리하고 비어 있으면 null을 반환합니다.
	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String normalizedValue = value.trim();
		return normalizedValue.isEmpty() ? null : normalizedValue;
	}

	// 문자열 공백 여부를 확인합니다.
	private boolean isBlank(String value) {
		return trimToNull(value) == null;
	}
}
