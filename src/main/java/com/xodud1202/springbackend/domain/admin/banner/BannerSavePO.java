package com.xodud1202.springbackend.domain.admin.banner;

import lombok.Data;

import java.util.List;

@Data
// 관리자 배너 저장 요청 데이터를 정의합니다.
public class BannerSavePO {
	// 배너 번호입니다.
	private Integer bannerNo;
	// 배너 구분 코드입니다.
	private String bannerDivCd;
	// 배너명입니다.
	private String bannerNm;
	// 노출 시작일시입니다.
	private String dispStartDt;
	// 노출 종료일시입니다.
	private String dispEndDt;
	// 노출 순서입니다.
	private Integer dispOrd;
	// 노출 여부입니다.
	private String showYn;
	// 등록자 번호입니다.
	private Long regNo;
	// 수정자 번호입니다.
	private Long udtNo;
	// 이미지 배너 정보입니다.
	private BannerImageInfoPO imageInfo;
	// 상품 탭 목록입니다.
	private List<BannerTabPO> tabList;
	// 상품 목록입니다.
	private List<BannerGoodsPO> goodsList;
}
