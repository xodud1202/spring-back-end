package com.xodud1202.springbackend.controller.shop;

import static com.xodud1202.springbackend.common.Constants.Shop.*;

import com.xodud1202.springbackend.domain.shop.order.ShopOrderAddressRegisterPO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderCancelPO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderAddressUpdatePO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderDiscountQuotePO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderPaymentConfirmPO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderPaymentFailPO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderPaymentPreparePO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderPageVO;
import com.xodud1202.springbackend.service.GoodsService;
import jakarta.servlet.http.Cookie;
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
// 쇼핑몰 주문서 조회 및 배송지 API를 제공합니다.
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
			ShopOrderPageVO result = goodsService.getShopOrderPage(
				cartIdList,
				custNo,
				resolveDeviceGbCd(request),
				resolveShopOrigin(request)
			);
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
			return ResponseEntity.ok(goodsService.searchShopOrderAddress(keyword, currentPage, countPerPage, custNo));
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			// 기타 예외는 500 응답과 함께 에러 로그를 반환합니다.
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
			return ResponseEntity.ok(goodsService.registerShopOrderAddress(param, custNo));
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			// 기타 예외는 500 응답과 함께 에러 로그를 반환합니다.
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
			return ResponseEntity.ok(goodsService.updateShopOrderAddress(param, custNo));
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			// 기타 예외는 500 응답과 함께 에러 로그를 반환합니다.
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
			return ResponseEntity.ok(goodsService.quoteShopOrderDiscount(param, custNo));
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			// 기타 예외는 500 응답과 함께 에러 로그를 반환합니다.
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
				goodsService.prepareShopOrderPayment(
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
			return ResponseEntity.ok(goodsService.confirmShopOrderPayment(param, custNo));
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
			goodsService.failShopOrderPayment(param, custNo);
			return ResponseEntity.ok(Map.of("success", true));
		} catch (IllegalArgumentException exception) {
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			log.error("쇼핑몰 주문 결제 실패 반영 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "결제 실패 반영에 실패했습니다."));
		}
	}

	// 쇼핑몰 마이페이지 주문취소를 즉시 완료 처리합니다.
	@PostMapping("/api/shop/mypage/order/cancel")
	public ResponseEntity<Object> cancelShopMypageOrder(
		@RequestBody(required = false) ShopOrderCancelPO param,
		HttpServletRequest request
	) {
		try {
			// 로그인 고객번호가 없으면 401 응답을 반환합니다.
			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}

			// 주문취소 즉시완료 결과를 반환합니다.
			return ResponseEntity.ok(goodsService.cancelShopMypageOrder(param, custNo));
		} catch (IllegalArgumentException exception) {
			// 환불 금액 불일치는 409 응답으로 반환합니다.
			if (SHOP_MYPAGE_ORDER_CANCEL_AMOUNT_MISMATCH_MESSAGE.equals(exception.getMessage())) {
				return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", exception.getMessage()));
			}

			// 주문 미존재는 404 응답으로 반환합니다.
			if (SHOP_MYPAGE_ORDER_NOT_FOUND_MESSAGE.equals(exception.getMessage())) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
			}

			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			// 기타 예외는 500 응답과 함께 에러 로그를 반환합니다.
			log.error("쇼핑몰 마이페이지 주문취소 처리 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "주문취소 처리에 실패했습니다."));
		}
	}

	// Toss 웹훅을 반영합니다.
	@PostMapping("/api/shop/order/payment/webhook")
	public ResponseEntity<Object> handleShopOrderPaymentWebhook(@RequestBody(required = false) String rawBody) {
		try {
			log.info("toss dept in /api/shop/order/payment/webhook ");
			// 웹훅 본문을 서비스에 전달해 상태를 반영합니다.
			goodsService.handleShopOrderPaymentWebhook(rawBody);
			return ResponseEntity.ok(Map.of("success", true));
		} catch (Exception exception) {
			log.error("쇼핑몰 주문 결제 웹훅 반영 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "웹훅 처리에 실패했습니다."));
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

	// 요청 헤더를 기준으로 결제 디바이스 코드를 반환합니다.
	private String resolveDeviceGbCd(HttpServletRequest request) {
		// 모바일 User-Agent면 MO, 그 외는 PC로 판단합니다.
		String userAgent = request == null ? null : request.getHeader("User-Agent");
		if (userAgent == null) {
			return DEVICE_GB_PC;
		}
		String normalizedUserAgent = userAgent.toLowerCase();
		if (normalizedUserAgent.contains("android")
			|| normalizedUserAgent.contains("iphone")
			|| normalizedUserAgent.contains("ipad")
			|| normalizedUserAgent.contains("mobile")) {
			return DEVICE_GB_MO;
		}
		return DEVICE_GB_PC;
	}

	// 요청 헤더를 기준으로 프론트 절대 Origin 값을 추론합니다.
	private String resolveShopOrigin(HttpServletRequest request) {
		// Origin, X-Forwarded, Referer 순으로 프론트 Origin을 추론합니다.
		String origin = trimToNull(request == null ? null : request.getHeader("Origin"));
		if (origin != null) {
			return origin;
		}
		String forwardedHost = trimToNull(request == null ? null : request.getHeader("X-Forwarded-Host"));
		String forwardedProto = trimToNull(request == null ? null : request.getHeader("X-Forwarded-Proto"));
		if (forwardedHost != null) {
			return (forwardedProto == null ? "http" : forwardedProto) + "://" + forwardedHost;
		}
		String referer = trimToNull(request == null ? null : request.getHeader("Referer"));
		if (referer != null) {
			int slashIndex = referer.indexOf('/', referer.indexOf("://") + 3);
			return slashIndex > -1 ? referer.substring(0, slashIndex) : referer;
		}
		if (request == null) {
			return "";
		}
		return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
	}

	// 문자열을 trim 처리하고 비어 있으면 null을 반환합니다.
	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
