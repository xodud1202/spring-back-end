package com.xodud1202.springbackend.controller.work;

import com.xodud1202.springbackend.domain.admin.common.UserInfoVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockSaleBootstrapResponseVO;
import com.xodud1202.springbackend.domain.work.stock.WorkStockSaleListResponseVO;
import com.xodud1202.springbackend.service.StockSaleHistoryService;
import com.xodud1202.springbackend.service.UserBaseService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
// 업무 로그인 세션을 사용하는 매매일지 조회 API를 제공합니다.
public class WorkStockSaleHistoryController extends WorkControllerSupport {
	private final StockSaleHistoryService stockSaleHistoryService;
	private final UserBaseService userBaseService;

	@GetMapping("/api/work/stock-sale-history/bootstrap")
	// 매매일지 화면 초기 구동 데이터를 조회합니다.
	public ResponseEntity<WorkStockSaleBootstrapResponseVO> getBootstrap(HttpServletRequest request) {
		try {
			Long workUserNo = resolveRequiredWorkUserNo(request);
			UserInfoVO currentUser = userBaseService.getUserInfoByUsrNo(workUserNo).orElse(null);
			return ResponseEntity.ok(stockSaleHistoryService.getStockSaleBootstrap(currentUser));
		} catch (SecurityException | IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("매매일지 bootstrap 조회 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("매매일지 초기 데이터 조회에 실패했습니다.", exception);
		}
	}

	@GetMapping("/api/work/stock-sale-history/list")
	// 매매일지 검색 조건에 맞는 종목별 합계와 상세 목록을 조회합니다.
	public ResponseEntity<WorkStockSaleListResponseVO> getStockSaleList(
		HttpServletRequest request,
		@RequestParam(required = false) String startSaleDt,
		@RequestParam(required = false) String endSaleDt,
		@RequestParam(required = false) List<String> stockAccountCdList,
		@RequestParam(required = false) List<String> stockNmCdList,
		@RequestParam(required = false) Integer pageNo,
		@RequestParam(required = false) Integer pageSize
	) {
		try {
			resolveRequiredWorkUserNo(request);
			return ResponseEntity.ok(
				stockSaleHistoryService.getStockSaleList(
					startSaleDt,
					endSaleDt,
					stockAccountCdList,
					stockNmCdList,
					pageNo,
					pageSize
				)
			);
		} catch (SecurityException | IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("매매일지 목록 조회 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("매매일지 목록 조회에 실패했습니다.", exception);
		}
	}
}
