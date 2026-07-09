package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.common.CommonCodeVO;
import com.xodud1202.springbackend.domain.work.vacation.WorkVacationCompanyVO;
import com.xodud1202.springbackend.domain.work.vacation.WorkVacationDeletePO;
import com.xodud1202.springbackend.domain.work.vacation.WorkVacationListResponseVO;
import com.xodud1202.springbackend.domain.work.vacation.WorkVacationListSearchPO;
import com.xodud1202.springbackend.domain.work.vacation.WorkVacationUpdatePO;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
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

	@Test
	@DisplayName("수정: 입력값을 정규화하고 휴가 사용 내역을 갱신한다")
	// 수정 요청도 등록과 같은 휴가자, 회사, 휴가구분, 날짜 검증 규칙을 사용합니다.
	void updateWorkVacation_normalizesAndUpdatesVacation() {
		// 저장 가능한 휴가 코드와 유효한 참조 데이터를 목으로 구성합니다.
		CommonCodeVO vacationCode = new CommonCodeVO();
		vacationCode.setCd("VACATION_01");
		when(vacationMapper.countActiveVacationPerson(2)).thenReturn(1);
		when(vacationMapper.countVacationCompany(1)).thenReturn(1);
		when(commonMapper.getCommonCodeList("VACATION")).thenReturn(List.of(vacationCode));
		when(vacationMapper.updateVacation(any(WorkVacationUpdatePO.class))).thenReturn(1);

		// 앞뒤 공백이 포함된 수정 요청을 전달합니다.
		WorkVacationUpdatePO request = new WorkVacationUpdatePO();
		request.setVacationSeq(10L);
		request.setPersonSeq(2);
		request.setWorkCompanySeq(1);
		request.setVacationCd(" VACATION_01 ");
		request.setStartDt("2026-07-10");
		request.setEndDt("2026-07-10");
		request.setVacationMemo(" 개인 일정 ");
		vacationService.updateWorkVacation(request, 5L);

		// 매퍼에는 정규화된 값과 수정자 번호가 전달되는지 검증합니다.
		ArgumentCaptor<WorkVacationUpdatePO> updateCaptor = ArgumentCaptor.forClass(WorkVacationUpdatePO.class);
		verify(vacationMapper).updateVacation(updateCaptor.capture());
		WorkVacationUpdatePO updateParam = updateCaptor.getValue();
		assertEquals(10L, updateParam.getVacationSeq());
		assertEquals(2, updateParam.getPersonSeq());
		assertEquals(1, updateParam.getWorkCompanySeq());
		assertEquals("VACATION_01", updateParam.getVacationCd());
		assertEquals("2026-07-10", updateParam.getStartDt());
		assertEquals("2026-07-10", updateParam.getEndDt());
		assertEquals("개인 일정", updateParam.getVacationMemo());
		assertEquals(5L, updateParam.getUdtNo());
	}

	@Test
	@DisplayName("삭제: 대상 휴가가 없으면 예외를 던진다")
	// 이미 삭제됐거나 없는 휴가 번호는 삭제 성공으로 처리하지 않습니다.
	void deleteWorkVacation_throwsWhenVacationDoesNotExist() {
		// 삭제 매퍼가 영향 행 없음으로 응답하도록 구성합니다.
		WorkVacationDeletePO request = new WorkVacationDeletePO();
		request.setVacationSeq(99L);
		when(vacationMapper.softDeleteVacation(any(WorkVacationDeletePO.class))).thenReturn(0);

		// 삭제 대상 없음 예외와 매퍼 전달 값을 함께 검증합니다.
		IllegalStateException exception = assertThrows(
			IllegalStateException.class,
			() -> vacationService.deleteWorkVacation(request, 5L)
		);
		assertEquals("삭제할 휴가 사용 내역을 찾을 수 없습니다.", exception.getMessage());
		ArgumentCaptor<WorkVacationDeletePO> deleteCaptor = ArgumentCaptor.forClass(WorkVacationDeletePO.class);
		verify(vacationMapper).softDeleteVacation(deleteCaptor.capture());
		assertEquals(99L, deleteCaptor.getValue().getVacationSeq());
		assertEquals(5L, deleteCaptor.getValue().getUdtNo());
	}
}
