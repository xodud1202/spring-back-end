package com.xodud1202.springbackend.domain.admin.exhibition;

import lombok.Data;

@Data
// 기획전 삭제 요청 객체입니다.
public class ExhibitionDeletePO {
	// 기획전 번호입니다.
	private Integer exhibitionNo;
	// 수정자 번호입니다.
	private Long udtNo;
}

