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

	// 장바구니 공통 요청 파라미터를 보관합니다.
	private record ShopCartCommand(String goodsId, String sizeId, Integer qty, Integer exhibitionNo, Long custNo) {
	}

	// 장바구니 등록/바로구매 공통 요청 파라미터를 해석합니다.
	private ShopCartCommand requireShopCartCommand(Map<String, Object> requestBody, HttpServletRequest request) {
		String goodsId = requireRequestBodyTextValue(requestBody, "goodsId", "상품코드를 확인해주세요.");
		String sizeId = requireRequestBodyTextValue(requestBody, "sizeId", "사이즈를 선택해주세요.");
		Integer qty = requirePositiveIntegerValue(requestBody == null ? null : requestBody.get("qty"), "수량을 확인해주세요.");
		Integer exhibitionNo = normalizeOptionalExhibitionNo(requestBody == null ? null : requestBody.get("exhibitionNo"));
		Long custNo = requireAuthenticatedCustNo(request);
		return new ShopCartCommand(goodsId, sizeId, qty, exhibitionNo, custNo);
	}

	// 쇼핑몰 상품 장바구니를 등록(기존 건은 수량 가산)합니다.
	@PostMapping("/api/shop/goods/cart/add")
	public ResponseEntity<Object> addShopGoodsCart(
		@RequestBody(required = false) Map<String, Object> requestBody,
		HttpServletRequest request
	) {
		try {
			// 필수 파라미터(goodsId/sizeId/qty)와 로그인 고객번호를 확인합니다.
			ShopCartCommand command = requireShopCartCommand(requestBody, request);

			// 장바구니 등록 처리 후 최종 수량을 반환합니다.
			int cartQty = cartService.addShopGoodsCart(
				command.goodsId(),
				command.sizeId(),
				command.qty(),
				command.custNo(),
				command.exhibitionNo()
			);
			return ResponseEntity.ok(Map.of("qty", cartQty, "message", "장바구니에 담았습니다."));
		} catch (SecurityException exception) {
			return unauthorizedResponse();
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
			// 필수 파라미터(goodsId/sizeId/qty)와 로그인 고객번호를 확인합니다.
			ShopCartCommand command = requireShopCartCommand(requestBody, request);

			// 바로구매 장바구니 등록 처리 후 생성된 장바구니 번호를 반환합니다.
			Long cartId = cartService.addShopGoodsOrderNowCart(
				command.goodsId(),
				command.sizeId(),
				command.qty(),
				command.custNo(),
				command.exhibitionNo()
			);
			return ResponseEntity.ok(Map.of("cartId", cartId, "goodsId", command.goodsId(), "message", "바로구매 정보를 등록했습니다."));
		} catch (SecurityException exception) {
			return unauthorizedResponse();
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
			// 로그인 고객번호를 확인합니다.
			Long custNo = requireAuthenticatedCustNo(request);

			// 장바구니 페이지 데이터를 조회해 반환합니다.
			ShopCartPageVO result = cartService.getShopCartPage(custNo);
			return ResponseEntity.ok(result);
		} catch (SecurityException exception) {
			return unauthorizedResponse();
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
			// 로그인 고객번호를 확인합니다.
			Long custNo = requireAuthenticatedCustNo(request);

			// 장바구니 선택 상품 기준 예상 최대 쿠폰 할인 금액을 계산합니다.
			ShopCartCouponEstimateVO result = cartService.getShopCartCouponEstimate(requestBody, custNo);
			return ResponseEntity.ok(result);
		} catch (SecurityException exception) {
			return unauthorizedResponse();
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
			// 로그인 고객번호를 확인합니다.
			Long custNo = requireAuthenticatedCustNo(request);

			// 장바구니 옵션(사이즈/수량) 변경을 수행합니다.
			cartService.updateShopCartOption(requestBody, custNo);
			return ResponseEntity.ok(Map.of("message", "장바구니 옵션을 변경했습니다."));
		} catch (SecurityException exception) {
			return unauthorizedResponse();
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
			// 로그인 고객번호를 확인합니다.
			Long custNo = requireAuthenticatedCustNo(request);

			// 선택된 장바구니 상품을 삭제합니다.
			int deletedCount = cartService.deleteShopCartItems(requestBody, custNo);
			return ResponseEntity.ok(Map.of("deletedCount", deletedCount, "message", "선택한 장바구니 상품을 삭제했습니다."));
		} catch (SecurityException exception) {
			return unauthorizedResponse();
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
			// 로그인 고객번호를 확인합니다.
			Long custNo = requireAuthenticatedCustNo(request);

			// 장바구니 전체 상품을 삭제합니다.
			int deletedCount = cartService.deleteShopCartAll(custNo);
			return ResponseEntity.ok(Map.of("deletedCount", deletedCount, "message", "장바구니 상품을 전체 삭제했습니다."));
		} catch (SecurityException exception) {
			return unauthorizedResponse();
		} catch (IllegalArgumentException exception) {
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			log.error("쇼핑몰 장바구니 전체 삭제 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "장바구니 전체 삭제에 실패했습니다."));
		}
	}
}
