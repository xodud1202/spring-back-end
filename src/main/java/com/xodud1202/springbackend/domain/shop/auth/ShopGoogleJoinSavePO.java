package com.xodud1202.springbackend.domain.shop.auth;

import lombok.Data;

@Data
// 구글 회원가입 CUSTOMER_BASE 저장 파라미터를 전달합니다.
public class ShopGoogleJoinSavePO {
	// 생성된 고객 번호입니다.
	private Long custNo;
	// 로그인 아이디입니다.
	private String loginId;
	// 비밀번호입니다.
	private String password;
	// 고객명입니다.
	private String custNm;
	// 고객 등급 코드입니다.
	private String custGradeCd;
	// 고객 상태 코드입니다.
	private String custStatCd;
	// 가입 구분입니다.
	private String joinGb;
	// 성별 코드입니다.
	private String sex;
	// 생년월일(YYYYMMDD)입니다.
	private String birth;
	// 휴대폰번호(숫자 11자리)입니다.
	private String phoneNumber;
	// 이메일입니다.
	private String email;
	// SMS 수신 동의 여부(Y/N)입니다.
	private String smsRsvYn;
	// 이메일 수신 동의 여부(Y/N)입니다.
	private String emailRsvYn;
	// 앱 푸시 수신 동의 여부(Y/N)입니다.
	private String appPushRsvYn;
	// 가입 디바이스 코드입니다.
	private String deviceGbCd;
	// CI 값입니다.
	private String ci;
	// DI 값입니다.
	private String di;
	// 등록자 번호입니다.
	private Integer regNo;
	// 수정자 번호입니다.
	private Integer udtNo;
}
