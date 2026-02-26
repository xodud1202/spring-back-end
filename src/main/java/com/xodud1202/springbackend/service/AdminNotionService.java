package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.admin.notion.AdminNotionCategorySortRowPO;
import com.xodud1202.springbackend.domain.admin.notion.AdminNotionCategorySortSavePO;
import com.xodud1202.springbackend.domain.admin.notion.AdminNotionCategoryVO;
import com.xodud1202.springbackend.domain.admin.notion.AdminNotionListQueryPO;
import com.xodud1202.springbackend.domain.admin.notion.AdminNotionListRowVO;
import com.xodud1202.springbackend.mapper.AdminNotionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
// 관리자 Notion 저장 목록 조회/카테고리 정렬 관리 비즈니스 로직을 처리합니다.
public class AdminNotionService {
	private static final int PAGE_SIZE = 20;
	private static final int CATEGORY_ID_MAX_LENGTH = 72;

	private final AdminNotionMapper adminNotionMapper;

	// 관리자 Notion 저장 목록 조회 조건을 검증합니다.
	public String validateAdminNotionSaveListQuery(AdminNotionListQueryPO param) {
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}

		String startDateText = trimToNull(param.getCreateDtStart());
		String endDateText = trimToNull(param.getCreateDtEnd());
		LocalDate startDate = parseDate(startDateText);
		if (startDateText != null && startDate == null) {
			return "등록일시 시작일 형식을 확인해주세요.";
		}

		LocalDate endDate = parseDate(endDateText);
		if (endDateText != null && endDate == null) {
			return "등록일시 종료일 형식을 확인해주세요.";
		}

		if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
			return "등록일시 기간을 확인해주세요.";
		}
		return null;
	}

	// 관리자 Notion 저장 목록을 페이징 조회합니다.
	public Map<String, Object> getAdminNotionSaveList(AdminNotionListQueryPO param) {
		int page = param.getPage() == null || param.getPage() < 1 ? 1 : param.getPage();
		int offset = (page - 1) * PAGE_SIZE;

		param.setCategoryId(trimToNull(param.getCategoryId()));
		param.setTitle(trimToNull(param.getTitle()));
		param.setCreateDtStart(trimToNull(param.getCreateDtStart()));
		param.setCreateDtEnd(trimToNull(param.getCreateDtEnd()));
		param.setPage(page);
		param.setPageSize(PAGE_SIZE);
		param.setOffset(offset);

		List<AdminNotionListRowVO> list = adminNotionMapper.getAdminNotionSaveList(param);
		int totalCount = adminNotionMapper.getAdminNotionSaveCount(param);

		Map<String, Object> result = new HashMap<>();
		result.put("list", list);
		result.put("totalCount", totalCount);
		result.put("page", page);
		result.put("pageSize", PAGE_SIZE);
		return result;
	}

	// 관리자 Notion 카테고리 옵션 목록을 조회합니다.
	public List<AdminNotionCategoryVO> getAdminNotionCategoryList() {
		return adminNotionMapper.getAdminNotionCategoryList();
	}

	// 관리자 Notion 카테고리 순서 저장 요청을 검증합니다.
	public String validateAdminNotionCategorySortSave(AdminNotionCategorySortSavePO param) {
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}
		if (param.getUdtNo() == null) {
			return "수정자 정보를 확인해주세요.";
		}
		if (param.getRows() == null || param.getRows().isEmpty()) {
			return "저장할 카테고리 데이터가 없습니다.";
		}

		boolean hasValidCategoryId = false;
		for (AdminNotionCategorySortRowPO row : param.getRows()) {
			if (row == null) {
				return "카테고리 데이터 형식이 올바르지 않습니다.";
			}
			String categoryId = trimToNull(row.getCategoryId());
			if (categoryId == null) {
				continue;
			}
			hasValidCategoryId = true;
			if (categoryId.length() > CATEGORY_ID_MAX_LENGTH) {
				return "카테고리 ID 길이를 확인해주세요.";
			}
		}

		if (!hasValidCategoryId) {
			return "저장할 카테고리 ID가 없습니다.";
		}
		return null;
	}

	@Transactional
	// 관리자 Notion 카테고리 정렬 순서를 저장합니다.
	public int saveAdminNotionCategorySort(AdminNotionCategorySortSavePO param) {
		List<String> categoryIdList = resolveOrderedDistinctCategoryIds(param.getRows());
		int sortSeq = 1;
		int affectedCount = 0;
		for (String categoryId : categoryIdList) {
			affectedCount += adminNotionMapper.updateAdminNotionCategorySortSeq(categoryId, sortSeq, param.getUdtNo());
			sortSeq += 1;
		}
		return affectedCount;
	}

	// 입력 카테고리 목록에서 순서를 유지하며 중복을 제거합니다.
	private List<String> resolveOrderedDistinctCategoryIds(List<AdminNotionCategorySortRowPO> rows) {
		Set<String> categoryIdSet = new LinkedHashSet<>();
		for (AdminNotionCategorySortRowPO row : rows) {
			if (row == null) {
				continue;
			}
			String categoryId = trimToNull(row.getCategoryId());
			if (categoryId == null) {
				continue;
			}
			categoryIdSet.add(categoryId);
		}
		return new ArrayList<>(categoryIdSet);
	}

	// 문자열을 trim 처리하고 빈값은 null로 변환합니다.
	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	// yyyy-MM-dd 형식 문자열을 LocalDate로 파싱합니다.
	private LocalDate parseDate(String dateText) {
		if (dateText == null) {
			return null;
		}
		try {
			return LocalDate.parse(dateText);
		} catch (Exception ignored) {
			return null;
		}
	}
}
