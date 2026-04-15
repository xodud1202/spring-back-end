package com.xodud1202.springbackend.common.util;

// 공통 문자열 정규화 유틸을 제공합니다.
public final class CommonTextUtils {

	// 유틸 클래스의 인스턴스 생성을 막습니다.
	private CommonTextUtils() {
	}

	// 문자열을 trim 처리하고 비어 있으면 null을 반환합니다.
	public static String trimToNull(String value) {
		// null 문자열은 그대로 null로 반환합니다.
		if (value == null) {
			return null;
		}
		String trimmedValue = value.trim();
		return trimmedValue.isEmpty() ? null : trimmedValue;
	}

	// 문자열이 null 또는 공백인지 확인합니다.
	public static boolean isBlank(String value) {
		// trim 결과가 없으면 공백으로 판단합니다.
		return trimToNull(value) == null;
	}

	// null 문자열을 빈 문자열로 변환합니다.
	public static String safeValue(String value) {
		// 뷰/로그/외부 연동에서 null 안전 문자열을 반환합니다.
		return value == null ? "" : value;
	}

	// 전달된 값 중 첫 번째 비어 있지 않은 문자열을 반환합니다.
	public static String firstNonBlank(String... values) {
		// 전달값이 없으면 선택 가능한 문자열도 없으므로 null을 반환합니다.
		if (values == null) {
			return null;
		}
		for (String value : values) {
			String normalizedValue = trimToNull(value);
			if (normalizedValue != null) {
				return normalizedValue;
			}
		}
		return null;
	}
}
