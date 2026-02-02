package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.admin.brand.BrandAdminVO;
import com.xodud1202.springbackend.domain.admin.brand.BrandPO;
import com.xodud1202.springbackend.mapper.BrandMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 관리자 브랜드 관련 비즈니스 로직을 처리합니다.
@Service
@RequiredArgsConstructor
public class BrandService {
	private final BrandMapper brandMapper;

	// 관리자 브랜드 목록을 페이징 조건으로 조회합니다.
	public Map<String, Object> getAdminBrandList(BrandPO param) {
		// 페이지 정보가 없으면 기본값을 사용합니다.
		int page = param.getPage() == null || param.getPage() < 1 ? 1 : param.getPage();
		int pageSize = 20;
		int offset = (page - 1) * pageSize;

		// 페이징 파라미터를 요청 객체에 반영합니다.
		param.setPage(page);
		param.setPageSize(pageSize);
		param.setOffset(offset);

		// 목록과 건수를 조회합니다.
		List<BrandAdminVO> list = brandMapper.getAdminBrandList(param);
		int totalCount = brandMapper.getAdminBrandCount(param);

		// 응답 데이터를 구성합니다.
		Map<String, Object> result = new HashMap<>();
		result.put("list", list);
		result.put("totalCount", totalCount);
		result.put("page", page);
		result.put("pageSize", pageSize);
		return result;
	}

	// 관리자 브랜드 상세 정보를 조회합니다.
	public BrandAdminVO getAdminBrandDetail(BrandPO param) {
		// 상세 정보를 단건 조회합니다.
		return brandMapper.getAdminBrandDetail(param);
	}

	// 관리자 브랜드 정보를 등록합니다.
	public int insertAdminBrand(BrandPO param) {
		// 신규 브랜드를 등록합니다.
		return brandMapper.insertAdminBrand(param);
	}

	// 관리자 브랜드 정보를 수정합니다.
	public int updateAdminBrand(BrandPO param) {
		// 기존 브랜드 정보를 수정합니다.
		return brandMapper.updateAdminBrand(param);
	}

	// 관리자 브랜드를 삭제합니다.
	public int deleteAdminBrand(BrandPO param) {
		// 기존 브랜드 정보를 삭제 처리합니다.
		return brandMapper.deleteAdminBrand(param);
	}
}
