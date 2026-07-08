package com.xodud1202.springbackend.service;

import static com.xodud1202.springbackend.common.util.CommonTextUtils.*;

import com.xodud1202.springbackend.domain.common.CommonCodeVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockAccountCashHistoryCreateRequestVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockAccountCheckRowVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockAccountDailyHistoryRowVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockAccountDailySaleAmountVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockAccountHistoryAccountGroupVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockAccountHistoryMonthVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockAccountHistoryResponseVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockAccountHistorySearchPO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockAccountHistoryValueRowVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockAccountMonthlyCashAmountVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockAccountMonthlySaleAmountVO;
import com.xodud1202.springbackend.mapper.CommonMapper;
import com.xodud1202.springbackend.mapper.StockAccountHistoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
// 주식계좌이력 화면의 월별 계좌 집계와 확인일별 이력 계산을 처리합니다.
public class StockAccountHistoryService {
	private static final String STOCK_ACCOUNT_GROUP_CODE = "STOCK_ACCOUNT";
	private static final String CASH_IN_OUT_GROUP_CODE = "CASH_IN_OUT";
	private static final String MONTH_KEY_BEFORE = "BEFORE";
	private static final String VALUE_TYPE_AMOUNT = "AMOUNT";
	private static final String VALUE_TYPE_RATE = "RATE";
	private static final int HISTORY_DEFAULT_OFFSET = 0;
	private static final int HISTORY_PAGE_SIZE = 50;
	private static final int CASH_HISTORY_PAGE_SIZE = 50;
	private static final BigDecimal ZERO_RATE = BigDecimal.ZERO.setScale(2);
	private static final YearMonth MONTHLY_START_MONTH = YearMonth.of(2026, 6);
	private static final DateTimeFormatter REQUEST_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
	private static final DateTimeFormatter DB_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
	private static final DateTimeFormatter MONTH_KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
	private static final DateTimeFormatter MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM");

	private final CommonMapper commonMapper;
	private final StockAccountHistoryMapper stockAccountHistoryMapper;

	// 월별 계산 중 계좌별 상태를 담습니다.
	private static final class AccountMonthlyState {
		private final Map<String, Long> buyAmountMap = new LinkedHashMap<>();
		private final Map<String, Long> sellAmountMap = new LinkedHashMap<>();
		private final Map<String, Long> depositAmountMap = new LinkedHashMap<>();
		private final Map<String, Long> withdrawAmountMap = new LinkedHashMap<>();
		private final Map<String, Long> principalAmountMap = new LinkedHashMap<>();
		private final Map<String, Long> checkAmountMap = new LinkedHashMap<>();
		private final Map<String, Long> profitAmountMap = new LinkedHashMap<>();
		private final Map<String, BigDecimal> profitRateMap = new LinkedHashMap<>();
	}

	// 주식계좌이력 화면 데이터를 조회합니다.
	public WorkStockAccountHistoryResponseVO getStockAccountHistory(
		List<String> stockAccountCdList,
		Integer historyOffset
	) {
		return getStockAccountHistory(stockAccountCdList, historyOffset, null);
	}

	// 주식계좌이력 화면 데이터를 조회합니다.
	public WorkStockAccountHistoryResponseVO getStockAccountHistory(
		List<String> stockAccountCdList,
		Integer historyOffset,
		Integer cashHistoryOffset
	) {
		List<CommonCodeVO> activeAccountList = getStockAccountList();
		List<CommonCodeVO> accountList = getSelectedAccountList(stockAccountCdList, activeAccountList);
		WorkStockAccountHistorySearchPO searchParam = buildSearchParam(accountList);
		List<WorkStockAccountHistoryMonthVO> monthList = buildMonthList(searchParam);
		Map<String, WorkStockAccountMonthlySaleAmountVO> saleAmountMap = buildMonthlySaleAmountMap(
			stockAccountHistoryMapper.getMonthlySaleAmountList(searchParam)
		);
		Map<String, WorkStockAccountMonthlyCashAmountVO> cashAmountMap = buildMonthlyCashAmountMap(
			stockAccountHistoryMapper.getMonthlyCashAmountList(searchParam)
		);
		List<WorkStockAccountCheckRowVO> checkRowList = stockAccountHistoryMapper.getStockAccountCheckRowList(searchParam);
		Map<String, Long> monthlyCheckAmountMap = buildMonthlyCheckAmountMap(checkRowList);
		Map<String, AccountMonthlyState> accountMonthlyStateMap = buildAccountMonthlyStateMap(
			accountList,
			monthList,
			saleAmountMap,
			cashAmountMap,
			monthlyCheckAmountMap
		);
		List<WorkStockAccountHistoryAccountGroupVO> accountGroupList = buildAccountGroupList(accountList, accountMonthlyStateMap);
		List<WorkStockAccountHistoryValueRowVO> summaryRowList = buildSummaryRowList(monthList, accountMonthlyStateMap);
		List<WorkStockAccountCheckRowVO> activeCheckRowList = accountList.size() == activeAccountList.size()
			? checkRowList
			: stockAccountHistoryMapper.getStockAccountCheckRowList(buildSearchParam(activeAccountList));
		List<WorkStockAccountDailyHistoryRowVO> allHistoryRowList = buildDailyHistoryRowList(
			checkRowList,
			stockAccountHistoryMapper.getDailySaleAmountList(searchParam),
			activeCheckRowList,
			activeAccountList
		);
		int resolvedOffset = normalizeHistoryOffset(historyOffset);
		List<WorkStockAccountDailyHistoryRowVO> pageHistoryRowList = sliceHistoryRowList(allHistoryRowList, resolvedOffset);
		int resolvedCashHistoryOffset = normalizeHistoryOffset(cashHistoryOffset);
		List<WorkStockAccountCashHistoryCreateRequestVO> cashHistoryRowList = buildCashHistoryRowList(
			accountList,
			resolvedCashHistoryOffset
		);

		WorkStockAccountHistoryResponseVO response = new WorkStockAccountHistoryResponseVO();
		response.setMonthList(monthList);
		response.setSummaryRowList(summaryRowList);
		response.setAccountGroupList(accountGroupList);
		response.setHistoryRowList(pageHistoryRowList);
		response.setHistoryTotalCount(allHistoryRowList.size());
		response.setHistoryPageSize(HISTORY_PAGE_SIZE);
		response.setHistoryHasMore(resolvedOffset + pageHistoryRowList.size() < allHistoryRowList.size());
		response.setCashHistoryRowList(sliceCashHistoryRowList(cashHistoryRowList));
		response.setCashHistoryPageSize(CASH_HISTORY_PAGE_SIZE);
		response.setCashHistoryHasMore(cashHistoryRowList.size() > CASH_HISTORY_PAGE_SIZE);
		return response;
	}

