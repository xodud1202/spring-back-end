package com.xodud1202.springbackend.service;

import static com.xodud1202.springbackend.common.util.CommonPaginationUtils.*;
import static com.xodud1202.springbackend.common.util.CommonTextUtils.*;

import com.xodud1202.springbackend.domain.admin.common.UserInfoVO;
import com.xodud1202.springbackend.domain.common.CommonCodeVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockSaleBootstrapResponseVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockSaleCreateRequestVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockSaleDisplayOrderItemPO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockSaleDisplayOrderUpdateRequestVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockSaleDisplayOrderUpdateResponseVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockSaleListResponseVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockSaleRowVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockSaleSearchPO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockSaleSummaryRowVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockSaleUpdateRequestVO;
import com.xodud1202.springbackend.mapper.CommonMapper;
import com.xodud1202.springbackend.mapper.StockSaleHistoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
// 매매일지 화면의 조회 조건, 합계, 상세 목록 비즈니스 로직을 처리합니다.
public class StockSaleHistoryService {
	private static final String STOCK_ACCOUNT_GROUP_CODE = "STOCK_ACCOUNT";
	private static final String STOCK_NAME_GROUP_CODE = "STOCK_NM";
	private static final String CASH_IN_OUT_GROUP_CODE = "CASH_IN_OUT";
	private static final int STOCK_SALE_DEFAULT_PAGE = 1;
	private static final int STOCK_SALE_DEFAULT_PAGE_SIZE = 20;
	private static final int STOCK_SALE_MAX_PAGE_SIZE = 10000;
	private static final int STOCK_SALE_MEMO_MAX_LENGTH = 300;
	private static final DateTimeFormatter STOCK_SALE_REQUEST_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
	private static final DateTimeFormatter STOCK_SALE_DB_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

	private final CommonMapper commonMapper;
	private final StockSaleHistoryMapper stockSaleHistoryMapper;

	// 계좌와 종목으로 현재 보유 포지션을 구분하는 키입니다.
	private record StockSalePositionKey(String stockAccountCd, String stockNmCd) {
	}

	// 계좌와 종목별 현재 보유 상태를 누적합니다.
	private static final class StockSaleHoldingState {
		private long holdingCnt;
		private long holdingPrincipalAmt;
	}

	// 매매일지 화면 초기 선택 목록을 조회합니다.
	public WorkStockSaleBootstrapResponseVO getStockSaleBootstrap(UserInfoVO currentUser) {
		WorkStockSaleBootstrapResponseVO response = new WorkStockSaleBootstrapResponseVO();
		response.setCurrentUser(currentUser);
		response.setAccountList(getStockAccountList());
		response.setStockList(getStockNameList());
		response.setCashInOutList(getCashInOutList());
		return response;
	}

	// 매매일지 거래 이력을 등록합니다.
	public void createStockSaleHistory(WorkStockSaleCreateRequestVO request, Long workUserNo) {
		if (request == null) {
			throw new IllegalArgumentException("등록할 매매일지 내용을 입력해주세요.");
		}

		WorkStockSaleCreateRequestVO param = buildCreateParam(request, workUserNo);
		stockSaleHistoryMapper.insertStockSaleHistory(param);
	}

	// 매매일지 거래 이력을 수정합니다.
	public void updateStockSaleHistory(WorkStockSaleUpdateRequestVO request, Long workUserNo) {
		if (request == null) {
			throw new IllegalArgumentException("수정할 매매일지 내용을 입력해주세요.");
		}

		WorkStockSaleUpdateRequestVO param = buildUpdateParam(request, workUserNo);
		int updatedCount = stockSaleHistoryMapper.updateStockSaleHistory(param);
		if (updatedCount != 1) {
			throw new IllegalStateException("수정할 매매일지를 찾을 수 없습니다.");
		}
	}

