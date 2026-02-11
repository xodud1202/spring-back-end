package com.xodud1202.springbackend.domain.admin.banner;

import lombok.Data;

@Data
// 이미지 배너 정보를 정의합니다.
public class BannerImageInfoPO {
	// 배너 번호입니다.
	private Integer bannerNo;
	// 이미지 경로입니다.
	private String imgPath;
	// 이동 URL입니다.
	private String url;
	// 배너 오픈 코드입니다.
	private String bannerOpenCd;
	// 노출 순서입니다.
	private Integer dispOrd;
	// 등록자 번호입니다.
	private Long regNo;
	// 수정자 번호입니다.
	private Long udtNo;
}
