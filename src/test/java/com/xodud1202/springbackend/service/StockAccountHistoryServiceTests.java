package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.common.CommonCodeVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockAccountCashHistoryCreateRequestVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockAccountCheckRowVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockAccountDailyHistoryRowVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockAccountDailySaleAmountVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockAccountHistoryAccountGroupVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockAccountHistoryResponseVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockAccountHistorySearchPO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockAccountHistoryValueRowVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockAccountMonthlyCashAmountVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockAccountMonthlySaleAmountVO;
import com.xodud1202.springbackend.mapper.CommonMapper;
import com.xodud1202.springbackend.mapper.StockAccountHistoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// StockAccountHistoryService의 주식계좌이력 집계 계산을 검증합니다.
class StockAccountHistoryServiceTests {
	@Mock
	private CommonMapper commonMapper;

	@Mock
	private StockAccountHistoryMapper stockAccountHistoryMapper;

	@InjectMocks
	private StockAccountHistoryService stockAccountHistoryService;

	@Test
	@DisplayName("월별 정보: 최신 확인월부터 이전까지 계좌별 누적 원금과 마지막 확인금액 계산")
	// 월별 표의 계좌별 누적 원금, 확인금액, 전체 요약 합계를 확인합니다.
	void getStockAccountHistory_returnsMonthlyAccountHistory() {
		// 2026년 07월까지 월 컬럼이 필요한 원천 데이터를 구성합니다.
		stubAccountList();
		when(stockAccountHistoryMapper.getLatestStockAccountHistoryDate(any())).thenReturn("20260720");
		when(stockAccountHistoryMapper.getLatestStockSaleDate(any())).thenReturn("20260707");
		when(stockAccountHistoryMapper.getMonthlySaleAmountList(any())).thenReturn(List.of(
			createMonthlySale("BEFORE", "ACCOUNT_A", 1000L, 0L, 1000L),
			createMonthlySale("202606", "ACCOUNT_A", 500L, 200L, 300L),
			createMonthlySale("202606", "ACCOUNT_B", 1000L, 0L, 1000L),
			createMonthlySale("202607", "ACCOUNT_B", 0L, 100L, -100L)
		));
		when(stockAccountHistoryMapper.getMonthlyCashAmountList(any())).thenReturn(List.of(
			createMonthlyCash("BEFORE", "ACCOUNT_A", 1000L, 0L),
			createMonthlyCash("202606", "ACCOUNT_A", 500L, 200L),
			createMonthlyCash("202606", "ACCOUNT_B", 1000L, 0L),
			createMonthlyCash("202607", "ACCOUNT_B", 0L, 100L)
		));
		when(stockAccountHistoryMapper.getStockAccountCheckRowList(any())).thenReturn(List.of(
			createCheckRow(1L, "20260610", "202606", "ACCOUNT_A", 1400L),
			createCheckRow(2L, "20260620", "202606", "ACCOUNT_A", 1500L),
			createCheckRow(3L, "20260705", "202607", "ACCOUNT_B", 1300L)
		));
		when(stockAccountHistoryMapper.getDailySaleAmountList(any())).thenReturn(List.of());

		// 주식계좌이력 응답의 월별 값을 검증합니다.
		WorkStockAccountHistoryResponseVO response = stockAccountHistoryService.getStockAccountHistory(List.of("ACCOUNT_A", "ACCOUNT_B"), null);
		assertEquals(List.of("2026.07", "2026.06", "이전"), response.getMonthList().stream().map(month -> month.getMonthLabel()).toList());
		assertEquals(new BigDecimal("2300"), findSummaryRow(response, "totalPrincipalAmt").getValueMap().get("202606"));
		assertEquals(new BigDecimal("2200"), findSummaryRow(response, "totalPrincipalAmt").getValueMap().get("202607"));
		assertEquals(new BigDecimal("0"), findSummaryRow(response, "totalProfitAmt").getValueMap().get("BEFORE"));
		assertEquals(new BigDecimal("-800"), findSummaryRow(response, "totalProfitAmt").getValueMap().get("202606"));
		assertEquals(new BigDecimal("-34.78"), findSummaryRow(response, "totalProfitRate").getValueMap().get("202606"));

		WorkStockAccountHistoryAccountGroupVO accountAGroup = findAccountGroup(response, "ACCOUNT_A");
		assertEquals(new BigDecimal("0"), findAccountRow(accountAGroup, "profitAmt").getValueMap().get("BEFORE"));
		assertEquals(new BigDecimal("500"), findAccountRow(accountAGroup, "depositAmt").getValueMap().get("202606"));
		assertEquals(new BigDecimal("200"), findAccountRow(accountAGroup, "withdrawAmt").getValueMap().get("202606"));
		assertEquals(new BigDecimal("1300"), findAccountRow(accountAGroup, "principalAmt").getValueMap().get("202606"));
		assertEquals(new BigDecimal("1500"), findAccountRow(accountAGroup, "checkAmt").getValueMap().get("202606"));
		assertEquals(new BigDecimal("200"), findAccountRow(accountAGroup, "profitAmt").getValueMap().get("202606"));
	}

