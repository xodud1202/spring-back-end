package com.xodud1202.springbackend.controller;

import com.xodud1202.springbackend.domain.admin.brand.BrandPO;
import com.xodud1202.springbackend.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// 관리자 브랜드 API를 제공합니다.
@RestController
@RequiredArgsConstructor
public class AdminBrandController {
	private final BrandService brandService;

	// 관리자 브랜드 목록을 조회합니다.
	@GetMapping("/api/admin/brand/admin/list")
	public ResponseEntity<Map<String, Object>> getAdminBrandList(BrandPO param) {
		// 브랜드 목록과 페이징 정보를 반환합니다.
		return ResponseEntity.ok(brandService.getAdminBrandList(param));
	}

	// 관리자 브랜드 상세 정보를 조회합니다.
	@GetMapping("/api/admin/brand/admin/detail")
	public ResponseEntity<Object> getAdminBrandDetail(BrandPO param) {
		// 브랜드 상세 정보를 반환합니다.
		return ResponseEntity.ok(brandService.getAdminBrandDetail(param));
	}

	// 관리자 브랜드를 등록합니다.
	@PostMapping("/api/admin/brand/admin/create")
	public ResponseEntity<Object> createAdminBrand(@RequestBody BrandPO param) {
		// 브랜드 등록 결과를 반환합니다.
		return ResponseEntity.ok(brandService.insertAdminBrand(param));
	}

	// 관리자 브랜드를 수정합니다.
	@PostMapping("/api/admin/brand/admin/update")
	public ResponseEntity<Object> updateAdminBrand(@RequestBody BrandPO param) {
		// 브랜드 수정 결과를 반환합니다.
		return ResponseEntity.ok(brandService.updateAdminBrand(param));
	}
}
