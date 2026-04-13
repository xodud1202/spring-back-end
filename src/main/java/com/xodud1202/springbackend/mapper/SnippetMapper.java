package com.xodud1202.springbackend.mapper;

import com.xodud1202.springbackend.common.mybatis.GeneratedLongKey;
import com.xodud1202.springbackend.domain.snippet.SnippetDetailRowVO;
import com.xodud1202.springbackend.domain.snippet.SnippetFolderSavePO;
import com.xodud1202.springbackend.domain.snippet.SnippetFolderVO;
import com.xodud1202.springbackend.domain.snippet.SnippetLanguageVO;
import com.xodud1202.springbackend.domain.snippet.SnippetListQueryPO;
import com.xodud1202.springbackend.domain.snippet.SnippetSavePO;
import com.xodud1202.springbackend.domain.snippet.SnippetSummaryVO;
import com.xodud1202.springbackend.domain.snippet.SnippetTagSavePO;
import com.xodud1202.springbackend.domain.snippet.SnippetTagVO;
import com.xodud1202.springbackend.domain.snippet.SnippetUserCreatePO;
import com.xodud1202.springbackend.domain.snippet.SnippetUserSessionVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

// 스니펫 기능 전용 MyBatis 매퍼를 정의합니다.
public interface SnippetMapper {
	// 구글 식별값으로 스니펫 사용자를 조회합니다.
	SnippetUserSessionVO getSnippetUserByGoogleSub(@Param("googleSub") String googleSub);

	// 사용자 번호로 스니펫 사용자를 조회합니다.
	SnippetUserSessionVO getSnippetUserByUserNo(@Param("snippetUserNo") Long snippetUserNo);

	// 신규 스니펫 사용자를 등록합니다.
	int insertSnippetUser(
		@Param("command") SnippetUserCreatePO command,
		@Param("generatedKey") GeneratedLongKey generatedKey
	);

	// 신규 등록 사용자 감사 번호를 사용자 번호 기준으로 갱신합니다.
	int updateSnippetUserAuditNo(@Param("snippetUserNo") Long snippetUserNo, @Param("auditNo") Long auditNo);

	// 로그인 시 사용자 기본 정보를 최신 값으로 갱신합니다.
	int updateSnippetUserLoginInfo(
		@Param("snippetUserNo") Long snippetUserNo,
		@Param("email") String email,
		@Param("userNm") String userNm,
		@Param("profileImgUrl") String profileImgUrl,
		@Param("auditNo") Long auditNo
	);

	// 언어 마스터 목록을 조회합니다.
	List<SnippetLanguageVO> getSnippetLanguageList();

	// 사용자별 폴더 목록과 스니펫 건수를 조회합니다.
	List<SnippetFolderVO> getSnippetFolderList(@Param("snippetUserNo") Long snippetUserNo);

	// 사용자별 태그 목록과 스니펫 건수를 조회합니다.
	List<SnippetTagVO> getSnippetTagList(@Param("snippetUserNo") Long snippetUserNo);

	// 최근 조회한 스니펫 목록을 조회합니다.
	List<SnippetSummaryVO> getRecentViewedSnippetList(@Param("snippetUserNo") Long snippetUserNo, @Param("limit") int limit);

	// 최근 복사한 스니펫 목록을 조회합니다.
	List<SnippetSummaryVO> getRecentCopiedSnippetList(@Param("snippetUserNo") Long snippetUserNo, @Param("limit") int limit);

	// 사용자 소유 폴더 존재 여부를 확인합니다.
	int countFolderByUserNo(@Param("snippetUserNo") Long snippetUserNo, @Param("folderNo") Long folderNo);

	// 사용자 소유 태그 존재 여부를 확인합니다.
	int countTagByUserNo(@Param("snippetUserNo") Long snippetUserNo, @Param("tagNo") Long tagNo);

	// 사용 가능한 언어 코드 존재 여부를 확인합니다.
	int countLanguageByLanguageCd(@Param("languageCd") String languageCd);

	// 사용자 소유 태그 목록 건수를 확인합니다.
	int countOwnedTagList(@Param("snippetUserNo") Long snippetUserNo, @Param("tagNoList") List<Long> tagNoList);

	// 폴더명 중복 건수를 조회합니다.
	int getFolderDuplicateCount(
		@Param("snippetUserNo") Long snippetUserNo,
		@Param("folderNm") String folderNm,
		@Param("excludeFolderNo") Long excludeFolderNo
	);

	// 태그명 중복 건수를 조회합니다.
	int getTagDuplicateCount(
		@Param("snippetUserNo") Long snippetUserNo,
		@Param("tagNm") String tagNm,
		@Param("excludeTagNo") Long excludeTagNo
	);

	// 폴더 정렬순서 최대값을 조회합니다.
	Integer getMaxFolderSortSeq(@Param("snippetUserNo") Long snippetUserNo);

	// 태그 정렬순서 최대값을 조회합니다.
	Integer getMaxTagSortSeq(@Param("snippetUserNo") Long snippetUserNo);

	// 스니펫 목록 총 건수를 조회합니다.
	int getSnippetCount(@Param("snippetUserNo") Long snippetUserNo, @Param("query") SnippetListQueryPO query);

