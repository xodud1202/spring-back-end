package com.xodud1202.springbackend.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
// 다이닝브랜즈그룹 Jira 첨부파일 다운로드 결과를 정의합니다.
public class DiningBrandsGroupJiraAttachmentDownloadResult {
	// 첨부파일 바이너리 데이터입니다.
	private final byte[] content;
	// 첨부파일 콘텐츠 타입입니다.
	private final String contentType;
	// 첨부파일 크기입니다.
	private final long contentLength;
}
