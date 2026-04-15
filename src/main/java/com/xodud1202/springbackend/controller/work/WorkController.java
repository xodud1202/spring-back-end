package com.xodud1202.springbackend.controller.work;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xodud1202.springbackend.common.work.WorkSessionPolicy;
import com.xodud1202.springbackend.domain.admin.common.UserInfoVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkCompanyVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkDetailResponseVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkDetailUpdatePO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkFileVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkImportPO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkImportResponseVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkManualCreatePO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkManualCreateResponseVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkProjectVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkReplyDeletePO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkReplyFileDownloadVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkReplySavePO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkReplyUpdatePO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkReplyVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkStatusListResponseVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkStatusSectionPageResponseVO;
import com.xodud1202.springbackend.domain.common.CommonCodeVO;
import com.xodud1202.springbackend.domain.work.WorkBootstrapResponseVO;
import com.xodud1202.springbackend.domain.work.WorkFileDeletePO;
import com.xodud1202.springbackend.service.CompanyWorkService;
import com.xodud1202.springbackend.service.UserBaseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
// 업무관리 워크스페이스 화면에서 사용하는 인증 후 API를 제공합니다.
public class WorkController {
	private final CompanyWorkService companyWorkService;
	private final UserBaseService userBaseService;
	private final ObjectMapper objectMapper;

