package com.xodud1202.springbackend.controller;

import com.xodud1202.springbackend.domain.admin.category.CategorySavePO;
import com.xodud1202.springbackend.domain.admin.category.CategoryVO;
import com.xodud1202.springbackend.service.GoodsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequiredArgsConstructor
// 관리자 카테고리 API를 제공합니다.
public class AdminCategoryController {
	private final GoodsService goodsService;

	// 관리자 카테고리 트리 목록을 조회합니다.
	@GetMapping("/api/admin/category/manage/list")
	public ResponseEntity<List<CategoryVO>> getCategoryManageList() {
		// 카테고리 목록을 반환합니다.
		return ResponseEntity.ok(goodsService.getAdminCategoryTreeList());
	}

	// 관리자 카테고리 상세 정보를 조회합니다.
	@GetMapping("/api/admin/category/manage/detail")
	public ResponseEntity<Object> getCategoryManageDetail(@RequestParam String categoryId) {
		// 카테고리 아이디 유효성을 확인합니다.
		if (categoryId == null || categoryId.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("message", "카테고리 코드를 입력해주세요."));
		}
		CategoryVO detail = goodsService.getAdminCategoryDetail(categoryId);
		if (detail == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(detail);
	}

	// 관리자 카테고리 다음 코드를 조회합니다.
	@GetMapping("/api/admin/category/manage/next-id")
	public ResponseEntity<Map<String, Object>> getCategoryManageNextId(@RequestParam(required = false) String parentCategoryId) {
		// 다음 카테고리 코드를 생성합니다.
		String nextId = goodsService.getNextAdminCategoryId(parentCategoryId);
		return ResponseEntity.ok(Map.of("categoryId", nextId));
	}

	// 관리자 카테고리를 등록합니다.
	@PostMapping("/api/admin/category/manage/create")
	public ResponseEntity<Object> createCategory(@RequestBody CategorySavePO param) {
		// 요청 검증을 수행합니다.
		String validationMessage = goodsService.validateAdminCategoryCreate(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		return ResponseEntity.ok(goodsService.createAdminCategory(param));
	}

	// 관리자 카테고리를 수정합니다.
	@PostMapping("/api/admin/category/manage/update")
	public ResponseEntity<Object> updateCategory(@RequestBody CategorySavePO param) {
		// 요청 검증을 수행합니다.
		String validationMessage = goodsService.validateAdminCategoryUpdate(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		return ResponseEntity.ok(goodsService.updateAdminCategory(param));
	}

	// 관리자 카테고리를 삭제 처리합니다.
	@PostMapping("/api/admin/category/manage/delete")
	public ResponseEntity<Object> deleteCategory(@RequestBody CategorySavePO param) {
		// 요청 검증을 수행합니다.
		String validationMessage = goodsService.validateAdminCategoryDelete(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		return ResponseEntity.ok(goodsService.deleteAdminCategory(param));
	}
}
