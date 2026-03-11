package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.admin.coupon.CouponDetailVO;
import com.xodud1202.springbackend.domain.admin.coupon.CouponPO;
import com.xodud1202.springbackend.domain.admin.coupon.CouponSavePO;
import com.xodud1202.springbackend.domain.admin.coupon.CouponTargetRowVO;
import com.xodud1202.springbackend.domain.admin.coupon.CouponTargetSaveRowPO;
import com.xodud1202.springbackend.domain.admin.coupon.CouponVO;
import com.xodud1202.springbackend.mapper.CouponMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 관리자 쿠폰 비즈니스 로직을 처리합니다.
@Service
@RequiredArgsConstructor
public class CouponService {
	private static final String SEARCH_GB_CPN_NO = "CPN_NO";
	private static final String SEARCH_GB_CPN_NM = "CPN_NM";
	private static final String DATE_GB_REG_DT = "REG_DT";
	private static final String DATE_GB_DOWN_DT = "DOWN_DT";
	private static final String CPN_STAT_WAIT = "CPN_STAT_01";
	private static final String CPN_TARGET_ALL = "CPN_TARGET_99";
	private static final String CPN_TARGET_GOODS = "CPN_TARGET_01";
	private static final String CPN_TARGET_BRAND = "CPN_TARGET_04";
	private static final String CPN_TARGET_EXHIBITION = "CPN_TARGET_02";
	private static final String CPN_TARGET_CATEGORY = "CPN_TARGET_03";
	private static final String CPN_USE_DT_PERIOD = "CPN_USE_DT_01";
	private static final String CPN_USE_DT_DATETIME = "CPN_USE_DT_02";
	private static final String TARGET_GB_APPLY = "TARGET_GB_01";
	private static final String TARGET_GB_EXCLUDE = "TARGET_GB_02";
	private static final int DEFAULT_PAGE = 1;
	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int MAX_PAGE_SIZE = 200;
	private static final int TARGET_BATCH_SIZE = 500;
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final DateTimeFormatter DATE_TIME_MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	private static final DateTimeFormatter DATE_TIME_SECOND_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final CouponMapper couponMapper;

	// 관리자 쿠폰 목록을 조회합니다.
	public Map<String, Object> getAdminCouponList(
		Integer page,
		Integer pageSize,
		String searchGb,
		String searchValue,
		String dateGb,
		String searchStartDt,
		String searchEndDt,
		String cpnStatCd,
		String cpnGbCd,
		String cpnTargetCd,
		String cpnDownAbleYn
	) {
		// 페이징 기본값을 계산합니다.
		int resolvedPage = page == null || page < 1 ? DEFAULT_PAGE : page;
		int resolvedPageSize = pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
		int offset = (resolvedPage - 1) * resolvedPageSize;

		// 검색 조건을 정규화합니다.
		String normalizedSearchValue = trimToNull(searchValue);
		String normalizedSearchGb = normalizeSearchGb(searchGb, normalizedSearchValue);
		String normalizedDateGb = normalizeDateGb(dateGb);
		String normalizedStartDt = normalizeSearchDate(searchStartDt, false);
		String normalizedEndDt = normalizeSearchDate(searchEndDt, true);
		if (normalizedStartDt != null && normalizedEndDt != null) {
			LocalDateTime startDateTime = LocalDateTime.parse(normalizedStartDt, DATE_TIME_SECOND_FORMATTER);
			LocalDateTime endDateTime = LocalDateTime.parse(normalizedEndDt, DATE_TIME_SECOND_FORMATTER);
			if (startDateTime.isAfter(endDateTime)) {
				throw new IllegalArgumentException("검색 시작일시는 종료일시보다 늦을 수 없습니다.");
			}
		}

		// 조회 파라미터를 구성합니다.
		CouponPO param = new CouponPO();
		param.setPage(resolvedPage);
		param.setPageSize(resolvedPageSize);
		param.setOffset(offset);
		param.setSearchGb(normalizedSearchGb);
		param.setSearchValue(normalizedSearchValue);
		param.setDateGb(normalizedDateGb);
		param.setSearchStartDt(normalizedStartDt);
		param.setSearchEndDt(normalizedEndDt);
		param.setCpnStatCd(trimToNull(cpnStatCd));
		param.setCpnGbCd(trimToNull(cpnGbCd));
		param.setCpnTargetCd(trimToNull(cpnTargetCd));
		param.setCpnDownAbleYn(normalizeYn(cpnDownAbleYn));

		// 목록과 건수를 조회합니다.
		List<CouponVO> list = couponMapper.getAdminCouponList(param);
		int totalCount = couponMapper.getAdminCouponCount(param);

		// 응답 데이터를 구성합니다.
		Map<String, Object> result = new HashMap<>();
		result.put("list", list);
		result.put("totalCount", totalCount);
		result.put("page", resolvedPage);
		result.put("pageSize", resolvedPageSize);
		return result;
	}

