package com.xodud1202.springbackend.controller.bo;

import com.xodud1202.springbackend.service.GoodsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
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
