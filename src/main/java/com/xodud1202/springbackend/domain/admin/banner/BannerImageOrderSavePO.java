package com.xodud1202.springbackend.domain.admin.banner;

import lombok.Data;

import java.util.List;

@Data
// 배너 이미지 정렬 저장 요청을 정의합니다.
public class BannerImageOrderSavePO {
	// 배너 번호입니다.
	private Integer bannerNo;
	// 수정자 번호입니다.
	private Long udtNo;
	// 정렬 목록입니다.
	private List<BannerImageOrderPO> orders;
}
