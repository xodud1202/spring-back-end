package com.xodud1202.springbackend.controller;

import com.xodud1202.springbackend.service.CouponService;
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
// 관리자 쿠폰 API를 제공합니다.
public class AdminCouponController {
	private final CouponService couponService;

	// 관리자 쿠폰 목록을 조회합니다.
	@GetMapping("/api/admin/coupon/list")
	public ResponseEntity<Object> getCouponList(
		@RequestParam(required = false) Integer page,
		@RequestParam(required = false) Integer pageSize,
		@RequestParam(required = false) String searchGb,
		@RequestParam(required = false) String searchValue,
		@RequestParam(required = false) String dateGb,
		@RequestParam(required = false) String searchStartDt,
		@RequestParam(required = false) String searchEndDt,
		@RequestParam(required = false) String cpnStatCd,
		@RequestParam(required = false) String cpnGbCd,
		@RequestParam(required = false) String cpnTargetCd,
		@RequestParam(required = false) String cpnDownAbleYn
	) {
		try {
			// 조회 조건으로 쿠폰 목록을 반환합니다.
			return ResponseEntity.ok(couponService.getAdminCouponList(
				page,
				pageSize,
				searchGb,
				searchValue,
				dateGb,
				searchStartDt,
				searchEndDt,
				cpnStatCd,
				cpnGbCd,
				cpnTargetCd,
				cpnDownAbleYn
			));
		} catch (IllegalArgumentException exception) {
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		}
	}
}
