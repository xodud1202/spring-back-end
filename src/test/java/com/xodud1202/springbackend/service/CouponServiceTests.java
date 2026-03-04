package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.admin.coupon.CouponPO;
import com.xodud1202.springbackend.domain.admin.coupon.CouponVO;
import com.xodud1202.springbackend.mapper.CouponMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// CouponService의 검색 조건 정규화 로직을 검증합니다.
class CouponServiceTests {

	// 쿠폰 매퍼 목 객체입니다.
	@Mock
	private CouponMapper couponMapper;

	// 테스트 대상 서비스입니다.
	@InjectMocks
	private CouponService couponService;

	@Test
	@DisplayName("목록 조회: 페이징 기본값과 검색 기본값을 적용한다")
	// 목록 조회 시 기본 페이징과 기본 검색 구분을 적용합니다.
	void getAdminCouponList_appliesDefaultPagingAndSearch() {
		// 목록/카운트 응답을 목으로 고정합니다.
		when(couponMapper.getAdminCouponList(any(CouponPO.class))).thenReturn(Collections.emptyList());
		when(couponMapper.getAdminCouponCount(any(CouponPO.class))).thenReturn(0);

		// 검색 구분이 비어있는 상태로 목록 조회를 수행합니다.
		Map<String, Object> result = couponService.getAdminCouponList(
			null,
			null,
			"",
			"테스트",
			"",
			"",
			"",
			"",
			"",
			"",
			""
		);

		// 매퍼로 전달된 파라미터를 검증합니다.
		ArgumentCaptor<CouponPO> captor = ArgumentCaptor.forClass(CouponPO.class);
		verify(couponMapper, times(1)).getAdminCouponList(captor.capture());
		verify(couponMapper, times(1)).getAdminCouponCount(any(CouponPO.class));
		CouponPO captured = captor.getValue();
		assertEquals(1, captured.getPage());
		assertEquals(20, captured.getPageSize());
		assertEquals(0, captured.getOffset());
		assertEquals("CPN_NM", captured.getSearchGb());
		assertEquals("REG_DT", captured.getDateGb());
		assertEquals(1, result.get("page"));
		assertEquals(20, result.get("pageSize"));
	}

	@Test
	@DisplayName("목록 조회: 쿠폰번호 검색은 CPN_NO 구분을 유지한다")
	// 목록 조회 시 쿠폰번호 검색 구분과 검색어를 유지합니다.
	void getAdminCouponList_keepsCouponNoSearch() {
		// 목록/카운트 응답을 목으로 고정합니다.
		when(couponMapper.getAdminCouponList(any(CouponPO.class))).thenReturn(List.of(new CouponVO()));
		when(couponMapper.getAdminCouponCount(any(CouponPO.class))).thenReturn(1);

		// 쿠폰번호 검색 조건으로 목록 조회를 수행합니다.
		couponService.getAdminCouponList(
			1,
			20,
			"CPN_NO",
			"100",
			"REG_DT",
			"",
			"",
			"",
			"",
			"",
			""
		);

		// 검색 구분과 검색어 전달값을 검증합니다.
		ArgumentCaptor<CouponPO> captor = ArgumentCaptor.forClass(CouponPO.class);
		verify(couponMapper).getAdminCouponList(captor.capture());
		CouponPO captured = captor.getValue();
		assertEquals("CPN_NO", captured.getSearchGb());
		assertEquals("100", captured.getSearchValue());
	}

