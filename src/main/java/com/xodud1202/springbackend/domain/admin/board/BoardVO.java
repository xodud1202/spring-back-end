package com.xodud1202.springbackend.domain.admin.board;

import com.xodud1202.springbackend.domain.common.CommonVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BoardVO extends CommonVO {
	private Long boardNo;
	private String boardDivCd;
	private String boardDivNm;
	private String boardDetailDivCd;
	private String boardDetailDivNm;
	private String title;
	private String viewYn;
	private Long readCnt;
}
