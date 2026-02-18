package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.admin.news.AdminNewsCategorySavePO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsCategorySaveRowPO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsPressSavePO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsPressSaveRowPO;
import com.xodud1202.springbackend.mapper.NewsMapper;
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
// 뉴스 RSS 관리 검증/저장 로직을 확인하는 테스트입니다.
class NewsServiceRssManageTests {
	@Mock
	private NewsMapper newsMapper;

	@Mock
	private RssFeedClient rssFeedClient;

	@InjectMocks
	private NewsService newsService;

	@Test
	@DisplayName("언론사 저장 검증에서 언론사명이 비어 있으면 오류를 반환한다")
	// 언론사명 필수 입력 검증을 확인합니다.
	void validateAdminNewsPressSave_returnsErrorWhenPressNmMissing() {
		AdminNewsPressSaveRowPO row = new AdminNewsPressSaveRowPO();
		row.setPressNm(" ");
		row.setUseYn("Y");
		AdminNewsPressSavePO param = new AdminNewsPressSavePO();
		param.setRows(List.of(row));
		param.setRegNo(1L);
		param.setUdtNo(1L);

		String result = newsService.validateAdminNewsPressSave(param);

		assertEquals("언론사명을 입력해 주세요.", result);
	}

	@Test
	@DisplayName("카테고리 저장 검증에서 카테고리 코드가 비어 있으면 오류를 반환한다")
	// 카테고리 코드 필수 입력 검증을 확인합니다.
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

		String result = newsService.validateAdminNewsCategorySave(param);

		assertEquals("카테고리코드를 입력해 주세요.", result);
	}

	@Test
	@DisplayName("신규 언론사 저장 시 자동 코드로 등록된다")
	// 신규 언론사 저장 처리 흐름을 확인합니다.
	void saveAdminNewsPress_insertsNewPressWithAutoCode() {
		AdminNewsPressSaveRowPO row = new AdminNewsPressSaveRowPO();
		row.setPressNm("테스트 언론사");
		row.setUseYn("Y");
		AdminNewsPressSavePO param = new AdminNewsPressSavePO();
		param.setRows(List.of(row));
		param.setRegNo(1L);
		param.setUdtNo(1L);

		when(newsMapper.insertAdminNewsPress(any())).thenAnswer(invocation -> {
			AdminNewsPressSaveRowPO saveRow = invocation.getArgument(0);
			saveRow.setPressNo(100L);
			return 1;
		});
		when(newsMapper.updateAdminNewsPressSortSeq(any(), any(), any())).thenReturn(1);

		int result = newsService.saveAdminNewsPress(param);

		assertEquals(2, result);
		assertEquals(100L, row.getPressNo());
		verify(newsMapper).insertAdminNewsPress(any());
		verify(newsMapper).updateAdminNewsPressSortSeq(100L, 1, 1L);
	}

	@Test
	@DisplayName("언론사 저장 검증이 정상 입력이면 null을 반환한다")
	// 정상 입력 시 오류가 없는지 확인합니다.
	void validateAdminNewsPressSave_returnsNullWhenValid() {
		AdminNewsPressSaveRowPO row = new AdminNewsPressSaveRowPO();
		row.setPressNm("JTBC");
		row.setUseYn("Y");
		AdminNewsPressSavePO param = new AdminNewsPressSavePO();
		param.setRows(List.of(row));
		param.setRegNo(1L);
		param.setUdtNo(1L);

		String result = newsService.validateAdminNewsPressSave(param);

		assertNull(result);
	}
}