	// 스니펫 목록을 조회합니다.
	List<SnippetSummaryVO> getSnippetList(@Param("snippetUserNo") Long snippetUserNo, @Param("query") SnippetListQueryPO query);

	// 스니펫 상세 본문을 조회합니다.
	SnippetDetailRowVO getSnippetDetail(@Param("snippetUserNo") Long snippetUserNo, @Param("snippetNo") Long snippetNo);

	// 스니펫에 연결된 태그 번호 목록을 조회합니다.
	List<Long> getSnippetTagNoList(@Param("snippetUserNo") Long snippetUserNo, @Param("snippetNo") Long snippetNo);

	// 신규 스니펫을 등록합니다.
	int insertSnippetBase(
		@Param("snippetUserNo") Long snippetUserNo,
		@Param("command") SnippetSavePO command,
		@Param("bodyHash") String bodyHash,
		@Param("auditNo") Long auditNo,
		@Param("generatedKey") GeneratedLongKey generatedKey
	);

	// 기존 스니펫을 수정합니다.
	int updateSnippetBase(
		@Param("snippetUserNo") Long snippetUserNo,
		@Param("snippetNo") Long snippetNo,
		@Param("command") SnippetSavePO command,
		@Param("bodyHash") String bodyHash,
		@Param("auditNo") Long auditNo
	);

	// 스니펫을 소프트 삭제합니다.
	int softDeleteSnippet(
		@Param("snippetUserNo") Long snippetUserNo,
		@Param("snippetNo") Long snippetNo,
		@Param("auditNo") Long auditNo
	);

	// 스니펫 즐겨찾기 여부를 갱신합니다.
	int updateSnippetFavorite(
		@Param("snippetUserNo") Long snippetUserNo,
		@Param("snippetNo") Long snippetNo,
		@Param("favoriteYn") String favoriteYn,
		@Param("auditNo") Long auditNo
	);

	// 스니펫 마지막 복사 일시를 갱신합니다.
	int updateSnippetLastCopiedDt(
		@Param("snippetUserNo") Long snippetUserNo,
		@Param("snippetNo") Long snippetNo,
		@Param("auditNo") Long auditNo
	);

	// 스니펫 마지막 조회 일시와 조회 수를 갱신합니다.
	int updateSnippetLastViewedDt(
		@Param("snippetUserNo") Long snippetUserNo,
		@Param("snippetNo") Long snippetNo,
		@Param("auditNo") Long auditNo
	);

	// 스니펫 태그 매핑을 모두 제거합니다.
	int deleteSnippetTagMapBySnippetNo(@Param("snippetUserNo") Long snippetUserNo, @Param("snippetNo") Long snippetNo);

	// 스니펫 태그 매핑을 일괄 등록합니다.
	int insertSnippetTagMapList(
		@Param("snippetNo") Long snippetNo,
		@Param("tagNoList") List<Long> tagNoList,
		@Param("auditNo") Long auditNo
	);

	// 폴더를 등록합니다.
	int insertSnippetFolder(
		@Param("snippetUserNo") Long snippetUserNo,
		@Param("command") SnippetFolderSavePO command,
		@Param("resolvedSortSeq") Integer resolvedSortSeq,
		@Param("auditNo") Long auditNo,
		@Param("generatedKey") GeneratedLongKey generatedKey
	);

	// 폴더를 수정합니다.
	int updateSnippetFolder(
		@Param("snippetUserNo") Long snippetUserNo,
		@Param("folderNo") Long folderNo,
		@Param("command") SnippetFolderSavePO command,
		@Param("resolvedSortSeq") Integer resolvedSortSeq,
		@Param("auditNo") Long auditNo
	);

	// 폴더를 소프트 삭제합니다.
	int softDeleteSnippetFolder(
		@Param("snippetUserNo") Long snippetUserNo,
		@Param("folderNo") Long folderNo,
		@Param("auditNo") Long auditNo
	);

	// 폴더 삭제 전에 연결 스니펫의 폴더를 해제합니다.
	int clearSnippetFolder(
		@Param("snippetUserNo") Long snippetUserNo,
		@Param("folderNo") Long folderNo,
		@Param("auditNo") Long auditNo
	);

	// 태그를 등록합니다.
	int insertSnippetTag(
		@Param("snippetUserNo") Long snippetUserNo,
		@Param("command") SnippetTagSavePO command,
		@Param("resolvedSortSeq") Integer resolvedSortSeq,
		@Param("auditNo") Long auditNo,
		@Param("generatedKey") GeneratedLongKey generatedKey
	);

	// 태그를 수정합니다.
	int updateSnippetTag(
		@Param("snippetUserNo") Long snippetUserNo,
		@Param("tagNo") Long tagNo,
		@Param("command") SnippetTagSavePO command,
		@Param("resolvedSortSeq") Integer resolvedSortSeq,
		@Param("auditNo") Long auditNo
	);

	// 태그를 소프트 삭제합니다.
	int softDeleteSnippetTag(
		@Param("snippetUserNo") Long snippetUserNo,
		@Param("tagNo") Long tagNo,
		@Param("auditNo") Long auditNo
	);

	// 태그 삭제 전에 관련 태그 매핑을 제거합니다.
	int deleteSnippetTagMapByTagNo(@Param("snippetUserNo") Long snippetUserNo, @Param("tagNo") Long tagNo);
}
