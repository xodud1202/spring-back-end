package com.xodud1202.springbackend.domain.work.stock;

// 계좌별 전체 이력 정보의 표시 기준을 정의합니다.
public enum WorkStockAccountHistoryViewType {
	// 모든 확인일을 표시합니다.
	ALL,
	// 년월별 첫 번째 확인일을 표시합니다.
	MONTH_START,
	// 년월별 마지막 확인일을 표시합니다.
	MONTH_END
}
