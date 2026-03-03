package com.xodud1202.springbackend.domain.admin.exhibition;

import lombok.Data;

@Data
// 기획전 상품 정보를 전달하는 객체입니다.
public class ExhibitionGoodsPO {
	// 프론트에서 사용한 행 키입니다.
	private String rowKey;
	// 기획전 상품 탭 번호입니다.
	private Integer exhibitionTabNo;
	// 기획전 탭 행 키입니다.
	private String exhibitionTabRowKey;
	// 기획전 번호입니다.
	private Integer exhibitionNo;
	// 상품코드입니다.
	private String goodsId;
	// 노출 순서입니다.
	private Integer dispOrd;
	// 노출 여부입니다.
	private String showYn;
	// 삭제 여부입니다.
	private String delYn;
	// 등록자 번호입니다.
	private Long regNo;
	// 수정자 번호입니다.
	private Long udtNo;
}

