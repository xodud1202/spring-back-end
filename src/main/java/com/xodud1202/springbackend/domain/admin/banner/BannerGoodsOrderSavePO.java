package com.xodud1202.springbackend.domain.admin.banner;

import lombok.Data;

import java.util.List;

@Data
// 배너 상품 정렬 저장 요청을 정의합니다.
public class BannerGoodsOrderSavePO {
	// 배너 번호입니다.
	private Integer bannerNo;
	// 배너 탭 번호입니다.
	private Integer bannerTabNo;
	// 수정자 번호입니다.
	private Long udtNo;
	// 정렬 목록입니다.
	private List<BannerGoodsPO> orders;
}
