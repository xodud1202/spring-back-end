package com.xodud1202.springbackend.controller;

import com.xodud1202.springbackend.domain.admin.common.AdminMenuLnb;
import com.xodud1202.springbackend.domain.admin.resume.ResumePO;
import com.xodud1202.springbackend.domain.admin.resume.ResumeVO;
import com.xodud1202.springbackend.service.ResumeService;
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
public class AdminResumeController {
	
	private final ResumeService resumeService;
	
	@GetMapping("/api/admin/resume/list")
	public ResponseEntity<List<ResumeVO>> getResumeInfo(ResumePO param) {
		return ResponseEntity.ok(resumeService.getAdminResumeList(param));
	}
}
