package com.xodud1202.springbackend.controller.shop;

import com.xodud1202.springbackend.service.BannerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
// 쇼핑몰 배너 조회 API를 제공합니다.
public class ShopBannerController {
	private final BannerService bannerService;

	// 쇼핑몰 메인 섹션 목록을 조회합니다.
	@GetMapping("/api/shop/main/sections")
	public ResponseEntity<Object> getShopBannerSections() {
		try {
			// 메인 섹션 목록을 조회해 sections 필드로 반환합니다.
			return ResponseEntity.ok(Map.of("sections", bannerService.getShopMainSectionList()));
		} catch (Exception exception) {
			// 예외 발생 시 에러 로그를 남기고 실패 메시지를 반환합니다.
			log.error("쇼핑몰 메인 섹션 조회 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "쇼핑 메인 배너 조회에 실패했습니다."));
		}
	}
}
