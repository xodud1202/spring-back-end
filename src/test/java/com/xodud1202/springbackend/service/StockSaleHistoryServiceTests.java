package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.work.stock.WorkStockSaleListResponseVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockSaleRowVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockSaleSearchPO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockSaleSummaryRowVO;
import com.xodud1202.springbackend.mapper.CommonMapper;
import com.xodud1202.springbackend.mapper.StockSaleHistoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// StockSaleHistoryService의 현재 보유원금 계산 로직을 검증합니다.
class StockSaleHistoryServiceTests {
	@Mock
	private CommonMapper commonMapper;

	@Mock
	private StockSaleHistoryMapper stockSaleHistoryMapper;

	@InjectMocks
	private StockSaleHistoryService stockSaleHistoryService;

	@Test
	@DisplayName("종목별 합계: 매수만 있으면 보유수량과 보유원금을 그대로 반환")
	// 매수 거래만 있는 열린 포지션의 현재 보유값을 확인합니다.
	void getStockSaleList_returnsBuyOnlyHoldingPrincipal() {
		// 단일 종목의 매수 거래만 구성합니다.
		stubStockSaleList(
			List.of(createSummary("STOCK_A", "KODEX 미국S&P500", 10000L, 0L)),
			List.of(createRow(1L, "ACCOUNT_A", "STOCK_A", 10, 10000L, 0L))
		);

		// 목록 조회 후 현재 보유 원금 계산값을 검증합니다.
		WorkStockSaleSummaryRowVO summary = getSingleSummary();
		assertEquals(10L, summary.getSaleCnt());
		assertEquals(10000L, summary.getHoldingPrincipalAmt());
		assertEquals(new BigDecimal("1000.00"), summary.getAverageSaleAmt());
		assertEquals(10000L, summary.getSaleAmt());
	}

	@Test
	@DisplayName("종목별 합계: 일부 매도 손익은 매도된 원금을 차감해 보유원금 계산")
	// 일부 매도 후 남은 수량과 원금이 손익을 제외한 매도 원금 기준으로 계산되는지 확인합니다.
	void getStockSaleList_subtractsSoldPrincipalWhenPartiallySold() {
		// 10주 매수 후 4주를 이익 2,000원으로 매도한 거래를 구성합니다.
		stubStockSaleList(
			List.of(createSummary("STOCK_A", "KODEX 미국S&P500", 4000L, 2000L)),
			List.of(
				createRow(1L, "ACCOUNT_A", "STOCK_A", 10, 10000L, 0L),
				createRow(2L, "ACCOUNT_A", "STOCK_A", -4, -6000L, 2000L)
			)
		);

		// 매도대금 6,000원에서 손익 2,000원을 제외한 4,000원만 원금에서 차감합니다.
		WorkStockSaleSummaryRowVO summary = getSingleSummary();
		assertEquals(6L, summary.getSaleCnt());
		assertEquals(6000L, summary.getHoldingPrincipalAmt());
		assertEquals(new BigDecimal("1000.00"), summary.getAverageSaleAmt());
	}

	@Test
	@DisplayName("종목별 합계: 전량 매도 후 재매수하면 이전 포지션 원금을 리셋")
	// 같은 계좌와 종목에서 닫힌 포지션 이후의 재매수는 새 원금으로 시작하는지 확인합니다.
	void getStockSaleList_resetsPrincipalAfterFullSellAndRebuy() {
		// 첫 포지션은 전량 매도하고 같은 계좌에서 새로 2주를 매수합니다.
		stubStockSaleList(
			List.of(createSummary("STOCK_A", "KODEX 미국S&P500", 2000L, 3000L)),
			List.of(
				createRow(1L, "ACCOUNT_A", "STOCK_A", 10, 10000L, 0L),
				createRow(2L, "ACCOUNT_A", "STOCK_A", -10, -13000L, 3000L),
				createRow(3L, "ACCOUNT_A", "STOCK_A", 2, 5000L, 0L)
			)
		);

		// 전량 매도된 이전 원금은 남지 않고 새 매수 원금만 표시됩니다.
		WorkStockSaleSummaryRowVO summary = getSingleSummary();
		assertEquals(2L, summary.getSaleCnt());
		assertEquals(5000L, summary.getHoldingPrincipalAmt());
		assertEquals(new BigDecimal("2500.00"), summary.getAverageSaleAmt());
	}

