package com.xodud1202.springbackend.mapper;

import com.xodud1202.springbackend.domain.admin.user.UserManagePO;
import com.xodud1202.springbackend.domain.admin.user.UserManageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

// 사용자 관리 관련 매퍼를 정의합니다.
@Mapper
public interface UserMapper {
    // 인증용 최소 필드를 조회하며 상태/비밀번호 변경을 즉시 반영하도록 두 캐시를 모두 우회합니다.
    @org.apache.ibatis.annotations.Select("SELECT USR_NO AS usrNo, LOGIN_ID AS loginId, PWD AS pwd, "
        + "USR_STAT_CD AS usrStatCd, REG_DT AS regDt, UDT_DT AS udtDt FROM eshop.USER_BASE WHERE USR_NO=#{usrNo}")
    @org.apache.ibatis.annotations.Options(useCache=false, flushCache=org.apache.ibatis.annotations.Options.FlushCachePolicy.TRUE)
    com.xodud1202.springbackend.entity.UserBaseEntity findAuthenticationUserByUsrNo(@Param("usrNo") Long usrNo);

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
