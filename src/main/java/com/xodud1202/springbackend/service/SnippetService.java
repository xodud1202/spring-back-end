package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.common.mybatis.GeneratedLongKey;
import com.xodud1202.springbackend.domain.snippet.SnippetBootstrapResponse;
import com.xodud1202.springbackend.domain.snippet.SnippetDetailRowVO;
import com.xodud1202.springbackend.domain.snippet.SnippetDetailVO;
import com.xodud1202.springbackend.domain.snippet.SnippetFolderSavePO;
import com.xodud1202.springbackend.domain.snippet.SnippetFolderVO;
import com.xodud1202.springbackend.domain.snippet.SnippetListQueryPO;
import com.xodud1202.springbackend.domain.snippet.SnippetListResponse;
import com.xodud1202.springbackend.domain.snippet.SnippetSavePO;
import com.xodud1202.springbackend.domain.snippet.SnippetSaveResponse;
import com.xodud1202.springbackend.domain.snippet.SnippetSummaryVO;
import com.xodud1202.springbackend.domain.snippet.SnippetTagSavePO;
import com.xodud1202.springbackend.domain.snippet.SnippetTagVO;
import com.xodud1202.springbackend.domain.snippet.SnippetUserSessionVO;
import com.xodud1202.springbackend.mapper.SnippetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
// 스니펫 메인 화면과 CRUD 비즈니스 로직을 처리합니다.
public class SnippetService {
	private static final int DEFAULT_PAGE = 1;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_PAGE_SIZE = 100;
	private static final int RECENT_SNIPPET_LIMIT = 5;
	private static final String QUICK_FILTER_ALL = "all";
	private static final String QUICK_FILTER_FAVORITE = "favorite";
	private static final String QUICK_FILTER_RECENT_VIEWED = "recent_viewed";
	private static final String QUICK_FILTER_RECENT_COPIED = "recent_copied";
	private static final String QUICK_FILTER_DUPLICATE = "duplicate";
	private static final String SORT_BY_UPDATED_DESC = "updated_desc";
	private static final String SORT_BY_VIEWED_DESC = "viewed_desc";
	private static final String SORT_BY_COPIED_DESC = "copied_desc";
	private static final String SORT_BY_COPY_COUNT_DESC = "copy_count_desc";
	private static final String SORT_BY_TITLE_ASC = "title_asc";

	private final SnippetMapper snippetMapper;
	private final SnippetAuthService snippetAuthService;

	// 스니펫 메인 화면 초기 데이터를 조회합니다.
	public SnippetBootstrapResponse getBootstrap(Long snippetUserNo) {
		// 로그인 사용자와 보조 목록, 최근 사용 목록을 한 번에 묶어 반환합니다.
		SnippetUserSessionVO currentUser = snippetAuthService.getRequiredSnippetUser(snippetUserNo);
		return new SnippetBootstrapResponse(
			currentUser,
			snippetMapper.getSnippetLanguageList(),
			snippetMapper.getSnippetFolderList(snippetUserNo),
			snippetMapper.getSnippetTagList(snippetUserNo),
			snippetMapper.getRecentViewedSnippetList(snippetUserNo, RECENT_SNIPPET_LIMIT),
			snippetMapper.getRecentCopiedSnippetList(snippetUserNo, RECENT_SNIPPET_LIMIT)
		);
	}

	// 조건에 맞는 스니펫 목록을 조회합니다.
	public SnippetListResponse getSnippetList(
		Long snippetUserNo,
		String q,
		Long folderNo,
		Long tagNo,
		String languageCd,
		String favoriteYn,
		String includeBodyYn,
		String sortBy,
		String quickFilter,
		Integer page,
		Integer size
	) {
		// 세션 사용자를 확인하고 목록 조회용 조건을 정규화합니다.
		snippetAuthService.getRequiredSnippetUser(snippetUserNo);
		SnippetListQueryPO query = buildSnippetListQuery(q, folderNo, tagNo, languageCd, favoriteYn, includeBodyYn, sortBy, quickFilter, page, size);
		int totalCount = snippetMapper.getSnippetCount(snippetUserNo, query);
		List<SnippetSummaryVO> list = snippetMapper.getSnippetList(snippetUserNo, query);
		return new SnippetListResponse(list, totalCount, query.page(), query.size());
	}

