package com.xodud1202.springbackend.mapper;

import com.xodud1202.springbackend.domain.admin.common.CommonCodeManagePO;
import com.xodud1202.springbackend.domain.common.CommonCodeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
// 공통 코드 관련 매퍼를 정의합니다.
public interface CommonMapper {
	// 사용 중인 공통 코드 목록을 조회합니다.
	List<CommonCodeVO> getCommonCodeList(@Param("grpCd") String grpCd);

	// 관리자용 상위 공통 코드 목록을 조회합니다.
	List<CommonCodeVO> getAdminRootCommonCodeList(@Param("grpCd") String grpCd, @Param("grpCdNm") String grpCdNm);

	// 관리자용 하위 공통 코드 목록을 조회합니다.
	List<CommonCodeVO> getAdminChildCommonCodeList(@Param("grpCd") String grpCd);

	// 관리자용 공통 코드 단건을 조회합니다.
	CommonCodeVO getAdminCommonCodeDetail(@Param("grpCd") String grpCd, @Param("cd") String cd);

	// 공통 코드 중복 건수를 조회합니다.
	int countAdminCommonCode(@Param("grpCd") String grpCd, @Param("cd") String cd);

	// 공통 코드 중복 건수를 기존 키 제외 조건으로 조회합니다.
	int countAdminCommonCodeExcludeOrigin(
		@Param("grpCd") String grpCd,
		@Param("cd") String cd,
		@Param("originGrpCd") String originGrpCd,
		@Param("originCd") String originCd
	);

	// 특정 그룹코드 하위 건수를 조회합니다.
	int countAdminCommonCodeByGrpCd(@Param("grpCd") String grpCd);

	// 관리자용 공통 코드를 등록합니다.
	int insertAdminCommonCode(CommonCodeManagePO param);

	// 관리자용 공통 코드를 수정합니다.
	int updateAdminCommonCode(CommonCodeManagePO param);

	// 상위 코드 변경 시 하위 코드의 그룹코드를 일괄 수정합니다.
	int updateAdminCommonCodeChildrenGrpCd(@Param("originGrpCd") String originGrpCd, @Param("nextGrpCd") String nextGrpCd);
}
