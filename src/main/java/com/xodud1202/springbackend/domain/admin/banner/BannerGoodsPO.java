package com.xodud1202.springbackend.domain.admin.banner;

import lombok.Data;

@Data
// 상품 배너 상품 정보를 정의합니다.
public class BannerGoodsPO {
	// 배너 번호입니다.
	private Integer bannerNo;
	// 배너 탭 번호입니다.
	private Integer bannerTabNo;
	// 상품 코드입니다.
	private String goodsId;
	// 노출 순서입니다.
	private Integer dispOrd;
	// 노출 여부입니다.
	private String showYn;
	// 등록자 번호입니다.
	private Long regNo;
	// 수정자 번호입니다.
	private Long udtNo;
	// 탭명입니다.
	private String tabNm;
}
