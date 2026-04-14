package com.xodud1202.springbackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkImportCompanyInfoVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkDetailResponseVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkDetailUpdatePO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkDetailVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkFileVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkImportFileSavePO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkImportJobSavePO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkImportPO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkImportResponseVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkManualCreatePO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkManualCreateResponseVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkManualSavePO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkReplyFileDownloadVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkReplyFileSavePO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkReplyFileVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkReplyDeletePO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkReplySavePO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkReplyUpdatePO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkReplyVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkUpdatePO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkCompanyVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkCompletedListResponseVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkListRowVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkProjectVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkSearchPO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkStatusListResponseVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkStatusSectionPageResponseVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkStatusSectionVO;
import com.xodud1202.springbackend.domain.common.CommonCodeVO;
import com.xodud1202.springbackend.domain.common.FtpProperties;
import com.xodud1202.springbackend.domain.work.WorkFileDeletePO;
import com.xodud1202.springbackend.entity.UserBaseEntity;
import com.xodud1202.springbackend.mapper.CommonMapper;
import com.xodud1202.springbackend.mapper.CompanyWorkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
// 관리자 회사 업무 조회 비즈니스 로직을 처리합니다.
public class CompanyWorkService {
	private static final int ADMIN_COMPANY_WORK_DEFAULT_PAGE = 1;
	private static final int ADMIN_COMPANY_WORK_DEFAULT_PAGE_SIZE = 10;
	private static final int ADMIN_COMPANY_WORK_MAX_PAGE_SIZE = 200;
	private static final int ADMIN_COMPANY_WORK_MAX_WORK_KEY_LENGTH = 50;
	private static final int ADMIN_COMPANY_WORK_MAX_TITLE_LENGTH = 255;
	private static final int ADMIN_COMPANY_WORK_MAX_MANAGER_NAME_LENGTH = 50;
	private static final int ADMIN_COMPANY_WORK_MAX_FILE_NAME_LENGTH = 255;
	private static final int ADMIN_COMPANY_WORK_MAX_FILE_URL_LENGTH = 255;
	private static final int ADMIN_COMPANY_WORK_MAX_REPLY_LENGTH = 65535;
	private static final String WORK_STATUS_GROUP_CODE = "WORK_STAT";
	private static final String WORK_PRIORITY_GROUP_CODE = "WORK_PRIOR";
	private static final String WORK_COMPLETED_STATUS_CODE = "WORK_STAT_05";
	private static final String WORK_WAIT_STATUS_CODE = "WORK_STAT_01";
	private static final String WORK_PRIOR_HIGH_CODE = "WORK_PRIOR_01";
	private static final String WORK_PRIOR_NORMAL_CODE = "WORK_PRIOR_02";
	private static final String WORK_PRIOR_LOW_CODE = "WORK_PRIOR_03";
	private static final String DINING_BRANDS_GROUP_COMPANY_NAME = "다이닝 브랜즈 그룹";
	private static final String JIRA_PLATFORM_NAME = "JIRA";
	private static final DateTimeFormatter ADMIN_COMPANY_WORK_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final DateTimeFormatter ADMIN_COMPANY_WORK_DATE_TIME_MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	private static final DateTimeFormatter ADMIN_COMPANY_WORK_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
	private static final DateTimeFormatter ADMIN_COMPANY_WORK_MANUAL_KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
	private static final DateTimeFormatter JIRA_OFFSET_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	private static final DateTimeFormatter JIRA_OFFSET_DATE_TIME_WITHOUT_MILLIS_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

	private final CompanyWorkMapper companyWorkMapper;
	private final CommonMapper commonMapper;
	private final DiningBrandsGroupJiraApiClient diningBrandsGroupJiraApiClient;
	private final FtpFileService ftpFileService;
	private final FtpProperties ftpProperties;

	// 관리자 회사 업무 회사 목록을 조회합니다.
	public List<AdminCompanyWorkCompanyVO> getAdminCompanyWorkCompanyList() {
		// 조회 결과가 없으면 빈 목록을 반환합니다.
		List<AdminCompanyWorkCompanyVO> companyList = companyWorkMapper.getAdminCompanyWorkCompanyList();
		return companyList == null ? List.of() : companyList;
	}

	// 관리자 회사 업무 프로젝트 목록을 조회합니다.
	public List<AdminCompanyWorkProjectVO> getAdminCompanyWorkProjectList(Integer workCompanySeq) {
		// 회사 번호를 검증한 뒤 해당 회사의 프로젝트 목록을 조회합니다.
		int resolvedWorkCompanySeq = normalizeRequiredSequence(workCompanySeq, "회사 정보를 확인해주세요.");
		List<AdminCompanyWorkProjectVO> projectList = companyWorkMapper.getAdminCompanyWorkProjectList(resolvedWorkCompanySeq);
		return projectList == null ? List.of() : projectList;
	}

	// 업무관리 화면에서 사용할 업무 상태 공통코드 목록을 조회합니다.
	public List<CommonCodeVO> getWorkStatusCodeList() {
		// 공통코드가 비어 있더라도 프런트는 빈 목록으로 처리할 수 있게 반환합니다.
		List<CommonCodeVO> workStatusCodeList = commonMapper.getCommonCodeList(WORK_STATUS_GROUP_CODE);
		return workStatusCodeList == null ? List.of() : workStatusCodeList;
	}

	// 업무관리 화면에서 사용할 업무 우선순위 공통코드 목록을 조회합니다.
	public List<CommonCodeVO> getWorkPriorityCodeList() {
		// 공통코드가 비어 있더라도 프런트는 빈 목록으로 처리할 수 있게 반환합니다.
		List<CommonCodeVO> workPriorityCodeList = commonMapper.getCommonCodeList(WORK_PRIORITY_GROUP_CODE);
		return workPriorityCodeList == null ? List.of() : workPriorityCodeList;
	}

	// 관리자 회사 업무 비완료 상태 목록을 조회합니다.
	public AdminCompanyWorkStatusListResponseVO getAdminCompanyWorkStatusList(
		Integer workCompanySeq,
		Integer workCompanyProjectSeq,
		String title
	) {
		// 공통 조회 조건을 구성한 뒤 비완료 업무 목록을 조회합니다.
		AdminCompanyWorkSearchPO param = createSearchParam(workCompanySeq, workCompanyProjectSeq, title, null, null);
		List<AdminCompanyWorkListRowVO> rowList = companyWorkMapper.getAdminCompanyWorkStatusList(param);
		List<CommonCodeVO> workStatusCodeList = commonMapper.getCommonCodeList(WORK_STATUS_GROUP_CODE);

		// 상태 코드별 행 목록을 맵으로 정리합니다.
		Map<String, List<AdminCompanyWorkListRowVO>> rowListMapByStatusCode = new LinkedHashMap<>();
		for (AdminCompanyWorkListRowVO rowItem : rowList == null ? List.<AdminCompanyWorkListRowVO>of() : rowList) {
			String workStatusCode = trimToNull(rowItem == null ? null : rowItem.getWorkStatCd());
			if (workStatusCode == null) {
				continue;
			}
			rowListMapByStatusCode.computeIfAbsent(workStatusCode, ignoredKey -> new ArrayList<>()).add(rowItem);
		}

		// 공통코드 정렬 순서대로 상태별 섹션 응답을 구성합니다.
		List<AdminCompanyWorkStatusSectionVO> statusSectionList = new ArrayList<>();
		for (CommonCodeVO workStatusCodeItem : workStatusCodeList == null ? List.<CommonCodeVO>of() : workStatusCodeList) {
			String workStatusCode = trimToNull(workStatusCodeItem == null ? null : workStatusCodeItem.getCd());
			if (workStatusCode == null || WORK_COMPLETED_STATUS_CODE.equals(workStatusCode)) {
				continue;
			}

			// 상태 코드별 목록이 없더라도 빈 섹션으로 유지해 프런트가 비노출 여부를 판단하게 합니다.
			AdminCompanyWorkStatusSectionVO statusSection = new AdminCompanyWorkStatusSectionVO();
			statusSection.setWorkStatCd(workStatusCode);
			List<AdminCompanyWorkListRowVO> statusRowList = rowListMapByStatusCode.getOrDefault(workStatusCode, List.of());
			statusSection.setList(statusRowList);
			statusSection.setTotalCount(statusRowList.size());
			statusSection.setHasMore(false);
			statusSectionList.add(statusSection);
		}

		// 상태별 목록 응답 객체를 반환합니다.
		AdminCompanyWorkStatusListResponseVO response = new AdminCompanyWorkStatusListResponseVO();
		response.setStatusSectionList(statusSectionList);
		return response;
	}

	// 업무관리 화면 상태 포함 전체 목록을 조회합니다.
	public AdminCompanyWorkStatusListResponseVO getWorkCompanyStatusList(
		Integer workCompanySeq,
		Integer workCompanyProjectSeq,
		String title,
		String includeBodyYn,
		List<String> workStatCdList,
		Integer sectionSize
	) {
		// 공통 조회 조건을 구성한 뒤 상태 포함 전체 목록을 조회합니다.
		AdminCompanyWorkSearchPO param = createSearchParam(
			workCompanySeq,
			workCompanyProjectSeq,
			title,
			null,
			null,
			includeBodyYn,
			workStatCdList
		);
		List<AdminCompanyWorkListRowVO> rowList = companyWorkMapper.getWorkCompanyList(param);
		List<CommonCodeVO> workStatusCodeList = getWorkStatusCodeList();
		int resolvedSectionSize = normalizePageSize(sectionSize);

		// 상태 코드별 행 목록을 맵으로 정리합니다.
		Map<String, List<AdminCompanyWorkListRowVO>> rowListMapByStatusCode = new LinkedHashMap<>();
		for (AdminCompanyWorkListRowVO rowItem : rowList == null ? List.<AdminCompanyWorkListRowVO>of() : rowList) {
			String workStatusCode = trimToNull(rowItem == null ? null : rowItem.getWorkStatCd());
			if (workStatusCode == null) {
				continue;
			}
			rowListMapByStatusCode.computeIfAbsent(workStatusCode, ignoredKey -> new ArrayList<>()).add(rowItem);
		}

		// 공통코드 정렬 순서대로 상태별 섹션 응답을 구성합니다.
		List<AdminCompanyWorkStatusSectionVO> statusSectionList = new ArrayList<>();
		for (CommonCodeVO workStatusCodeItem : workStatusCodeList) {
			String workStatusCode = trimToNull(workStatusCodeItem == null ? null : workStatusCodeItem.getCd());
			if (workStatusCode == null) {
				continue;
			}

			AdminCompanyWorkStatusSectionVO statusSection = new AdminCompanyWorkStatusSectionVO();
			statusSection.setWorkStatCd(workStatusCode);
			List<AdminCompanyWorkListRowVO> fullRowList = rowListMapByStatusCode.getOrDefault(workStatusCode, List.of());
			List<AdminCompanyWorkListRowVO> visibleRowList = sliceStatusSectionRowList(fullRowList, resolvedSectionSize);
			statusSection.setList(visibleRowList);
			statusSection.setTotalCount(fullRowList.size());
			statusSection.setHasMore(fullRowList.size() > visibleRowList.size());
			statusSectionList.add(statusSection);
		}

		// 필터 결과에만 존재하는 상태 코드도 누락 없이 마지막에 추가합니다.
		for (Map.Entry<String, List<AdminCompanyWorkListRowVO>> entry : rowListMapByStatusCode.entrySet()) {
			boolean alreadyAdded = statusSectionList.stream()
				.anyMatch(statusSectionItem -> Objects.equals(statusSectionItem.getWorkStatCd(), entry.getKey()));
			if (alreadyAdded) {
				continue;
			}

			AdminCompanyWorkStatusSectionVO statusSection = new AdminCompanyWorkStatusSectionVO();
			statusSection.setWorkStatCd(entry.getKey());
			List<AdminCompanyWorkListRowVO> visibleRowList = sliceStatusSectionRowList(entry.getValue(), resolvedSectionSize);
			statusSection.setList(visibleRowList);
			statusSection.setTotalCount(entry.getValue().size());
			statusSection.setHasMore(entry.getValue().size() > visibleRowList.size());
			statusSectionList.add(statusSection);
		}

		// 상태별 목록 응답 객체를 반환합니다.
		AdminCompanyWorkStatusListResponseVO response = new AdminCompanyWorkStatusListResponseVO();
		response.setStatusSectionList(statusSectionList);
		return response;
	}

	// 업무관리 화면에서 특정 상태의 다음 업무 목록을 조회합니다.
	public AdminCompanyWorkStatusSectionPageResponseVO getWorkCompanyStatusSectionPage(
		Integer workCompanySeq,
		Integer workCompanyProjectSeq,
		String title,
		String includeBodyYn,
		String workStatCd,
		Integer offset,
		Integer limit
	) {
		// 추가 조회 대상 상태 코드와 페이징 조건을 먼저 검증합니다.
		String normalizedWorkStatusCode = trimToNull(workStatCd);
		if (normalizedWorkStatusCode == null) {
			throw new IllegalArgumentException("업무 상태를 확인해주세요.");
		}
		validateWorkStatusCode(normalizedWorkStatusCode);

		int resolvedLimit = normalizePageSize(limit);
		int resolvedOffset = normalizeNonNegativeOffset(offset);
		AdminCompanyWorkSearchPO param = createSearchParam(
			workCompanySeq,
			workCompanyProjectSeq,
			title,
			1,
			resolvedLimit,
			includeBodyYn,
			List.of(normalizedWorkStatusCode)
		);
		param.setOffset(resolvedOffset);
		param.setPageSize(resolvedLimit);

		// 특정 상태의 목록과 전체 건수를 함께 조회합니다.
		List<AdminCompanyWorkListRowVO> rowList = companyWorkMapper.getWorkCompanySectionList(param);
		int totalCount = companyWorkMapper.getWorkCompanySectionCount(param);

		// 상태별 추가 조회 응답 구조로 변환합니다.
		AdminCompanyWorkStatusSectionPageResponseVO response = new AdminCompanyWorkStatusSectionPageResponseVO();
		response.setWorkStatCd(normalizedWorkStatusCode);
		response.setList(rowList == null ? List.of() : rowList);
		response.setTotalCount(totalCount);
		response.setOffset(resolvedOffset);
		response.setLimit(resolvedLimit);
		response.setHasMore(totalCount > resolvedOffset + (rowList == null ? 0 : rowList.size()));
		return response;
	}

