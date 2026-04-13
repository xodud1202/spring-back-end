package com.xodud1202.springbackend.domain.snippet;

import java.time.LocalDateTime;

// 스니펫 상세 본문 조회용 기본 행 데이터를 전달합니다.
public record SnippetDetailRowVO(
	Long snippetNo,
	Long folderNo,
	String languageCd,
	String title,
	String summary,
	String snippetBody,
	String memo,
	String favoriteYn,
	Integer viewCnt,
	Integer copyCnt,
	LocalDateTime lastViewedDt,
	LocalDateTime lastCopiedDt,
	String duplicateYn,
	LocalDateTime regDt,
	LocalDateTime udtDt
) {
}
