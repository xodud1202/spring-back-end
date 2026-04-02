package com.xodud1202.springbackend.controller.shop;

import com.xodud1202.springbackend.domain.shop.order.ShopOrderAddressRegisterPO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderAddressUpdatePO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderDiscountQuotePO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderPageVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderPaymentConfirmPO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderPaymentFailPO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderPaymentPreparePO;
import com.xodud1202.springbackend.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
// 쇼핑몰 주문서 및 결제 API를 제공합니다.
public class ShopOrderController extends ShopControllerSupport {
	private final OrderService orderService;

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
			ShopOrderPageVO result = orderService.getShopOrderPage(
				cartIdList,
				custNo,
				resolveDeviceGbCd(request),
				resolveShopOrigin(request)
			);
			return ResponseEntity.ok(result);
		} catch (IllegalArgumentException exception) {
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			log.error("쇼핑몰 주문서 페이지 조회 실패 message={} cartIdList={}", exception.getMessage(), cartIdList, exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "주문서 조회에 실패했습니다."));
		}
	}

	// 쇼핑몰 주문서 배송지 검색 결과를 조회합니다.
	@GetMapping("/api/shop/order/address/search")
	public ResponseEntity<Object> searchShopOrderAddress(
		@RequestParam(value = "keyword", required = false) String keyword,
		@RequestParam(value = "currentPage", required = false) Integer currentPage,
		@RequestParam(value = "countPerPage", required = false) Integer countPerPage,
		HttpServletRequest request
	) {
		try {
			// 로그인 고객번호가 없으면 401 응답을 반환합니다.
			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}

			// 배송지 검색 결과를 조회해 반환합니다.
			return ResponseEntity.ok(orderService.searchShopOrderAddress(keyword, currentPage, countPerPage, custNo));
		} catch (IllegalArgumentException exception) {
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			log.error("쇼핑몰 배송지 검색 실패 message={} keyword={}", exception.getMessage(), keyword, exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "배송지 검색에 실패했습니다."));
		}
	}

	// 쇼핑몰 주문서 배송지를 등록합니다.
	@PostMapping("/api/shop/order/address")
	public ResponseEntity<Object> registerShopOrderAddress(
		@RequestBody(required = false) ShopOrderAddressRegisterPO param,
		HttpServletRequest request
	) {
		try {
			// 로그인 고객번호가 없으면 401 응답을 반환합니다.
			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}

			// 배송지 등록 결과를 조회해 반환합니다.
			return ResponseEntity.ok(orderService.registerShopOrderAddress(param, custNo));
		} catch (IllegalArgumentException exception) {
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			log.error("쇼핑몰 배송지 등록 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "배송지 등록에 실패했습니다."));
		}
	}

	// 쇼핑몰 주문서 배송지를 수정합니다.
	@PutMapping("/api/shop/order/address")
	public ResponseEntity<Object> updateShopOrderAddress(
		@RequestBody(required = false) ShopOrderAddressUpdatePO param,
		HttpServletRequest request
	) {
		try {
			// 로그인 고객번호가 없으면 401 응답을 반환합니다.
			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}

			// 배송지 수정 결과를 조회해 반환합니다.
			return ResponseEntity.ok(orderService.updateShopOrderAddress(param, custNo));
		} catch (IllegalArgumentException exception) {
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			log.error("쇼핑몰 배송지 수정 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "배송지 수정에 실패했습니다."));
		}
	}

	// 쇼핑몰 주문서 할인 금액을 재계산합니다.
	@PostMapping("/api/shop/order/discount/quote")
	public ResponseEntity<Object> quoteShopOrderDiscount(
		@RequestBody(required = false) ShopOrderDiscountQuotePO param,
		HttpServletRequest request
	) {
		try {
			// 로그인 고객번호가 없으면 401 응답을 반환합니다.
			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}

			// 할인 재계산 결과를 조회해 반환합니다.
			return ResponseEntity.ok(orderService.quoteShopOrderDiscount(param, custNo));
		} catch (IllegalArgumentException exception) {
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			log.error("쇼핑몰 주문서 할인 재계산 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "할인 혜택 계산에 실패했습니다."));
		}
	}

	// 쇼핑몰 주문 결제 준비 정보를 생성합니다.
	@PostMapping("/api/shop/order/payment/prepare")
	public ResponseEntity<Object> prepareShopOrderPayment(
		@RequestBody(required = false) ShopOrderPaymentPreparePO param,
		HttpServletRequest request
	) {
		try {
			// 로그인 고객번호가 없으면 401 응답을 반환합니다.
			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}

			// 주문 결제 준비 결과를 생성해 반환합니다.
			return ResponseEntity.ok(
				orderService.prepareShopOrderPayment(
					param,
					custNo,
					resolveDeviceGbCd(request),
					resolveShopOrigin(request)
				)
			);
		} catch (IllegalArgumentException exception) {
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			log.error("쇼핑몰 주문 결제 준비 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "주문 결제 준비에 실패했습니다."));
		}
	}

	// 쇼핑몰 주문 결제를 승인합니다.
	@PostMapping("/api/shop/order/payment/confirm")
	public ResponseEntity<Object> confirmShopOrderPayment(
		@RequestBody(required = false) ShopOrderPaymentConfirmPO param,
		HttpServletRequest request
	) {
		try {
			// 로그인 고객번호가 없으면 401 응답을 반환합니다.
			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}

			// 결제 승인 결과를 반환합니다.
			return ResponseEntity.ok(orderService.confirmShopOrderPayment(param, custNo));
		} catch (IllegalArgumentException exception) {
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			log.error("쇼핑몰 주문 결제 승인 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "결제 승인 처리에 실패했습니다."));
		}
	}

	// 쇼핑몰 주문 결제 실패/취소 결과를 반영합니다.
	@PostMapping("/api/shop/order/payment/fail")
	public ResponseEntity<Object> failShopOrderPayment(
		@RequestBody(required = false) ShopOrderPaymentFailPO param,
		HttpServletRequest request
	) {
		try {
			// 로그인 고객번호가 없으면 401 응답을 반환합니다.
			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}

			// 결제 실패/취소 결과를 저장한 뒤 성공 응답을 반환합니다.
			orderService.failShopOrderPayment(param, custNo);
			return ResponseEntity.ok(Map.of("success", true));
		} catch (IllegalArgumentException exception) {
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			log.error("쇼핑몰 주문 결제 실패 반영 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "결제 실패 반영에 실패했습니다."));
		}
	}

	// Toss 웹훅을 반영합니다.
	@PostMapping("/api/shop/order/payment/webhook")
	public ResponseEntity<Object> handleShopOrderPaymentWebhook(@RequestBody(required = false) String rawBody) {
		try {
			// 웹훅 본문을 서비스에 전달해 상태를 반영합니다.
			orderService.handleShopOrderPaymentWebhook(rawBody);
			return ResponseEntity.ok(Map.of("success", true));
		} catch (Exception exception) {
			log.error("쇼핑몰 주문 결제 웹훅 반영 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "웹훅 처리에 실패했습니다."));
		}
	}
}