	@Transactional
	// 매매일지 계좌와 주식 선택 목록의 노출순서를 저장합니다.
	public WorkStockSaleDisplayOrderUpdateResponseVO updateStockSaleDisplayOrder(WorkStockSaleDisplayOrderUpdateRequestVO request, Long workUserNo) {
		if (request == null) {
			throw new IllegalArgumentException("저장할 노출순서 정보를 입력해주세요.");
		}
		if (workUserNo == null || workUserNo <= 0) {
			throw new IllegalArgumentException("로그인 정보를 확인해주세요.");
		}

		List<WorkStockSaleDisplayOrderItemPO> accountOrderList = buildDisplayOrderUpdateList(
			request.getAccountOrderList(),
			getStockAccountList(),
			"계좌 노출순서를 확인해주세요."
		);
		List<WorkStockSaleDisplayOrderItemPO> stockOrderList = buildDisplayOrderUpdateList(
			request.getStockOrderList(),
			getStockNameList(),
			"주식 노출순서를 확인해주세요."
		);

		int accountUpdatedCount = updateDisplayOrderList(STOCK_ACCOUNT_GROUP_CODE, accountOrderList, workUserNo);
		int stockUpdatedCount = updateDisplayOrderList(STOCK_NAME_GROUP_CODE, stockOrderList, workUserNo);

		WorkStockSaleDisplayOrderUpdateResponseVO response = new WorkStockSaleDisplayOrderUpdateResponseVO();
		response.setMessage("노출순서를 저장했습니다.");
		response.setAccountUpdatedCount(accountUpdatedCount);
		response.setStockUpdatedCount(stockUpdatedCount);
		return response;
	}

	// 검색 조건에 맞는 종목별 합계와 상세 목록 페이지를 조회합니다.
	public WorkStockSaleListResponseVO getStockSaleList(
		String startSaleDt,
		String endSaleDt,
		List<String> stockAccountCdList,
		List<String> stockNmCdList,
		Integer pageNo,
		Integer pageSize
	) {
		WorkStockSaleSearchPO param = buildSearchParam(startSaleDt, endSaleDt, stockAccountCdList, stockNmCdList, pageNo, pageSize);
		int totalCount = stockSaleHistoryMapper.getStockSaleRowCount(param);
		int totalPageCount = calculateTotalPageCount(totalCount, param.getPageSize());
		int resolvedPageNo = resolvePageNoWithinRange(param.getPageNo(), totalPageCount);
		param.setPageNo(resolvedPageNo);
		param.setOffset(calculateOffset(resolvedPageNo, param.getPageSize()));

		WorkStockSaleSearchPO holdingParam = buildCurrentHoldingSearchParam(param);
		List<WorkStockSaleSummaryRowVO> summaryList = stockSaleHistoryMapper.getStockSaleSummaryList(holdingParam);
		List<WorkStockSaleRowVO> holdingSourceRowList = stockSaleHistoryMapper.getStockSaleHoldingSourceRowList(holdingParam);
		List<WorkStockSaleRowVO> rowList = stockSaleHistoryMapper.getStockSaleRowList(param);

		WorkStockSaleListResponseVO response = new WorkStockSaleListResponseVO();
		response.setSummaryList(buildCurrentHoldingSummaryList(summaryList, holdingSourceRowList));
		response.setRowList(rowList == null ? List.of() : rowList);
		response.setTotalCount(totalCount);
		response.setPageNo(resolvedPageNo);
		response.setPageSize(param.getPageSize());
		response.setTotalPageCount(totalPageCount);
		return response;
	}

	// 현재 보유원금 계산은 날짜 조건 없이 선택 계좌와 종목 조건만 사용합니다.
	private WorkStockSaleSearchPO buildCurrentHoldingSearchParam(WorkStockSaleSearchPO searchParam) {
		WorkStockSaleSearchPO holdingParam = new WorkStockSaleSearchPO();
		holdingParam.setStockAccountCdList(searchParam == null ? List.of() : searchParam.getStockAccountCdList());
		holdingParam.setStockNmCdList(searchParam == null ? List.of() : searchParam.getStockNmCdList());
		return holdingParam;
	}