	// 스니펫 상세 정보를 조회합니다.
	public SnippetDetailVO getSnippetDetail(Long snippetUserNo, Long snippetNo) {
		// 사용자 소유 스니펫인지 확인하고 연결된 태그 번호까지 묶어 반환합니다.
		snippetAuthService.getRequiredSnippetUser(snippetUserNo);
		Long normalizedSnippetNo = normalizeRequiredId(snippetNo, "스니펫 번호를 확인해주세요.");
		SnippetDetailRowVO detailRow = snippetMapper.getSnippetDetail(snippetUserNo, normalizedSnippetNo);
		if (detailRow == null || detailRow.snippetNo() == null) {
			throw new IllegalArgumentException("스니펫을 찾을 수 없습니다.");
		}

		List<Long> tagNoList = snippetMapper.getSnippetTagNoList(snippetUserNo, normalizedSnippetNo);
		return new SnippetDetailVO(
			detailRow.snippetNo(),
			detailRow.folderNo(),
			detailRow.languageCd(),
			detailRow.title(),
			detailRow.summary(),
			detailRow.snippetBody(),
			detailRow.memo(),
			detailRow.favoriteYn(),
			detailRow.viewCnt(),
			detailRow.copyCnt(),
			detailRow.lastViewedDt(),
			detailRow.lastCopiedDt(),
			detailRow.duplicateYn(),
			detailRow.regDt(),
			detailRow.udtDt(),
			tagNoList == null ? List.of() : tagNoList
		);
	}

	@Transactional
	// 신규 스니펫을 등록합니다.
	public SnippetSaveResponse createSnippet(Long snippetUserNo, SnippetSavePO command) {
		// 저장 명령을 검증/정규화한 뒤 본문 해시와 태그 매핑을 함께 저장합니다.
		snippetAuthService.getRequiredSnippetUser(snippetUserNo);
		SnippetSavePO normalizedCommand = normalizeSnippetSaveCommand(command);
		String bodyHash = buildSnippetBodyHash(normalizedCommand.snippetBody());
		validateSnippetSaveCommand(snippetUserNo, normalizedCommand);

		GeneratedLongKey generatedKey = new GeneratedLongKey();
		snippetMapper.insertSnippetBase(snippetUserNo, normalizedCommand, bodyHash, snippetUserNo, generatedKey);
		Long createdSnippetNo = generatedKey.getValue();
		if (createdSnippetNo == null) {
			throw new IllegalStateException("스니펫 등록에 실패했습니다.");
		}

		replaceSnippetTags(snippetUserNo, createdSnippetNo, normalizedCommand.tagNoList());
		return new SnippetSaveResponse(createdSnippetNo, "스니펫이 등록되었습니다.");
	}

	@Transactional
	// 기존 스니펫을 수정합니다.
	public SnippetSaveResponse updateSnippet(Long snippetUserNo, Long snippetNo, SnippetSavePO command) {
		// 소유 스니펫 여부와 입력값을 검증한 뒤 본문 해시와 태그 매핑을 함께 갱신합니다.
		snippetAuthService.getRequiredSnippetUser(snippetUserNo);
		Long normalizedSnippetNo = normalizeRequiredId(snippetNo, "스니펫 번호를 확인해주세요.");
		ensureSnippetExists(snippetUserNo, normalizedSnippetNo);

		SnippetSavePO normalizedCommand = normalizeSnippetSaveCommand(command);
		String bodyHash = buildSnippetBodyHash(normalizedCommand.snippetBody());
		validateSnippetSaveCommand(snippetUserNo, normalizedCommand);

		int affectedRowCount = snippetMapper.updateSnippetBase(snippetUserNo, normalizedSnippetNo, normalizedCommand, bodyHash, snippetUserNo);
		if (affectedRowCount < 1) {
			throw new IllegalStateException("스니펫 수정에 실패했습니다.");
		}

		replaceSnippetTags(snippetUserNo, normalizedSnippetNo, normalizedCommand.tagNoList());
		return new SnippetSaveResponse(normalizedSnippetNo, "스니펫이 수정되었습니다.");
	}