	@Test
	@DisplayName("종목별 합계: 다른 계좌의 종료 포지션은 열린 계좌 원금에 섞이지 않음")
	// 계좌별로 포지션 종료와 현재 보유 상태가 독립 계산되는지 확인합니다.
	void getStockSaleList_separatesClosedPositionByAccount() {
		// ACCOUNT_A는 전량 매도했고 ACCOUNT_B만 현재 보유 중인 거래를 구성합니다.
		stubStockSaleList(
			List.of(createSummary("STOCK_A", "KODEX 미국S&P500", 2000L, 3000L)),
			List.of(
				createRow(1L, "ACCOUNT_A", "STOCK_A", 10, 10000L, 0L),
				createRow(2L, "ACCOUNT_A", "STOCK_A", -10, -13000L, 3000L),
				createRow(3L, "ACCOUNT_B", "STOCK_A", 3, 9000L, 0L)
			)
		);

		// 닫힌 ACCOUNT_A 원금은 제외하고 열린 ACCOUNT_B 원금만 합산합니다.
		WorkStockSaleSummaryRowVO summary = getSingleSummary();
		assertEquals(3L, summary.getSaleCnt());
		assertEquals(9000L, summary.getHoldingPrincipalAmt());
		assertEquals(new BigDecimal("3000.00"), summary.getAverageSaleAmt());
	}

	@Test
	@DisplayName("종목별 합계: 보유원금 계산 조회는 날짜 필터를 사용하지 않음")
	// 날짜 검색은 상세 목록에만 적용하고 현재 보유원금은 최신 전체 거래 기준으로 계산합니다.
	void getStockSaleList_ignoresDateFilterForHoldingPrincipal() {
		// 날짜 필터를 전달해도 보유원금용 조회 조건에는 계좌와 종목만 남깁니다.
		stubStockSaleList(
			List.of(createSummary("STOCK_A", "KODEX 미국S&P500", 10000L, 0L)),
			List.of(createRow(1L, "ACCOUNT_A", "STOCK_A", 10, 10000L, 0L))
		);

		stockSaleHistoryService.getStockSaleList("2026-06-01", "2026-06-30", List.of("ACCOUNT_A"), List.of("STOCK_A"), 1, 20);

		// 보유원금 원천 조회에는 날짜 조건이 제거되었는지 확인합니다.
		ArgumentCaptor<WorkStockSaleSearchPO> holdingParamCaptor = ArgumentCaptor.forClass(WorkStockSaleSearchPO.class);
		verify(stockSaleHistoryMapper).getStockSaleHoldingSourceRowList(holdingParamCaptor.capture());
		WorkStockSaleSearchPO holdingParam = holdingParamCaptor.getValue();
		assertNull(holdingParam.getStartSaleDt());
		assertNull(holdingParam.getEndSaleDt());
		assertEquals(List.of("ACCOUNT_A"), holdingParam.getStockAccountCdList());
		assertEquals(List.of("STOCK_A"), holdingParam.getStockNmCdList());
	}

	// 매매일지 목록 조회에 필요한 매퍼 응답을 구성합니다.
	private void stubStockSaleList(List<WorkStockSaleSummaryRowVO> summaryList, List<WorkStockSaleRowVO> holdingSourceRowList) {
		when(stockSaleHistoryMapper.getStockSaleRowCount(any(WorkStockSaleSearchPO.class))).thenReturn(0);
		when(stockSaleHistoryMapper.getStockSaleSummaryList(any(WorkStockSaleSearchPO.class))).thenReturn(summaryList);
		when(stockSaleHistoryMapper.getStockSaleHoldingSourceRowList(any(WorkStockSaleSearchPO.class))).thenReturn(holdingSourceRowList);
		when(stockSaleHistoryMapper.getStockSaleRowList(any(WorkStockSaleSearchPO.class))).thenReturn(List.of());
	}

	// 단일 종목 합계 조회 결과를 반환합니다.
	private WorkStockSaleSummaryRowVO getSingleSummary() {
		WorkStockSaleListResponseVO response = stockSaleHistoryService.getStockSaleList(null, null, List.of(), List.of(), 1, 20);
		assertEquals(1, response.getSummaryList().size());
		return response.getSummaryList().get(0);
	}

	// 테스트용 종목별 기존 합계 행을 생성합니다.
	private WorkStockSaleSummaryRowVO createSummary(String stockNmCd, String stockNm, long saleAmt, long profitAmt) {
		WorkStockSaleSummaryRowVO summary = new WorkStockSaleSummaryRowVO();
		summary.setStockNmCd(stockNmCd);
		summary.setStockNm(stockNm);
		summary.setSaleCnt(0L);
		summary.setSaleAmt(saleAmt);
		summary.setHoldingPrincipalAmt(0L);
		summary.setProfitAmt(profitAmt);
		return summary;
	}

	// 테스트용 매매 원천 거래 행을 생성합니다.
	private WorkStockSaleRowVO createRow(Long saleHistSeq, String stockAccountCd, String stockNmCd, int saleCnt, long saleAmt, long profitAmt) {
		WorkStockSaleRowVO row = new WorkStockSaleRowVO();
		row.setSaleHistSeq(saleHistSeq);
		row.setSaleDt("20260101");
		row.setStockAccountCd(stockAccountCd);
		row.setStockNmCd(stockNmCd);
		row.setSaleCnt(saleCnt);
		row.setSaleAmt(saleAmt);
		row.setProfitAmt(profitAmt);
		return row;
	}
}
