package com.xodud1202.springbackend.controller.shop;

import com.xodud1202.springbackend.service.ExhibitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
}

