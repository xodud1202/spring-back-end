package com.xodud1202.springbackend.controller;

import com.xodud1202.springbackend.domain.admin.brand.BrandVO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsPO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsDetailVO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsMerchVO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsDescSavePO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsDescVO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsImageSavePO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsImageVO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsImageOrderSavePO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsSavePO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsCategorySavePO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsSizeOrderSavePO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsSizeSavePO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsSizeVO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsCategoryVO;
import com.xodud1202.springbackend.domain.admin.category.CategoryVO;
import com.xodud1202.springbackend.service.GoodsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
// 관리자 상품 API를 제공합니다.
public class AdminGoodsController {
	private final GoodsService goodsService;

	// 관리자 상품 목록을 조회합니다.
	@GetMapping("/api/admin/goods/list")
	public ResponseEntity<Map<String, Object>> getGoodsList(GoodsPO param) {
		return ResponseEntity.ok(goodsService.getAdminGoodsList(param));
	}

	// 상품 분류 목록을 조회합니다.
	@GetMapping("/api/admin/goods/merch/list")
	public ResponseEntity<List<GoodsMerchVO>> getGoodsMerchList() {
		return ResponseEntity.ok(goodsService.getGoodsMerchList());
	}

	// 브랜드 목록을 조회합니다.
	@GetMapping("/api/admin/brand/list")
	public ResponseEntity<List<BrandVO>> getBrandList() {
		return ResponseEntity.ok(goodsService.getBrandList());
	}

	// 관리자 상품 상세 정보를 조회합니다.
	@GetMapping("/api/admin/goods/detail")
	public ResponseEntity<Object> getGoodsDetail(@RequestParam String goodsId) {
		if (goodsId == null || goodsId.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("message", "상품코드를 입력해주세요."));
		}

		GoodsDetailVO detail = goodsService.getAdminGoodsDetail(goodsId);
		if (detail == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(detail);
	}

