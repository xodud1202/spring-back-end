package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.admin.banner.BannerPO;
import com.xodud1202.springbackend.domain.admin.banner.BannerDeletePO;
import com.xodud1202.springbackend.domain.admin.banner.BannerImageOrderSavePO;
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
import org.springframework.mock.web.MockMultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
		String message = bannerService.validateBannerCreate(param, null, null);

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
		String message = bannerService.validateBannerCreate(param, null, null);

		// 기간 형식 오류가 없는지 확인합니다.
		assertNull(message);
		assertEquals("2026-02-11 10:20:00", param.getDispStartDt());
		assertEquals("2026-02-11 11:20:30", param.getDispEndDt());
		assertNull(param.getBannerNo());
	}

	@Test
	@DisplayName("이미지 업로드 검증: 대배너/띠배너 이외 구분은 오류를 반환한다")
	// 이미지 업로드 검증 시 이미지 배너 구분이 아니면 오류를 반환합니다.
	void validateBannerImageUpload_returnsErrorWhenBannerDivIsNotImageType() {
		// 테스트용 업로드 파일을 구성합니다.
		MockMultipartFile image = new MockMultipartFile(
			"image",
			"sample.png",
			"image/png",
			new byte[] { 1, 2, 3 }
		);

		// 이미지 업로드 검증을 수행합니다.
		String message = bannerService.validateBannerImageUpload(1, "BANNER_DIV_02", 1L, image);

		// 이미지 업로드 가능 배너 구분 검증 메시지를 확인합니다.
		assertEquals("이미지 업로드는 대배너/띠배너에서만 가능합니다.", message);
	}

	@Test
	@DisplayName("이미지 정렬 검증: 정렬 목록이 없으면 오류를 반환한다")
	// 이미지 정렬 저장 검증 시 정렬 목록이 비어있으면 오류를 반환합니다.
	void validateBannerImageOrder_returnsErrorWhenOrdersEmpty() {
		// 정렬 요청 기본 데이터를 구성합니다.
		BannerImageOrderSavePO param = new BannerImageOrderSavePO();
		param.setBannerNo(1);
		param.setUdtNo(1L);

		// 정렬 저장 검증을 수행합니다.
		String message = bannerService.validateBannerImageOrder(param);

		// 정렬 목록 누락 메시지를 확인합니다.
		assertEquals("저장할 정렬 정보가 없습니다.", message);
	}

	@Test
	@DisplayName("삭제 검증: 배너 번호가 없으면 오류를 반환한다")
	// 삭제 검증 시 배너 번호가 없으면 오류를 반환합니다.
	void validateBannerDelete_returnsErrorWhenBannerNoMissing() {
		// 삭제 요청 기본 데이터를 구성합니다.
		BannerDeletePO param = new BannerDeletePO();
		param.setUdtNo(1L);

		// 삭제 검증을 수행합니다.
		String message = bannerService.validateBannerDelete(param);

		// 배너 번호 누락 메시지를 확인합니다.
		assertEquals("배너 번호를 확인해주세요.", message);
	}

	@Test
	@DisplayName("삭제 처리: 배너가 존재하면 상세를 정리 후 삭제 상태로 변경한다")
	// 삭제 처리 시 상세 데이터를 정리하고 기본 정보를 삭제 상태로 변경합니다.
	void deleteBanner_deletesDetailAndMarksBaseDeleted() {
		// 삭제 요청 기본 데이터를 구성합니다.
		BannerDeletePO param = new BannerDeletePO();
		param.setBannerNo(100);
		param.setUdtNo(1L);

		// 배너 존재 여부/삭제 결과를 목으로 고정합니다.
		when(bannerMapper.countBannerByNo(100)).thenReturn(1);
		when(bannerMapper.updateBannerBaseDelete(100, 1L)).thenReturn(1);

		// 삭제를 수행합니다.
		int deleted = bannerService.deleteBanner(param);

		// 삭제 처리 결과와 매퍼 호출을 검증합니다.
		assertEquals(1, deleted);
		verify(bannerMapper, times(1)).deleteImageBannerInfoByBannerNo(100);
		verify(bannerMapper, times(1)).deleteBannerGoodsByBannerNo(100);
		verify(bannerMapper, times(1)).deleteBannerTabByBannerNo(100);
		verify(bannerMapper, times(1)).updateBannerBaseDelete(100, 1L);
	}

	@Test
	@DisplayName("삭제 처리: 배너가 없으면 예외를 반환한다")
	// 삭제 처리 시 배너가 존재하지 않으면 예외를 반환합니다.
	void deleteBanner_throwsWhenBannerNotFound() {
		// 삭제 요청 기본 데이터를 구성합니다.
		BannerDeletePO param = new BannerDeletePO();
		param.setBannerNo(99999);
		param.setUdtNo(1L);

		// 배너 미존재 응답을 목으로 고정합니다.
		when(bannerMapper.countBannerByNo(99999)).thenReturn(0);

		// 삭제 예외를 검증합니다.
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> bannerService.deleteBanner(param));
		assertEquals("배너 정보를 확인해주세요.", exception.getMessage());
	}
}
