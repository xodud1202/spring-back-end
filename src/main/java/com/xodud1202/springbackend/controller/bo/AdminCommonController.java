package com.xodud1202.springbackend.controller.bo;

import static com.xodud1202.springbackend.common.util.CommonValidationUtils.isAllowedFileExtension;

import com.xodud1202.springbackend.common.snippet.SnippetSessionPolicy;
import com.xodud1202.springbackend.common.work.WorkSessionPolicy;
import com.xodud1202.springbackend.domain.admin.common.AdminMenuLnb;
import com.xodud1202.springbackend.domain.admin.common.CommonCodeManagePO;
import com.xodud1202.springbackend.domain.admin.common.MenuManageSavePO;
import com.xodud1202.springbackend.domain.admin.common.MenuManageVO;
import com.xodud1202.springbackend.domain.common.CommonCodeVO;
import com.xodud1202.springbackend.domain.common.FtpProperties;
import com.xodud1202.springbackend.security.SignedLoginTokenService;
import com.xodud1202.springbackend.service.AdminCommonService;
import com.xodud1202.springbackend.service.FtpFileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
public class AdminCommonController {
	
	private final AdminCommonService adminCommonService;
	private final FtpFileService ftpFileService;
	private final FtpProperties ftpProperties;
	private final SignedLoginTokenService signedLoginTokenService;
	
	@GetMapping("/api/admin/menu/list")
	public ResponseEntity<List<AdminMenuLnb>> getResumeInfo() {
		return ResponseEntity.ok(adminCommonService.getAdminMenuLnbInfo());
	}

	@GetMapping("/api/admin/menu/manage/list")
	public ResponseEntity<List<MenuManageVO>> getAdminMenuManageList() {
		return ResponseEntity.ok(adminCommonService.getAdminMenuManageList());
	}

