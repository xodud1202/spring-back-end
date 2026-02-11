package com.xodud1202.springbackend.domain.admin.banner;

import lombok.Data;

@Data
// 관리자 배너 목록 정보를 정의합니다.
public class BannerVO {
	// 배너 번호입니다.
	private Integer bannerNo;
	// 배너 구분 코드입니다.
	private String bannerDivCd;
	// 배너 구분명입니다.
	private String bannerDivNm;
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
	// 이미지 경로입니다.
	private String imgPath;
	// 등록일시입니다.
	private String regDt;
	// 수정일시입니다.
	private String udtDt;
}
