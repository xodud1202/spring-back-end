package com.xodud1202.springbackend.controller;

import com.xodud1202.springbackend.domain.admin.common.AdminMenuLnb;
import com.xodud1202.springbackend.service.AdminCommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
public class AdminCommonController {
	
	private final AdminCommonService adminCommonService;
	
	@GetMapping("/api/admin/menu/list")
	public ResponseEntity<List<AdminMenuLnb>> getResumeInfo() {
		return ResponseEntity.ok(adminCommonService.getAdminMenuLnbInfo());
	}
}
