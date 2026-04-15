package com.xodud1202.springbackend.controller.bo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkCompanyVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkCompletedListResponseVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkDetailResponseVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkDetailUpdatePO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkImportPO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkImportResponseVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkListRowVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkManualCreatePO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkManualCreateResponseVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkProjectVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkReplyDeletePO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkReplyFileDownloadVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkReplySavePO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkReplyUpdatePO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkReplyVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkUpdatePO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkStatusListResponseVO;
import com.xodud1202.springbackend.service.CompanyWorkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequiredArgsConstructor
// 관리자 회사 업무 API를 제공합니다.
public class AdminCompanyWorkController {
	private final CompanyWorkService companyWorkService;
	private final ObjectMapper objectMapper;

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
			AdminCompanyWorkDetailResponseVO response = companyWorkService.getCompanyWorkDetail(workSeq);
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		}
	}

	// 관리자 회사 업무 댓글 전용 목록을 조회합니다.
	@GetMapping("/api/admin/company/work/reply/list")
	public ResponseEntity<Object> getAdminCompanyWorkReplyList(
		@RequestParam(required = false) Long workSeq
	) {
		try {
			// 선택 업무 기준 댓글 목록만 조회해 반환합니다.
			List<AdminCompanyWorkReplyVO> response = companyWorkService.getAdminCompanyWorkReplyList(workSeq);
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

	// 관리자 회사 업무 수기 등록을 저장합니다.
	@PostMapping("/api/admin/company/work/manual")
	public ResponseEntity<Object> createAdminCompanyWorkManual(@RequestBody AdminCompanyWorkManualCreatePO param) {
		if (!isAuthenticatedRequest()) {
			return unauthorizedResponse();
		}

		try {
			// 수기 등록 요청을 처리하고 저장 결과를 반환합니다.
			AdminCompanyWorkManualCreateResponseVO response = companyWorkService.createAdminCompanyWorkManual(param);
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (AccessDeniedException exception) {
			// 본인 사용자 정보가 아니면 403 응답으로 반환합니다.
			return ResponseEntity.status(403).body(Map.of("message", exception.getMessage()));
		} catch (IllegalStateException exception) {
			// 내부 상태 오류는 500 응답으로 반환합니다.
			return ResponseEntity.internalServerError().body(Map.of("message", exception.getMessage()));
		}
	}

	// 관리자 회사 업무 상세 수정 항목을 저장합니다.
	@PostMapping("/api/admin/company/work/detail/update")
	public ResponseEntity<Object> updateAdminCompanyWorkDetail(@RequestBody AdminCompanyWorkDetailUpdatePO param) {
		try {
			// 상세 저장 요청을 처리하고 최신 상세 정보를 반환합니다.
			return ResponseEntity.ok(companyWorkService.updateAdminCompanyWorkDetailAndGetDetail(param));
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
	@PostMapping(value = "/api/admin/company/work/reply", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> saveAdminCompanyWorkReply(@RequestBody AdminCompanyWorkReplySavePO param) {
		if (!isAuthenticatedRequest()) {
			return unauthorizedResponse();
		}

		try {
			// 댓글 등록 요청을 처리하고 저장된 댓글을 반환합니다.
			AdminCompanyWorkReplyVO response = companyWorkService.saveAdminCompanyWorkReply(param);
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (AccessDeniedException exception) {
			// 본인 댓글 권한이 아닌 경우 403 응답으로 반환합니다.
			return ResponseEntity.status(403).body(Map.of("message", exception.getMessage()));
		} catch (IllegalStateException exception) {
			// 내부 상태 오류는 500 응답으로 반환합니다.
			return ResponseEntity.internalServerError().body(Map.of("message", exception.getMessage()));
		}
	}

	// 관리자 회사 업무 댓글과 첨부파일을 함께 등록합니다.
	@PostMapping(value = "/api/admin/company/work/reply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Object> saveAdminCompanyWorkReplyWithFiles(
		@RequestPart("payload") String payload,
		@RequestPart(value = "files", required = false) List<MultipartFile> files
	) {
		if (!isAuthenticatedRequest()) {
			return unauthorizedResponse();
		}

		try {
			// 댓글 저장 JSON을 객체로 변환한 뒤 첨부파일과 함께 저장합니다.
			AdminCompanyWorkReplySavePO param = objectMapper.readValue(payload, AdminCompanyWorkReplySavePO.class);
			AdminCompanyWorkReplyVO response = companyWorkService.saveAdminCompanyWorkReply(param, files);
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (AccessDeniedException exception) {
			// 본인 댓글 권한이 아닌 경우 403 응답으로 반환합니다.
			return ResponseEntity.status(403).body(Map.of("message", exception.getMessage()));
		} catch (IOException exception) {
			// 멀티파트 payload 파싱 실패는 400으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", "댓글 등록 요청 정보를 확인해주세요."));
		} catch (IllegalStateException exception) {
			// 내부 상태 오류는 500 응답으로 반환합니다.
			return ResponseEntity.internalServerError().body(Map.of("message", exception.getMessage()));
		}
	}

	// 관리자 회사 업무 댓글과 첨부파일을 함께 수정합니다.
	@PostMapping(value = "/api/admin/company/work/reply/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Object> updateAdminCompanyWorkReply(
		@RequestPart("payload") String payload,
		@RequestPart(value = "files", required = false) List<MultipartFile> files
	) {
		if (!isAuthenticatedRequest()) {
			return unauthorizedResponse();
		}

		try {
			// 댓글 수정 JSON을 객체로 변환한 뒤 첨부파일과 함께 저장합니다.
			AdminCompanyWorkReplyUpdatePO param = objectMapper.readValue(payload, AdminCompanyWorkReplyUpdatePO.class);
			AdminCompanyWorkReplyVO response = companyWorkService.updateAdminCompanyWorkReply(param, files);
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (AccessDeniedException exception) {
			// 본인 댓글 권한이 아닌 경우 403 응답으로 반환합니다.
			return ResponseEntity.status(403).body(Map.of("message", exception.getMessage()));
		} catch (IOException exception) {
			// 멀티파트 payload 파싱 실패는 400으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", "댓글 수정 요청 정보를 확인해주세요."));
		} catch (IllegalStateException exception) {
			// 내부 상태 오류는 500 응답으로 반환합니다.
			return ResponseEntity.internalServerError().body(Map.of("message", exception.getMessage()));
		}
	}

	// 관리자 회사 업무 댓글을 삭제 처리합니다.
	@PostMapping("/api/admin/company/work/reply/delete")
	public ResponseEntity<Object> deleteAdminCompanyWorkReply(@RequestBody AdminCompanyWorkReplyDeletePO param) {
		if (!isAuthenticatedRequest()) {
			return unauthorizedResponse();
		}

		try {
			// 댓글 삭제 요청을 처리하고 완료 메시지를 반환합니다.
			companyWorkService.deleteAdminCompanyWorkReply(param);
			return ResponseEntity.ok(Map.of("message", "댓글을 삭제했습니다."));
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (AccessDeniedException exception) {
			// 본인 댓글 권한이 아닌 경우 403 응답으로 반환합니다.
			return ResponseEntity.status(403).body(Map.of("message", exception.getMessage()));
		} catch (IllegalStateException exception) {
			// 내부 상태 오류는 500 응답으로 반환합니다.
			return ResponseEntity.internalServerError().body(Map.of("message", exception.getMessage()));
		}
	}

	// 관리자 회사 업무 댓글 첨부파일을 다운로드합니다.
	@GetMapping("/api/admin/company/work/reply/file/download")
	public ResponseEntity<Object> downloadAdminCompanyWorkReplyFile(
		@RequestParam(required = false) Integer replyFileSeq
	) {
		// 공개 API로 열려 있어도 실제 다운로드는 인증된 관리자만 허용합니다.
		if (!isAuthenticatedRequest()) {
			return unauthorizedResponse();
		}

		try {
			// 댓글 첨부파일 메타와 파일 데이터를 조회해 attachment 응답으로 반환합니다.
			AdminCompanyWorkReplyFileDownloadVO response = companyWorkService.downloadAdminCompanyWorkReplyFile(replyFileSeq);
			String encodedFileName = encodeAttachmentFileName(response.getReplyFileNm());
			return ResponseEntity.ok()
				.header(
					HttpHeaders.CONTENT_DISPOSITION,
					"attachment; filename=\"download\"; filename*=UTF-8''" + encodedFileName
				)
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(response.getFileData());
		} catch (IllegalArgumentException exception) {
			// 요청값 오류는 400 응답으로 반환합니다.
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (IllegalStateException exception) {
			// 내부 다운로드 오류는 500 응답으로 반환합니다.
			return ResponseEntity.internalServerError().body(Map.of("message", exception.getMessage()));
		}
	}

	// 현재 요청이 인증된 사용자 요청인지 확인합니다.
	private boolean isAuthenticatedRequest() {
		// 익명 인증 또는 미인증 상태는 다운로드를 허용하지 않습니다.
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null
			&& authentication.isAuthenticated()
			&& !(authentication instanceof AnonymousAuthenticationToken);
	}

	// 로그인 필요 응답을 공통 형식으로 반환합니다.
	private ResponseEntity<Object> unauthorizedResponse() {
		// 인증되지 않은 요청은 401 응답으로 반환합니다.
		return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
	}

	// attachment 헤더용 파일명을 UTF-8 RFC5987 형식으로 인코딩합니다.
	private String encodeAttachmentFileName(String fileName) {
		// 한글과 공백이 포함된 파일명도 브라우저가 안전하게 해석할 수 있게 변환합니다.
		return URLEncoder.encode(fileName == null ? "download" : fileName, StandardCharsets.UTF_8)
			.replace("+", "%20");
	}
}
