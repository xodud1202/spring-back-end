package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.admin.banner.BannerPO;
import com.xodud1202.springbackend.domain.admin.banner.BannerSavePO;
import com.xodud1202.springbackend.domain.admin.banner.BannerVO;
import com.xodud1202.springbackend.mapper.BannerMapper;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// BannerService의 배너 기간 관련 핵심 로직을 검증합니다.
class BannerServiceTests {

	// 배너 매퍼 목 객체입니다.
	@Mock
	private BannerMapper bannerMapper;

	// FTP 파일 서비스 목 객체입니다.
	@Mock
	private FtpFileService ftpFileService;

	// 테스트 대상 서비스입니다.
	@InjectMocks
	private BannerService bannerService;

	@Test
	@DisplayName("목록 조회: 날짜 검색 조건을 DB 비교 형식으로 정규화한다")
	// 목록 조회 시 날짜 검색 조건을 DB 비교 형식으로 정규화합니다.
	void getAdminBannerList_normalizesDateRangeFilters() {
		// 목록/카운트 응답을 목으로 고정합니다.
		when(bannerMapper.getAdminBannerList(org.mockito.ArgumentMatchers.any(BannerPO.class))).thenReturn(Collections.emptyList());
		when(bannerMapper.getAdminBannerCount(org.mockito.ArgumentMatchers.any(BannerPO.class))).thenReturn(0);

		// 날짜 범위를 전달해 목록 조회를 수행합니다.
		Map<String, Object> result = bannerService.getAdminBannerList(
			"BANNER_DIV_01",
			"Y",
			"배너",
			"2026-02-01",
			"2026-02-15",
			1,
			20
		);

		// 매퍼로 전달된 검색 조건을 캡처해 검증합니다.
		ArgumentCaptor<BannerPO> captor = ArgumentCaptor.forClass(BannerPO.class);
		verify(bannerMapper, times(1)).getAdminBannerList(captor.capture());
		verify(bannerMapper, times(1)).getAdminBannerCount(org.mockito.ArgumentMatchers.any(BannerPO.class));
		BannerPO captured = captor.getValue();
		assertEquals("2026-02-01 00:00:00", captured.getSearchStartDt());
		assertEquals("2026-02-15 23:59:59", captured.getSearchEndDt());

		// 기본 응답 페이징 정보도 함께 확인합니다.
		assertEquals(1, result.get("page"));
		assertEquals(20, result.get("pageSize"));
	}

	@Test
	@DisplayName("목록 조회: datetime-local 값을 초 단위 문자열로 정규화한다")
	// 목록 조회 시 datetime-local 값을 초 단위 문자열로 변환합니다.
	void getAdminBannerList_normalizesDateTimeLocalFilters() {
		// 목록/카운트 응답을 목으로 고정합니다.
		when(bannerMapper.getAdminBannerList(org.mockito.ArgumentMatchers.any(BannerPO.class))).thenReturn(List.of(new BannerVO()));
		when(bannerMapper.getAdminBannerCount(org.mockito.ArgumentMatchers.any(BannerPO.class))).thenReturn(1);

		// datetime-local 포맷 값을 전달해 목록 조회를 수행합니다.
		bannerService.getAdminBannerList(
			"",
			"Y",
			"",
			"2026-02-11T10:20",
			"2026-02-11T10:20",
			1,
			20
		);

		// 매퍼로 전달된 검색 조건을 캡처해 검증합니다.
		ArgumentCaptor<BannerPO> listCaptor = ArgumentCaptor.forClass(BannerPO.class);
		verify(bannerMapper).getAdminBannerList(listCaptor.capture());
		BannerPO captured = listCaptor.getValue();
		assertEquals("2026-02-11 10:20:00", captured.getSearchStartDt());
		assertEquals("2026-02-11 10:20:59", captured.getSearchEndDt());
	}

	@Test
	@DisplayName("등록 검증: 노출 시작일이 종료일보다 늦으면 오류를 반환한다")
	// 등록 검증 시 노출 시작일이 종료일보다 늦으면 오류를 반환합니다.
	void validateBannerCreate_returnsErrorWhenDisplayPeriodIsReversed() {
		// 등록 요청 기본 데이터를 구성합니다.
		BannerSavePO param = new BannerSavePO();
		param.setBannerDivCd("BANNER_DIV_01");
		param.setShowYn("Y");
		param.setRegNo(1L);
		param.setDispStartDt("2026-02-12 10:00:00");
		param.setDispEndDt("2026-02-11 10:00:00");

		// 검증을 수행합니다.
		String message = bannerService.validateBannerCreate(param, null);

		// 기간 역전 오류 메시지를 검증합니다.
		assertEquals("노출 시작일시는 종료일시보다 늦을 수 없습니다.", message);
	}

	@Test
	@DisplayName("등록 검증: 노출기간 형식이 올바르면 저장 포맷으로 정규화한다")
	// 등록 검증 시 노출기간 형식이 올바르면 포맷을 정규화합니다.
	void validateBannerCreate_normalizesDisplayPeriodFormat() {
		// 등록 요청 기본 데이터를 구성합니다.
		BannerSavePO param = new BannerSavePO();
		param.setBannerDivCd("BANNER_DIV_04");
		param.setShowYn("Y");
		param.setRegNo(1L);
		param.setDispStartDt("2026-02-11T10:20");
		param.setDispEndDt("2026-02-11 11:20:30");
		param.setGoodsList(List.of());

		// 분기 검증 전에 공통 검증 결과를 확인합니다.
		String message = bannerService.validateBannerCreate(param, null);

		// 기간 형식 오류가 없는지 확인합니다.
		assertEquals("상품리스트배너는 상품을 1개 이상 등록해주세요.", message);
		assertEquals("2026-02-11 10:20:00", param.getDispStartDt());
		assertEquals("2026-02-11 11:20:30", param.getDispEndDt());
		assertNull(param.getBannerNo());
	}
}
