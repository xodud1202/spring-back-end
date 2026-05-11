package com.xodud1202.springbackend.service;

import static com.xodud1202.springbackend.common.util.CommonTextUtils.*;
import static com.xodud1202.springbackend.common.util.CommonValidationUtils.*;

import com.xodud1202.springbackend.domain.admin.common.UserInfoVO;
import com.xodud1202.springbackend.domain.common.CommonCodeVO;
import com.xodud1202.springbackend.domain.work.vacation.WorkVacationBootstrapResponseVO;
import com.xodud1202.springbackend.domain.work.vacation.WorkVacationCompanyVO;
import com.xodud1202.springbackend.domain.work.vacation.WorkVacationCreatePO;
import com.xodud1202.springbackend.domain.work.vacation.WorkVacationCreateResponseVO;
import com.xodud1202.springbackend.domain.work.vacation.WorkVacationListResponseVO;
import com.xodud1202.springbackend.domain.work.vacation.WorkVacationListRowVO;
import com.xodud1202.springbackend.domain.work.vacation.WorkVacationListSearchPO;
import com.xodud1202.springbackend.domain.work.vacation.WorkVacationPersonVO;
import com.xodud1202.springbackend.domain.work.vacation.WorkVacationSummaryRowVO;
import com.xodud1202.springbackend.mapper.CommonMapper;
import com.xodud1202.springbackend.mapper.VacationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
// 휴가관리 조회와 등록 비즈니스 로직을 처리합니다.
public class VacationService {
	private static final String VACATION_GROUP_CODE = "VACATION";
	private static final String MORNING_HALF_VACATION_CODE = "VACATION_02";
	private static final String AFTERNOON_HALF_VACATION_CODE = "VACATION_03";
	private static final int VACATION_MEMO_MAX_LENGTH = 65535;
	private static final DateTimeFormatter VACATION_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

	private final VacationMapper vacationMapper;
	private final CommonMapper commonMapper;

	// 휴가관리 화면 초기 구동 데이터를 조회합니다.
	public WorkVacationBootstrapResponseVO getWorkVacationBootstrap(UserInfoVO currentUser) {
		WorkVacationBootstrapResponseVO response = new WorkVacationBootstrapResponseVO();
		response.setCurrentUser(currentUser);
		response.setCompanyList(getVacationCompanyList());
		response.setPersonList(getVacationPersonList());
		response.setVacationCodeList(getVacationCodeList());
		return response;
	}

	// 휴가자와 회사 조건 기준으로 선택 회사, 연도 목록, 연차 요약, 휴가 사용 목록을 조회합니다.
	public WorkVacationListResponseVO getWorkVacationList(Integer personSeq, Integer workCompanySeq, Integer vacationYear, String defaultCompanyYn) {
		Integer selectedWorkCompanySeq = resolveSelectedWorkCompanySeq(personSeq, workCompanySeq, defaultCompanyYn);
		WorkVacationListSearchPO yearSearchParam = buildVacationSearchParam(personSeq, selectedWorkCompanySeq, null);
		List<Integer> yearList = vacationMapper.getVacationYearList(yearSearchParam);
		Integer selectedYear = resolveSelectedVacationYear(vacationYear, yearList);
		WorkVacationListSearchPO searchParam = buildVacationSearchParam(personSeq, selectedWorkCompanySeq, selectedYear);
		List<WorkVacationSummaryRowVO> summaryList = vacationMapper.getVacationSummaryList(searchParam);
		List<WorkVacationListRowVO> vacationList = vacationMapper.getVacationList(searchParam);

		WorkVacationListResponseVO response = new WorkVacationListResponseVO();
		response.setSelectedWorkCompanySeq(selectedWorkCompanySeq);
		response.setYearList(yearList == null ? List.of() : yearList);
		response.setSelectedYear(selectedYear);
		response.setSummaryList(summaryList == null ? List.of() : summaryList);
		response.setVacationList(vacationList == null ? List.of() : vacationList);
		return response;
	}

	@Transactional
	// 휴가 사용 내역을 등록합니다.
	public WorkVacationCreateResponseVO createWorkVacation(WorkVacationCreatePO command, Long workUserNo) {
		WorkVacationCreatePO normalizedCommand = normalizeVacationCreateCommand(command, workUserNo);
		validateVacationCreateCommand(normalizedCommand);
		vacationMapper.insertVacation(normalizedCommand);

		WorkVacationCreateResponseVO response = new WorkVacationCreateResponseVO();
		response.setMessage("휴가가 등록되었습니다.");
		response.setVacationSeq(normalizedCommand.getVacationSeq());
		return response;
	}

