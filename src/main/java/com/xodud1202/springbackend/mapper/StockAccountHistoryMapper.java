package com.xodud1202.springbackend.mapper;

import com.xodud1202.springbackend.domain.work.stock.WorkStockAccountCheckRowVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockAccountCashHistoryCreateRequestVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockAccountDailySaleAmountVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockAccountHistorySearchPO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockAccountMonthlyCashAmountVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockAccountMonthlySaleAmountVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
// 주식계좌이력 조회 Mapper를 정의합니다.
public interface StockAccountHistoryMapper {
	// 입출금 또는 확인금액 테이블의 최신 기준일을 조회합니다.
	String getLatestStockAccountHistoryDate(WorkStockAccountHistorySearchPO param);

	// 매매일지의 최신 기준일을 조회합니다.
	String getLatestStockSaleDate(WorkStockAccountHistorySearchPO param);

	// 계좌별 월간 매수와 매도 금액을 조회합니다.
	List<WorkStockAccountMonthlySaleAmountVO> getMonthlySaleAmountList(WorkStockAccountHistorySearchPO param);

	// 계좌별 월간 입금과 출금 금액을 조회합니다.
	List<WorkStockAccountMonthlyCashAmountVO> getMonthlyCashAmountList(WorkStockAccountHistorySearchPO param);

	// 계좌 확인금액 원천 목록을 조회합니다.
	List<WorkStockAccountCheckRowVO> getStockAccountCheckRowList(WorkStockAccountHistorySearchPO param);

	// 일자별 입출금 순원금 합계를 조회합니다.
	List<WorkStockAccountDailySaleAmountVO> getDailySaleAmountList(WorkStockAccountHistorySearchPO param);

	// 계좌와 확인일이 같은 확인 평가금을 수정합니다.
	int updateStockAccountCheckAmount(WorkStockAccountCheckRowVO param);

	// 계좌 확인 평가금을 등록합니다.
	int insertStockAccountCheckAmount(WorkStockAccountCheckRowVO param);

	// 계좌 입출금 이력을 등록합니다.
	int insertStockAccountCashHistory(WorkStockAccountCashHistoryCreateRequestVO param);
}