	@Transactional
	// 스니펫을 삭제합니다.
	public void deleteSnippet(Long snippetUserNo, Long snippetNo) {
		// 소유 스니펫의 태그 매핑을 제거하고 소프트 삭제합니다.
		snippetAuthService.getRequiredSnippetUser(snippetUserNo);
		Long normalizedSnippetNo = normalizeRequiredId(snippetNo, "스니펫 번호를 확인해주세요.");
		snippetMapper.deleteSnippetTagMapBySnippetNo(snippetUserNo, normalizedSnippetNo);
		int affectedRowCount = snippetMapper.softDeleteSnippet(snippetUserNo, normalizedSnippetNo, snippetUserNo);
		if (affectedRowCount < 1) {
			throw new IllegalArgumentException("삭제할 스니펫을 찾을 수 없습니다.");
		}
	}

	@Transactional
	// 스니펫 즐겨찾기 여부를 갱신합니다.
	public void updateSnippetFavorite(Long snippetUserNo, Long snippetNo, String favoriteYn) {
		// Y/N 값으로 정규화한 뒤 대상 스니펫의 즐겨찾기 여부를 갱신합니다.
		snippetAuthService.getRequiredSnippetUser(snippetUserNo);
		Long normalizedSnippetNo = normalizeRequiredId(snippetNo, "스니펫 번호를 확인해주세요.");
		String normalizedFavoriteYn = normalizeYn(favoriteYn, "즐겨찾기 값을 확인해주세요.");
		int affectedRowCount = snippetMapper.updateSnippetFavorite(snippetUserNo, normalizedSnippetNo, normalizedFavoriteYn, snippetUserNo);
		if (affectedRowCount < 1) {
			throw new IllegalArgumentException("수정할 스니펫을 찾을 수 없습니다.");
		}
	}

	@Transactional
	// 스니펫 마지막 복사 일시와 복사 수를 갱신합니다.
	public void markSnippetCopied(Long snippetUserNo, Long snippetNo) {
		// 복사 이벤트 발생 시 마지막 복사 일시와 복사 수를 함께 갱신합니다.
		snippetAuthService.getRequiredSnippetUser(snippetUserNo);
		Long normalizedSnippetNo = normalizeRequiredId(snippetNo, "스니펫 번호를 확인해주세요.");
		int affectedRowCount = snippetMapper.updateSnippetLastCopiedDt(snippetUserNo, normalizedSnippetNo, snippetUserNo);
		if (affectedRowCount < 1) {
			throw new IllegalArgumentException("복사 처리할 스니펫을 찾을 수 없습니다.");
		}
	}

	@Transactional
	// 스니펫 마지막 조회 일시와 조회 수를 갱신합니다.
	public void markSnippetViewed(Long snippetUserNo, Long snippetNo) {
		// 상세 조회 이벤트 발생 시 마지막 조회 일시와 조회 수를 함께 갱신합니다.
		snippetAuthService.getRequiredSnippetUser(snippetUserNo);
		Long normalizedSnippetNo = normalizeRequiredId(snippetNo, "스니펫 번호를 확인해주세요.");
		int affectedRowCount = snippetMapper.updateSnippetLastViewedDt(snippetUserNo, normalizedSnippetNo, snippetUserNo);
		if (affectedRowCount < 1) {
			throw new IllegalArgumentException("조회 처리할 스니펫을 찾을 수 없습니다.");
		}
	}

	@Transactional
	// 사용자 폴더를 등록합니다.
	public SnippetFolderVO createFolder(Long snippetUserNo, SnippetFolderSavePO command) {
		// 폴더명을 검증하고 정렬순서를 보정한 뒤 신규 폴더를 등록합니다.
		snippetAuthService.getRequiredSnippetUser(snippetUserNo);
		SnippetFolderSavePO normalizedCommand = normalizeFolderCommand(command);
		validateFolderDuplicate(snippetUserNo, normalizedCommand.folderNm(), null);

		GeneratedLongKey generatedKey = new GeneratedLongKey();
		Integer resolvedSortSeq = resolveFolderSortSeq(snippetUserNo, normalizedCommand.sortSeq());
		snippetMapper.insertSnippetFolder(snippetUserNo, normalizedCommand, resolvedSortSeq, snippetUserNo, generatedKey);
		Long createdFolderNo = generatedKey.getValue();
		if (createdFolderNo == null) {
			throw new IllegalStateException("폴더 등록에 실패했습니다.");
		}
		return findRequiredFolder(snippetUserNo, createdFolderNo);
	}

