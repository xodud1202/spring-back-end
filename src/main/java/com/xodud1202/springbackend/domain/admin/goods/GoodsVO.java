package com.xodud1202.springbackend.domain.admin.goods;

import com.xodud1202.springbackend.domain.common.CommonVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

// 관리자 상품 목록 응답 데이터를 정의합니다.
@Data
@EqualsAndHashCode(callSuper = true)
public class GoodsVO extends CommonVO {
	private String goodsId;
	private Integer brandNo;
	private String brandNm;
	private String erpStyleCd;
	private String goodsNm;
	private String goodsStatCd;
	private String goodsStatNm;
	private String goodsDivCd;
	private String goodsDivNm;
	private String showYn;
	// 상품 이미지 경로입니다.
	private String imgPath;
	// 상품 이미지 URL입니다.
	private String imgUrl;
}
