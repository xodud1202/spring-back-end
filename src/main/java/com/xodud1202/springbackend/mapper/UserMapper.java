package com.xodud1202.springbackend.mapper;

import com.xodud1202.springbackend.domain.admin.user.UserManagePO;
import com.xodud1202.springbackend.domain.admin.user.UserManageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

// 사용자 관리 관련 매퍼를 정의합니다.
@Mapper
public interface UserMapper {
	// 사용자 목록을 조회합니다.
	List<UserManageVO> getAdminUserList(
		@Param("searchGb") String searchGb,
		@Param("searchValue") String searchValue,
		@Param("usrStatCd") String usrStatCd,
		@Param("usrGradeCd") String usrGradeCd
	);

	// 로그인 아이디 중복 건수를 조회합니다.
	int countAdminUserByLoginId(@Param("loginId") String loginId);

	// 사용자 단건을 조회합니다.
	UserManageVO getAdminUserDetail(@Param("usrNo") Long usrNo);

	// 사용자 등록을 처리합니다.
	int insertAdminUser(UserManagePO param);

	// 사용자 수정을 처리합니다.
	int updateAdminUser(UserManagePO param);
}

