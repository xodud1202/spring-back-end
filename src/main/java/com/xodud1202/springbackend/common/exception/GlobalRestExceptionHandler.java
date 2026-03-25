package com.xodud1202.springbackend.common.exception;

import com.xodud1202.springbackend.common.response.ApiMessageResponse;
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

	// 서버 내부 상태 오류를 500 응답으로 변환합니다.
	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ApiMessageResponse> handleIllegalStateException(IllegalStateException exception) {
		log.error("REST 요청 처리 실패 message={}", exception.getMessage(), exception);
		return ResponseEntity.internalServerError()
			.body(new ApiMessageResponse(resolveMessage(exception.getMessage(), DEFAULT_INTERNAL_SERVER_ERROR_MESSAGE)));
	}

	// 처리되지 않은 예외를 500 응답으로 변환합니다.
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiMessageResponse> handleException(Exception exception) {
		log.error("REST 미처리 예외 발생 message={}", exception.getMessage(), exception);
		return ResponseEntity.internalServerError().body(new ApiMessageResponse(DEFAULT_INTERNAL_SERVER_ERROR_MESSAGE));
	}

	// 응답 메시지를 비어 있지 않은 기본값으로 보정합니다.
	private String resolveMessage(String message, String defaultMessage) {
		if (message == null || message.isBlank()) {
			return defaultMessage;
		}
		return message;
	}
}
