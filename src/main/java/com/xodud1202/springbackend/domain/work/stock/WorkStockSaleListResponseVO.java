package com.xodud1202.springbackend.domain.work.stock;

import lombok.Data;

import java.util.List;

@Data
// 매매일지 검색 결과의 종목 합계와 상세 목록 페이지를 전달합니다.
public class WorkStockSaleListResponseVO {
	private List<WorkStockSaleSummaryRowVO> summaryList;
	private List<WorkStockSaleRowVO> rowList;
	private int totalCount;
	private int pageNo;
	private int pageSize;
	private int totalPageCount;
}
