package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.shop.auth.ShopCustomerSessionVO;
import com.xodud1202.springbackend.domain.shop.auth.ShopGoogleLoginRequest;
import com.xodud1202.springbackend.domain.shop.auth.ShopGoogleLoginResponse;
import com.xodud1202.springbackend.mapper.ShopAuthMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// 쇼핑몰 고객 로그인 관련 비즈니스 로직을 처리합니다.
@Service
@RequiredArgsConstructor
public class ShopAuthService {
	private final ShopAuthMapper shopAuthMapper;

	// 구글 로그인 식별값으로 기존 회원 여부를 판정합니다.
	public ShopGoogleLoginResponse loginWithGoogle(ShopGoogleLoginRequest request) {
		// 요청 데이터 유효성을 확인합니다.
		if (request == null || isBlank(request.getSub())) {
			throw new IllegalArgumentException("구글 사용자 식별값을 확인해주세요.");
		}

		// CI에 저장된 구글 sub 기준으로 기존 고객 정보를 조회합니다.
		ShopCustomerSessionVO customer = shopAuthMapper.getShopCustomerByCi(request.getSub().trim());
		ShopGoogleLoginResponse response = new ShopGoogleLoginResponse();
		response.setLoginId(buildGoogleLoginId(request.getSub()));

		// 기존 고객이 있으면 즉시 로그인 가능한 응답을 구성합니다.
		if (customer != null && customer.getCustNo() != null) {
			response.setLoginSuccess(true);
			response.setJoinRequired(false);
			response.setCustNo(customer.getCustNo());
			response.setCustNm(customer.getCustNm());
			response.setCustGradeCd(customer.getCustGradeCd());
			return response;
		}

		// 기존 고객이 없으면 추가 정보 입력이 필요한 응답을 반환합니다.
		response.setLoginSuccess(false);
		response.setJoinRequired(true);
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

	// 문자열 공백 여부를 확인합니다.
	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
}
