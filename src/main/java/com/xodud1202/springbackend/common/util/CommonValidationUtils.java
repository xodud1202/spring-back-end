package com.xodud1202.springbackend.common.util;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

// 공통 검증 유틸을 제공합니다.
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

	// 파일명에서 마지막 점 뒤의 확장자를 소문자로 추출합니다.
	public static String extractFileExtension(String fileName) {
		// 파일명이 없거나 확장자 구분자가 없으면 확장자를 없는 것으로 처리합니다.
		String normalizedFileName = trimToNull(fileName);
		if (normalizedFileName == null) {
			return null;
		}

		int extensionIndex = normalizedFileName.lastIndexOf('.');
		if (extensionIndex < 0 || extensionIndex == normalizedFileName.length() - 1) {
			return null;
		}
		return normalizeExtension(normalizedFileName.substring(extensionIndex + 1));
	}

	// 파일명이 허용 확장자 목록에 완전일치하는지 확인합니다.
	public static boolean isAllowedFileExtension(String allowedExtensions, String fileName) {
		// 파일명에서 추출한 확장자를 공통 확장자 검증으로 위임합니다.
		return isAllowedExtension(allowedExtensions, extractFileExtension(fileName));
	}

	// 확장자가 허용 확장자 목록에 완전일치하는지 확인합니다.
	public static boolean isAllowedExtension(String allowedExtensions, String extension) {
		// 전체 허용 설정이면 확장자 목록 검사를 생략합니다.
		if (isAllowAllFileExtensionConfigured(allowedExtensions)) {
			return true;
		}

		String normalizedExtension = normalizeExtension(extension);
		if (normalizedExtension == null) {
			return false;
		}
		return parseAllowedExtensionSet(allowedExtensions).contains(normalizedExtension);
	}

	// 콤마 구분 허용 확장자 설정을 완전일치 검증용 Set으로 변환합니다.
	public static Set<String> parseAllowedExtensionSet(String allowedExtensions) {
		// 빈 설정은 허용 확장자가 없는 것으로 처리합니다.
		Set<String> result = new LinkedHashSet<>();
		String normalizedAllowedExtensions = trimToNull(allowedExtensions);
		if (normalizedAllowedExtensions == null) {
			return result;
		}
		for (String allowedExtensionItem : normalizedAllowedExtensions.split(",")) {
			String normalizedExtension = normalizeExtension(allowedExtensionItem);
			if (normalizedExtension != null) {
				result.add(normalizedExtension);
			}
		}
		return Set.copyOf(result);
	}

	// 허용 확장자 설정이 전체 허용 모드인지 확인합니다.
	public static boolean isAllowAllFileExtensionConfigured(String allowedExtensions) {
		return "*".equals(trimToNull(allowedExtensions));
	}

	// 확장자 문자열을 비교 가능한 소문자 값으로 정규화합니다.
	private static String normalizeExtension(String extension) {
		// 앞 점과 공백을 제거해 설정값과 파일명 확장자 모두 같은 기준으로 비교합니다.
		String normalizedExtension = trimToNull(extension);
		if (normalizedExtension == null) {
			return null;
		}
		while (normalizedExtension.startsWith(".")) {
			normalizedExtension = normalizedExtension.substring(1);
		}
		normalizedExtension = trimToNull(normalizedExtension);
		if (normalizedExtension == null) {
			return null;
		}
		return normalizedExtension.toLowerCase(Locale.ROOT);
	}

	// 문자열을 공백 제거 후 null 안전 값으로 변환합니다.
	private static String trimToNull(String value) {
		// 공백뿐인 문자열은 빈 확장자와 동일하게 취급합니다.
		if (value == null) {
			return null;
		}
		String trimmedValue = value.trim();
		return trimmedValue.isEmpty() ? null : trimmedValue;
	}
}
