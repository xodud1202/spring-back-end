package com.xodud1202.springbackend.domain.snippet;

import java.time.LocalDateTime;
import java.util.List;

// 스니펫 상세 화면에 필요한 응답 데이터를 전달합니다.
public record SnippetDetailVO(
	Long snippetNo,
	Long folderNo,
	String languageCd,
	String title,
	String summary,
	String snippetBody,
	String memo,
	String favoriteYn,
	LocalDateTime lastCopiedDt,
	LocalDateTime regDt,
	LocalDateTime udtDt,
	List<Long> tagNoList
) {
}
