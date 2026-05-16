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
import com.xodud1202.springbackend.mapper.CommonMapper;
import com.xodud1202.springbackend.mapper.StockSaleHistoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
// 매매일지 화면의 조회 조건, 합계, 상세 목록 비즈니스 로직을 처리합니다.
public class StockSaleHistoryService {
	private static final String STOCK_ACCOUNT_GROUP_CODE = "STOCK_ACCOUNT";
	private static final String STOCK_NAME_GROUP_CODE = "STOCK_NM";
	private static final int STOCK_SALE_DEFAULT_PAGE = 1;
	private static final int STOCK_SALE_DEFAULT_PAGE_SIZE = 20;
	private static final int STOCK_SALE_MAX_PAGE_SIZE = 10000;
	private static final int STOCK_SALE_MEMO_MAX_LENGTH = 300;
	private static final DateTimeFormatter STOCK_SALE_REQUEST_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
	private static final DateTimeFormatter STOCK_SALE_DB_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

	private final CommonMapper commonMapper;
	private final StockSaleHistoryMapper stockSaleHistoryMapper;

	// 매매일지 화면 초기 선택 목록을 조회합니다.
	public WorkStockSaleBootstrapResponseVO getStockSaleBootstrap(UserInfoVO currentUser) {
		WorkStockSaleBootstrapResponseVO response = new WorkStockSaleBootstrapResponseVO();
		response.setCurrentUser(currentUser);
		response.setAccountList(getStockAccountList());
		response.setStockList(getStockNameList());
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

		List<WorkStockSaleSummaryRowVO> summaryList = stockSaleHistoryMapper.getStockSaleSummaryList(param);
		List<WorkStockSaleRowVO> rowList = stockSaleHistoryMapper.getStockSaleRowList(param);

		WorkStockSaleListResponseVO response = new WorkStockSaleListResponseVO();
		response.setSummaryList(summaryList == null ? List.of() : summaryList);
		response.setRowList(rowList == null ? List.of() : rowList);
		response.setTotalCount(totalCount);
		response.setPageNo(resolvedPageNo);
		response.setPageSize(param.getPageSize());
		response.setTotalPageCount(totalPageCount);
		return response;
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

	// 등록 요청 값을 DB 저장 형식으로 정규화하고 검증합니다.
	private WorkStockSaleCreateRequestVO buildCreateParam(WorkStockSaleCreateRequestVO request, Long workUserNo) {
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

		WorkStockSaleCreateRequestVO param = new WorkStockSaleCreateRequestVO();
		param.setSaleDt(normalizedSaleDt);
		param.setStockAccountCd(normalizedAccountCd);
		param.setStockNmCd(normalizedStockNmCd);
		param.setSaleCnt(request.getSaleCnt());
		param.setSaleAmt(request.getSaleAmt());
		param.setProfitAmt(request.getProfitAmt() == null ? 0L : request.getProfitAmt());
		param.setMemo(normalizedMemo);
		param.setRegNo(workUserNo);
		param.setUdtNo(workUserNo);
		return param;
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
