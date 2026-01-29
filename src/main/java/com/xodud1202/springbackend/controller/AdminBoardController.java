package com.xodud1202.springbackend.controller;

import com.xodud1202.springbackend.domain.admin.board.BoardPO;
import com.xodud1202.springbackend.domain.common.FtpProperties;
import com.xodud1202.springbackend.service.BoardService;
import com.xodud1202.springbackend.service.FtpFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AdminBoardController {
	private final BoardService boardService;
	private final FtpFileService ftpFileService;
	private final FtpProperties ftpProperties;

	// 관리자 게시판 목록을 조회합니다.
	@GetMapping("/api/admin/board/list")
	public ResponseEntity<Map<String, Object>> getBoardList(BoardPO param) {
		return ResponseEntity.ok(boardService.getAdminBoardList(param));
	}

	// 관리자 게시판 상세 정보를 조회합니다.
	@GetMapping("/api/admin/board/detail")
	public ResponseEntity<Object> getBoardDetail(BoardPO param) {
		return ResponseEntity.ok(boardService.getAdminBoardDetail(param));
	}

	// 관리자 게시판 정보를 수정합니다.
	@PostMapping("/api/admin/board/update")
	public ResponseEntity<Object> updateBoard(@RequestBody BoardPO param) {
		return ResponseEntity.ok(boardService.updateAdminBoard(param));
	}

	// 관리자 게시판에 게시글을 등록합니다.
	@PostMapping("/api/admin/board/create")
	public ResponseEntity<Object> createBoard(@RequestBody BoardPO param) {
		return ResponseEntity.ok(boardService.insertAdminBoard(param));
	}

	// 관리자 게시글을 삭제 처리합니다.
	@PostMapping("/api/admin/board/delete")
	public ResponseEntity<Object> deleteBoard(@RequestBody BoardPO param) {
		return ResponseEntity.ok(boardService.deleteAdminBoard(param));
	}

	// 게시글 이미지를 업로드합니다.
	@PostMapping("/api/admin/board/image/upload")
	public ResponseEntity<Object> uploadBoardImage(
			@RequestParam("image") MultipartFile image,
			@RequestParam(value = "boardNo", required = false) Long boardNo
	) {
		try {
			String validationError = validateBoardImage(image);
			if (validationError != null) {
				return ResponseEntity.badRequest().body(Map.of("error", validationError));
			}

			String imageUrl = boardNo == null
					? ftpFileService.uploadBoardRegImage(image)
					: ftpFileService.uploadBoardImage(image, boardNo);
			return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
		} catch (IOException e) {
			return ResponseEntity.internalServerError()
					.body(Map.of("error", "이미지 업로드 실패: " + e.getMessage()));
		}
	}

	// 게시글 이미지를 조회합니다.
	@GetMapping("/api/admin/board/image/{boardNo}/{fileName}")
	public ResponseEntity<byte[]> getBoardImage(
			@PathVariable("boardNo") Long boardNo,
			@PathVariable("fileName") String fileName
	) {
		try {
			Path filePath = Paths.get(ftpProperties.getUploadBoardTargetPath(), String.valueOf(boardNo), fileName);
			if (!Files.exists(filePath)) {
				return ResponseEntity.notFound().build();
			}
			String contentType = Files.probeContentType(filePath);
			byte[] fileBytes = Files.readAllBytes(filePath);
			return ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_TYPE, contentType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType)
					.body(fileBytes);
		} catch (IOException e) {
			return ResponseEntity.internalServerError().build();
		}
	}

	// 게시글 이미지 업로드 유효성 검사를 수행합니다.
	private String validateBoardImage(MultipartFile image) {
		if (image.isEmpty()) {
			return "이미지 파일이 없습니다.";
		}

		long maxSizeInBytes = (long) ftpProperties.getUploadBoardMaxSize() * 1024 * 1024;
		if (image.getSize() > maxSizeInBytes) {
			return "파일 크기가 " + ftpProperties.getUploadBoardMaxSize() + "MB를 초과합니다.";
		}

		String originalFilename = image.getOriginalFilename();
		if (originalFilename == null) {
			return "파일명이 올바르지 않습니다.";
		}

		String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
		String allowedExtensions = ftpProperties.getUploadBoardAllowExtension();
		if (!allowedExtensions.contains(extension)) {
			return "허용되지 않은 파일 형식입니다. 허용 형식: " + allowedExtensions;
		}

		return null;
	}
}
