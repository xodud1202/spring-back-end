package com.xodud1202.springbackend.domain.common;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "nas.ftp")
public class FtpProperties {
	// 접속 정보
	private String host;
	private int port;
	private String username;
	private String pwd;
	
	// default upload 설정
	private String uploadDefaultTargetPath;
	private int uploadDefaultMaxSize;
	private String uploadDefaultAllowExtension;
	private String uploadDefaultView;
	
	// resume upload 설정
	private int uploadResumeMaxSize;
	private String uploadResumeAllowExtension;
	private String uploadResumeFaceImgTargetPath;
	private String uploadResumeFaceImgView;
	private String uploadResumeEducationTargetPath;
	private String uploadResumeEducationView;

	// board upload 설정
	private int uploadBoardMaxSize;
	private String uploadBoardAllowExtension;
	private String uploadBoardTargetPath;
	private String uploadBoardView;

	// editor upload 설정
	private int uploadEditorMaxSize;
	private String uploadEditorAllowExtension;
	private String uploadEditorTargetPath;
	private String uploadEditorView;

	// goods upload 설정
	private int uploadGoodsMaxSize;
	private String uploadGoodsAllowExtension;
	private String uploadGoodsTargetPath;
	private String uploadGoodsView;

	// banner upload 설정
	private int uploadBannerMaxSize;
	private String uploadBannerAllowExtension;
	private String uploadBannerTargetPath;
	private String uploadBannerView;

	// brand upload 설정
	private int uploadBrandMaxSize;
	private String uploadBrandAllowExtension;
	private String uploadBrandTargetPath;
	private String uploadBrandViewBase;
}
