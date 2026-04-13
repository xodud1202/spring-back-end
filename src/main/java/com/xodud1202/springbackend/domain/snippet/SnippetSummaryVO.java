package com.xodud1202.springbackend.domain.snippet;

import java.time.LocalDateTime;

// 스니펫 목록 카드에 필요한 요약 정보를 전달합니다.
public record SnippetSummaryVO(
	Long snippetNo,
	String title,
	String summary,
	String languageCd,
	String languageNm,
	String favoriteYn,
	Long folderNo,
	String folderNm,
	String tagNameText,
	Integer viewCnt,
	Integer copyCnt,
	LocalDateTime lastViewedDt,
	LocalDateTime lastCopiedDt,
	String duplicateYn,
	LocalDateTime regDt,
	LocalDateTime udtDt
) {
}
