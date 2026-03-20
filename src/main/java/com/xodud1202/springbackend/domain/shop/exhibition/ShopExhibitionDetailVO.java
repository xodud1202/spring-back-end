package com.xodud1202.springbackend.domain.shop.exhibition;

import lombok.Data;

import java.util.List;

@Data
// 쇼핑몰 기획전 상세 화면 데이터를 전달합니다.
public class ShopExhibitionDetailVO {
	// 기획전 번호입니다.
	private Integer exhibitionNo;
	// 기획전명입니다.
	private String exhibitionNm;
	// 기획전 노출 시작 일시입니다.
	private String dispStartDt;
	// 기획전 노출 종료 일시입니다.
	private String dispEndDt;
	// 데스크톱 기본 기준의 노출 HTML입니다.
	private String visibleHtml;
	// PC 상세 HTML입니다.
	private String pcHtml;
	// 모바일 상세 HTML입니다.
	private String mobileHtml;
	// 기본 선택 탭 번호입니다.
	private Integer defaultTabNo;
	// 노출 가능한 탭 목록입니다.
	private List<ShopExhibitionTabVO> tabList;
}
