package com.xodud1202.springbackend.mapper;

import com.xodud1202.springbackend.domain.work.vacation.WorkVacationCompanyVO;
import com.xodud1202.springbackend.domain.work.vacation.WorkVacationCreatePO;
import com.xodud1202.springbackend.domain.work.vacation.WorkVacationListRowVO;
import com.xodud1202.springbackend.domain.work.vacation.WorkVacationListSearchPO;
import com.xodud1202.springbackend.domain.work.vacation.WorkVacationPersonVO;
import com.xodud1202.springbackend.domain.work.vacation.WorkVacationSummaryRowVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
// 휴가관리 도메인 매퍼를 정의합니다.
public interface VacationMapper {
	// 휴가 사용 가능 회사 목록을 조회합니다.
	List<WorkVacationCompanyVO> getVacationCompanyList();

	// 휴가자 목록을 조회합니다.
	List<WorkVacationPersonVO> getVacationPersonList();

	// 휴가자 존재 여부를 조회합니다.
	int countActiveVacationPerson(@Param("personSeq") Integer personSeq);

	// 휴가 사용 가능 회사 존재 여부를 조회합니다.
	int countVacationCompany(@Param("workCompanySeq") Integer workCompanySeq);

	// 휴가가 존재하는 회사 중 기본 선택할 회사 번호를 조회합니다.
	Integer getDefaultVacationCompanySeq(WorkVacationListSearchPO param);

	// 선택 조건에 해당하는 휴가년도 목록을 조회합니다.
	List<Integer> getVacationYearList(WorkVacationListSearchPO param);

	// 휴가자별 회사별 연차 사용 요약 목록을 조회합니다.
	List<WorkVacationSummaryRowVO> getVacationSummaryList(WorkVacationListSearchPO param);

	// 휴가 사용 상세 목록을 조회합니다.
	List<WorkVacationListRowVO> getVacationList(WorkVacationListSearchPO param);

	// 휴가 사용 정보를 저장합니다.
	int insertVacation(WorkVacationCreatePO param);
}