	// 거래 이력을 계좌+종목 포지션으로 계산한 뒤 종목별 합계 행에 반영합니다.
	private List<WorkStockSaleSummaryRowVO> buildCurrentHoldingSummaryList(
		List<WorkStockSaleSummaryRowVO> summaryList,
		List<WorkStockSaleRowVO> holdingSourceRowList
	) {
		Map<String, StockSaleHoldingState> holdingStateMap = buildStockSaleHoldingStateMap(holdingSourceRowList);
		List<WorkStockSaleSummaryRowVO> adjustedSummaryList = new ArrayList<>();
		for (WorkStockSaleSummaryRowVO summaryItem : summaryList == null ? List.<WorkStockSaleSummaryRowVO>of() : summaryList) {
			if (summaryItem == null) {
				continue;
			}

			// 종목별 열린 포지션만 더해 현재 보유수량과 보유원금을 만든다.
			StockSaleHoldingState stockHoldingState = holdingStateMap.getOrDefault(summaryItem.getStockNmCd(), new StockSaleHoldingState());
			long holdingCnt = stockHoldingState.holdingCnt;
			long holdingPrincipalAmt = stockHoldingState.holdingPrincipalAmt;
			summaryItem.setSaleCnt(holdingCnt);
			summaryItem.setHoldingPrincipalAmt(holdingPrincipalAmt);
			summaryItem.setAverageSaleAmt(calculateHoldingAverageSaleAmt(holdingCnt, holdingPrincipalAmt));
			adjustedSummaryList.add(summaryItem);
		}
		return adjustedSummaryList;
	}

	// 원천 거래 목록을 계좌+종목별 포지션으로 계산하고 종목 단위로 다시 합산합니다.
	private Map<String, StockSaleHoldingState> buildStockSaleHoldingStateMap(List<WorkStockSaleRowVO> holdingSourceRowList) {
		Map<StockSalePositionKey, StockSaleHoldingState> positionStateMap = new HashMap<>();
		for (WorkStockSaleRowVO rowItem : holdingSourceRowList == null ? List.<WorkStockSaleRowVO>of() : holdingSourceRowList) {
			String stockAccountCd = trimToNull(rowItem == null ? null : rowItem.getStockAccountCd());
			String stockNmCd = trimToNull(rowItem == null ? null : rowItem.getStockNmCd());
			if (stockAccountCd == null || stockNmCd == null) {
				continue;
			}

			// 계좌별 전량 매도와 재매수를 독립된 포지션으로 처리한다.
			StockSalePositionKey positionKey = new StockSalePositionKey(stockAccountCd, stockNmCd);
			StockSaleHoldingState positionState = positionStateMap.computeIfAbsent(positionKey, ignored -> new StockSaleHoldingState());
			applyStockSaleHoldingRow(positionState, rowItem);
		}

		Map<String, StockSaleHoldingState> stockHoldingStateMap = new HashMap<>();
		for (Map.Entry<StockSalePositionKey, StockSaleHoldingState> entry : positionStateMap.entrySet()) {
			StockSaleHoldingState positionState = entry.getValue();
			if (positionState.holdingCnt <= 0L) {
				continue;
			}

			// 열린 계좌별 포지션을 종목별 표시 합계로 모읍니다.
			StockSaleHoldingState stockState = stockHoldingStateMap.computeIfAbsent(entry.getKey().stockNmCd(), ignored -> new StockSaleHoldingState());
			stockState.holdingCnt += positionState.holdingCnt;
			stockState.holdingPrincipalAmt += positionState.holdingPrincipalAmt;
		}
		return stockHoldingStateMap;
	}

	// 거래 한 건을 현재 포지션 상태에 반영합니다.
	private void applyStockSaleHoldingRow(StockSaleHoldingState positionState, WorkStockSaleRowVO rowItem) {
		long saleCnt = normalizeLong(rowItem.getSaleCnt());
		long saleAmt = normalizeLong(rowItem.getSaleAmt());
		long profitAmt = normalizeLong(rowItem.getProfitAmt());
		if (saleCnt > 0L) {
			positionState.holdingCnt += saleCnt;
			positionState.holdingPrincipalAmt += saleAmt;
		} else if (saleCnt < 0L) {
			long soldPrincipalAmt = Math.abs(saleAmt) - profitAmt;
			positionState.holdingCnt += saleCnt;
			positionState.holdingPrincipalAmt -= soldPrincipalAmt;
		}

		// 전량 매도 후 같은 계좌+종목을 다시 매수하면 새 포지션이 0원부터 시작되도록 리셋합니다.
		if (positionState.holdingCnt <= 0L) {
			positionState.holdingCnt = 0L;
			positionState.holdingPrincipalAmt = 0L;
		}
	}