	// 관리자 회사 업무 완료 목록을 조회합니다.
	public AdminCompanyWorkCompletedListResponseVO getAdminCompanyWorkCompletedList(
		Integer workCompanySeq,
		Integer workCompanyProjectSeq,
		String title,
		Integer page,
		Integer pageSize
	) {
		// 공통 조회 조건과 페이징 조건을 함께 구성합니다.
		AdminCompanyWorkSearchPO param = createSearchParam(workCompanySeq, workCompanyProjectSeq, title, page, pageSize);
		List<AdminCompanyWorkListRowVO> rowList = companyWorkMapper.getAdminCompanyWorkCompletedList(param);
		int totalCount = companyWorkMapper.getAdminCompanyWorkCompletedCount(param);

		// 완료 목록 응답 객체를 반환합니다.
		AdminCompanyWorkCompletedListResponseVO response = new AdminCompanyWorkCompletedListResponseVO();
		response.setList(rowList == null ? List.of() : rowList);
		response.setTotalCount(totalCount);
		response.setPage(param.getPage());
		response.setPageSize(param.getPageSize());
		return response;
	}

	// 관리자 회사 업무 상세 팝업 정보를 조회합니다.
	public AdminCompanyWorkDetailResponseVO getAdminCompanyWorkDetail(Long workSeq) {
		// 선택 업무 번호를 검증한 뒤 상세/첨부/댓글 정보를 함께 조회합니다.
		long resolvedWorkSeq = normalizeRequiredWorkSequence(workSeq, "업무 정보를 확인해주세요.");
		AdminCompanyWorkDetailVO detail = getRequiredAdminCompanyWorkDetail(resolvedWorkSeq);
		List<AdminCompanyWorkFileVO> fileList = companyWorkMapper.getAdminCompanyWorkFileList(resolvedWorkSeq);
		List<AdminCompanyWorkReplyVO> replyList = companyWorkMapper.getAdminCompanyWorkReplyList(resolvedWorkSeq);
		List<AdminCompanyWorkReplyFileVO> replyFileList = companyWorkMapper.getAdminCompanyWorkReplyFileList(resolvedWorkSeq);

		// 상세 팝업 응답 구조로 묶어서 반환합니다.
		AdminCompanyWorkDetailResponseVO response = new AdminCompanyWorkDetailResponseVO();
		response.setDetail(detail);
		response.setFileList(fileList == null ? List.of() : fileList);
		response.setReplyList(applyReplyFileList(replyList, replyFileList));
		return response;
	}

	// 업무관리 화면 전용 상세 정보를 시분초 포함 형식으로 조회합니다.
	public AdminCompanyWorkDetailResponseVO getWorkCompanyWorkDetail(Long workSeq) {
		// 선택 업무 번호를 검증한 뒤 상세/첨부/댓글 정보를 함께 조회합니다.
		long resolvedWorkSeq = normalizeRequiredWorkSequence(workSeq, "업무 정보를 확인해주세요.");
		AdminCompanyWorkDetailVO detail = getRequiredWorkCompanyWorkDetail(resolvedWorkSeq);
		List<AdminCompanyWorkFileVO> fileList = companyWorkMapper.getAdminCompanyWorkFileList(resolvedWorkSeq);
		List<AdminCompanyWorkReplyVO> replyList = companyWorkMapper.getAdminCompanyWorkReplyList(resolvedWorkSeq);
		List<AdminCompanyWorkReplyFileVO> replyFileList = companyWorkMapper.getAdminCompanyWorkReplyFileList(resolvedWorkSeq);

		// 상세 응답 구조로 묶어서 반환합니다.
		AdminCompanyWorkDetailResponseVO response = new AdminCompanyWorkDetailResponseVO();
		response.setDetail(detail);
		response.setFileList(fileList == null ? List.of() : fileList);
		response.setReplyList(applyReplyFileList(replyList, replyFileList));
		return response;
	}

	// 관리자 회사 업무 댓글 전용 목록을 조회합니다.
	public List<AdminCompanyWorkReplyVO> getAdminCompanyWorkReplyList(Long workSeq) {
		// 선택 업무 번호를 검증하고 대상 업무 존재 여부를 먼저 확인합니다.
		long resolvedWorkSeq = normalizeRequiredWorkSequence(workSeq, "업무 정보를 확인해주세요.");
		getRequiredAdminCompanyWorkDetail(resolvedWorkSeq);

		// 댓글 목록과 댓글 첨부파일 목록을 함께 조회해 댓글별 첨부를 매핑합니다.
		List<AdminCompanyWorkReplyVO> replyList = companyWorkMapper.getAdminCompanyWorkReplyList(resolvedWorkSeq);
		List<AdminCompanyWorkReplyFileVO> replyFileList = companyWorkMapper.getAdminCompanyWorkReplyFileList(resolvedWorkSeq);
		return applyReplyFileList(replyList, replyFileList);
	}

	@Transactional
	// 관리자 회사 업무 그리드 즉시 수정 항목을 저장합니다.
	public AdminCompanyWorkListRowVO updateAdminCompanyWork(AdminCompanyWorkUpdatePO param) {
		// 요청값을 정규화하고 필수값을 검증합니다.
		AdminCompanyWorkUpdatePO normalizedParam = normalizeUpdateParam(param);
		validateWorkStatusCode(normalizedParam.getWorkStatCd());

		// 실제 업무 수정이 반영되지 않으면 요청 오류로 처리합니다.
		int updatedCount = companyWorkMapper.updateAdminCompanyWorkEditableFields(normalizedParam);
		if (updatedCount < 1) {
			throw new IllegalArgumentException("업무 정보를 확인해주세요.");
		}

		// 최신 행 정보를 다시 조회해 프런트 즉시 반영용으로 반환합니다.
		AdminCompanyWorkListRowVO updatedRow = companyWorkMapper.getAdminCompanyWorkRow(normalizedParam.getWorkSeq());
		if (updatedRow == null) {
			throw new IllegalStateException("수정된 업무 정보를 확인할 수 없습니다.");
		}
		return updatedRow;
	}

	@Transactional
	// 관리자 회사 업무 상세 수정 항목을 저장합니다.
	public AdminCompanyWorkDetailVO updateAdminCompanyWorkDetail(AdminCompanyWorkDetailUpdatePO param) {
		// 요청값을 정규화하고 필수값을 검증합니다.
		AdminCompanyWorkDetailUpdatePO normalizedParam = normalizeDetailUpdateParam(param);
		validateWorkStatusCode(normalizedParam.getWorkStatCd());

		// 실제 업무 수정이 반영되지 않으면 요청 오류로 처리합니다.
		int updatedCount = companyWorkMapper.updateAdminCompanyWorkDetailFields(normalizedParam);
		if (updatedCount < 1) {
			throw new IllegalArgumentException("업무 정보를 확인해주세요.");
		}

		// 저장 후 최신 상세 정보를 반환합니다.
		return getRequiredAdminCompanyWorkDetail(normalizedParam.getWorkSeq());
	}

	@Transactional
	// 회사 업무 상세 수정과 업무 첨부파일 변경을 함께 저장합니다.
	public AdminCompanyWorkDetailResponseVO updateAdminCompanyWorkDetail(
		AdminCompanyWorkDetailUpdatePO param,
		List<MultipartFile> files,
		Long currentUserNo
	) {
		// 상세 수정 요청값을 정규화한 뒤 본문/첨부 변경을 순서대로 반영합니다.
		AdminCompanyWorkDetailVO updatedDetail = updateAdminCompanyWorkDetail(param);
		AdminCompanyWorkDetailUpdatePO normalizedParam = normalizeDetailUpdateParam(param);

		// 삭제 요청 첨부파일을 먼저 반영해 동일 요청의 신규 첨부가 이어서 저장되도록 처리합니다.
		for (Integer workJobFileSeq : normalizedParam.getDeleteWorkJobFileSeqList() == null ? List.<Integer>of() : normalizedParam.getDeleteWorkJobFileSeqList()) {
			WorkFileDeletePO deleteCommand = new WorkFileDeletePO();
			deleteCommand.setWorkSeq(normalizedParam.getWorkSeq());
			deleteCommand.setWorkJobFileSeq(workJobFileSeq);
			deleteAdminCompanyWorkFile(deleteCommand, currentUserNo);
		}

		// 신규 첨부파일이 있으면 같은 업무에 이어서 저장합니다.
		for (MultipartFile fileItem : normalizeWorkFileList(files)) {
			uploadAdminCompanyWorkFile(updatedDetail.getWorkSeq(), fileItem, currentUserNo);
		}
		return getAdminCompanyWorkDetail(updatedDetail.getWorkSeq());
	}

	@Transactional
	// 회사 업무 첨부파일을 업로드하고 저장된 첨부 메타를 반환합니다.
	public AdminCompanyWorkFileVO uploadAdminCompanyWorkFile(Long workSeq, MultipartFile file, Long currentUserNo) {
		// 업무 존재 여부와 업로드 파일 유효성을 먼저 검증합니다.
		long resolvedWorkSeq = normalizeRequiredWorkSequence(workSeq, "업무 정보를 확인해주세요.");
		long resolvedCurrentUserNo = normalizeRequiredUserNo(currentUserNo, "로그인 사용자 정보를 확인해주세요.");
		getRequiredAdminCompanyWorkDetail(resolvedWorkSeq);

		List<MultipartFile> normalizedFileList = normalizeWorkFileList(List.of(file));
		if (normalizedFileList.isEmpty()) {
			throw new IllegalArgumentException("업무 첨부파일을 선택해주세요.");
		}

		// 단건 업로드 기준으로 첨부를 저장하고 최신 메타를 다시 조회합니다.
		AdminCompanyWorkImportFileSavePO fileSaveParam = saveWorkFile(normalizedFileList.get(0), resolvedWorkSeq, resolvedCurrentUserNo, resolvedCurrentUserNo);
		AdminCompanyWorkFileVO savedFile = companyWorkMapper.getAdminCompanyWorkFile(fileSaveParam.getWorkJobFileSeq());
		if (savedFile == null) {
			throw new IllegalStateException("업무 첨부파일 정보를 확인할 수 없습니다.");
		}
		return savedFile;
	}

	@Transactional
	// 회사 업무 첨부파일을 삭제 처리합니다.
	public void deleteAdminCompanyWorkFile(WorkFileDeletePO param, Long currentUserNo) {
		// 삭제 대상 첨부파일과 업무 매칭 여부를 먼저 검증합니다.
		if (param == null) {
			throw new IllegalArgumentException("업무 첨부파일 정보를 확인해주세요.");
		}

		long resolvedWorkSeq = normalizeRequiredWorkSequence(param.getWorkSeq(), "업무 정보를 확인해주세요.");
		int resolvedWorkJobFileSeq = normalizeRequiredSequence(param.getWorkJobFileSeq(), "업무 첨부파일 정보를 확인해주세요.");
		long resolvedCurrentUserNo = normalizeRequiredUserNo(currentUserNo, "로그인 사용자 정보를 확인해주세요.");
		getRequiredAdminCompanyWorkDetail(resolvedWorkSeq);

		AdminCompanyWorkFileVO workFile = companyWorkMapper.getAdminCompanyWorkFile(resolvedWorkJobFileSeq);
		if (workFile == null || workFile.getWorkSeq() == null || !workFile.getWorkSeq().equals(resolvedWorkSeq)) {
			throw new IllegalArgumentException("업무 첨부파일 정보를 확인해주세요.");
		}

		// DB 삭제 플래그를 먼저 반영하고, 내부 업로드 파일이면 FTP 정리도 함께 수행합니다.
		int deletedCount = companyWorkMapper.softDeleteAdminCompanyWorkFile(resolvedWorkSeq, resolvedWorkJobFileSeq, resolvedCurrentUserNo);
		if (deletedCount < 1) {
			throw new IllegalStateException("업무 첨부파일 삭제 중 오류가 발생했습니다.");
		}

		try {
			ftpFileService.deleteCompanyWorkFile(workFile.getWorkJobFileUrl());
		} catch (Exception exception) {
			log.warn("회사 업무 첨부파일 FTP 삭제에 실패했습니다. workSeq={}, workJobFileSeq={}", resolvedWorkSeq, resolvedWorkJobFileSeq, exception);
		}
	}