	@Transactional
	// 사용자 폴더를 수정합니다.
	public SnippetFolderVO updateFolder(Long snippetUserNo, Long folderNo, SnippetFolderSavePO command) {
		// 폴더 존재 여부와 이름 중복을 검증한 뒤 폴더 기본 정보를 갱신합니다.
		snippetAuthService.getRequiredSnippetUser(snippetUserNo);
		Long normalizedFolderNo = normalizeRequiredId(folderNo, "폴더 번호를 확인해주세요.");
		ensureFolderExists(snippetUserNo, normalizedFolderNo);

		SnippetFolderSavePO normalizedCommand = normalizeFolderCommand(command);
		validateFolderDuplicate(snippetUserNo, normalizedCommand.folderNm(), normalizedFolderNo);

		Integer resolvedSortSeq = resolveFolderSortSeq(snippetUserNo, normalizedCommand.sortSeq());
		int affectedRowCount = snippetMapper.updateSnippetFolder(
			snippetUserNo,
			normalizedFolderNo,
			normalizedCommand,
			resolvedSortSeq,
			snippetUserNo
		);
		if (affectedRowCount < 1) {
			throw new IllegalStateException("폴더 수정에 실패했습니다.");
		}
		return findRequiredFolder(snippetUserNo, normalizedFolderNo);
	}

	@Transactional
	// 사용자 폴더를 삭제합니다.
	public void deleteFolder(Long snippetUserNo, Long folderNo) {
		// 폴더 연결을 먼저 해제한 뒤 폴더를 소프트 삭제합니다.
		snippetAuthService.getRequiredSnippetUser(snippetUserNo);
		Long normalizedFolderNo = normalizeRequiredId(folderNo, "폴더 번호를 확인해주세요.");
		ensureFolderExists(snippetUserNo, normalizedFolderNo);

		snippetMapper.clearSnippetFolder(snippetUserNo, normalizedFolderNo, snippetUserNo);
		int affectedRowCount = snippetMapper.softDeleteSnippetFolder(snippetUserNo, normalizedFolderNo, snippetUserNo);
		if (affectedRowCount < 1) {
			throw new IllegalStateException("폴더 삭제에 실패했습니다.");
		}
	}

	@Transactional
	// 사용자 태그를 등록합니다.
	public SnippetTagVO createTag(Long snippetUserNo, SnippetTagSavePO command) {
		// 태그명을 검증하고 정렬순서를 보정한 뒤 신규 태그를 등록합니다.
		snippetAuthService.getRequiredSnippetUser(snippetUserNo);
		SnippetTagSavePO normalizedCommand = normalizeTagCommand(command);
		validateTagDuplicate(snippetUserNo, normalizedCommand.tagNm(), null);

		GeneratedLongKey generatedKey = new GeneratedLongKey();
		Integer resolvedSortSeq = resolveTagSortSeq(snippetUserNo, normalizedCommand.sortSeq());
		snippetMapper.insertSnippetTag(snippetUserNo, normalizedCommand, resolvedSortSeq, snippetUserNo, generatedKey);
		Long createdTagNo = generatedKey.getValue();
		if (createdTagNo == null) {
			throw new IllegalStateException("태그 등록에 실패했습니다.");
		}
		return findRequiredTag(snippetUserNo, createdTagNo);
	}

	@Transactional
	// 사용자 태그를 수정합니다.
	public SnippetTagVO updateTag(Long snippetUserNo, Long tagNo, SnippetTagSavePO command) {
		// 태그 존재 여부와 이름 중복을 검증한 뒤 태그 기본 정보를 갱신합니다.
		snippetAuthService.getRequiredSnippetUser(snippetUserNo);
		Long normalizedTagNo = normalizeRequiredId(tagNo, "태그 번호를 확인해주세요.");
		ensureTagExists(snippetUserNo, normalizedTagNo);

		SnippetTagSavePO normalizedCommand = normalizeTagCommand(command);
		validateTagDuplicate(snippetUserNo, normalizedCommand.tagNm(), normalizedTagNo);

		Integer resolvedSortSeq = resolveTagSortSeq(snippetUserNo, normalizedCommand.sortSeq());
		int affectedRowCount = snippetMapper.updateSnippetTag(
			snippetUserNo,
			normalizedTagNo,
			normalizedCommand,
			resolvedSortSeq,
			snippetUserNo
		);
		if (affectedRowCount < 1) {
			throw new IllegalStateException("태그 수정에 실패했습니다.");
		}
		return findRequiredTag(snippetUserNo, normalizedTagNo);
	}

