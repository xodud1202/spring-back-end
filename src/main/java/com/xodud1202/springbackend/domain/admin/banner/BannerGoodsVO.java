package com.xodud1202.springbackend.domain.admin.banner;

import lombok.Data;

@Data
// 배너 상품 목록 조회 정보를 정의합니다.
public class BannerGoodsVO {
	// 배너 번호입니다.
	private Integer bannerNo;
	// 배너 탭 번호입니다.
	private Integer bannerTabNo;
	// 탭명입니다.
	private String tabNm;
	// 상품 코드입니다.
	private String goodsId;
	// ERP 품번 코드입니다.
	private String erpStyleCd;
	// 상품명입니다.
	private String goodsNm;
	// 상품 상태명입니다.
	private String goodsStatNm;
	// 상품 구분명입니다.
	private String goodsDivNm;
	// 이미지 경로입니다.
	private String imgPath;
	// 이미지 URL입니다.
	private String imgUrl;
	// 노출 순서입니다.
	private Integer dispOrd;
	// 노출 여부입니다.
	private String showYn;
}
