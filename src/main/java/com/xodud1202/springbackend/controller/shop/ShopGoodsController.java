package com.xodud1202.springbackend.controller.shop;

import com.xodud1202.springbackend.domain.shop.auth.ShopCustomerSessionVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsCouponDownloadRequestPO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsDetailVO;
import com.xodud1202.springbackend.service.GoodsService;
import com.xodud1202.springbackend.service.ShopAuthService;
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

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
// 쇼핑몰 상품상세 API를 제공합니다.
public class ShopGoodsController extends ShopControllerSupport {
	private final GoodsService goodsService;
	private final ShopAuthService shopAuthService;

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

			// 현재 로그인 세션 기준 고객번호/등급코드를 읽어 상세 조회에 전달합니다.
			Long custNo = resolveAuthenticatedCustNo(request);
			ShopCustomerSessionVO customer = shopAuthService.getShopCustomerByCustNo(custNo);
			String custGradeCd = customer == null ? null : customer.custGradeCd();
			ShopGoodsDetailVO detail = goodsService.getShopGoodsDetail(goodsId, custNo, custGradeCd);
			if (detail == null || detail.getGoods() == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "상품 정보를 찾을 수 없습니다."));
			}
			return ResponseEntity.ok(detail);
		} catch (IllegalArgumentException exception) {
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			log.error("쇼핑몰 상품상세 조회 실패 message={} goodsId={}", exception.getMessage(), goodsId, exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "상품상세 조회에 실패했습니다."));
		}
	}

	// 쇼핑몰 상품상세에서 현재 상품에 다운로드 가능한 상품쿠폰 1건을 다운로드합니다.
	@PostMapping("/api/shop/goods/coupon/download")
	public ResponseEntity<Object> downloadShopGoodsCoupon(
		@RequestBody(required = false) ShopGoodsCouponDownloadRequestPO requestBody,
		HttpServletRequest request
	) {
		try {
			// 로그인 고객번호를 확인합니다.
			Long custNo = requireAuthenticatedCustNo(request);

			// 상품상세 기준 쿠폰 다운로드를 수행합니다.
			String goodsId = requestBody == null ? null : requestBody.getGoodsId();
			Long cpnNo = requestBody == null ? null : requestBody.getCpnNo();
			goodsService.downloadShopGoodsCoupon(goodsId, cpnNo, custNo);
			return ResponseEntity.ok(Map.of("message", "쿠폰을 다운로드했습니다."));
		} catch (SecurityException exception) {
			return unauthorizedResponse();
		} catch (IllegalArgumentException exception) {
			if ("상품 정보를 찾을 수 없습니다.".equals(exception.getMessage())) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
			}
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			log.error("쇼핑몰 상품상세 쿠폰 다운로드 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "쿠폰 다운로드에 실패했습니다."));
		}
	}

	// 쇼핑몰 상품 위시리스트를 토글(등록/삭제)합니다.
	@PostMapping("/api/shop/goods/wishlist/toggle")
	public ResponseEntity<Object> toggleShopGoodsWishlist(
		@RequestBody(required = false) Map<String, Object> requestBody,
		HttpServletRequest request
	) {
		try {
			// 필수 파라미터(goodsId)와 로그인 고객번호를 확인합니다.
			String goodsId = requireRequestBodyTextValue(requestBody, "goodsId", "상품코드를 확인해주세요.");

			// 로그인 고객번호를 확인합니다.
			Long custNo = requireAuthenticatedCustNo(request);

			// 위시리스트 토글 처리 후 최종 상태를 반환합니다.
			boolean wished = goodsService.toggleShopGoodsWishlist(goodsId, custNo);
			return ResponseEntity.ok(Map.of("wished", wished));
		} catch (SecurityException exception) {
			return unauthorizedResponse();
		} catch (IllegalArgumentException exception) {
			if ("상품 정보를 찾을 수 없습니다.".equals(exception.getMessage())) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
			}
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			log.error("쇼핑몰 위시리스트 토글 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "위시리스트 처리에 실패했습니다."));
		}
	}
}
