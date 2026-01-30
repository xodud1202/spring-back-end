package com.xodud1202.springbackend.controller;

import com.xodud1202.springbackend.domain.admin.goods.GoodsPO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsDetailVO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsMerchVO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsSavePO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsSizeOrderSavePO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsSizeSavePO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsSizeVO;
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

import java.util.List;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
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
}
