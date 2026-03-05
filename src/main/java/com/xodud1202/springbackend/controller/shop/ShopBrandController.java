package com.xodud1202.springbackend.controller.shop;

import com.xodud1202.springbackend.service.BrandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
// 쇼핑몰 브랜드 조회 API를 제공합니다.
public class ShopBrandController {
	private final BrandService brandService;

	// 쇼핑몰 헤더 브랜드 목록을 조회합니다.
	@GetMapping("/api/shop/header/brands")
	public ResponseEntity<Object> getShopHeaderBrands() {
		try {
			// 브랜드 레이어에서 사용할 브랜드 목록을 반환합니다.
			return ResponseEntity.ok(brandService.getShopHeaderBrandList());
		} catch (Exception exception) {
			// 예외 발생 시 에러 로그와 실패 메시지를 반환합니다.
			log.error("쇼핑몰 헤더 브랜드 조회 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "헤더 브랜드 조회에 실패했습니다."));
		}
	}
}
