package com.xodud1202.springbackend.domain.shop.auth;

import jakarta.validation.constraints.NotBlank;

// 구글 신규 회원가입 요청 정보를 전달합니다.
public record ShopGoogleJoinRequest(
	@NotBlank(message = "구글 사용자 식별값을 확인해주세요.") String sub,
	@NotBlank(message = "이메일을 확인해주세요.") String email,
	@NotBlank(message = "고객명을 입력해주세요.") String custNm,
	String sex,
	String birth,
	String phoneNumber,
	String smsRsvYn,
	String emailRsvYn,
	String appPushRsvYn,
	String privateAgreeYn,
	String termsAgreeYn,
	String deviceType
) {
}
