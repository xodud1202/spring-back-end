package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.admin.news.AdminNewsCategorySavePO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsCategorySaveRowPO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsPressSavePO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsPressSaveRowPO;
import com.xodud1202.springbackend.mapper.AdminNewsRssManageMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// AdminNewsRssManageService의 입력 검증/저장 로직을 검증합니다.
class AdminNewsRssManageServiceTests {
	@Mock
	private AdminNewsRssManageMapper adminNewsRssManageMapper;

	@InjectMocks
	private AdminNewsRssManageService adminNewsRssManageService;

	@Test
	@DisplayName("언론사 저장 검증: 언론사명이 비어 있으면 오류를 반환한다")
	// 언론사명 필수값 검증을 수행합니다.
	void validateAdminNewsPressSave_returnsErrorWhenPressNmMissing() {
		AdminNewsPressSaveRowPO row = new AdminNewsPressSaveRowPO();
		row.setPressNm(" ");
		row.setUseYn("Y");
		AdminNewsPressSavePO param = new AdminNewsPressSavePO();
		param.setRows(List.of(row));
		param.setRegNo(1L);
		param.setUdtNo(1L);

		String result = adminNewsRssManageService.validateAdminNewsPressSave(param);

		assertEquals("언론사명을 입력해주세요.", result);
	}

	@Test
	@DisplayName("카테고리 저장 검증: 카테고리코드가 비어 있으면 오류를 반환한다")
	// 카테고리코드 필수값 검증을 수행합니다.
	void validateAdminNewsCategorySave_returnsErrorWhenCategoryCdMissing() {
		AdminNewsCategorySaveRowPO row = new AdminNewsCategorySaveRowPO();
		row.setCategoryCd(" ");
		row.setCategoryNm("정치");
		row.setUseYn("Y");
		row.setSourceNm("정치");
		row.setRssUrl("https://example.com/rss");
		AdminNewsCategorySavePO param = new AdminNewsCategorySavePO();
		param.setPressNo(1L);
		param.setRows(List.of(row));
		param.setRegNo(1L);
		param.setUdtNo(1L);

		String result = adminNewsRssManageService.validateAdminNewsCategorySave(param);

		assertEquals("카테고리코드를 입력해주세요.", result);
	}

	@Test
	@DisplayName("언론사 저장: 신규 언론사 저장 시 자동 코드로 등록된다")
	// 언론사 신규 저장 시 자동 코드 생성과 등록 호출을 검증합니다.
	void saveAdminNewsPress_insertsNewPressWithAutoCode() {
		AdminNewsPressSaveRowPO row = new AdminNewsPressSaveRowPO();
		row.setPressNm("테스트 언론사");
		row.setUseYn("Y");
		AdminNewsPressSavePO param = new AdminNewsPressSavePO();
		param.setRows(List.of(row));
		param.setRegNo(1L);
		param.setUdtNo(1L);

		when(adminNewsRssManageMapper.insertAdminNewsPress(any())).thenAnswer(invocation -> {
			AdminNewsPressSaveRowPO saveRow = invocation.getArgument(0);
			saveRow.setPressNo(100L);
			return 1;
		});
		when(adminNewsRssManageMapper.updateAdminNewsPressSortSeq(any(), any(), any())).thenReturn(1);

		int result = adminNewsRssManageService.saveAdminNewsPress(param);

		assertEquals(2, result);
		assertEquals(100L, row.getPressNo());
		verify(adminNewsRssManageMapper).insertAdminNewsPress(any());
		verify(adminNewsRssManageMapper).updateAdminNewsPressSortSeq(100L, 1, 1L);
	}

	@Test
	@DisplayName("언론사 저장 검증: 정상 데이터면 오류가 없다")
	// 정상 언론사 저장 요청 검증을 수행합니다.
	void validateAdminNewsPressSave_returnsNullWhenValid() {
		AdminNewsPressSaveRowPO row = new AdminNewsPressSaveRowPO();
		row.setPressNm("JTBC");
		row.setUseYn("Y");
		AdminNewsPressSavePO param = new AdminNewsPressSavePO();
		param.setRows(List.of(row));
		param.setRegNo(1L);
		param.setUdtNo(1L);

		String result = adminNewsRssManageService.validateAdminNewsPressSave(param);

		assertNull(result);
	}
}