	// 휴가 사용 가능 회사 목록을 조회합니다.
	private List<WorkVacationCompanyVO> getVacationCompanyList() {
		List<WorkVacationCompanyVO> companyList = vacationMapper.getVacationCompanyList();
		return companyList == null ? List.of() : companyList;
	}

	// 휴가자 목록을 조회합니다.
	private List<WorkVacationPersonVO> getVacationPersonList() {
		List<WorkVacationPersonVO> personList = vacationMapper.getVacationPersonList();
		return personList == null ? List.of() : personList;
	}

	// 휴가구분 공통코드를 조회합니다.
	private List<CommonCodeVO> getVacationCodeList() {
		List<CommonCodeVO> vacationCodeList = commonMapper.getCommonCodeList(VACATION_GROUP_CODE);
		return vacationCodeList == null ? List.of() : vacationCodeList;
	}

	// 목록 검색 조건을 정규화합니다.
	private WorkVacationListSearchPO buildVacationSearchParam(Integer personSeq, Integer workCompanySeq, Integer vacationYear) {
		WorkVacationListSearchPO searchParam = new WorkVacationListSearchPO();
		searchParam.setPersonSeq(normalizeOptionalPositiveInt(personSeq, "휴가자 정보를 확인해주세요."));
		searchParam.setWorkCompanySeq(normalizeOptionalPositiveInt(workCompanySeq, "회사 정보를 확인해주세요."));
		searchParam.setVacationYear(normalizeOptionalPositiveInt(vacationYear, "휴가년도를 확인해주세요."));
		return searchParam;
	}

	// 요청 회사가 없고 기본 선택이 필요하면 DISP_ORD가 가장 낮은 휴가 존재 회사를 선택합니다.
	private Integer resolveSelectedWorkCompanySeq(Integer personSeq, Integer workCompanySeq, String defaultCompanyYn) {
		Integer normalizedWorkCompanySeq = normalizeOptionalPositiveInt(workCompanySeq, "회사 정보를 확인해주세요.");
		if (normalizedWorkCompanySeq != null || !isDefaultCompanyRequested(defaultCompanyYn)) {
			return normalizedWorkCompanySeq;
		}
		WorkVacationListSearchPO defaultCompanySearchParam = buildVacationSearchParam(personSeq, null, null);
		return vacationMapper.getDefaultVacationCompanySeq(defaultCompanySearchParam);
	}

	// 회사 기본 선택 요청 여부를 확인합니다.
	private boolean isDefaultCompanyRequested(String defaultCompanyYn) {
		return "Y".equalsIgnoreCase(trimToNull(defaultCompanyYn));
	}

	// 요청 연도가 선택 가능한 연도이면 유지하고, 아니면 최신 휴가년도를 선택합니다.
	private Integer resolveSelectedVacationYear(Integer vacationYear, List<Integer> yearList) {
		Integer normalizedVacationYear = normalizeOptionalPositiveInt(vacationYear, "휴가년도를 확인해주세요.");
		if (yearList == null || yearList.isEmpty()) {
			return null;
		}
		if (normalizedVacationYear != null && yearList.contains(normalizedVacationYear)) {
			return normalizedVacationYear;
		}
		return yearList.get(0);
	}

	// 선택 정수값을 null 또는 양수로 정규화합니다.
	private Integer normalizeOptionalPositiveInt(Integer value, String invalidMessage) {
		if (value == null) {
			return null;
		}
		return requirePositiveInt(value, invalidMessage);
	}

	// 등록 요청 값을 저장 가능한 값으로 정규화합니다.
	private WorkVacationCreatePO normalizeVacationCreateCommand(WorkVacationCreatePO command, Long workUserNo) {
		if (command == null) {
			throw new IllegalArgumentException("휴가 등록 요청 정보를 확인해주세요.");
		}

		WorkVacationCreatePO normalizedCommand = new WorkVacationCreatePO();
		normalizedCommand.setPersonSeq(requirePositiveInt(command.getPersonSeq(), "이름을 선택해주세요."));
		normalizedCommand.setWorkCompanySeq(requirePositiveInt(command.getWorkCompanySeq(), "회사를 선택해주세요."));
		normalizedCommand.setVacationCd(requireVacationCodeText(command.getVacationCd()));
		normalizedCommand.setStartDt(requireVacationDateText(command.getStartDt(), "시작일을 선택해주세요."));
		normalizedCommand.setEndDt(requireVacationDateText(command.getEndDt(), "종료일을 선택해주세요."));
		normalizedCommand.setVacationMemo(normalizeVacationMemo(command.getVacationMemo()));
		normalizedCommand.setRegNo(requirePositiveLong(workUserNo, "로그인이 필요합니다."));
		normalizedCommand.setUdtNo(workUserNo);
		return normalizedCommand;
	}

