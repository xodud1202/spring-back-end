package com.xodud1202.springbackend.domain.admin.exhibition;

import lombok.Data;

import java.util.List;

@Data
// 기획전 상세 정보를 전달하는 객체입니다.
public class ExhibitionDetailVO {
	// 기획전 번호입니다.
	private Integer exhibitionNo;
	// 기획전명입니다.
	private String exhibitionNm;
	// 노출 시작일시입니다.
	private String dispStartDt;
	// 노출 종료일시입니다.
	private String dispEndDt;
	// 리스트 노출 여부입니다.
	private String listShowYn;
	// 노출 여부입니다.
	private String showYn;
	// PC 상세 HTML입니다.
	private String exhibitionPcDesc;
	// MO 상세 HTML입니다.
	private String exhibitionMoDesc;
	// 탭 목록입니다.
	private List<ExhibitionTabPO> tabList;
	// 탭별 상품 목록입니다.
	private List<ExhibitionGoodsVO> goodsList;
}