	// 관리자 상품을 등록합니다.
	@PostMapping("/api/admin/goods/create")
	public ResponseEntity<Object> createGoods(@RequestBody GoodsSavePO param) {
		String validationMessage = goodsService.validateGoodsSave(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		return ResponseEntity.ok(goodsService.insertAdminGoods(param));
	}

	// 관리자 상품을 수정합니다.
	@PostMapping("/api/admin/goods/update")
	public ResponseEntity<Object> updateGoods(@RequestBody GoodsSavePO param) {
		String validationMessage = goodsService.validateGoodsUpdate(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		return ResponseEntity.ok(goodsService.updateAdminGoods(param));
	}

	// 카테고리 목록을 조회합니다.
	@GetMapping("/api/admin/category/list")
	public ResponseEntity<List<CategoryVO>> getCategoryList(@RequestParam(required = false) Integer categoryLevel,
			@RequestParam(required = false) String parentCategoryId) {
		return ResponseEntity.ok(goodsService.getCategoryList(categoryLevel, parentCategoryId));
	}

	// 관리자 상품 카테고리 목록을 조회합니다.
	@GetMapping("/api/admin/goods/category/list")
	public ResponseEntity<List<GoodsCategoryVO>> getGoodsCategoryList(@RequestParam String goodsId) {
		if (goodsId == null || goodsId.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(List.of());
		}
		return ResponseEntity.ok(goodsService.getAdminGoodsCategoryList(goodsId));
	}

	// 관리자 상품 카테고리를 단건 저장합니다.
	@PostMapping("/api/admin/goods/category/save")
	public ResponseEntity<Object> saveGoodsCategory(@RequestBody GoodsCategorySavePO param) {
		String validationMessage = goodsService.validateGoodsCategorySave(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		return ResponseEntity.ok(goodsService.saveAdminGoodsCategory(param));
	}

	// 관리자 상품 카테고리를 단건 삭제합니다.
	@PostMapping("/api/admin/goods/category/delete")
	public ResponseEntity<Object> deleteGoodsCategory(@RequestBody GoodsCategorySavePO param) {
		String validationMessage = goodsService.validateGoodsCategoryDelete(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		return ResponseEntity.ok(goodsService.deleteAdminGoodsCategory(param));
	}

	// 관리자 상품 사이즈 목록을 조회합니다.
	@GetMapping("/api/admin/goods/size/list")
	public ResponseEntity<List<GoodsSizeVO>> getGoodsSizeList(@RequestParam String goodsId) {
		if (goodsId == null || goodsId.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(List.of());
		}
		return ResponseEntity.ok(goodsService.getAdminGoodsSizeList(goodsId));
	}

	// 관리자 상품 사이즈를 저장합니다.
	@PostMapping("/api/admin/goods/size/save")
	public ResponseEntity<Object> saveGoodsSize(@RequestBody GoodsSizeSavePO param) {
		if (param == null) {
			return ResponseEntity.badRequest().body(Map.of("message", "요청 데이터가 없습니다."));
		}
		String lookupSizeId = param.getOriginSizeId() == null || param.getOriginSizeId().trim().isEmpty()
			? param.getSizeId()
			: param.getOriginSizeId();
		GoodsSizeVO current = goodsService.getAdminGoodsSizeDetail(param.getGoodsId(), lookupSizeId);
		boolean isNew = current == null;
		String validationMessage = goodsService.validateGoodsSizeSave(param, isNew);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		return ResponseEntity.ok(goodsService.saveAdminGoodsSize(param));
	}

	// 관리자 상품 사이즈를 삭제 처리합니다.
	@PostMapping("/api/admin/goods/size/delete")
	public ResponseEntity<Object> deleteGoodsSize(@RequestBody GoodsSizeSavePO param) {
		if (param == null) {
			return ResponseEntity.badRequest().body(Map.of("message", "요청 데이터가 없습니다."));
		}
		String validationMessage = goodsService.validateGoodsSizeDelete(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		return ResponseEntity.ok(goodsService.deleteAdminGoodsSize(param));
	}

	// 관리자 상품 사이즈 순서를 저장합니다.
	@PostMapping("/api/admin/goods/size/order/save")
	public ResponseEntity<Object> saveGoodsSizeOrder(@RequestBody GoodsSizeOrderSavePO param) {
		String validationMessage = goodsService.validateGoodsSizeOrderSave(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		return ResponseEntity.ok(goodsService.updateAdminGoodsSizeOrder(param));
	}

	// 관리자 상품 이미지 목록을 조회합니다.
	@GetMapping("/api/admin/goods/image/list")
	public ResponseEntity<List<GoodsImageVO>> getGoodsImageList(@RequestParam String goodsId) {
		if (goodsId == null || goodsId.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(List.of());
		}
		return ResponseEntity.ok(goodsService.getAdminGoodsImageList(goodsId));
	}

	// 관리자 상품 이미지를 업로드합니다.
	@PostMapping("/api/admin/goods/image/upload")
	public ResponseEntity<Object> uploadGoodsImage(
			@RequestParam("image") MultipartFile image,
			@RequestParam String goodsId,
			@RequestParam Long regNo) {
		String validationMessage = goodsService.validateGoodsImageUpload(goodsId, image, regNo);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		try {
			GoodsImageVO uploaded = goodsService.uploadAdminGoodsImage(goodsId, image, regNo);
			return ResponseEntity.ok(uploaded);
		} catch (IOException e) {
			log.error("상품 이미지 업로드 실패", e);
			return ResponseEntity.internalServerError().body(Map.of("message", "이미지 업로드에 실패했습니다."));
		}
	}

	// 관리자 상품 이미지를 삭제합니다.
	@PostMapping("/api/admin/goods/image/delete")
	public ResponseEntity<Object> deleteGoodsImage(@RequestBody GoodsImageSavePO param) {
		String validationMessage = goodsService.validateGoodsImageDelete(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		return ResponseEntity.ok(goodsService.deleteAdminGoodsImage(param));
	}

	// 관리자 상품 이미지 순서를 저장합니다.
	@PostMapping("/api/admin/goods/image/order/save")
	public ResponseEntity<Object> saveGoodsImageOrder(@RequestBody GoodsImageOrderSavePO param) {
		String validationMessage = goodsService.validateGoodsImageOrderSave(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		return ResponseEntity.ok(goodsService.updateAdminGoodsImageOrder(param));
	}

	// 관리자 상품 상세 설명을 조회합니다.
	@GetMapping("/api/admin/goods/desc/list")
	public ResponseEntity<List<GoodsDescVO>> getGoodsDescList(@RequestParam String goodsId) {
		if (goodsId == null || goodsId.trim().isEmpty()) {
			return ResponseEntity.badRequest().body(List.of());
		}
		return ResponseEntity.ok(goodsService.getAdminGoodsDescList(goodsId));
	}

	// 관리자 상품 상세 설명을 저장합니다.
	@PostMapping("/api/admin/goods/desc/save")
	public ResponseEntity<Object> saveGoodsDesc(@RequestBody GoodsDescSavePO param) {
		String validationMessage = goodsService.validateGoodsDescSave(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		return ResponseEntity.ok(goodsService.saveAdminGoodsDesc(param));
	}
}
