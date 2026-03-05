package com.xodud1202.springbackend.controller.bo;

import com.xodud1202.springbackend.domain.admin.coupon.CouponDetailVO;
import com.xodud1202.springbackend.domain.admin.coupon.CouponSavePO;
import com.xodud1202.springbackend.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

	// 관리자 쿠폰 상세를 조회합니다.
	@GetMapping("/api/admin/coupon/detail")
	public ResponseEntity<Object> getCouponDetail(@RequestParam Long cpnNo) {
		// 쿠폰 번호 유효성을 확인합니다.
		if (cpnNo == null || cpnNo < 1) {
			return ResponseEntity.badRequest().body(Map.of("message", "쿠폰 번호를 확인해주세요."));
		}
		CouponDetailVO detail = couponService.getAdminCouponDetail(cpnNo);
		if (detail == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(detail);
	}

	// 관리자 쿠폰 대상 목록을 조회합니다.
	@GetMapping("/api/admin/coupon/target/list")
	public ResponseEntity<Object> getCouponTargetList(@RequestParam Long cpnNo) {
		try {
			return ResponseEntity.ok(couponService.getAdminCouponTargetList(cpnNo));
		} catch (IllegalArgumentException exception) {
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		}
	}

	// 관리자 쿠폰을 저장합니다.
	@PostMapping("/api/admin/coupon/save")
	public ResponseEntity<Object> saveCoupon(@RequestBody CouponSavePO param) {
		// 저장 요청값을 확인합니다.
		String validationMessage = couponService.validateCouponSave(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		try {
			return ResponseEntity.ok(couponService.saveCoupon(param));
		} catch (IllegalArgumentException exception) {
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		}
	}

	// 쿠폰 대상 엑셀 파일을 파싱합니다.
	@PostMapping(value = "/api/admin/coupon/target/excel/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Object> parseCouponTargetExcel(@RequestParam("file") MultipartFile file) {
		// 엑셀 파싱 요청값을 확인합니다.
		String validationMessage = couponService.validateCouponTargetExcelParse(file);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		try {
			return ResponseEntity.ok(couponService.parseCouponTargetExcel(file));
		} catch (IllegalArgumentException exception) {
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (IOException exception) {
			return ResponseEntity.internalServerError().body(Map.of("message", "엑셀 파싱에 실패했습니다."));
		}
	}

	// 쿠폰 대상 엑셀 템플릿을 다운로드합니다.
	@GetMapping("/api/admin/coupon/target/excel/download")
	public ResponseEntity<Object> downloadCouponTargetExcelTemplate() {
		try {
			byte[] data = couponService.buildCouponTargetExcelTemplate();
			return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"coupon_target_template.xlsx\"")
				.contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.body(data);
		} catch (IOException exception) {
			return ResponseEntity.internalServerError().body(Map.of("message", "엑셀 다운로드에 실패했습니다."));
		}
	}
}
