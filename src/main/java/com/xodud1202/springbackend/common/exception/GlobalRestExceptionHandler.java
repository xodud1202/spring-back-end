package com.xodud1202.springbackend.common.exception;

import com.xodud1202.springbackend.common.response.ApiMessageResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
// 공통 REST 예외를 일관된 JSON message 응답으로 변환합니다.
public class GlobalRestExceptionHandler {
	private static final Logger log = LoggerFactory.getLogger(GlobalRestExceptionHandler.class);
	private static final String DEFAULT_BAD_REQUEST_MESSAGE = "요청값을 확인해주세요.";
	private static final String DEFAULT_INTERNAL_SERVER_ERROR_MESSAGE = "요청 처리 중 오류가 발생했습니다.";

	// Bean Validation 실패를 400 응답으로 변환합니다.
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiMessageResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
		FieldError fieldError = exception.getBindingResult().getFieldError();
		String message = fieldError == null ? DEFAULT_BAD_REQUEST_MESSAGE : resolveMessage(fieldError.getDefaultMessage(), DEFAULT_BAD_REQUEST_MESSAGE);
		return ResponseEntity.badRequest().body(new ApiMessageResponse(message));
	}

	// 파라미터 누락을 400 응답으로 변환합니다.
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ApiMessageResponse> handleMissingServletRequestParameterException(MissingServletRequestParameterException exception) {
		return ResponseEntity.badRequest().body(new ApiMessageResponse(resolveMessage(exception.getMessage(), DEFAULT_BAD_REQUEST_MESSAGE)));
	}

	// 파라미터 타입 불일치를 400 응답으로 변환합니다.
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiMessageResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException exception) {
		return ResponseEntity.badRequest().body(new ApiMessageResponse(resolveMessage(exception.getMessage(), DEFAULT_BAD_REQUEST_MESSAGE)));
	}

	// 잘못된 JSON 요청을 400 응답으로 변환합니다.
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiMessageResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception) {
		return ResponseEntity.badRequest().body(new ApiMessageResponse(DEFAULT_BAD_REQUEST_MESSAGE));
	}

	// 비즈니스 유효성 오류를 400 응답으로 변환합니다.
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiMessageResponse> handleIllegalArgumentException(IllegalArgumentException exception) {
		return ResponseEntity.badRequest().body(new ApiMessageResponse(resolveMessage(exception.getMessage(), DEFAULT_BAD_REQUEST_MESSAGE)));
	}

	// 보안 위반을 401 응답으로 변환합니다.
	@ExceptionHandler(SecurityException.class)
	public ResponseEntity<ApiMessageResponse> handleSecurityException(SecurityException exception) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
			.body(new ApiMessageResponse(resolveMessage(exception.getMessage(), "인증에 실패했습니다.")));
	}

	// 클라이언트 연결 종료로 응답 쓰기가 실패한 경우 요청 정보만 경고 로그로 남깁니다.
	@ExceptionHandler(AsyncRequestNotUsableException.class)
	public ResponseEntity<Void> handleAsyncRequestNotUsableException(
		AsyncRequestNotUsableException exception,
		HttpServletRequest request
	) {
		log.warn("REST 응답 종료 request={} message={}", buildRequestSummary(request), exception.getMessage(), exception);
		return ResponseEntity.noContent().build();
	}

	// 서버 내부 상태 오류를 500 응답으로 변환합니다.
	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ApiMessageResponse> handleIllegalStateException(
		IllegalStateException exception,
		HttpServletRequest request
	) {
		log.error("REST 요청 처리 실패 request={} message={}", buildRequestSummary(request), exception.getMessage(), exception);
		return ResponseEntity.internalServerError()
			.body(new ApiMessageResponse(resolveMessage(exception.getMessage(), DEFAULT_INTERNAL_SERVER_ERROR_MESSAGE)));
	}

	// 처리되지 않은 예외를 500 응답으로 변환합니다.
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiMessageResponse> handleException(Exception exception, HttpServletRequest request) {
		log.error("REST 미처리 예외 발생 request={} message={}", buildRequestSummary(request), exception.getMessage(), exception);
		return ResponseEntity.internalServerError().body(new ApiMessageResponse(DEFAULT_INTERNAL_SERVER_ERROR_MESSAGE));
	}

	// 응답 메시지를 비어 있지 않은 기본값으로 보정합니다.
	private String resolveMessage(String message, String defaultMessage) {
		if (message == null || message.isBlank()) {
			return defaultMessage;
		}
		return message;
	}

	// 로그 추적용 요청 메서드/URL/쿼리/IP를 문자열로 조합합니다.
	private String buildRequestSummary(HttpServletRequest request) {
		if (request == null) {
			return "unknown";
		}

		String method = resolveValue(request.getMethod(), "UNKNOWN");
		String requestUri = resolveValue(request.getRequestURI(), "");
		String queryString = request.getQueryString();
		String clientIp = resolveClientIp(request);
		String requestPath = queryString == null || queryString.isBlank()
			? requestUri
			: requestUri + "?" + queryString;
		return method + " " + requestPath + " ip=" + clientIp;
	}

	// 프록시 헤더를 우선해 클라이언트 IP를 추출합니다.
	private String resolveClientIp(HttpServletRequest request) {
		String forwardedFor = trimToNull(request.getHeader("X-Forwarded-For"));
		if (forwardedFor != null) {
			int commaIndex = forwardedFor.indexOf(',');
			return commaIndex >= 0 ? forwardedFor.substring(0, commaIndex).trim() : forwardedFor;
		}

		String realIp = trimToNull(request.getHeader("X-Real-IP"));
		if (realIp != null) {
			return realIp;
		}
		return resolveValue(request.getRemoteAddr(), "unknown");
	}

	// 문자열을 trim 처리하고 비어 있으면 null로 변환합니다.
	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	// 문자열이 비어 있으면 기본값으로 보정합니다.
	private String resolveValue(String value, String defaultValue) {
		String normalized = trimToNull(value);
		return normalized == null ? defaultValue : normalized;
	}
}