	@Test
	@DisplayName("목록 조회: 등록기간 날짜 검색은 일 단위로 정규화한다")
	// 목록 조회 시 등록기간 날짜를 시작/종료 시각으로 정규화합니다.
	void getAdminCouponList_normalizesRegDateRange() {
		// 목록/카운트 응답을 목으로 고정합니다.
		when(couponMapper.getAdminCouponList(any(CouponPO.class))).thenReturn(Collections.emptyList());
		when(couponMapper.getAdminCouponCount(any(CouponPO.class))).thenReturn(0);

		// 날짜 범위를 전달해 목록 조회를 수행합니다.
		couponService.getAdminCouponList(
			1,
			20,
			"CPN_NM",
			"",
			"REG_DT",
			"2026-03-01",
			"2026-03-02",
			"",
			"",
			"",
			""
		);

		// 정규화된 날짜 포맷을 검증합니다.
		ArgumentCaptor<CouponPO> captor = ArgumentCaptor.forClass(CouponPO.class);
		verify(couponMapper).getAdminCouponList(captor.capture());
		CouponPO captured = captor.getValue();
		assertEquals("2026-03-01 00:00:00", captured.getSearchStartDt());
		assertEquals("2026-03-02 23:59:59", captured.getSearchEndDt());
		assertEquals("REG_DT", captured.getDateGb());
	}

	@Test
	@DisplayName("목록 조회: 다운로드가능기간 datetime-local 검색을 정규화한다")
	// 목록 조회 시 다운로드가능기간 datetime-local 값을 정규화합니다.
	void getAdminCouponList_normalizesDownDateTimeLocalRange() {
		// 목록/카운트 응답을 목으로 고정합니다.
		when(couponMapper.getAdminCouponList(any(CouponPO.class))).thenReturn(Collections.emptyList());
		when(couponMapper.getAdminCouponCount(any(CouponPO.class))).thenReturn(0);

		// datetime-local 범위를 전달해 목록 조회를 수행합니다.
		couponService.getAdminCouponList(
			1,
			20,
			"CPN_NM",
			"",
			"DOWN_DT",
			"2026-03-01T10:20",
			"2026-03-02T11:30",
			"",
			"",
			"",
			""
		);

		// 정규화된 시간 포맷을 검증합니다.
		ArgumentCaptor<CouponPO> captor = ArgumentCaptor.forClass(CouponPO.class);
		verify(couponMapper).getAdminCouponList(captor.capture());
		CouponPO captured = captor.getValue();
		assertEquals("2026-03-01 10:20:00", captured.getSearchStartDt());
		assertEquals("2026-03-02 11:30:59", captured.getSearchEndDt());
		assertEquals("DOWN_DT", captured.getDateGb());
	}

	@Test
	@DisplayName("목록 조회: 시작일시가 종료일시보다 늦으면 예외를 반환한다")
	// 목록 조회 시 시작일시가 종료일시보다 늦으면 예외를 반환합니다.
	void getAdminCouponList_throwsWhenDateRangeReversed() {
		// 날짜 역전 조건으로 목록 조회를 수행합니다.
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> couponService.getAdminCouponList(
			1,
			20,
			"CPN_NM",
			"",
			"REG_DT",
			"2026-03-02",
			"2026-03-01",
			"",
			"",
			"",
			""
		));

		// 예외 메시지를 검증합니다.
		assertEquals("검색 시작일시는 종료일시보다 늦을 수 없습니다.", exception.getMessage());
	}

	@Test
	@DisplayName("목록 조회: 고객 다운로드 가능 여부는 Y/N만 유지한다")
	// 목록 조회 시 고객 다운로드 가능 여부 필터값을 정규화합니다.
	void getAdminCouponList_normalizesDownloadAbleYn() {
		// 목록/카운트 응답을 목으로 고정합니다.
		when(couponMapper.getAdminCouponList(any(CouponPO.class))).thenReturn(Collections.emptyList());
		when(couponMapper.getAdminCouponCount(any(CouponPO.class))).thenReturn(0);

		// 잘못된 Y/N 값을 포함해 목록 조회를 수행합니다.
		couponService.getAdminCouponList(
			1,
			20,
			"CPN_NM",
			"",
			"REG_DT",
			"",
			"",
			"",
			"",
			"",
			"INVALID"
		);

		// 정규화된 필터값을 검증합니다.
		ArgumentCaptor<CouponPO> captor = ArgumentCaptor.forClass(CouponPO.class);
		verify(couponMapper).getAdminCouponList(captor.capture());
		CouponPO captured = captor.getValue();
		assertNull(captured.getCpnDownAbleYn());
	}
}
