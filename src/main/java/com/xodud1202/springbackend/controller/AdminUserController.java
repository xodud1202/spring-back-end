package com.xodud1202.springbackend.controller;

import com.xodud1202.springbackend.domain.admin.user.UserManagePO;
import com.xodud1202.springbackend.domain.admin.user.UserManageVO;
import com.xodud1202.springbackend.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// 관리자 사용자 관리 API를 제공하는 컨트롤러입니다.
@RestController
@RequiredArgsConstructor
public class AdminUserController {
	private final AdminUserService adminUserService;

	/**
	 * 관리자 사용자 목록을 조회합니다.
	 * @param searchGb 검색 구분(loginId/userNm)
	 * @param searchValue 검색어
	 * @param usrStatCd 사용자 상태 코드
	 * @param usrGradeCd 사용자 등급 코드
	 * @return 사용자 목록
	 */
	@GetMapping("/api/admin/user/manage/list")
	public ResponseEntity<List<UserManageVO>> getAdminUserList(
		@RequestParam(value = "searchGb", required = false) String searchGb,
		@RequestParam(value = "searchValue", required = false) String searchValue,
		@RequestParam(value = "usrStatCd", required = false) String usrStatCd,
		@RequestParam(value = "usrGradeCd", required = false) String usrGradeCd
	) {
		return ResponseEntity.ok(adminUserService.getAdminUserList(searchGb, searchValue, usrStatCd, usrGradeCd));
	}

	/**
	 * 관리자 사용자를 등록합니다.
	 * @param param 사용자 등록 정보
	 * @return 등록 결과
	 */
	@PostMapping("/api/admin/user/manage/create")
	public ResponseEntity<Object> createAdminUser(@RequestBody UserManagePO param) {
		String validationMessage = adminUserService.validateCreateAdminUser(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		return ResponseEntity.ok(adminUserService.createAdminUser(param));
	}

	/**
	 * 관리자 사용자를 수정합니다.
	 * @param param 사용자 수정 정보
	 * @return 수정 결과
	 */
	@PostMapping("/api/admin/user/manage/update")
	public ResponseEntity<Object> updateAdminUser(@RequestBody UserManagePO param) {
		String validationMessage = adminUserService.validateUpdateAdminUser(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		return ResponseEntity.ok(adminUserService.updateAdminUser(param));
	}
}