	// 관리자 쿠폰 상세를 조회합니다.
	public CouponDetailVO getAdminCouponDetail(Long cpnNo) {
		// 쿠폰 번호가 유효하지 않으면 조회하지 않습니다.
		if (cpnNo == null || cpnNo < 1) {
			return null;
		}
		return couponMapper.getAdminCouponDetail(cpnNo);
	}

	// 관리자 쿠폰 대상 목록을 조회합니다.
	public Map<String, Object> getAdminCouponTargetList(Long cpnNo) {
		// 쿠폰 번호 유효성을 확인합니다.
		if (cpnNo == null || cpnNo < 1) {
			throw new IllegalArgumentException("쿠폰 번호를 확인해주세요.");
		}
		CouponDetailVO detail = couponMapper.getAdminCouponDetail(cpnNo);
		if (detail == null) {
			throw new IllegalArgumentException("쿠폰 정보를 확인해주세요.");
		}

		// 쿠폰 타겟별 적용 대상을 조회합니다.
		List<CouponTargetRowVO> applyList = switch (detail.getCpnTargetCd()) {
			case CPN_TARGET_GOODS -> couponMapper.getAdminCouponApplyGoodsTargetList(cpnNo);
			case CPN_TARGET_BRAND -> couponMapper.getAdminCouponApplyBrandTargetList(cpnNo);
			case CPN_TARGET_EXHIBITION -> couponMapper.getAdminCouponApplyExhibitionTargetList(cpnNo);
			case CPN_TARGET_CATEGORY -> couponMapper.getAdminCouponApplyCategoryTargetList(cpnNo);
			default -> List.of();
		};

		// 제외 대상은 상품만 조회합니다.
		List<CouponTargetRowVO> excludeList = couponMapper.getAdminCouponExcludeGoodsTargetList(cpnNo);

		// 응답 데이터를 구성합니다.
		Map<String, Object> result = new HashMap<>();
		result.put("cpnTargetCd", detail.getCpnTargetCd());
		result.put("applyList", applyList);
		result.put("excludeList", excludeList);
		return result;
	}

	// 관리자 쿠폰 저장 요청을 검증합니다.
	public String validateCouponSave(CouponSavePO param) {
		// 요청 데이터 유효성을 확인합니다.
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}

		// 등록/수정 모드를 판별합니다.
		boolean isCreateMode = param.getCpnNo() == null || param.getCpnNo() < 1;
		if (isCreateMode && param.getRegNo() == null) {
			return "등록자 정보를 확인해주세요.";
		}
		if (!isCreateMode && param.getUdtNo() == null) {
			return "수정자 정보를 확인해주세요.";
		}
		if (!isCreateMode && (param.getCpnNo() == null || param.getCpnNo() < 1)) {
			return "쿠폰 번호를 확인해주세요.";
		}

		// 쿠폰 기본 필수값을 확인합니다.
		if (isBlank(param.getCpnNm())) {
			return "쿠폰명을 입력해주세요.";
		}
		if (isBlank(param.getCpnGbCd())) {
			return "쿠폰 종류를 선택해주세요.";
		}
		if (isBlank(param.getCpnTargetCd())) {
			return "쿠폰 타겟을 선택해주세요.";
		}
		if (isBlank(param.getCpnUseDtGb())) {
			return "사용 가능 기간 구분을 선택해주세요.";
		}

		// 쿠폰 코드값을 정규화합니다.
		param.setCpnNm(trimToNull(param.getCpnNm()));
		param.setCpnStatCd(trimToNull(param.getCpnStatCd()));
		param.setCpnGbCd(trimToNull(param.getCpnGbCd()));
		param.setCpnTargetCd(trimToNull(param.getCpnTargetCd()));
		param.setCpnUseDtGb(trimToNull(param.getCpnUseDtGb()));
		param.setCpnDownAbleYn(normalizeYnDefaultY(param.getCpnDownAbleYn()));

