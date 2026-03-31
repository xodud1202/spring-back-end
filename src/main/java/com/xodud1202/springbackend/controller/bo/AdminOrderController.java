package com.xodud1202.springbackend.controller.bo;

import com.xodud1202.springbackend.domain.admin.order.AdminOrderDetailStatusUpdatePO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderStartDeliveryPreparePO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderStartDeliveryStatusPO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderCancelPO;
import com.xodud1202.springbackend.service.GoodsService;
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
	private final GoodsService goodsService;

	// 관리자 주문 상세 정보를 조회합니다.
	@GetMapping("/api/admin/order/detail")
	public ResponseEntity<Object> getOrderDetail(
		@RequestParam String ordNo
	) {
		try {
			// 주문번호 기준으로 주문 상세 정보를 반환합니다.
			return ResponseEntity.ok(goodsService.getAdminOrderDetail(ordNo));
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
			return ResponseEntity.ok(goodsService.getAdminOrderCancelPage(ordNo));
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
			return ResponseEntity.ok(goodsService.cancelAdminOrder(param));
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			// 예상치 못한 오류는 500 응답으로 반환합니다.
			return ResponseEntity.internalServerError().body(Map.of("message", "주문 취소 처리 중 오류가 발생했습니다."));
		}
	}

	// 관리자 주문상세를 상품 준비중 상태로 변경합니다.
	@PostMapping("/api/admin/order/detail/prepare")
	public ResponseEntity<Object> prepareAdminOrderDetail(
		@RequestBody AdminOrderDetailStatusUpdatePO param
	) {
		try {
			// 선택한 주문상세를 상품 준비중 상태로 변경한 결과를 반환합니다.
			return ResponseEntity.ok(goodsService.prepareAdminOrderDetail(param));
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
			return ResponseEntity.ok(goodsService.getAdminOrderStartDeliveryList(page, pageSize, ordDtlStatCd));
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
			return ResponseEntity.ok(goodsService.prepareAdminOrderStartDelivery(param));
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
			return ResponseEntity.ok(goodsService.startAdminOrderStartDelivery(param));
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
			return ResponseEntity.ok(goodsService.completeAdminOrderStartDelivery(param));
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
			return ResponseEntity.ok(goodsService.getAdminOrderList(
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
