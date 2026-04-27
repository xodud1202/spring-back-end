package com.xodud1202.springbackend.common.web;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

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
	@DisplayName("공통 요청 로그는 프록시 헤더 IP와 마스킹된 경로를 INFO로 남긴다")
	// 역프록시 환경에서도 실제 클라이언트 IP와 민감값이 가려진 경로를 기록하는지 검증합니다.
	void doFilter_logsForwardedIpAndMaskedQueryString() throws Exception {
		// 프록시 헤더와 쿼리스트링이 포함된 요청을 준비합니다.
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/shop/order/page");
		request.setQueryString("cartId=12&paymentKey=secret-value&empty");
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
			.contains("path=/api/shop/order/page?cartId=*&paymentKey=*&empty")
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
}
