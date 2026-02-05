package com.xodud1202.springbackend.domain.admin.common;

import lombok.Data;

// 로그인 사용자 정보를 전달합니다.
@Data
public class UserInfoVO {
	// 사용자 번호입니다.
	private Long usrNo;
	// 로그인 아이디입니다.
	private String loginId;
	// 사용자명입니다.
	private String userNm;
	// 사용자 등급 코드입니다.
	private String usrGradeCd;
	// 사용자 상태 코드입니다.
	private String usrStatCd;
}