		// 쿠폰 상태 코드 기본값을 설정합니다.
		if (isBlank(param.getCpnStatCd())) {
			param.setCpnStatCd(CPN_STAT_WAIT);
		}

		// 다운로드 가능기간을 정규화합니다.
		String normalizedDownStartDt = normalizeSaveDateTime(param.getCpnDownStartDt(), false);
		String normalizedDownEndDt = normalizeSaveDateTime(param.getCpnDownEndDt(), true);
		if (normalizedDownStartDt == null || normalizedDownEndDt == null) {
			return "다운로드 가능 기간을 확인해주세요.";
		}
		if (LocalDateTime.parse(normalizedDownStartDt, DATE_TIME_SECOND_FORMATTER)
			.isAfter(LocalDateTime.parse(normalizedDownEndDt, DATE_TIME_SECOND_FORMATTER))) {
			return "다운로드 가능 시작일시는 종료일시보다 늦을 수 없습니다.";
		}
		param.setCpnDownStartDt(normalizedDownStartDt);
		param.setCpnDownEndDt(normalizedDownEndDt);

		// 사용 가능 기간 구분별 입력값을 검증합니다.
		if (CPN_USE_DT_PERIOD.equals(param.getCpnUseDtGb())) {
			if (param.getCpnUsableDt() == null || param.getCpnUsableDt() < 1) {
				return "다운로드 후 사용 가능 일수를 입력해주세요.";
			}
			param.setCpnUseStartDt(null);
			param.setCpnUseEndDt(null);
		} else if (CPN_USE_DT_DATETIME.equals(param.getCpnUseDtGb())) {
			String normalizedUseStartDt = normalizeSaveDateTime(param.getCpnUseStartDt(), false);
			String normalizedUseEndDt = normalizeSaveDateTime(param.getCpnUseEndDt(), true);
			if (normalizedUseStartDt == null || normalizedUseEndDt == null) {
				return "사용 가능 일시를 확인해주세요.";
			}
			if (LocalDateTime.parse(normalizedUseStartDt, DATE_TIME_SECOND_FORMATTER)
				.isAfter(LocalDateTime.parse(normalizedUseEndDt, DATE_TIME_SECOND_FORMATTER))) {
				return "사용 가능 시작일시는 종료일시보다 늦을 수 없습니다.";
			}
			param.setCpnUsableDt(null);
			param.setCpnUseStartDt(normalizedUseStartDt);
			param.setCpnUseEndDt(normalizedUseEndDt);
		} else {
			return "사용 가능 기간 구분을 확인해주세요.";
		}

		// 상태 중지 일시를 정규화합니다.
		String normalizedStatStopDt = normalizeSaveDateTime(param.getStatStopDt(), true);
		param.setStatStopDt(normalizedStatStopDt);

		// 쿠폰 타겟 코드 유효성을 확인합니다.
		if (!CPN_TARGET_ALL.equals(param.getCpnTargetCd())
			&& !CPN_TARGET_GOODS.equals(param.getCpnTargetCd())
			&& !CPN_TARGET_BRAND.equals(param.getCpnTargetCd())
			&& !CPN_TARGET_EXHIBITION.equals(param.getCpnTargetCd())
			&& !CPN_TARGET_CATEGORY.equals(param.getCpnTargetCd())) {
			return "쿠폰 타겟을 확인해주세요.";
		}

		// 적용/제외 대상 목록을 정규화합니다.
		param.setApplyTargets(normalizeTargetSaveRows(param.getApplyTargets(), TARGET_GB_APPLY));
		param.setExcludeTargets(normalizeTargetSaveRows(param.getExcludeTargets(), TARGET_GB_EXCLUDE));

		// 전체 타겟은 적용 대상을 저장하지 않습니다.
		if (CPN_TARGET_ALL.equals(param.getCpnTargetCd()) && !param.getApplyTargets().isEmpty()) {
			return "전체 타겟은 적용 대상을 등록할 수 없습니다.";
		}

