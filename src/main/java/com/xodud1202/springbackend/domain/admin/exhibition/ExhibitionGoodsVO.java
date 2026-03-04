package com.xodud1202.springbackend.domain.admin.exhibition;

import lombok.Data;

@Data
// 기획전 탭 상품 조회 행 정보를 전달하는 객체입니다.
public class ExhibitionGoodsVO {
	// 기획전 번호입니다.
	private Integer exhibitionNo;
	// 기획전 탭 번호입니다.
	private Integer exhibitionTabNo;
	// 탭명입니다.
	private String tabNm;
	// 상품코드입니다.
	private String goodsId;
	// ERP 품번 코드입니다.
	private String erpStyleCd;
	// 상품명입니다.
	private String goodsNm;
	// 이미지 경로입니다.
	private String imgPath;
	// 이미지 URL입니다.
	private String imgUrl;
	// 노출 순서입니다.
	private Integer dispOrd;
	// 노출 여부입니다.
	private String showYn;
	// 삭제 여부입니다.
	private String delYn;
}