	@Test
	@DisplayName("월별 정보: 삼성증권 종합 2026년 06월 총원금액은 입출금 누적으로 계산")
	// 총원금액이 매매금액이 아닌 입출금 금액의 누적으로 계산되는지 확인합니다.
	void getStockAccountHistory_usesCashHistoryForPrincipalAmount() {
		// 삼성증권 종합의 2026년 06월 이전 입출금 누적 예시를 구성합니다.
		stubSamsungAccountList();
		when(stockAccountHistoryMapper.getLatestStockAccountHistoryDate(any())).thenReturn("20260707");
		when(stockAccountHistoryMapper.getLatestStockSaleDate(any())).thenReturn("20260707");
		when(stockAccountHistoryMapper.getMonthlySaleAmountList(any())).thenReturn(List.of(
			createMonthlySale("BEFORE", "STOCK_ACCOUNT_04", 5429715L, 0L, 5429715L),
			createMonthlySale("202606", "STOCK_ACCOUNT_04", 386532L, 0L, 386532L)
		));
		when(stockAccountHistoryMapper.getMonthlyCashAmountList(any())).thenReturn(List.of(
			createMonthlyCash("BEFORE", "STOCK_ACCOUNT_04", 5445165L, 0L),
			createMonthlyCash("202606", "STOCK_ACCOUNT_04", 903000L, 531700L)
		));
		when(stockAccountHistoryMapper.getStockAccountCheckRowList(any())).thenReturn(List.of());
		when(stockAccountHistoryMapper.getDailySaleAmountList(any())).thenReturn(List.of());

		// 매매금액 누적 5,816,247이 아니라 입출금 누적 5,816,465가 총원금액입니다.
		WorkStockAccountHistoryResponseVO response = stockAccountHistoryService.getStockAccountHistory(List.of("STOCK_ACCOUNT_04"), null);
		WorkStockAccountHistoryAccountGroupVO accountGroup = findAccountGroup(response, "STOCK_ACCOUNT_04");
		assertEquals(new BigDecimal("5816465"), findAccountRow(accountGroup, "principalAmt").getValueMap().get("202606"));
		assertEquals(new BigDecimal("5816465"), findSummaryRow(response, "totalPrincipalAmt").getValueMap().get("202606"));
	}

