package com.xodud1202.springbackend.controller;

import com.xodud1202.springbackend.domain.resume.ResumeBase;
import com.xodud1202.springbackend.service.ResumeService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
public class ResumeController {
	
	private final ResumeService resumeService;
	
	@GetMapping("/api/resume/info")
	public ResponseEntity<Map<String, Object>> getResumeInfo(@RequestParam @NotBlank(message = "로그인 ID는 필수입니다.")  String loginId) {
		Map<String, Object> response = new HashMap<>();
		
		try {
			// 이력서 메인 정보 조회
			Optional<ResumeBase> resume = resumeService.getResumeByLoginId(loginId);
			if (resume.isPresent()) {
				ResumeBase resumeBase = resume.get();
				Long usrNo = resumeBase.getUsrNo();
				
				response.put("result", "OK");
				response.put("resumeBase", resumeBase);
				// 이력서 자기소개서 조회
				response.put("resumeIntroduceList", resumeService.getResumeIntroduceByUsrNo(usrNo));
				response.put("resumeExperienceList", resumeService.getResumeExperienceWithDetails(usrNo));
				response.put("resumeOtherExperienceList", resumeService.getResumeOtherExperienceList(usrNo));
			} else {
				response.put("result", "NOT_FOUND");
				response.put("message", "이력서 정보를 찾을 수 없습니다.");
			}
			
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			log.error("이력서 조회 중 오류 발생: ", e);
			response.put("result", "ERROR");
			response.put("message", "서버 오류가 발생했습니다.");
			return ResponseEntity.internalServerError().body(response);
		}
	}
}