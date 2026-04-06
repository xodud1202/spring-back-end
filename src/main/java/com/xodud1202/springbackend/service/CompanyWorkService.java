package com.xodud1202.springbackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkImportCompanyInfoVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkImportFileSavePO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkImportJobSavePO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkImportPO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkImportResponseVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkCompanyVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkCompletedListResponseVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkListRowVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkProjectVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkSearchPO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkStatusListResponseVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkStatusSectionVO;
import com.xodud1202.springbackend.domain.common.CommonCodeVO;
import com.xodud1202.springbackend.mapper.CommonMapper;
import com.xodud1202.springbackend.mapper.CompanyWorkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
// 관리자 회사 업무 조회 비즈니스 로직을 처리합니다.
public class CompanyWorkService {
	private static final int ADMIN_COMPANY_WORK_DEFAULT_PAGE = 1;
	private static final int ADMIN_COMPANY_WORK_DEFAULT_PAGE_SIZE = 20;
	private static final int ADMIN_COMPANY_WORK_MAX_PAGE_SIZE = 200;
	private static final int ADMIN_COMPANY_WORK_MAX_WORK_KEY_LENGTH = 50;
	private static final int ADMIN_COMPANY_WORK_MAX_TITLE_LENGTH = 255;
	private static final int ADMIN_COMPANY_WORK_MAX_MANAGER_NAME_LENGTH = 50;
	private static final int ADMIN_COMPANY_WORK_MAX_FILE_NAME_LENGTH = 255;
	private static final int ADMIN_COMPANY_WORK_MAX_FILE_URL_LENGTH = 255;
	private static final String WORK_STATUS_GROUP_CODE = "WORK_STAT";
	private static final String WORK_COMPLETED_STATUS_CODE = "WORK_STAT_05";
	private static final String WORK_WAIT_STATUS_CODE = "WORK_STAT_01";
	private static final String WORK_PRIOR_HIGH_CODE = "WORK_PRIOR_01";
	private static final String WORK_PRIOR_NORMAL_CODE = "WORK_PRIOR_02";
	private static final String WORK_PRIOR_LOW_CODE = "WORK_PRIOR_03";
	private static final String DINING_BRANDS_GROUP_COMPANY_NAME = "다이닝 브랜즈 그룹";
	private static final String JIRA_PLATFORM_NAME = "JIRA";
	private static final DateTimeFormatter ADMIN_COMPANY_WORK_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final DateTimeFormatter JIRA_OFFSET_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	private static final DateTimeFormatter JIRA_OFFSET_DATE_TIME_WITHOUT_MILLIS_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

	private final CompanyWorkMapper companyWorkMapper;
	private final CommonMapper commonMapper;
	private final DiningBrandsGroupJiraApiClient diningBrandsGroupJiraApiClient;

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
			statusSection.setList(rowListMapByStatusCode.getOrDefault(workStatusCode, List.of()));
			statusSectionList.add(statusSection);
		}

		// 상태별 목록 응답 객체를 반환합니다.
		AdminCompanyWorkStatusListResponseVO response = new AdminCompanyWorkStatusListResponseVO();
		response.setStatusSectionList(statusSectionList);
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

	// 관리자 회사 업무 공통 조회 파라미터를 생성합니다.
	private AdminCompanyWorkSearchPO createSearchParam(
		Integer workCompanySeq,
		Integer workCompanyProjectSeq,
		String title,
		Integer page,
		Integer pageSize
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
		return param;
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

	// 현재 구현이 지원하는 회사/플랫폼인지 확인합니다.
	private void validateSupportedCompanyPlatform(AdminCompanyWorkImportCompanyInfoVO companyInfo) {
		// 다이닝브랜즈그룹 Jira만 지원하고 나머지는 차단합니다.
		String companyName = trimToNull(companyInfo == null ? null : companyInfo.getWorkCompanyNm());
		String platformName = trimToNull(companyInfo == null ? null : companyInfo.getWorkPlatformNm());
		if (!DINING_BRANDS_GROUP_COMPANY_NAME.equals(companyName) || platformName == null || !JIRA_PLATFORM_NAME.equalsIgnoreCase(platformName)) {
			throw new IllegalArgumentException("아직 지원하지 않는 회사 업무 플랫폼입니다.");
		}
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
