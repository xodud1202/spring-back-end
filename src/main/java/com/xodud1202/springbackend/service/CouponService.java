package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.admin.coupon.CouponPO;
import com.xodud1202.springbackend.domain.admin.coupon.CouponVO;
import com.xodud1202.springbackend.mapper.CouponMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 관리자 쿠폰 목록 비즈니스 로직을 처리합니다.
@Service
@RequiredArgsConstructor
public class CouponService {
	private static final String SEARCH_GB_CPN_NO = "CPN_NO";
	private static final String SEARCH_GB_CPN_NM = "CPN_NM";
	private static final String DATE_GB_REG_DT = "REG_DT";
	private static final String DATE_GB_DOWN_DT = "DOWN_DT";
	private static final int DEFAULT_PAGE = 1;
	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int MAX_PAGE_SIZE = 200;
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final DateTimeFormatter DATE_TIME_MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
			LocalDateTime startDateTime = LocalDateTime.parse(normalizedStartDt, DATE_TIME_FORMATTER);
			LocalDateTime endDateTime = LocalDateTime.parse(normalizedEndDt, DATE_TIME_FORMATTER);
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
				return DATE_TIME_FORMATTER.format(localDateTime);
			}
			// 분 단위 값은 초를 보정합니다.
			if (normalized.length() == 16) {
				LocalDateTime localDateTime = LocalDateTime.parse(normalized, DATE_TIME_MINUTE_FORMATTER);
				LocalDateTime resolvedDateTime = isEnd
					? localDateTime.withSecond(59)
					: localDateTime.withSecond(0);
				return DATE_TIME_FORMATTER.format(resolvedDateTime);
			}
			// 초까지 전달된 값은 그대로 사용합니다.
			if (normalized.length() == 19) {
				LocalDateTime localDateTime = LocalDateTime.parse(normalized, DATE_TIME_FORMATTER);
				return DATE_TIME_FORMATTER.format(localDateTime);
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

	// 문자열 공백을 제거하고 빈값은 null로 반환합니다.
	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
