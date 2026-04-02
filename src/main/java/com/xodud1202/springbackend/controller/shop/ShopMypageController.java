package com.xodud1202.springbackend.controller.shop;

import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageCancelHistoryPageVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageCancelHistoryVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageCouponDownloadRequestPO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageCouponPageVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderCancelPageVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderDetailPageVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderPageVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderReturnPageVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypagePointPageVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageWishPageVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderCancelPO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderDetailStatusUpdatePO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderDetailStatusUpdateVO;
import com.xodud1202.springbackend.service.DeliveryService;
import com.xodud1202.springbackend.service.GoodsService;
import com.xodud1202.springbackend.service.OrderCancelService;
import com.xodud1202.springbackend.service.OrderReturnService;
import com.xodud1202.springbackend.service.OrderService;
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

import static com.xodud1202.springbackend.common.Constants.Shop.SHOP_MYPAGE_ORDER_CANCEL_AMOUNT_MISMATCH_MESSAGE;
import static com.xodud1202.springbackend.common.Constants.Shop.SHOP_MYPAGE_ORDER_NOT_FOUND_MESSAGE;

@Slf4j
@RestController
@RequiredArgsConstructor
// 쇼핑몰 마이페이지 API를 제공합니다.
public class ShopMypageController extends ShopControllerSupport {
	private final GoodsService goodsService;
	private final OrderService orderService;
	private final OrderCancelService orderCancelService;
	private final OrderReturnService orderReturnService;
	private final DeliveryService deliveryService;

