package com.xodud1202.springbackend.domain.admin.exhibition;

import lombok.Data;

import java.util.List;

@Data
// 기획전 등록/수정을 위한 요청 객체입니다.
public class ExhibitionSavePO {
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
	// PC 상세 설명입니다.
	private String exhibitionPcDesc;
	// MO 상세 설명입니다.
	private String exhibitionMoDesc;
	// 등록자 번호입니다.
	private Long regNo;
	// 수정자 번호입니다.
	private Long udtNo;
	// 탭 목록입니다.
	private List<ExhibitionTabPO> tabList;
	// 탭별 상품 목록입니다.
	private List<ExhibitionGoodsPO> goodsList;
	// 탭 삭제 시 연관 상품 강제 삭제 여부입니다.
	private Boolean forceDeleteGoodsWithTabs;
}
