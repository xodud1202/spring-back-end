package com.xodud1202.springbackend.controller.shop;

import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsDetailVO;
import com.xodud1202.springbackend.service.GoodsService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
// 쇼핑몰 상품상세 조회 API를 제공합니다.
public class ShopGoodsController {
	private static final String COOKIE_CUST_NO = "cust_no";
	private static final String COOKIE_CUST_GRADE_CD = "cust_grade_cd";

	private final GoodsService goodsService;

	// 쇼핑몰 상품상세 상단 화면 데이터를 조회합니다.
	@GetMapping("/api/shop/goods/detail")
	public ResponseEntity<Object> getShopGoodsDetail(
		@RequestParam(value = "goodsId", required = false) String goodsId,
		HttpServletRequest request
	) {
		try {
			// 필수 파라미터(goodsId) 유효성을 확인합니다.
			if (goodsId == null || goodsId.trim().isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of("message", "상품코드를 확인해주세요."));
			}

			// 로그인 쿠키에서 고객번호/등급코드를 읽어 상세 조회에 전달합니다.
			Long custNo = parseCustNoCookie(request);
			String custGradeCd = findCookieValue(request, COOKIE_CUST_GRADE_CD);
			ShopGoodsDetailVO detail = goodsService.getShopGoodsDetail(goodsId, custNo, decodeCookieValue(custGradeCd));
			if (detail == null || detail.getGoods() == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "상품 정보를 찾을 수 없습니다."));
			}
			return ResponseEntity.ok(detail);
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			// 기타 예외는 500 응답과 함께 에러 로그를 반환합니다.
			log.error("쇼핑몰 상품상세 조회 실패 message={} goodsId={}", exception.getMessage(), goodsId, exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "상품상세 조회에 실패했습니다."));
		}
	}

	// 요청 쿠키에서 고객번호를 파싱해 반환합니다.
	private Long parseCustNoCookie(HttpServletRequest request) {
		// 고객번호 쿠키 값이 없으면 null을 반환합니다.
		String custNoValue = findCookieValue(request, COOKIE_CUST_NO);
		if (custNoValue == null || custNoValue.trim().isEmpty()) {
			return null;
		}

		// 숫자 형식이 아니면 로그인 정보가 없는 것으로 처리합니다.
		try {
			return Long.valueOf(custNoValue.trim());
		} catch (NumberFormatException exception) {
			return null;
		}
	}

	// 요청 쿠키에서 지정한 이름의 값을 조회합니다.
	private String findCookieValue(HttpServletRequest request, String cookieName) {
		if (request.getCookies() == null || cookieName == null) {
			return null;
		}
		return Arrays.stream(request.getCookies())
			.filter(cookie -> cookieName.equals(cookie.getName()))
			.findFirst()
			.map(Cookie::getValue)
			.orElse(null);
	}

	// URL 인코딩된 쿠키 값을 원문 문자열로 디코딩합니다.
	private String decodeCookieValue(String value) {
		if (value == null) {
			return null;
		}
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}
}
