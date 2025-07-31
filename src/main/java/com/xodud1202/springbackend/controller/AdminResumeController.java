package com.xodud1202.springbackend.controller;

import com.xodud1202.springbackend.domain.admin.resume.ResumePO;
import com.xodud1202.springbackend.domain.admin.resume.ResumeVO;
import com.xodud1202.springbackend.entity.ResumeBaseEntity;
import com.xodud1202.springbackend.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
public class AdminResumeController {
	
	private final ResumeService resumeService;
	
	/**
	 * 관리자 이력서 목록을 조회합니다.
	 * @param param 이력서 검색 조건 및 검색어가 포함된 객체
	 * @return 이력서 목록을 담고 있는 {@code ResponseEntity} 객체
	 */
	@GetMapping("/api/admin/resume/list")
	public ResponseEntity<List<ResumeVO>> getResumeInfo(ResumePO param) {
		return ResponseEntity.ok(resumeService.getAdminResumeList(param));
	}
	
	
	@GetMapping("/api/admin/resume/{usrNo}")
	public ResponseEntity<ResumeBaseEntity> getResumeInfo(@PathVariable("usrNo") Long usrNo) {
		return ResponseEntity.ok(resumeService.getResumeBaseByUsrNo(usrNo));
	}
}
