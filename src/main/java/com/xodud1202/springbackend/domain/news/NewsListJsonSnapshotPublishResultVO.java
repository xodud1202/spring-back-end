package com.xodud1202.springbackend.domain.news;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
// 뉴스 목록 JSON 스냅샷 업로드 결과 정보를 보관합니다.
public class NewsListJsonSnapshotPublishResultVO {
	private String targetPath;
	private String fileName;
	private String tempFileName;
	private Integer targetCount;
	private Integer successTargetCount;
	private Integer failedTargetCount;
	private Integer jsonByteSize;
}
