package com.xodud1202.springbackend.common.util;

// 공통 숫자 검증 유틸을 제공합니다.
public final class CommonValidationUtils {

	// 유틸 클래스의 인스턴스 생성을 막습니다.
	private CommonValidationUtils() {
	}

	// 1 이상 정수값을 검증해 반환합니다.
	public static int requirePositiveInt(Integer value, String invalidMessage) {
		// 값이 없거나 0 이하이면 호출부 메시지로 예외를 반환합니다.
		if (value == null || value < 1) {
			throw new IllegalArgumentException(invalidMessage);
		}
		return value;
	}

	// 1 이상 long 값을 검증해 반환합니다.
	public static long requirePositiveLong(Long value, String invalidMessage) {
		// 값이 없거나 0 이하이면 호출부 메시지로 예외를 반환합니다.
		if (value == null || value < 1L) {
			throw new IllegalArgumentException(invalidMessage);
		}
		return value;
	}
}
