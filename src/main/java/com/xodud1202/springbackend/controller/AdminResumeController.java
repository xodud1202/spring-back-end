package com.xodud1202.springbackend.controller;

import com.xodud1202.springbackend.domain.admin.resume.ResumePO;
import com.xodud1202.springbackend.domain.admin.resume.ResumeVO;
import com.xodud1202.springbackend.domain.resume.ResumeEducation;
import com.xodud1202.springbackend.domain.resume.ResumeExperienceBase;
import com.xodud1202.springbackend.entity.ResumeBaseEntity;
import com.xodud1202.springbackend.entity.ResumeIntroduceEntity;
import com.xodud1202.springbackend.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

	/**
	 * 주어진 사용자 번호에 해당하는 자기소개 정보를 조회합니다.
	 * @param usrNo 조회할 사용자의 고유 번호
	 * @return 지정된 사용자 번호에 해당하는 자기소개 정보를 포함하는 {@code ResponseEntity} 객체
	 */
	@GetMapping("/api/admin/resume/introduce/{usrNo}")
	public ResponseEntity<Map<String, Object>> getResumeIntroduce(@PathVariable("usrNo") Long usrNo) {
		Map<String, Object> response = new HashMap<>();
		List<ResumeIntroduceEntity> introduceList = resumeService.getResumeIntroduceByUsrNo(usrNo);

		if (introduceList != null && !introduceList.isEmpty()) {
			ResumeIntroduceEntity introduce = introduceList.get(0);
			response.put("usrNo", introduce.getUsrNo());
			response.put("sortSeq", introduce.getSortSeq());
			response.put("introduceTitle", introduce.getIntroduceTitle());
			response.put("introduce", introduce.getIntroduce());
		} else {
			response.put("usrNo", usrNo);
			response.put("introduce", "");
		}

		return ResponseEntity.ok(response);
	}

	/**
	 * 주어진 사용자 번호에 해당하는 자기소개 정보를 저장합니다.
	 * @param usrNo 조회할 사용자의 고유 번호
	 * @param body 자기소개 내용이 포함된 요청 본문
	 * @return 저장 처리 결과를 포함하는 {@code ResponseEntity} 객체
	 */
	@PutMapping("/api/admin/resume/introduce/{usrNo}")
	public ResponseEntity<Map<String, String>> updateResumeIntroduce(@PathVariable("usrNo") Long usrNo, @RequestBody Map<String, Object> body) {
		String introduce = Objects.toString(body == null ? null : body.get("introduce"), "");
		return ResponseEntity.ok(resumeService.updateResumeIntroduceByUsrNo(usrNo, introduce));
	}

	/**
	 * 주어진 사용자 번호에 해당하는 이력서 기본 정보를 조회합니다.
	 * 이력서 기본 정보에는 사용자 이름, 부제목, 연락처, 이메일, 포트폴리오 링크, 최근 급여, 사진 경로,
	 * 스킬 정보, 주소 등이 포함됩니다. 만약 해당 사용자 번호에 대한 데이터가 존재하지 않을 경우,
	 * 빈 {@code ResponseEntity}가 반환됩니다.
	 * @param usrNo 조회할 사용자의 고유 번호
	 * @return 지정된 사용자 번호에 해당하는 이력서 기본 정보를 포함하는 {@code ResponseEntity} 객체
	 */
	@GetMapping("/api/admin/resume/{usrNo}")
	public ResponseEntity<ResumeBaseEntity> getResumeInfo(@PathVariable("usrNo") Long usrNo) {
		return ResponseEntity.ok(resumeService.getResumeBaseByUsrNo(usrNo));
	}

	@PutMapping("/api/admin/resume/{usrNo}")
	public ResponseEntity<Map<String, String>> updateResumeInfo(@RequestBody ResumeBaseEntity updatedResume) {
		return ResponseEntity.ok(resumeService.updateResumeBaseByUsrNo(updatedResume));
	}

	@GetMapping("/api/admin/resume/experience/{usrNo}")
	public ResponseEntity<List<ResumeExperienceBase>> getResumeExperience(@PathVariable("usrNo") Long usrNo) {
		return ResponseEntity.ok(resumeService.getAdminResumeExperienceList(usrNo));
	}

	@PutMapping("/api/admin/resume/experience/{usrNo}")
	public ResponseEntity<Map<String, String>> updateResumeExperience(@PathVariable("usrNo") Long usrNo,
	                                                                  @RequestBody ResumeExperienceBase body) {
		return ResponseEntity.ok(resumeService.saveResumeExperience(usrNo, body));
	}

	@DeleteMapping("/api/admin/resume/experience/{usrNo}/{experienceNo}")
	public ResponseEntity<Map<String, String>> deleteResumeExperience(@PathVariable("usrNo") Long usrNo,
	                                                                  @PathVariable("experienceNo") Long experienceNo) {
		return ResponseEntity.ok(resumeService.deleteResumeExperience(usrNo, experienceNo));
	}

	@GetMapping("/api/admin/resume/education/{usrNo}")
	public ResponseEntity<List<ResumeEducation>> getResumeEducation(@PathVariable("usrNo") Long usrNo) {
		return ResponseEntity.ok(resumeService.getAdminResumeEducationList(usrNo));
	}

	@PutMapping("/api/admin/resume/education/{usrNo}")
	public ResponseEntity<Map<String, String>> updateResumeEducation(@PathVariable("usrNo") Long usrNo,
	                                                                 @RequestBody ResumeEducation body) {
		return ResponseEntity.ok(resumeService.saveResumeEducation(usrNo, body));
	}

	@DeleteMapping("/api/admin/resume/education/{usrNo}")
	public ResponseEntity<Map<String, String>> deleteResumeEducation(@PathVariable("usrNo") Long usrNo,
	                                                                 @RequestParam("educationNo") Long educationNo) {
		return ResponseEntity.ok(resumeService.deleteResumeEducation(usrNo, educationNo));
	}
}

