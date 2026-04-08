package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.common.FtpProperties;
import lombok.RequiredArgsConstructor;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
// FTP 파일 업로드/삭제와 경로 계산을 제공하는 서비스입니다.
public class FtpFileService {
	private static final DateTimeFormatter FTP_TEMP_FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
	private static final int ATOMIC_TEXT_UPLOAD_CONNECT_TIMEOUT_MILLIS = 10000;
	private static final int ATOMIC_TEXT_UPLOAD_SOCKET_TIMEOUT_MILLIS = 30000;
	private static final int ATOMIC_TEXT_UPLOAD_DATA_TIMEOUT_MILLIS = 30000;
	private static final int PUBLIC_FILE_DOWNLOAD_CONNECT_TIMEOUT_MILLIS = 10000;
	private static final int PUBLIC_FILE_DOWNLOAD_READ_TIMEOUT_MILLIS = 30000;
	
	private final FtpProperties ftpProperties;

	/**
	 * 이력서 이미지 파일을 FTP 서버에 업로드하고 접근 가능한 URL을 반환합니다.
	 * @param file 업로드할 파일
	 * @param usrNo 사용자 번호
	 * @return 업로드된 파일의 접근 URL
	 */
	public String uploadResumeImage(MultipartFile file, String usrNo) throws IOException {
		return uploadImageToFtp(
				file,
				ftpProperties.getUploadResumeFaceImgTargetPath(),
				ftpProperties.getUploadResumeFaceImgView(),
				new String[] { usrNo },
				usrNo
		);
	}

	/**
	 * 학력 로고 이미지 파일을 FTP 서버에 업로드하고 접근 가능한 URL을 반환합니다.
	 * @param file 업로드할 파일
	 * @param usrNo 사용자 번호
	 * @return 업로드된 파일의 접근 URL
	 */
	public String uploadResumeEducationLogo(MultipartFile file, String usrNo) throws IOException {
		return uploadImageToFtp(
				file,
				ftpProperties.getUploadResumeEducationTargetPath(),
				ftpProperties.getUploadResumeEducationView(),
				new String[] { usrNo },
				usrNo
		);
	}

	/**
	 * 게시글 이미지 파일을 FTP 서버에 업로드하고 접근 가능한 URL을 반환합니다.
	 * @param file 업로드할 파일
	 * @param boardNo 게시글 번호
	 * @return 업로드된 파일의 접근 URL
	 */
	public String uploadBoardImage(MultipartFile file, Long boardNo) throws IOException {
		String folderName = String.valueOf(boardNo);
		return uploadImageToFtp(
				file,
				ftpProperties.getUploadBoardTargetPath(),
				ftpProperties.getUploadBoardView(),
				new String[] { folderName },
				folderName
		);
	}

	/**
	 * 게시글 등록 중 이미지 파일을 FTP 서버에 업로드하고 접근 가능한 URL을 반환합니다.
	 * @param file 업로드할 파일
	 * @return 업로드된 파일의 접근 URL
	 */
	public String uploadBoardRegImage(MultipartFile file) throws IOException {
		String monthFolder = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
		return uploadImageToFtp(
				file,
				ftpProperties.getUploadBoardTargetPath(),
				ftpProperties.getUploadBoardView(),
				new String[] { "reg", monthFolder },
				"reg"
		);
	}

	/**
	 * 에디터 이미지 파일을 FTP 서버에 업로드하고 접근 가능한 URL을 반환합니다.
	 * @param file 업로드할 파일
	 * @return 업로드된 파일의 접근 URL
	 */
	public String uploadEditorImage(MultipartFile file) throws IOException {
		String dateFolder = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		return uploadImageToFtp(
				file,
				ftpProperties.getUploadEditorTargetPath(),
				ftpProperties.getUploadEditorView(),
				new String[] { dateFolder },
				"editor"
		);
	}