	@Transactional
	// 사용 중인 전체 계좌의 확인 평가금을 같은 날짜 기준으로 저장합니다.
	public int saveStockAccountCheckAmountList(List<WorkStockAccountCheckRowVO> requestList, Long workUserNo) {
		if (requestList == null || requestList.isEmpty()) {
			throw new IllegalArgumentException("저장할 계좌확인금액을 입력해주세요.");
		}

		List<CommonCodeVO> activeAccountList = getStockAccountList();
		if (activeAccountList.isEmpty()) {
			throw new IllegalArgumentException("사용 중인 계좌가 없습니다.");
		}
		List<WorkStockAccountCheckRowVO> paramList = buildCheckAmountSaveParamList(requestList, activeAccountList, workUserNo);
		for (WorkStockAccountCheckRowVO param : paramList) {
			saveStockAccountCheckAmount(param);
		}
		return paramList.size();
	}

	// 계좌 확인 평가금을 계좌와 날짜 기준으로 수정하거나 등록합니다.
	private boolean saveStockAccountCheckAmount(WorkStockAccountCheckRowVO param) {
		int updatedCount = stockAccountHistoryMapper.updateStockAccountCheckAmount(param);
		if (updatedCount > 0) {
			return false;
		}
		stockAccountHistoryMapper.insertStockAccountCheckAmount(param);
		return true;
	}

	@Transactional
	// 계좌 입출금 이력을 등록합니다.
	public void createStockAccountCashHistory(WorkStockAccountCashHistoryCreateRequestVO request, Long workUserNo) {
		if (request == null) {
			throw new IllegalArgumentException("등록할 입출금 내역을 입력해주세요.");
		}

		WorkStockAccountCashHistoryCreateRequestVO param = buildCashHistorySaveParam(request, workUserNo);
		stockAccountHistoryMapper.insertStockAccountCashHistory(param);
	}

	@Transactional
	// 계좌 입출금 이력을 수정합니다.
	public void updateStockAccountCashHistory(WorkStockAccountCashHistoryCreateRequestVO request, Long workUserNo) {
		if (request == null) {
			throw new IllegalArgumentException("수정할 입출금 내역을 입력해주세요.");
		}
		validateCashHistSeq(request.getCashHistSeq());

		WorkStockAccountCashHistoryCreateRequestVO param = buildCashHistorySaveParam(request, workUserNo);
		param.setCashHistSeq(request.getCashHistSeq());
		int updatedCount = stockAccountHistoryMapper.updateStockAccountCashHistory(param);
		if (updatedCount <= 0) {
			throw new IllegalArgumentException("수정할 입출금 내역을 확인해주세요.");
		}
	}

	// 선택된 계좌가 없으면 활성 계좌 전체를 사용합니다.
	private List<CommonCodeVO> getSelectedAccountList(List<String> stockAccountCdList, List<CommonCodeVO> activeAccountList) {
		List<String> normalizedAccountCodeList = normalizeCodeList(stockAccountCdList);
		if (normalizedAccountCodeList.isEmpty()) {
			return activeAccountList;
		}

		Map<String, CommonCodeVO> activeAccountMap = new LinkedHashMap<>();
		for (CommonCodeVO accountItem : activeAccountList) {
			if (accountItem != null && trimToNull(accountItem.getCd()) != null) {
				activeAccountMap.put(accountItem.getCd(), accountItem);
			}
		}

		List<CommonCodeVO> selectedAccountList = new ArrayList<>();
		for (String accountCode : normalizedAccountCodeList) {
			CommonCodeVO accountItem = activeAccountMap.get(accountCode);
			if (accountItem == null) {
				throw new IllegalArgumentException("계좌를 확인해주세요.");
			}
			selectedAccountList.add(accountItem);
		}
		return selectedAccountList;
	}

	// 주식계좌 활성 공통코드 목록을 조회합니다.
	private List<CommonCodeVO> getStockAccountList() {
		List<CommonCodeVO> accountList = commonMapper.getCommonCodeList(STOCK_ACCOUNT_GROUP_CODE);
		return accountList == null ? List.of() : accountList;
	}