	// 휴가 등록 요청의 DB 정합성을 검증합니다.
	private void validateVacationCreateCommand(WorkVacationCreatePO command) {
		validateActiveVacationPerson(command.getPersonSeq());
		validateVacationCompany(command.getWorkCompanySeq());
		validateVacationCode(command.getVacationCd());
		validateVacationDateRange(command.getVacationCd(), command.getStartDt(), command.getEndDt());
	}

	// 휴가자 존재 여부를 검증합니다.
	private void validateActiveVacationPerson(Integer personSeq) {
		if (vacationMapper.countActiveVacationPerson(personSeq) < 1) {
			throw new IllegalArgumentException("휴가자를 확인해주세요.");
		}
	}

	// 휴가 사용 가능 회사 여부를 검증합니다.
	private void validateVacationCompany(Integer workCompanySeq) {
		if (vacationMapper.countVacationCompany(workCompanySeq) < 1) {
			throw new IllegalArgumentException("휴가 사용 회사를 확인해주세요.");
		}
	}

	// 휴가 구분 코드가 사용 가능한 공통코드인지 검증합니다.
	private void validateVacationCode(String vacationCd) {
		for (CommonCodeVO vacationCodeItem : getVacationCodeList()) {
			if (vacationCd.equals(vacationCodeItem.getCd())) {
				return;
			}
		}
		throw new IllegalArgumentException("휴가구분을 확인해주세요.");
	}

	// 휴가 시작일과 종료일의 기간 규칙을 검증합니다.
	private void validateVacationDateRange(String vacationCd, String startDt, String endDt) {
		LocalDate startDate = parseVacationDate(startDt, "시작일을 확인해주세요.");
		LocalDate endDate = parseVacationDate(endDt, "종료일을 확인해주세요.");
		if (endDate.isBefore(startDate)) {
			throw new IllegalArgumentException("종료일은 시작일보다 빠를 수 없습니다.");
		}
		if (isHalfVacationCode(vacationCd) && !startDate.equals(endDate)) {
			throw new IllegalArgumentException("반차는 시작일과 종료일이 같아야 합니다.");
		}
	}

	// 휴가 구분 코드를 필수 문자열로 정규화합니다.
	private String requireVacationCodeText(String vacationCd) {
		String normalizedVacationCd = trimToNull(vacationCd);
		if (normalizedVacationCd == null) {
			throw new IllegalArgumentException("휴가구분을 선택해주세요.");
		}
		return normalizedVacationCd;
	}

	// 휴가 날짜 문자열을 필수 ISO 날짜로 정규화합니다.
	private String requireVacationDateText(String dateText, String invalidMessage) {
		LocalDate vacationDate = parseVacationDate(dateText, invalidMessage);
		return VACATION_DATE_FORMATTER.format(vacationDate);
	}

	// 휴가 날짜 문자열을 LocalDate로 변환합니다.
	private LocalDate parseVacationDate(String dateText, String invalidMessage) {
		String normalizedDateText = trimToNull(dateText);
		if (normalizedDateText == null) {
			throw new IllegalArgumentException(invalidMessage);
		}
		try {
			return LocalDate.parse(normalizedDateText, VACATION_DATE_FORMATTER);
		} catch (DateTimeParseException exception) {
			throw new IllegalArgumentException(invalidMessage);
		}
	}

	// 휴가 사유를 저장 가능한 문자열로 정규화합니다.
	private String normalizeVacationMemo(String vacationMemo) {
		String normalizedVacationMemo = trimToNull(vacationMemo);
		if (normalizedVacationMemo != null && normalizedVacationMemo.length() > VACATION_MEMO_MAX_LENGTH) {
			throw new IllegalArgumentException("휴가사유가 너무 깁니다.");
		}
		return normalizedVacationMemo;
	}

	// 반차 휴가구분 코드인지 확인합니다.
	private boolean isHalfVacationCode(String vacationCd) {
		return MORNING_HALF_VACATION_CODE.equals(vacationCd) || AFTERNOON_HALF_VACATION_CODE.equals(vacationCd);
	}

}
