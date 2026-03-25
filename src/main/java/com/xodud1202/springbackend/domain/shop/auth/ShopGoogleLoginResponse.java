package com.xodud1202.springbackend.domain.shop.auth;

// 구글 로그인 판정 결과를 전달합니다.
public record ShopGoogleLoginResponse(
	boolean loginSuccess,
	boolean joinRequired,
	Long custNo,
	String custNm,
	String custGradeCd,
	String loginId,
	String message
) {
	// 로그인 성공 응답을 생성합니다.
	public static ShopGoogleLoginResponse loginSuccess(Long custNo, String custNm, String custGradeCd, String loginId) {
		return new ShopGoogleLoginResponse(true, false, custNo, custNm, custGradeCd, loginId, null);
	}

	// 추가 정보 입력이 필요한 응답을 생성합니다.
	public static ShopGoogleLoginResponse joinRequired(String loginId) {
		return new ShopGoogleLoginResponse(false, true, null, null, null, loginId, null);
	}
}
