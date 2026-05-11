package com.xodud1202.springbackend.domain.work.vacation;

import com.xodud1202.springbackend.domain.admin.common.UserInfoVO;
import com.xodud1202.springbackend.domain.common.CommonCodeVO;
import lombok.Data;

import java.util.List;

@Data
// 휴가관리 화면 초기 응답 정보를 정의합니다.
public class WorkVacationBootstrapResponseVO {
	// 현재 로그인 사용자 정보입니다.
	private UserInfoVO currentUser;
	// 휴가 사용 가능 회사 목록입니다.
	private List<WorkVacationCompanyVO> companyList;
	// 휴가자 목록입니다.
	private List<WorkVacationPersonVO> personList;
	// 휴가 구분 공통코드 목록입니다.
	private List<CommonCodeVO> vacationCodeList;
}