	/**
	 * 회사 업무 댓글 첨부파일을 FTP 서버에 업로드하고 접근 가능한 URL을 반환합니다.
	 * @param file 업로드할 첨부파일
	 * @param workSeq 업무 번호
	 * @param replySeq 댓글 번호
	 * @param regNo 등록자 번호
	 * @return 업로드된 첨부파일의 접근 URL
	 */
	public String uploadCompanyWorkReplyFile(MultipartFile file, Long workSeq, Long replySeq, String regNo) throws IOException {
		String[] subDirs = {
				String.valueOf(workSeq),
				String.valueOf(replySeq)
		};
		String fileKeyPrefix = "reply_" + safePathSegment(regNo) + "_" + replySeq;
		return uploadFileToFtp(
				file,
				ftpProperties.getUploadCompanyWorkReplyTargetPath(),
				ftpProperties.getUploadCompanyWorkReplyView(),
				subDirs,
				fileKeyPrefix
		);
	}

	/**
	 * 상품 이미지 파일을 FTP 서버에 업로드하고 접근 가능한 URL을 반환합니다.
	 * @param file 업로드할 파일
	 * @param goodsId 상품 코드
	 * @param regNo 등록자 번호
	 * @return 업로드된 파일의 접근 URL
	 */
	public String uploadGoodsImage(MultipartFile file, String goodsId, String regNo) throws IOException {
		return uploadImageToFtp(
				file,
				ftpProperties.getUploadGoodsTargetPath(),
				ftpProperties.getUploadGoodsView(),
				new String[] { goodsId },
				regNo
		);
	}

	/**
	 * 배너 이미지 파일을 FTP 서버에 업로드하고 접근 가능한 URL을 반환합니다.
	 * @param file 업로드할 파일
	 * @param bannerNo 배너 번호
	 * @param regNo 등록자 번호
	 * @return 업로드된 파일의 접근 URL
	 */
	public String uploadBannerImage(MultipartFile file, Integer bannerNo, String regNo) throws IOException {
		return uploadImageToFtp(
				file,
				ftpProperties.getUploadBannerTargetPath(),
				ftpProperties.getUploadBannerView(),
				new String[] { String.valueOf(bannerNo) },
				regNo
		);
	}

	/**
	 * 기획전 썸네일 이미지를 FTP 서버에 업로드하고 접근 가능한 URL을 반환합니다.
	 * @param file 업로드할 이미지 파일
	 * @param exhibitionNo 기획전 번호
	 * @param regNo 등록자 번호
	 * @return 업로드된 파일의 접근 URL
	 */
	public String uploadExhibitionImage(MultipartFile file, String exhibitionNo, String regNo) throws IOException {
		return uploadImageToFtp(
				file,
				ftpProperties.getUploadExhibitionTargetPath(),
				ftpProperties.getUploadExhibitionView(),
				new String[] { exhibitionNo },
				regNo
		);
	}

	/**
	 * 브랜드 로고 이미지를 FTP 서버에 업로드하고 조회 가능한 URL을 반환합니다.
	 * @param file 업로드할 이미지 파일
	 * @param brandNo 브랜드 번호
	 * @return 업로드된 이미지의 URL
	 */
	public String uploadBrandLogo(MultipartFile file, String brandNo) throws IOException {
		FTPClient ftpClient = new FTPClient();

		try {
			// FTP 서버 연결
			ftpClient.connect(ftpProperties.getHost(), ftpProperties.getPort());
			boolean login = ftpClient.login(ftpProperties.getUsername(), ftpProperties.getPwd());

			if (!login) {
				throw new IOException("FTP 로그인 실패");
			}

			// 설정
			ftpClient.enterLocalPassiveMode();
			ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

			// 업로드 경로 이동
			String targetPath = ftpProperties.getUploadBrandTargetPath();
			ftpClient.changeWorkingDirectory(targetPath);

			// 파일명 생성
			String fileName = buildBrandLogoFileName(file, brandNo);

			// 파일 업로드
			try (InputStream inputStream = file.getInputStream()) {
				boolean uploaded = ftpClient.storeFile(fileName, inputStream);

				if (!uploaded) {
					throw new IOException("FTP 파일 업로드 실패");
				}
			}

			return buildBrandLogoUrl(fileName);
		} finally {
			if (ftpClient.isConnected()) {
				ftpClient.logout();
				ftpClient.disconnect();
			}
		}
	}

