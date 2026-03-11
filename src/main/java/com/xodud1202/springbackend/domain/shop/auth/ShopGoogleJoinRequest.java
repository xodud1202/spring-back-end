package com.xodud1202.springbackend.domain.shop.auth;

import lombok.Data;

@Data
// 구글 신규 회원가입 요청 정보를 전달합니다.
public class ShopGoogleJoinRequest {
	// 구글 사용자 고유 식별자입니다.
	private String sub;
	// 구글 이메일입니다.
	private String email;
	// 고객명입니다.
	private String custNm;
	// 성별 코드(X/M/F)입니다.
	private String sex;
	// 생년월일(YYYY-MM-DD)입니다.
	private String birth;
	// 휴대폰번호(010-0000-0000)입니다.
	private String phoneNumber;
	// SMS 수신 동의 여부(Y/N)입니다.
	private String smsRsvYn;
	// 이메일 수신 동의 여부(Y/N)입니다.
	private String emailRsvYn;
	// 앱 푸시 수신 동의 여부(Y/N)입니다.
	private String appPushRsvYn;
	// 개인정보 처리방침 동의 여부(Y/N)입니다.
	private String privateAgreeYn;
	// 서비스 이용약관 동의 여부(Y/N)입니다.
	private String termsAgreeYn;
	// 가입 디바이스 타입(WEB/MOBILE/APP)입니다.
	private String deviceType;
}
