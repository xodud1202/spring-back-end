package com.xodud1202.springbackend.domain.admin.brand;

import com.xodud1202.springbackend.domain.common.CommonVO;
import lombok.Data;

// 관리자 브랜드 목록/상세 응답 데이터를 정의합니다.
@Data
public class BrandAdminVO extends CommonVO {
	private Integer brandNo;
	private String brandNm;
	private String brandLogoPath;
	private String brandNoti;
	private Integer dispOrd;
	private String useYn;
	private String delYn;
}
