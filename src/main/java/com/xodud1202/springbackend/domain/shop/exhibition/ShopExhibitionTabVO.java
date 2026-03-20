package com.xodud1202.springbackend.domain.shop.exhibition;

import lombok.Data;

@Data
// 쇼핑몰 기획전 상세 화면 탭 정보를 전달합니다.
public class ShopExhibitionTabVO {
	// 기획전 탭 번호입니다.
	private Integer exhibitionTabNo;
	// 기획전 탭명입니다.
	private String exhibitionTabNm;
	// 탭 노출 시작 일시입니다.
	private String dispStartDt;
	// 탭 노출 종료 일시입니다.
	private String dispEndDt;
}