	@Transactional
	// 관리자 회사 업무 Jira 이슈를 가져와 업무와 첨부파일을 저장합니다.
	public AdminCompanyWorkImportResponseVO importAdminCompanyWork(AdminCompanyWorkImportPO param) {
		// 요청값을 정규화하고 필수값을 검증합니다.
		AdminCompanyWorkImportPO normalizedParam = normalizeImportParam(param);

		// 회사/프로젝트 유효성을 확인하고 회사별 지원 여부를 검증합니다.
		AdminCompanyWorkImportCompanyInfoVO companyInfo = getValidatedImportCompanyInfo(
			normalizedParam.getWorkCompanySeq(),
			normalizedParam.getWorkCompanyProjectSeq()
		);
		validateSupportedCompanyPlatform(companyInfo);

		// 중복 업무키 여부를 먼저 확인합니다.
		int duplicateCount = companyWorkMapper.countAdminCompanyWorkDuplicateKey(
			normalizedParam.getWorkCompanySeq(),
			normalizedParam.getWorkCompanyProjectSeq(),
			normalizedParam.getWorkKey()
		);
		if (duplicateCount > 0) {
			throw new IllegalArgumentException("이미 등록된 업무입니다.");
		}

		// Jira API에서 이슈 상세를 조회한 뒤 저장 파라미터를 구성합니다.
		JsonNode jiraIssueNode = diningBrandsGroupJiraApiClient.getIssue(companyInfo.getApiUrl(), normalizedParam.getWorkKey());
		AdminCompanyWorkImportJobSavePO jobSaveParam = buildImportJobSaveParam(normalizedParam, jiraIssueNode);
		companyWorkMapper.insertAdminCompanyWorkImportJob(jobSaveParam);
		saveImportFileList(jobSaveParam.getWorkSeq(), normalizedParam, jiraIssueNode);

		// 저장 완료 응답을 구성해 반환합니다.
		AdminCompanyWorkImportResponseVO response = new AdminCompanyWorkImportResponseVO();
		response.setMessage("업무를 가져왔습니다.");
		response.setWorkSeq(jobSaveParam.getWorkSeq());
		response.setWorkKey(jobSaveParam.getWorkKey());
		return response;
	}

	@Transactional
	// 관리자 회사 업무 수기 등록을 저장합니다.
	public AdminCompanyWorkManualCreateResponseVO createAdminCompanyWorkManual(AdminCompanyWorkManualCreatePO param) {
		// 관리자 화면 기본 흐름은 현재 로그인 사용자 기준으로 첨부 없는 저장을 수행합니다.
		return createAdminCompanyWorkManual(param, null, resolveRequiredCurrentAdminUserNo());
	}

	@Transactional
	// 회사 업무 수기 등록과 업무 첨부파일 저장을 함께 처리합니다.
	public AdminCompanyWorkManualCreateResponseVO createAdminCompanyWorkManual(
		AdminCompanyWorkManualCreatePO param,
		List<MultipartFile> files,
		Long currentUserNo
	) {
		// 요청값을 정규화하고 필수값과 권한을 검증합니다.
		AdminCompanyWorkManualCreatePO normalizedParam = normalizeManualCreateParam(param);
		validateManualCreateRequester(normalizedParam.getRegNo(), normalizedParam.getUdtNo(), currentUserNo);
		getValidatedImportCompanyInfo(normalizedParam.getWorkCompanySeq(), normalizedParam.getWorkCompanyProjectSeq());
		validateWorkPriorCode(normalizedParam.getWorkPriorCd());

		// 기본 저장 파라미터를 구성하고 수기 등록 업무를 저장합니다.
		AdminCompanyWorkManualSavePO saveParam = buildManualCreateSaveParam(normalizedParam);
		int insertedCount = companyWorkMapper.insertAdminCompanyWorkManualJob(saveParam);
		if (insertedCount < 1 || saveParam.getWorkSeq() == null) {
			throw new IllegalStateException("업무 등록 중 오류가 발생했습니다.");
		}
		saveWorkFileList(
			saveParam.getWorkSeq(),
			normalizedParam.getRegNo(),
			normalizedParam.getUdtNo(),
			normalizeWorkFileList(files)
		);

		// 저장 완료 응답을 구성해 반환합니다.
		AdminCompanyWorkManualCreateResponseVO response = new AdminCompanyWorkManualCreateResponseVO();
		response.setMessage("업무를 등록했습니다.");
		response.setWorkSeq(saveParam.getWorkSeq());
		response.setWorkKey(saveParam.getWorkKey());
		return response;
	}

	@Transactional
	// 관리자 회사 업무 댓글을 저장합니다.
	public AdminCompanyWorkReplyVO saveAdminCompanyWorkReply(AdminCompanyWorkReplySavePO param) {
		// 기존 JSON 저장 경로도 멀티파트 저장 로직을 재사용합니다.
		return saveAdminCompanyWorkReply(param, null, resolveRequiredCurrentAdminUserNo());
	}

	@Transactional
	// 관리자 회사 업무 댓글과 첨부파일을 함께 저장합니다.
	public AdminCompanyWorkReplyVO saveAdminCompanyWorkReply(
		AdminCompanyWorkReplySavePO param,
		List<MultipartFile> files
	) {
		// 관리자 화면 기본 흐름은 현재 로그인 사용자 기준 저장을 수행합니다.
		return saveAdminCompanyWorkReply(param, files, resolveRequiredCurrentAdminUserNo());
	}

	@Transactional
	// 회사 업무 댓글과 첨부파일을 현재 로그인 사용자 기준으로 저장합니다.
	public AdminCompanyWorkReplyVO saveAdminCompanyWorkReply(
		AdminCompanyWorkReplySavePO param,
		List<MultipartFile> files,
		Long currentUserNo
	) {
		// 요청값을 정규화하고 업무 존재 여부를 확인합니다.
		AdminCompanyWorkReplySavePO normalizedParam = normalizeReplySaveParam(param);
		validateReplySaveRequester(normalizedParam.getRegNo(), normalizedParam.getUdtNo(), currentUserNo);
		List<MultipartFile> normalizedFileList = normalizeReplyFileList(files);
		validateReplySaveContent(normalizedParam.getReplyComment(), normalizedFileList);
		getRequiredAdminCompanyWorkDetail(normalizedParam.getWorkSeq());

		// 댓글 저장 후 첨부파일 메타까지 저장하고 최신 댓글 정보를 조회해 반환합니다.
		companyWorkMapper.insertAdminCompanyWorkReply(normalizedParam);
		saveReplyFileList(
			normalizedParam.getWorkSeq(),
			normalizedParam.getReplySeq(),
			normalizedParam.getRegNo(),
			normalizedParam.getUdtNo(),
			normalizedFileList
		);
		return getRequiredAdminCompanyWorkReply(normalizedParam.getReplySeq());
	}

	@Transactional
	// 관리자 회사 업무 댓글과 첨부파일을 함께 수정합니다.
	public AdminCompanyWorkReplyVO updateAdminCompanyWorkReply(
		AdminCompanyWorkReplyUpdatePO param,
		List<MultipartFile> files
	) {
		// 관리자 화면 기본 흐름은 현재 로그인 사용자 기준 저장을 수행합니다.
		return updateAdminCompanyWorkReply(param, files, resolveRequiredCurrentAdminUserNo());
	}

	@Transactional
	// 회사 업무 댓글과 첨부파일을 현재 로그인 사용자 기준으로 수정합니다.
	public AdminCompanyWorkReplyVO updateAdminCompanyWorkReply(
		AdminCompanyWorkReplyUpdatePO param,
		List<MultipartFile> files,
		Long currentUserNo
	) {
		// 요청값과 작성자 권한과 첨부 삭제 대상을 함께 검증합니다.
		AdminCompanyWorkReplyUpdatePO normalizedParam = normalizeReplyUpdateParam(param);
		validateReplyUpdateRequester(normalizedParam.getUdtNo(), currentUserNo);
		getRequiredAdminCompanyWorkDetail(normalizedParam.getWorkSeq());
		AdminCompanyWorkReplyVO currentReply = getAuthorizedAdminCompanyWorkReply(
			normalizedParam.getReplySeq(),
			normalizedParam.getWorkSeq(),
			currentUserNo
		);
		List<MultipartFile> normalizedFileList = normalizeReplyFileList(files);
		List<AdminCompanyWorkReplyFileVO> currentReplyFileList = companyWorkMapper.getAdminCompanyWorkReplyFileListByReplySeq(normalizedParam.getReplySeq());
		List<Integer> deleteReplyFileSeqList = normalizeDeleteReplyFileSeqList(
			normalizedParam.getDeleteReplyFileSeqList(),
			currentReplyFileList
		);
		validateReplyUpdateContent(
			normalizedParam.getReplyComment(),
			(currentReplyFileList == null ? 0 : currentReplyFileList.size()) - deleteReplyFileSeqList.size(),
			normalizedFileList
		);
		normalizedParam.setDeleteReplyFileSeqList(deleteReplyFileSeqList);

		// 댓글 본문과 삭제 대상 첨부를 반영한 뒤 신규 첨부를 저장합니다.
		int updatedCount = companyWorkMapper.updateAdminCompanyWorkReply(normalizedParam);
		if (updatedCount < 1) {
			throw new IllegalStateException("댓글 수정 중 오류가 발생했습니다.");
		}
		if (!deleteReplyFileSeqList.isEmpty()) {
			companyWorkMapper.softDeleteAdminCompanyWorkReplyFiles(
				normalizedParam.getReplySeq(),
				normalizedParam.getWorkSeq(),
				deleteReplyFileSeqList,
				normalizedParam.getUdtNo()
			);
		}
		saveReplyFileList(
			normalizedParam.getWorkSeq(),
			normalizedParam.getReplySeq(),
			currentReply.getRegNo(),
			normalizedParam.getUdtNo(),
			normalizedFileList
		);
		return getRequiredAdminCompanyWorkReply(normalizedParam.getReplySeq());
	}

	@Transactional
	// 관리자 회사 업무 댓글을 삭제 처리합니다.
	public void deleteAdminCompanyWorkReply(AdminCompanyWorkReplyDeletePO param) {
		// 관리자 화면 기본 흐름은 현재 로그인 사용자 기준 삭제를 수행합니다.
		deleteAdminCompanyWorkReply(param, resolveRequiredCurrentAdminUserNo());
	}

	@Transactional
	// 회사 업무 댓글을 현재 로그인 사용자 기준으로 삭제 처리합니다.
	public void deleteAdminCompanyWorkReply(AdminCompanyWorkReplyDeletePO param, Long currentUserNo) {
		// 요청값과 작성자 권한을 검증한 뒤 댓글과 첨부를 함께 숨김 처리합니다.
		AdminCompanyWorkReplyDeletePO normalizedParam = normalizeReplyDeleteParam(param);
		validateReplyUpdateRequester(normalizedParam.getUdtNo(), currentUserNo);
		getRequiredAdminCompanyWorkDetail(normalizedParam.getWorkSeq());
		getAuthorizedAdminCompanyWorkReply(normalizedParam.getReplySeq(), normalizedParam.getWorkSeq(), currentUserNo);

		int deletedReplyCount = companyWorkMapper.softDeleteAdminCompanyWorkReply(normalizedParam);
		if (deletedReplyCount < 1) {
			throw new IllegalStateException("댓글 삭제 중 오류가 발생했습니다.");
		}
		companyWorkMapper.softDeleteAdminCompanyWorkReplyFileList(
			normalizedParam.getReplySeq(),
			normalizedParam.getWorkSeq(),
			normalizedParam.getUdtNo()
		);
	}

	// 관리자 회사 업무 댓글 첨부파일을 다운로드합니다.
	public AdminCompanyWorkReplyFileDownloadVO downloadAdminCompanyWorkReplyFile(Integer replyFileSeq) {
		// 요청 댓글 첨부파일 번호를 검증하고 메타를 조회합니다.
		int resolvedReplyFileSeq = normalizeRequiredSequence(replyFileSeq, "댓글 첨부파일 정보를 확인해주세요.");
		AdminCompanyWorkReplyFileVO replyFile = companyWorkMapper.getAdminCompanyWorkReplyFile(resolvedReplyFileSeq);
		if (replyFile == null) {
			throw new IllegalArgumentException("댓글 첨부파일 정보를 확인해주세요.");
		}

		// 저장 URL을 기준으로 공개 파일을 다운로드하고 응답 구조로 반환합니다.
		try {
			String replyFileUrl = trimToNull(replyFile.getReplyFileUrl());
			String replyFileViewBase = trimToNull(ftpProperties.getUploadCompanyWorkReplyView());
			if (replyFileViewBase == null || replyFileUrl == null || !replyFileUrl.startsWith(replyFileViewBase)) {
				throw new IllegalStateException("댓글 첨부파일 URL을 확인할 수 없습니다.");
			}

			AdminCompanyWorkReplyFileDownloadVO response = new AdminCompanyWorkReplyFileDownloadVO();
			response.setReplyFileNm(safeValue(replyFile.getReplyFileNm()));
			response.setFileData(ftpFileService.downloadFileFromUrl(replyFileUrl));
			return response;
		} catch (IllegalStateException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new IllegalStateException("댓글 첨부파일 다운로드 중 오류가 발생했습니다.", exception);
		}
	}

	// 관리자 회사 업무 공통 조회 파라미터를 생성합니다.
	private AdminCompanyWorkSearchPO createSearchParam(
		Integer workCompanySeq,
		Integer workCompanyProjectSeq,
		String title,
		Integer page,
		Integer pageSize
	) {
		// 관리자 화면 기본 조회는 본문검색 미사용, 상태 미지정 조건으로 위임합니다.
		return createSearchParam(workCompanySeq, workCompanyProjectSeq, title, page, pageSize, null, null);
	}