	// 현재 보유수량과 보유원금으로 보유평단을 계산합니다.
	private BigDecimal calculateHoldingAverageSaleAmt(long holdingCnt, long holdingPrincipalAmt) {
		if (holdingCnt <= 0L) {
			return null;
		}
		return BigDecimal.valueOf(holdingPrincipalAmt).divide(BigDecimal.valueOf(holdingCnt), 2, RoundingMode.HALF_UP);
	}

	// nullable 숫자 값을 계산용 long 기본값으로 정규화합니다.
	private long normalizeLong(Number value) {
		return value == null ? 0L : value.longValue();
	}

	// 요청 순서 목록이 현재 활성 공통코드 전체와 일치하는지 확인하고 저장 순서를 재계산합니다.
	private List<WorkStockSaleDisplayOrderItemPO> buildDisplayOrderUpdateList(
		List<WorkStockSaleDisplayOrderItemPO> requestOrderList,
		List<CommonCodeVO> optionList,
		String invalidMessage
	) {
		if (requestOrderList == null) {
			throw new IllegalArgumentException(invalidMessage);
		}

		LinkedHashSet<String> validCodeSet = new LinkedHashSet<>();
		for (CommonCodeVO optionItem : optionList) {
			String optionCode = trimToNull(optionItem == null ? null : optionItem.getCd());
			if (optionCode != null) {
				validCodeSet.add(optionCode);
			}
		}
		if (requestOrderList.size() != validCodeSet.size()) {
			throw new IllegalArgumentException(invalidMessage);
		}

		LinkedHashSet<String> requestedCodeSet = new LinkedHashSet<>();
		List<WorkStockSaleDisplayOrderItemPO> normalizedOrderList = new ArrayList<>();
		int nextDispOrd = 1;
		for (WorkStockSaleDisplayOrderItemPO requestItem : requestOrderList) {
			String requestCode = trimToNull(requestItem == null ? null : requestItem.getCd());
			if (requestCode == null || !validCodeSet.contains(requestCode) || !requestedCodeSet.add(requestCode)) {
				throw new IllegalArgumentException(invalidMessage);
			}

			WorkStockSaleDisplayOrderItemPO normalizedItem = new WorkStockSaleDisplayOrderItemPO();
			normalizedItem.setCd(requestCode);
			normalizedItem.setDispOrd(nextDispOrd++);
			normalizedOrderList.add(normalizedItem);
		}
		if (!requestedCodeSet.equals(validCodeSet)) {
			throw new IllegalArgumentException(invalidMessage);
		}
		return List.copyOf(normalizedOrderList);
	}

	// 공통코드 그룹의 노출순서를 전달된 순서대로 수정합니다.
	private int updateDisplayOrderList(String groupCode, List<WorkStockSaleDisplayOrderItemPO> orderList, Long workUserNo) {
		int updatedCount = 0;
		for (WorkStockSaleDisplayOrderItemPO orderItem : orderList) {
			updatedCount += commonMapper.updateCommonCodeDispOrd(groupCode, orderItem.getCd(), orderItem.getDispOrd(), workUserNo);
		}
		if (updatedCount != orderList.size()) {
			throw new IllegalStateException("노출순서 저장 대상이 변경되었습니다. 새로고침 후 다시 시도해주세요.");
		}
		return updatedCount;
	}

	// 계좌 공통코드 목록을 조회합니다.
	private List<CommonCodeVO> getStockAccountList() {
		List<CommonCodeVO> accountList = commonMapper.getCommonCodeList(STOCK_ACCOUNT_GROUP_CODE);
		return accountList == null ? List.of() : accountList;
	}