	// 활성 계좌 목록을 코드 기준 Map으로 변환합니다.
	private Map<String, CommonCodeVO> buildActiveAccountMap(List<CommonCodeVO> activeAccountList) {
		Map<String, CommonCodeVO> activeAccountMap = new LinkedHashMap<>();
		for (CommonCodeVO accountItem : activeAccountList == null ? List.<CommonCodeVO>of() : activeAccountList) {
			String accountCode = trimToNull(accountItem == null ? null : accountItem.getCd());
			if (accountCode != null) {
				activeAccountMap.put(accountCode, accountItem);
			}
		}
		return activeAccountMap;
	}

	// 입출금구분 활성 공통코드 목록을 조회합니다.
	private List<CommonCodeVO> getCashInOutCodeList() {
		List<CommonCodeVO> cashInOutCodeList = commonMapper.getCommonCodeList(CASH_IN_OUT_GROUP_CODE);
		return cashInOutCodeList == null ? List.of() : cashInOutCodeList;
	}

	// 전체 계좌 확인 평가금 저장 요청 값을 DB 저장 형식으로 정규화하고 검증합니다.
	private List<WorkStockAccountCheckRowVO> buildCheckAmountSaveParamList(
		List<WorkStockAccountCheckRowVO> requestList,
		List<CommonCodeVO> activeAccountList,
		Long workUserNo
	) {
		validateWorkUserNo(workUserNo);
		Map<String, CommonCodeVO> activeAccountMap = buildActiveAccountMap(activeAccountList);
		Map<String, WorkStockAccountCheckRowVO> requestMap = new LinkedHashMap<>();
		String commonCheckDt = null;
		for (WorkStockAccountCheckRowVO request : requestList) {
			if (request == null) {
				throw new IllegalArgumentException("계좌확인금액 입력값을 확인해주세요.");
			}
			String normalizedCheckDt = normalizeRequestDate(request.getCheckDt(), "확인일자를 확인해주세요.");
			if (commonCheckDt == null) {
				commonCheckDt = normalizedCheckDt;
			} else if (!commonCheckDt.equals(normalizedCheckDt)) {
				throw new IllegalArgumentException("확인일자는 하나만 선택해주세요.");
			}
			String normalizedAccountCd = trimToNull(request.getStockAccountCd());
			validateRequiredCode(normalizedAccountCd, activeAccountList, "계좌를 확인해주세요.");
			if (requestMap.containsKey(normalizedAccountCd)) {
				throw new IllegalArgumentException("계좌확인금액 계좌가 중복되었습니다.");
			}
			if (request.getStockTotalAmt() == null || request.getStockTotalAmt() < 0L) {
				throw new IllegalArgumentException("확인금액을 입력해주세요.");
			}
			requestMap.put(normalizedAccountCd, request);
		}

		List<WorkStockAccountCheckRowVO> paramList = new ArrayList<>();
		for (String activeAccountCd : activeAccountMap.keySet()) {
			WorkStockAccountCheckRowVO request = requestMap.get(activeAccountCd);
			if (request == null) {
				throw new IllegalArgumentException("모든 사용 계좌의 확인금액을 입력해주세요.");
			}
			WorkStockAccountCheckRowVO param = new WorkStockAccountCheckRowVO();
			param.setCheckDt(commonCheckDt);
			param.setStockAccountCd(activeAccountCd);
			param.setStockTotalAmt(request.getStockTotalAmt());
			param.setRegNo(workUserNo);
			param.setUdtNo(workUserNo);
			paramList.add(param);
		}
		return paramList;
	}

	// 입출금 요청 값을 DB 저장 형식으로 정규화하고 검증합니다.
	private WorkStockAccountCashHistoryCreateRequestVO buildCashHistorySaveParam(
		WorkStockAccountCashHistoryCreateRequestVO request,
		Long workUserNo
	) {
		validateWorkUserNo(workUserNo);
		String normalizedCashDt = normalizeRequestDate(request.getCashDt(), "입출금일자를 확인해주세요.");
		String normalizedAccountCd = trimToNull(request.getStockAccountCd());
		String normalizedCashInOutCd = trimToNull(request.getCashInOutCd());
		validateRequiredCode(normalizedAccountCd, getStockAccountList(), "계좌를 선택해주세요.");
		validateRequiredCode(normalizedCashInOutCd, getCashInOutCodeList(), "입출금구분을 선택해주세요.");
		if (request.getCashAmt() == null || request.getCashAmt() <= 0L) {
			throw new IllegalArgumentException("입출금액을 입력해주세요.");
		}

		WorkStockAccountCashHistoryCreateRequestVO param = new WorkStockAccountCashHistoryCreateRequestVO();
		param.setCashDt(normalizedCashDt);
		param.setStockAccountCd(normalizedAccountCd);
		param.setCashInOutCd(normalizedCashInOutCd);
		param.setCashAmt(request.getCashAmt());
		param.setRegNo(workUserNo);
		param.setUdtNo(workUserNo);
		return param;
	}

	// 입출금 이력 식별자를 검증합니다.
	private void validateCashHistSeq(Long cashHistSeq) {
		if (cashHistSeq == null || cashHistSeq <= 0) {
			throw new IllegalArgumentException("수정할 입출금 내역을 확인해주세요.");
		}
	}

	// 로그인 사용자 번호를 저장 가능 값으로 검증합니다.
	private void validateWorkUserNo(Long workUserNo) {
		if (workUserNo == null || workUserNo <= 0) {
			throw new IllegalArgumentException("로그인 정보를 확인해주세요.");
		}
	}

	// 화면 날짜 값을 DB의 yyyyMMdd 문자열로 변환합니다.
	private String normalizeRequestDate(String dateValue, String invalidMessage) {
		String normalizedDate = trimToNull(dateValue);
		if (normalizedDate == null) {
			throw new IllegalArgumentException(invalidMessage);
		}
		try {
			LocalDate date = LocalDate.parse(normalizedDate, REQUEST_DATE_FORMATTER);
			return date.format(DB_DATE_FORMATTER);
		} catch (DateTimeParseException exception) {
			throw new IllegalArgumentException(invalidMessage);
		}
	}

