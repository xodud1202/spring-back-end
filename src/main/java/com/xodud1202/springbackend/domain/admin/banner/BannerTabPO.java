package com.xodud1202.springbackend.domain.admin.banner;

import lombok.Data;

@Data
// 상품 탭 정보를 정의합니다.
public class BannerTabPO {
	// 배너 탭 번호입니다.
	private Integer bannerTabNo;
	// 배너 번호입니다.
	private Integer bannerNo;
	// 탭명입니다.
	private String tabNm;
	// 노출 순서입니다.
	private Integer dispOrd;
	// 노출 여부입니다.
	private String showYn;
	// 삭제 여부입니다.
	private String delYn;
	// 등록자 번호입니다.
	private Long regNo;
	// 수정자 번호입니다.
	private Long udtNo;
}