		return null;
	}

	@Transactional
	// 관리자 쿠폰을 저장합니다.
	public Map<String, Object> saveCoupon(CouponSavePO param) {
		// 저장 전 유효성을 확인합니다.
		String validationMessage = validateCouponSave(param);
		if (validationMessage != null) {
			throw new IllegalArgumentException(validationMessage);
		}

		// 등록/수정 모드를 판별합니다.
		boolean isCreateMode = param.getCpnNo() == null || param.getCpnNo() < 1;
		Long writerNo = resolveWriterNo(param);
		if (writerNo == null) {
			throw new IllegalArgumentException("수정자 정보를 확인해주세요.");
		}

		// 쿠폰 기본 정보를 저장합니다.
		Long savedCouponNo;
		if (isCreateMode) {
			param.setUdtNo(param.getUdtNo() == null ? writerNo : param.getUdtNo());
			couponMapper.insertCouponBase(param);
			savedCouponNo = param.getCpnNo();
		} else {
			if (couponMapper.countCouponByNo(param.getCpnNo()) == 0) {
				throw new IllegalArgumentException("쿠폰 정보를 확인해주세요.");
			}
			param.setUdtNo(writerNo);
			couponMapper.updateCouponBase(param);
			savedCouponNo = param.getCpnNo();
		}
		if (savedCouponNo == null || savedCouponNo < 1) {
			throw new IllegalArgumentException("쿠폰 저장에 실패했습니다.");
		}

		// 쿠폰 대상을 재구성해 저장합니다.
		List<CouponTargetSaveRowPO> saveTargetRows = buildValidSaveTargetRows(param);
		couponMapper.deleteCouponTargetByCpnNo(savedCouponNo);
		if (!saveTargetRows.isEmpty()) {
			for (int startIndex = 0; startIndex < saveTargetRows.size(); startIndex += TARGET_BATCH_SIZE) {
				int endIndex = Math.min(startIndex + TARGET_BATCH_SIZE, saveTargetRows.size());
				List<CouponTargetSaveRowPO> batchRows = saveTargetRows.subList(startIndex, endIndex);
				couponMapper.insertCouponTargetBatch(savedCouponNo, writerNo, writerNo, batchRows);
			}
		}

		// 저장 결과를 반환합니다.
		Map<String, Object> result = new HashMap<>();
		result.put("cpnNo", savedCouponNo);
		result.put("savedTargetCount", saveTargetRows.size());
		result.put("isCreate", isCreateMode);
		return result;
	}

	// 쿠폰 대상 엑셀 파싱 요청을 검증합니다.
	public String validateCouponTargetExcelParse(MultipartFile file) {
		// 파일 유효성을 확인합니다.
		if (file == null || file.isEmpty()) {
			return "업로드할 엑셀 파일을 선택해주세요.";
		}
		String fileName = file.getOriginalFilename();
		if (fileName == null || !fileName.toLowerCase().endsWith(".xlsx")) {
			return "xlsx 파일만 업로드할 수 있습니다.";
		}
		return null;
	}

	// 쿠폰 대상 엑셀 업로드 데이터를 파싱합니다.
	public Map<String, Object> parseCouponTargetExcel(MultipartFile file) throws IOException {
		// 엑셀에서 상품코드 목록을 추출합니다.
		List<String> parsedGoodsIds = parseGoodsIdsFromExcel(file);
		List<CouponTargetRowVO> validGoodsRows = resolveValidGoodsTargets(parsedGoodsIds);

		// 응답 데이터를 구성합니다.
		Map<String, Object> result = new HashMap<>();
		result.put("list", validGoodsRows);
		result.put("uploadedCount", validGoodsRows.size());
		result.put("requestedCount", parsedGoodsIds.size());
		return result;
	}

	// 쿠폰 대상 엑셀 템플릿을 생성합니다.
	public byte[] buildCouponTargetExcelTemplate() throws IOException {
		// 엑셀 워크북을 생성합니다.
		try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet("template");
			Row header = sheet.createRow(0);
			header.createCell(0).setCellValue("상품코드");
			workbook.write(outputStream);
			return outputStream.toByteArray();
		}
	}

	// 저장 대상 목록을 구성합니다.
	private List<CouponTargetSaveRowPO> buildValidSaveTargetRows(CouponSavePO param) {
		// 적용/제외 대상의 유효 값을 계산합니다.
		List<CouponTargetRowVO> validApplyRows = switch (param.getCpnTargetCd()) {
			case CPN_TARGET_GOODS -> resolveValidGoodsTargets(extractTargetValues(param.getApplyTargets()));
			case CPN_TARGET_BRAND -> resolveValidBrandTargets(extractTargetValues(param.getApplyTargets()));
			case CPN_TARGET_EXHIBITION -> resolveValidExhibitionTargets(extractTargetValues(param.getApplyTargets()));
			case CPN_TARGET_CATEGORY -> resolveValidCategoryTargets(extractTargetValues(param.getApplyTargets()));
			default -> List.of();
		};
		List<CouponTargetRowVO> validExcludeRows = resolveValidGoodsTargets(extractTargetValues(param.getExcludeTargets()));

		// 최종 저장 대상 목록을 중복 제거해 생성합니다.
		Map<String, CouponTargetSaveRowPO> deduplicatedTargetMap = new LinkedHashMap<>();
		for (CouponTargetRowVO row : validApplyRows) {
			if (row == null || isBlank(row.getTargetValue())) {
				continue;
			}
			CouponTargetSaveRowPO saveRow = new CouponTargetSaveRowPO();
			saveRow.setTargetGbCd(TARGET_GB_APPLY);
			saveRow.setTargetValue(row.getTargetValue());
			deduplicatedTargetMap.put(TARGET_GB_APPLY + "::" + row.getTargetValue(), saveRow);
		}
		for (CouponTargetRowVO row : validExcludeRows) {
			if (row == null || isBlank(row.getTargetValue())) {
				continue;
			}
			CouponTargetSaveRowPO saveRow = new CouponTargetSaveRowPO();
			saveRow.setTargetGbCd(TARGET_GB_EXCLUDE);
			saveRow.setTargetValue(row.getTargetValue());
			deduplicatedTargetMap.put(TARGET_GB_EXCLUDE + "::" + row.getTargetValue(), saveRow);
		}
		return new ArrayList<>(deduplicatedTargetMap.values());
	}

	// 상품 대상 목록에서 유효한 상품만 반환합니다.
	private List<CouponTargetRowVO> resolveValidGoodsTargets(List<String> targetValues) {
		// 대상값이 없으면 빈 목록을 반환합니다.
		if (targetValues.isEmpty()) {
			return List.of();
		}
		List<CouponTargetRowVO> existingRows = couponMapper.getExistingGoodsTargetRows(targetValues);
		Map<String, CouponTargetRowVO> existingRowMap = new LinkedHashMap<>();
		for (CouponTargetRowVO row : existingRows) {
			if (row == null || isBlank(row.getTargetValue())) {
				continue;
			}
			existingRowMap.put(row.getTargetValue(), row);
		}

		// 입력 순서를 유지하며 유효 대상만 반영합니다.
		List<CouponTargetRowVO> result = new ArrayList<>();
		for (String targetValue : targetValues) {
			CouponTargetRowVO existingRow = existingRowMap.get(targetValue);
			if (existingRow != null) {
				result.add(existingRow);
			}
		}
		return result;
	}

	// 브랜드 대상 목록에서 유효한 브랜드만 반환합니다.
	private List<CouponTargetRowVO> resolveValidBrandTargets(List<String> targetValues) {
		// 대상값이 없으면 빈 목록을 반환합니다.
		if (targetValues.isEmpty()) {
			return List.of();
		}

		// 숫자형 브랜드 번호만 추출합니다.
		List<Integer> brandNoList = new ArrayList<>();
		for (String targetValue : targetValues) {
			try {
				brandNoList.add(Integer.parseInt(targetValue));
			} catch (NumberFormatException ignored) {
				// 숫자 변환이 불가능한 값은 무시합니다.
			}
		}
		if (brandNoList.isEmpty()) {
			return List.of();
		}

		// 유효한 브랜드 목록을 조회합니다.
		List<CouponTargetRowVO> existingRows = couponMapper.getExistingBrandTargetRows(brandNoList);
		Map<String, CouponTargetRowVO> existingRowMap = new LinkedHashMap<>();
		for (CouponTargetRowVO row : existingRows) {
			if (row == null || isBlank(row.getTargetValue())) {
				continue;
			}
			existingRowMap.put(row.getTargetValue(), row);
		}

		// 입력 순서를 유지하며 유효 대상만 반영합니다.
		List<CouponTargetRowVO> result = new ArrayList<>();
		for (String targetValue : targetValues) {
			CouponTargetRowVO existingRow = existingRowMap.get(targetValue);
			if (existingRow != null) {
				result.add(existingRow);
			}
		}
		return result;
	}

	// 기획전 대상 목록에서 유효한 기획전만 반환합니다.
	private List<CouponTargetRowVO> resolveValidExhibitionTargets(List<String> targetValues) {
		// 대상값이 없으면 빈 목록을 반환합니다.
		if (targetValues.isEmpty()) {
			return List.of();
		}

		// 숫자형 기획전 번호만 추출합니다.
		List<Integer> exhibitionNoList = new ArrayList<>();
		Map<String, String> originalValueMap = new LinkedHashMap<>();
		for (String targetValue : targetValues) {
			try {
				Integer parsedNo = Integer.parseInt(targetValue);
				exhibitionNoList.add(parsedNo);
				originalValueMap.put(String.valueOf(parsedNo), targetValue);
			} catch (NumberFormatException ignored) {
				// 숫자 변환이 불가능한 값은 무시합니다.
			}
		}
		if (exhibitionNoList.isEmpty()) {
			return List.of();
		}

		// 유효한 기획전 목록을 조회합니다.
		List<CouponTargetRowVO> existingRows = couponMapper.getExistingExhibitionTargetRows(exhibitionNoList);
		Map<String, CouponTargetRowVO> existingRowMap = new LinkedHashMap<>();
		for (CouponTargetRowVO row : existingRows) {
			if (row == null || isBlank(row.getTargetValue())) {
				continue;
			}
			existingRowMap.put(row.getTargetValue(), row);
		}

		// 입력 순서를 유지하며 유효 대상만 반영합니다.
		List<CouponTargetRowVO> result = new ArrayList<>();
		for (String targetValue : targetValues) {
			CouponTargetRowVO existingRow = existingRowMap.get(targetValue);
			if (existingRow != null) {
				result.add(existingRow);
			}
		}
		return result;
	}

	// 카테고리 대상 목록에서 유효한 카테고리만 반환합니다.
	private List<CouponTargetRowVO> resolveValidCategoryTargets(List<String> targetValues) {
		// 대상값이 없으면 빈 목록을 반환합니다.
		if (targetValues.isEmpty()) {
			return List.of();
		}
		List<CouponTargetRowVO> existingRows = couponMapper.getExistingCategoryTargetRows(targetValues);
		Map<String, CouponTargetRowVO> existingRowMap = new LinkedHashMap<>();
		for (CouponTargetRowVO row : existingRows) {
			if (row == null || isBlank(row.getTargetValue())) {
				continue;
			}
			existingRowMap.put(row.getTargetValue(), row);
		}

		// 입력 순서를 유지하며 유효 대상만 반영합니다.
		List<CouponTargetRowVO> result = new ArrayList<>();
		for (String targetValue : targetValues) {
			CouponTargetRowVO existingRow = existingRowMap.get(targetValue);
			if (existingRow != null) {
				result.add(existingRow);
			}
		}
		return result;
	}

	// 저장 요청 대상 목록을 정규화합니다.
	private List<CouponTargetSaveRowPO> normalizeTargetSaveRows(List<CouponTargetSaveRowPO> sourceRows, String targetGbCd) {
		// 입력 목록이 비어있으면 빈 목록을 반환합니다.
		if (sourceRows == null || sourceRows.isEmpty()) {
			return List.of();
		}

		// 대상 값을 중복 없이 정규화합니다.
		Set<String> deduplicatedValueSet = new LinkedHashSet<>();
		List<CouponTargetSaveRowPO> normalizedRows = new ArrayList<>();
		for (CouponTargetSaveRowPO row : sourceRows) {
			if (row == null) {
				continue;
			}
			String normalizedValue = trimToNull(row.getTargetValue());
			if (normalizedValue == null || !deduplicatedValueSet.add(normalizedValue)) {
				continue;
			}
			CouponTargetSaveRowPO normalizedRow = new CouponTargetSaveRowPO();
			normalizedRow.setTargetGbCd(targetGbCd);
			normalizedRow.setTargetValue(normalizedValue);
			normalizedRows.add(normalizedRow);
		}
		return normalizedRows;
	}

	// 저장 요청 대상값 목록을 추출합니다.
	private List<String> extractTargetValues(List<CouponTargetSaveRowPO> rows) {
		// 입력 목록이 비어있으면 빈 목록을 반환합니다.
		if (rows == null || rows.isEmpty()) {
			return List.of();
		}

		// 대상값을 중복 없이 추출합니다.
		Set<String> deduplicatedValueSet = new LinkedHashSet<>();
		for (CouponTargetSaveRowPO row : rows) {
			if (row == null) {
				continue;
			}
			String normalizedValue = trimToNull(row.getTargetValue());
			if (normalizedValue == null) {
				continue;
			}
			deduplicatedValueSet.add(normalizedValue);
		}
		return new ArrayList<>(deduplicatedValueSet);
	}

	// 저장 대상 일시 문자열을 정규화합니다.
	private String normalizeSaveDateTime(String value, boolean isEnd) {
		// 입력값이 없으면 null을 반환합니다.
		String normalized = trimToNull(value);
		if (normalized == null) {
			return null;
		}
		normalized = normalized.replace("T", " ");

		// 종료시각 24시는 23:59:59로 변환합니다.
		if (normalized.matches("^\\d{4}-\\d{2}-\\d{2}\\s24(:00(:00)?)?$")) {
			if (!isEnd) {
				throw new IllegalArgumentException("시작시간에는 24시를 사용할 수 없습니다.");
			}
			return normalized.substring(0, 10) + " 23:59:59";
		}

		try {
			// 날짜만 입력된 경우 시작/종료 시각을 보정합니다.
			if (normalized.length() == 10) {
				LocalDate parsedDate = LocalDate.parse(normalized, DATE_FORMATTER);
				LocalDateTime parsedDateTime = isEnd ? parsedDate.atTime(23, 59, 59) : parsedDate.atStartOfDay();
				return DATE_TIME_SECOND_FORMATTER.format(parsedDateTime);
			}
			// 분 단위 입력은 초를 보정합니다.
			if (normalized.length() == 16) {
				LocalDateTime parsedDateTime = LocalDateTime.parse(normalized, DATE_TIME_MINUTE_FORMATTER);
				LocalDateTime resolvedDateTime = isEnd
					? parsedDateTime.withSecond(59)
					: parsedDateTime.withSecond(0);
				return DATE_TIME_SECOND_FORMATTER.format(resolvedDateTime);
			}
			// 초 단위 입력은 그대로 사용합니다.
			if (normalized.length() == 19) {
				LocalDateTime parsedDateTime = LocalDateTime.parse(normalized, DATE_TIME_SECOND_FORMATTER);
				return DATE_TIME_SECOND_FORMATTER.format(parsedDateTime);
			}
		} catch (DateTimeParseException exception) {
			throw new IllegalArgumentException("일시 형식을 확인해주세요.");
		}
		throw new IllegalArgumentException("일시 형식을 확인해주세요.");
	}

	// 엑셀에서 상품코드 목록을 추출합니다.
	private List<String> parseGoodsIdsFromExcel(MultipartFile file) throws IOException {
		// 파일 유효성을 검증합니다.
		String validationMessage = validateCouponTargetExcelParse(file);
		if (validationMessage != null) {
			throw new IllegalArgumentException(validationMessage);
		}

		Set<String> deduplicatedGoodsIdSet = new LinkedHashSet<>();
		try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
			Sheet sheet = workbook.getSheetAt(0);
			if (sheet == null) {
				return List.of();
			}

			// 헤더 존재 여부를 확인하고 시작 행을 결정합니다.
			int startRow = 0;
			Row headerRow = sheet.getRow(0);
			String firstValue = getCellString(headerRow, 0);
			if ("상품코드".equals(firstValue)) {
				startRow = 1;
			}

			// 데이터 행에서 상품코드를 추출합니다.
			for (int rowIndex = startRow; rowIndex <= sheet.getLastRowNum(); rowIndex += 1) {
				Row row = sheet.getRow(rowIndex);
				String goodsId = trimToNull(getCellString(row, 0));
				if (goodsId == null) {
					continue;
				}
				deduplicatedGoodsIdSet.add(goodsId);
			}
		}
		return new ArrayList<>(deduplicatedGoodsIdSet);
	}

	// 엑셀 셀 문자열 값을 반환합니다.
	private String getCellString(Row row, int cellIndex) {
		// 행이나 셀이 없으면 빈 문자열을 반환합니다.
		if (row == null || row.getCell(cellIndex) == null) {
			return "";
		}
		try {
			String value = row.getCell(cellIndex).getStringCellValue();
			return value == null ? "" : value.trim();
		} catch (Exception exception) {
			try {
				return String.valueOf((long) row.getCell(cellIndex).getNumericCellValue());
			} catch (Exception ignored) {
				return "";
			}
		}
	}

	// 검색 구분을 정규화합니다.
	private String normalizeSearchGb(String searchGb, String searchValue) {
		// 검색어가 없으면 검색 구분을 사용하지 않습니다.
		if (searchValue == null) {
			return null;
		}
		String normalizedSearchGb = trimToNull(searchGb);
		if (SEARCH_GB_CPN_NO.equals(normalizedSearchGb) || SEARCH_GB_CPN_NM.equals(normalizedSearchGb)) {
			return normalizedSearchGb;
		}
		return SEARCH_GB_CPN_NM;
	}

	// 기간 검색 구분을 정규화합니다.
	private String normalizeDateGb(String dateGb) {
		String normalizedDateGb = trimToNull(dateGb);
		if (DATE_GB_REG_DT.equals(normalizedDateGb) || DATE_GB_DOWN_DT.equals(normalizedDateGb)) {
			return normalizedDateGb;
		}
		return DATE_GB_REG_DT;
	}

	// 조회 날짜/일시를 DB 비교 형식으로 정규화합니다.
	private String normalizeSearchDate(String value, boolean isEnd) {
		String normalized = trimToNull(value);
		if (normalized == null) {
			return null;
		}
		normalized = normalized.replace("T", " ");
		try {
			// 날짜만 전달되면 일 시작/종료 시각으로 보정합니다.
			if (normalized.length() == 10) {
				LocalDate localDate = LocalDate.parse(normalized, DATE_FORMATTER);
				LocalDateTime localDateTime = isEnd ? localDate.atTime(23, 59, 59) : localDate.atStartOfDay();
				return DATE_TIME_SECOND_FORMATTER.format(localDateTime);
			}
			// 분 단위 값은 초를 보정합니다.
			if (normalized.length() == 16) {
				LocalDateTime localDateTime = LocalDateTime.parse(normalized, DATE_TIME_MINUTE_FORMATTER);
				LocalDateTime resolvedDateTime = isEnd
					? localDateTime.withSecond(59)
					: localDateTime.withSecond(0);
				return DATE_TIME_SECOND_FORMATTER.format(resolvedDateTime);
			}
			// 초까지 전달된 값은 그대로 사용합니다.
			if (normalized.length() == 19) {
				LocalDateTime localDateTime = LocalDateTime.parse(normalized, DATE_TIME_SECOND_FORMATTER);
				return DATE_TIME_SECOND_FORMATTER.format(localDateTime);
			}
		} catch (DateTimeParseException exception) {
			throw new IllegalArgumentException("검색 기간 형식을 확인해주세요.");
		}
		throw new IllegalArgumentException("검색 기간 형식을 확인해주세요.");
	}

	// Y/N 값을 정규화합니다.
	private String normalizeYn(String value) {
		String normalized = trimToNull(value);
		if ("Y".equals(normalized) || "N".equals(normalized)) {
			return normalized;
		}
		return null;
	}

	// Y/N 값을 정규화하고 기본값을 Y로 설정합니다.
	private String normalizeYnDefaultY(String value) {
		String normalized = normalizeYn(value);
		return normalized == null ? "Y" : normalized;
	}

	// 저장자 번호를 계산합니다.
	private Long resolveWriterNo(CouponSavePO param) {
		// 수정자 우선, 없으면 등록자를 사용합니다.
		if (param == null) {
			return null;
		}
		if (param.getUdtNo() != null) {
			return param.getUdtNo();
		}
		return param.getRegNo();
	}

	// 문자열 공백 여부를 확인합니다.
	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	// 문자열 공백을 제거하고 빈값은 null로 반환합니다.
	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
