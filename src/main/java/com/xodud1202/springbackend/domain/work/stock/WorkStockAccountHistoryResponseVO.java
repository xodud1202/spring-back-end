package com.xodud1202.springbackend.domain.work.stock;

import lombok.Data;

import java.util.List;

@Data
// 주식계좌이력 월별 정보와 전체 이력 정보를 함께 전달합니다.
public class WorkStockAccountHistoryResponseVO {
	private List<WorkStockAccountHistoryMonthVO> monthList;
	private List<WorkStockAccountHistoryValueRowVO> summaryRowList;
	private List<WorkStockAccountHistoryAccountGroupVO> accountGroupList;
	private List<WorkStockAccountDailyHistoryRowVO> historyRowList;
	private Integer historyTotalCount;
	private Integer historyPageSize;
	private Boolean historyHasMore;
}
