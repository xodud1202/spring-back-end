package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.common.FtpProperties;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = FtpFileServiceIntegrationTest.TestConfig.class)
@EnabledIfEnvironmentVariable(named = "RUN_FTP_INTEGRATION_TEST", matches = "Y")
// 실제 FTP 서버에 파일을 업로드/rename하여 원자적 교체 동작을 검증하는 통합 테스트입니다.
class FtpFileServiceIntegrationTest {
	private static final DateTimeFormatter TEST_FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
	private static final String DEFAULT_NEWS_SNAPSHOT_TARGET_PATH = "/HDD1/Media/nas/news";

	@Autowired
	private FtpFileService ftpFileService;

	@Autowired
	private FtpProperties ftpProperties;

	private String uploadedTargetPath;
	private String uploadedFinalFileName;

	@Test
	@DisplayName("UTF-8 텍스트 파일을 FTP에 임시업로드 후 rename으로 최종 파일로 교체한다")
	// 실제 FTP에서 임시파일 업로드 후 최종 파일 존재/내용 확인과 임시파일 제거 여부를 검증합니다.
	void uploadUtf8TextFileAtomically_uploadsAndRenamesOnRealFtp() throws Exception {
		String targetPath = ftpFileService.resolveNewsSnapshotTargetPath(DEFAULT_NEWS_SNAPSHOT_TARGET_PATH);
		String finalFileName = "newsList.integration-test." + LocalDateTime.now().format(TEST_FILE_TIME_FORMATTER) + ".json";
		String content = "{\"integrationTest\":\"Y\",\"message\":\"ftp-atomic-upload\"}";

		String tempFileName = ftpFileService.uploadUtf8TextFileAtomically(targetPath, finalFileName, content);
		this.uploadedTargetPath = targetPath;
		this.uploadedFinalFileName = finalFileName;

		// 업로드 직후 FTP 서버에서 최종 파일이 존재하고 임시파일은 없어졌는지 확인합니다.
		assertThat(existsFile(targetPath, finalFileName)).isTrue();
		assertThat(existsFile(targetPath, tempFileName)).isFalse();

		// 업로드된 최종 파일 내용을 다시 읽어 전송 문자열과 일치하는지 확인합니다.
		String storedContent = readUtf8FileContent(targetPath, finalFileName);
		assertThat(storedContent).isEqualTo(content);
	}

	@AfterEach
	// 테스트에서 생성한 최종 파일이 있으면 FTP에서 정리합니다.
	void cleanUpUploadedFile() throws Exception {
		if (uploadedTargetPath == null || uploadedFinalFileName == null) {
			return;
		}
		deleteFileIfExists(uploadedTargetPath, uploadedFinalFileName);
		uploadedTargetPath = null;
		uploadedFinalFileName = null;
	}

	/**
	 * FTP 대상 경로에 특정 파일이 존재하는지 확인합니다.
	 * @param targetPath FTP 대상 경로
	 * @param fileName 파일명
	 * @return 존재 여부
	 */
	private boolean existsFile(String targetPath, String fileName) throws IOException {
		FTPClient ftpClient = new FTPClient();
		try {
			// FTP에 접속한 뒤 대상 폴더로 이동합니다.
			connectAndMoveToTargetPath(ftpClient, targetPath);

			// 파일 목록을 조회해 동일 파일명이 있는지 확인합니다.
			return ftpClient.listFiles(fileName).length > 0;
		} finally {
			disconnectQuietly(ftpClient);
		}
	}

	/**
	 * FTP 파일의 UTF-8 내용을 문자열로 읽어옵니다.
	 * @param targetPath FTP 대상 경로
	 * @param fileName 파일명
	 * @return UTF-8 문자열 내용
	 */
	private String readUtf8FileContent(String targetPath, String fileName) throws IOException {
		FTPClient ftpClient = new FTPClient();
		try {
			// FTP에 접속한 뒤 대상 폴더로 이동합니다.
			connectAndMoveToTargetPath(ftpClient, targetPath);

			// 파일 스트림을 읽고 명시적으로 pending command를 완료합니다.
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			boolean retrieved = ftpClient.retrieveFile(fileName, outputStream);
			if (!retrieved) {
				throw new IOException("FTP 파일 조회 실패: " + fileName);
			}
			if (!ftpClient.completePendingCommand()) {
				throw new IOException("FTP pending command 완료 실패: " + fileName);
			}
			return outputStream.toString(StandardCharsets.UTF_8);
		} finally {
			disconnectQuietly(ftpClient);
		}
	}

	/**
	 * FTP 대상 경로에서 파일이 존재하면 삭제합니다.
	 * @param targetPath FTP 대상 경로
	 * @param fileName 파일명
	 */
	private void deleteFileIfExists(String targetPath, String fileName) throws IOException {
		FTPClient ftpClient = new FTPClient();
		try {
			// FTP에 접속한 뒤 대상 폴더로 이동합니다.
			connectAndMoveToTargetPath(ftpClient, targetPath);

			// 생성된 테스트 파일만 삭제하고, 존재하지 않으면 그대로 종료합니다.
			if (ftpClient.listFiles(fileName).length == 0) {
				return;
			}
			boolean deleted = ftpClient.deleteFile(fileName);
			if (!deleted) {
				throw new IOException("FTP 테스트 파일 삭제 실패: " + fileName);
			}
		} finally {
			disconnectQuietly(ftpClient);
		}
	}

	/**
	 * FTP 서버에 접속하고 대상 경로로 이동합니다.
	 * @param ftpClient FTP 클라이언트
	 * @param targetPath 대상 경로
	 */
	private void connectAndMoveToTargetPath(FTPClient ftpClient, String targetPath) throws IOException {
		ftpClient.connect(ftpProperties.getHost(), ftpProperties.getPort());
		boolean login = ftpClient.login(ftpProperties.getUsername(), ftpProperties.getPwd());
		if (!login) {
			throw new IOException("FTP 로그인 실패");
		}
		ftpClient.enterLocalPassiveMode();
		ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

		// 절대 경로를 세그먼트 단위로 이동합니다.
		if (targetPath.startsWith("/") && !ftpClient.changeWorkingDirectory("/")) {
			throw new IOException("FTP 루트 이동 실패");
		}
		String[] pathSegments = targetPath.split("/");
		for (String pathSegment : pathSegments) {
			if (pathSegment == null || pathSegment.trim().isEmpty()) {
				continue;
			}
			if (!ftpClient.changeWorkingDirectory(pathSegment.trim())) {
				throw new IOException("FTP 대상 경로 이동 실패: " + targetPath);
			}
		}
	}

	/**
	 * FTP 연결이 살아 있으면 로그아웃/연결종료를 수행합니다.
	 * @param ftpClient FTP 클라이언트
	 */
	private void disconnectQuietly(FTPClient ftpClient) {
		try {
			if (ftpClient.isConnected()) {
				ftpClient.logout();
				ftpClient.disconnect();
			}
		} catch (Exception ignored) {
			// 정리 단계 예외는 테스트 본문 결과를 덮어쓰지 않도록 무시합니다.
		}
	}

	@Configuration
	@EnableConfigurationProperties(FtpProperties.class)
	@Import(FtpFileService.class)
	// FTP 통합 테스트용 최소 스프링 컨텍스트 설정입니다.
	static class TestConfig {
	}
}
