package com.xodud1202.springbackend.mapper;

import com.xodud1202.springbackend.domain.common.CommonCodeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommonMapper {
	List<CommonCodeVO> getCommonCodeList(@Param("grpCd") String grpCd);
}
