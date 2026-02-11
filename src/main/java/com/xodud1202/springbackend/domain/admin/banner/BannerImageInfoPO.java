package com.xodud1202.springbackend.domain.admin.banner;

import lombok.Data;

@Data
// 이미지 배너 정보를 정의합니다.
public class BannerImageInfoPO {
	// 클라이언트 임시 행 키입니다.
	private String rowKey;
	// 이미지 배너 번호입니다.
	private Long imageBannerNo;
	// 배너 번호입니다.
	private Integer bannerNo;
	// 배너 이미지명입니다.
	private String bannerNm;
	// 이미지 경로입니다.
	private String imgPath;
	// 이동 URL입니다.
	private String url;
	// 배너 오픈 코드입니다.
	private String bannerOpenCd;
	// 노출 순서입니다.
	private Integer dispOrd;
	// 노출 시작일시입니다.
	private String dispStartDt;
	// 노출 종료일시입니다.
	private String dispEndDt;
	// 노출 여부입니다.
	private String showYn;
	// 삭제 여부입니다.
	private String delYn;
	// 등록자 번호입니다.
	private Long regNo;
	// 등록일시입니다.
	private String regDt;
	// 수정자 번호입니다.
	private Long udtNo;
	// 수정일시입니다.
	private String udtDt;
}