	/**
	 * FTP에서 상품 이미지 파일을 삭제합니다.
	 * @param goodsId 상품 코드
	 * @param fileName 삭제할 파일명
	 */
	public void deleteGoodsImage(String goodsId, String fileName) throws IOException {
		deleteFileFromFtp(
				ftpProperties.getUploadGoodsTargetPath(),
				new String[] { goodsId },
				fileName
		);
	}

	/**
	 * FTP에서 배너 이미지 파일을 삭제합니다.
	 * @param bannerNo 배너 번호
	 * @param fileName 삭제할 파일명
	 */
	public void deleteBannerImage(Integer bannerNo, String fileName) throws IOException {
		deleteFileFromFtp(
				ftpProperties.getUploadBannerTargetPath(),
				new String[] { String.valueOf(bannerNo) },
				fileName
		);
	}

	/**
	 * 회사 업무 댓글 첨부파일 URL을 기준으로 FTP 파일을 삭제합니다.
	 * @param fileUrl 삭제할 첨부파일 URL
	 */
	public void deleteCompanyWorkReplyFile(String fileUrl) throws IOException {
		String normalizedFileUrl = safeTrim(fileUrl);
		if (normalizedFileUrl == null) {
			return;
		}

		// 저장 URL에서 댓글 첨부 하위 경로와 파일명을 복원합니다.
		String[] pathSegments = resolvePathSegmentsFromPublicUrl(
				normalizedFileUrl,
				ftpProperties.getUploadCompanyWorkReplyView()
		);
		if (pathSegments.length < 3) {
			return;
		}

		// 마지막 세그먼트는 파일명이고 앞부분은 하위 디렉토리입니다.
		String fileName = pathSegments[pathSegments.length - 1];
		String[] subDirs = new String[pathSegments.length - 1];
		System.arraycopy(pathSegments, 0, subDirs, 0, pathSegments.length - 1);
		deleteFileFromFtp(ftpProperties.getUploadCompanyWorkReplyTargetPath(), subDirs, fileName);
	}

