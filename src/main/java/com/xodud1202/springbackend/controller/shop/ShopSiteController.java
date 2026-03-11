package com.xodud1202.springbackend.controller.shop;

import com.xodud1202.springbackend.service.SiteInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
// 쇼핑몰 사이트 기본 정보 조회 API를 제공합니다.
public class ShopSiteController {
	private final SiteInfoService siteInfoService;

	// 쇼핑몰 화면 공통에 필요한 사이트 기본 정보를 조회합니다.
	@GetMapping("/api/shop/site/info")
	public ResponseEntity<Object> getShopSiteInfo() {
		try {
			// SSR 화면에서 사용할 사이트 기본 정보를 반환합니다.
			return ResponseEntity.ok(siteInfoService.getShopSiteInfo());
		} catch (Exception exception) {
			// 예외 발생 시 에러 로그와 실패 메시지를 반환합니다.
			log.error("쇼핑몰 사이트 기본 정보 조회 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("message", "사이트 기본 정보 조회에 실패했습니다."));
		}
	}
}
