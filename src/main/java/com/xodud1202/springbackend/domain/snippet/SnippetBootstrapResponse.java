package com.xodud1202.springbackend.domain.snippet;

import java.util.List;

// 스니펫 메인 화면 초기 구동에 필요한 데이터를 묶어 전달합니다.
public record SnippetBootstrapResponse(
	SnippetUserSessionVO currentUser,
	List<SnippetLanguageVO> languageList,
	List<SnippetFolderVO> folderList,
	List<SnippetTagVO> tagList
) {
}
