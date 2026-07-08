package com.xodud1202.springbackend.controller.work;

import com.xodud1202.springbackend.domain.work.stock.WorkStockAccountCashHistoryCreateRequestVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockAccountCheckRowVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockAccountHistoryResponseVO;
import com.xodud1202.springbackend.service.StockAccountHistoryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
// 업무 로그인 세션을 사용하는 주식계좌이력 API를 제공합니다.
public class WorkStockAccountHistoryController extends WorkControllerSupport {
	private final StockAccountHistoryService stockAccountHistoryService;

	@GetMapping("/api/work/stock-account-history")
	// 주식계좌이력 월별 정보와 계좌별 전체 이력 정보를 조회합니다.
	public ResponseEntity<WorkStockAccountHistoryResponseVO> getStockAccountHistory(
		HttpServletRequest request,
		@RequestParam(required = false) List<String> stockAccountCdList,
		@RequestParam(required = false) Integer historyOffset,
		@RequestParam(required = false) Integer cashHistoryOffset
	) {
		try {
			resolveRequiredWorkUserNo(request);
			return ResponseEntity.ok(stockAccountHistoryService.getStockAccountHistory(stockAccountCdList, historyOffset, cashHistoryOffset));
		} catch (SecurityException | IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("주식계좌이력 조회 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("주식계좌이력 조회에 실패했습니다.", exception);
		}
	}

	@PostMapping("/api/work/stock-account-history/check-amount")
	// 사용 중인 전체 계좌의 확인 평가금을 같은 날짜 기준으로 저장합니다.
	public ResponseEntity<Map<String, String>> saveStockAccountCheckAmount(
		HttpServletRequest request,
		@RequestBody List<WorkStockAccountCheckRowVO> saveRequestList
	) {
		try {
			Long workUserNo = resolveRequiredWorkUserNo(request);
			stockAccountHistoryService.saveStockAccountCheckAmountList(saveRequestList, workUserNo);
			return ResponseEntity.ok(Map.of("message", "계좌확인금액을 저장했습니다."));
		} catch (SecurityException | IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("계좌확인금액 저장 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("계좌확인금액 저장에 실패했습니다.", exception);
		}
	}

	@PostMapping("/api/work/stock-account-history/cash-history")
	// 계좌 입출금 이력을 등록합니다.
	public ResponseEntity<Map<String, String>> createStockAccountCashHistory(
		HttpServletRequest request,
		@RequestBody WorkStockAccountCashHistoryCreateRequestVO createRequest
	) {
		try {
			Long workUserNo = resolveRequiredWorkUserNo(request);
			stockAccountHistoryService.createStockAccountCashHistory(createRequest, workUserNo);
			return ResponseEntity.ok(Map.of("message", "입출금 내역을 등록했습니다."));
		} catch (SecurityException | IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("입출금 내역 등록 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("입출금 내역 등록에 실패했습니다.", exception);
		}
	}

	@PutMapping("/api/work/stock-account-history/cash-history/{cashHistSeq}")
	// 계좌 입출금 이력을 수정합니다.
	public ResponseEntity<Map<String, String>> updateStockAccountCashHistory(
		HttpServletRequest request,
		@PathVariable Long cashHistSeq,
		@RequestBody WorkStockAccountCashHistoryCreateRequestVO updateRequest
	) {
		try {
			Long workUserNo = resolveRequiredWorkUserNo(request);
			if (updateRequest == null) {
				updateRequest = new WorkStockAccountCashHistoryCreateRequestVO();
			}
			updateRequest.setCashHistSeq(cashHistSeq);
			stockAccountHistoryService.updateStockAccountCashHistory(updateRequest, workUserNo);
			return ResponseEntity.ok(Map.of("message", "입출금 내역을 수정했습니다."));
		} catch (SecurityException | IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("입출금 내역 수정 실패 cashHistSeq={} message={}", cashHistSeq, exception.getMessage(), exception);
			throw new IllegalStateException("입출금 내역 수정에 실패했습니다.", exception);
		}
	}
}
