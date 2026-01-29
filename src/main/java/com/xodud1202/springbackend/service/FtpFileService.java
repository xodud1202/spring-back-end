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
			
			// resume 업로드 경로 사용
			String uploadDir = ftpProperties.getUploadResumeFaceImgTargetPath();
			ftpClient.changeWorkingDirectory(uploadDir);

			// usrNo 폴더 확인 및 생성 로직 추가
			if (!ftpClient.changeWorkingDirectory(usrNo)) {
				// 폴더가 없으면 생성 시도
				if (ftpClient.makeDirectory(usrNo)) {
					ftpClient.changeWorkingDirectory(usrNo);
				} else {
					throw new IOException("FTP 사용자 폴더 생성 실패: " + usrNo);
				}
			}
			
			// 파일명 생성
			String fileName = buildFileName(file, usrNo);
			
			// 파일 업로드
			try (InputStream inputStream = file.getInputStream()) {
				boolean uploaded = ftpClient.storeFile(fileName, inputStream);
				
				if (!uploaded) {
					throw new IOException("FTP 파일 업로드 실패");
				}
			}
			
			// 웹에서 접근 가능한 URL 반환
			return ftpProperties.getUploadResumeFaceImgView() + "/" + usrNo + "/" + fileName;
		} finally {
			if (ftpClient.isConnected()) {
				ftpClient.logout();
				ftpClient.disconnect();
			}
		}
	}

	/**
	 * 게시글 이미지 파일을 FTP 서버에 업로드하고 접근 가능한 URL을 반환합니다.
	 * @param file 업로드할 파일
	 * @param boardNo 게시글 번호
	 * @return 업로드된 파일의 접근 URL
	 */
	public String uploadBoardImage(MultipartFile file, Long boardNo) throws IOException {
		FTPClient ftpClient = new FTPClient();
		String folderName = String.valueOf(boardNo);

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

			// 게시판 업로드 경로 사용
			String uploadDir = ftpProperties.getUploadBoardTargetPath();
			ftpClient.changeWorkingDirectory(uploadDir);

			// boardNo 폴더 확인 및 생성
			if (!ftpClient.changeWorkingDirectory(folderName)) {
				if (ftpClient.makeDirectory(folderName)) {
					ftpClient.changeWorkingDirectory(folderName);
				} else {
					throw new IOException("FTP 게시글 폴더 생성 실패: " + folderName);
				}
			}

			// 파일명 생성
			String fileName = buildFileName(file, folderName);

			// 파일 업로드
			try (InputStream inputStream = file.getInputStream()) {
				boolean uploaded = ftpClient.storeFile(fileName, inputStream);

				if (!uploaded) {
					throw new IOException("FTP 파일 업로드 실패");
				}
			}

			// 웹에서 접근 가능한 URL 반환
			return ftpProperties.getUploadBoardView() + "/" + folderName + "/" + fileName;
		} finally {
			if (ftpClient.isConnected()) {
				ftpClient.logout();
				ftpClient.disconnect();
			}
		}
	}

	/**
	 * 게시글 등록 중 이미지 파일을 FTP 서버에 업로드하고 접근 가능한 URL을 반환합니다.
	 * @param file 업로드할 파일
	 * @return 업로드된 파일의 접근 URL
	 */
	public String uploadBoardRegImage(MultipartFile file) throws IOException {
		FTPClient ftpClient = new FTPClient();
		String monthFolder = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));

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

			// 게시판 등록 임시 업로드 경로 사용
			String uploadDir = ftpProperties.getUploadBoardTargetPath();
			ftpClient.changeWorkingDirectory(uploadDir);

			// reg 폴더 확인 및 생성
			if (!ftpClient.changeWorkingDirectory("reg")) {
				if (ftpClient.makeDirectory("reg")) {
					ftpClient.changeWorkingDirectory("reg");
				} else {
					throw new IOException("FTP 게시글 reg 폴더 생성 실패");
				}
			}

			// 월 폴더 확인 및 생성
			if (!ftpClient.changeWorkingDirectory(monthFolder)) {
				if (ftpClient.makeDirectory(monthFolder)) {
					ftpClient.changeWorkingDirectory(monthFolder);
				} else {
					throw new IOException("FTP 게시글 월 폴더 생성 실패: " + monthFolder);
				}
			}

			// 파일명 생성
			String fileName = buildFileName(file, "reg");

			// 파일 업로드
			try (InputStream inputStream = file.getInputStream()) {
				boolean uploaded = ftpClient.storeFile(fileName, inputStream);

				if (!uploaded) {
					throw new IOException("FTP 파일 업로드 실패");
				}
			}

			// 웹에서 접근 가능한 URL 반환
			return ftpProperties.getUploadBoardView() + "/reg/" + monthFolder + "/" + fileName;
		} finally {
			if (ftpClient.isConnected()) {
				ftpClient.logout();
				ftpClient.disconnect();
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
