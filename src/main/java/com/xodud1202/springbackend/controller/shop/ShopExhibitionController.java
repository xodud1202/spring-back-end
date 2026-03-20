package com.xodud1202.springbackend.controller.shop;

import com.xodud1202.springbackend.service.ExhibitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
// 쇼핑몰 기획전 조회 API를 제공합니다.
public class ShopExhibitionController {
	private static final String SHOP_EXHIBITION_NOT_FOUND_MESSAGE = "기획전 정보를 찾을 수 없습니다.";
	private static final String SHOP_EXHIBITION_TAB_NOT_FOUND_MESSAGE = "기획전 탭 정보를 찾을 수 없습니다.";

	private final ExhibitionService exhibitionService;

	// 쇼핑몰 기획전 목록 페이지 데이터를 조회합니다.
	@GetMapping("/api/shop/exhibition/list")
	public ResponseEntity<Object> getShopExhibitionList(@RequestParam(value = "pageNo", required = false) Integer pageNo) {
		try {
			// 노출 가능한 기획전 목록을 페이지 단위로 반환합니다.
			return ResponseEntity.ok(exhibitionService.getShopExhibitionPage(pageNo));
		} catch (Exception exception) {
			// 예외 발생 시 에러 로그와 실패 메시지를 반환합니다.
			log.error("쇼핑몰 기획전 목록 조회 실패 message={} pageNo={}", exception.getMessage(), pageNo, exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "기획전 목록 조회에 실패했습니다."));
		}
	}

	// 쇼핑몰 기획전 상세 화면 데이터를 조회합니다.
	@GetMapping("/api/shop/exhibition/detail")
	public ResponseEntity<Object> getShopExhibitionDetail(@RequestParam(value = "exhibitionNo", required = false) Integer exhibitionNo) {
		try {
			// 노출 가능한 기획전 상세 정보를 반환합니다.
			return ResponseEntity.ok(exhibitionService.getShopExhibitionDetail(exhibitionNo));
		} catch (IllegalArgumentException exception) {
			// 기획전 미노출/미존재는 404, 파라미터 오류는 400으로 구분합니다.
			if (SHOP_EXHIBITION_NOT_FOUND_MESSAGE.equals(exception.getMessage())) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
			}
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			// 기타 예외는 500 응답과 에러 로그를 반환합니다.
			log.error("쇼핑몰 기획전 상세 조회 실패 message={} exhibitionNo={}", exception.getMessage(), exhibitionNo, exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "기획전 상세 조회에 실패했습니다."));
		}
	}

	// 쇼핑몰 기획전 탭 상품 더보기 데이터를 조회합니다.
	@GetMapping("/api/shop/exhibition/goods")
	public ResponseEntity<Object> getShopExhibitionGoods(
		@RequestParam(value = "exhibitionNo", required = false) Integer exhibitionNo,
		@RequestParam(value = "exhibitionTabNo", required = false) Integer exhibitionTabNo,
		@RequestParam(value = "pageNo", required = false) Integer pageNo
	) {
		try {
			// 노출 가능한 탭 상품 목록을 페이지 단위로 반환합니다.
			return ResponseEntity.ok(exhibitionService.getShopExhibitionGoodsPage(exhibitionNo, exhibitionTabNo, pageNo));
		} catch (IllegalArgumentException exception) {
			// 기획전/탭 미노출은 404, 파라미터 오류는 400으로 구분합니다.
			if (SHOP_EXHIBITION_NOT_FOUND_MESSAGE.equals(exception.getMessage())
				|| SHOP_EXHIBITION_TAB_NOT_FOUND_MESSAGE.equals(exception.getMessage())) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
			}
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		} catch (Exception exception) {
			// 기타 예외는 500 응답과 에러 로그를 반환합니다.
			log.error(
				"쇼핑몰 기획전 상품 조회 실패 message={} exhibitionNo={} exhibitionTabNo={} pageNo={}",
				exception.getMessage(),
				exhibitionNo,
				exhibitionTabNo,
				pageNo,
				exception
			);
			return ResponseEntity.internalServerError().body(Map.of("message", "기획전 상품 조회에 실패했습니다."));
		}
	}
}