	// 필수 코드가 활성 공통코드 목록에 존재하는지 확인합니다.
	private void validateRequiredCode(String code, List<CommonCodeVO> optionList, String invalidMessage) {
		if (code == null) {
			throw new IllegalArgumentException(invalidMessage);
		}
		boolean exists = optionList.stream().anyMatch(optionItem -> code.equals(optionItem.getCd()));
		if (!exists) {
			throw new IllegalArgumentException(invalidMessage);
		}
	}

	// 선택 계좌 목록을 검색 파라미터로 변환합니다.
	private WorkStockAccountHistorySearchPO buildSearchParam(List<CommonCodeVO> accountList) {
		List<String> accountCodeList = new ArrayList<>();
		for (CommonCodeVO accountItem : accountList == null ? List.<CommonCodeVO>of() : accountList) {
			String accountCode = trimToNull(accountItem == null ? null : accountItem.getCd());
			if (accountCode != null) {
				accountCodeList.add(accountCode);
			}
		}

		WorkStockAccountHistorySearchPO searchParam = new WorkStockAccountHistorySearchPO();
		searchParam.setStockAccountCdList(accountCodeList);
		return searchParam;
	}

	// 월별 정보 탭의 컬럼 목록을 생성합니다.
	private List<WorkStockAccountHistoryMonthVO> buildMonthList(WorkStockAccountHistorySearchPO searchParam) {
		String latestAccountHistoryDate = trimToNull(stockAccountHistoryMapper.getLatestStockAccountHistoryDate(searchParam));
		String latestFallbackSaleDate = trimToNull(stockAccountHistoryMapper.getLatestStockSaleDate(searchParam));
		YearMonth latestMonth = parseYearMonth(latestAccountHistoryDate == null ? latestFallbackSaleDate : latestAccountHistoryDate);
		List<WorkStockAccountHistoryMonthVO> monthList = new ArrayList<>();
		if (latestMonth != null && !latestMonth.isBefore(MONTHLY_START_MONTH)) {
			YearMonth currentMonth = latestMonth;
			while (!currentMonth.isBefore(MONTHLY_START_MONTH)) {
				monthList.add(createMonth(currentMonth.format(MONTH_KEY_FORMATTER), currentMonth.format(MONTH_LABEL_FORMATTER)));
				currentMonth = currentMonth.minusMonths(1);
			}
		}
		monthList.add(createMonth(MONTH_KEY_BEFORE, "이전"));
		return monthList;
	}

	// 화면 표시용 월 컬럼을 누적 계산용 과거순 목록으로 변환합니다.
	private List<WorkStockAccountHistoryMonthVO> buildCalculationMonthList(List<WorkStockAccountHistoryMonthVO> displayMonthList) {
		List<WorkStockAccountHistoryMonthVO> calculationMonthList = new ArrayList<>();
		for (WorkStockAccountHistoryMonthVO monthItem : displayMonthList == null ? List.<WorkStockAccountHistoryMonthVO>of() : displayMonthList) {
			if (MONTH_KEY_BEFORE.equals(monthItem.getMonthKey())) {
				calculationMonthList.add(monthItem);
			}
		}
		List<WorkStockAccountHistoryMonthVO> regularMonthList = new ArrayList<>();
		for (WorkStockAccountHistoryMonthVO monthItem : displayMonthList == null ? List.<WorkStockAccountHistoryMonthVO>of() : displayMonthList) {
			if (!MONTH_KEY_BEFORE.equals(monthItem.getMonthKey())) {
				regularMonthList.add(monthItem);
			}
		}
		regularMonthList.sort(Comparator.comparing(WorkStockAccountHistoryMonthVO::getMonthKey));
		calculationMonthList.addAll(regularMonthList);
		if (!calculationMonthList.isEmpty()) {
			return calculationMonthList;
		}
		return List.of(createMonth(MONTH_KEY_BEFORE, "이전"));
	}

	// 월 컬럼 응답 객체를 생성합니다.
	private WorkStockAccountHistoryMonthVO createMonth(String monthKey, String monthLabel) {
		WorkStockAccountHistoryMonthVO month = new WorkStockAccountHistoryMonthVO();
		month.setMonthKey(monthKey);
		month.setMonthLabel(monthLabel);
		return month;
	}

	// 날짜 문자열에서 월 정보를 파싱합니다.
	private YearMonth parseYearMonth(String dateValue) {
		String normalizedDate = trimToNull(dateValue);
		if (normalizedDate == null || normalizedDate.length() < 6) {
			return null;
		}
		try {
			return YearMonth.of(
				Integer.parseInt(normalizedDate.substring(0, 4)),
				Integer.parseInt(normalizedDate.substring(4, 6))
			);
		} catch (RuntimeException exception) {
			return null;
		}
	}

	// 월간 매매금액 목록을 계좌와 월 기준 Map으로 변환합니다.
	private Map<String, WorkStockAccountMonthlySaleAmountVO> buildMonthlySaleAmountMap(List<WorkStockAccountMonthlySaleAmountVO> saleAmountList) {
		Map<String, WorkStockAccountMonthlySaleAmountVO> saleAmountMap = new HashMap<>();
		for (WorkStockAccountMonthlySaleAmountVO saleAmountItem : saleAmountList == null ? List.<WorkStockAccountMonthlySaleAmountVO>of() : saleAmountList) {
			String mapKey = buildAccountMonthKey(saleAmountItem.getStockAccountCd(), saleAmountItem.getMonthKey());
			saleAmountMap.put(mapKey, saleAmountItem);
		}
		return saleAmountMap;
	}