	@GetMapping("/api/work/bootstrap")
	// 업무관리 화면 초기 구동 데이터를 조회합니다.
	public ResponseEntity<WorkBootstrapResponseVO> getBootstrap(HttpServletRequest request) {
		try {
			// 로그인 사용자 기준으로 초기 화면에 필요한 선택 목록과 사용자 정보를 함께 반환합니다.
			Long workUserNo = resolveRequiredWorkUserNo(request);
			List<AdminCompanyWorkCompanyVO> companyList = companyWorkService.getAdminCompanyWorkCompanyList();
			Integer initialCompanySeq = companyList.isEmpty() ? null : companyList.get(0).getWorkCompanySeq();
			List<AdminCompanyWorkProjectVO> projectList = initialCompanySeq == null
				? List.of()
				: companyWorkService.getAdminCompanyWorkProjectList(initialCompanySeq);
			List<CommonCodeVO> workStatList = companyWorkService.getWorkStatusCodeList();
			List<CommonCodeVO> workPriorList = companyWorkService.getWorkPriorityCodeList();
			UserInfoVO currentUser = userBaseService.getUserInfoByUsrNo(workUserNo).orElse(null);

			WorkBootstrapResponseVO response = new WorkBootstrapResponseVO();
			response.setCurrentUser(currentUser);
			response.setCompanyList(companyList);
			response.setProjectList(projectList);
			response.setWorkStatList(workStatList);
			response.setWorkPriorList(workPriorList);
			return ResponseEntity.ok(response);
		} catch (SecurityException | IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("업무관리 bootstrap 조회 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("업무관리 초기 데이터 조회에 실패했습니다.", exception);
		}
	}

	@GetMapping("/api/work/project/list")
	// 선택 회사 기준 프로젝트 목록을 조회합니다.
	public ResponseEntity<List<AdminCompanyWorkProjectVO>> getProjectList(
		HttpServletRequest request,
		@RequestParam(required = false) Integer workCompanySeq
	) {
		try {
			// 로그인 세션을 확인한 뒤 대상 회사의 프로젝트 목록을 반환합니다.
			resolveRequiredWorkUserNo(request);
			return ResponseEntity.ok(companyWorkService.getAdminCompanyWorkProjectList(workCompanySeq));
		} catch (SecurityException | IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("업무관리 프로젝트 목록 조회 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("프로젝트 목록 조회에 실패했습니다.", exception);
		}
	}

	@GetMapping("/api/work/list")
	// 회사/프로젝트/검색 조건 기준 상태별 업무 목록을 조회합니다.
	public ResponseEntity<AdminCompanyWorkStatusListResponseVO> getWorkList(
		HttpServletRequest request,
		@RequestParam(required = false) Integer workCompanySeq,
		@RequestParam(required = false) Integer workCompanyProjectSeq,
		@RequestParam(required = false) String title,
		@RequestParam(required = false) String includeBodyYn,
		@RequestParam(required = false) List<String> workStatCdList,
		@RequestParam(required = false) Integer sectionSize
	) {
		try {
			// 로그인 사용자 기준으로 상태 포함 전체 목록을 반환합니다.
			resolveRequiredWorkUserNo(request);
			return ResponseEntity.ok(
				companyWorkService.getWorkCompanyStatusList(
					workCompanySeq,
					workCompanyProjectSeq,
					title,
					includeBodyYn,
					workStatCdList,
					sectionSize
				)
			);
		} catch (SecurityException | IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("업무관리 목록 조회 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("업무 목록 조회에 실패했습니다.", exception);
		}
	}

	@GetMapping("/api/work/list/section")
	// 회사/프로젝트/검색 조건 기준 특정 상태의 추가 업무 목록을 조회합니다.
	public ResponseEntity<AdminCompanyWorkStatusSectionPageResponseVO> getWorkListSection(
		HttpServletRequest request,
		@RequestParam(required = false) Integer workCompanySeq,
		@RequestParam(required = false) Integer workCompanyProjectSeq,
		@RequestParam(required = false) String title,
		@RequestParam(required = false) String includeBodyYn,
		@RequestParam(required = false) String workStatCd,
		@RequestParam(required = false) Integer offset,
		@RequestParam(required = false) Integer limit
	) {
		try {
			// 로그인 사용자 기준으로 특정 상태의 다음 업무 목록을 반환합니다.
			resolveRequiredWorkUserNo(request);
			return ResponseEntity.ok(
				companyWorkService.getWorkCompanyStatusSectionPage(
					workCompanySeq,
					workCompanyProjectSeq,
					title,
					includeBodyYn,
					workStatCd,
					offset,
					limit
				)
			);
		} catch (SecurityException | IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("업무관리 상태별 추가 목록 조회 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("업무 추가 목록 조회에 실패했습니다.", exception);
		}
	}

	@GetMapping("/api/work/detail")
	// 선택 업무의 상세, 첨부파일, 댓글 정보를 조회합니다.
	public ResponseEntity<AdminCompanyWorkDetailResponseVO> getDetail(
		HttpServletRequest request,
		@RequestParam(required = false) Long workSeq
	) {
		try {
			// 로그인 사용자 기준으로 상세 데이터를 반환합니다.
			resolveRequiredWorkUserNo(request);
			return ResponseEntity.ok(companyWorkService.getCompanyWorkDetail(workSeq));
		} catch (SecurityException | IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("업무관리 상세 조회 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("업무 상세 조회에 실패했습니다.", exception);
		}
	}

	@PostMapping(value = "/api/work/manual", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	// 업무를 수기로 등록하고 첨부파일을 함께 저장합니다.
	public ResponseEntity<AdminCompanyWorkManualCreateResponseVO> createManual(
		HttpServletRequest request,
		@RequestPart("payload") String payload,
		@RequestPart(value = "files", required = false) List<MultipartFile> files
	) {
		try {
			// 로그인 사용자번호를 등록자/수정자에 강제로 반영해 수기 등록을 수행합니다.
			Long workUserNo = resolveRequiredWorkUserNo(request);
			AdminCompanyWorkManualCreatePO command = objectMapper.readValue(payload, AdminCompanyWorkManualCreatePO.class);
			command.setRegNo(workUserNo);
			command.setUdtNo(workUserNo);
			return ResponseEntity.ok(companyWorkService.createAdminCompanyWorkManual(command, files, workUserNo));
		} catch (SecurityException | IllegalArgumentException exception) {
			throw exception;
		} catch (IOException exception) {
			throw new IllegalArgumentException("수기 등록 요청 정보를 확인해주세요.");
		} catch (Exception exception) {
			log.error("업무관리 수기 등록 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("업무 등록에 실패했습니다.", exception);
		}
	}

	@PostMapping("/api/work/import")
	// 외부 SR 업무를 가져와 저장합니다.
	public ResponseEntity<AdminCompanyWorkImportResponseVO> importWork(
		HttpServletRequest request,
		@Valid @RequestBody AdminCompanyWorkImportPO command
	) {
		try {
			// 로그인 사용자번호를 등록자/수정자에 강제로 반영해 가져오기를 수행합니다.
			Long workUserNo = resolveRequiredWorkUserNo(request);
			command.setRegNo(workUserNo);
			command.setUdtNo(workUserNo);
			return ResponseEntity.ok(companyWorkService.importAdminCompanyWork(command));
		} catch (SecurityException | IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("업무관리 SR 가져오기 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("SR 가져오기에 실패했습니다.", exception);
		}
	}

	@PostMapping(value = "/api/work/detail/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	// 업무 상세 수정과 업무 첨부 변경을 함께 저장합니다.
	public ResponseEntity<AdminCompanyWorkDetailResponseVO> updateDetail(
		HttpServletRequest request,
		@RequestPart("payload") String payload,
		@RequestPart(value = "files", required = false) List<MultipartFile> files
	) {
		try {
			// 로그인 사용자번호를 수정자에 강제로 반영해 상세 저장을 수행합니다.
			Long workUserNo = resolveRequiredWorkUserNo(request);
			AdminCompanyWorkDetailUpdatePO command = objectMapper.readValue(payload, AdminCompanyWorkDetailUpdatePO.class);
			command.setUdtNo(workUserNo);
			return ResponseEntity.ok(companyWorkService.updateAdminCompanyWorkDetail(command, files, workUserNo));
		} catch (SecurityException | IllegalArgumentException exception) {
			throw exception;
		} catch (IOException exception) {
			throw new IllegalArgumentException("업무 상세 저장 요청 정보를 확인해주세요.");
		} catch (Exception exception) {
			log.error("업무관리 상세 저장 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("업무 상세 저장에 실패했습니다.", exception);
		}
	}

	@PostMapping(value = "/api/work/file/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	// 업무 첨부파일을 업로드합니다.
	public ResponseEntity<AdminCompanyWorkFileVO> uploadWorkFile(
		HttpServletRequest request,
		@RequestParam(required = false) Long workSeq,
		@RequestPart("file") MultipartFile file
	) {
		try {
			// 로그인 사용자번호 기준으로 단건 업무 첨부파일을 저장합니다.
			Long workUserNo = resolveRequiredWorkUserNo(request);
			return ResponseEntity.ok(companyWorkService.uploadAdminCompanyWorkFile(workSeq, file, workUserNo));
		} catch (SecurityException | IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("업무관리 첨부 업로드 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("업무 첨부파일 업로드에 실패했습니다.", exception);
		}
	}

	@PostMapping("/api/work/file/delete")
	// 업무 첨부파일을 삭제 처리합니다.
	public ResponseEntity<Map<String, String>> deleteWorkFile(
		HttpServletRequest request,
		@Valid @RequestBody WorkFileDeletePO command
	) {
		try {
			// 로그인 사용자번호 기준으로 대상 업무 첨부파일을 삭제합니다.
			Long workUserNo = resolveRequiredWorkUserNo(request);
			companyWorkService.deleteAdminCompanyWorkFile(command, workUserNo);
			return ResponseEntity.ok(Map.of("message", "업무 첨부파일을 삭제했습니다."));
		} catch (SecurityException | IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("업무관리 첨부 삭제 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("업무 첨부파일 삭제에 실패했습니다.", exception);
		}
	}

	@PostMapping(value = "/api/work/reply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	// 댓글과 댓글 첨부파일을 함께 등록합니다.
	public ResponseEntity<AdminCompanyWorkReplyVO> createReply(
		HttpServletRequest request,
		@RequestPart("payload") String payload,
		@RequestPart(value = "files", required = false) List<MultipartFile> files
	) {
		try {
			// 로그인 사용자번호를 등록자/수정자에 강제로 반영해 댓글을 등록합니다.
			Long workUserNo = resolveRequiredWorkUserNo(request);
			AdminCompanyWorkReplySavePO command = objectMapper.readValue(payload, AdminCompanyWorkReplySavePO.class);
			command.setRegNo(workUserNo);
			command.setUdtNo(workUserNo);
			return ResponseEntity.ok(companyWorkService.saveAdminCompanyWorkReply(command, files, workUserNo));
		} catch (SecurityException | IllegalArgumentException exception) {
			throw exception;
		} catch (IOException exception) {
			throw new IllegalArgumentException("댓글 등록 요청 정보를 확인해주세요.");
		} catch (Exception exception) {
			log.error("업무관리 댓글 등록 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("댓글 등록에 실패했습니다.", exception);
		}
	}

	@PostMapping(value = "/api/work/reply/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	// 댓글과 댓글 첨부파일 변경을 함께 저장합니다.
	public ResponseEntity<AdminCompanyWorkReplyVO> updateReply(
		HttpServletRequest request,
		@RequestPart("payload") String payload,
		@RequestPart(value = "files", required = false) List<MultipartFile> files
	) {
		try {
			// 로그인 사용자번호를 수정자에 강제로 반영해 댓글 수정을 수행합니다.
			Long workUserNo = resolveRequiredWorkUserNo(request);
			AdminCompanyWorkReplyUpdatePO command = objectMapper.readValue(payload, AdminCompanyWorkReplyUpdatePO.class);
			command.setUdtNo(workUserNo);
			return ResponseEntity.ok(companyWorkService.updateAdminCompanyWorkReply(command, files, workUserNo));
		} catch (SecurityException | IllegalArgumentException exception) {
			throw exception;
		} catch (IOException exception) {
			throw new IllegalArgumentException("댓글 수정 요청 정보를 확인해주세요.");
		} catch (Exception exception) {
			log.error("업무관리 댓글 수정 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("댓글 수정에 실패했습니다.", exception);
		}
	}

	@PostMapping("/api/work/reply/delete")
	// 댓글을 삭제 처리합니다.
	public ResponseEntity<Map<String, String>> deleteReply(
		HttpServletRequest request,
		@Valid @RequestBody AdminCompanyWorkReplyDeletePO command
	) {
		try {
			// 로그인 사용자번호를 수정자에 강제로 반영해 댓글 삭제를 수행합니다.
			Long workUserNo = resolveRequiredWorkUserNo(request);
			command.setUdtNo(workUserNo);
			companyWorkService.deleteAdminCompanyWorkReply(command, workUserNo);
			return ResponseEntity.ok(Map.of("message", "댓글을 삭제했습니다."));
		} catch (SecurityException | IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("업무관리 댓글 삭제 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("댓글 삭제에 실패했습니다.", exception);
		}
	}

	@GetMapping("/api/work/reply/file/download")
	// 댓글 첨부파일을 다운로드합니다.
	public ResponseEntity<Object> downloadReplyFile(
		HttpServletRequest request,
		@RequestParam(required = false) Integer replyFileSeq
	) {
		try {
			// 로그인 사용자 기준으로 댓글 첨부파일 다운로드를 허용합니다.
			resolveRequiredWorkUserNo(request);
			AdminCompanyWorkReplyFileDownloadVO response = companyWorkService.downloadAdminCompanyWorkReplyFile(replyFileSeq);
			String encodedFileName = encodeAttachmentFileName(response.getReplyFileNm());
			return ResponseEntity.ok()
				.header(
					HttpHeaders.CONTENT_DISPOSITION,
					"attachment; filename=\"download\"; filename*=UTF-8''" + encodedFileName
				)
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(response.getFileData());
		} catch (SecurityException | IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("업무관리 댓글 첨부 다운로드 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("댓글 첨부파일 다운로드에 실패했습니다.", exception);
		}
	}

	// 세션에서 로그인된 업무관리 사용자번호를 읽어옵니다.
	private Long resolveRequiredWorkUserNo(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null) {
			throw new SecurityException("로그인이 필요합니다.");
		}

		Long workUserNo = WorkSessionPolicy.resolveWorkUserNo(session.getAttribute(WorkSessionPolicy.SESSION_ATTR_WORK_USER_NO));
		if (workUserNo != null) {
			return workUserNo;
		}
		throw new SecurityException("로그인이 필요합니다.");
	}

	// attachment 헤더용 파일명을 UTF-8 RFC5987 형식으로 인코딩합니다.
	private String encodeAttachmentFileName(String fileName) {
		// 한글과 공백이 포함된 파일명도 브라우저가 안전하게 해석할 수 있게 변환합니다.
		return URLEncoder.encode(fileName == null ? "download" : fileName, StandardCharsets.UTF_8)
			.replace("+", "%20");
	}
}