	// 주식명 공통코드 목록을 조회합니다.
	private List<CommonCodeVO> getStockNameList() {
		List<CommonCodeVO> stockList = commonMapper.getCommonCodeList(STOCK_NAME_GROUP_CODE);
		return stockList == null ? List.of() : stockList;
	}

	// 입출금구분 공통코드 목록을 조회합니다.
	private List<CommonCodeVO> getCashInOutList() {
		List<CommonCodeVO> cashInOutList = commonMapper.getCommonCodeList(CASH_IN_OUT_GROUP_CODE);
		return cashInOutList == null ? List.of() : cashInOutList;
	}

	// 등록 요청 값을 DB 저장 형식으로 정규화하고 검증합니다.
	private WorkStockSaleCreateRequestVO buildCreateParam(WorkStockSaleCreateRequestVO request, Long workUserNo) {
		WorkStockSaleCreateRequestVO param = buildStockSaleSaveParam(request, workUserNo, new WorkStockSaleCreateRequestVO());
		param.setRegNo(workUserNo);
		return param;
	}

	// 수정 요청 값을 DB 저장 형식으로 정규화하고 검증합니다.
	private WorkStockSaleUpdateRequestVO buildUpdateParam(WorkStockSaleUpdateRequestVO request, Long workUserNo) {
		if (request.getSaleHistSeq() == null || request.getSaleHistSeq() <= 0) {
			throw new IllegalArgumentException("수정할 매매일지 정보를 확인해주세요.");
		}

		WorkStockSaleUpdateRequestVO param = buildStockSaleSaveParam(request, workUserNo, new WorkStockSaleUpdateRequestVO());
		param.setSaleHistSeq(request.getSaleHistSeq());
		return param;
	}

	// 등록과 수정 요청의 공통 입력값을 정규화하고 검증합니다.
	private <T extends WorkStockSaleCreateRequestVO> T buildStockSaleSaveParam(WorkStockSaleCreateRequestVO request, Long workUserNo, T param) {
		if (workUserNo == null || workUserNo <= 0) {
			throw new IllegalArgumentException("로그인 정보를 확인해주세요.");
		}

		String normalizedSaleDt = normalizeSaleDate(request.getSaleDt(), "매매일자를 확인해주세요.");
		String normalizedAccountCd = trimToNull(request.getStockAccountCd());
		String normalizedStockNmCd = trimToNull(request.getStockNmCd());
		String normalizedMemo = normalizeMemo(request.getMemo());
		if (normalizedSaleDt == null) {
			throw new IllegalArgumentException("매매일자를 입력해주세요.");
		}
		validateRequiredCode(normalizedAccountCd, getStockAccountList(), "계좌를 선택해주세요.");
		validateRequiredCode(normalizedStockNmCd, getStockNameList(), "주식명을 선택해주세요.");
		validateRequiredInteger(request.getSaleCnt(), "매매수를 입력해주세요.");
		validateRequiredLong(request.getSaleAmt(), "매매금액을 입력해주세요.");
		validateStockSaleAmountRule(request.getSaleCnt(), request.getSaleAmt(), request.getProfitAmt());

		param.setSaleDt(normalizedSaleDt);
		param.setStockAccountCd(normalizedAccountCd);
		param.setStockNmCd(normalizedStockNmCd);
		param.setSaleCnt(request.getSaleCnt());
		param.setSaleAmt(request.getSaleAmt());
		param.setProfitAmt(request.getProfitAmt() == null ? 0L : request.getProfitAmt());
		param.setMemo(normalizedMemo);
		param.setUdtNo(workUserNo);
		return param;
	}

	// 매매수 방향에 맞는 매매금액과 손익금액 입력 규칙을 검증합니다.
	private void validateStockSaleAmountRule(Integer saleCnt, Long saleAmt, Long profitAmt) {
		if (saleCnt == null || saleCnt == 0) {
			throw new IllegalArgumentException("매매수를 입력해주세요.");
		}
		if (saleAmt == null || saleAmt == 0L) {
			throw new IllegalArgumentException("매매금액을 입력해주세요.");
		}
		if (saleCnt > 0 && saleAmt < 0L) {
			throw new IllegalArgumentException("매수는 양수만 입력 할 수 있습니다.");
		}
		if (saleCnt < 0 && saleAmt > 0L) {
			throw new IllegalArgumentException("매도는 음수만 입력 할 수 있습니다.");
		}
		if (saleCnt > 0 && normalizeLong(profitAmt) != 0L) {
			throw new IllegalArgumentException("매수 등록 시 손익금액은 입력할 수 없습니다.");
		}
	}

