package com.xodud1202.springbackend.controller.bo;

import com.xodud1202.springbackend.domain.admin.order.AdminOrderDetailStatusUpdatePO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderReturnManageListResponseVO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderReturnManagePickupCompletePageVO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderReturnManagePickupRequestPO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderReturnManagePickupStartPO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderReturnPageVO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderReturnPO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderReturnWithdrawPO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderReturnWithdrawVO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderStartDeliveryPreparePO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderStartDeliveryStatusPO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderCancelPO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderReturnResultVO;
import com.xodud1202.springbackend.service.DeliveryService;
import com.xodud1202.springbackend.service.OrderCancelService;
import com.xodud1202.springbackend.service.OrderReturnService;
import com.xodud1202.springbackend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Validated
@RestController
@RequiredArgsConstructor
// 관리자 주문 API를 제공합니다.
public class AdminOrderController {
	private final OrderService orderService;
	private final OrderCancelService orderCancelService;
	private final OrderReturnService orderReturnService;
	private final DeliveryService deliveryService;

	// 관리자 주문 상세 정보를 조회합니다.
	@GetMapping("/api/admin/order/detail")
	public ResponseEntity<Object> getOrderDetail(
		@RequestParam String ordNo
	) {
		try {
			// 주문번호 기준으로 주문 상세 정보를 반환합니다.
			return ResponseEntity.ok(orderService.getAdminOrderDetail(ordNo));
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		}
	}

	// 관리자 주문취소 신청 화면 데이터를 조회합니다.
	@GetMapping("/api/admin/order/cancel/page")
	public ResponseEntity<Object> getAdminOrderCancelPage(
		@RequestParam String ordNo
	) {
		try {
			// 주문번호 기준으로 취소 신청 화면 데이터를 반환합니다.
			return ResponseEntity.ok(orderCancelService.getAdminOrderCancelPage(ordNo));
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		}
	}

	// 관리자 주문취소를 즉시 완료 처리합니다.
	@PostMapping("/api/admin/order/cancel")
	public ResponseEntity<Object> cancelAdminOrder(
		@RequestBody ShopOrderCancelPO param
	) {
		try {
			// 취소 요청을 처리하고 완료 결과를 반환합니다.
			return ResponseEntity.ok(orderCancelService.cancelAdminOrder(param));
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			// 예상치 못한 오류는 500 응답으로 반환합니다.
			return ResponseEntity.internalServerError().body(Map.of("message", "주문 취소 처리 중 오류가 발생했습니다."));
		}
	}

	// 관리자 주문반품 신청 화면 데이터를 조회합니다.
	@GetMapping("/api/admin/order/return/page")
	public ResponseEntity<Object> getAdminOrderReturnPage(
		@RequestParam String ordNo
	) {
		try {
			// 주문번호 기준으로 반품 신청 화면 데이터를 반환합니다.
			AdminOrderReturnPageVO result = orderReturnService.getAdminOrderReturnPage(ordNo);
			return ResponseEntity.ok(result);
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		}
	}

	// 관리자 주문반품 신청을 저장합니다.
	@PostMapping("/api/admin/order/return")
	public ResponseEntity<Object> returnAdminOrder(
		@RequestBody(required = false) AdminOrderReturnPO param
	) {
		try {
			// 반품 요청을 처리하고 신청 결과를 반환합니다.
			ShopOrderReturnResultVO result = orderReturnService.returnAdminOrder(param);
			return ResponseEntity.ok(result);
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			// 예상치 못한 오류는 500 응답으로 반환합니다.
			return ResponseEntity.internalServerError().body(Map.of("message", "반품 신청 처리 중 오류가 발생했습니다."));
		}
	}

	// 관리자 주문반품 철회를 저장합니다.
	@PostMapping("/api/admin/order/return/withdraw")
	public ResponseEntity<Object> withdrawAdminOrderReturn(
		@RequestBody(required = false) AdminOrderReturnWithdrawPO param
	) {
		try {
			// 반품 철회 요청을 처리하고 결과를 반환합니다.
			AdminOrderReturnWithdrawVO result = orderReturnService.withdrawAdminOrderReturn(param);
			return ResponseEntity.ok(result);
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			// 예상치 못한 오류는 500 응답으로 반환합니다.
			return ResponseEntity.internalServerError().body(Map.of("message", "반품 철회 처리 중 오류가 발생했습니다."));
		}
	}

