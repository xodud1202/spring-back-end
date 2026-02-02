package com.xodud1202.springbackend.mapper;

import com.xodud1202.springbackend.domain.admin.brand.BrandAdminVO;
import com.xodud1202.springbackend.domain.admin.brand.BrandPO;

import java.util.List;

// 관리자 브랜드 관련 MyBatis 매퍼를 정의합니다.
public interface BrandMapper {
	// 관리자 브랜드 목록을 조회합니다.
	List<BrandAdminVO> getAdminBrandList(BrandPO param);

	// 관리자 브랜드 목록 조회 건수를 반환합니다.
	int getAdminBrandCount(BrandPO param);

	// 관리자 브랜드 상세 정보를 조회합니다.
	BrandAdminVO getAdminBrandDetail(BrandPO param);

	// 관리자 브랜드를 등록합니다.
	int insertAdminBrand(BrandPO param);

	// 관리자 브랜드를 수정합니다.
	int updateAdminBrand(BrandPO param);
	// 관리자 브랜드 삭제를 처리합니다.
	int deleteAdminBrand(BrandPO param);
}