	// 회사 업무 공통 조회 파라미터를 본문검색과 상태 필터 포함 기준으로 생성합니다.
	private AdminCompanyWorkSearchPO createSearchParam(
		Integer workCompanySeq,
		Integer workCompanyProjectSeq,
		String title,
		Integer page,
		Integer pageSize,
		String includeBodyYn,
		List<String> workStatCdList
	) {
		// 필수 회사와 프로젝트 번호를 먼저 검증합니다.
		int resolvedWorkCompanySeq = normalizeRequiredSequence(workCompanySeq, "회사를 선택해주세요.");
		int resolvedWorkCompanyProjectSeq = normalizeRequiredSequence(workCompanyProjectSeq, "프로젝트를 선택해주세요.");

		// 페이징 조건은 완료 목록에서만 사용하도록 기본값을 계산합니다.
		int resolvedPage = page == null ? ADMIN_COMPANY_WORK_DEFAULT_PAGE : normalizePage(page);
		int resolvedPageSize = pageSize == null ? ADMIN_COMPANY_WORK_DEFAULT_PAGE_SIZE : normalizePageSize(pageSize);
		int offset = (resolvedPage - 1) * resolvedPageSize;

		// 검증 완료된 조회 조건을 PO에 반영합니다.
		AdminCompanyWorkSearchPO param = new AdminCompanyWorkSearchPO();
		param.setWorkCompanySeq(resolvedWorkCompanySeq);
		param.setWorkCompanyProjectSeq(resolvedWorkCompanyProjectSeq);
		param.setTitle(trimToNull(title));
		param.setPage(resolvedPage);
		param.setPageSize(resolvedPageSize);
		param.setOffset(offset);
		param.setIncludeBodyYn(normalizeIncludeBodyYn(includeBodyYn));
		param.setWorkStatCdList(normalizeWorkStatusCodeFilter(workStatCdList));
		return param;
	}

	// 관리자 회사 업무 상세 수정 요청값을 정규화합니다.
	private AdminCompanyWorkDetailUpdatePO normalizeDetailUpdateParam(AdminCompanyWorkDetailUpdatePO param) {
		// 요청 객체와 필수 업무 번호를 먼저 검증합니다.
		if (param == null) {
			throw new IllegalArgumentException("상세 저장 요청 정보를 확인해주세요.");
		}

		long resolvedWorkSeq = normalizeRequiredWorkSequence(param.getWorkSeq(), "업무 정보를 확인해주세요.");
		String normalizedWorkStatCd = trimToNull(param.getWorkStatCd());
		if (normalizedWorkStatCd == null) {
			throw new IllegalArgumentException("업무 상태를 확인해주세요.");
		}
		String normalizedTitle = null;
		if (param.getTitle() != null) {
			normalizedTitle = trimToNull(param.getTitle());
			if (normalizedTitle == null) {
				throw new IllegalArgumentException("업무 제목을 확인해주세요.");
			}
			if (normalizedTitle.length() > ADMIN_COMPANY_WORK_MAX_TITLE_LENGTH) {
				throw new IllegalArgumentException("업무 제목을 확인해주세요.");
			}
		}
		String normalizedCoManager = param.getCoManager() == null
			? null
			: normalizeOptionalManagerName(param.getCoManager(), "업무담당자를 확인해주세요.");
		String normalizedContent = param.getContent() == null
			? null
			: normalizeOptionalCompanyWorkContent(param.getContent());
		String normalizedWorkCreateDt = normalizeOptionalDate(param.getWorkCreateDt(), "업무 생성 일시를 확인해주세요.");
		String normalizedWorkStartDt = normalizeOptionalDate(param.getWorkStartDt(), "시작일시를 확인해주세요.");
		String normalizedWorkEndDt = normalizeOptionalDate(param.getWorkEndDt(), "종료일시를 확인해주세요.");
		Integer normalizedWorkTime = normalizeOptionalWorkTime(param.getWorkTime());
		List<Integer> normalizedDeleteWorkJobFileSeqList = normalizeDeleteWorkFileSeqList(param.getDeleteWorkJobFileSeqList());
		long resolvedUdtNo = normalizeRequiredUserNo(param.getUdtNo(), "로그인 사용자 정보를 확인해주세요.");

		// 정규화된 상세 저장 요청값을 새 객체에 반영합니다.
		AdminCompanyWorkDetailUpdatePO normalizedParam = new AdminCompanyWorkDetailUpdatePO();
		normalizedParam.setWorkSeq(resolvedWorkSeq);
		normalizedParam.setTitle(normalizedTitle);
		normalizedParam.setWorkStatCd(normalizedWorkStatCd);
		normalizedParam.setCoManager(normalizedCoManager);
		normalizedParam.setWorkCreateDt(normalizedWorkCreateDt);
		normalizedParam.setWorkStartDt(normalizedWorkStartDt);
		normalizedParam.setWorkEndDt(normalizedWorkEndDt);
		normalizedParam.setWorkTime(normalizedWorkTime);
		normalizedParam.setContent(normalizedContent);
		normalizedParam.setDeleteWorkJobFileSeqList(normalizedDeleteWorkJobFileSeqList);
		normalizedParam.setUdtNo(resolvedUdtNo);
		return normalizedParam;
	}

	// 관리자 회사 업무 즉시 수정 요청값을 정규화합니다.
	private AdminCompanyWorkUpdatePO normalizeUpdateParam(AdminCompanyWorkUpdatePO param) {
		// 요청 객체와 필수 업무 번호를 먼저 검증합니다.
		if (param == null) {
			throw new IllegalArgumentException("수정 요청 정보를 확인해주세요.");
		}

		long resolvedWorkSeq = normalizeRequiredWorkSequence(param.getWorkSeq(), "업무 정보를 확인해주세요.");
		String normalizedWorkStatCd = trimToNull(param.getWorkStatCd());
		if (normalizedWorkStatCd == null) {
			throw new IllegalArgumentException("업무 상태를 확인해주세요.");
		}
		String normalizedWorkStartDt = normalizeOptionalDate(param.getWorkStartDt(), "시작일시를 확인해주세요.");
		String normalizedWorkEndDt = normalizeOptionalDate(param.getWorkEndDt(), "종료일시를 확인해주세요.");
		Integer normalizedWorkTime = normalizeOptionalWorkTime(param.getWorkTime());
		String normalizedItManager = trimToNull(limitLength(trimToNull(param.getItManager()), ADMIN_COMPANY_WORK_MAX_MANAGER_NAME_LENGTH));
		long resolvedUdtNo = normalizeRequiredUserNo(param.getUdtNo(), "로그인 사용자 정보를 확인해주세요.");

		// 정규화된 수정 요청값을 새 객체에 반영합니다.
		AdminCompanyWorkUpdatePO normalizedParam = new AdminCompanyWorkUpdatePO();
		normalizedParam.setWorkSeq(resolvedWorkSeq);
		normalizedParam.setWorkStatCd(normalizedWorkStatCd);
		normalizedParam.setWorkStartDt(normalizedWorkStartDt);
		normalizedParam.setWorkEndDt(normalizedWorkEndDt);
		normalizedParam.setWorkTime(normalizedWorkTime);
		normalizedParam.setItManager(normalizedItManager);
		normalizedParam.setUdtNo(resolvedUdtNo);
		return normalizedParam;
	}

	// 관리자 회사 업무 수기 등록 요청값을 정규화합니다.
	private AdminCompanyWorkManualCreatePO normalizeManualCreateParam(AdminCompanyWorkManualCreatePO param) {
		// 요청 객체와 필수값을 먼저 검증합니다.
		if (param == null) {
			throw new IllegalArgumentException("수기 등록 요청 정보를 확인해주세요.");
		}

		int resolvedWorkCompanySeq = normalizeRequiredSequence(param.getWorkCompanySeq(), "회사를 선택해주세요.");
		int resolvedWorkCompanyProjectSeq = normalizeRequiredSequence(param.getWorkCompanyProjectSeq(), "프로젝트를 선택해주세요.");
		String normalizedTitle = trimToNull(param.getTitle());
		if (normalizedTitle == null) {
			throw new IllegalArgumentException("타이틀을 입력해주세요.");
		}
		if (normalizedTitle.length() > ADMIN_COMPANY_WORK_MAX_TITLE_LENGTH) {
			throw new IllegalArgumentException("타이틀을 확인해주세요.");
		}
		String normalizedWorkPriorCd = trimToNull(param.getWorkPriorCd());
		if (normalizedWorkPriorCd == null) {
			throw new IllegalArgumentException("우선순위를 선택해주세요.");
		}
		String normalizedContent = normalizeOptionalCompanyWorkContent(param.getContent());
		String normalizedCoManager = normalizeOptionalManagerName(param.getCoManager(), "업무담당자를 확인해주세요.");
		long resolvedRegNo = normalizeRequiredUserNo(param.getRegNo(), "로그인 사용자 정보를 확인해주세요.");
		long resolvedUdtNo = normalizeRequiredUserNo(param.getUdtNo(), "로그인 사용자 정보를 확인해주세요.");

		// 정규화된 수기 등록 요청값을 새 객체에 반영합니다.
		AdminCompanyWorkManualCreatePO normalizedParam = new AdminCompanyWorkManualCreatePO();
		normalizedParam.setWorkCompanySeq(resolvedWorkCompanySeq);
		normalizedParam.setWorkCompanyProjectSeq(resolvedWorkCompanyProjectSeq);
		normalizedParam.setTitle(normalizedTitle);
		normalizedParam.setContent(normalizedContent);
		normalizedParam.setCoManager(normalizedCoManager);
		normalizedParam.setWorkPriorCd(normalizedWorkPriorCd);
		normalizedParam.setRegNo(resolvedRegNo);
		normalizedParam.setUdtNo(resolvedUdtNo);
		return normalizedParam;
	}

	// 관리자 회사 업무 댓글 등록 요청값을 정규화합니다.
	private AdminCompanyWorkReplySavePO normalizeReplySaveParam(AdminCompanyWorkReplySavePO param) {
		// 요청 객체와 필수값을 먼저 검증합니다.
		if (param == null) {
			throw new IllegalArgumentException("댓글 등록 요청 정보를 확인해주세요.");
		}

		long resolvedWorkSeq = normalizeRequiredWorkSequence(param.getWorkSeq(), "업무 정보를 확인해주세요.");
		String normalizedReplyComment = normalizeOptionalReplyComment(param.getReplyComment());
		long resolvedRegNo = normalizeRequiredUserNo(param.getRegNo(), "로그인 사용자 정보를 확인해주세요.");
		long resolvedUdtNo = normalizeRequiredUserNo(param.getUdtNo(), "로그인 사용자 정보를 확인해주세요.");

		// 정규화된 댓글 저장 요청값을 새 객체에 반영합니다.
		AdminCompanyWorkReplySavePO normalizedParam = new AdminCompanyWorkReplySavePO();
		normalizedParam.setWorkSeq(resolvedWorkSeq);
		normalizedParam.setReplyComment(normalizedReplyComment);
		normalizedParam.setRegNo(resolvedRegNo);
		normalizedParam.setUdtNo(resolvedUdtNo);
		return normalizedParam;
	}

	// 관리자 회사 업무 댓글 수정 요청값을 정규화합니다.
	private AdminCompanyWorkReplyUpdatePO normalizeReplyUpdateParam(AdminCompanyWorkReplyUpdatePO param) {
		// 요청 객체와 필수값을 먼저 검증합니다.
		if (param == null) {
			throw new IllegalArgumentException("댓글 수정 요청 정보를 확인해주세요.");
		}

		long resolvedReplySeq = normalizeRequiredWorkSequence(param.getReplySeq(), "댓글 정보를 확인해주세요.");
		long resolvedWorkSeq = normalizeRequiredWorkSequence(param.getWorkSeq(), "업무 정보를 확인해주세요.");
		String normalizedReplyComment = normalizeOptionalReplyComment(param.getReplyComment());
		long resolvedUdtNo = normalizeRequiredUserNo(param.getUdtNo(), "로그인 사용자 정보를 확인해주세요.");

		// 정규화된 댓글 수정 요청값을 새 객체에 반영합니다.
		AdminCompanyWorkReplyUpdatePO normalizedParam = new AdminCompanyWorkReplyUpdatePO();
		normalizedParam.setReplySeq(resolvedReplySeq);
		normalizedParam.setWorkSeq(resolvedWorkSeq);
		normalizedParam.setReplyComment(normalizedReplyComment);
		normalizedParam.setDeleteReplyFileSeqList(
			param.getDeleteReplyFileSeqList() == null ? List.of() : new ArrayList<>(param.getDeleteReplyFileSeqList())
		);
		normalizedParam.setUdtNo(resolvedUdtNo);
		return normalizedParam;
	}

	// 관리자 회사 업무 댓글 삭제 요청값을 정규화합니다.
	private AdminCompanyWorkReplyDeletePO normalizeReplyDeleteParam(AdminCompanyWorkReplyDeletePO param) {
		// 요청 객체와 필수값을 먼저 검증합니다.
		if (param == null) {
			throw new IllegalArgumentException("댓글 삭제 요청 정보를 확인해주세요.");
		}

		long resolvedReplySeq = normalizeRequiredWorkSequence(param.getReplySeq(), "댓글 정보를 확인해주세요.");
		long resolvedWorkSeq = normalizeRequiredWorkSequence(param.getWorkSeq(), "업무 정보를 확인해주세요.");
		long resolvedUdtNo = normalizeRequiredUserNo(param.getUdtNo(), "로그인 사용자 정보를 확인해주세요.");

		// 정규화된 댓글 삭제 요청값을 새 객체에 반영합니다.
		AdminCompanyWorkReplyDeletePO normalizedParam = new AdminCompanyWorkReplyDeletePO();
		normalizedParam.setReplySeq(resolvedReplySeq);
		normalizedParam.setWorkSeq(resolvedWorkSeq);
		normalizedParam.setUdtNo(resolvedUdtNo);
		return normalizedParam;
	}

