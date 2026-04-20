package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.common.FtpProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// FTP 파일 서비스의 SR import 저장 파일명 규칙을 검증합니다.
class FtpFileServiceTests {

	@Test
	@DisplayName("SR import 저장 파일명은 원본 한글 파일명 대신 ASCII-safe 규칙과 확장자를 사용한다")
	// 한글 원본 파일명이어도 저장용 파일명은 ASCII-safe 형식과 원본 확장자로 생성되는지 확인합니다.
	void buildImportedCompanyWorkStorageFileNameUsesAsciiSafePattern() {
		FtpFileService ftpFileService = new FtpFileService(new FtpProperties());

		String result = ftpFileService.buildImportedCompanyWorkStorageFileName("OBK 영업관리_플랫폼별 예약현황 구성.pptx", 33L);

		assertThat(result).matches("import_33_\\d{17}_1\\.pptx");
		assertThat(result).doesNotContain("영업관리");
		assertThat(result).doesNotContain(" ");
	}

	@Test
	@DisplayName("SR import 저장 파일명은 확장자가 없으면 ASCII-safe 기본명만 생성한다")
	// 확장자가 없는 원본 파일명이어도 저장용 파일명 규칙이 깨지지 않는지 확인합니다.
	void buildImportedCompanyWorkStorageFileNameHandlesMissingExtension() {
		FtpFileService ftpFileService = new FtpFileService(new FtpProperties());

		String result = ftpFileService.buildImportedCompanyWorkStorageFileName("첨부파일", 41L);

		assertThat(result).matches("import_41_\\d{17}_1");
		assertThat(result).doesNotContain("첨부파일");
	}
}
