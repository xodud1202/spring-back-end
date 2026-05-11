package com.xodud1202.springbackend.controller.work;

import com.xodud1202.springbackend.domain.admin.common.UserInfoVO;
import com.xodud1202.springbackend.domain.work.vacation.WorkVacationBootstrapResponseVO;
import com.xodud1202.springbackend.domain.work.vacation.WorkVacationCreatePO;
import com.xodud1202.springbackend.domain.work.vacation.WorkVacationCreateResponseVO;
import com.xodud1202.springbackend.domain.work.vacation.WorkVacationListResponseVO;
import com.xodud1202.springbackend.service.UserBaseService;
import com.xodud1202.springbackend.service.VacationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
// 업무관리 로그인 세션을 사용하는 휴가관리 API를 제공합니다.
public class WorkVacationController extends WorkControllerSupport {
	private final VacationService vacationService;
	private final UserBaseService userBaseService;

	@GetMapping("/api/work/vacation/bootstrap")
	// 휴가관리 화면 초기 구동 데이터를 조회합니다.
	public ResponseEntity<WorkVacationBootstrapResponseVO> getBootstrap(HttpServletRequest request) {
		try {
			// 로그인 사용자와 휴가관리 선택 목록을 함께 반환합니다.
			Long workUserNo = resolveRequiredWorkUserNo(request);
			UserInfoVO currentUser = userBaseService.getUserInfoByUsrNo(workUserNo).orElse(null);
			return ResponseEntity.ok(vacationService.getWorkVacationBootstrap(currentUser));
		} catch (SecurityException | IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("휴가관리 bootstrap 조회 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("휴가관리 초기 데이터 조회에 실패했습니다.", exception);
		}
	}

	@GetMapping("/api/work/vacation/list")
	// 휴가자와 회사 조건에 맞는 연차 요약과 사용 목록을 조회합니다.
	public ResponseEntity<WorkVacationListResponseVO> getVacationList(
		HttpServletRequest request,
		@RequestParam(required = false) Integer personSeq,
		@RequestParam(required = false) Integer workCompanySeq,
		@RequestParam(required = false) Integer vacationYear,
		@RequestParam(required = false) String defaultCompanyYn
	) {
		try {
			// 로그인 세션 확인 후 선택 조건 기준으로 휴가 목록을 반환합니다.
			resolveRequiredWorkUserNo(request);
			return ResponseEntity.ok(vacationService.getWorkVacationList(personSeq, workCompanySeq, vacationYear, defaultCompanyYn));
		} catch (SecurityException | IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("휴가관리 목록 조회 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("휴가관리 목록 조회에 실패했습니다.", exception);
		}
	}

	@PostMapping("/api/work/vacation")
	// 휴가 사용 내역을 등록합니다.
	public ResponseEntity<WorkVacationCreateResponseVO> createVacation(
		HttpServletRequest request,
		@RequestBody WorkVacationCreatePO command
	) {
		try {
			// 로그인 사용자번호를 등록자와 수정자로 사용합니다.
			Long workUserNo = resolveRequiredWorkUserNo(request);
			return ResponseEntity.ok(vacationService.createWorkVacation(command, workUserNo));
		} catch (SecurityException | IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("휴가 등록 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("휴가 등록에 실패했습니다.", exception);
		}
	}

}
