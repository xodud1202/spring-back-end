package com.xodud1202.springbackend.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

// 공통 REST 예외 처리기의 로그용 요청 요약을 검증합니다.
class GlobalRestExceptionHandlerTests {
	@Test
	@DisplayName("요청 요약은 query string 값을 로그에 포함하지 않는다")
	// 민감한 쿼리 파라미터가 예외 로그 요청 요약에 노출되지 않는지 검증합니다.
	void buildRequestSummary_omitsQueryString() {
		// query string이 포함된 요청 객체를 구성합니다.
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/order/address/search");
		request.setQueryString("keyword=secret-address&token=secret-token");
		request.setRemoteAddr("127.0.0.1");

		// private 요청 요약 메서드를 호출합니다.
		String summary = ReflectionTestUtils.invokeMethod(
			new GlobalRestExceptionHandler(),
			"buildRequestSummary",
			request
		);

		// 요청 경로만 남고 query string 값은 제외되는지 확인합니다.
		assertEquals("GET /api/admin/order/address/search ip=127.0.0.1", summary);
		assertFalse(summary.contains("secret-address"));
		assertFalse(summary.contains("secret-token"));
	}
}
