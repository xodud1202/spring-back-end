package com.xodud1202.springbackend.domain.work.stock;

import lombok.Data;

@Data
// 주식계좌이력 월별 표의 월 컬럼 정보를 전달합니다.
public class WorkStockAccountHistoryMonthVO {
	private String monthKey;
	private String monthLabel;
}