	@Test
	@DisplayName("전체 이력 정보: 확인일 기준으로 누적 원금과 확인 평가금 합계를 계산")
	// 확인금액이 있는 날짜별 이력 행의 원금, 평가금, 손익을 확인합니다.
	void getStockAccountHistory_returnsDailyHistoryByCheckDate() {
		// 확인일 앞뒤로 입출금 순원금이 누적되는 원천 데이터를 구성합니다.
		stubAccountList();
		when(stockAccountHistoryMapper.getLatestStockAccountHistoryDate(any())).thenReturn("20260620");
		when(stockAccountHistoryMapper.getLatestStockSaleDate(any())).thenReturn("20260615");
		when(stockAccountHistoryMapper.getMonthlySaleAmountList(any())).thenReturn(List.of());
		when(stockAccountHistoryMapper.getMonthlyCashAmountList(any())).thenReturn(List.of());
		List<WorkStockAccountCheckRowVO> selectedCheckRowList = List.of(
			createCheckRow(1L, "20260610", "202606", "ACCOUNT_A", 1500L),
			createCheckRow(2L, "20260620", "202606", "ACCOUNT_A", 1800L)
		);
		List<WorkStockAccountCheckRowVO> activeCheckRowList = List.of(
			createCheckRow(1L, "20260610", "202606", "ACCOUNT_A", 1500L),
			createCheckRow(2L, "20260620", "202606", "ACCOUNT_A", 1800L),
			createCheckRow(3L, "20260620", "202606", "ACCOUNT_B", 700L)
		);
		when(stockAccountHistoryMapper.getStockAccountCheckRowList(any()))
			.thenReturn(selectedCheckRowList)
			.thenReturn(activeCheckRowList);
		when(stockAccountHistoryMapper.getDailySaleAmountList(any())).thenReturn(List.of(
			createDailySale("20260601", 1000L),
			createDailySale("20260615", 500L)
		));

		// 최신 확인일이 먼저 노출되고 해당 날짜까지의 누적 원금을 사용합니다.
		WorkStockAccountHistoryResponseVO response = stockAccountHistoryService.getStockAccountHistory(List.of("ACCOUNT_A"), null);
		assertEquals(2, response.getHistoryTotalCount());
		assertEquals(false, response.getHistoryHasMore());
		WorkStockAccountDailyHistoryRowVO latestRow = response.getHistoryRowList().get(0);
		assertEquals("2026-06-20", latestRow.getCheckDt());
		assertEquals(1500L, latestRow.getPrincipalAmt());
		assertEquals(1800L, latestRow.getCheckAmt());
		assertEquals(300L, latestRow.getProfitAmt());
		assertEquals(new BigDecimal("20.00"), latestRow.getProfitRate());
		assertEquals(1800L, latestRow.getCheckAccountAmountMap().get("ACCOUNT_A"));
		assertEquals(700L, latestRow.getCheckAccountAmountMap().get("ACCOUNT_B"));
	}

	@Test
	@DisplayName("입출금 내역: 선택 계좌의 최신순 목록을 50건 단위로 조회")
	// 입출금 내역 탭의 스크롤 페이지는 51건을 조회해 다음 페이지 존재 여부를 판단합니다.
	void getStockAccountHistory_returnsCashHistoryRowsByPage() {
		// 선택 계좌와 51건의 입출금 조회 결과를 구성합니다.
		stubAccountList();
		List<WorkStockAccountCashHistoryCreateRequestVO> cashHistoryRowList = new ArrayList<>();
		for (int index = 0; index < 51; index++) {
			cashHistoryRowList.add(createCashHistoryRow(100L + index, "20260730", "ACCOUNT_A", "계좌A", "CASH_IN_OUT_01", "입금", 1000L + index));
		}
		when(stockAccountHistoryMapper.getCashHistoryRowList(any())).thenReturn(cashHistoryRowList);

		// 화면에는 50건만 반환하고 날짜는 date input과 같은 형식으로 변환합니다.
		WorkStockAccountHistoryResponseVO response = stockAccountHistoryService.getStockAccountHistory(List.of("ACCOUNT_A"), null, 0);
		ArgumentCaptor<WorkStockAccountHistorySearchPO> searchCaptor = ArgumentCaptor.forClass(WorkStockAccountHistorySearchPO.class);
		verify(stockAccountHistoryMapper).getCashHistoryRowList(searchCaptor.capture());
		assertEquals(List.of("ACCOUNT_A"), searchCaptor.getValue().getStockAccountCdList());
		assertEquals(0, searchCaptor.getValue().getCashHistoryOffset());
		assertEquals(51, searchCaptor.getValue().getCashHistoryLimit());
		assertEquals(50, response.getCashHistoryRowList().size());
		assertEquals(50, response.getCashHistoryPageSize());
		assertEquals(true, response.getCashHistoryHasMore());
		assertEquals("2026-07-30", response.getCashHistoryRowList().get(0).getCashDt());
		assertEquals(100L, response.getCashHistoryRowList().get(0).getCashHistSeq());
	}

