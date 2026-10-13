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
	// 이력 표의 표시 기준과 페이지에 영향을 받지 않는 전체 그래프 원천입니다.
	private List<WorkStockAccountProfitGraphPointVO> profitGraphPointList;
	private Integer historyTotalCount;
	private Integer historyPageSize;
	private Boolean historyHasMore;
	private List<WorkStockAccountCashHistoryCreateRequestVO> cashHistoryRowList;
	private Integer cashHistoryPageSize;
	private Boolean cashHistoryHasMore;
}
