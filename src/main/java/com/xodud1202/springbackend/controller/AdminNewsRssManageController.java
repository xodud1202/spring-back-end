package com.xodud1202.springbackend.controller;

import com.xodud1202.springbackend.domain.admin.news.AdminNewsCategoryDeletePO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsCategoryRowVO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsCategorySavePO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsPressDeletePO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsPressRowVO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsPressSavePO;
import com.xodud1202.springbackend.service.AdminNewsRssManageService;
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
// 관리자 뉴스 RSS 관리 API를 제공합니다.
public class AdminNewsRssManageController {
	private final AdminNewsRssManageService adminNewsRssManageService;

	// 관리자 뉴스 언론사 목록을 조회합니다.
	@GetMapping("/api/admin/news/rss/manage/press/list")
	public ResponseEntity<Object> getAdminNewsPressList() {
		List<AdminNewsPressRowVO> list = adminNewsRssManageService.getAdminNewsPressList();
		return ResponseEntity.ok(list);
	}

	// 관리자 뉴스 카테고리 목록을 조회합니다.
	@GetMapping("/api/admin/news/rss/manage/category/list")
	public ResponseEntity<Object> getAdminNewsCategoryList(@RequestParam Long pressNo) {
		if (pressNo == null) {
			return ResponseEntity.badRequest().body(Map.of("message", "언론사를 선택해주세요."));
		}
		List<AdminNewsCategoryRowVO> list = adminNewsRssManageService.getAdminNewsCategoryListByPressNo(pressNo);
		return ResponseEntity.ok(list);
	}

	// 관리자 뉴스 언론사 목록을 저장합니다.
	@PostMapping("/api/admin/news/rss/manage/press/save")
	public ResponseEntity<Object> saveAdminNewsPress(@RequestBody AdminNewsPressSavePO param) {
		String validationMessage = adminNewsRssManageService.validateAdminNewsPressSave(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		return ResponseEntity.ok(adminNewsRssManageService.saveAdminNewsPress(param));
	}

	// 관리자 뉴스 카테고리 목록을 저장합니다.
	@PostMapping("/api/admin/news/rss/manage/category/save")
	public ResponseEntity<Object> saveAdminNewsCategory(@RequestBody AdminNewsCategorySavePO param) {
		String validationMessage = adminNewsRssManageService.validateAdminNewsCategorySave(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		return ResponseEntity.ok(adminNewsRssManageService.saveAdminNewsCategory(param));
	}

	// 관리자 뉴스 언론사를 삭제합니다.
	@PostMapping("/api/admin/news/rss/manage/press/delete")
	public ResponseEntity<Object> deleteAdminNewsPress(@RequestBody AdminNewsPressDeletePO param) {
		String validationMessage = adminNewsRssManageService.validateAdminNewsPressDelete(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		return ResponseEntity.ok(adminNewsRssManageService.deleteAdminNewsPress(param));
	}

	// 관리자 뉴스 카테고리를 삭제합니다.
	@PostMapping("/api/admin/news/rss/manage/category/delete")
	public ResponseEntity<Object> deleteAdminNewsCategory(@RequestBody AdminNewsCategoryDeletePO param) {
		String validationMessage = adminNewsRssManageService.validateAdminNewsCategoryDelete(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		return ResponseEntity.ok(adminNewsRssManageService.deleteAdminNewsCategory(param));
	}
}