	/**
	 * 공개 URL의 파일을 다운로드해 바이트 배열로 반환합니다.
	 * @param fileUrl 다운로드할 파일 URL
	 * @return 다운로드한 파일 바이트 배열
	 */
	public byte[] downloadFileFromUrl(String fileUrl) throws IOException {
		HttpURLConnection connection = null;

		try {
			// 공개 URL 연결을 열고 응답 상태를 확인합니다.
			connection = (HttpURLConnection) URI.create(fileUrl).toURL().openConnection();
			connection.setInstanceFollowRedirects(true);
			connection.setConnectTimeout(PUBLIC_FILE_DOWNLOAD_CONNECT_TIMEOUT_MILLIS);
			connection.setReadTimeout(PUBLIC_FILE_DOWNLOAD_READ_TIMEOUT_MILLIS);
			connection.setRequestMethod("GET");

			int responseCode = connection.getResponseCode();
			if (responseCode < 200 || responseCode >= 300) {
				throw new IOException("공개 파일 다운로드 실패: " + responseCode);
			}

			// 정상 응답 바디를 모두 읽어 반환합니다.
			try (InputStream inputStream = connection.getInputStream()) {
				return inputStream.readAllBytes();
			}
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	/**
	 * UTF-8 텍스트 파일을 FTP에 임시파일 업로드 후 rename으로 원자적으로 교체합니다.
	 * @param targetPath 업로드 대상 디렉토리 경로
	 * @param finalFileName 최종 파일명
	 * @param content 저장할 UTF-8 텍스트 내용
	 * @return 업로드에 사용한 임시 파일명
	 */
	public String uploadUtf8TextFileAtomically(String targetPath, String finalFileName, String content) throws IOException {
		FTPClient ftpClient = new FTPClient();
		String tempFileName = buildAtomicTempFileName(finalFileName);
		byte[] contentBytes = (content == null ? "" : content).getBytes(StandardCharsets.UTF_8);

		try {
			// 뉴스 스냅샷 업로드가 무한 대기하지 않도록 접속 타임아웃을 먼저 적용합니다.
			configureAtomicTextUploadPreConnectTimeout(ftpClient);

			// FTP 서버 접속 및 로그인 후 이진 업로드 모드를 설정합니다.
			connectAndLoginFtpClient(ftpClient);
			configureAtomicTextUploadPostConnectTimeout(ftpClient);
			ftpClient.enterLocalPassiveMode();
			ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

			// 대상 경로가 없으면 생성하고, 최종 파일명과 임시 파일명을 같은 폴더에서 관리합니다.
			changeOrCreateDirectories(ftpClient, targetPath);

			// 임시 파일로 전체 내용을 먼저 업로드합니다.
			try (InputStream inputStream = new ByteArrayInputStream(contentBytes)) {
				boolean uploaded = ftpClient.storeFile(tempFileName, inputStream);
				if (!uploaded) {
					throw new IOException("FTP 임시 파일 업로드 실패: " + tempFileName);
				}
			}

			// 서버가 overwrite rename을 지원하는 경우 최종 파일을 원자적으로 교체합니다.
			boolean renamed = ftpClient.rename(tempFileName, finalFileName);
			if (!renamed) {
				// rename 실패 시 임시 파일을 정리하고 기존 최종 파일은 유지합니다.
				ftpClient.deleteFile(tempFileName);
				throw new IOException("FTP 파일 rename 실패(원자적 교체 미지원 가능): " + tempFileName + " -> " + finalFileName);
			}

			return tempFileName;
		} finally {
			if (ftpClient.isConnected()) {
				ftpClient.logout();
				ftpClient.disconnect();
			}
		}
	}

	/**
	 * 원자적 텍스트 업로드용 FTP 접속 전 타임아웃을 적용합니다.
	 * @param ftpClient FTP 클라이언트
	 */
	private void configureAtomicTextUploadPreConnectTimeout(FTPClient ftpClient) {
		ftpClient.setConnectTimeout(ATOMIC_TEXT_UPLOAD_CONNECT_TIMEOUT_MILLIS);
		ftpClient.setDefaultTimeout(ATOMIC_TEXT_UPLOAD_SOCKET_TIMEOUT_MILLIS);
	}

	/**
	 * 원자적 텍스트 업로드용 FTP 접속 후 타임아웃을 적용합니다.
	 * @param ftpClient FTP 클라이언트
	 * @throws IOException FTP 소켓 타임아웃 적용 실패 시 발생
	 */
	private void configureAtomicTextUploadPostConnectTimeout(FTPClient ftpClient) throws IOException {
		ftpClient.setSoTimeout(ATOMIC_TEXT_UPLOAD_SOCKET_TIMEOUT_MILLIS);
		ftpClient.setDataTimeout(Duration.ofMillis(ATOMIC_TEXT_UPLOAD_DATA_TIMEOUT_MILLIS));
	}

	/**
	 * 뉴스 스냅샷 업로드 대상 경로를 반환합니다.
	 * @param defaultPath 설정 미존재 시 사용할 기본 경로
	 * @return 뉴스 스냅샷 업로드 경로
	 */
	public String resolveNewsSnapshotTargetPath(String defaultPath) {
		String configuredPath = ftpProperties.getNewsSnapshotTargetPath();
		if (configuredPath == null || configuredPath.trim().isEmpty()) {
			return defaultPath;
		}
		return configuredPath.trim();
	}

	/**
	 * 상품 이미지 접근 URL을 생성합니다.
	 * @param goodsId 상품 코드
	 * @param fileName 파일명
	 * @return 접근 가능한 URL
	 */
	public String buildGoodsImageUrl(String goodsId, String fileName) {
		if (goodsId == null || goodsId.trim().isEmpty() || fileName == null || fileName.trim().isEmpty()) {
			return null;
		}
		return ftpProperties.getUploadGoodsView() + "/" + goodsId + "/" + fileName;
	}

	/**
	 * FTP에 이미지 파일을 업로드하고 접근 가능한 URL을 반환합니다.
	 * @param file 업로드할 파일
	 * @param targetPath 업로드 대상 기본 경로
	 * @param viewBase 웹 접근 기본 URL
	 * @param subDirs 추가로 생성할 하위 폴더 목록
	 * @param fileKeyPrefix 파일명 생성에 사용할 키
	 * @return 업로드된 파일의 접근 URL
	 */
	private String uploadImageToFtp(
			MultipartFile file,
			String targetPath,
			String viewBase,
			String[] subDirs,
			String fileKeyPrefix
	) throws IOException {
		return uploadFileToFtp(file, targetPath, viewBase, subDirs, fileKeyPrefix);
	}

	/**
	 * FTP에 일반 파일을 업로드하고 접근 가능한 URL을 반환합니다.
	 * @param file 업로드할 파일
	 * @param targetPath 업로드 대상 기본 경로
	 * @param viewBase 웹 접근 기본 URL
	 * @param subDirs 추가로 생성할 하위 폴더 목록
	 * @param fileKeyPrefix 파일명 생성에 사용할 키
	 * @return 업로드된 파일의 접근 URL
	 */
	private String uploadFileToFtp(
			MultipartFile file,
			String targetPath,
			String viewBase,
			String[] subDirs,
			String fileKeyPrefix
	) throws IOException {
		FTPClient ftpClient = new FTPClient();

		try {
			// FTP 서버 연결
			ftpClient.connect(ftpProperties.getHost(), ftpProperties.getPort());
			boolean login = ftpClient.login(ftpProperties.getUsername(), ftpProperties.getPwd());

			if (!login) {
				throw new IOException("FTP 로그인 실패");
			}

			// 설정
			ftpClient.enterLocalPassiveMode();
			ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

			// 업로드 기본 경로가 없으면 생성하고, 있으면 해당 경로로 이동합니다.
			changeOrCreateDirectories(ftpClient, targetPath);

			// 하위 폴더 생성/이동
			for (String subDir : subDirs) {
				ensureAndChangeDirectory(ftpClient, subDir);
			}

			// 파일명 생성
			String fileName = buildFileName(file, fileKeyPrefix);

			// 파일 업로드
			try (InputStream inputStream = file.getInputStream()) {
				boolean uploaded = ftpClient.storeFile(fileName, inputStream);

				if (!uploaded) {
					throw new IOException("FTP 파일 업로드 실패");
				}
			}

			// 웹에서 접근 가능한 URL 반환
			return buildPublicFileUrl(viewBase, subDirs, fileName);
		} finally {
			if (ftpClient.isConnected()) {
				ftpClient.logout();
				ftpClient.disconnect();
			}
		}
	}

	/**
	 * FTP에서 파일을 삭제합니다.
	 * @param targetPath 업로드 대상 기본 경로
	 * @param subDirs 하위 폴더 목록
	 * @param fileName 삭제할 파일명
	 */
	private void deleteFileFromFtp(String targetPath, String[] subDirs, String fileName) throws IOException {
		FTPClient ftpClient = new FTPClient();
		try {
			ftpClient.connect(ftpProperties.getHost(), ftpProperties.getPort());
			boolean login = ftpClient.login(ftpProperties.getUsername(), ftpProperties.getPwd());
			if (!login) {
				throw new IOException("FTP 로그인 실패");
			}
			ftpClient.enterLocalPassiveMode();
			ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
			ftpClient.changeWorkingDirectory(targetPath);
			for (String subDir : subDirs) {
				if (!ftpClient.changeWorkingDirectory(subDir)) {
					return;
				}
			}
			ftpClient.deleteFile(fileName);
		} finally {
			if (ftpClient.isConnected()) {
				ftpClient.logout();
				ftpClient.disconnect();
			}
		}
	}

	/**
	 * FTP 서버에 접속하고 로그인합니다.
	 * @param ftpClient FTP 클라이언트
	 */
	private void connectAndLoginFtpClient(FTPClient ftpClient) throws IOException {
		ftpClient.connect(ftpProperties.getHost(), ftpProperties.getPort());
		boolean login = ftpClient.login(ftpProperties.getUsername(), ftpProperties.getPwd());
		if (!login) {
			throw new IOException("FTP 로그인 실패");
		}
	}

	/**
	 * 절대/상대 경로 문자열을 기준으로 FTP 디렉토리를 생성 및 이동합니다.
	 * @param ftpClient FTP 클라이언트
	 * @param targetPath 대상 경로
	 */
	private void changeOrCreateDirectories(FTPClient ftpClient, String targetPath) throws IOException {
		String normalizedTargetPath = targetPath == null ? "" : targetPath.trim();
		if (normalizedTargetPath.isEmpty()) {
			throw new IOException("FTP 대상 경로가 비어 있습니다.");
		}

		// 절대 경로는 루트부터 이동해 폴더를 순차 생성합니다.
		if (normalizedTargetPath.startsWith("/")) {
			if (!ftpClient.changeWorkingDirectory("/")) {
				throw new IOException("FTP 루트 경로 이동 실패");
			}
		}

		// 경로 세그먼트 단위로 디렉토리를 생성/이동합니다.
		String[] pathSegments = normalizedTargetPath.split("/");
		for (String pathSegment : pathSegments) {
			String normalizedSegment = pathSegment == null ? "" : pathSegment.trim();
			if (normalizedSegment.isEmpty()) {
				continue;
			}
			ensureAndChangeDirectory(ftpClient, normalizedSegment);
		}
	}

	/**
	 * 원자적 교체용 임시 파일명을 생성합니다.
	 * @param finalFileName 최종 파일명
	 * @return 임시 파일명
	 */
	private String buildAtomicTempFileName(String finalFileName) {
		String timeKey = LocalDateTime.now().format(FTP_TEMP_FILE_TIME_FORMATTER);
		return finalFileName + ".tmp." + timeKey;
	}

	/**
	 * FTP 디렉토리를 확인하고 없으면 생성 후 이동합니다.
	 * @param ftpClient FTP 클라이언트
	 * @param directoryName 확인할 디렉토리명
	 */
	private void ensureAndChangeDirectory(FTPClient ftpClient, String directoryName) throws IOException {
		if (!ftpClient.changeWorkingDirectory(directoryName)) {
			if (ftpClient.makeDirectory(directoryName)) {
				ftpClient.changeWorkingDirectory(directoryName);
			} else {
				throw new IOException("FTP 폴더 생성 실패: " + directoryName);
			}
		}
	}

	/**
	 * 업로드할 파일명을 생성합니다.
	 * @param file 업로드 대상 파일
	 * @param folderKey 업로드 폴더 식별자
	 * @return 생성된 파일명
	 */
	private String buildFileName(MultipartFile file, String folderKey) {
		String originalFilename = file.getOriginalFilename();
		String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
		return folderKey + "_" + System.currentTimeMillis() + extension;
	}

	/**
	 * 브랜드 로고 파일명을 생성합니다.
	 * @param file 업로드 파일
	 * @param brandNo 브랜드 번호
	 * @return 브랜드 번호_시분초밀리초.확장자 형식의 파일명
	 */
	private String buildBrandLogoFileName(MultipartFile file, String brandNo) {
		String originalFilename = file.getOriginalFilename();
		String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
		String timeKey = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
		String normalizedBrandNo = brandNo.trim();
		return normalizedBrandNo + "_" + timeKey + extension;
	}

	/**
	 * 브랜드 로고 조회 URL을 생성합니다.
	 * @param fileName 업로드된 파일명
	 * @return 조회 가능한 URL
	 */
	private String buildBrandLogoUrl(String fileName) {
		String viewBase = ftpProperties.getUploadBrandViewBase();
		String targetPath = ftpProperties.getUploadBrandTargetPath();
		String normalizedViewBase = viewBase.endsWith("/") ? viewBase.substring(0, viewBase.length() - 1) : viewBase;
		String normalizedTargetPath = targetPath.startsWith("/") ? targetPath : "/" + targetPath;
		return normalizedViewBase + normalizedTargetPath + "/" + fileName;
	}

	/**
	 * 공개 URL 기본 경로와 하위 경로와 파일명으로 최종 URL을 생성합니다.
	 * @param viewBase 공개 URL 기본 경로
	 * @param subDirs 하위 디렉토리 목록
	 * @param fileName 파일명
	 * @return 최종 공개 URL
	 */
	private String buildPublicFileUrl(String viewBase, String[] subDirs, String fileName) {
		String normalizedViewBase = trimTrailingSlash(viewBase);
		String joinedSubDirs = subDirs == null || subDirs.length == 0 ? "" : "/" + String.join("/", subDirs);
		return normalizedViewBase + joinedSubDirs + "/" + fileName;
	}

	/**
	 * 공개 URL에서 기준 view 경로 이후의 세그먼트 목록을 복원합니다.
	 * @param fileUrl 첨부파일 URL
	 * @param viewBase 기준 공개 URL
	 * @return 하위 경로 세그먼트 목록
	 */
	private String[] resolvePathSegmentsFromPublicUrl(String fileUrl, String viewBase) {
		String normalizedFileUrl = safeTrim(fileUrl);
		String normalizedViewBase = trimTrailingSlash(viewBase);
		if (normalizedFileUrl == null || normalizedViewBase == null || !normalizedFileUrl.startsWith(normalizedViewBase + "/")) {
			return new String[0];
		}

		// 공개 URL 기준 경로를 제거한 뒤 세그먼트를 분리합니다.
		String relativePath = normalizedFileUrl.substring(normalizedViewBase.length() + 1);
		return relativePath.split("/");
	}

	/**
	 * 경로 세그먼트로 사용할 문자열을 안전하게 정리합니다.
	 * @param value 정리할 문자열
	 * @return 정리된 경로 세그먼트
	 */
	private String safePathSegment(String value) {
		String normalizedValue = safeTrim(value);
		return normalizedValue == null ? "anonymous" : normalizedValue;
	}

	/**
	 * 문자열 양끝 공백을 제거하고 빈 값은 null로 변환합니다.
	 * @param value 정리할 문자열
	 * @return 정리된 문자열
	 */
	private String safeTrim(String value) {
		if (value == null) {
			return null;
		}
		String trimmedValue = value.trim();
		return trimmedValue.isEmpty() ? null : trimmedValue;
	}

	/**
	 * URL 끝의 슬래시를 제거합니다.
	 * @param value 정리할 URL 문자열
	 * @return 끝 슬래시가 제거된 URL
	 */
	private String trimTrailingSlash(String value) {
		String normalizedValue = safeTrim(value);
		if (normalizedValue == null) {
			return null;
		}
		return normalizedValue.endsWith("/") ? normalizedValue.substring(0, normalizedValue.length() - 1) : normalizedValue;
	}
}
