package com.xodud1202.springbackend.domain.admin.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

// 사용자 관리 등록/수정 파라미터를 담는 객체입니다.
@Data
public class UserManagePO {
	private Long usrNo;
	private String loginId;
	private String pwd;
	private String userNm;
	private String usrGradeCd;
	private String usrStatCd;
	@JsonProperty("hPhoneNo")
	private String hPhoneNo;
	private String email;
	private Long regNo;
	private Long udtNo;
}