	// 현재 로그인 사용자와 댓글 등록 요청 사용자가 일치하는지 확인합니다.
	private void validateReplySaveRequester(Long regNo, Long udtNo) {
		// 등록자와 수정자 번호는 모두 현재 로그인 사용자와 같아야 합니다.
		validateReplySaveRequester(regNo, udtNo, resolveRequiredCurrentAdminUserNo());
	}

	// 지정한 사용자번호와 댓글 등록 요청 사용자가 일치하는지 확인합니다.
	private void validateReplySaveRequester(Long regNo, Long udtNo, Long currentAdminUserNo) {
		// 등록자와 수정자 번호는 모두 현재 로그인 사용자와 같아야 합니다.
		if (!currentAdminUserNo.equals(regNo) || !currentAdminUserNo.equals(udtNo)) {
			throw new AccessDeniedException("본인 사용자 정보로만 댓글을 등록할 수 있습니다.");
		}
	}

	// 현재 로그인 사용자와 수기 등록 요청 사용자가 일치하는지 확인합니다.
	private void validateManualCreateRequester(Long regNo, Long udtNo) {
		// 등록자와 수정자 번호는 모두 현재 로그인 사용자와 같아야 합니다.
		validateManualCreateRequester(regNo, udtNo, resolveRequiredCurrentAdminUserNo());
	}

	// 지정한 사용자번호와 수기 등록 요청 사용자가 일치하는지 확인합니다.
	private void validateManualCreateRequester(Long regNo, Long udtNo, Long currentAdminUserNo) {
		// 등록자와 수정자 번호는 모두 현재 로그인 사용자와 같아야 합니다.
		if (!currentAdminUserNo.equals(regNo) || !currentAdminUserNo.equals(udtNo)) {
			throw new AccessDeniedException("본인 사용자 정보로만 업무를 등록할 수 있습니다.");
		}
	}

	// 현재 로그인 사용자와 댓글 수정/삭제 요청 사용자가 일치하는지 확인합니다.
	private void validateReplyUpdateRequester(Long udtNo) {
		// 수정자 번호는 현재 로그인 사용자와 같아야 합니다.
		validateReplyUpdateRequester(udtNo, resolveRequiredCurrentAdminUserNo());
	}

	// 지정한 사용자번호와 댓글 수정/삭제 요청 사용자가 일치하는지 확인합니다.
	private void validateReplyUpdateRequester(Long udtNo, Long currentAdminUserNo) {
		// 수정자 번호는 현재 로그인 사용자와 같아야 합니다.
		if (!currentAdminUserNo.equals(udtNo)) {
			throw new AccessDeniedException("본인 사용자 정보로만 댓글을 수정 또는 삭제할 수 있습니다.");
		}
	}

	// 댓글 저장 시 본문 또는 첨부파일이 하나 이상 존재하는지 확인합니다.
	private void validateReplySaveContent(String replyComment, List<MultipartFile> fileList) {
		// 본문과 첨부파일이 모두 비어 있으면 저장을 차단합니다.
		if (trimToNull(replyComment) == null && (fileList == null || fileList.isEmpty())) {
			throw new IllegalArgumentException("댓글 내용 또는 첨부파일을 등록해주세요.");
		}
	}

	// 댓글 수정 시 본문 또는 활성 첨부파일이 하나 이상 존재하는지 확인합니다.
	private void validateReplyUpdateContent(String replyComment, int remainingFileCount, List<MultipartFile> fileList) {
		// 수정 후 본문과 활성 첨부파일이 모두 비어 있으면 저장을 차단합니다.
		if (trimToNull(replyComment) == null && remainingFileCount < 1 && (fileList == null || fileList.isEmpty())) {
			throw new IllegalArgumentException("댓글 내용 또는 첨부파일을 등록해주세요.");
		}
	}

	// 멀티파트 댓글 첨부파일 목록을 정규화하고 개별 파일을 검증합니다.
	private List<MultipartFile> normalizeReplyFileList(List<MultipartFile> files) {
		List<MultipartFile> normalizedFileList = new ArrayList<>();
		for (MultipartFile fileItem : files == null ? List.<MultipartFile>of() : files) {
			if (fileItem == null || fileItem.isEmpty()) {
				continue;
			}

			// 실제 업로드 대상 파일만 남기고 개별 유효성을 확인합니다.
			validateReplyAttachmentFile(fileItem);
			normalizedFileList.add(fileItem);
		}
		return normalizedFileList;
	}

	// 멀티파트 업무 첨부파일 목록을 정규화하고 개별 파일을 검증합니다.
	private List<MultipartFile> normalizeWorkFileList(List<MultipartFile> files) {
		List<MultipartFile> normalizedFileList = new ArrayList<>();
		for (MultipartFile fileItem : files == null ? List.<MultipartFile>of() : files) {
			if (fileItem == null || fileItem.isEmpty()) {
				continue;
			}

			// 실제 업로드 대상 파일만 남기고 개별 유효성을 확인합니다.
			validateWorkAttachmentFile(fileItem);
			normalizedFileList.add(fileItem);
		}
		return normalizedFileList;
	}

	// 댓글 첨부파일 개별 유효성을 검증합니다.
	private void validateReplyAttachmentFile(MultipartFile file) {
		// 업로드 설정과 파일명과 용량과 확장자를 순서대로 확인합니다.
		if (ftpProperties.getUploadCompanyWorkReplyMaxSize() <= 0) {
			throw new IllegalStateException("댓글 첨부파일 업로드 설정을 확인해주세요.");
		}
		if (trimToNull(ftpProperties.getUploadCompanyWorkReplyAllowExtension()) == null) {
			throw new IllegalStateException("댓글 첨부파일 허용 확장자 설정을 확인해주세요.");
		}

		String originalFileName = extractOriginalFileName(file);
		long maxSizeInBytes = (long) ftpProperties.getUploadCompanyWorkReplyMaxSize() * 1024 * 1024;
		if (file.getSize() > maxSizeInBytes) {
			throw new IllegalArgumentException("댓글 첨부파일 크기가 " + ftpProperties.getUploadCompanyWorkReplyMaxSize() + "MB를 초과합니다.");
		}

		String extension = extractFileExtension(originalFileName);
		if (extension == null || !isAllowedFileExtension(ftpProperties.getUploadCompanyWorkReplyAllowExtension(), extension)) {
			throw new IllegalArgumentException("허용되지 않는 댓글 첨부파일 형식입니다. 허용 형식: " + ftpProperties.getUploadCompanyWorkReplyAllowExtension());
		}
	}

	// 업무 첨부파일 개별 유효성을 검증합니다.
	private void validateWorkAttachmentFile(MultipartFile file) {
		// 현재 업무 첨부는 댓글 첨부 업로드 설정을 재사용합니다.
		if (ftpProperties.getUploadCompanyWorkReplyMaxSize() <= 0) {
			throw new IllegalStateException("업무 첨부파일 업로드 설정을 확인해주세요.");
		}
		if (trimToNull(ftpProperties.getUploadCompanyWorkReplyAllowExtension()) == null) {
			throw new IllegalStateException("업무 첨부파일 허용 확장자 설정을 확인해주세요.");
		}

		String originalFileName = extractOriginalFileName(file);
		long maxSizeInBytes = (long) ftpProperties.getUploadCompanyWorkReplyMaxSize() * 1024 * 1024;
		if (file.getSize() > maxSizeInBytes) {
			throw new IllegalArgumentException("업무 첨부파일 크기가 " + ftpProperties.getUploadCompanyWorkReplyMaxSize() + "MB를 초과합니다.");
		}

		String extension = extractFileExtension(originalFileName);
		if (extension == null || !isAllowedFileExtension(ftpProperties.getUploadCompanyWorkReplyAllowExtension(), extension)) {
			throw new IllegalArgumentException("허용되지 않는 업무 첨부파일 형식입니다. 허용 형식: " + ftpProperties.getUploadCompanyWorkReplyAllowExtension());
		}
	}

	// 삭제 요청된 업무 첨부파일 목록을 정규화합니다.
	private List<Integer> normalizeDeleteWorkFileSeqList(List<Integer> deleteWorkFileSeqList) {
		LinkedHashSet<Integer> normalizedDeleteWorkFileSeqSet = new LinkedHashSet<>();
		for (Integer deleteWorkFileSeq : deleteWorkFileSeqList == null ? List.<Integer>of() : deleteWorkFileSeqList) {
			normalizedDeleteWorkFileSeqSet.add(
				normalizeRequiredSequence(deleteWorkFileSeq, "삭제할 업무 첨부파일 정보를 확인해주세요.")
			);
		}
		return new ArrayList<>(normalizedDeleteWorkFileSeqSet);
	}

	// 삭제 요청된 댓글 첨부파일 목록을 현재 댓글 범위 안에서 정규화합니다.
	private List<Integer> normalizeDeleteReplyFileSeqList(
		List<Integer> deleteReplyFileSeqList,
		List<AdminCompanyWorkReplyFileVO> currentReplyFileList
	) {
		LinkedHashSet<Integer> validReplyFileSeqSet = new LinkedHashSet<>();
		for (AdminCompanyWorkReplyFileVO currentReplyFileItem : currentReplyFileList == null ? List.<AdminCompanyWorkReplyFileVO>of() : currentReplyFileList) {
			if (currentReplyFileItem != null && currentReplyFileItem.getReplyFileSeq() != null) {
				validReplyFileSeqSet.add(currentReplyFileItem.getReplyFileSeq());
			}
		}

		LinkedHashSet<Integer> normalizedDeleteReplyFileSeqSet = new LinkedHashSet<>();
		for (Integer deleteReplyFileSeq : deleteReplyFileSeqList == null ? List.<Integer>of() : deleteReplyFileSeqList) {
			int resolvedReplyFileSeq = normalizeRequiredSequence(deleteReplyFileSeq, "삭제할 댓글 첨부파일 정보를 확인해주세요.");
			if (!validReplyFileSeqSet.contains(resolvedReplyFileSeq)) {
				throw new IllegalArgumentException("삭제할 댓글 첨부파일 정보를 확인해주세요.");
			}
			normalizedDeleteReplyFileSeqSet.add(resolvedReplyFileSeq);
		}
		return new ArrayList<>(normalizedDeleteReplyFileSeqSet);
	}

	// 댓글 첨부파일을 FTP와 DB에 함께 저장합니다.
	private void saveReplyFileList(
		Long workSeq,
		Long replySeq,
		Long regNo,
		Long udtNo,
		List<MultipartFile> fileList
	) {
		// 저장할 첨부파일이 없으면 바로 종료합니다.
		if (fileList == null || fileList.isEmpty()) {
			return;
		}

		List<String> uploadedFileUrlList = new ArrayList<>();

		try {
			// 업로드 성공 URL을 누적해 두었다가 실패 시 정리할 수 있게 합니다.
			for (MultipartFile fileItem : fileList) {
				String originalFileName = extractOriginalFileName(fileItem);
				String uploadedFileUrl = limitLength(
					ftpFileService.uploadCompanyWorkReplyFile(
						fileItem,
						workSeq,
						replySeq,
						String.valueOf(regNo)
					),
					ADMIN_COMPANY_WORK_MAX_FILE_URL_LENGTH
				);
				uploadedFileUrlList.add(uploadedFileUrl);

				AdminCompanyWorkReplyFileSavePO fileSaveParam = new AdminCompanyWorkReplyFileSavePO();
				fileSaveParam.setReplySeq(replySeq);
				fileSaveParam.setWorkSeq(workSeq);
				fileSaveParam.setReplyFileNm(limitLength(originalFileName, ADMIN_COMPANY_WORK_MAX_FILE_NAME_LENGTH));
				fileSaveParam.setReplyFileUrl(uploadedFileUrl);
				fileSaveParam.setReplyFileSize(fileItem.getSize());
				fileSaveParam.setRegNo(regNo);
				fileSaveParam.setUdtNo(udtNo);
				companyWorkMapper.insertAdminCompanyWorkReplyFile(fileSaveParam);
			}
		} catch (IllegalArgumentException exception) {
			// 사용자 교정이 가능한 오류가 나면 업로드된 파일만 정리하고 그대로 전달합니다.
			cleanupUploadedReplyFiles(uploadedFileUrlList);
			throw exception;
		} catch (Exception exception) {
			// 저장 중 예기치 않은 오류가 나면 업로드된 파일을 정리한 뒤 서버 오류로 변환합니다.
			cleanupUploadedReplyFiles(uploadedFileUrlList);
			throw new IllegalStateException("댓글 첨부파일 저장 중 오류가 발생했습니다.", exception);
		}
	}

