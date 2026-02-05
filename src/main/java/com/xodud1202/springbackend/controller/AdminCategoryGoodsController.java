package com.xodud1202.springbackend.controller;

import com.xodud1202.springbackend.domain.admin.category.CategoryGoodsDeletePO;
import com.xodud1202.springbackend.domain.admin.category.CategoryGoodsOrderSavePO;
import com.xodud1202.springbackend.domain.admin.category.CategoryGoodsRegisterPO;
import com.xodud1202.springbackend.domain.admin.category.CategoryGoodsVO;
import com.xodud1202.springbackend.service.GoodsService;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequiredArgsConstructor
// 카테고리별 상품 관리 API를 제공합니다.
public class AdminCategoryGoodsController {
	private final GoodsService goodsService;

	// 카테고리별 상품 목록을 조회합니다.
	@GetMapping("/api/admin/category/goods/list")
	public ResponseEntity<Object> getCategoryGoodsList(@RequestParam String categoryId) {
		// 카테고리 코드 유효성을 확인합니다.
		if (categoryId == null || categoryId.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("message", "카테고리를 선택해주세요."));
		}
		List<CategoryGoodsVO> list = goodsService.getAdminCategoryGoodsList(categoryId);
		return ResponseEntity.ok(list);
	}

	// 카테고리 상품 정렬 순서를 저장합니다.
	@PostMapping("/api/admin/category/goods/order/save")
	public ResponseEntity<Object> saveCategoryGoodsOrder(@RequestBody CategoryGoodsOrderSavePO param) {
		// 요청 검증을 수행합니다.
		String validationMessage = goodsService.validateCategoryGoodsOrderSave(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		return ResponseEntity.ok(goodsService.saveCategoryGoodsOrder(param));
	}

	// 카테고리 상품을 등록합니다.
	@PostMapping("/api/admin/category/goods/register")
	public ResponseEntity<Object> registerCategoryGoods(@RequestBody CategoryGoodsRegisterPO param) {
		// 요청 검증을 수행합니다.
		String validationMessage = goodsService.validateCategoryGoodsRegister(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		return ResponseEntity.ok(goodsService.registerCategoryGoods(param));
	}

	// 카테고리 상품을 삭제합니다.
	@PostMapping("/api/admin/category/goods/delete")
	public ResponseEntity<Object> deleteCategoryGoods(@RequestBody CategoryGoodsDeletePO param) {
		// 요청 검증을 수행합니다.
		String validationMessage = goodsService.validateCategoryGoodsDelete(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		return ResponseEntity.ok(goodsService.deleteCategoryGoods(param));
	}

	// 카테고리 상품 엑셀 파일을 다운로드합니다.
	@GetMapping("/api/admin/category/goods/excel/download")
	public ResponseEntity<Object> downloadCategoryGoodsExcel(@RequestParam String categoryId) {
		// 카테고리 코드 유효성을 확인합니다.
		if (categoryId == null || categoryId.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("message", "카테고리를 선택해주세요."));
		}
		try {
			byte[] data = goodsService.buildCategoryGoodsExcel(categoryId);
			String fileDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
			String filename = "category_goods_" + fileDate + ".xlsx";
			return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
				.contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.body(data);
		} catch (IOException e) {
			return ResponseEntity.internalServerError().body(Map.of("message", "엑셀 다운로드에 실패했습니다."));
		}
	}

	// 카테고리 상품 엑셀 파일을 업로드합니다.
	@PostMapping("/api/admin/category/goods/excel/upload")
	public ResponseEntity<Object> uploadCategoryGoodsExcel(
		@RequestParam("file") MultipartFile file,
		@RequestParam Long regNo,
		@RequestParam Long udtNo) {
		// 요청 검증을 수행합니다.
		String validationMessage = goodsService.validateCategoryGoodsExcelUpload(file, regNo, udtNo);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		try {
			Map<String, Object> result = goodsService.uploadCategoryGoodsExcel(file, regNo, udtNo);
			return ResponseEntity.ok(result);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
		} catch (IOException e) {
			return ResponseEntity.internalServerError().body(Map.of("message", "엑셀 업로드에 실패했습니다."));
		}
	}
}
