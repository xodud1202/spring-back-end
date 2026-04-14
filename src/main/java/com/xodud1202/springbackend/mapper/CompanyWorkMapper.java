package com.xodud1202.springbackend.mapper;

import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkImportCompanyInfoVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkImportFileSavePO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkImportJobSavePO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkManualSavePO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkDetailUpdatePO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkDetailVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkFileVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkReplyFileSavePO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkReplyFileVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkReplyDeletePO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkReplySavePO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkReplyUpdatePO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkReplyVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkUpdatePO;
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

	// 업무관리 화면용 상태 포함 전체 목록을 조회합니다.
	List<AdminCompanyWorkListRowVO> getWorkCompanyList(AdminCompanyWorkSearchPO param);

	// 업무관리 화면 특정 상태의 추가 목록을 조회합니다.
	List<AdminCompanyWorkListRowVO> getWorkCompanySectionList(AdminCompanyWorkSearchPO param);

	// 업무관리 화면 특정 상태의 전체 건수를 조회합니다.
	int getWorkCompanySectionCount(AdminCompanyWorkSearchPO param);

	// 관리자 회사 업무 완료 목록을 조회합니다.
	List<AdminCompanyWorkListRowVO> getAdminCompanyWorkCompletedList(AdminCompanyWorkSearchPO param);

	// 관리자 회사 업무 완료 목록 건수를 조회합니다.
	int getAdminCompanyWorkCompletedCount(AdminCompanyWorkSearchPO param);

	// 관리자 회사 업무 단건 행 정보를 조회합니다.
	AdminCompanyWorkListRowVO getAdminCompanyWorkRow(@Param("workSeq") Long workSeq);

	// 관리자 회사 업무 상세 정보를 조회합니다.
	AdminCompanyWorkDetailVO getAdminCompanyWorkDetail(@Param("workSeq") Long workSeq);

	// 업무관리 화면용 회사 업무 상세 정보를 조회합니다.
	AdminCompanyWorkDetailVO getWorkCompanyWorkDetail(@Param("workSeq") Long workSeq);

	// 관리자 회사 업무 첨부파일 목록을 조회합니다.
	List<AdminCompanyWorkFileVO> getAdminCompanyWorkFileList(@Param("workSeq") Long workSeq);

	// 관리자 회사 업무 첨부파일 단건을 조회합니다.
	AdminCompanyWorkFileVO getAdminCompanyWorkFile(@Param("workJobFileSeq") Integer workJobFileSeq);

	// 관리자 회사 업무 댓글 목록을 조회합니다.
	List<AdminCompanyWorkReplyVO> getAdminCompanyWorkReplyList(@Param("workSeq") Long workSeq);

	// 관리자 회사 업무 댓글 첨부파일 목록을 조회합니다.
	List<AdminCompanyWorkReplyFileVO> getAdminCompanyWorkReplyFileList(@Param("workSeq") Long workSeq);

	// 관리자 회사 업무 댓글 단건을 조회합니다.
	AdminCompanyWorkReplyVO getAdminCompanyWorkReply(@Param("replySeq") Long replySeq);

	// 관리자 회사 업무 댓글 첨부파일 목록을 댓글 번호 기준으로 조회합니다.
	List<AdminCompanyWorkReplyFileVO> getAdminCompanyWorkReplyFileListByReplySeq(@Param("replySeq") Long replySeq);

	// 관리자 회사 업무 댓글 첨부파일 단건을 조회합니다.
	AdminCompanyWorkReplyFileVO getAdminCompanyWorkReplyFile(@Param("replyFileSeq") Integer replyFileSeq);

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

	// 관리자 회사 업무 수기 등록 기본정보를 저장합니다.
	int insertAdminCompanyWorkManualJob(AdminCompanyWorkManualSavePO param);

	// 관리자 회사 업무 첨부파일 정보를 저장합니다.
	int insertAdminCompanyWorkImportFile(AdminCompanyWorkImportFileSavePO param);

	// 관리자 회사 업무 첨부파일을 삭제 처리합니다.
	int softDeleteAdminCompanyWorkFile(
		@Param("workSeq") Long workSeq,
		@Param("workJobFileSeq") Integer workJobFileSeq,
		@Param("udtNo") Long udtNo
	);

	// 관리자 회사 업무 즉시 수정 가능 항목을 저장합니다.
	int updateAdminCompanyWorkEditableFields(AdminCompanyWorkUpdatePO param);

	// 관리자 회사 업무 상세 수정 항목을 저장합니다.
	int updateAdminCompanyWorkDetailFields(AdminCompanyWorkDetailUpdatePO param);

	// 관리자 회사 업무 댓글을 저장합니다.
	int insertAdminCompanyWorkReply(AdminCompanyWorkReplySavePO param);

	// 관리자 회사 업무 댓글을 수정합니다.
	int updateAdminCompanyWorkReply(AdminCompanyWorkReplyUpdatePO param);

	// 관리자 회사 업무 댓글을 삭제 처리합니다.
	int softDeleteAdminCompanyWorkReply(AdminCompanyWorkReplyDeletePO param);

	// 관리자 회사 업무 댓글 첨부파일을 저장합니다.
	int insertAdminCompanyWorkReplyFile(AdminCompanyWorkReplyFileSavePO param);

	// 관리자 회사 업무 댓글 첨부파일을 선택 삭제 처리합니다.
	int softDeleteAdminCompanyWorkReplyFiles(
		@Param("replySeq") Long replySeq,
		@Param("workSeq") Long workSeq,
		@Param("replyFileSeqList") List<Integer> replyFileSeqList,
		@Param("udtNo") Long udtNo
	);

	// 관리자 회사 업무 댓글 첨부파일 전체를 삭제 처리합니다.
	int softDeleteAdminCompanyWorkReplyFileList(
		@Param("replySeq") Long replySeq,
		@Param("workSeq") Long workSeq,
		@Param("udtNo") Long udtNo
	);
}
