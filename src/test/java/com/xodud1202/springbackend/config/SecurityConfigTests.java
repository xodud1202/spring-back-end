package com.xodud1202.springbackend.config;

import com.xodud1202.springbackend.common.web.CommonRequestLoggingFilter;
import com.xodud1202.springbackend.security.JwtTokenProvider;
import com.xodud1202.springbackend.service.CustomUserDetailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.Cookie;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
	classes = SecurityConfigTests.TestApplication.class,
	properties = {
		"security.csrf.allowed-origins[0]=http://localhost:3014",
		"security.csrf.allowed-origins[1]=http://127.0.0.1:3014"
	}
)
@AutoConfigureMockMvc
// Spring Security 경로 보호 정책을 검증합니다.
class SecurityConfigTests {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CommonRequestLoggingFilter commonRequestLoggingFilter;

	@Autowired
	private FilterRegistrationBean<CommonRequestLoggingFilter> commonRequestLoggingFilterRegistration;

	@MockBean
	private JwtTokenProvider jwtTokenProvider;

	@MockBean
	private CustomUserDetailService customUserDetailService;

	@Test
	@DisplayName("운영성 업로드 API는 비인증 요청을 거부한다")
	// 관리자 JWT가 없는 /api/upload/image 요청이 컨트롤러에 도달하지 못하는지 검증합니다.
	void uploadImage_rejectsAnonymousRequest() throws Exception {
		mockMvc.perform(post("/api/upload/image"))
			.andExpect(status().is4xxClientError());
	}

	@Test
	@DisplayName("뉴스 스냅샷 발행 API는 비인증 요청을 거부한다")
	// 관리자 JWT가 없는 /api/news/refresh/file 요청이 공개 뉴스 패턴보다 먼저 차단되는지 검증합니다.
	void newsRefreshFile_rejectsAnonymousRequest() throws Exception {
		mockMvc.perform(get("/api/news/refresh/file"))
			.andExpect(status().is4xxClientError());
	}

	@Test
	@DisplayName("공개 뉴스 조회 API는 비인증 요청을 허용한다")
	// 공개 뉴스 조회 API는 기존 공개 계약을 유지하는지 검증합니다.
	void newsPress_allowsAnonymousRequest() throws Exception {
		mockMvc.perform(get("/api/news/press"))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("쿠키 인증 변경 API는 허용 Origin 요청을 통과시킨다")
	// 허용된 프론트 Origin에서 온 쿠키 요청은 컨트롤러에 도달하는지 검증합니다.
	void cookieCsrf_allowsAllowedOriginCookieRequest() throws Exception {
		mockMvc.perform(post("/api/shop/cart/delete")
				.header(HttpHeaders.ORIGIN, "http://localhost:3014")
				.cookie(new Cookie("shop_auth", "signed-token")))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("쿠키 인증 변경 API는 허용 Referer 요청을 통과시킨다")
	// Origin이 없더라도 허용된 Referer가 있으면 컨트롤러에 도달하는지 검증합니다.
	void cookieCsrf_allowsAllowedRefererCookieRequest() throws Exception {
		mockMvc.perform(post("/api/shop/cart/delete")
				.header(HttpHeaders.REFERER, "http://127.0.0.1:3014/cart")
				.cookie(new Cookie("shop_auth", "signed-token")))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("쿠키 인증 변경 API는 외부 Origin 요청을 거부한다")
	// 허용 목록에 없는 Origin의 쿠키 요청은 CSRF 방어로 차단하는지 검증합니다.
	void cookieCsrf_rejectsForeignOriginCookieRequest() throws Exception {
		mockMvc.perform(post("/api/shop/cart/delete")
				.header(HttpHeaders.ORIGIN, "https://evil.example")
				.cookie(new Cookie("shop_auth", "signed-token")))
			.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("쿠키 인증 변경 API는 Origin과 Referer가 없는 쿠키 요청을 거부한다")
	// 출처를 확인할 수 없는 쿠키 요청은 CSRF 방어로 차단하는지 검증합니다.
	void cookieCsrf_rejectsMissingOriginCookieRequest() throws Exception {
		mockMvc.perform(post("/api/shop/cart/delete")
				.cookie(new Cookie("shop_auth", "signed-token")))
			.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("쿠키가 없는 변경 API 요청은 Origin 검사 대상에서 제외한다")
	// 비쿠키 요청은 기존 컨트롤러 인증/공개 정책에 맡기는지 검증합니다.
	void cookieCsrf_skipsRequestWithoutCookie() throws Exception {
		mockMvc.perform(post("/api/shop/cart/delete"))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("공통 요청 로그 필터는 서블릿 자동 등록을 비활성화한다")
	// 공통 요청 로그 필터가 Security 체인에서만 실행되도록 자동 등록이 꺼져 있는지 검증합니다.
	void commonRequestLoggingFilterRegistration_disablesServletAutoRegistration() {
		// 등록 빈이 같은 필터 인스턴스를 대상으로 자동 등록만 비활성화했는지 확인합니다.
		assertThat(commonRequestLoggingFilter).isNotNull();
		assertThat(commonRequestLoggingFilterRegistration.getFilter()).isSameAs(commonRequestLoggingFilter);
		assertThat(commonRequestLoggingFilterRegistration.isEnabled()).isFalse();
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@Import({SecurityConfig.class, TestEndpointController.class})
	// 테스트에 필요한 MVC/Security 구성만 로딩하는 부트스트랩 설정입니다.
	static class TestApplication {
	}

	@RestController
	// 보안 경로 매칭 검증을 위한 테스트용 엔드포인트입니다.
	static class TestEndpointController {
		@PostMapping("/api/upload/image")
		// 테스트용 운영성 업로드 응답을 반환합니다.
		ResponseEntity<Map<String, Object>> uploadImage() {
			return ResponseEntity.ok(Map.of("success", true));
		}

		@GetMapping("/api/news/refresh/file")
		// 테스트용 뉴스 스냅샷 발행 응답을 반환합니다.
		ResponseEntity<Map<String, Object>> refreshNewsFile() {
			return ResponseEntity.ok(Map.of("success", true));
		}

		@GetMapping("/api/news/press")
		// 테스트용 공개 뉴스 조회 응답을 반환합니다.
		ResponseEntity<Map<String, Object>> getNewsPress() {
			return ResponseEntity.ok(Map.of("success", true));
		}

		@PostMapping("/api/shop/cart/delete")
		// 테스트용 쇼핑몰 변경 API 응답을 반환합니다.
		ResponseEntity<Map<String, Object>> deleteShopCart() {
			return ResponseEntity.ok(Map.of("success", true));
		}
	}
}