	// 필수 코드가 공통코드 목록에 존재하는지 확인합니다.
	private void validateRequiredCode(String code, List<CommonCodeVO> optionList, String invalidMessage) {
		if (code == null) {
			throw new IllegalArgumentException(invalidMessage);
		}
		boolean exists = optionList.stream().anyMatch(optionItem -> code.equals(optionItem.getCd()));
		if (!exists) {
			throw new IllegalArgumentException(invalidMessage);
		}
	}

	// 필수 정수 값을 확인합니다.
	private void validateRequiredInteger(Integer value, String invalidMessage) {
		if (value == null) {
			throw new IllegalArgumentException(invalidMessage);
		}
	}

	// 필수 금액 값을 확인합니다.
	private void validateRequiredLong(Long value, String invalidMessage) {
		if (value == null) {
			throw new IllegalArgumentException(invalidMessage);
		}
	}

	// 메모를 DB 길이에 맞게 정규화합니다.
	private String normalizeMemo(String memo) {
		String normalizedMemo = trimToNull(memo);
		if (normalizedMemo == null) {
			return "";
		}
		if (normalizedMemo.length() > STOCK_SALE_MEMO_MAX_LENGTH) {
			throw new IllegalArgumentException("메모는 300자 이하로 입력해주세요.");
		}
		return normalizedMemo;
	}

	// 조회 요청 값을 DB 조회 조건으로 정규화합니다.
	private WorkStockSaleSearchPO buildSearchParam(
		String startSaleDt,
		String endSaleDt,
		List<String> stockAccountCdList,
		List<String> stockNmCdList,
		Integer pageNo,
		Integer pageSize
	) {
		String normalizedStartSaleDt = normalizeSaleDate(startSaleDt, "시작일을 확인해주세요.");
		String normalizedEndSaleDt = normalizeSaleDate(endSaleDt, "종료일을 확인해주세요.");
		if (normalizedStartSaleDt != null && normalizedEndSaleDt != null && normalizedStartSaleDt.compareTo(normalizedEndSaleDt) > 0) {
			throw new IllegalArgumentException("종료일은 시작일보다 빠를 수 없습니다.");
		}

		int resolvedPageSize = normalizePageSize(pageSize, STOCK_SALE_DEFAULT_PAGE_SIZE, STOCK_SALE_MAX_PAGE_SIZE);
		int resolvedPageNo = normalizePage(pageNo, STOCK_SALE_DEFAULT_PAGE);

		WorkStockSaleSearchPO param = new WorkStockSaleSearchPO();
		param.setStartSaleDt(normalizedStartSaleDt);
		param.setEndSaleDt(normalizedEndSaleDt);
		param.setStockAccountCdList(normalizeCodeList(stockAccountCdList));
		param.setStockNmCdList(normalizeCodeList(stockNmCdList));
		param.setPageNo(resolvedPageNo);
		param.setPageSize(resolvedPageSize);
		param.setOffset(calculateOffset(resolvedPageNo, resolvedPageSize));
		return param;
	}

	// 화면 날짜 값을 DB의 yyyyMMdd 문자열로 변환합니다.
	private String normalizeSaleDate(String saleDt, String invalidMessage) {
		String normalizedSaleDt = trimToNull(saleDt);
		if (normalizedSaleDt == null) {
			return null;
		}
		try {
			LocalDate saleDate = LocalDate.parse(normalizedSaleDt, STOCK_SALE_REQUEST_DATE_FORMATTER);
			return saleDate.format(STOCK_SALE_DB_DATE_FORMATTER);
		} catch (DateTimeParseException exception) {
			throw new IllegalArgumentException(invalidMessage);
		}
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