	// 쇼핑몰 마이페이지 위시리스트 페이지 데이터를 조회합니다.
	@GetMapping("/api/shop/mypage/wish/page")
	public ResponseEntity<Object> getShopMypageWishPage(
		@RequestParam(value = "pageNo", required = false) Integer pageNo,
		HttpServletRequest request
	) {
		try {
			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}
			ShopMypageWishPageVO result = goodsService.getShopMypageWishPage(custNo, pageNo);
			return ResponseEntity.ok(result);
		} catch (IllegalArgumentException exception) {
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
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
			Object goodsIdValue = requestBody == null ? null : requestBody.get("goodsId");
			String goodsId = goodsIdValue instanceof String ? ((String) goodsIdValue).trim() : "";
			if (goodsId.isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of("message", "상품코드를 확인해주세요."));
			}

			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}

			goodsService.deleteShopMypageWishGoods(goodsId, custNo);
			return ResponseEntity.ok(Map.of("message", "위시리스트에서 삭제했습니다."));
		} catch (IllegalArgumentException exception) {
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
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
			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}
			ShopMypageCouponPageVO result = goodsService.getShopMypageCouponPage(custNo, ownedPageNo, downloadablePageNo);
			return ResponseEntity.ok(result);
		} catch (IllegalArgumentException exception) {
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			log.error("쇼핑몰 마이페이지 쿠폰함 조회 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "쿠폰함 조회에 실패했습니다."));
		}
	}

	// 쇼핑몰 마이페이지 주문내역 페이지 데이터를 조회합니다.
	@GetMapping("/api/shop/mypage/order/page")
	public ResponseEntity<Object> getShopMypageOrderPage(
		@RequestParam(required = false) Integer pageNo,
		@RequestParam(required = false) String startDate,
		@RequestParam(required = false) String endDate,
		HttpServletRequest request
	) {
		try {
			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}
			ShopMypageOrderPageVO result = orderService.getShopMypageOrderPage(custNo, pageNo, startDate, endDate);
			return ResponseEntity.ok(result);
		} catch (IllegalArgumentException exception) {
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			log.error("쇼핑몰 마이페이지 주문내역 조회 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "주문내역 조회에 실패했습니다."));
		}
	}

	// 쇼핑몰 마이페이지 주문상세 페이지 데이터를 조회합니다.
	@GetMapping("/api/shop/mypage/order/detail")
	public ResponseEntity<Object> getShopMypageOrderDetailPage(
		@RequestParam(value = "ordNo", required = false) String ordNo,
		HttpServletRequest request
	) {
		try {
			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}
			if (ordNo == null || ordNo.trim().isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of("message", "주문번호를 확인해주세요."));
			}

			ShopMypageOrderDetailPageVO result = orderService.getShopMypageOrderDetailPage(custNo, ordNo);
			return ResponseEntity.ok(result);
		} catch (IllegalArgumentException exception) {
			if ("주문 정보를 찾을 수 없습니다.".equals(exception.getMessage())) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
			}
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			log.error("쇼핑몰 마이페이지 주문상세 조회 실패 message={} ordNo={}", exception.getMessage(), ordNo, exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "주문상세 조회에 실패했습니다."));
		}
	}

	// 쇼핑몰 마이페이지 주문취소 신청 화면 데이터를 조회합니다.
	@GetMapping("/api/shop/mypage/order/cancel/page")
	public ResponseEntity<Object> getShopMypageOrderCancelPage(
		@RequestParam(value = "ordNo", required = false) String ordNo,
		@RequestParam(value = "ordDtlNo", required = false) Integer ordDtlNo,
		HttpServletRequest request
	) {
		try {
			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}
			if (ordNo == null || ordNo.trim().isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of("message", "주문번호를 확인해주세요."));
			}

			ShopMypageOrderCancelPageVO result = orderCancelService.getShopMypageOrderCancelPage(custNo, ordNo, ordDtlNo);
			return ResponseEntity.ok(result);
		} catch (IllegalArgumentException exception) {
			if ("주문 정보를 찾을 수 없습니다.".equals(exception.getMessage())) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
			}
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			log.error(
				"쇼핑몰 마이페이지 주문취소 신청 화면 조회 실패 message={} ordNo={} ordDtlNo={}",
				exception.getMessage(),
				ordNo,
				ordDtlNo,
				exception
			);
			return ResponseEntity.internalServerError().body(Map.of("message", "주문취소 신청 화면 조회에 실패했습니다."));
		}
	}

	// 쇼핑몰 마이페이지 반품 신청 화면 데이터를 조회합니다.
	@GetMapping("/api/shop/mypage/order/return/page")
	public ResponseEntity<Object> getShopMypageOrderReturnPage(
		@RequestParam(value = "ordNo", required = false) String ordNo,
		@RequestParam(value = "ordDtlNo", required = false) Integer ordDtlNo,
		HttpServletRequest request
	) {
		try {
			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}
			if (ordNo == null || ordNo.trim().isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of("message", "주문번호를 확인해주세요."));
			}

			ShopMypageOrderReturnPageVO result = orderReturnService.getShopMypageOrderReturnPage(custNo, ordNo, ordDtlNo);
			return ResponseEntity.ok(result);
		} catch (IllegalArgumentException exception) {
			if ("주문 정보를 찾을 수 없습니다.".equals(exception.getMessage())) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
			}
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			log.error(
				"쇼핑몰 마이페이지 반품 신청 화면 조회 실패 message={} ordNo={} ordDtlNo={}",
				exception.getMessage(),
				ordNo,
				ordDtlNo,
				exception
			);
			return ResponseEntity.internalServerError().body(Map.of("message", "반품 신청 화면 조회에 실패했습니다."));
		}
	}

	// 쇼핑몰 마이페이지 취소내역 페이지 데이터를 조회합니다.
	@GetMapping("/api/shop/mypage/order/cancel/history")
	public ResponseEntity<Object> getShopMypageCancelHistoryPage(
		@RequestParam(value = "pageNo", required = false) Integer pageNo,
		@RequestParam(value = "startDate", required = false) String startDate,
		@RequestParam(value = "endDate", required = false) String endDate,
		HttpServletRequest request
	) {
		try {
			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}

			ShopMypageCancelHistoryPageVO result = orderCancelService.getShopMypageCancelHistoryPage(custNo, pageNo, startDate, endDate);
			return ResponseEntity.ok(result);
		} catch (IllegalArgumentException exception) {
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			log.error("쇼핑몰 마이페이지 취소내역 조회 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "취소내역 조회에 실패했습니다."));
		}
	}

	// 쇼핑몰 마이페이지 취소상세 단건을 조회합니다.
	@GetMapping("/api/shop/mypage/order/cancel/detail")
	public ResponseEntity<Object> getShopMypageCancelHistoryDetail(
		@RequestParam(required = false) String clmNo,
		HttpServletRequest request
	) {
		try {
			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}

			ShopMypageCancelHistoryVO result = orderCancelService.getShopMypageCancelHistoryDetail(custNo, clmNo);
			return ResponseEntity.ok(result);
		} catch (IllegalArgumentException exception) {
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			log.error("쇼핑몰 마이페이지 취소상세 조회 실패 clmNo={} message={}", clmNo, exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "취소상세 조회에 실패했습니다."));
		}
	}

	// 쇼핑몰 마이페이지에서 쿠폰 1건을 다운로드합니다.
	@PostMapping("/api/shop/mypage/coupon/download")
	public ResponseEntity<Object> downloadShopMypageCoupon(
		@RequestBody(required = false) ShopMypageCouponDownloadRequestPO requestBody,
		HttpServletRequest request
	) {
		try {
			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}

			goodsService.downloadShopMypageCoupon(requestBody, custNo);
			return ResponseEntity.ok(Map.of("message", "쿠폰을 다운로드했습니다."));
		} catch (IllegalArgumentException exception) {
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			log.error("쇼핑몰 마이페이지 쿠폰 다운로드 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "쿠폰 다운로드에 실패했습니다."));
		}
	}

	// 쇼핑몰 마이페이지에서 현재 다운로드 가능한 쿠폰을 전체 다운로드합니다.
	@PostMapping("/api/shop/mypage/coupon/download/all")
	public ResponseEntity<Object> downloadAllShopMypageCoupon(HttpServletRequest request) {
		try {
			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}

			int downloadedCount = goodsService.downloadAllShopMypageCoupon(custNo);
			String message = downloadedCount > 0 ? "전체 쿠폰을 다운로드했습니다." : "다운로드 가능한 쿠폰이 없습니다.";
			return ResponseEntity.ok(Map.of("downloadedCount", downloadedCount, "message", message));
		} catch (IllegalArgumentException exception) {
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			log.error("쇼핑몰 마이페이지 전체 쿠폰 다운로드 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "전체 쿠폰 다운로드에 실패했습니다."));
		}
	}

	// 쇼핑몰 마이페이지 포인트 내역 페이지 데이터를 조회합니다.
	@GetMapping("/api/shop/mypage/point/page")
	public ResponseEntity<Object> getShopMypagePointPage(
		@RequestParam(required = false) Integer pageNo,
		HttpServletRequest request
	) {
		try {
			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}

			ShopMypagePointPageVO result = orderService.getShopMypagePointPage(custNo, pageNo);
			return ResponseEntity.ok(result);
		} catch (IllegalArgumentException exception) {
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			log.error("쇼핑몰 마이페이지 포인트 내역 조회 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "포인트 내역 조회에 실패했습니다."));
		}
	}

	// 쇼핑몰 마이페이지 주문취소를 즉시 완료 처리합니다.
	@PostMapping("/api/shop/mypage/order/cancel")
	public ResponseEntity<Object> cancelShopMypageOrder(
		@RequestBody(required = false) ShopOrderCancelPO param,
		HttpServletRequest request
	) {
		try {
			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}

			return ResponseEntity.ok(orderCancelService.cancelShopMypageOrder(param, custNo));
		} catch (IllegalArgumentException exception) {
			if (SHOP_MYPAGE_ORDER_CANCEL_AMOUNT_MISMATCH_MESSAGE.equals(exception.getMessage())) {
				return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", exception.getMessage()));
			}
			if (SHOP_MYPAGE_ORDER_NOT_FOUND_MESSAGE.equals(exception.getMessage())) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
			}
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			log.error("쇼핑몰 마이페이지 주문취소 처리 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "주문취소 처리에 실패했습니다."));
		}
	}

	// 쇼핑몰 마이페이지 배송중 주문상품을 배송완료 처리합니다.
	@PostMapping("/api/shop/mypage/order/delivery/complete")
	public ResponseEntity<Object> completeShopMypageOrderDelivery(
		@RequestBody(required = false) ShopOrderDetailStatusUpdatePO param,
		HttpServletRequest request
	) {
		try {
			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}

			ShopOrderDetailStatusUpdateVO result = deliveryService.completeShopMypageOrderDelivery(param, custNo);
			return ResponseEntity.ok(result);
		} catch (IllegalArgumentException exception) {
			if (SHOP_MYPAGE_ORDER_NOT_FOUND_MESSAGE.equals(exception.getMessage())) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
			}
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			log.error("쇼핑몰 마이페이지 배송완료 처리 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "배송완료 처리에 실패했습니다."));
		}
	}

	// 쇼핑몰 마이페이지 배송완료 주문상품을 구매확정 처리합니다.
	@PostMapping("/api/shop/mypage/order/purchase/confirm")
	public ResponseEntity<Object> confirmShopMypageOrderPurchase(
		@RequestBody(required = false) ShopOrderDetailStatusUpdatePO param,
		HttpServletRequest request
	) {
		try {
			Long custNo = parseCustNoCookie(request);
			if (custNo == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
			}

			ShopOrderDetailStatusUpdateVO result = orderService.confirmShopMypageOrderPurchase(param, custNo);
			return ResponseEntity.ok(result);
		} catch (IllegalArgumentException exception) {
			if (SHOP_MYPAGE_ORDER_NOT_FOUND_MESSAGE.equals(exception.getMessage())) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
			}
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			log.error("쇼핑몰 마이페이지 구매확정 처리 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "구매확정 처리에 실패했습니다."));
		}
	}
}
