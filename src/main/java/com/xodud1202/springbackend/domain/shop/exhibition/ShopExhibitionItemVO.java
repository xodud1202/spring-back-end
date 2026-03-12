package com.xodud1202.springbackend.domain.shop.exhibition;

import lombok.Data;

@Data
// 쇼핑몰 기획전 목록 단건 정보를 전달합니다.
public class ShopExhibitionItemVO {
	// 기획전 번호입니다.
	private Integer exhibitionNo;
	// 기획전명(썸네일명)입니다.
	private String exhibitionNm;
	// 기획전 썸네일 이미지 URL입니다.
	private String thumbnailUrl;
	// 기획전 노출 시작 일시입니다.
	private String dispStartDt;
}

