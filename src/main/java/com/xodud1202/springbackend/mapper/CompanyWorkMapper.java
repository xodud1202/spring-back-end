package com.xodud1202.springbackend.mapper;

import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkImportCompanyInfoVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkImportFileSavePO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkImportJobSavePO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkCompanyVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkListRowVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkProjectVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkSearchPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
// 관리자 회사 업무 도메인 매퍼를 정의합니다.
public interface CompanyWorkMapper {
	// 관리자 회사 업무 회사 목록을 조회합니다.
	List<AdminCompanyWorkCompanyVO> getAdminCompanyWorkCompanyList();

	// 관리자 회사 업무 프로젝트 목록을 조회합니다.
	List<AdminCompanyWorkProjectVO> getAdminCompanyWorkProjectList(@Param("workCompanySeq") Integer workCompanySeq);

	// 관리자 회사 업무 비완료 상태 목록을 조회합니다.
	List<AdminCompanyWorkListRowVO> getAdminCompanyWorkStatusList(AdminCompanyWorkSearchPO param);

	// 관리자 회사 업무 완료 목록을 조회합니다.
	List<AdminCompanyWorkListRowVO> getAdminCompanyWorkCompletedList(AdminCompanyWorkSearchPO param);

	// 관리자 회사 업무 완료 목록 건수를 조회합니다.
	int getAdminCompanyWorkCompletedCount(AdminCompanyWorkSearchPO param);

	// 관리자 회사 업무 가져오기 대상 회사 정보를 조회합니다.
	AdminCompanyWorkImportCompanyInfoVO getAdminCompanyWorkImportCompanyInfo(@Param("workCompanySeq") Integer workCompanySeq);

	// 관리자 회사 업무 프로젝트 매칭 여부를 조회합니다.
	int countAdminCompanyWorkProjectMatch(
		@Param("workCompanySeq") Integer workCompanySeq,
		@Param("workCompanyProjectSeq") Integer workCompanyProjectSeq
	);

	// 관리자 회사 업무키 중복 건수를 조회합니다.
	int countAdminCompanyWorkDuplicateKey(
		@Param("workCompanySeq") Integer workCompanySeq,
		@Param("workCompanyProjectSeq") Integer workCompanyProjectSeq,
		@Param("workKey") String workKey
	);

	// 관리자 회사 업무 기본정보를 저장합니다.
	int insertAdminCompanyWorkImportJob(AdminCompanyWorkImportJobSavePO param);

	// 관리자 회사 업무 첨부파일 정보를 저장합니다.
	int insertAdminCompanyWorkImportFile(AdminCompanyWorkImportFileSavePO param);
}
