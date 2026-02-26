package com.xodud1202.springbackend.controller;

import com.xodud1202.springbackend.domain.admin.notion.AdminNotionCategorySortSavePO;
import com.xodud1202.springbackend.domain.admin.notion.AdminNotionListQueryPO;
import com.xodud1202.springbackend.service.AdminNotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
// 관리자 Notion 저장 목록/카테고리 정렬 API를 제공하는 컨트롤러입니다.
public class AdminNotionController {
	private final AdminNotionService adminNotionService;

	// 관리자 Notion 저장 목록을 조회합니다.
	@GetMapping("/api/admin/notion/save/list")
	public ResponseEntity<Object> getAdminNotionSaveList(AdminNotionListQueryPO param) {
		String validationMessage = adminNotionService.validateAdminNotionSaveListQuery(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		return ResponseEntity.ok(adminNotionService.getAdminNotionSaveList(param));
	}

	// 관리자 Notion 카테고리 목록을 조회합니다.
	@GetMapping("/api/admin/notion/category/list")
	public ResponseEntity<Object> getAdminNotionCategoryList() {
		return ResponseEntity.ok(adminNotionService.getAdminNotionCategoryList());
	}

	// 관리자 Notion 카테고리 순서를 저장합니다.
	@PostMapping("/api/admin/notion/category/sort/save")
	public ResponseEntity<Object> saveAdminNotionCategorySort(@RequestBody AdminNotionCategorySortSavePO param) {
		String validationMessage = adminNotionService.validateAdminNotionCategorySortSave(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		return ResponseEntity.ok(adminNotionService.saveAdminNotionCategorySort(param));
	}
}
