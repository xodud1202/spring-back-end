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
	private String uploadResumeTargetPath;
	private int uploadResumeMaxSize;
	private String uploadResumeAllowExtension;
	private String uploadResumeView;
}
