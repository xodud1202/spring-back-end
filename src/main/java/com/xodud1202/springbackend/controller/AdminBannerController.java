package com.xodud1202.springbackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xodud1202.springbackend.domain.admin.banner.BannerGoodsOrderSavePO;
import com.xodud1202.springbackend.domain.admin.banner.BannerSavePO;
import com.xodud1202.springbackend.service.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Validated
@RestController
@RequiredArgsConstructor
// 관리자 배너 API를 제공합니다.
public class AdminBannerController {
	private final BannerService bannerService;
	private final ObjectMapper objectMapper;

	// 관리자 배너 목록을 조회합니다.
	@GetMapping("/api/admin/banner/list")
	public ResponseEntity<Object> getBannerList(
		@RequestParam(required = false) String bannerDivCd,
		@RequestParam(required = false) String showYn,
		@RequestParam(required = false) String searchValue,
		@RequestParam(required = false) String searchStartDt,
		@RequestParam(required = false) String searchEndDt,
		@RequestParam(required = false) Integer page,
		@RequestParam(required = false) Integer pageSize
	) {
		// 목록 조회 결과를 반환합니다.
		return ResponseEntity.ok(bannerService.getAdminBannerList(bannerDivCd, showYn, searchValue, searchStartDt, searchEndDt, page, pageSize));
	}

	// 관리자 배너 상세를 조회합니다.
	@GetMapping("/api/admin/banner/detail")
	public ResponseEntity<Object> getBannerDetail(@RequestParam Integer bannerNo) {
		// 배너 번호 유효성을 확인합니다.
		if (bannerNo == null || bannerNo < 1) {
			return ResponseEntity.badRequest().body(Map.of("message", "배너 번호를 확인해주세요."));
		}
		Object detail = bannerService.getAdminBannerDetail(bannerNo);
		if (detail == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(detail);
	}

	// 관리자 배너를 등록합니다.
	@PostMapping(value = "/api/admin/banner/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Object> createBanner(
		@RequestPart("payload") String payload,
		@RequestPart(value = "image", required = false) MultipartFile image
	) {
		try {
			// 요청 JSON을 객체로 변환합니다.
			BannerSavePO param = objectMapper.readValue(payload, BannerSavePO.class);
			// 요청 유효성을 검증합니다.
			String validationMessage = bannerService.validateBannerCreate(param, image);
			if (validationMessage != null) {
				return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
			}
			return ResponseEntity.ok(bannerService.createBanner(param, image));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
		} catch (IOException e) {
			return ResponseEntity.internalServerError().body(Map.of("message", "배너 등록 중 오류가 발생했습니다."));
		}
	}

	// 관리자 배너를 수정합니다.
	@PostMapping(value = "/api/admin/banner/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Object> updateBanner(
		@RequestPart("payload") String payload,
		@RequestPart(value = "image", required = false) MultipartFile image
	) {
		try {
			// 요청 JSON을 객체로 변환합니다.
			BannerSavePO param = objectMapper.readValue(payload, BannerSavePO.class);
			// 요청 유효성을 검증합니다.
			String validationMessage = bannerService.validateBannerUpdate(param, image);
			if (validationMessage != null) {
				return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
			}
			return ResponseEntity.ok(bannerService.updateBanner(param, image));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
		} catch (IOException e) {
			return ResponseEntity.internalServerError().body(Map.of("message", "배너 수정 중 오류가 발생했습니다."));
		}
	}

	// 배너 상품 정렬 순서를 저장합니다.
	@PostMapping("/api/admin/banner/goods/order/save")
	public ResponseEntity<Object> saveBannerGoodsOrder(@org.springframework.web.bind.annotation.RequestBody BannerGoodsOrderSavePO param) {
		// 요청 유효성을 검증합니다.
		String validationMessage = bannerService.validateBannerGoodsOrder(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		return ResponseEntity.ok(bannerService.saveBannerGoodsOrder(param));
	}

	// 배너 상품 엑셀을 파싱합니다.
	@PostMapping(value = "/api/admin/banner/goods/excel/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Object> parseBannerGoodsExcel(
		@RequestParam("bannerDivCd") String bannerDivCd,
		@RequestParam("file") MultipartFile file
	) {
		// 요청 유효성을 검증합니다.
		String validationMessage = bannerService.validateBannerGoodsExcelParse(bannerDivCd, file);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		try {
			return ResponseEntity.ok(bannerService.parseBannerGoodsExcel(bannerDivCd, file));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
		} catch (IOException e) {
			return ResponseEntity.internalServerError().body(Map.of("message", "엑셀 파싱에 실패했습니다."));
		}
	}

	// 배너 상품 엑셀 템플릿을 다운로드합니다.
	@GetMapping("/api/admin/banner/goods/excel/download")
	public ResponseEntity<Object> downloadBannerGoodsExcelTemplate(@RequestParam("bannerDivCd") String bannerDivCd) {
		// 요청 유효성을 검증합니다.
		String validationMessage = bannerService.validateBannerGoodsExcelTemplate(bannerDivCd);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		try {
			byte[] data = bannerService.buildBannerGoodsExcelTemplate(bannerDivCd);
			String filename = "banner_goods_template_" + bannerDivCd + ".xlsx";
			return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
				.contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.body(data);
		} catch (IOException e) {
			return ResponseEntity.internalServerError().body(Map.of("message", "엑셀 다운로드에 실패했습니다."));
		}
	}
}