	@Test
	@DisplayName("손익 계산: 확인 평가금이 0이면 월별과 일별 손익을 0으로 반환")
	// 확인 평가금이 없는 행은 원금이 있어도 손익금과 손익율을 0으로 계산합니다.
	void getStockAccountHistory_returnsZeroProfitWhenCheckAmountIsZero() {
		// 원금은 있지만 확인 평가금이 0인 월별과 일별 원천 데이터를 구성합니다.
		stubAccountList();
		when(stockAccountHistoryMapper.getLatestStockAccountHistoryDate(any())).thenReturn("20260620");
		when(stockAccountHistoryMapper.getLatestStockSaleDate(any())).thenReturn("20260615");
		when(stockAccountHistoryMapper.getMonthlySaleAmountList(any())).thenReturn(List.of(
			createMonthlySale("202606", "ACCOUNT_A", 1000L, 0L, 1000L)
		));
		when(stockAccountHistoryMapper.getMonthlyCashAmountList(any())).thenReturn(List.of(
			createMonthlyCash("202606", "ACCOUNT_A", 1000L, 0L)
		));
		when(stockAccountHistoryMapper.getStockAccountCheckRowList(any())).thenReturn(List.of(
			createCheckRow(1L, "20260620", "202606", "ACCOUNT_A", 0L)
		));
		when(stockAccountHistoryMapper.getDailySaleAmountList(any())).thenReturn(List.of(
			createDailySale("20260615", 1000L)
		));

		// 월별과 일별 모두 확인 평가금 0이면 손익 값도 0으로 고정됩니다.
		WorkStockAccountHistoryResponseVO response = stockAccountHistoryService.getStockAccountHistory(List.of("ACCOUNT_A"), null);
		WorkStockAccountHistoryAccountGroupVO accountAGroup = findAccountGroup(response, "ACCOUNT_A");
		assertEquals(new BigDecimal("0"), findAccountRow(accountAGroup, "profitAmt").getValueMap().get("202606"));
		assertEquals(new BigDecimal("0.00"), findAccountRow(accountAGroup, "profitRate").getValueMap().get("202606"));
		assertEquals(new BigDecimal("0"), findSummaryRow(response, "totalProfitAmt").getValueMap().get("202606"));
		assertEquals(new BigDecimal("0.00"), findSummaryRow(response, "totalProfitRate").getValueMap().get("202606"));
		assertEquals(0L, response.getHistoryRowList().get(0).getProfitAmt());
		assertEquals(new BigDecimal("0.00"), response.getHistoryRowList().get(0).getProfitRate());
	}

	@Test
	@DisplayName("계좌 확인 평가금 저장: 사용 중인 전체 계좌를 같은 날짜로 일괄 저장")
	// 계좌 확인 평가금 저장 시 전체 활성 계좌를 같은 확인일로 수정하거나 등록합니다.
	void saveStockAccountCheckAmountList_savesAllActiveAccounts() {
		// 활성 계좌와 계좌별 수정 결과를 구성합니다.
		stubAccountList();
		List<WorkStockAccountCheckRowVO> requestList = List.of(
			createCheckSaveRequest("2026-07-08", "ACCOUNT_A", 12345L),
			createCheckSaveRequest("2026-07-08", "ACCOUNT_B", 23456L)
		);
		when(stockAccountHistoryMapper.updateStockAccountCheckAmount(any())).thenReturn(1, 0);

		// 확인일은 yyyyMMdd로 정규화되고 기존 행이 없던 계좌만 등록됩니다.
		int savedCount = stockAccountHistoryService.saveStockAccountCheckAmountList(requestList, 9L);
		ArgumentCaptor<WorkStockAccountCheckRowVO> updateCaptor = ArgumentCaptor.forClass(WorkStockAccountCheckRowVO.class);
		ArgumentCaptor<WorkStockAccountCheckRowVO> insertCaptor = ArgumentCaptor.forClass(WorkStockAccountCheckRowVO.class);
		verify(stockAccountHistoryMapper, times(2)).updateStockAccountCheckAmount(updateCaptor.capture());
		verify(stockAccountHistoryMapper).insertStockAccountCheckAmount(insertCaptor.capture());
		assertEquals(2, savedCount);
		assertEquals(List.of("ACCOUNT_A", "ACCOUNT_B"), updateCaptor.getAllValues().stream().map(WorkStockAccountCheckRowVO::getStockAccountCd).toList());
		assertEquals(List.of("20260708", "20260708"), updateCaptor.getAllValues().stream().map(WorkStockAccountCheckRowVO::getCheckDt).toList());
		assertEquals(List.of(12345L, 23456L), updateCaptor.getAllValues().stream().map(WorkStockAccountCheckRowVO::getStockTotalAmt).toList());
		assertEquals(9L, updateCaptor.getAllValues().get(0).getRegNo());
		assertEquals(9L, updateCaptor.getAllValues().get(0).getUdtNo());
		assertEquals("ACCOUNT_B", insertCaptor.getValue().getStockAccountCd());
	}