	// 월간 입출금 목록을 계좌와 월 기준 Map으로 변환합니다.
	private Map<String, WorkStockAccountMonthlyCashAmountVO> buildMonthlyCashAmountMap(List<WorkStockAccountMonthlyCashAmountVO> cashAmountList) {
		Map<String, WorkStockAccountMonthlyCashAmountVO> cashAmountMap = new HashMap<>();
		for (WorkStockAccountMonthlyCashAmountVO cashAmountItem : cashAmountList == null ? List.<WorkStockAccountMonthlyCashAmountVO>of() : cashAmountList) {
			String mapKey = buildAccountMonthKey(cashAmountItem.getStockAccountCd(), cashAmountItem.getMonthKey());
			cashAmountMap.put(mapKey, cashAmountItem);
		}
		return cashAmountMap;
	}

	// 확인금액 원천 목록에서 계좌별 월 마지막 금액을 구성합니다.
	private Map<String, Long> buildMonthlyCheckAmountMap(List<WorkStockAccountCheckRowVO> checkRowList) {
		Map<String, Long> monthlyCheckAmountMap = new HashMap<>();
		for (WorkStockAccountCheckRowVO checkRowItem : checkRowList == null ? List.<WorkStockAccountCheckRowVO>of() : checkRowList) {
			String mapKey = buildAccountMonthKey(checkRowItem.getStockAccountCd(), checkRowItem.getMonthKey());
			monthlyCheckAmountMap.put(mapKey, normalizeLong(checkRowItem.getStockTotalAmt()));
		}
		return monthlyCheckAmountMap;
	}

	// 계좌별 월별 계산 상태를 구성합니다.
	private Map<String, AccountMonthlyState> buildAccountMonthlyStateMap(
		List<CommonCodeVO> accountList,
		List<WorkStockAccountHistoryMonthVO> monthList,
		Map<String, WorkStockAccountMonthlySaleAmountVO> saleAmountMap,
		Map<String, WorkStockAccountMonthlyCashAmountVO> cashAmountMap,
		Map<String, Long> monthlyCheckAmountMap
	) {
		Map<String, AccountMonthlyState> accountMonthlyStateMap = new LinkedHashMap<>();
		for (CommonCodeVO accountItem : accountList == null ? List.<CommonCodeVO>of() : accountList) {
			String accountCode = trimToNull(accountItem == null ? null : accountItem.getCd());
			if (accountCode == null) {
				continue;
			}

			AccountMonthlyState state = new AccountMonthlyState();
			long runningPrincipalAmt = 0L;
			for (WorkStockAccountHistoryMonthVO monthItem : buildCalculationMonthList(monthList)) {
				String monthKey = monthItem.getMonthKey();
				String accountMonthKey = buildAccountMonthKey(accountCode, monthKey);
				WorkStockAccountMonthlySaleAmountVO saleAmount = saleAmountMap.get(accountMonthKey);
				WorkStockAccountMonthlyCashAmountVO cashAmount = cashAmountMap.get(accountMonthKey);
				long buyAmt = normalizeLong(saleAmount == null ? null : saleAmount.getBuyAmt());
				long sellAmt = normalizeLong(saleAmount == null ? null : saleAmount.getSellAmt());
				long depositAmt = normalizeLong(cashAmount == null ? null : cashAmount.getDepositAmt());
				long withdrawAmt = normalizeLong(cashAmount == null ? null : cashAmount.getWithdrawAmt());
				runningPrincipalAmt += calculatePrincipalChangeAmt(depositAmt, withdrawAmt);

				long checkAmt = monthlyCheckAmountMap.getOrDefault(accountMonthKey, 0L);
				long profitAmt = calculateProfitAmt(checkAmt, runningPrincipalAmt);
				state.buyAmountMap.put(monthKey, buyAmt);
				state.sellAmountMap.put(monthKey, sellAmt);
				state.depositAmountMap.put(monthKey, depositAmt);
				state.withdrawAmountMap.put(monthKey, withdrawAmt);
				state.principalAmountMap.put(monthKey, runningPrincipalAmt);
				state.checkAmountMap.put(monthKey, checkAmt);
				state.profitAmountMap.put(monthKey, profitAmt);
				state.profitRateMap.put(monthKey, calculateProfitRate(profitAmt, runningPrincipalAmt));
			}
			accountMonthlyStateMap.put(accountCode, state);
		}
		return accountMonthlyStateMap;
	}

	// 계좌별 행 묶음 목록을 구성합니다.
	private List<WorkStockAccountHistoryAccountGroupVO> buildAccountGroupList(
		List<CommonCodeVO> accountList,
		Map<String, AccountMonthlyState> accountMonthlyStateMap
	) {
		List<WorkStockAccountHistoryAccountGroupVO> accountGroupList = new ArrayList<>();
		for (CommonCodeVO accountItem : accountList == null ? List.<CommonCodeVO>of() : accountList) {
			String accountCode = trimToNull(accountItem == null ? null : accountItem.getCd());
			AccountMonthlyState state = accountMonthlyStateMap.get(accountCode);
			if (accountCode == null || state == null) {
				continue;
			}

			WorkStockAccountHistoryAccountGroupVO accountGroup = new WorkStockAccountHistoryAccountGroupVO();
			accountGroup.setStockAccountCd(accountCode);
			accountGroup.setStockAccountNm(accountItem.getCdNm());
			accountGroup.setRowList(List.of(
				createLongValueRow("buyAmt", "매수액", state.buyAmountMap),
				createLongValueRow("sellAmt", "매도액", state.sellAmountMap),
				createLongValueRow("depositAmt", "입금액", state.depositAmountMap),
				createLongValueRow("withdrawAmt", "출금액", state.withdrawAmountMap),
				createLongValueRow("principalAmt", "총원금액", state.principalAmountMap),
				createLongValueRow("checkAmt", "월 중 확인 평가금", state.checkAmountMap),
				createLongValueRow("profitAmt", "원금대비 손익금", state.profitAmountMap),
				createRateValueRow("profitRate", "원금대비 손익율", state.profitRateMap)
			));
			accountGroupList.add(accountGroup);
		}
		return accountGroupList;
	}