	// 댓글 첨부파일 업로드 후 실패한 경우 이미 올라간 FTP 파일을 정리합니다.
	private void cleanupUploadedReplyFiles(List<String> uploadedFileUrlList) {
		for (String uploadedFileUrl : uploadedFileUrlList == null ? List.<String>of() : uploadedFileUrlList) {
			try {
				// 정리 실패는 원래 예외를 덮지 않도록 로그만 남깁니다.
				ftpFileService.deleteCompanyWorkReplyFile(uploadedFileUrl);
			} catch (Exception cleanupException) {
				log.warn("회사 업무 댓글 첨부파일 정리에 실패했습니다. fileUrl={}", uploadedFileUrl, cleanupException);
			}
		}
	}

	// 업무 첨부파일을 FTP와 DB에 함께 저장합니다.
	private void saveWorkFileList(
		Long workSeq,
		Long regNo,
		Long udtNo,
		List<MultipartFile> fileList
	) {
		// 저장할 첨부파일이 없으면 바로 종료합니다.
		if (fileList == null || fileList.isEmpty()) {
			return;
		}

		List<String> uploadedFileUrlList = new ArrayList<>();

		try {
			// 업로드 성공 URL을 누적해 두었다가 실패 시 정리할 수 있게 합니다.
			for (MultipartFile fileItem : fileList) {
				AdminCompanyWorkImportFileSavePO savedFile = saveWorkFile(fileItem, workSeq, regNo, udtNo);
				uploadedFileUrlList.add(savedFile.getWorkJobFileUrl());
			}
		} catch (IllegalArgumentException exception) {
			// 사용자 교정이 가능한 오류가 나면 업로드된 파일만 정리하고 그대로 전달합니다.
			cleanupUploadedWorkFiles(uploadedFileUrlList);
			throw exception;
		} catch (Exception exception) {
			// 저장 중 예기치 않은 오류가 나면 업로드된 파일을 정리한 뒤 서버 오류로 변환합니다.
			cleanupUploadedWorkFiles(uploadedFileUrlList);
			throw new IllegalStateException("업무 첨부파일 저장 중 오류가 발생했습니다.", exception);
		}
	}

	// 업무 첨부파일 단건을 FTP와 DB에 함께 저장합니다.
	private AdminCompanyWorkImportFileSavePO saveWorkFile(
		MultipartFile file,
		Long workSeq,
		Long regNo,
		Long udtNo
	) {
		try {
			// FTP 업로드 후 첨부 메타를 저장합니다.
			String originalFileName = extractOriginalFileName(file);
			String uploadedFileUrl = limitLength(
				ftpFileService.uploadCompanyWorkFile(file, workSeq, String.valueOf(regNo)),
				ADMIN_COMPANY_WORK_MAX_FILE_URL_LENGTH
			);

			AdminCompanyWorkImportFileSavePO fileSaveParam = new AdminCompanyWorkImportFileSavePO();
			fileSaveParam.setWorkSeq(workSeq);
			fileSaveParam.setWorkJobFileNm(limitLength(originalFileName, ADMIN_COMPANY_WORK_MAX_FILE_NAME_LENGTH));
			fileSaveParam.setWorkJobFileUrl(uploadedFileUrl);
			fileSaveParam.setRegNo(regNo);
			fileSaveParam.setUdtNo(udtNo);
			companyWorkMapper.insertAdminCompanyWorkImportFile(fileSaveParam);
			return fileSaveParam;
		} catch (IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new IllegalStateException("업무 첨부파일 저장 중 오류가 발생했습니다.", exception);
		}
	}

	// 업무 첨부파일 업로드 후 실패한 경우 이미 올라간 FTP 파일을 정리합니다.
	private void cleanupUploadedWorkFiles(List<String> uploadedFileUrlList) {
		for (String uploadedFileUrl : uploadedFileUrlList == null ? List.<String>of() : uploadedFileUrlList) {
			try {
				// 정리 실패는 원래 예외를 덮지 않도록 로그만 남깁니다.
				ftpFileService.deleteCompanyWorkFile(uploadedFileUrl);
			} catch (Exception cleanupException) {
				log.warn("회사 업무 첨부파일 정리에 실패했습니다. fileUrl={}", uploadedFileUrl, cleanupException);
			}
		}
	}

	// 댓글 목록에 댓글 첨부파일 목록을 replySeq 기준으로 주입합니다.
	private List<AdminCompanyWorkReplyVO> applyReplyFileList(
		List<AdminCompanyWorkReplyVO> replyList,
		List<AdminCompanyWorkReplyFileVO> replyFileList
	) {
		Map<Long, List<AdminCompanyWorkReplyFileVO>> replyFileMap = new HashMap<>();
		for (AdminCompanyWorkReplyFileVO replyFileItem : replyFileList == null ? List.<AdminCompanyWorkReplyFileVO>of() : replyFileList) {
			Long replySeq = replyFileItem == null ? null : replyFileItem.getReplySeq();
			if (replySeq == null) {
				continue;
			}

			// 댓글 번호 기준으로 첨부파일 목록을 누적합니다.
			replyFileMap.computeIfAbsent(replySeq, ignoredKey -> new ArrayList<>()).add(replyFileItem);
		}

		List<AdminCompanyWorkReplyVO> normalizedReplyList = new ArrayList<>();
		for (AdminCompanyWorkReplyVO replyItem : replyList == null ? List.<AdminCompanyWorkReplyVO>of() : replyList) {
			if (replyItem == null) {
				continue;
			}

			// 댓글이 첨부가 없더라도 빈 목록을 유지하도록 반영합니다.
			replyItem.setReplyFileList(replyFileMap.getOrDefault(replyItem.getReplySeq(), List.of()));
			normalizedReplyList.add(replyItem);
		}
		return normalizedReplyList;
	}

	// 수기 등록 요청을 DB 저장 파라미터로 변환합니다.
	private AdminCompanyWorkManualSavePO buildManualCreateSaveParam(AdminCompanyWorkManualCreatePO param) {
		// 기본 상태와 자동 생성 업무키를 포함한 저장 파라미터를 구성합니다.
		AdminCompanyWorkManualSavePO saveParam = new AdminCompanyWorkManualSavePO();
		saveParam.setWorkCompanySeq(param.getWorkCompanySeq());
		saveParam.setWorkCompanyProjectSeq(param.getWorkCompanyProjectSeq());
		saveParam.setWorkStatCd(WORK_WAIT_STATUS_CODE);
		saveParam.setWorkPriorCd(param.getWorkPriorCd());
		saveParam.setWorkKey(generateManualWorkKey(param.getWorkCompanySeq(), param.getWorkCompanyProjectSeq(), param.getRegNo()));
		saveParam.setTitle(param.getTitle());
		saveParam.setContent(param.getContent());
		saveParam.setCoManager(param.getCoManager());
		saveParam.setRegNo(param.getRegNo());
		saveParam.setUdtNo(param.getUdtNo());
		return saveParam;
	}

	// 수기 등록 업무키를 자동 생성합니다.
	private String generateManualWorkKey(Integer workCompanySeq, Integer workCompanyProjectSeq, Long regNo) {
		// 같은 밀리초 충돌을 피하기 위해 짧은 재시도 범위 안에서 고유 키를 생성합니다.
		for (int attempt = 0; attempt < 100; attempt += 1) {
			LocalDateTime generatedDateTime = LocalDateTime.now().plusNanos((long) attempt * 1_000_000L);
			String generatedWorkKey = "MANUAL-" + generatedDateTime.format(ADMIN_COMPANY_WORK_MANUAL_KEY_FORMATTER) + "-" + regNo;
			if (generatedWorkKey.length() > ADMIN_COMPANY_WORK_MAX_WORK_KEY_LENGTH) {
				throw new IllegalStateException("수기 업무키를 생성할 수 없습니다.");
			}

			int duplicateCount = companyWorkMapper.countAdminCompanyWorkDuplicateKey(
				workCompanySeq,
				workCompanyProjectSeq,
				generatedWorkKey
			);
			if (duplicateCount < 1) {
				return generatedWorkKey;
			}
		}
		throw new IllegalStateException("수기 업무키를 생성할 수 없습니다.");
	}

	// 관리자 회사 업무 가져오기 요청값을 정규화합니다.
	private AdminCompanyWorkImportPO normalizeImportParam(AdminCompanyWorkImportPO param) {
		// 요청 객체와 필수값을 먼저 검증합니다.
		if (param == null) {
			throw new IllegalArgumentException("가져오기 요청 정보를 확인해주세요.");
		}

		int resolvedWorkCompanySeq = normalizeRequiredSequence(param.getWorkCompanySeq(), "회사를 선택해주세요.");
		int resolvedWorkCompanyProjectSeq = normalizeRequiredSequence(param.getWorkCompanyProjectSeq(), "프로젝트를 선택해주세요.");
		String normalizedWorkKey = trimToNull(param.getWorkKey());
		if (normalizedWorkKey == null) {
			throw new IllegalArgumentException("업무키를 입력해주세요.");
		}
		if (normalizedWorkKey.length() > ADMIN_COMPANY_WORK_MAX_WORK_KEY_LENGTH) {
			throw new IllegalArgumentException("업무키를 확인해주세요.");
		}
		long resolvedRegNo = normalizeRequiredUserNo(param.getRegNo(), "로그인 사용자 정보를 확인해주세요.");
		long resolvedUdtNo = normalizeRequiredUserNo(param.getUdtNo(), "로그인 사용자 정보를 확인해주세요.");

		// 정규화된 요청값을 새 객체에 반영합니다.
		AdminCompanyWorkImportPO normalizedParam = new AdminCompanyWorkImportPO();
		normalizedParam.setWorkCompanySeq(resolvedWorkCompanySeq);
		normalizedParam.setWorkCompanyProjectSeq(resolvedWorkCompanyProjectSeq);
		normalizedParam.setWorkKey(normalizedWorkKey);
		normalizedParam.setRegNo(resolvedRegNo);
		normalizedParam.setUdtNo(resolvedUdtNo);
		return normalizedParam;
	}

	// 관리자 회사 업무 가져오기 대상 회사 정보를 검증하고 반환합니다.
	private AdminCompanyWorkImportCompanyInfoVO getValidatedImportCompanyInfo(Integer workCompanySeq, Integer workCompanyProjectSeq) {
		// 회사 기본정보 존재 여부를 먼저 확인합니다.
		AdminCompanyWorkImportCompanyInfoVO companyInfo = companyWorkMapper.getAdminCompanyWorkImportCompanyInfo(workCompanySeq);
		if (companyInfo == null) {
			throw new IllegalArgumentException("회사 정보를 확인해주세요.");
		}

		// 프로젝트가 해당 회사에 속한 항목인지 확인합니다.
		int projectMatchCount = companyWorkMapper.countAdminCompanyWorkProjectMatch(workCompanySeq, workCompanyProjectSeq);
		if (projectMatchCount < 1) {
			throw new IllegalArgumentException("프로젝트 정보를 확인해주세요.");
		}
		return companyInfo;
	}

	// 관리자 회사 업무 상세 정보를 조회하고 없으면 예외를 발생시킵니다.
	private AdminCompanyWorkDetailVO getRequiredAdminCompanyWorkDetail(Long workSeq) {
		// 삭제되지 않은 업무 상세가 없으면 요청 오류로 처리합니다.
		AdminCompanyWorkDetailVO detail = companyWorkMapper.getAdminCompanyWorkDetail(workSeq);
		if (detail == null) {
			throw new IllegalArgumentException("업무 정보를 확인해주세요.");
		}
		return detail;
	}

	// 업무관리 화면 전용 상세 정보를 조회하고 없으면 예외를 발생시킵니다.
	private AdminCompanyWorkDetailVO getRequiredWorkCompanyWorkDetail(Long workSeq) {
		// 삭제되지 않은 업무 상세가 없으면 요청 오류로 처리합니다.
		AdminCompanyWorkDetailVO detail = companyWorkMapper.getWorkCompanyWorkDetail(workSeq);
		if (detail == null) {
			throw new IllegalArgumentException("업무 정보를 확인해주세요.");
		}
		return detail;
	}

	// 관리자 회사 업무 댓글 정보를 조회하고 첨부파일 목록까지 함께 반환합니다.
	private AdminCompanyWorkReplyVO getRequiredAdminCompanyWorkReply(Long replySeq) {
		// 댓글이 없으면 서버 상태 오류로 처리합니다.
		AdminCompanyWorkReplyVO reply = companyWorkMapper.getAdminCompanyWorkReply(replySeq);
		if (reply == null) {
			throw new IllegalStateException("저장된 댓글 정보를 확인할 수 없습니다.");
		}

		// 댓글 첨부파일 목록을 함께 주입해 반환합니다.
		List<AdminCompanyWorkReplyFileVO> replyFileList = companyWorkMapper.getAdminCompanyWorkReplyFileListByReplySeq(replySeq);
		reply.setReplyFileList(replyFileList == null ? List.of() : replyFileList);
		return reply;
	}

	// 수정 또는 삭제 가능한 댓글인지 확인하고 현재 댓글 정보를 반환합니다.
	private AdminCompanyWorkReplyVO getAuthorizedAdminCompanyWorkReply(Long replySeq, Long workSeq) {
		// 관리자 화면 기본 흐름은 현재 로그인 사용자 기준 권한을 검증합니다.
		return getAuthorizedAdminCompanyWorkReply(replySeq, workSeq, resolveRequiredCurrentAdminUserNo());
	}

