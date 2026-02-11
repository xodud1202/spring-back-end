package com.xodud1202.springbackend.domain.admin.banner;

import lombok.Data;

@Data
// 관리자 배너 삭제 요청 데이터를 정의합니다.
public class BannerDeletePO {
	private Integer bannerNo;
	private Long udtNo;
}
