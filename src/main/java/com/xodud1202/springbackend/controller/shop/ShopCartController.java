package com.xodud1202.springbackend.controller.shop;

import com.xodud1202.springbackend.domain.shop.cart.ShopCartCouponEstimateRequestPO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartCouponEstimateVO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartDeletePO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartOptionUpdatePO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartPageVO;
import com.xodud1202.springbackend.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
// 쇼핑몰 장바구니 API를 제공합니다.
public class ShopCartController extends ShopControllerSupport {
	private final CartService cartService;

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

			Object sizeIdValue = requestBody == null ? null : requestBody.get("sizeId");
			String sizeId = sizeIdValue instanceof String ? ((String) sizeIdValue).trim() : "";
			if (sizeId.isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of("message", "사이즈를 선택해주세요."));
			}

			Integer qty = parseIntegerValue(requestBody == null ? null : requestBody.get("qty"));
			if (qty == null || qty < 1) {
				return ResponseEntity.badRequest().body(Map.of("message", "수량을 확인해주세요."));
			}
			Integer exhibitionNo = normalizeOptionalExhibitionNo(requestBody == null ? null : requestBody.get("exhibitionNo"));

			// 로그인 고객번호가 없으면 401 응답을 반환합니다.
			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}

			// 장바구니 등록 처리 후 최종 수량을 반환합니다.
			int cartQty = cartService.addShopGoodsCart(goodsId, sizeId, qty, custNo, exhibitionNo);
			return ResponseEntity.ok(Map.of("qty", cartQty, "message", "장바구니에 담았습니다."));
		} catch (IllegalArgumentException exception) {
			// 조회 가능한 상품이 없으면 404 응답을 반환합니다.
			if ("상품 정보를 찾을 수 없습니다.".equals(exception.getMessage())) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
			}
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			log.error("쇼핑몰 장바구니 등록 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "장바구니 처리에 실패했습니다."));
		}
	}

	// 쇼핑몰 상품 바로구매용 장바구니를 등록합니다.
	@PostMapping("/api/shop/goods/order-now")
	public ResponseEntity<Object> addShopGoodsOrderNow(
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

			Object sizeIdValue = requestBody == null ? null : requestBody.get("sizeId");
			String sizeId = sizeIdValue instanceof String ? ((String) sizeIdValue).trim() : "";
			if (sizeId.isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of("message", "사이즈를 선택해주세요."));
			}

			Integer qty = parseIntegerValue(requestBody == null ? null : requestBody.get("qty"));
			if (qty == null || qty < 1) {
				return ResponseEntity.badRequest().body(Map.of("message", "수량을 확인해주세요."));
			}
			Integer exhibitionNo = normalizeOptionalExhibitionNo(requestBody == null ? null : requestBody.get("exhibitionNo"));

			// 로그인 고객번호가 없으면 401 응답을 반환합니다.
			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}

			// 바로구매 장바구니 등록 처리 후 생성된 장바구니 번호를 반환합니다.
			Long cartId = cartService.addShopGoodsOrderNowCart(goodsId, sizeId, qty, custNo, exhibitionNo);
			return ResponseEntity.ok(Map.of("cartId", cartId, "goodsId", goodsId, "message", "바로구매 정보를 등록했습니다."));
		} catch (IllegalArgumentException exception) {
			// 조회 가능한 상품이 없으면 404 응답으로 반환합니다.
			if ("상품 정보를 찾을 수 없습니다.".equals(exception.getMessage())) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
			}
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			log.error("쇼핑몰 바로구매 장바구니 등록 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "바로구매 처리에 실패했습니다."));
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
			ShopCartPageVO result = cartService.getShopCartPage(custNo);
			return ResponseEntity.ok(result);
		} catch (IllegalArgumentException exception) {
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
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
			ShopCartCouponEstimateVO result = cartService.getShopCartCouponEstimate(requestBody, custNo);
			return ResponseEntity.ok(result);
		} catch (IllegalArgumentException exception) {
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
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
			cartService.updateShopCartOption(requestBody, custNo);
			return ResponseEntity.ok(Map.of("message", "장바구니 옵션을 변경했습니다."));
		} catch (IllegalArgumentException exception) {
			// 장바구니 대상 미존재는 404 응답으로 반환합니다.
			if ("변경할 장바구니 상품을 찾을 수 없습니다.".equals(exception.getMessage())) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
			}
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
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
			int deletedCount = cartService.deleteShopCartItems(requestBody, custNo);
			return ResponseEntity.ok(Map.of("deletedCount", deletedCount, "message", "선택한 장바구니 상품을 삭제했습니다."));
		} catch (IllegalArgumentException exception) {
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
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
			int deletedCount = cartService.deleteShopCartAll(custNo);
			return ResponseEntity.ok(Map.of("deletedCount", deletedCount, "message", "장바구니 상품을 전체 삭제했습니다."));
		} catch (IllegalArgumentException exception) {
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			log.error("쇼핑몰 장바구니 전체 삭제 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "장바구니 전체 삭제에 실패했습니다."));
		}
	}
}