	@Transactional
	// 사용자 태그를 삭제합니다.
	public void deleteTag(Long snippetUserNo, Long tagNo) {
		// 태그 매핑을 먼저 제거한 뒤 태그를 소프트 삭제합니다.
		snippetAuthService.getRequiredSnippetUser(snippetUserNo);
		Long normalizedTagNo = normalizeRequiredId(tagNo, "태그 번호를 확인해주세요.");
		ensureTagExists(snippetUserNo, normalizedTagNo);

		snippetMapper.deleteSnippetTagMapByTagNo(snippetUserNo, normalizedTagNo);
		int affectedRowCount = snippetMapper.softDeleteSnippetTag(snippetUserNo, normalizedTagNo, snippetUserNo);
		if (affectedRowCount < 1) {
			throw new IllegalStateException("태그 삭제에 실패했습니다.");
		}
	}

	// 목록 조회용 조건을 정규화합니다.
	private SnippetListQueryPO buildSnippetListQuery(
		String q,
		Long folderNo,
		Long tagNo,
		String languageCd,
		String favoriteYn,
		String includeBodyYn,
		String sortBy,
		String quickFilter,
		Integer page,
		Integer size
	) {
		// 페이징 기본값과 검색/정렬 조건을 안전한 형태로 맞춥니다.
		int resolvedPage = page == null || page < 1 ? DEFAULT_PAGE : page;
		int resolvedSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_PAGE_SIZE);
		int resolvedOffset = (resolvedPage - 1) * resolvedSize;

