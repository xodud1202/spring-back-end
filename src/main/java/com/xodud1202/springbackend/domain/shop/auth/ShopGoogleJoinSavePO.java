package com.xodud1202.springbackend.domain.shop.auth;

// 구글 회원가입 CUSTOMER_BASE 저장 명령을 전달합니다.
public record ShopGoogleJoinSavePO(
	String loginId,
	String password,
	String custNm,
	String custGradeCd,
	String custStatCd,
	String joinGb,
	String sex,
	String birth,
	String phoneNumber,
	String email,
	String smsRsvYn,
	String emailRsvYn,
	String appPushRsvYn,
	String deviceGbCd,
	String ci,
	String di,
	Integer regNo,
	Integer udtNo
) {
}