	// 전체 요약 행 목록을 구성합니다.
	private List<WorkStockAccountHistoryValueRowVO> buildSummaryRowList(
		List<WorkStockAccountHistoryMonthVO> monthList,
		Map<String, AccountMonthlyState> accountMonthlyStateMap
	) {
		Map<String, Long> totalPrincipalMap = createEmptyLongMonthMap(monthList);
		Map<String, Long> totalCheckMap = createEmptyLongMonthMap(monthList);
		for (AccountMonthlyState state : accountMonthlyStateMap.values()) {
			addMonthAmountMap(totalPrincipalMap, state.principalAmountMap);
			addMonthAmountMap(totalCheckMap, state.checkAmountMap);
		}

		Map<String, Long> totalProfitMap = createEmptyLongMonthMap(monthList);
		Map<String, BigDecimal> totalProfitRateMap = createEmptyRateMonthMap(monthList);
		for (WorkStockAccountHistoryMonthVO monthItem : monthList == null ? List.<WorkStockAccountHistoryMonthVO>of() : monthList) {
			String monthKey = monthItem.getMonthKey();
			long principalAmt = totalPrincipalMap.getOrDefault(monthKey, 0L);
			long checkAmt = totalCheckMap.getOrDefault(monthKey, 0L);
			long profitAmt = calculateProfitAmt(checkAmt, principalAmt);
			totalProfitMap.put(monthKey, profitAmt);
			totalProfitRateMap.put(monthKey, calculateProfitRate(profitAmt, principalAmt));
		}

		return List.of(
			createLongValueRow("totalPrincipalAmt", "전체 총원금", totalPrincipalMap),
			createLongValueRow("totalCheckAmt", "월 중 확인 평가금 총액", totalCheckMap),
			createLongValueRow("totalProfitAmt", "월중 확인 손익금 총액", totalProfitMap),
			createRateValueRow("totalProfitRate", "총액 월중 대비 수익율", totalProfitRateMap)
		);
	}

	// 일별 확인 이력 목록을 구성합니다.
	private List<WorkStockAccountDailyHistoryRowVO> buildDailyHistoryRowList(
		List<WorkStockAccountCheckRowVO> checkRowList,
		List<WorkStockAccountDailySaleAmountVO> saleAmountList,
		List<WorkStockAccountCheckRowVO> activeCheckRowList,
		List<CommonCodeVO> activeAccountList
	) {
		Map<String, Map<String, WorkStockAccountCheckRowVO>> checkRowByDateMap = buildLatestCheckRowByDateMap(checkRowList);
		Map<String, Map<String, WorkStockAccountCheckRowVO>> activeCheckRowByDateMap = buildLatestCheckRowByDateMap(activeCheckRowList);
		NavigableMap<String, Long> cumulativePrincipalMap = buildCumulativePrincipalMap(saleAmountList);
		List<String> checkDateList = new ArrayList<>(checkRowByDateMap.keySet());
		checkDateList.sort(Comparator.reverseOrder());

		List<WorkStockAccountDailyHistoryRowVO> historyRowList = new ArrayList<>();
		for (String checkDate : checkDateList) {
			long checkAmt = sumDailyCheckAmount(checkRowByDateMap.get(checkDate));
			Map<String, Long> checkAccountAmountMap = buildDailyCheckAccountAmountMap(activeAccountList, activeCheckRowByDateMap.get(checkDate));
			long principalAmt = resolveCumulativePrincipalAmt(cumulativePrincipalMap, checkDate);
			long profitAmt = calculateProfitAmt(checkAmt, principalAmt);
			WorkStockAccountDailyHistoryRowVO historyRow = new WorkStockAccountDailyHistoryRowVO();
			historyRow.setCheckDt(formatDisplayDate(checkDate));
			historyRow.setPrincipalAmt(principalAmt);
			historyRow.setCheckAmt(checkAmt);
			historyRow.setProfitAmt(profitAmt);
			historyRow.setProfitRate(calculateProfitRate(profitAmt, principalAmt));
			historyRow.setCheckAccountAmountMap(checkAccountAmountMap);
			historyRowList.add(historyRow);
		}
		return historyRowList;
	}

	// 확인금액 원천 목록에서 날짜와 계좌별 최신 행만 남깁니다.
	private Map<String, Map<String, WorkStockAccountCheckRowVO>> buildLatestCheckRowByDateMap(List<WorkStockAccountCheckRowVO> checkRowList) {
		Map<String, Map<String, WorkStockAccountCheckRowVO>> checkRowByDateMap = new LinkedHashMap<>();
		for (WorkStockAccountCheckRowVO checkRowItem : checkRowList == null ? List.<WorkStockAccountCheckRowVO>of() : checkRowList) {
			String checkDate = trimToNull(checkRowItem == null ? null : checkRowItem.getCheckDt());
			String accountCode = trimToNull(checkRowItem == null ? null : checkRowItem.getStockAccountCd());
			if (checkDate == null || accountCode == null) {
				continue;
			}
			checkRowByDateMap.computeIfAbsent(checkDate, ignored -> new LinkedHashMap<>()).put(accountCode, checkRowItem);
		}
		return checkRowByDateMap;
	}

