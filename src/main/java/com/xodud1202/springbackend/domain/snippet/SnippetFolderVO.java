package com.xodud1202.springbackend.domain.snippet;

// 사용자별 폴더 정보를 전달합니다.
public record SnippetFolderVO(
	Long folderNo,
	Long snippetUserNo,
	String folderNm,
	String colorHex,
	Integer sortSeq,
	Long snippetCount
) {
}
