package com.xodud1202.springbackend.common.web;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

// 공통 요청 로그 필터의 IP/경로 기록 규칙을 검증합니다.
class CommonRequestLoggingFilterTests {
	private CommonRequestLoggingFilter commonRequestLoggingFilter;
	private Logger logger;
	private ListAppender<ILoggingEvent> listAppender;

	@BeforeEach
	// 테스트 대상 필터와 캡처용 로그 appender를 초기화합니다.
	void setUp() {
		commonRequestLoggingFilter = new CommonRequestLoggingFilter();
		logger = (Logger) LoggerFactory.getLogger(CommonRequestLoggingFilter.class);
		listAppender = new ListAppender<>();
		listAppender.start();
		logger.addAppender(listAppender);
	}

	@AfterEach
	// 테스트마다 연결한 로그 appender를 정리합니다.
	void tearDown() {
		logger.detachAppender(listAppender);
		listAppender.stop();
	}

	@Test
	@DisplayName("공통 요청 로그는 프록시 헤더 IP와 원본 쿼리값을 INFO로 남긴다")
	// 실제 클라이언트 IP와 중복 키, 인코딩, 빈 값을 포함한 원본 쿼리스트링을 기록하는지 검증합니다.
	void doFilter_logsForwardedIpAndOriginalQueryString() throws Exception {
		// 프록시 헤더와 여러 형태의 쿼리값이 포함된 상품 상세 요청을 준비합니다.
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/shop/goods/detail");
		request.setQueryString("goodsId=000123&goodsId=456&paymentKey=test-payment-key&keyword=%EC%83%81%ED%92%88+%26+%3D&empty=&flag");
		request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.5");
		MockHttpServletResponse response = new MockHttpServletResponse();

		// 정상 응답을 반환하는 필터 체인을 수행합니다.
		FilterChain filterChain = (req, res) -> ((MockHttpServletResponse) res).setStatus(204);
		commonRequestLoggingFilter.doFilter(request, response, filterChain);

		// 로그 한 건이 INFO 레벨로 남았는지 확인합니다.
		assertThat(listAppender.list).hasSize(1);
		ILoggingEvent loggingEvent = listAppender.list.get(0);
		assertThat(loggingEvent.getLevel()).isEqualTo(Level.INFO);
		assertThat(loggingEvent.getFormattedMessage())
			.contains("ip=203.0.113.10")
			.contains("method=GET")
			.contains("path=/api/shop/goods/detail?goodsId=000123&goodsId=456&paymentKey=test-payment-key&keyword=%EC%83%81%ED%92%88+%26+%3D&empty=&flag")
			.contains("status=204")
			.contains("durationMs=");
	}

	@Test
	@DisplayName("공통 요청 로그는 프록시 헤더가 없으면 remoteAddr를 사용한다")
	// 프록시 헤더가 없는 직접 요청에서도 원격 주소와 경로를 기록하는지 검증합니다.
	void doFilter_logsRemoteAddrWhenProxyHeaderMissing() throws Exception {
		// 헤더가 없는 직접 요청을 준비합니다.
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/shop/auth/logout");
		request.setRemoteAddr("127.0.0.1");
		MockHttpServletResponse response = new MockHttpServletResponse();

		// 필터 체인을 수행해 기본 200 응답을 반환합니다.
		FilterChain filterChain = (req, res) -> ((MockHttpServletResponse) res).setStatus(200);
		commonRequestLoggingFilter.doFilter(request, response, filterChain);

		// remoteAddr와 경로가 로그에 남았는지 확인합니다.
		assertThat(listAppender.list).hasSize(1);
		String formattedMessage = listAppender.list.get(0).getFormattedMessage();
		assertThat(formattedMessage)
			.contains("ip=127.0.0.1")
			.contains("method=POST")
			.contains("path=/api/shop/auth/logout")
			.contains("status=200");
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
		"application/json|{\"password\":\"test-password\",\"token\":\"test-token\",\"paymentKey\":\"test-payment-key\"}",
		"application/x-www-form-urlencoded|password=test-password&token=test-token&paymentKey=test-payment-key"
	})
	@DisplayName("POST 요청은 URL 쿼리값만 기록하고 JSON 및 폼 본문은 노출하지 않는다")
	// POST 본문의 민감값을 로그에 남기지 않고 다음 처리 단계에 원문으로 전달하는지 검증합니다.
	void doFilter_logsPostQueryWithoutBody(String contentType, String requestBody) throws Exception {
		// URL 쿼리와 민감값을 담은 본문을 별도로 설정합니다.
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/shop/order/complete");
		request.setQueryString("goodsId=000123&quantity=2");
		request.setContentType(contentType);
		request.setContent(requestBody.getBytes(StandardCharsets.UTF_8));
		MockHttpServletResponse response = new MockHttpServletResponse();

		// 다음 필터가 요청 본문 전체를 변경 없이 읽을 수 있는지 확인합니다.
		FilterChain filterChain = (req, res) -> assertThat(
			new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
		).isEqualTo(requestBody);
		commonRequestLoggingFilter.doFilter(request, response, filterChain);

		// POST URL 값은 보존하고 본문의 필드명과 값은 기록하지 않는지 확인합니다.
		assertThat(listAppender.list).hasSize(1);
		assertThat(listAppender.list.get(0).getFormattedMessage())
			.contains("method=POST")
			.contains("path=/api/shop/order/complete?goodsId=000123&quantity=2")
			.contains("status=200")
			.doesNotContain("password", "token", "paymentKey", requestBody);
	}
}
