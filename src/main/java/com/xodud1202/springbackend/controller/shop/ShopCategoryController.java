package com.xodud1202.springbackend.controller.shop;

import com.xodud1202.springbackend.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
// 쇼핑몰 카테고리 조회 API를 제공합니다.
public class ShopCategoryController {
	private final CategoryService categoryService;

	// 쇼핑몰 헤더 카테고리 트리를 조회합니다.
	@GetMapping("/api/shop/header/categories")
	public ResponseEntity<Object> getShopHeaderCategories() {
		try {
			// 1/2/3차 카테고리 트리 데이터를 반환합니다.
			return ResponseEntity.ok(categoryService.getShopHeaderCategoryTree());
		} catch (Exception exception) {
			// 예외 발생 시 에러 로그와 실패 메시지를 반환합니다.
			log.error("쇼핑몰 헤더 카테고리 조회 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "헤더 카테고리 조회에 실패했습니다."));
		}
	}

	// 쇼핑몰 카테고리 페이지 데이터를 조회합니다.
	@GetMapping("/api/shop/category/page")
	public ResponseEntity<Object> getShopCategoryPage(@RequestParam(value = "categoryId", required = false) String categoryId) {
		try {
			// 선택 카테고리 기준 카테고리 페이지 데이터를 반환합니다.
			return ResponseEntity.ok(categoryService.getShopCategoryPage(categoryId));
		} catch (Exception exception) {
			// 예외 발생 시 에러 로그와 실패 메시지를 반환합니다.
			log.error("쇼핑몰 카테고리 페이지 조회 실패 message={} categoryId={}", exception.getMessage(), categoryId, exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "카테고리 페이지 조회에 실패했습니다."));
		}
	}
}