		return new SnippetListQueryPO(
			trimToNull(q),
			normalizeOptionalId(folderNo),
			normalizeOptionalId(tagNo),
			trimToNull(languageCd),
			normalizeOptionalYn(favoriteYn),
			normalizeYn(includeBodyYn, "본문 검색 여부를 확인해주세요."),
			normalizeSortBy(sortBy),
			normalizeQuickFilter(quickFilter),
			resolvedPage,
			resolvedSize,
			resolvedOffset
		);
	}

	// 스니펫 저장 요청을 정규화합니다.
	private SnippetSavePO normalizeSnippetSaveCommand(SnippetSavePO command) {
		// 문자열 공백을 정리하고 태그 목록은 중복 제거 후 순서를 보존합니다.
		if (command == null) {
			throw new IllegalArgumentException("스니펫 저장 요청이 없습니다.");
		}
		return new SnippetSavePO(
			normalizeOptionalId(command.folderNo()),
			normalizeRequiredText(command.languageCd(), "언어 코드를 선택해주세요."),
			normalizeRequiredText(command.title(), "제목을 입력해주세요."),
			trimToNull(command.summary()),
			normalizeRequiredSnippetBody(command.snippetBody()),
			trimToNull(command.memo()),
			normalizeYn(command.favoriteYn(), "즐겨찾기 값을 확인해주세요."),
			normalizeTagNoList(command.tagNoList())
		);
	}

	// 폴더 저장 요청을 정규화합니다.
	private SnippetFolderSavePO normalizeFolderCommand(SnippetFolderSavePO command) {
		// 폴더명과 색상값의 공백을 제거해 저장 기준을 일관되게 맞춥니다.
		if (command == null) {
			throw new IllegalArgumentException("폴더 저장 요청이 없습니다.");
		}
		return new SnippetFolderSavePO(
			normalizeRequiredText(command.folderNm(), "폴더명을 입력해주세요."),
			trimToNull(command.colorHex()),
			command.sortSeq()
		);
	}

	// 태그 저장 요청을 정규화합니다.
	private SnippetTagSavePO normalizeTagCommand(SnippetTagSavePO command) {
		// 태그명과 색상값의 공백을 제거해 저장 기준을 일관되게 맞춥니다.
		if (command == null) {
			throw new IllegalArgumentException("태그 저장 요청이 없습니다.");
		}
		return new SnippetTagSavePO(
			normalizeRequiredText(command.tagNm(), "태그명을 입력해주세요."),
			trimToNull(command.colorHex()),
			command.sortSeq()
		);
	}

	// 스니펫 저장 요청의 참조 무결성을 검증합니다.
	private void validateSnippetSaveCommand(Long snippetUserNo, SnippetSavePO command) {
		// 언어, 폴더, 태그가 모두 현재 사용자 기준으로 유효한지 확인합니다.
		ensureLanguageExists(command.languageCd());
		if (command.folderNo() != null) {
			ensureFolderExists(snippetUserNo, command.folderNo());
		}
		ensureOwnedTagList(snippetUserNo, command.tagNoList());
	}

	// 언어 코드가 사용 가능한 값인지 확인합니다.
	private void ensureLanguageExists(String languageCd) {
		if (snippetMapper.countLanguageByLanguageCd(languageCd) < 1) {
			throw new IllegalArgumentException("사용 가능한 언어를 선택해주세요.");
		}
	}

	// 폴더가 현재 사용자 소유인지 확인합니다.
	private void ensureFolderExists(Long snippetUserNo, Long folderNo) {
		if (snippetMapper.countFolderByUserNo(snippetUserNo, folderNo) < 1) {
			throw new IllegalArgumentException("폴더를 찾을 수 없습니다.");
		}
	}

	// 태그가 현재 사용자 소유인지 확인합니다.
	private void ensureTagExists(Long snippetUserNo, Long tagNo) {
		if (snippetMapper.countTagByUserNo(snippetUserNo, tagNo) < 1) {
			throw new IllegalArgumentException("태그를 찾을 수 없습니다.");
		}
	}

	// 스니펫이 현재 사용자 소유인지 확인합니다.
	private void ensureSnippetExists(Long snippetUserNo, Long snippetNo) {
		if (snippetMapper.getSnippetDetail(snippetUserNo, snippetNo) == null) {
			throw new IllegalArgumentException("스니펫을 찾을 수 없습니다.");
		}
	}

	// 태그 목록이 모두 현재 사용자 소유인지 확인합니다.
	private void ensureOwnedTagList(Long snippetUserNo, List<Long> tagNoList) {
		// 비어 있지 않은 태그 목록만 건수 기준으로 소유권을 검증합니다.
		if (tagNoList == null || tagNoList.isEmpty()) {
			return;
		}
		int ownedTagCount = snippetMapper.countOwnedTagList(snippetUserNo, tagNoList);
		if (ownedTagCount != tagNoList.size()) {
			throw new IllegalArgumentException("선택한 태그 중 사용할 수 없는 항목이 있습니다.");
		}
	}

	// 스니펫 태그 매핑을 전체 교체합니다.
	private void replaceSnippetTags(Long snippetUserNo, Long snippetNo, List<Long> tagNoList) {
		// 기존 매핑을 모두 제거한 뒤 현재 태그 목록만 다시 등록합니다.
		snippetMapper.deleteSnippetTagMapBySnippetNo(snippetUserNo, snippetNo);
		if (tagNoList == null || tagNoList.isEmpty()) {
			return;
		}
		snippetMapper.insertSnippetTagMapList(snippetNo, tagNoList, snippetUserNo);
	}

	// 폴더명 중복을 확인합니다.
	private void validateFolderDuplicate(Long snippetUserNo, String folderNm, Long excludeFolderNo) {
		if (snippetMapper.getFolderDuplicateCount(snippetUserNo, folderNm, excludeFolderNo) > 0) {
			throw new IllegalArgumentException("같은 이름의 폴더가 이미 존재합니다.");
		}
	}

	// 태그명 중복을 확인합니다.
	private void validateTagDuplicate(Long snippetUserNo, String tagNm, Long excludeTagNo) {
		if (snippetMapper.getTagDuplicateCount(snippetUserNo, tagNm, excludeTagNo) > 0) {
			throw new IllegalArgumentException("같은 이름의 태그가 이미 존재합니다.");
		}
	}

	// 폴더 정렬순서를 보정합니다.
	private Integer resolveFolderSortSeq(Long snippetUserNo, Integer requestedSortSeq) {
		if (requestedSortSeq != null) {
			return requestedSortSeq;
		}
		Integer maxSortSeq = snippetMapper.getMaxFolderSortSeq(snippetUserNo);
		return maxSortSeq == null ? 1 : maxSortSeq + 1;
	}

	// 태그 정렬순서를 보정합니다.
	private Integer resolveTagSortSeq(Long snippetUserNo, Integer requestedSortSeq) {
		if (requestedSortSeq != null) {
			return requestedSortSeq;
		}
		Integer maxSortSeq = snippetMapper.getMaxTagSortSeq(snippetUserNo);
		return maxSortSeq == null ? 1 : maxSortSeq + 1;
	}

	// 등록 또는 수정 후 폴더 목록에서 대상 폴더를 찾아 반환합니다.
	private SnippetFolderVO findRequiredFolder(Long snippetUserNo, Long folderNo) {
		return snippetMapper.getSnippetFolderList(snippetUserNo).stream()
			.filter(folder -> Objects.equals(folder.folderNo(), folderNo))
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("폴더 조회에 실패했습니다."));
	}

	// 등록 또는 수정 후 태그 목록에서 대상 태그를 찾아 반환합니다.
	private SnippetTagVO findRequiredTag(Long snippetUserNo, Long tagNo) {
		return snippetMapper.getSnippetTagList(snippetUserNo).stream()
			.filter(tag -> Objects.equals(tag.tagNo(), tagNo))
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("태그 조회에 실패했습니다."));
	}

	// 태그 번호 목록을 중복 제거하고 양수 값만 유지합니다.
	private List<Long> normalizeTagNoList(List<Long> tagNoList) {
		// 입력 태그가 없으면 빈 목록으로 통일하고, 있으면 순서를 보존한 채 중복을 제거합니다.
		if (tagNoList == null || tagNoList.isEmpty()) {
			return List.of();
		}
		LinkedHashSet<Long> normalizedTagNoSet = new LinkedHashSet<>();
		for (Long tagNo : tagNoList) {
			Long normalizedTagNo = normalizeOptionalId(tagNo);
			if (normalizedTagNo != null) {
				normalizedTagNoSet.add(normalizedTagNo);
			}
		}
		return new ArrayList<>(normalizedTagNoSet);
	}

	// 스니펫 본문을 저장용 기준으로 정규화합니다.
	private String normalizeRequiredSnippetBody(String value) {
		// 줄바꿈은 LF로 맞추되, 앞뒤 공백 라인은 유지한 채 비어 있는 본문만 차단합니다.
		if (value == null) {
			throw new IllegalArgumentException("스니펫 본문을 입력해주세요.");
		}
		String normalizedValue = value.replace("\r\n", "\n").replace('\r', '\n');
		if (normalizedValue.trim().isEmpty()) {
			throw new IllegalArgumentException("스니펫 본문을 입력해주세요.");
		}
		return normalizedValue;
	}

	// 본문 정규화 해시를 계산합니다.
	private String buildSnippetBodyHash(String snippetBody) {
		// 중복 판정 기준에 맞게 본문을 정규화한 뒤 SHA-256 해시로 변환합니다.
		String normalizedBody = normalizeBodyForHash(snippetBody);
		return toSha256Hex(normalizedBody);
	}

	// 해시 계산용 본문을 정규화합니다.
	private String normalizeBodyForHash(String snippetBody) {
		// CRLF를 LF로 맞추고, 각 줄 오른쪽 공백과 앞뒤 빈 줄을 제거합니다.
		String normalizedLineBreakBody = snippetBody.replace("\r\n", "\n").replace('\r', '\n');
		String[] lines = normalizedLineBreakBody.split("\n", -1);
		List<String> normalizedLineList = new ArrayList<>(lines.length);
		for (String line : lines) {
			normalizedLineList.add(trimRightWhitespace(line));
		}

		int startIndex = 0;
		while (startIndex < normalizedLineList.size() && isBlankLine(normalizedLineList.get(startIndex))) {
			startIndex++;
		}

		int endIndex = normalizedLineList.size() - 1;
		while (endIndex >= startIndex && isBlankLine(normalizedLineList.get(endIndex))) {
			endIndex--;
		}

		if (endIndex < startIndex) {
			return "";
		}
		return String.join("\n", normalizedLineList.subList(startIndex, endIndex + 1));
	}

	// 각 줄의 오른쪽 공백을 제거합니다.
	private String trimRightWhitespace(String value) {
		int endIndex = value.length();
		while (endIndex > 0) {
			char currentChar = value.charAt(endIndex - 1);
			if (currentChar != ' ' && currentChar != '\t') {
				break;
			}
			endIndex--;
		}
		return value.substring(0, endIndex);
	}

	// 빈 줄 여부를 확인합니다.
	private boolean isBlankLine(String value) {
		return value.trim().isEmpty();
	}

	// 문자열을 SHA-256 16진수로 변환합니다.
	private String toSha256Hex(String value) {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
			byte[] digest = messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));
			StringBuilder hexBuilder = new StringBuilder(digest.length * 2);
			for (byte currentByte : digest) {
				hexBuilder.append(String.format("%02x", currentByte));
			}
			return hexBuilder.toString();
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("본문 해시 계산에 실패했습니다.", exception);
		}
	}

	// 필수 식별자를 양수 Long 값으로 정규화합니다.
	private Long normalizeRequiredId(Long value, String errorMessage) {
		Long normalizedValue = normalizeOptionalId(value);
		if (normalizedValue == null) {
			throw new IllegalArgumentException(errorMessage);
		}
		return normalizedValue;
	}

	// 선택 식별자를 양수 Long 값으로 정규화합니다.
	private Long normalizeOptionalId(Long value) {
		if (value == null || value < 1L) {
			return null;
		}
		return value;
	}

	// 필수 문자열을 trim 처리하고 비어 있으면 예외를 반환합니다.
	private String normalizeRequiredText(String value, String errorMessage) {
		String normalizedValue = trimToNull(value);
		if (normalizedValue == null) {
			throw new IllegalArgumentException(errorMessage);
		}
		return normalizedValue;
	}

	// 선택 Y/N 문자열을 정규화합니다.
	private String normalizeOptionalYn(String value) {
		String normalizedValue = trimToNull(value);
		if (normalizedValue == null) {
			return null;
		}
		return normalizeYn(normalizedValue, "Y/N 값을 확인해주세요.");
	}

	// 퀵필터 값을 정규화합니다.
	private String normalizeQuickFilter(String value) {
		String normalizedValue = trimToNull(value);
		if (normalizedValue == null) {
			return QUICK_FILTER_ALL;
		}

		String lowerCaseValue = normalizedValue.toLowerCase(Locale.ROOT);
		if (
			QUICK_FILTER_ALL.equals(lowerCaseValue) ||
			QUICK_FILTER_FAVORITE.equals(lowerCaseValue) ||
			QUICK_FILTER_RECENT_VIEWED.equals(lowerCaseValue) ||
			QUICK_FILTER_RECENT_COPIED.equals(lowerCaseValue) ||
			QUICK_FILTER_DUPLICATE.equals(lowerCaseValue)
		) {
			return lowerCaseValue;
		}
		throw new IllegalArgumentException("퀵필터 값을 확인해주세요.");
	}

	// 정렬 기준 값을 정규화합니다.
	private String normalizeSortBy(String value) {
		String normalizedValue = trimToNull(value);
		if (normalizedValue == null) {
			return SORT_BY_UPDATED_DESC;
		}

		String lowerCaseValue = normalizedValue.toLowerCase(Locale.ROOT);
		if (
			SORT_BY_UPDATED_DESC.equals(lowerCaseValue) ||
			SORT_BY_VIEWED_DESC.equals(lowerCaseValue) ||
			SORT_BY_COPIED_DESC.equals(lowerCaseValue) ||
			SORT_BY_COPY_COUNT_DESC.equals(lowerCaseValue) ||
			SORT_BY_TITLE_ASC.equals(lowerCaseValue)
		) {
			return lowerCaseValue;
		}
		throw new IllegalArgumentException("정렬 기준 값을 확인해주세요.");
	}

	// Y/N 값을 대문자 기준으로 정규화합니다.
	private String normalizeYn(String value, String errorMessage) {
		String normalizedValue = trimToNull(value);
		if (normalizedValue == null) {
			return "N";
		}
		String upperCaseValue = normalizedValue.toUpperCase(Locale.ROOT);
		if ("Y".equals(upperCaseValue) || "N".equals(upperCaseValue)) {
			return upperCaseValue;
		}
		throw new IllegalArgumentException(errorMessage);
	}

	// 문자열을 trim 처리하고 비어 있으면 null로 변환합니다.
	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmedValue = value.trim();
		return trimmedValue.isEmpty() ? null : trimmedValue;
	}
}
