package com.xodud1202.springbackend.controller;

import com.xodud1202.springbackend.domain.admin.common.AdminMenuLnb;
import com.xodud1202.springbackend.domain.common.CommonCodeVO;
import com.xodud1202.springbackend.domain.common.FtpProperties;
import com.xodud1202.springbackend.service.AdminCommonService;
import com.xodud1202.springbackend.service.FtpFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
	
	@GetMapping("/api/admin/menu/list")
	public ResponseEntity<List<AdminMenuLnb>> getResumeInfo() {
		return ResponseEntity.ok(adminCommonService.getAdminMenuLnbInfo());
	}

	@GetMapping("/api/admin/common/code")
	public ResponseEntity<List<CommonCodeVO>> getCommonCodeList(@RequestParam("grpCd") String grpCd) {
		return ResponseEntity.ok(adminCommonService.getCommonCodeList(grpCd));
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

		String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
		String allowedExtensions = ftpProperties.getUploadResumeAllowExtension();
		if (!allowedExtensions.contains(extension)) {
			return "허용되지 않은 파일 형식입니다. 허용 형식: " + allowedExtensions;
		}

		return null;
	}
}