	// 일자별 입출금 순원금으로 날짜별 누적 원금을 생성합니다.
	private NavigableMap<String, Long> buildCumulativePrincipalMap(List<WorkStockAccountDailySaleAmountVO> saleAmountList) {
		NavigableMap<String, Long> cumulativePrincipalMap = new TreeMap<>();
		long runningPrincipalAmt = 0L;
		for (WorkStockAccountDailySaleAmountVO saleAmountItem : saleAmountList == null ? List.<WorkStockAccountDailySaleAmountVO>of() : saleAmountList) {
			String saleDate = trimToNull(saleAmountItem == null ? null : saleAmountItem.getSaleDt());
			if (saleDate == null) {
				continue;
			}
			runningPrincipalAmt += normalizeLong(saleAmountItem.getNetSaleAmt());
			cumulativePrincipalMap.put(saleDate, runningPrincipalAmt);
		}
		return cumulativePrincipalMap;
	}

	// 확인일 이하의 최신 누적 원금을 반환합니다.
	private long resolveCumulativePrincipalAmt(NavigableMap<String, Long> cumulativePrincipalMap, String checkDate) {
		Map.Entry<String, Long> principalEntry = cumulativePrincipalMap.floorEntry(checkDate);
		return principalEntry == null ? 0L : normalizeLong(principalEntry.getValue());
	}

	// 날짜별 확인금액 합계를 계산합니다.
	private long sumDailyCheckAmount(Map<String, WorkStockAccountCheckRowVO> checkRowByAccountMap) {
		long checkAmt = 0L;
		for (WorkStockAccountCheckRowVO checkRowItem : checkRowByAccountMap == null ? List.<WorkStockAccountCheckRowVO>of() : checkRowByAccountMap.values()) {
			checkAmt += normalizeLong(checkRowItem.getStockTotalAmt());
		}
		return checkAmt;
	}

	// 활성 계좌 순서대로 해당일의 계좌별 확인금액 Map을 생성합니다.
	private Map<String, Long> buildDailyCheckAccountAmountMap(
		List<CommonCodeVO> activeAccountList,
		Map<String, WorkStockAccountCheckRowVO> checkRowByAccountMap
	) {
		Map<String, Long> checkAccountAmountMap = new LinkedHashMap<>();
		for (CommonCodeVO activeAccount : activeAccountList == null ? List.<CommonCodeVO>of() : activeAccountList) {
			String accountCode = trimToNull(activeAccount == null ? null : activeAccount.getCd());
			if (accountCode == null) {
				continue;
			}
			WorkStockAccountCheckRowVO checkRow = checkRowByAccountMap == null ? null : checkRowByAccountMap.get(accountCode);
			checkAccountAmountMap.put(accountCode, normalizeLong(checkRow == null ? null : checkRow.getStockTotalAmt()));
		}
		return checkAccountAmountMap;
	}

	// 전체 이력 더보기 offset을 정규화합니다.
	private int normalizeHistoryOffset(Integer historyOffset) {
		if (historyOffset == null || historyOffset < 0) {
			return HISTORY_DEFAULT_OFFSET;
		}
		return historyOffset;
	}

	// 전체 이력 목록에서 더보기 페이지 범위를 잘라냅니다.
	private List<WorkStockAccountDailyHistoryRowVO> sliceHistoryRowList(List<WorkStockAccountDailyHistoryRowVO> allHistoryRowList, int offset) {
		if (allHistoryRowList == null || offset >= allHistoryRowList.size()) {
			return List.of();
		}
		int endIndex = Math.min(offset + HISTORY_PAGE_SIZE, allHistoryRowList.size());
		return new ArrayList<>(allHistoryRowList.subList(offset, endIndex));
	}

	// 입출금 이력 페이지 목록을 최신순으로 조회하고 날짜를 화면 형식으로 변환합니다.
	private List<WorkStockAccountCashHistoryCreateRequestVO> buildCashHistoryRowList(List<CommonCodeVO> accountList, int offset) {
		WorkStockAccountHistorySearchPO cashHistorySearchParam = buildSearchParam(accountList);
		cashHistorySearchParam.setCashHistoryOffset(offset);
		cashHistorySearchParam.setCashHistoryLimit(CASH_HISTORY_PAGE_SIZE + 1);
		List<WorkStockAccountCashHistoryCreateRequestVO> cashHistoryRowList = stockAccountHistoryMapper.getCashHistoryRowList(cashHistorySearchParam);
		if (cashHistoryRowList == null || cashHistoryRowList.isEmpty()) {
			return List.of();
		}
		for (WorkStockAccountCashHistoryCreateRequestVO cashHistoryRow : cashHistoryRowList) {
			if (cashHistoryRow != null) {
				cashHistoryRow.setCashDt(formatDisplayDate(cashHistoryRow.getCashDt()));
			}
		}
		return cashHistoryRowList;
	}

	// 입출금 이력 목록에서 화면 노출 페이지 크기만 잘라냅니다.
	private List<WorkStockAccountCashHistoryCreateRequestVO> sliceCashHistoryRowList(List<WorkStockAccountCashHistoryCreateRequestVO> cashHistoryRowList) {
		if (cashHistoryRowList == null || cashHistoryRowList.isEmpty()) {
			return List.of();
		}
		int endIndex = Math.min(CASH_HISTORY_PAGE_SIZE, cashHistoryRowList.size());
		return new ArrayList<>(cashHistoryRowList.subList(0, endIndex));
	}

