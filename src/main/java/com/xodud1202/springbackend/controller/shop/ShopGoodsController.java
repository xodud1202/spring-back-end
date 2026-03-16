package com.xodud1202.springbackend.controller.shop;

import com.xodud1202.springbackend.domain.shop.cart.ShopCartDeletePO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartCouponEstimateRequestPO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartCouponEstimateVO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartOptionUpdatePO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartPageVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsDetailVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageCouponDownloadRequestPO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageCouponPageVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageWishPageVO;
import com.xodud1202.springbackend.service.GoodsService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
			log.info("check goodsId: {}", goodsId);
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

	// 쇼핑몰 상품 위시리스트를 토글(등록/삭제)합니다.
	@PostMapping("/api/shop/goods/wishlist/toggle")
	public ResponseEntity<Object> toggleShopGoodsWishlist(
		@RequestBody(required = false) Map<String, Object> requestBody,
		HttpServletRequest request
	) {
		try {
			// 필수 파라미터(goodsId) 유효성을 확인합니다.
			Object goodsIdValue = requestBody == null ? null : requestBody.get("goodsId");
			String goodsId = goodsIdValue instanceof String ? ((String) goodsIdValue).trim() : "";
			if (goodsId.isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of("message", "상품코드를 확인해주세요."));
			}

			// 로그인 고객번호가 없으면 401 응답을 반환합니다.
			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}

			// 위시리스트 토글 처리 후 최종 상태를 반환합니다.
			boolean wished = goodsService.toggleShopGoodsWishlist(goodsId, custNo);
			return ResponseEntity.ok(Map.of("wished", wished));
		} catch (IllegalArgumentException exception) {
			// 조회 가능한 상품이 없으면 404 응답을 반환합니다.
			if ("상품 정보를 찾을 수 없습니다.".equals(exception.getMessage())) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
			}
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			// 기타 예외는 500 응답과 함께 에러 로그를 반환합니다.
			log.error("쇼핑몰 위시리스트 토글 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "위시리스트 처리에 실패했습니다."));
		}
	}

	// 쇼핑몰 마이페이지 위시리스트 페이지 데이터를 조회합니다.
	@GetMapping("/api/shop/mypage/wish/page")
	public ResponseEntity<Object> getShopMypageWishPage(
		@RequestParam(value = "pageNo", required = false) Integer pageNo,
		HttpServletRequest request
	) {
		try {
			// 로그인 고객번호가 없으면 401 응답을 반환합니다.
			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}

			// 위시리스트 페이지 데이터를 조회해 반환합니다.
			ShopMypageWishPageVO result = goodsService.getShopMypageWishPage(custNo, pageNo);
			return ResponseEntity.ok(result);
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			// 기타 예외는 500 응답과 함께 에러 로그를 반환합니다.
			log.error("쇼핑몰 마이페이지 위시리스트 조회 실패 message={} pageNo={}", exception.getMessage(), pageNo, exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "위시리스트 조회에 실패했습니다."));
		}
	}

	// 쇼핑몰 마이페이지 위시리스트 상품을 삭제합니다.
	@PostMapping("/api/shop/mypage/wish/delete")
	public ResponseEntity<Object> deleteShopMypageWish(
		@RequestBody(required = false) Map<String, Object> requestBody,
		HttpServletRequest request
	) {
		try {
			// 필수 파라미터(goodsId) 유효성을 확인합니다.
			Object goodsIdValue = requestBody == null ? null : requestBody.get("goodsId");
			String goodsId = goodsIdValue instanceof String ? ((String) goodsIdValue).trim() : "";
			if (goodsId.isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of("message", "상품코드를 확인해주세요."));
			}

			// 로그인 고객번호가 없으면 401 응답을 반환합니다.
			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}

			// 위시리스트 상품을 삭제합니다.
			goodsService.deleteShopMypageWishGoods(goodsId, custNo);
			return ResponseEntity.ok(Map.of("message", "위시리스트에서 삭제했습니다."));
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			// 기타 예외는 500 응답과 함께 에러 로그를 반환합니다.
			log.error("쇼핑몰 마이페이지 위시리스트 삭제 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "위시리스트 삭제에 실패했습니다."));
		}
	}

	// 쇼핑몰 마이페이지 쿠폰함 페이지 데이터를 조회합니다.
	@GetMapping("/api/shop/mypage/coupon/page")
	public ResponseEntity<Object> getShopMypageCouponPage(
		@RequestParam(required = false) Integer ownedPageNo,
		@RequestParam(required = false) Integer downloadablePageNo,
		HttpServletRequest request
	) {
		try {
			// 로그인 고객번호가 없으면 401 응답을 반환합니다.
			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}

			// 쿠폰함 페이지 데이터를 조회해 반환합니다.
			ShopMypageCouponPageVO result = goodsService.getShopMypageCouponPage(custNo, ownedPageNo, downloadablePageNo);
			return ResponseEntity.ok(result);
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			// 기타 예외는 500 응답과 함께 에러 로그를 반환합니다.
			log.error("쇼핑몰 마이페이지 쿠폰함 조회 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "쿠폰함 조회에 실패했습니다."));
		}
	}

	// 쇼핑몰 마이페이지에서 쿠폰 1건을 다운로드합니다.
	@PostMapping("/api/shop/mypage/coupon/download")
	public ResponseEntity<Object> downloadShopMypageCoupon(
		@RequestBody(required = false) ShopMypageCouponDownloadRequestPO requestBody,
		HttpServletRequest request
	) {
		try {
			// 로그인 고객번호가 없으면 401 응답을 반환합니다.
			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}

			// 쿠폰 1건 다운로드를 수행합니다.
			goodsService.downloadShopMypageCoupon(requestBody, custNo);
			return ResponseEntity.ok(Map.of("message", "쿠폰을 다운로드했습니다."));
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			// 기타 예외는 500 응답과 함께 에러 로그를 반환합니다.
			log.error("쇼핑몰 마이페이지 쿠폰 다운로드 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "쿠폰 다운로드에 실패했습니다."));
		}
	}

	// 쇼핑몰 마이페이지에서 현재 다운로드 가능한 쿠폰을 전체 다운로드합니다.
	@PostMapping("/api/shop/mypage/coupon/download/all")
	public ResponseEntity<Object> downloadAllShopMypageCoupon(HttpServletRequest request) {
		try {
			// 로그인 고객번호가 없으면 401 응답을 반환합니다.
			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}

			// 현재 다운로드 가능한 쿠폰 전체 다운로드를 수행합니다.
			int downloadedCount = goodsService.downloadAllShopMypageCoupon(custNo);
			String message = downloadedCount > 0 ? "전체 쿠폰을 다운로드했습니다." : "다운로드 가능한 쿠폰이 없습니다.";
			return ResponseEntity.ok(Map.of("downloadedCount", downloadedCount, "message", message));
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			// 기타 예외는 500 응답과 함께 에러 로그를 반환합니다.
			log.error("쇼핑몰 마이페이지 전체 쿠폰 다운로드 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "전체 쿠폰 다운로드에 실패했습니다."));
		}
	}

	// 쇼핑몰 상품 장바구니를 등록(기존 건은 수량 가산)합니다.
	@PostMapping("/api/shop/goods/cart/add")
	public ResponseEntity<Object> addShopGoodsCart(
		@RequestBody(required = false) Map<String, Object> requestBody,
		HttpServletRequest request
	) {
		try {
			// 필수 파라미터(goodsId/sizeId/qty) 유효성을 확인합니다.
			Object goodsIdValue = requestBody == null ? null : requestBody.get("goodsId");
			String goodsId = goodsIdValue instanceof String ? ((String) goodsIdValue).trim() : "";
			if (goodsId.isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of("message", "상품코드를 확인해주세요."));
			}

			Object sizeIdValue = requestBody.get("sizeId");
			String sizeId = sizeIdValue instanceof String ? ((String) sizeIdValue).trim() : "";
			if (sizeId.isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of("message", "사이즈를 선택해주세요."));
			}

			Integer qty = parseIntegerValue(requestBody.get("qty"));
			if (qty == null || qty < 1) {
				return ResponseEntity.badRequest().body(Map.of("message", "수량을 확인해주세요."));
			}

			// 로그인 고객번호가 없으면 401 응답을 반환합니다.
			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}

			// 장바구니 등록 처리 후 최종 수량을 반환합니다.
			int cartQty = goodsService.addShopGoodsCart(goodsId, sizeId, qty, custNo);
			return ResponseEntity.ok(Map.of("qty", cartQty, "message", "장바구니에 담았습니다."));
		} catch (IllegalArgumentException exception) {
			// 조회 가능한 상품이 없으면 404 응답을 반환합니다.
			if ("상품 정보를 찾을 수 없습니다.".equals(exception.getMessage())) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
			}
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			// 기타 예외는 500 응답과 함께 에러 로그를 반환합니다.
			log.error("쇼핑몰 장바구니 등록 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "장바구니 처리에 실패했습니다."));
		}
	}

	// 쇼핑몰 장바구니 페이지 데이터를 조회합니다.
	@GetMapping("/api/shop/cart/page")
	public ResponseEntity<Object> getShopCartPage(HttpServletRequest request) {
		try {
			// 로그인 고객번호가 없으면 401 응답을 반환합니다.
			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}

			// 장바구니 페이지 데이터를 조회해 반환합니다.
			ShopCartPageVO result = goodsService.getShopCartPage(custNo);
			return ResponseEntity.ok(result);
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			// 기타 예외는 500 응답과 함께 에러 로그를 반환합니다.
			log.error("쇼핑몰 장바구니 페이지 조회 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "장바구니 조회에 실패했습니다."));
		}
	}

	// 쇼핑몰 장바구니 선택 상품 기준 예상 최대 쿠폰 할인 금액을 계산합니다.
	@PostMapping("/api/shop/cart/coupon/estimate")
	public ResponseEntity<Object> estimateShopCartCoupon(
		@RequestBody(required = false) ShopCartCouponEstimateRequestPO requestBody,
		HttpServletRequest request
	) {
		try {
			// 로그인 고객번호가 없으면 401 응답을 반환합니다.
			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}

			// 장바구니 선택 상품 기준 예상 최대 쿠폰 할인 금액을 계산합니다.
			ShopCartCouponEstimateVO result = goodsService.getShopCartCouponEstimate(requestBody, custNo);
			return ResponseEntity.ok(result);
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			// 기타 예외는 500 응답과 함께 에러 로그를 반환합니다.
			log.error("쇼핑몰 장바구니 쿠폰 예상 할인 계산 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "장바구니 예상 할인 계산에 실패했습니다."));
		}
	}

	// 쇼핑몰 장바구니 옵션(사이즈/수량)을 변경합니다.
	@PostMapping("/api/shop/cart/option/update")
	public ResponseEntity<Object> updateShopCartOption(
		@RequestBody(required = false) ShopCartOptionUpdatePO requestBody,
		HttpServletRequest request
	) {
		try {
			// 로그인 고객번호가 없으면 401 응답을 반환합니다.
			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}

			// 장바구니 옵션(사이즈/수량) 변경을 수행합니다.
			goodsService.updateShopCartOption(requestBody, custNo);
			return ResponseEntity.ok(Map.of("message", "장바구니 옵션을 변경했습니다."));
		} catch (IllegalArgumentException exception) {
			// 장바구니 대상 미존재는 404 응답으로 반환합니다.
			if ("변경할 장바구니 상품을 찾을 수 없습니다.".equals(exception.getMessage())) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
			}
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			// 기타 예외는 500 응답과 함께 에러 로그를 반환합니다.
			log.error("쇼핑몰 장바구니 옵션 변경 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "장바구니 옵션 변경에 실패했습니다."));
		}
	}

	// 쇼핑몰 장바구니 선택 상품을 삭제합니다.
	@PostMapping("/api/shop/cart/delete")
	public ResponseEntity<Object> deleteShopCartItems(
		@RequestBody(required = false) ShopCartDeletePO requestBody,
		HttpServletRequest request
	) {
		try {
			// 로그인 고객번호가 없으면 401 응답을 반환합니다.
			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}

			// 선택된 장바구니 상품을 삭제합니다.
			int deletedCount = goodsService.deleteShopCartItems(requestBody, custNo);
			return ResponseEntity.ok(Map.of("deletedCount", deletedCount, "message", "선택한 장바구니 상품을 삭제했습니다."));
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			// 기타 예외는 500 응답과 함께 에러 로그를 반환합니다.
			log.error("쇼핑몰 장바구니 선택 삭제 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "장바구니 선택 삭제에 실패했습니다."));
		}
	}

	// 쇼핑몰 장바구니 전체 상품을 삭제합니다.
	@PostMapping("/api/shop/cart/delete/all")
	public ResponseEntity<Object> deleteShopCartAll(HttpServletRequest request) {
		try {
			// 로그인 고객번호가 없으면 401 응답을 반환합니다.
			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}

			// 장바구니 전체 상품을 삭제합니다.
			int deletedCount = goodsService.deleteShopCartAll(custNo);
			return ResponseEntity.ok(Map.of("deletedCount", deletedCount, "message", "장바구니 상품을 전체 삭제했습니다."));
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			// 기타 예외는 500 응답과 함께 에러 로그를 반환합니다.
			log.error("쇼핑몰 장바구니 전체 삭제 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "장바구니 전체 삭제에 실패했습니다."));
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

	// 숫자 또는 문자열 값을 정수로 변환합니다.
	private Integer parseIntegerValue(Object rawValue) {
		// 값이 없으면 null을 반환합니다.
		switch (rawValue) {
			case null -> {
				return null;
			}

			// 숫자 타입은 int 값으로 변환합니다.
			case Number numberValue -> {
				return numberValue.intValue();
			}

			// 문자열 타입은 공백 제거 후 정수 변환을 시도합니다.
			case String stringValue -> {
				String normalizedValue = stringValue.trim();
				if (normalizedValue.isEmpty()) {
					return null;
				}
				try {
					return Integer.valueOf(normalizedValue);
				} catch (NumberFormatException exception) {
					return null;
				}
			}
			default -> {
			}
		}
		// 변환 불가능한 타입이면 null을 반환합니다.
		return null;
	}

	// URL 인코딩된 쿠키 값을 원문 문자열로 디코딩합니다.
	private String decodeCookieValue(String value) {
		if (value == null) {
			return null;
		}
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}
}