	// 관리자 주문 반품 회수지 우편번호 검색 결과를 조회합니다.
	// 관리자 반품 회수 관리 목록을 조회합니다.
	// 愿由ъ옄 諛섑뭹 ?뚯닔 愿由?紐⑸줉??議고쉶?⑸땲??
	@GetMapping("/api/admin/order/return/manage/list")
	public ResponseEntity<Object> getAdminOrderReturnManageList(
		@RequestParam(required = false) Integer page,
		@RequestParam(required = false) Integer pageSize,
		@RequestParam(required = false) String chgDtlStatCd
	) {
		try {
			// 조회 조건으로 관리자 반품 회수 관리 목록을 반환합니다.
			AdminOrderReturnManageListResponseVO result = orderReturnService.getAdminOrderReturnManageList(page, pageSize, chgDtlStatCd);
			return ResponseEntity.ok(result);
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		}
	}

	// 관리자 반품 회수 신청을 저장합니다.
	@PostMapping("/api/admin/order/return/manage/pickup/request")
	public ResponseEntity<Object> requestAdminOrderReturnPickup(
		@RequestBody(required = false) AdminOrderReturnManagePickupRequestPO param
	) {
		try {
			// 선택한 반품 클레임을 반품 회수 신청 상태로 변경합니다.
			return ResponseEntity.ok(orderReturnService.requestAdminOrderReturnPickup(param));
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			// 예상치 못한 오류는 500 응답으로 반환합니다.
			return ResponseEntity.internalServerError().body(Map.of("message", "송장저장 처리 중 오류가 발생했습니다."));
		}
	}

	// 관리자 반품 회수 신청 클레임을 회수중 상태로 변경합니다.
	@PostMapping("/api/admin/order/return/manage/pickup/start")
	public ResponseEntity<Object> startAdminOrderReturnPickup(
		@RequestBody(required = false) AdminOrderReturnManagePickupStartPO param
	) {
		try {
			// 선택한 반품 클레임을 반품 회수중 상태로 변경합니다.
			return ResponseEntity.ok(orderReturnService.startAdminOrderReturnPickup(param));
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			// 예상치 못한 오류는 500 응답으로 반환합니다.
			return ResponseEntity.internalServerError().body(Map.of("message", "회수중 처리 중 오류가 발생했습니다."));
		}
	}

	// 관리자 반품 회수완료 검수 팝업 화면 데이터를 조회합니다.
	@GetMapping("/api/admin/order/return/manage/pickup/complete/page")
	public ResponseEntity<Object> getAdminOrderReturnPickupCompletePage(
		@RequestParam String clmNo
	) {
		try {
			// 클레임번호 기준으로 반품 회수완료 검수 팝업 데이터를 반환합니다.
			AdminOrderReturnManagePickupCompletePageVO result = orderReturnService.getAdminOrderReturnManagePickupCompletePage(clmNo);
			return ResponseEntity.ok(result);
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			// 예상치 못한 오류는 500 응답으로 반환합니다.
			return ResponseEntity.internalServerError().body(Map.of("message", "회수완료 검수 화면 조회 중 오류가 발생했습니다."));
		}
	}

	// 愿由ъ옄 二쇰Ц 諛섑뭹 ?뚯닔吏 ?고렪踰덊샇 寃??寃곌낵瑜?議고쉶?⑸땲??
	@GetMapping("/api/admin/order/address/search")
	public ResponseEntity<Object> searchAdminOrderAddress(
		@RequestParam(value = "keyword", required = false) String keyword,
		@RequestParam(value = "currentPage", required = false) Integer currentPage,
		@RequestParam(value = "countPerPage", required = false) Integer countPerPage
	) {
		try {
			// 주소 검색 결과를 조회해 반환합니다.
			return ResponseEntity.ok(orderService.searchAdminOrderAddress(keyword, currentPage, countPerPage));
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			// 예상치 못한 오류는 500 응답으로 반환합니다.
			return ResponseEntity.internalServerError().body(Map.of("message", "주소 검색에 실패했습니다."));
		}
	}

