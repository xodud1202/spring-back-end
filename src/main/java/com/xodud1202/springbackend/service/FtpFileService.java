package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.common.FtpProperties;
import lombok.RequiredArgsConstructor;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class FtpFileService {
	
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
			ftpClient.changeWorkingDirectory(targetPath);

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
			return viewBase + "/" + String.join("/", subDirs) + "/" + fileName;
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
}