	@Test
	@DisplayName("계좌 확인 평가금 저장: 사용 중인 계좌가 누락되면 거부")
	// 계좌 확인 평가금 일괄 저장 요청은 활성 계좌 전체를 포함해야 합니다.
	void saveStockAccountCheckAmountList_rejectsMissingActiveAccount() {
		// 활성 계좌 중 하나만 요청해 누락 상황을 구성합니다.
		stubAccountList();
		List<WorkStockAccountCheckRowVO> requestList = List.of(
			createCheckSaveRequest("2026-07-08", "ACCOUNT_A", 12345L)
		);

		// 누락된 활성 계좌가 있으면 저장 쿼리를 실행하지 않습니다.
		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> stockAccountHistoryService.saveStockAccountCheckAmountList(requestList, 9L)
		);
		verify(stockAccountHistoryMapper, never()).updateStockAccountCheckAmount(any());
		verify(stockAccountHistoryMapper, never()).insertStockAccountCheckAmount(any());
		assertEquals("모든 사용 계좌의 확인금액을 입력해주세요.", exception.getMessage());
	}

	@Test
	@DisplayName("계좌 확인 평가금 저장: 확인일이 여러 개면 거부")
	// 계좌 확인 평가금 일괄 저장 요청은 하나의 확인일만 사용해야 합니다.
	void saveStockAccountCheckAmountList_rejectsDifferentCheckDate() {
		// 활성 계좌 전체를 포함하지만 확인일이 서로 다른 요청을 구성합니다.
		stubAccountList();
		List<WorkStockAccountCheckRowVO> requestList = List.of(
			createCheckSaveRequest("2026-07-08", "ACCOUNT_A", 12345L),
			createCheckSaveRequest("2026-07-09", "ACCOUNT_B", 23456L)
		);

		// 확인일이 여러 개면 저장 쿼리를 실행하지 않습니다.
		IllegalArgumentException exception = assertThrows(
			IllegalArgumentException.class,
			() -> stockAccountHistoryService.saveStockAccountCheckAmountList(requestList, 9L)
		);
		verify(stockAccountHistoryMapper, never()).updateStockAccountCheckAmount(any());
		verify(stockAccountHistoryMapper, never()).insertStockAccountCheckAmount(any());
		assertEquals("확인일자는 하나만 선택해주세요.", exception.getMessage());
	}

	@Test
	@DisplayName("계좌 입출금 등록: 모든 요청은 신규 입출금 이력으로 등록")
	// 입출금 등록은 날짜와 계좌 중복 여부와 관계없이 신규 이력으로 등록합니다.
	void createStockAccountCashHistory_insertsCashHistory() {
		// 활성 계좌와 입출금구분 공통코드를 구성합니다.
		stubAccountList();
		when(commonMapper.getCommonCodeList(eq("CASH_IN_OUT"))).thenReturn(List.of(
			createCommonCode("CASH_IN_OUT_01", "입금"),
			createCommonCode("CASH_IN_OUT_02", "출금")
		));
		WorkStockAccountCashHistoryCreateRequestVO request = createCashHistoryRequest("2026-07-08", "ACCOUNT_A", "CASH_IN_OUT_01", 1000L);

		// 입출금 요청 값은 저장용 날짜와 사용자 번호로 정규화됩니다.
		stockAccountHistoryService.createStockAccountCashHistory(request, 9L);
		ArgumentCaptor<WorkStockAccountCashHistoryCreateRequestVO> insertCaptor = ArgumentCaptor.forClass(WorkStockAccountCashHistoryCreateRequestVO.class);
		verify(stockAccountHistoryMapper).insertStockAccountCashHistory(insertCaptor.capture());
		assertEquals("20260708", insertCaptor.getValue().getCashDt());
		assertEquals("ACCOUNT_A", insertCaptor.getValue().getStockAccountCd());
		assertEquals("CASH_IN_OUT_01", insertCaptor.getValue().getCashInOutCd());
		assertEquals(1000L, insertCaptor.getValue().getCashAmt());
		assertEquals(9L, insertCaptor.getValue().getRegNo());
		assertEquals(9L, insertCaptor.getValue().getUdtNo());
	}

	@Test
	@DisplayName("계좌 입출금 수정: 기존 입출금 이력을 식별자로 수정")
	// 입출금 수정은 기존 CASH_HIST_SEQ 행의 일자, 계좌, 구분, 금액을 갱신합니다.
	void updateStockAccountCashHistory_updatesCashHistory() {
		// 활성 계좌와 입출금구분 공통코드, 수정 대상 요청을 구성합니다.
		stubAccountList();
		when(commonMapper.getCommonCodeList(eq("CASH_IN_OUT"))).thenReturn(List.of(
			createCommonCode("CASH_IN_OUT_01", "입금"),
			createCommonCode("CASH_IN_OUT_02", "출금")
		));
		when(stockAccountHistoryMapper.updateStockAccountCashHistory(any())).thenReturn(1);
		WorkStockAccountCashHistoryCreateRequestVO request = createCashHistoryRequest("2026-07-08", "ACCOUNT_A", "CASH_IN_OUT_02", 2000L);
		request.setCashHistSeq(15L);

		// 수정 요청 값은 저장용 날짜와 사용자 번호로 정규화됩니다.
		stockAccountHistoryService.updateStockAccountCashHistory(request, 9L);
		ArgumentCaptor<WorkStockAccountCashHistoryCreateRequestVO> updateCaptor = ArgumentCaptor.forClass(WorkStockAccountCashHistoryCreateRequestVO.class);
		verify(stockAccountHistoryMapper).updateStockAccountCashHistory(updateCaptor.capture());
		assertEquals(15L, updateCaptor.getValue().getCashHistSeq());
		assertEquals("20260708", updateCaptor.getValue().getCashDt());
		assertEquals("ACCOUNT_A", updateCaptor.getValue().getStockAccountCd());
		assertEquals("CASH_IN_OUT_02", updateCaptor.getValue().getCashInOutCd());
		assertEquals(2000L, updateCaptor.getValue().getCashAmt());
		assertEquals(9L, updateCaptor.getValue().getUdtNo());
	}

	// 테스트 계좌 공통코드 목록을 구성합니다.
	private void stubAccountList() {
		when(commonMapper.getCommonCodeList(eq("STOCK_ACCOUNT"))).thenReturn(List.of(
			createCommonCode("ACCOUNT_A", "계좌A"),
			createCommonCode("ACCOUNT_B", "계좌B")
		));
	}

	// 삼성증권 종합 테스트 계좌 공통코드 목록을 구성합니다.
	private void stubSamsungAccountList() {
		when(commonMapper.getCommonCodeList(eq("STOCK_ACCOUNT"))).thenReturn(List.of(
			createCommonCode("STOCK_ACCOUNT_04", "삼성증권 종합")
		));
	}

	// 공통코드 테스트 객체를 생성합니다.
	private CommonCodeVO createCommonCode(String code, String codeName) {
		CommonCodeVO commonCode = new CommonCodeVO();
		commonCode.setCd(code);
		commonCode.setCdNm(codeName);
		return commonCode;
	}

	// 월별 매매금액 테스트 객체를 생성합니다.
	private WorkStockAccountMonthlySaleAmountVO createMonthlySale(String monthKey, String accountCode, long buyAmt, long sellAmt, long netSaleAmt) {
		WorkStockAccountMonthlySaleAmountVO saleAmount = new WorkStockAccountMonthlySaleAmountVO();
		saleAmount.setMonthKey(monthKey);
		saleAmount.setStockAccountCd(accountCode);
		saleAmount.setBuyAmt(buyAmt);
		saleAmount.setSellAmt(sellAmt);
		saleAmount.setNetSaleAmt(netSaleAmt);
		return saleAmount;
	}

	// 월별 입출금 테스트 객체를 생성합니다.
	private WorkStockAccountMonthlyCashAmountVO createMonthlyCash(String monthKey, String accountCode, long depositAmt, long withdrawAmt) {
		WorkStockAccountMonthlyCashAmountVO cashAmount = new WorkStockAccountMonthlyCashAmountVO();
		cashAmount.setMonthKey(monthKey);
		cashAmount.setStockAccountCd(accountCode);
		cashAmount.setDepositAmt(depositAmt);
		cashAmount.setWithdrawAmt(withdrawAmt);
		return cashAmount;
	}

	// 확인금액 테스트 객체를 생성합니다.
	private WorkStockAccountCheckRowVO createCheckRow(Long checkSeq, String checkDt, String monthKey, String accountCode, long checkAmt) {
		WorkStockAccountCheckRowVO checkRow = new WorkStockAccountCheckRowVO();
		checkRow.setStockCheckSeq(checkSeq);
		checkRow.setCheckDt(checkDt);
		checkRow.setMonthKey(monthKey);
		checkRow.setStockAccountCd(accountCode);
		checkRow.setStockTotalAmt(checkAmt);
		return checkRow;
	}

	// 확인금액 저장 테스트 객체를 생성합니다.
	private WorkStockAccountCheckRowVO createCheckSaveRequest(String checkDt, String accountCode, long checkAmt) {
		WorkStockAccountCheckRowVO request = new WorkStockAccountCheckRowVO();
		request.setCheckDt(checkDt);
		request.setStockAccountCd(accountCode);
		request.setStockTotalAmt(checkAmt);
		return request;
	}

	// 입출금 등록 테스트 객체를 생성합니다.
	private WorkStockAccountCashHistoryCreateRequestVO createCashHistoryRequest(String cashDt, String accountCode, String cashInOutCd, long cashAmt) {
		WorkStockAccountCashHistoryCreateRequestVO request = new WorkStockAccountCashHistoryCreateRequestVO();
		request.setCashDt(cashDt);
		request.setStockAccountCd(accountCode);
		request.setCashInOutCd(cashInOutCd);
		request.setCashAmt(cashAmt);
		return request;
	}

	// 입출금 목록 테스트 객체를 생성합니다.
	private WorkStockAccountCashHistoryCreateRequestVO createCashHistoryRow(
		Long cashHistSeq,
		String cashDt,
		String accountCode,
		String accountName,
		String cashInOutCd,
		String cashInOutName,
		long cashAmt
	) {
		WorkStockAccountCashHistoryCreateRequestVO row = createCashHistoryRequest(cashDt, accountCode, cashInOutCd, cashAmt);
		row.setCashHistSeq(cashHistSeq);
		row.setStockAccountNm(accountName);
		row.setCashInOutNm(cashInOutName);
		return row;
	}

	// 일별 입출금 순원금 테스트 객체를 생성합니다.
	private WorkStockAccountDailySaleAmountVO createDailySale(String saleDt, long netSaleAmt) {
		WorkStockAccountDailySaleAmountVO saleAmount = new WorkStockAccountDailySaleAmountVO();
		saleAmount.setSaleDt(saleDt);
		saleAmount.setNetSaleAmt(netSaleAmt);
		return saleAmount;
	}

	// 전체 요약 행을 찾습니다.
	private WorkStockAccountHistoryValueRowVO findSummaryRow(WorkStockAccountHistoryResponseVO response, String rowKey) {
		return response.getSummaryRowList().stream()
			.filter(row -> rowKey.equals(row.getRowKey()))
			.findFirst()
			.orElseThrow();
	}

	// 계좌별 행 묶음을 찾습니다.
	private WorkStockAccountHistoryAccountGroupVO findAccountGroup(WorkStockAccountHistoryResponseVO response, String accountCode) {
		return response.getAccountGroupList().stream()
			.filter(group -> accountCode.equals(group.getStockAccountCd()))
			.findFirst()
			.orElseThrow();
	}

	// 계좌별 지표 행을 찾습니다.
	private WorkStockAccountHistoryValueRowVO findAccountRow(WorkStockAccountHistoryAccountGroupVO accountGroup, String rowKey) {
		return accountGroup.getRowList().stream()
			.filter(row -> rowKey.equals(row.getRowKey()))
			.findFirst()
			.orElseThrow();
	}
}
