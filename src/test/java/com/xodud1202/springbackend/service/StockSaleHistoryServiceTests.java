package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.work.stock.WorkStockSaleListResponseVO;
import com.xodud1202.springbackend.domain.common.CommonCodeVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockSaleCreateRequestVO;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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

	@Test
	@DisplayName("매매등록: 매도는 음수만 입력 가능")
	// 매도 수량에 양수 금액이 들어오면 저장 전에 차단합니다.
	void createStockSaleHistory_rejectsPositiveAmountWhenSaleCountIsNegative() {
		stubStockSaleOptionLists();
		WorkStockSaleCreateRequestVO request = createStockSaleCreateRequest(-10, 10000L, 0L);

		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> stockSaleHistoryService.createStockSaleHistory(request, 1L)
		);

		assertEquals("매도는 음수만 입력 할 수 있습니다.", exception.getMessage());
		verify(stockSaleHistoryMapper, never()).insertStockSaleHistory(any(WorkStockSaleCreateRequestVO.class));
	}

	@Test
	@DisplayName("매매등록: 매수는 양수만 입력 가능")
	// 매수 수량에 음수 금액이 들어오면 저장 전에 차단합니다.
	void createStockSaleHistory_rejectsNegativeAmountWhenSaleCountIsPositive() {
		stubStockSaleOptionLists();
		WorkStockSaleCreateRequestVO request = createStockSaleCreateRequest(10, -10000L, 0L);

		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> stockSaleHistoryService.createStockSaleHistory(request, 1L)
		);

		assertEquals("매수는 양수만 입력 할 수 있습니다.", exception.getMessage());
		verify(stockSaleHistoryMapper, never()).insertStockSaleHistory(any(WorkStockSaleCreateRequestVO.class));
	}

	@Test
	@DisplayName("매매등록: 매수 수량에는 손익금액을 입력할 수 없음")
	// 매수 등록에 손익금액이 들어오면 저장 전에 차단합니다.
	void createStockSaleHistory_rejectsProfitAmountWhenSaleCountIsPositive() {
		stubStockSaleOptionLists();
		WorkStockSaleCreateRequestVO request = createStockSaleCreateRequest(10, 10000L, 1000L);

		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> stockSaleHistoryService.createStockSaleHistory(request, 1L)
		);

		assertEquals("매수 등록 시 손익금액은 입력할 수 없습니다.", exception.getMessage());
		verify(stockSaleHistoryMapper, never()).insertStockSaleHistory(any(WorkStockSaleCreateRequestVO.class));
	}

	@Test
	@DisplayName("매매등록: 매수 수량의 빈 손익금액은 0으로 저장")
	// 매수 등록에서 손익금액이 비어 있으면 입력하지 않은 것으로 보고 0으로 저장합니다.
	void createStockSaleHistory_allowsPositiveSaleCountWhenProfitAmountIsEmpty() {
		stubStockSaleOptionLists();
		WorkStockSaleCreateRequestVO request = createStockSaleCreateRequest(10, 10000L, null);
		when(stockSaleHistoryMapper.insertStockSaleHistory(any(WorkStockSaleCreateRequestVO.class))).thenReturn(1);

		stockSaleHistoryService.createStockSaleHistory(request, 1L);

		ArgumentCaptor<WorkStockSaleCreateRequestVO> createCaptor = ArgumentCaptor.forClass(WorkStockSaleCreateRequestVO.class);
		verify(stockSaleHistoryMapper).insertStockSaleHistory(createCaptor.capture());
		assertEquals(10, createCaptor.getValue().getSaleCnt());
		assertEquals(10000L, createCaptor.getValue().getSaleAmt());
		assertEquals(0L, createCaptor.getValue().getProfitAmt());
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

	// 매매등록 검증에 필요한 계좌와 종목 공통코드 목록을 구성합니다.
	private void stubStockSaleOptionLists() {
		when(commonMapper.getCommonCodeList(eq("STOCK_ACCOUNT"))).thenReturn(List.of(createCommonCode("ACCOUNT_A", "계좌A")));
		when(commonMapper.getCommonCodeList(eq("STOCK_NM"))).thenReturn(List.of(createCommonCode("STOCK_A", "종목A")));
	}

	// 공통코드를 생성합니다.
	private CommonCodeVO createCommonCode(String code, String codeName) {
		CommonCodeVO commonCode = new CommonCodeVO();
		commonCode.setCd(code);
		commonCode.setCdNm(codeName);
		return commonCode;
	}

	// 매매등록 요청을 생성합니다.
	private WorkStockSaleCreateRequestVO createStockSaleCreateRequest(Integer saleCnt, Long saleAmt, Long profitAmt) {
		WorkStockSaleCreateRequestVO request = new WorkStockSaleCreateRequestVO();
		request.setSaleDt("2026-06-10");
		request.setStockAccountCd("ACCOUNT_A");
		request.setStockNmCd("STOCK_A");
		request.setSaleCnt(saleCnt);
		request.setSaleAmt(saleAmt);
		request.setProfitAmt(profitAmt);
		request.setMemo("");
		return request;
	}

	// 종목별 기존 합계 행을 생성합니다.
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

	// 매매 원천 거래 행을 생성합니다.
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
