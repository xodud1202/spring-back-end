package com.xodud1202.springbackend.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// 공통 검증 유틸의 파일 확장자 검증 동작을 확인합니다.
class CommonValidationUtilsTests {

	@Test
	@DisplayName("허용 확장자는 콤마 구분 목록과 완전일치할 때만 통과한다")
	// 부분 문자열 확장자가 허용 목록에 포함된 것처럼 처리되지 않는지 검증합니다.
	void isAllowedFileExtension_requiresExactExtensionMatch() {
		// jpg, jpeg, png, gif 목록에서 완전일치와 부분일치를 함께 확인합니다.
		String allowedExtensions = "jpg,jpeg,png,gif";

		assertThat(CommonValidationUtils.isAllowedFileExtension(allowedExtensions, "brand-logo.JPG")).isTrue();
		assertThat(CommonValidationUtils.isAllowedFileExtension(allowedExtensions, "brand-logo.jp")).isFalse();
		assertThat(CommonValidationUtils.isAllowedFileExtension(allowedExtensions, "brand-logo.peg")).isFalse();
		assertThat(CommonValidationUtils.isAllowedFileExtension(allowedExtensions, "brand-logo")).isFalse();
	}

	@Test
	@DisplayName("전체 허용 설정은 확장자 목록 검사를 생략한다")
	// 별표 설정이 필요한 첨부 파일 경로에서 기존 전체 허용 의미를 유지합니다.
	void isAllowedFileExtension_allowsAnyExtensionWhenWildcardConfigured() {
		// 전체 허용 설정은 확장자 없는 파일명도 허용합니다.
		assertThat(CommonValidationUtils.isAllowedFileExtension("*", "archive.exe")).isTrue();
		assertThat(CommonValidationUtils.isAllowedFileExtension("*", "README")).isTrue();
	}
}
