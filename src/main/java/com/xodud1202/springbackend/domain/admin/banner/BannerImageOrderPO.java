package com.xodud1202.springbackend.domain.admin.banner;

import lombok.Data;

@Data
// 배너 이미지 정렬 정보를 정의합니다.
public class BannerImageOrderPO {
	// 이미지 배너 번호입니다.
	private Long imageBannerNo;
	// 노출 순서입니다.
	private Integer dispOrd;
}
