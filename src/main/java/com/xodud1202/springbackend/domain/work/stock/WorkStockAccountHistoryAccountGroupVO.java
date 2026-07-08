package com.xodud1202.springbackend.domain.work.stock;

import lombok.Data;

import java.util.List;

@Data
// 주식계좌이력 월별 표의 계좌별 행 묶음을 전달합니다.
public class WorkStockAccountHistoryAccountGroupVO {
	private String stockAccountCd;
	private String stockAccountNm;
	private List<WorkStockAccountHistoryValueRowVO> rowList;
}
