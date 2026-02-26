package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.admin.notion.AdminNotionCategorySortRowPO;
import com.xodud1202.springbackend.domain.admin.notion.AdminNotionCategorySortSavePO;
import com.xodud1202.springbackend.domain.admin.notion.AdminNotionListQueryPO;
import com.xodud1202.springbackend.mapper.AdminNotionMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// 관리자 Notion 저장 목록/카테고리 정렬 서비스 로직을 검증합니다.
class AdminNotionServiceTests {
	@Mock
	private AdminNotionMapper adminNotionMapper;

	@InjectMocks
	private AdminNotionService adminNotionService;

	@Test
	@DisplayName("목록 조회 검증: 시작일이 종료일보다 크면 오류를 반환한다")
	// 잘못된 기간 입력을 검증합니다.
	void validateAdminNotionSaveListQuery_returnsErrorWhenStartIsAfterEnd() {
		AdminNotionListQueryPO param = new AdminNotionListQueryPO();
		param.setCreateDtStart("2026-03-01");
		param.setCreateDtEnd("2026-02-28");

		String result = adminNotionService.validateAdminNotionSaveListQuery(param);

		assertEquals("등록일시 기간을 확인해주세요.", result);
	}

	@Test
	@DisplayName("목록 조회 검증: 정상 기간 입력이면 null을 반환한다")
	// 정상 기간 입력값을 검증합니다.
	void validateAdminNotionSaveListQuery_returnsNullWhenDateRangeValid() {
		AdminNotionListQueryPO param = new AdminNotionListQueryPO();
		param.setCreateDtStart("2026-02-01");
		param.setCreateDtEnd("2026-02-28");

		String result = adminNotionService.validateAdminNotionSaveListQuery(param);

		assertNull(result);
	}

	@Test
	@DisplayName("목록 조회: page 미지정 시 기본 페이지와 페이지사이즈를 적용한다")
	// 페이징 기본값 적용 여부를 확인합니다.
	void getAdminNotionSaveList_appliesDefaultPagingWhenPageMissing() {
		AdminNotionListQueryPO param = new AdminNotionListQueryPO();
		when(adminNotionMapper.getAdminNotionSaveList(any())).thenReturn(List.of());
		when(adminNotionMapper.getAdminNotionSaveCount(any())).thenReturn(0);

		Map<String, Object> result = adminNotionService.getAdminNotionSaveList(param);

		assertEquals(1, result.get("page"));
		assertEquals(20, result.get("pageSize"));
		verify(adminNotionMapper).getAdminNotionSaveList(any(AdminNotionListQueryPO.class));
		verify(adminNotionMapper).getAdminNotionSaveCount(any(AdminNotionListQueryPO.class));
	}

	@Test
	@DisplayName("카테고리 순서 저장: 중복 categoryId는 1회만 반영하고 순서대로 sortSeq를 갱신한다")
	// 중복 제거 후 정렬 순서 저장을 검증합니다.
	void saveAdminNotionCategorySort_updatesSortSeqWithDistinctCategoryOrder() {
		AdminNotionCategorySortRowPO row1 = new AdminNotionCategorySortRowPO();
		row1.setCategoryId("CAT-A");
		AdminNotionCategorySortRowPO row2 = new AdminNotionCategorySortRowPO();
		row2.setCategoryId("CAT-B");
		AdminNotionCategorySortRowPO row3 = new AdminNotionCategorySortRowPO();
		row3.setCategoryId("CAT-A");

		AdminNotionCategorySortSavePO param = new AdminNotionCategorySortSavePO();
		param.setRows(List.of(row1, row2, row3));
		param.setUdtNo(100L);

		when(adminNotionMapper.updateAdminNotionCategorySortSeq(eq("CAT-A"), eq(1), eq(100L))).thenReturn(1);
		when(adminNotionMapper.updateAdminNotionCategorySortSeq(eq("CAT-B"), eq(2), eq(100L))).thenReturn(1);

		int affected = adminNotionService.saveAdminNotionCategorySort(param);

		assertEquals(2, affected);
		verify(adminNotionMapper).updateAdminNotionCategorySortSeq("CAT-A", 1, 100L);
		verify(adminNotionMapper).updateAdminNotionCategorySortSeq("CAT-B", 2, 100L);
		verify(adminNotionMapper, never()).updateAdminNotionCategorySortSeq("CAT-A", 3, 100L);
	}

	@Test
	@DisplayName("카테고리 순서 저장 검증: 수정자 번호가 없으면 오류를 반환한다")
	// 카테고리 순서 저장 필수값 검증을 확인합니다.
	void validateAdminNotionCategorySortSave_returnsErrorWhenUdtNoMissing() {
		AdminNotionCategorySortRowPO row = new AdminNotionCategorySortRowPO();
		row.setCategoryId("CAT-A");
		AdminNotionCategorySortSavePO param = new AdminNotionCategorySortSavePO();
		param.setRows(List.of(row));

		String result = adminNotionService.validateAdminNotionCategorySortSave(param);

		assertEquals("수정자 정보를 확인해주세요.", result);
	}
}
