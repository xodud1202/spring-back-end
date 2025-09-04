package com.xodud1202.springbackend.controller;

import com.xodud1202.springbackend.domain.admin.common.AdminMenuLnb;
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
	
	@PostMapping("/api/upload/image")
	public ResponseEntity<?> uploadImage(
			@RequestParam("image") MultipartFile image,
			@RequestParam("usrNo") String usrNo
	) {
		try {
			// 파일이 비어있는지 확인
			if (image.isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of("error", "이미지 파일이 없습니다."));
			}
			
			// 파일 크기 확인 (resume 설정의 max size 사용)
			long maxSizeInBytes = (long) ftpProperties.getUploadResumeMaxSize() * 1024 * 1024; // MB to bytes
			if (image.getSize() > maxSizeInBytes) {
				return ResponseEntity.badRequest()
						.body(Map.of("error", "파일 크기가 " + ftpProperties.getUploadResumeMaxSize() + "MB를 초과합니다."));
			}
			
			// 허용된 확장자 확인
			String originalFilename = image.getOriginalFilename();
			if (originalFilename == null) {
				return ResponseEntity.badRequest().body(Map.of("error", "파일명이 올바르지 않습니다."));
			}
			
			String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
			String allowedExtensions = ftpProperties.getUploadResumeAllowExtension();
			if (!allowedExtensions.contains(extension)) {
				return ResponseEntity.badRequest()
						.body(Map.of("error", "허용되지 않은 파일 형식입니다. 허용 형식: " + allowedExtensions));
			}
			
			// FTP 업로드
			String imageUrl = ftpFileService.uploadResumeImage(image, usrNo);
			
			// 성공 응답
			Map<String, String> response = new HashMap<>();
			response.put("faceImgPath", imageUrl);
			response.put("message", "이미지 업로드 성공");
			
			return ResponseEntity.ok(response);
			
		} catch (IOException e) {
			return ResponseEntity.internalServerError()
					.body(Map.of("error", "FTP 업로드 실패: " + e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.internalServerError()
					.body(Map.of("error", "이미지 업로드 실패: " + e.getMessage()));
		}
	}
}