	// 일자 문자열을 화면 표시 날짜로 변환합니다.
	private String formatDisplayDate(String dateValue) {
		String normalizedDate = trimToNull(dateValue);
		if (normalizedDate == null || normalizedDate.length() != 8) {
			return normalizedDate == null ? "" : normalizedDate;
		}
		return normalizedDate.substring(0, 4) + "-" + normalizedDate.substring(4, 6) + "-" + normalizedDate.substring(6, 8);
	}

	// 월별 long Map을 BigDecimal 응답 행으로 변환합니다.
	private WorkStockAccountHistoryValueRowVO createLongValueRow(String rowKey, String rowLabel, Map<String, Long> valueMap) {
		Map<String, BigDecimal> responseValueMap = new LinkedHashMap<>();
		for (Map.Entry<String, Long> valueEntry : valueMap.entrySet()) {
			responseValueMap.put(valueEntry.getKey(), BigDecimal.valueOf(normalizeLong(valueEntry.getValue())));
		}
		return createValueRow(rowKey, rowLabel, VALUE_TYPE_AMOUNT, responseValueMap);
	}

	// 월별 수익률 Map을 응답 행으로 변환합니다.
	private WorkStockAccountHistoryValueRowVO createRateValueRow(String rowKey, String rowLabel, Map<String, BigDecimal> valueMap) {
		return createValueRow(rowKey, rowLabel, VALUE_TYPE_RATE, new LinkedHashMap<>(valueMap));
	}

	// 월별 지표 응답 행을 생성합니다.
	private WorkStockAccountHistoryValueRowVO createValueRow(
		String rowKey,
		String rowLabel,
		String valueType,
		Map<String, BigDecimal> valueMap
	) {
		WorkStockAccountHistoryValueRowVO row = new WorkStockAccountHistoryValueRowVO();
		row.setRowKey(rowKey);
		row.setRowLabel(rowLabel);
		row.setValueType(valueType);
		row.setValueMap(valueMap);
		return row;
	}

	// 월 컬럼 전체를 0 금액 Map으로 초기화합니다.
	private Map<String, Long> createEmptyLongMonthMap(List<WorkStockAccountHistoryMonthVO> monthList) {
		Map<String, Long> monthMap = new LinkedHashMap<>();
		for (WorkStockAccountHistoryMonthVO monthItem : monthList == null ? List.<WorkStockAccountHistoryMonthVO>of() : monthList) {
			monthMap.put(monthItem.getMonthKey(), 0L);
		}
		return monthMap;
	}

	// 월 컬럼 전체를 0 수익률 Map으로 초기화합니다.
	private Map<String, BigDecimal> createEmptyRateMonthMap(List<WorkStockAccountHistoryMonthVO> monthList) {
		Map<String, BigDecimal> monthMap = new LinkedHashMap<>();
		for (WorkStockAccountHistoryMonthVO monthItem : monthList == null ? List.<WorkStockAccountHistoryMonthVO>of() : monthList) {
			monthMap.put(monthItem.getMonthKey(), ZERO_RATE);
		}
		return monthMap;
	}

	// 기준 Map에 월별 금액을 더합니다.
	private void addMonthAmountMap(Map<String, Long> targetMap, Map<String, Long> sourceMap) {
		for (Map.Entry<String, Long> sourceEntry : sourceMap.entrySet()) {
			targetMap.put(sourceEntry.getKey(), targetMap.getOrDefault(sourceEntry.getKey(), 0L) + normalizeLong(sourceEntry.getValue()));
		}
	}

	// 원금대비 손익률을 퍼센트 표시값으로 계산합니다.
	private BigDecimal calculateProfitRate(long profitAmt, long principalAmt) {
		if (principalAmt == 0L) {
			return ZERO_RATE;
		}
		return BigDecimal.valueOf(profitAmt)
			.multiply(BigDecimal.valueOf(100L))
			.divide(BigDecimal.valueOf(principalAmt), 2, RoundingMode.HALF_UP);
	}

	// 확인 평가금이 없으면 원금대비 손익금을 0으로 계산합니다.
	private long calculateProfitAmt(long checkAmt, long principalAmt) {
		if (checkAmt == 0L) {
			return 0L;
		}
		return checkAmt - principalAmt;
	}

	// 입금액과 양수 출금액으로 원금 증감액을 계산합니다.
	private long calculatePrincipalChangeAmt(long depositAmt, long withdrawAmt) {
		return depositAmt - withdrawAmt;
	}

	// 계좌와 월을 조합한 Map 키를 생성합니다.
	private String buildAccountMonthKey(String stockAccountCd, String monthKey) {
		return trimToNull(stockAccountCd) + "|" + trimToNull(monthKey);
	}

	// nullable 숫자를 long 기본값으로 정규화합니다.
	private long normalizeLong(Number value) {
		return value == null ? 0L : value.longValue();
	}

	// 중복과 공백을 제거한 코드 목록을 생성합니다.
	private List<String> normalizeCodeList(List<String> codeList) {
		if (codeList == null || codeList.isEmpty()) {
			return List.of();
		}
		LinkedHashSet<String> normalizedCodeSet = new LinkedHashSet<>();
		for (String codeItem : codeList) {
			String normalizedCode = trimToNull(codeItem);
			if (normalizedCode != null) {
				normalizedCodeSet.add(normalizedCode);
			}
		}
		return List.copyOf(normalizedCodeSet);
	}
}
