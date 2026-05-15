package com.xodud1202.springbackend.service;

import static com.xodud1202.springbackend.common.util.CommonPaginationUtils.*;
import static com.xodud1202.springbackend.common.util.CommonTextUtils.*;

import com.xodud1202.springbackend.domain.admin.common.UserInfoVO;
import com.xodud1202.springbackend.domain.common.CommonCodeVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockSaleBootstrapResponseVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockSaleListResponseVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockSaleRowVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockSaleSearchPO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockSaleSummaryRowVO;
import com.xodud1202.springbackend.mapper.CommonMapper;
import com.xodud1202.springbackend.mapper.StockSaleHistoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
	private static final int STOCK_SALE_MAX_PAGE_SIZE = 100;
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