	// 관리자 주문상세를 상품 준비중 상태로 변경합니다.
	@PostMapping("/api/admin/order/detail/prepare")
	public ResponseEntity<Object> prepareAdminOrderDetail(
		@RequestBody AdminOrderDetailStatusUpdatePO param
	) {
		try {
			// 선택한 주문상세를 상품 준비중 상태로 변경한 결과를 반환합니다.
			return ResponseEntity.ok(deliveryService.prepareAdminOrderDetail(param));
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			// 예상치 못한 오류는 500 응답으로 반환합니다.
			return ResponseEntity.internalServerError().body(Map.of("message", "상품 준비중 처리 중 오류가 발생했습니다."));
		}
	}

	// 관리자 배송 시작 관리 목록을 조회합니다.
	@GetMapping("/api/admin/order/start/delivery/list")
	public ResponseEntity<Object> getAdminOrderStartDeliveryList(
		@RequestParam(required = false) Integer page,
		@RequestParam(required = false) Integer pageSize,
		@RequestParam(required = false) String ordDtlStatCd
	) {
		try {
			// 조회 조건으로 관리자 배송 시작 관리 목록을 반환합니다.
			return ResponseEntity.ok(deliveryService.getAdminOrderStartDeliveryList(page, pageSize, ordDtlStatCd));
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		}
	}

	// 관리자 상품 준비중 주문을 배송 준비중으로 변경합니다.
	@PostMapping("/api/admin/order/start/delivery/prepare")
	public ResponseEntity<Object> prepareAdminOrderStartDelivery(
		@RequestBody AdminOrderStartDeliveryPreparePO param
	) {
		try {
			// 선택한 상품을 배송 준비중 상태로 변경한 결과를 반환합니다.
			return ResponseEntity.ok(deliveryService.prepareAdminOrderStartDelivery(param));
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			// 예상치 못한 오류는 500 응답으로 반환합니다.
			return ResponseEntity.internalServerError().body(Map.of("message", "배송 준비중 처리 중 오류가 발생했습니다."));
		}
	}

	// 관리자 배송 준비중 주문을 배송중으로 변경합니다.
	@PostMapping("/api/admin/order/start/delivery/start")
	public ResponseEntity<Object> startAdminOrderStartDelivery(
		@RequestBody AdminOrderStartDeliveryStatusPO param
	) {
		try {
			// 선택한 상품을 배송중 상태로 변경한 결과를 반환합니다.
			return ResponseEntity.ok(deliveryService.startAdminOrderStartDelivery(param));
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			// 예상치 못한 오류는 500 응답으로 반환합니다.
			return ResponseEntity.internalServerError().body(Map.of("message", "배송중 처리 중 오류가 발생했습니다."));
		}
	}

	// 관리자 배송중 주문을 배송완료로 변경합니다.
	@PostMapping("/api/admin/order/start/delivery/complete")
	public ResponseEntity<Object> completeAdminOrderStartDelivery(
		@RequestBody AdminOrderStartDeliveryStatusPO param
	) {
		try {
			// 선택한 상품을 배송완료 상태로 변경한 결과를 반환합니다.
			return ResponseEntity.ok(deliveryService.completeAdminOrderStartDelivery(param));
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			// 예상치 못한 오류는 500 응답으로 반환합니다.
			return ResponseEntity.internalServerError().body(Map.of("message", "배송완료 처리 중 오류가 발생했습니다."));
		}
	}

	// 관리자 주문 목록을 조회합니다.
	@GetMapping("/api/admin/order/list")
	public ResponseEntity<Object> getOrderList(
		@RequestParam(required = false) Integer page,
		@RequestParam(required = false) Integer pageSize,
		@RequestParam(required = false) String searchGb,
		@RequestParam(required = false) String searchValue,
		@RequestParam(required = false) String dateGb,
		@RequestParam(required = false) String searchStartDt,
		@RequestParam(required = false) String searchEndDt,
		@RequestParam(required = false) String ordDtlStatCd,
		@RequestParam(required = false) String chgDtlStatCd
	) {
		try {
			// 조회 조건으로 관리자 주문 목록을 반환합니다.
			return ResponseEntity.ok(orderService.getAdminOrderList(
				page,
				pageSize,
				searchGb,
				searchValue,
				dateGb,
				searchStartDt,
				searchEndDt,
				ordDtlStatCd,
				chgDtlStatCd
			));
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		}
	}
}
