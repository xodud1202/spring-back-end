package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.work.vacation.WorkVacationCompanyVO;
import com.xodud1202.springbackend.domain.work.vacation.WorkVacationListResponseVO;
import com.xodud1202.springbackend.domain.work.vacation.WorkVacationListSearchPO;
import com.xodud1202.springbackend.mapper.CommonMapper;
import com.xodud1202.springbackend.mapper.VacationMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
// VacationService의 휴가 목록 선택 조건 정규화 로직을 검증합니다.
class VacationServiceTests {

	// 휴가관리 매퍼 목 객체입니다.
	@Mock
	private VacationMapper vacationMapper;

	// 공통코드 매퍼 목 객체입니다.
	@Mock
	private CommonMapper commonMapper;

	// 테스트 대상 서비스입니다.
	@InjectMocks
	private VacationService vacationService;

	@Test
	@DisplayName("목록 조회: 회사 필터 후보는 선택 회사가 아니라 휴가자 기준으로 조회한다")
	// 선택 회사가 있어도 라디오 후보 목록은 해당 휴가자의 전체 등록 회사 기준을 유지합니다.
	void getWorkVacationList_usesPersonOnlyForCompanyFilterList() {
		// 선택 회사와 별개인 회사 필터 후보를 목으로 구성합니다.
		WorkVacationCompanyVO company = new WorkVacationCompanyVO();
		company.setWorkCompanySeq(1);
		company.setWorkCompanyNm("다이닝브랜즈그룹");
		when(vacationMapper.getVacationFilterCompanyList(any(WorkVacationListSearchPO.class))).thenReturn(List.of(company));
		when(vacationMapper.getVacationYearList(any(WorkVacationListSearchPO.class))).thenReturn(List.of(2026));
		when(vacationMapper.getVacationSummaryList(any(WorkVacationListSearchPO.class))).thenReturn(List.of());
		when(vacationMapper.getVacationList(any(WorkVacationListSearchPO.class))).thenReturn(List.of());

		// 휴가자와 선택 회사를 함께 전달해 목록을 조회합니다.
		WorkVacationListResponseVO response = vacationService.getWorkVacationList(2, 1, null, null);

		// 회사 후보 조회는 선택 회사 없이 휴가자 기준으로만 수행되는지 검증합니다.
		ArgumentCaptor<WorkVacationListSearchPO> filterCaptor = ArgumentCaptor.forClass(WorkVacationListSearchPO.class);
		verify(vacationMapper).getVacationFilterCompanyList(filterCaptor.capture());
		WorkVacationListSearchPO filterParam = filterCaptor.getValue();
		assertEquals(2, filterParam.getPersonSeq());
		assertNull(filterParam.getWorkCompanySeq());
		assertNull(filterParam.getVacationYear());
		assertEquals(1, response.getCompanyList().size());
		assertEquals(1, response.getCompanyList().get(0).getWorkCompanySeq());

		// 실제 목록 조회 조건은 요청한 선택 회사를 유지하는지 검증합니다.
		ArgumentCaptor<WorkVacationListSearchPO> listCaptor = ArgumentCaptor.forClass(WorkVacationListSearchPO.class);
		verify(vacationMapper).getVacationSummaryList(listCaptor.capture());
		assertEquals(1, listCaptor.getValue().getWorkCompanySeq());
	}
}