	@GetMapping("/api/admin/menu/manage/detail")
	public ResponseEntity<Object> getAdminMenuManageDetail(@RequestParam(required = false) Integer menuNo) {
		if (menuNo == null || menuNo <= 0) {
			return ResponseEntity.badRequest().body(Map.of("message", "메뉴 번호를 확인해주세요."));
		}
		MenuManageVO detail = adminCommonService.getAdminMenuManageDetail(menuNo);
		if (detail == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(detail);
	}

	@PostMapping("/api/admin/menu/manage/create")
	public ResponseEntity<Object> createAdminMenu(@RequestBody MenuManageSavePO param) {
		String validationMessage = adminCommonService.validateAdminMenuCreate(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		return ResponseEntity.ok(adminCommonService.createAdminMenu(param));
	}

	@PostMapping("/api/admin/menu/manage/update")
	public ResponseEntity<Object> updateAdminMenu(@RequestBody MenuManageSavePO param) {
		String validationMessage = adminCommonService.validateAdminMenuUpdate(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		return ResponseEntity.ok(adminCommonService.updateAdminMenu(param));
	}

	@PostMapping("/api/admin/menu/manage/delete")
	public ResponseEntity<Object> deleteAdminMenu(@RequestBody MenuManageSavePO param) {
		String validationMessage = adminCommonService.validateAdminMenuDelete(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		return ResponseEntity.ok(adminCommonService.deleteAdminMenu(param));
	}

	@GetMapping("/api/admin/common/code")
	public ResponseEntity<List<CommonCodeVO>> getCommonCodeList(@RequestParam("grpCd") String grpCd) {
		return ResponseEntity.ok(adminCommonService.getCommonCodeList(grpCd));
	}

	/**
	 * 관리자 상위 공통 코드 목록을 조회합니다.
	 * @param grpCd 그룹코드 검색어(코드값 완전일치)
	 * @param grpCdNm 그룹코드명 검색어(접두어 일치)
	 * @return 상위 공통 코드 목록
	 */
	@GetMapping("/api/admin/common/code/manage/group/list")
	public ResponseEntity<List<CommonCodeVO>> getAdminRootCommonCodeList(
		@RequestParam(value = "grpCd", required = false) String grpCd,
		@RequestParam(value = "grpCdNm", required = false) String grpCdNm
	) {
		return ResponseEntity.ok(adminCommonService.getAdminRootCommonCodeList(grpCd, grpCdNm));
	}

	/**
	 * 관리자 하위 공통 코드 목록을 조회합니다.
	 * @param grpCd 조회할 상위 그룹코드
	 * @return 하위 공통 코드 목록
	 */
	@GetMapping("/api/admin/common/code/manage/child/list")
	public ResponseEntity<List<CommonCodeVO>> getAdminChildCommonCodeList(@RequestParam("grpCd") String grpCd) {
		return ResponseEntity.ok(adminCommonService.getAdminChildCommonCodeList(grpCd));
	}

	/**
	 * 관리자 공통 코드를 등록합니다.
	 * @param param 공통 코드 저장 정보
	 * @return 등록 결과
	 */
	@PostMapping("/api/admin/common/code/manage/create")
	public ResponseEntity<Object> createAdminCommonCode(@RequestBody CommonCodeManagePO param) {
		String validationMessage = adminCommonService.validateAdminCommonCodeCreate(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		return ResponseEntity.ok(adminCommonService.createAdminCommonCode(param));
	}

	/**
	 * 관리자 공통 코드를 수정합니다.
	 * @param param 공통 코드 저장 정보
	 * @return 수정 결과
	 */
	@PostMapping("/api/admin/common/code/manage/update")
	public ResponseEntity<Object> updateAdminCommonCode(@RequestBody CommonCodeManagePO param) {
		String validationMessage = adminCommonService.validateAdminCommonCodeUpdate(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		return ResponseEntity.ok(adminCommonService.updateAdminCommonCode(param));
	}

	/**
	 * 이력서용 이미지 업로드를 처리합니다.
	 * @param image 업로드할 이미지 파일
	 * @param usrNo 사용자 번호
	 * @return 업로드 처리 결과
	 */
	@PostMapping("/api/upload/image")
	public ResponseEntity<?> uploadImage(
			@RequestParam("image") MultipartFile image,
			@RequestParam("usrNo") String usrNo
	) {
		return handleResumeImageUpload(image, usrNo, "faceImgPath");
	}

	/**
	 * 학력 로고 업로드를 처리합니다.
	 * @param image 업로드할 이미지 파일
	 * @param usrNo 사용자 번호
	 * @return 업로드 처리 결과
	 */
	@PostMapping("/api/upload/education-logo")
	public ResponseEntity<?> uploadEducationLogo(
			@RequestParam("image") MultipartFile image,
			@RequestParam("usrNo") String usrNo
	) {
		return handleResumeEducationLogoUpload(image, usrNo);
	}

	/**
	 * 에디터 이미지 업로드를 처리합니다.
	 * @param image 업로드할 이미지 파일
	 * @return 업로드 처리 결과
	 */
	@PostMapping("/api/upload/editor-image")
	public ResponseEntity<?> uploadEditorImage(
			@RequestParam("image") MultipartFile image,
			HttpServletRequest request
	) {
		if (!isEditorImageUploadAuthorized(request)) {
			return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
		}
		return handleEditorImageUpload(image);
	}

	/**
	 * 브랜드 로고 이미지 업로드를 처리합니다.
	 * @param image 업로드할 이미지 파일
	 * @param brandNo 브랜드 번호
	 * @return 업로드 처리 결과
	 */
	@PostMapping("/api/upload/brand-logo")
	public ResponseEntity<?> uploadBrandLogo(
			@RequestParam("image") MultipartFile image,
			@RequestParam("brandNo") String brandNo
	) {
		return handleBrandLogoUpload(image, brandNo);
	}

	/**
	 * 이력서 이미지 업로드를 공통 처리합니다.
	 * @param image 업로드할 이미지 파일
	 * @param usrNo 사용자 번호
	 * @param responseKey 응답에 담을 URL 키
	 * @return 업로드 처리 결과
	 */
	private ResponseEntity<?> handleResumeImageUpload(MultipartFile image, String usrNo, String responseKey) {
		try {
			String validationError = validateResumeImage(image);
			if (validationError != null) {
				return ResponseEntity.badRequest().body(Map.of("error", validationError));
			}

			String imageUrl = ftpFileService.uploadResumeImage(image, usrNo);

			Map<String, String> response = new HashMap<>();
			response.put(responseKey, imageUrl);
			response.put("message", "이미지 업로드가 완료되었습니다.");
			return ResponseEntity.ok(response);
		} catch (IOException e) {
			return ResponseEntity.internalServerError()
					.body(Map.of("error", "FTP 업로드 실패: " + e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.internalServerError()
					.body(Map.of("error", "이미지 업로드 실패: " + e.getMessage()));
		}
	}

	/**
	 * 학력 로고 이미지 업로드를 처리합니다.
	 * @param image 업로드할 이미지 파일
	 * @param usrNo 사용자 번호
	 * @return 업로드 처리 결과
	 */
	private ResponseEntity<?> handleResumeEducationLogoUpload(MultipartFile image, String usrNo) {
		try {
			String validationError = validateResumeImage(image);
			if (validationError != null) {
				return ResponseEntity.badRequest().body(Map.of("error", validationError));
			}

			String imageUrl = ftpFileService.uploadResumeEducationLogo(image, usrNo);

			Map<String, String> response = new HashMap<>();
			response.put("logoPath", imageUrl);
			response.put("message", "이미지 업로드가 완료되었습니다.");
			return ResponseEntity.ok(response);
		} catch (IOException e) {
			return ResponseEntity.internalServerError()
					.body(Map.of("error", "FTP 업로드 실패: " + e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.internalServerError()
					.body(Map.of("error", "이미지 업로드 실패: " + e.getMessage()));
		}
	}

	/**
	 * 에디터 이미지 업로드를 처리합니다.
	 * @param image 업로드할 이미지 파일
	 * @return 업로드 처리 결과
	 */
	private ResponseEntity<?> handleEditorImageUpload(MultipartFile image) {
		try {
			String validationError = validateEditorImage(image);
			if (validationError != null) {
				return ResponseEntity.badRequest().body(Map.of("error", validationError));
			}

			String imageUrl = ftpFileService.uploadEditorImage(image);

			Map<String, String> response = new HashMap<>();
			response.put("imageUrl", imageUrl);
			response.put("message", "이미지 업로드가 완료되었습니다.");
			return ResponseEntity.ok(response);
		} catch (IOException e) {
			return ResponseEntity.internalServerError()
					.body(Map.of("error", "FTP 업로드 실패: " + e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.internalServerError()
					.body(Map.of("error", "이미지 업로드 실패: " + e.getMessage()));
		}
	}

	/**
	 * 에디터 이미지 업로드 권한을 확인합니다.
	 * @param request 업로드 요청
	 * @return 업로드 허용 여부
	 */
	private boolean isEditorImageUploadAuthorized(HttpServletRequest request) {
		// 관리자 JWT 인증 또는 업무/스니펫 로그인 세션 중 하나가 있어야 업로드를 허용합니다.
		return isAdminJwtAuthenticated()
			|| resolveWorkUploadUserNo(request) != null
			|| resolveSnippetUploadUserNo(request) != null;
	}

	/**
	 * 현재 요청이 관리자 JWT 인증을 통과했는지 확인합니다.
	 * @return 관리자 인증 여부
	 */
	private boolean isAdminJwtAuthenticated() {
		// Spring Security 컨텍스트에 익명 사용자가 아닌 인증 정보가 있으면 관리자 JWT로 간주합니다.
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null
			&& authentication.isAuthenticated()
			&& !"anonymousUser".equals(String.valueOf(authentication.getPrincipal()));
	}

	/**
	 * 업무관리 로그인 사용자번호를 세션 또는 서명 쿠키에서 조회합니다.
	 * @param request 업로드 요청
	 * @return 인증된 업무관리 사용자번호
	 */
	private Long resolveWorkUploadUserNo(HttpServletRequest request) {
		// 기존 업무관리 세션을 우선 사용하고, 없으면 서명 쿠키로 복구합니다.
		HttpSession session = request == null ? null : request.getSession(false);
		Long sessionUserNo = session == null ? null : WorkSessionPolicy.resolveWorkUserNo(session.getAttribute(WorkSessionPolicy.SESSION_ATTR_WORK_USER_NO));
		if (sessionUserNo != null) {
			return sessionUserNo;
		}
		return normalizePositiveUserNo(WorkSessionPolicy.resolveWorkUserNoFromRequest(request, signedLoginTokenService));
	}

	/**
	 * 스니펫 로그인 사용자번호를 세션 또는 서명 쿠키에서 조회합니다.
	 * @param request 업로드 요청
	 * @return 인증된 스니펫 사용자번호
	 */
	private Long resolveSnippetUploadUserNo(HttpServletRequest request) {
		// 기존 스니펫 세션을 우선 사용하고, 없으면 서명 쿠키로 복구합니다.
		HttpSession session = request == null ? null : request.getSession(false);
		Long sessionUserNo = session == null ? null : SnippetSessionPolicy.resolveSnippetUserNo(session.getAttribute(SnippetSessionPolicy.SESSION_ATTR_SNIPPET_USER_NO));
		if (sessionUserNo != null) {
			return sessionUserNo;
		}
		return normalizePositiveUserNo(SnippetSessionPolicy.resolveSnippetUserNoFromRequest(request, signedLoginTokenService));
	}

	/**
	 * 인증 사용자번호를 양수 Long 또는 null로 정규화합니다.
	 * @param userNo 인증 정책에서 해석한 사용자번호
	 * @return 양수 사용자번호 또는 null
	 */
	private Long normalizePositiveUserNo(Long userNo) {
		// 목 객체 기본값이나 비정상 토큰 결과가 인증으로 취급되지 않도록 양수만 허용합니다.
		return userNo == null || userNo < 1L ? null : userNo;
	}

	/**
	 * 브랜드 로고 이미지 업로드를 처리합니다.
	 * @param image 업로드할 이미지 파일
	 * @param brandNo 브랜드 번호
	 * @return 업로드 처리 결과
	 */
	private ResponseEntity<?> handleBrandLogoUpload(MultipartFile image, String brandNo) {
		try {
			String validationError = validateBrandLogoImage(image, brandNo);
			if (validationError != null) {
				log.warn(
						"브랜드 로고 업로드 검증 실패. brandNo={}, originalFilename={}, size={}, error={}",
						sanitizeLogValue(brandNo),
						getMultipartOriginalFilename(image),
						getMultipartSize(image),
						validationError
				);
				return ResponseEntity.badRequest().body(Map.of("error", validationError));
			}

			String imageUrl = ftpFileService.uploadBrandLogo(image, brandNo);

			Map<String, String> response = new HashMap<>();
			response.put("brandLogoPath", imageUrl);
			response.put("message", "이미지 업로드가 완료되었습니다.");
			return ResponseEntity.ok(response);
		} catch (IOException e) {
			log.error("브랜드 로고 업로드 중 FTP 오류가 발생했습니다. brandNo={}", sanitizeLogValue(brandNo), e);
			return ResponseEntity.internalServerError()
					.body(Map.of("error", "FTP 업로드 실패: " + e.getMessage()));
		} catch (Exception e) {
			log.error("브랜드 로고 업로드 중 서버 오류가 발생했습니다. brandNo={}", sanitizeLogValue(brandNo), e);
			return ResponseEntity.internalServerError()
					.body(Map.of("error", "이미지 업로드 실패: " + e.getMessage()));
		}
	}

	/**
	 * 이미지 업로드 유효성 검사를 수행합니다.
	 * @param image 업로드할 이미지 파일
	 * @return 오류 메시지(정상일 경우 null)
	 */
	private String validateResumeImage(MultipartFile image) {
		if (image.isEmpty()) {
			return "이미지 파일이 없습니다.";
		}

		long maxSizeInBytes = (long) ftpProperties.getUploadResumeMaxSize() * 1024 * 1024;
		if (image.getSize() > maxSizeInBytes) {
			return "파일 크기가 " + ftpProperties.getUploadResumeMaxSize() + "MB를 초과합니다.";
		}

		String originalFilename = image.getOriginalFilename();
		if (originalFilename == null) {
			return "파일명이 올바르지 않습니다.";
		}

		String allowedExtensions = ftpProperties.getUploadResumeAllowExtension();
		if (!isAllowedFileExtension(allowedExtensions, originalFilename)) {
			return "허용되지 않은 파일 형식입니다. 허용 형식: " + allowedExtensions;
		}

		return null;
	}

	/**
	 * 에디터 이미지 업로드 유효성 검사를 수행합니다.
	 * @param image 업로드할 이미지 파일
	 * @return 오류 메시지(정상일 경우 null)
	 */
	private String validateEditorImage(MultipartFile image) {
		if (image.isEmpty()) {
			return "이미지 파일이 없습니다.";
		}

		long maxSizeInBytes = (long) ftpProperties.getUploadEditorMaxSize() * 1024 * 1024;
		if (image.getSize() > maxSizeInBytes) {
			return "파일 크기가 " + ftpProperties.getUploadEditorMaxSize() + "MB를 초과합니다.";
		}

		String originalFilename = image.getOriginalFilename();
		if (originalFilename == null) {
			return "파일명이 올바르지 않습니다.";
		}

		String allowedExtensions = ftpProperties.getUploadEditorAllowExtension();
		if (!isAllowedFileExtension(allowedExtensions, originalFilename)) {
			return "허용되지 않은 파일 형식입니다. 허용 형식: " + allowedExtensions;
		}

		return null;
	}

	/**
	 * 브랜드 로고 이미지 업로드 유효성 검사를 수행합니다.
	 * @param image 업로드할 이미지 파일
	 * @param brandNo 브랜드 번호
	 * @return 오류 메시지(정상일 경우 null)
	 */
	private String validateBrandLogoImage(MultipartFile image, String brandNo) {
		if (brandNo == null || brandNo.trim().isEmpty()) {
			return "브랜드 번호가 없습니다.";
		}

		if (image.isEmpty()) {
			return "이미지 파일이 없습니다.";
		}

		if (ftpProperties.getUploadBrandMaxSize() <= 0) {
			return "브랜드 로고 업로드 설정이 올바르지 않습니다.";
		}

		long maxSizeInBytes = (long) ftpProperties.getUploadBrandMaxSize() * 1024 * 1024;
		if (image.getSize() > maxSizeInBytes) {
			return "파일 크기가 " + ftpProperties.getUploadBrandMaxSize() + "MB를 초과합니다.";
		}

		String originalFilename = image.getOriginalFilename();
		if (originalFilename == null) {
			return "파일명이 올바르지 않습니다.";
		}

		String allowedExtensions = ftpProperties.getUploadBrandAllowExtension();
		if (allowedExtensions == null || allowedExtensions.trim().isEmpty()) {
			return "브랜드 로고 업로드 설정이 올바르지 않습니다.";
		}
		if (!isAllowedFileExtension(allowedExtensions, originalFilename)) {
			return "허용되지 않는 파일 형식입니다. 허용 형식: " + allowedExtensions;
		}

		return null;
	}

	/**
	 * 멀티파트 파일의 원본 파일명을 로그용 문자열로 변환합니다.
	 * @param image 업로드 파일
	 * @return 로그에 남길 파일명
	 */
	private String getMultipartOriginalFilename(MultipartFile image) {
		if (image == null || image.getOriginalFilename() == null) {
			return "";
		}
		return image.getOriginalFilename();
	}

	/**
	 * 멀티파트 파일 크기를 로그용 값으로 변환합니다.
	 * @param image 업로드 파일
	 * @return 로그에 남길 파일 크기
	 */
	private long getMultipartSize(MultipartFile image) {
		if (image == null) {
			return -1L;
		}
		return image.getSize();
	}

	/**
	 * 로그에 남길 문자열 값을 정리합니다.
	 * @param value 로그 대상 값
	 * @return 공백 제거된 문자열
	 */
	private String sanitizeLogValue(String value) {
		if (value == null) {
			return "";
		}
		return value.trim();
	}
}
