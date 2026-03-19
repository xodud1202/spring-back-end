package com.xodud1202.springbackend.controller.shop;

import com.xodud1202.springbackend.domain.shop.cart.ShopCartPageVO;
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
// 쇼핑몰 주문서 조회 API를 제공합니다.
public class ShopOrderController {
	private static final String COOKIE_CUST_NO = "cust_no";

	private final GoodsService goodsService;

	// 쇼핑몰 주문서 페이지 데이터를 cartId 기준으로 조회합니다.
	@GetMapping("/api/shop/order/page")
	public ResponseEntity<Object> getShopOrderPage(
		@RequestParam(value = "cartId", required = false) List<Long> cartIdList,
		HttpServletRequest request
	) {
		try {
			// 로그인 고객번호가 없으면 401 응답을 반환합니다.
			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}

			// 주문서 페이지 데이터를 조회해 반환합니다.
			ShopCartPageVO result = goodsService.getShopOrderPage(cartIdList, custNo);
			return ResponseEntity.ok(result);
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			// 기타 예외는 500 응답과 함께 에러 로그를 반환합니다.
			log.error("쇼핑몰 주문서 페이지 조회 실패 message={} cartIdList={}", exception.getMessage(), cartIdList, exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "주문서 조회에 실패했습니다."));
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
}
