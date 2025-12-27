package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.common.FtpProperties;
import lombok.RequiredArgsConstructor;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class FtpFileService {
	
	private final FtpProperties ftpProperties;
	
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
			String originalFilename = file.getOriginalFilename();
			String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
			String fileName = usrNo + "_" + System.currentTimeMillis() + extension;
			
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
}