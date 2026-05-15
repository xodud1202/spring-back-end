package com.xodud1202.springbackend.mapper;

import com.xodud1202.springbackend.domain.work.stock.WorkStockSaleRowVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockSaleSearchPO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockSaleSummaryRowVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
// 매매일지 조회 전용 Mapper를 정의합니다.
public interface StockSaleHistoryMapper {
	// 검색 조건에 맞는 종목별 매매 합계를 조회합니다.
	List<WorkStockSaleSummaryRowVO> getStockSaleSummaryList(WorkStockSaleSearchPO param);

	// 검색 조건에 맞는 매매 상세 목록을 페이징 조회합니다.
	List<WorkStockSaleRowVO> getStockSaleRowList(WorkStockSaleSearchPO param);

	// 검색 조건에 맞는 매매 상세 목록 전체 건수를 조회합니다.
	int getStockSaleRowCount(WorkStockSaleSearchPO param);
}
