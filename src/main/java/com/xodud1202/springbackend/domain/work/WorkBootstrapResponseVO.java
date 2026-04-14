package com.xodud1202.springbackend.domain.work;

import com.xodud1202.springbackend.domain.admin.common.UserInfoVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkCompanyVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkProjectVO;
import com.xodud1202.springbackend.domain.common.CommonCodeVO;
import lombok.Data;

import java.util.List;

// 업무관리 초기 화면에 필요한 선택 목록과 로그인 사용자 정보를 정의합니다.
@Data
public class WorkBootstrapResponseVO {
	// 현재 로그인 사용자 정보입니다.
	private UserInfoVO currentUser;
	// 회사 선택 목록입니다.
	private List<AdminCompanyWorkCompanyVO> companyList;
	// 초기 선택 회사의 프로젝트 목록입니다.
	private List<AdminCompanyWorkProjectVO> projectList;
	// 업무 상태 공통코드 목록입니다.
	private List<CommonCodeVO> workStatList;
	// 업무 우선순위 공통코드 목록입니다.
	private List<CommonCodeVO> workPriorList;
}
