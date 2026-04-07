package com.xodud1202.springbackend.controller.bo;

import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkCompanyVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkCompletedListResponseVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkDetailResponseVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkDetailUpdatePO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkImportPO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkImportResponseVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkListRowVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkProjectVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkReplySavePO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkReplyVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkUpdatePO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkStatusListResponseVO;
import com.xodud1202.springbackend.service.CompanyWorkService;
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
// 관리자 회사 업무 API를 제공합니다.
public class AdminCompanyWorkController {
	private final CompanyWorkService companyWorkService;

	// 관리자 회사 업무 회사 목록을 조회합니다.
	@GetMapping("/api/admin/company/work/company/list")
	public ResponseEntity<List<AdminCompanyWorkCompanyVO>> getAdminCompanyWorkCompanyList() {
		// 회사 선택 목록을 그대로 반환합니다.
		return ResponseEntity.ok(companyWorkService.getAdminCompanyWorkCompanyList());
	}

	// 관리자 회사 업무 프로젝트 목록을 조회합니다.
	@GetMapping("/api/admin/company/work/project/list")
	public ResponseEntity<Object> getAdminCompanyWorkProjectList(
		@RequestParam(required = false) Integer workCompanySeq
	) {
		try {
			// 선택 회사 기준 프로젝트 목록을 반환합니다.
			List<AdminCompanyWorkProjectVO> response = companyWorkService.getAdminCompanyWorkProjectList(workCompanySeq);
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		}
	}

	// 관리자 회사 업무 비완료 상태 목록을 조회합니다.
	@GetMapping("/api/admin/company/work/status/list")
	public ResponseEntity<Object> getAdminCompanyWorkStatusList(
		@RequestParam(required = false) Integer workCompanySeq,
		@RequestParam(required = false) Integer workCompanyProjectSeq,
		@RequestParam(required = false) String title
	) {
		try {
			// 회사와 프로젝트와 타이틀 조건으로 상태별 목록을 반환합니다.
			AdminCompanyWorkStatusListResponseVO response = companyWorkService.getAdminCompanyWorkStatusList(
				workCompanySeq,
				workCompanyProjectSeq,
				title
			);
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		}
	}

	// 관리자 회사 업무 완료 목록을 조회합니다.
	@GetMapping("/api/admin/company/work/completed/list")
	public ResponseEntity<Object> getAdminCompanyWorkCompletedList(
		@RequestParam(required = false) Integer workCompanySeq,
		@RequestParam(required = false) Integer workCompanyProjectSeq,
		@RequestParam(required = false) String title,
		@RequestParam(required = false) Integer page,
		@RequestParam(required = false) Integer pageSize
	) {
		try {
			// 회사와 프로젝트와 타이틀 조건으로 완료 목록을 페이지 단위로 반환합니다.
			AdminCompanyWorkCompletedListResponseVO response = companyWorkService.getAdminCompanyWorkCompletedList(
				workCompanySeq,
				workCompanyProjectSeq,
				title,
				page,
				pageSize
			);
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		}
	}

	// 관리자 회사 업무 상세 정보를 조회합니다.
	@GetMapping("/api/admin/company/work/detail")
	public ResponseEntity<Object> getAdminCompanyWorkDetail(
		@RequestParam(required = false) Long workSeq
	) {
		try {
			// 선택 업무 기준 상세 팝업 데이터를 반환합니다.
			AdminCompanyWorkDetailResponseVO response = companyWorkService.getAdminCompanyWorkDetail(workSeq);
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		}
	}

	// 관리자 회사 업무 Jira 이슈를 가져와 저장합니다.
	@PostMapping("/api/admin/company/work/import")
	public ResponseEntity<Object> importAdminCompanyWork(@RequestBody AdminCompanyWorkImportPO param) {
		try {
			// 가져오기 요청을 처리하고 저장 결과를 반환합니다.
			AdminCompanyWorkImportResponseVO response = companyWorkService.importAdminCompanyWork(param);
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException exception) {
			// 요청값이나 외부 조회 실패 중 사용자 교정이 가능한 오류는 400으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (IllegalStateException exception) {
			// 설정 누락이나 외부 시스템 오류는 500으로 반환합니다.
			return ResponseEntity.internalServerError().body(Map.of("message", exception.getMessage()));
		}
	}

	// 관리자 회사 업무 상세 수정 항목을 저장합니다.
	@PostMapping("/api/admin/company/work/detail/update")
	public ResponseEntity<Object> updateAdminCompanyWorkDetail(@RequestBody AdminCompanyWorkDetailUpdatePO param) {
		try {
			// 상세 저장 요청을 처리하고 최신 상세 정보를 반환합니다.
			return ResponseEntity.ok(companyWorkService.updateAdminCompanyWorkDetail(param));
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (IllegalStateException exception) {
			// 내부 상태 오류는 500 응답으로 반환합니다.
			return ResponseEntity.internalServerError().body(Map.of("message", exception.getMessage()));
		}
	}

	// 관리자 회사 업무 그리드 즉시 수정 항목을 저장합니다.
	@PostMapping("/api/admin/company/work/update")
	public ResponseEntity<Object> updateAdminCompanyWork(@RequestBody AdminCompanyWorkUpdatePO param) {
		try {
			// 수정 요청을 처리하고 최신 업무 행 정보를 반환합니다.
			AdminCompanyWorkListRowVO response = companyWorkService.updateAdminCompanyWork(param);
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (IllegalStateException exception) {
			// 내부 상태 오류는 500 응답으로 반환합니다.
			return ResponseEntity.internalServerError().body(Map.of("message", exception.getMessage()));
		}
	}

	// 관리자 회사 업무 댓글을 등록합니다.
	@PostMapping("/api/admin/company/work/reply")
	public ResponseEntity<Object> saveAdminCompanyWorkReply(@RequestBody AdminCompanyWorkReplySavePO param) {
		try {
			// 댓글 등록 요청을 처리하고 저장된 댓글을 반환합니다.
			AdminCompanyWorkReplyVO response = companyWorkService.saveAdminCompanyWorkReply(param);
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (IllegalStateException exception) {
			// 내부 상태 오류는 500 응답으로 반환합니다.
			return ResponseEntity.internalServerError().body(Map.of("message", exception.getMessage()));
		}
	}
}
