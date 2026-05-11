package com.xodud1202.springbackend.domain.work.vacation;

import lombok.Data;

@Data
// 휴가자 선택 항목을 정의합니다.
public class WorkVacationPersonVO {
	// 휴가자 번호입니다.
	private Integer personSeq;
	// 휴가자명입니다.
	private String personNm;
	// 즐겨찾기 여부입니다.
	private String favoriteYn;
	// 표시 순서입니다.
	private Integer dispOrd;
}