	// 지정한 사용자 기준으로 수정 또는 삭제 가능한 댓글인지 확인하고 현재 댓글 정보를 반환합니다.
	private AdminCompanyWorkReplyVO getAuthorizedAdminCompanyWorkReply(Long replySeq, Long workSeq, Long currentAdminUserNo) {
		// 댓글 존재 여부와 업무 매칭 여부를 먼저 확인합니다.
		AdminCompanyWorkReplyVO reply = companyWorkMapper.getAdminCompanyWorkReply(replySeq);
		if (reply == null || reply.getWorkSeq() == null || !reply.getWorkSeq().equals(workSeq)) {
			throw new IllegalArgumentException("댓글 정보를 확인해주세요.");
		}

		// 현재 로그인 사용자가 작성자가 아니면 수정/삭제를 허용하지 않습니다.
		if (reply.getRegNo() == null || !reply.getRegNo().equals(currentAdminUserNo)) {
			throw new AccessDeniedException("본인이 작성한 댓글만 수정 또는 삭제할 수 있습니다.");
		}
		return reply;
	}

	// 현재 로그인한 관리자 사용자번호를 조회합니다.
	private Long resolveCurrentAdminUserNo() {
		// 스프링 시큐리티 인증정보에서 관리자 사용자번호를 추출합니다.
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			return null;
		}
		Object principal = authentication.getPrincipal();
		if (principal instanceof UserBaseEntity userBaseEntity) {
			return userBaseEntity.getUsrNo();
		}
		return null;
	}

	// 현재 로그인한 관리자 사용자번호를 필수값으로 조회합니다.
	private Long resolveRequiredCurrentAdminUserNo() {
		// 로그인 정보가 없으면 권한 오류로 처리합니다.
		Long currentAdminUserNo = resolveCurrentAdminUserNo();
		if (currentAdminUserNo == null || currentAdminUserNo < 1) {
			throw new AccessDeniedException("로그인 사용자 정보를 확인해주세요.");
		}
		return currentAdminUserNo;
	}

	// 현재 구현이 지원하는 회사/플랫폼인지 확인합니다.
	private void validateSupportedCompanyPlatform(AdminCompanyWorkImportCompanyInfoVO companyInfo) {
		// 다이닝브랜즈그룹 Jira만 지원하고 나머지는 차단합니다.
		String companyName = trimToNull(companyInfo == null ? null : companyInfo.getWorkCompanyNm());
		String platformName = trimToNull(companyInfo == null ? null : companyInfo.getWorkPlatformNm());
		if (!DINING_BRANDS_GROUP_COMPANY_NAME.equals(companyName) || platformName == null || !JIRA_PLATFORM_NAME.equalsIgnoreCase(platformName)) {
			throw new IllegalArgumentException("아직 지원하지 않는 회사 업무 플랫폼입니다.");
		}
	}

	// 업무 우선순위 코드가 사용 가능한 공통코드인지 확인합니다.
	private void validateWorkPriorCode(String workPriorCd) {
		// 사용 가능한 우선순위 코드 목록에 존재하지 않으면 요청 오류로 처리합니다.
		List<CommonCodeVO> workPriorityCodeList = commonMapper.getCommonCodeList(WORK_PRIORITY_GROUP_CODE);
		for (CommonCodeVO workPriorityCodeItem : workPriorityCodeList == null ? List.<CommonCodeVO>of() : workPriorityCodeList) {
			if (workPriorityCodeItem != null && workPriorCd.equals(trimToNull(workPriorityCodeItem.getCd()))) {
				return;
			}
		}
		throw new IllegalArgumentException("우선순위를 확인해주세요.");
	}

	// 업무 상태 코드가 사용 가능한 공통코드인지 확인합니다.
	private void validateWorkStatusCode(String workStatCd) {
		// 사용 가능한 상태 코드 목록에 존재하지 않으면 요청 오류로 처리합니다.
		List<CommonCodeVO> workStatusCodeList = commonMapper.getCommonCodeList(WORK_STATUS_GROUP_CODE);
		for (CommonCodeVO workStatusCodeItem : workStatusCodeList == null ? List.<CommonCodeVO>of() : workStatusCodeList) {
			if (workStatusCodeItem != null && workStatCd.equals(trimToNull(workStatusCodeItem.getCd()))) {
				return;
			}
		}
		throw new IllegalArgumentException("업무 상태를 확인해주세요.");
	}

	// Jira 이슈 응답을 회사 업무 저장 파라미터로 변환합니다.
	private AdminCompanyWorkImportJobSavePO buildImportJobSaveParam(AdminCompanyWorkImportPO param, JsonNode jiraIssueNode) {
		// Jira 기본 필드 노드를 추출합니다.
		String resolvedWorkKey = resolveJiraIssueKey(jiraIssueNode, param.getWorkKey());
		JsonNode fieldsNode = jiraIssueNode == null ? null : jiraIssueNode.path("fields");
		String summary = trimToNull(fieldsNode == null ? null : fieldsNode.path("summary").asText(null));
		String title = "[" + resolvedWorkKey + "] " + safeValue(summary);
		String content = extractJiraDescriptionText(fieldsNode == null ? null : fieldsNode.path("description"));
		String workCreateDt = resolveJiraCreatedDateTime(fieldsNode == null ? null : fieldsNode.path("created").asText(null));
		String coManager = trimToNull(fieldsNode == null ? null : fieldsNode.path("reporter").path("displayName").asText(null));
		String workPriorCd = resolveJiraWorkPriorCd(fieldsNode == null ? null : fieldsNode.path("priority").path("name").asText(null));

		// DB 저장용 업무 파라미터를 구성합니다.
		AdminCompanyWorkImportJobSavePO jobSaveParam = new AdminCompanyWorkImportJobSavePO();
		jobSaveParam.setWorkCompanySeq(param.getWorkCompanySeq());
		jobSaveParam.setWorkCompanyProjectSeq(param.getWorkCompanyProjectSeq());
		jobSaveParam.setWorkStatCd(WORK_WAIT_STATUS_CODE);
		jobSaveParam.setWorkPriorCd(workPriorCd);
		jobSaveParam.setWorkKey(limitLength(resolvedWorkKey, ADMIN_COMPANY_WORK_MAX_WORK_KEY_LENGTH));
		jobSaveParam.setTitle(limitLength(title, ADMIN_COMPANY_WORK_MAX_TITLE_LENGTH));
		jobSaveParam.setContent(content);
		jobSaveParam.setWorkCreateDt(workCreateDt);
		jobSaveParam.setCoManager(limitLength(safeValue(coManager), ADMIN_COMPANY_WORK_MAX_MANAGER_NAME_LENGTH));
		jobSaveParam.setRegNo(param.getRegNo());
		jobSaveParam.setUdtNo(param.getUdtNo());
		return jobSaveParam;
	}

	// Jira 첨부파일 배열을 회사 업무 첨부파일 테이블에 저장합니다.
	private void saveImportFileList(Long workSeq, AdminCompanyWorkImportPO param, JsonNode jiraIssueNode) {
		// 업무 번호가 없거나 첨부 배열이 없으면 종료합니다.
		if (workSeq == null || jiraIssueNode == null) {
			return;
		}

		JsonNode attachmentNode = jiraIssueNode.path("fields").path("attachment");
		if (!attachmentNode.isArray()) {
			return;
		}

		// 첨부 배열을 순회하며 건별 저장 파라미터를 생성해 저장합니다.
		for (JsonNode attachmentItem : attachmentNode) {
			String fileName = limitLength(
				safeValue(trimToNull(attachmentItem.path("filename").asText(null))),
				ADMIN_COMPANY_WORK_MAX_FILE_NAME_LENGTH
			);
			String fileUrl = limitLength(
				safeValue(trimToNull(attachmentItem.path("content").asText(null))),
				ADMIN_COMPANY_WORK_MAX_FILE_URL_LENGTH
			);
			if (fileName.isEmpty() && fileUrl.isEmpty()) {
				continue;
			}

			AdminCompanyWorkImportFileSavePO fileSaveParam = new AdminCompanyWorkImportFileSavePO();
			fileSaveParam.setWorkSeq(workSeq);
			fileSaveParam.setWorkJobFileNm(fileName);
			fileSaveParam.setWorkJobFileUrl(fileUrl);
			fileSaveParam.setRegNo(param.getRegNo());
			fileSaveParam.setUdtNo(param.getUdtNo());
			companyWorkMapper.insertAdminCompanyWorkImportFile(fileSaveParam);
		}
	}

	// Jira 이슈 키를 응답값 우선으로 해석합니다.
	private String resolveJiraIssueKey(JsonNode jiraIssueNode, String fallbackWorkKey) {
		// 응답 key가 없으면 요청 업무키를 사용합니다.
		String responseWorkKey = trimToNull(jiraIssueNode == null ? null : jiraIssueNode.path("key").asText(null));
		String resolvedWorkKey = responseWorkKey == null ? trimToNull(fallbackWorkKey) : responseWorkKey;
		if (resolvedWorkKey == null) {
			throw new IllegalStateException("Jira 업무키를 확인할 수 없습니다.");
		}
		return resolvedWorkKey;
	}

	// Jira 우선순위명을 내부 우선순위 코드로 변환합니다.
	private String resolveJiraWorkPriorCd(String jiraPriorityName) {
		// Jira 우선순위명별로 내부 공통코드에 매핑합니다.
		String normalizedPriorityName = trimToNull(jiraPriorityName);
		if ("높음".equals(normalizedPriorityName)) {
			return WORK_PRIOR_HIGH_CODE;
		}
		if ("낮음".equals(normalizedPriorityName)) {
			return WORK_PRIOR_LOW_CODE;
		}
		return WORK_PRIOR_NORMAL_CODE;
	}

	// Jira 생성일시 문자열을 DB 저장용 포맷으로 변환합니다.
	private String resolveJiraCreatedDateTime(String jiraCreatedDateTime) {
		// Jira ISO 오프셋 일시를 LocalDateTime으로 변환합니다.
		String normalizedJiraCreatedDateTime = trimToNull(jiraCreatedDateTime);
		if (normalizedJiraCreatedDateTime == null) {
			throw new IllegalStateException("Jira 업무 생성일시를 확인할 수 없습니다.");
		}

		try {
			LocalDateTime createdDateTime = parseJiraCreatedDateTime(normalizedJiraCreatedDateTime);
			return createdDateTime.format(ADMIN_COMPANY_WORK_DATE_TIME_FORMATTER);
		} catch (Exception exception) {
			throw new IllegalStateException("Jira 업무 생성일시를 변환할 수 없습니다.", exception);
		}
	}

	// Jira 생성일시 문자열을 허용 가능한 패턴으로 순차 파싱합니다.
	private LocalDateTime parseJiraCreatedDateTime(String jiraCreatedDateTime) {
		// 표준 ISO 오프셋 형식을 먼저 시도합니다.
		try {
			return OffsetDateTime.parse(jiraCreatedDateTime).toLocalDateTime();
		} catch (DateTimeParseException ignoredException) {
			// Jira가 +0000 형식을 반환하는 경우를 대비해 다음 패턴을 이어서 시도합니다.
		}

		// 밀리초가 포함된 Jira 오프셋 형식을 시도합니다.
		try {
			return OffsetDateTime.parse(jiraCreatedDateTime, JIRA_OFFSET_DATE_TIME_FORMATTER).toLocalDateTime();
		} catch (DateTimeParseException ignoredException) {
			// 밀리초가 없는 형식도 이어서 시도합니다.
		}

		// 마지막으로 밀리초 없는 +0000 형식을 시도합니다.
		return OffsetDateTime.parse(jiraCreatedDateTime, JIRA_OFFSET_DATE_TIME_WITHOUT_MILLIS_FORMATTER).toLocalDateTime();
	}

	// 날짜/일시 문자열을 yyyy-MM-dd HH:mm:ss 형식으로 정규화합니다.
	private String normalizeOptionalDate(String value, String invalidMessage) {
		// 빈 값은 null로 저장 가능하도록 그대로 반환합니다.
		String normalizedValue = trimToNull(value);
		if (normalizedValue == null) {
			return null;
		}

		try {
			String normalizedDateTimeValue = normalizedValue.replace('T', ' ');
			if (normalizedDateTimeValue.length() == 10) {
				LocalDate localDate = LocalDate.parse(normalizedDateTimeValue, ADMIN_COMPANY_WORK_DATE_FORMATTER);
				return localDate.atStartOfDay().format(ADMIN_COMPANY_WORK_DATE_TIME_FORMATTER);
			}
			if (normalizedDateTimeValue.length() == 16) {
				LocalDateTime localDateTime = LocalDateTime.parse(normalizedDateTimeValue, ADMIN_COMPANY_WORK_DATE_TIME_MINUTE_FORMATTER);
				return localDateTime.format(ADMIN_COMPANY_WORK_DATE_TIME_FORMATTER);
			}
			LocalDateTime localDateTime = LocalDateTime.parse(normalizedDateTimeValue, ADMIN_COMPANY_WORK_DATE_TIME_FORMATTER);
			return localDateTime.format(ADMIN_COMPANY_WORK_DATE_TIME_FORMATTER);
		} catch (DateTimeParseException exception) {
			throw new IllegalArgumentException(invalidMessage);
		}
	}

	// 업무 공수시간 값을 저장 가능한 정수로 정규화합니다.
	private Integer normalizeOptionalWorkTime(Integer workTime) {
		// 빈 값은 null 저장을 허용하고 음수만 차단합니다.
		if (workTime == null) {
			return null;
		}
		if (workTime < 0) {
			throw new IllegalArgumentException("업무 공수시간을 확인해주세요.");
		}
		return workTime;
	}

	// 본문 포함 검색 여부를 Y/N 값으로 정규화합니다.
	private String normalizeIncludeBodyYn(String includeBodyYn) {
		String normalizedIncludeBodyYn = trimToNull(includeBodyYn);
		if (normalizedIncludeBodyYn == null) {
			return "N";
		}
		if ("Y".equalsIgnoreCase(normalizedIncludeBodyYn)) {
			return "Y";
		}
		if ("N".equalsIgnoreCase(normalizedIncludeBodyYn)) {
			return "N";
		}
		throw new IllegalArgumentException("본문 포함 검색 여부를 확인해주세요.");
	}

	// 업무 상태 다중 선택 목록을 공통코드 기준으로 정규화합니다.
	private List<String> normalizeWorkStatusCodeFilter(List<String> workStatCdList) {
		LinkedHashSet<String> normalizedWorkStatusCodeSet = new LinkedHashSet<>();
		for (String workStatCd : workStatCdList == null ? List.<String>of() : workStatCdList) {
			String normalizedWorkStatusCode = trimToNull(workStatCd);
			if (normalizedWorkStatusCode == null) {
				continue;
			}
			validateWorkStatusCode(normalizedWorkStatusCode);
			normalizedWorkStatusCodeSet.add(normalizedWorkStatusCode);
		}
		return new ArrayList<>(normalizedWorkStatusCodeSet);
	}

	// 업무 본문 HTML 문자열을 저장 가능한 값으로 정규화합니다.
	private String normalizeOptionalCompanyWorkContent(String content) {
		// 본문이 비어 있거나 의미 있는 텍스트/이미지가 없으면 빈 문자열로 저장합니다.
		String normalizedContent = safeValue(content).trim();
		if (normalizedContent.isEmpty()) {
			return "";
		}

		String visibleText = normalizedContent
			.replaceAll("(?i)<img\\b[^>]*>", " IMG ")
			.replace("&nbsp;", " ")
			.replaceAll("<[^>]*>", " ")
			.replaceAll("\\s+", " ")
			.trim();
		if (visibleText.isEmpty()) {
			return "";
		}
		return normalizedContent;
	}

	// 업무 담당자명을 저장 가능한 값으로 정규화합니다.
	private String normalizeOptionalManagerName(String managerName, String invalidMessage) {
		// 빈 값은 빈 문자열로 저장하고, 길이 초과는 요청 오류로 처리합니다.
		String normalizedManagerName = trimToNull(managerName);
		if (normalizedManagerName == null) {
			return "";
		}
		if (normalizedManagerName.length() > ADMIN_COMPANY_WORK_MAX_MANAGER_NAME_LENGTH) {
			throw new IllegalArgumentException(invalidMessage);
		}
		return normalizedManagerName;
	}

	// 댓글 HTML 문자열을 저장 가능한 값으로 정규화합니다.
	private String normalizeOptionalReplyComment(String replyComment) {
		// Quill 빈 본문을 포함한 공백 댓글은 null로 정리합니다.
		String normalizedReplyComment = trimToNull(replyComment);
		if (normalizedReplyComment == null) {
			return null;
		}

		String visibleText = normalizedReplyComment
			.replace("&nbsp;", " ")
			.replaceAll("<[^>]*>", " ")
			.replaceAll("\\s+", " ")
			.trim();
		if (visibleText.isEmpty()) {
			return null;
		}
		return limitLength(normalizedReplyComment, ADMIN_COMPANY_WORK_MAX_REPLY_LENGTH);
	}

	// 멀티파트 파일에서 원본 파일명을 추출하고 저장 가능한 값으로 정규화합니다.
	private String extractOriginalFileName(MultipartFile file) {
		// 브라우저별 경로 포함 파일명은 마지막 파일명만 남기고 검증합니다.
		String originalFileName = trimToNull(file == null ? null : file.getOriginalFilename());
		if (originalFileName == null) {
			throw new IllegalArgumentException("댓글 첨부파일명을 확인해주세요.");
		}

		String normalizedFileName = originalFileName
			.replace("\\", "/");
		int lastSlashIndex = normalizedFileName.lastIndexOf('/');
		String baseFileName = lastSlashIndex >= 0 ? normalizedFileName.substring(lastSlashIndex + 1) : normalizedFileName;
		if (trimToNull(baseFileName) == null) {
			throw new IllegalArgumentException("댓글 첨부파일명을 확인해주세요.");
		}
		return baseFileName;
	}

	// 파일명에서 확장자를 추출합니다.
	private String extractFileExtension(String fileName) {
		// 마지막 점 뒤 문자열만 확장자로 해석합니다.
		String normalizedFileName = trimToNull(fileName);
		if (normalizedFileName == null) {
			return null;
		}

		int extensionIndex = normalizedFileName.lastIndexOf('.');
		if (extensionIndex < 0 || extensionIndex == normalizedFileName.length() - 1) {
			return null;
		}
		return normalizedFileName.substring(extensionIndex + 1).toLowerCase();
	}

	// 허용 확장자 목록에 현재 파일 확장자가 포함되는지 확인합니다.
	private boolean isAllowedFileExtension(String allowedExtensions, String extension) {
		// 콤마 기준으로 분리한 뒤 정확히 일치하는 확장자만 허용합니다.
		String normalizedExtension = trimToNull(extension);
		if (normalizedExtension == null) {
			return false;
		}

		for (String allowedExtensionItem : safeValue(allowedExtensions).split(",")) {
			String normalizedAllowedExtension = trimToNull(allowedExtensionItem);
			if (normalizedAllowedExtension != null && normalizedExtension.equalsIgnoreCase(normalizedAllowedExtension)) {
				return true;
			}
		}
		return false;
	}

	// Jira ADF 본문을 여러 줄 plain text로 변환합니다.
	private String extractJiraDescriptionText(JsonNode descriptionNode) {
		// 본문이 없으면 빈 문자열을 반환합니다.
		if (descriptionNode == null || descriptionNode.isMissingNode() || descriptionNode.isNull()) {
			return "";
		}

		StringBuilder contentBuilder = new StringBuilder();
		appendJiraDescriptionNode(descriptionNode, contentBuilder);
		return normalizeJiraDescriptionText(contentBuilder.toString());
	}

	// Jira ADF 노드를 순회하며 plain text를 수집합니다.
	private void appendJiraDescriptionNode(JsonNode node, StringBuilder collector) {
		// null 노드는 무시합니다.
		if (node == null || node.isNull() || node.isMissingNode()) {
			return;
		}

		if (node.isArray()) {
			// 배열 노드는 순서대로 재귀 순회합니다.
			for (JsonNode childNode : node) {
				appendJiraDescriptionNode(childNode, collector);
			}
			return;
		}
		if (!node.isObject()) {
			return;
		}

		// Jira 노드 타입별로 텍스트/줄바꿈 처리 규칙을 적용합니다.
		String nodeType = trimToNull(node.path("type").asText(null));
		if ("text".equals(nodeType)) {
			collector.append(node.path("text").asText(""));
			return;
		}
		if ("hardBreak".equals(nodeType)) {
			appendLineBreakIfNeeded(collector);
			return;
		}
		if ("mention".equals(nodeType)) {
			String mentionText = firstNonNull(
				trimToNull(node.path("attrs").path("text").asText(null)),
				trimToNull(node.path("attrs").path("id").asText(null))
			);
			collector.append(safeValue(mentionText));
			return;
		}
		if ("emoji".equals(nodeType)) {
			String emojiText = firstNonNull(
				trimToNull(node.path("attrs").path("text").asText(null)),
				trimToNull(node.path("attrs").path("shortName").asText(null))
			);
			collector.append(safeValue(emojiText));
			return;
		}
		if ("inlineCard".equals(nodeType)) {
			String cardUrl = trimToNull(node.path("attrs").path("url").asText(null));
			collector.append(safeValue(cardUrl));
			return;
		}

		// 자식 content를 먼저 순회한 뒤 블록 노드라면 줄바꿈을 보강합니다.
		appendJiraDescriptionNode(node.path("content"), collector);
		if (isJiraBlockNode(nodeType)) {
			appendLineBreakIfNeeded(collector);
		}
	}

	// Jira 블록 노드 여부를 반환합니다.
	private boolean isJiraBlockNode(String nodeType) {
		// 문단성 블록은 줄 단위 구분을 유지합니다.
		return "paragraph".equals(nodeType)
			|| "heading".equals(nodeType)
			|| "heading_1".equals(nodeType)
			|| "heading_2".equals(nodeType)
			|| "heading_3".equals(nodeType)
			|| "blockquote".equals(nodeType)
			|| "codeBlock".equals(nodeType)
			|| "panel".equals(nodeType)
			|| "listItem".equals(nodeType)
			|| "tableRow".equals(nodeType)
			|| "rule".equals(nodeType);
	}

	// 텍스트 끝에 줄바꿈이 없으면 한 줄을 추가합니다.
	private void appendLineBreakIfNeeded(StringBuilder collector) {
		// 직전 문자가 줄바꿈이 아니면 줄바꿈을 추가합니다.
		if (collector.length() == 0) {
			return;
		}
		if (collector.charAt(collector.length() - 1) != '\n') {
			collector.append('\n');
		}
	}

	// Jira 본문 텍스트의 공백과 줄바꿈을 정리합니다.
	private String normalizeJiraDescriptionText(String value) {
		// CRLF를 LF로 통일하고 양끝 공백을 제거합니다.
		String normalizedValue = safeValue(value).replace("\r\n", "\n").replace('\r', '\n').trim();
		return normalizedValue;
	}

	// 필수 시퀀스 값을 1 이상 정수로 검증합니다.
	private int normalizeRequiredSequence(Integer sequenceValue, String invalidMessage) {
		// 값이 없거나 0 이하이면 요청 오류로 처리합니다.
		if (sequenceValue == null || sequenceValue < 1) {
			throw new IllegalArgumentException(invalidMessage);
		}
		return sequenceValue;
	}

	// 필수 업무 시퀀스를 1 이상 long 값으로 검증합니다.
	private long normalizeRequiredWorkSequence(Long workSeq, String invalidMessage) {
		// 값이 없거나 0 이하이면 요청 오류로 처리합니다.
		if (workSeq == null || workSeq < 1) {
			throw new IllegalArgumentException(invalidMessage);
		}
		return workSeq;
	}

	// 필수 사용자 번호를 1 이상 long 값으로 검증합니다.
	private long normalizeRequiredUserNo(Long userNo, String invalidMessage) {
		// 값이 없거나 0 이하이면 요청 오류로 처리합니다.
		if (userNo == null || userNo < 1) {
			throw new IllegalArgumentException(invalidMessage);
		}
		return userNo;
	}

	// 페이지 번호를 1 이상으로 보정합니다.
	private int normalizePage(Integer page) {
		// 유효하지 않은 페이지는 기본 페이지로 보정합니다.
		if (page == null || page < 1) {
			return ADMIN_COMPANY_WORK_DEFAULT_PAGE;
		}
		return page;
	}

	// 페이지 크기를 허용 범위로 보정합니다.
	private int normalizePageSize(Integer pageSize) {
		// 유효하지 않은 페이지 크기는 기본 크기로 보정합니다.
		if (pageSize == null || pageSize < 1) {
			return ADMIN_COMPANY_WORK_DEFAULT_PAGE_SIZE;
		}
		return Math.min(pageSize, ADMIN_COMPANY_WORK_MAX_PAGE_SIZE);
	}

	// 추가 조회 오프셋을 0 이상으로 보정합니다.
	private int normalizeNonNegativeOffset(Integer offset) {
		// 유효하지 않은 오프셋은 0으로 보정합니다.
		if (offset == null || offset < 0) {
			return 0;
		}
		return offset;
	}

	// 상태 섹션 목록을 지정한 최대 개수만큼 잘라 반환합니다.
	private List<AdminCompanyWorkListRowVO> sliceStatusSectionRowList(List<AdminCompanyWorkListRowVO> rowList, int sectionSize) {
		// 최대 개수까지만 노출하고 나머지는 더보기로 이어지게 처리합니다.
		List<AdminCompanyWorkListRowVO> safeRowList = rowList == null ? List.of() : rowList;
		if (safeRowList.size() <= sectionSize) {
			return safeRowList;
		}
		return new ArrayList<>(safeRowList.subList(0, sectionSize));
	}

	// 문자열을 trim 처리하고 빈 값은 null로 변환합니다.
	private String trimToNull(String value) {
		// null 문자열은 그대로 null로 반환합니다.
		if (value == null) {
			return null;
		}
		String trimmedValue = value.trim();
		return trimmedValue.isEmpty() ? null : trimmedValue;
	}

	// 앞에서부터 null이 아닌 문자열을 반환합니다.
	private String firstNonNull(String... values) {
		// 전달된 문자열 중 첫 번째 유효값을 반환합니다.
		for (String value : values) {
			String normalizedValue = trimToNull(value);
			if (normalizedValue != null) {
				return normalizedValue;
			}
		}
		return null;
	}

	// null 문자열을 빈 문자열로 변환합니다.
	private String safeValue(String value) {
		return value == null ? "" : value;
	}

	// 문자열을 지정한 길이 이하로 제한합니다.
	private String limitLength(String value, int maxLength) {
		// 최대 길이를 초과하면 좌측부터 허용 길이만큼만 잘라 반환합니다.
		String normalizedValue = safeValue(value);
		if (normalizedValue.length() <= maxLength) {
			return normalizedValue;
		}
		return normalizedValue.substring(0, maxLength);
	}
}
