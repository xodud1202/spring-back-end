package com.xodud1202.springbackend.domain.admin.exhibition;

import lombok.Data;

@Data
// 기획전 목록 행 정보를 전달하는 객체입니다.
public class ExhibitionVO {
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
	// 썸네일 URL입니다.
	private String thumbnailUrl;
	// 등록일시입니다.
	private String regDt;
	// 수정일시입니다.
	private String udtDt;
}
