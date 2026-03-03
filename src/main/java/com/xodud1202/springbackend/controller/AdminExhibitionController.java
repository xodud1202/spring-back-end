package com.xodud1202.springbackend.controller;

import com.xodud1202.springbackend.domain.admin.exhibition.ExhibitionDeletePO;
import com.xodud1202.springbackend.domain.admin.exhibition.ExhibitionDetailVO;
import com.xodud1202.springbackend.domain.admin.exhibition.ExhibitionSavePO;
import com.xodud1202.springbackend.service.ExhibitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Validated
@RestController
@RequiredArgsConstructor
// 관리자 기획전 API를 제공합니다.
public class AdminExhibitionController {
	private final ExhibitionService exhibitionService;

	// 관리자 기획전 목록을 조회합니다.
	@GetMapping("/api/admin/exhibition/list")
	public ResponseEntity<Object> getExhibitionList(
		@RequestParam(required = false) Integer page,
		@RequestParam(required = false) String searchGb,
		@RequestParam(required = false) String searchValue,
		@RequestParam(required = false) String searchStartDt,
		@RequestParam(required = false) String searchEndDt,
		@RequestParam(required = false) Integer pageSize
	) {
		try {
			return ResponseEntity.ok(exhibitionService.getAdminExhibitionList(page, searchGb, searchValue, searchStartDt, searchEndDt, pageSize));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
		}
	}

	// 관리자 기획전 상세를 조회합니다.
	@GetMapping("/api/admin/exhibition/detail")
	public ResponseEntity<Object> getExhibitionDetail(@RequestParam Integer exhibitionNo) {
		// 기획전 번호 유효성을 확인합니다.
		if (exhibitionNo == null || exhibitionNo < 1) {
			return ResponseEntity.badRequest().body(Map.of("message", "기획전 번호를 확인해주세요."));
		}
		ExhibitionDetailVO detail = exhibitionService.getAdminExhibitionDetail(exhibitionNo);
		if (detail == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(detail);
	}

	// 엑셀 업로드 파일로 기획전 상품 데이터를 파싱합니다.
	@PostMapping(value = "/api/admin/exhibition/goods/excel/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Object> parseExhibitionGoodsExcel(@RequestParam("file") MultipartFile file) {
		// 엑셀 파싱 요청값을 확인합니다.
		String validationMessage = exhibitionService.validateExhibitionGoodsExcelUpload(file);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		try {
			return ResponseEntity.ok(exhibitionService.parseExhibitionGoodsExcel(file));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
		} catch (IOException e) {
			return ResponseEntity.internalServerError().body(Map.of("message", "엑셀 파싱에 실패했습니다."));
		}
	}

	// 기획전 상품 엑셀 템플릿을 다운로드합니다.
	@GetMapping("/api/admin/exhibition/goods/excel/download")
	public ResponseEntity<Object> downloadExhibitionGoodsExcel(
		@RequestParam(value = "exhibitionNo", required = false) Integer exhibitionNo,
		@RequestParam(value = "exhibitionTabNo", required = false) Integer exhibitionTabNo
	) {
		try {
			byte[] data = exhibitionService.buildExhibitionGoodsExcelTemplate(exhibitionNo, exhibitionTabNo);
			String filename = "exhibition_goods_template.xlsx";
			return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
				.contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.body(data);
		} catch (IOException e) {
			return ResponseEntity.internalServerError().body(Map.of("message", "엑셀 다운로드에 실패했습니다."));
		}
	}

	// 기획전을 등록합니다.
	@PostMapping("/api/admin/exhibition/create")
	public ResponseEntity<Object> createExhibition(@RequestBody ExhibitionSavePO param) {
		// 기획전 등록 요청값을 확인합니다.
		String validationMessage = exhibitionService.validateExhibitionCreate(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		try {
			return ResponseEntity.ok(exhibitionService.createExhibition(param));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
		}
	}

	// 기획전을 수정합니다.
	@PostMapping("/api/admin/exhibition/update")
	public ResponseEntity<Object> updateExhibition(@RequestBody ExhibitionSavePO param) {
		// 기획전 수정 요청값을 확인합니다.
		String validationMessage = exhibitionService.validateExhibitionUpdate(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		try {
			return ResponseEntity.ok(exhibitionService.updateExhibition(param));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
		}
	}

	// 기획전을 삭제합니다.
	@PostMapping("/api/admin/exhibition/delete")
	public ResponseEntity<Object> deleteExhibition(@RequestBody ExhibitionDeletePO param) {
		// 기획전 삭제 요청값을 확인합니다.
		String validationMessage = exhibitionService.validateExhibitionDelete(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		try {
			return ResponseEntity.ok(exhibitionService.deleteExhibition(param));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
		}
	}
}
