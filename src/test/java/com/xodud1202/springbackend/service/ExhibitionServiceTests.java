package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.common.FtpProperties;
import com.xodud1202.springbackend.domain.shop.exhibition.ShopExhibitionItemVO;
import com.xodud1202.springbackend.domain.shop.exhibition.ShopExhibitionPageVO;
import com.xodud1202.springbackend.mapper.ExhibitionMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// ExhibitionService의 쇼핑몰 기획전 목록 페이징 로직을 검증합니다.
class ExhibitionServiceTests {
	@Mock
	private ExhibitionMapper exhibitionMapper;

	@Mock
	private FtpProperties ftpProperties;

	@Mock
	private FtpFileService ftpFileService;

	@InjectMocks
	private ExhibitionService exhibitionService;

	@Test
	@DisplayName("쇼핑 기획전 목록 조회는 정상 페이지 요청 시 목록/페이지 정보를 반환한다")
	// 정상 요청에서 페이징 응답과 목록 데이터가 구성되는지 검증합니다.
	void getShopExhibitionPage_returnsPagedListWhenValidRequest() {
		// 노출 건수와 목록 응답을 목으로 설정합니다.
		ShopExhibitionItemVO item = new ShopExhibitionItemVO();
		item.setExhibitionNo(2);
		item.setExhibitionNm("2026 S/S 신상품 기획전");
		item.setThumbnailUrl("https://image.xodud1202.kro.kr/exhibition/2.png");
		when(exhibitionMapper.countShopVisibleExhibitionList()).thenReturn(25);
		when(exhibitionMapper.getShopVisibleExhibitionList(20, 20)).thenReturn(List.of(item));

		// 2페이지 요청 결과를 조회합니다.
		ShopExhibitionPageVO result = exhibitionService.getShopExhibitionPage(2);

		// 페이징 정보와 목록 반환값을 검증합니다.
		assertThat(result.getTotalCount()).isEqualTo(25);
		assertThat(result.getPageNo()).isEqualTo(2);
		assertThat(result.getPageSize()).isEqualTo(20);
		assertThat(result.getTotalPageCount()).isEqualTo(2);
		assertThat(result.getExhibitionList()).hasSize(1);
		assertThat(result.getExhibitionList().get(0).getExhibitionNo()).isEqualTo(2);
	}

	@Test
	@DisplayName("쇼핑 기획전 목록 조회는 페이지 범위를 초과하면 마지막 페이지로 보정한다")
	// 요청 페이지가 범위를 벗어나면 마지막 페이지와 오프셋으로 보정되는지 검증합니다.
	void getShopExhibitionPage_clampsPageNoToLastPageWhenOutOfRange() {
		// 총 21건(2페이지) 데이터 기준 목 응답을 설정합니다.
		when(exhibitionMapper.countShopVisibleExhibitionList()).thenReturn(21);
		when(exhibitionMapper.getShopVisibleExhibitionList(anyInt(), anyInt())).thenReturn(List.of());

		// 범위를 초과한 9페이지를 요청합니다.
		ShopExhibitionPageVO result = exhibitionService.getShopExhibitionPage(9);

		// 마지막 페이지 보정과 오프셋 계산 결과를 검증합니다.
		assertThat(result.getPageNo()).isEqualTo(2);
		assertThat(result.getPageSize()).isEqualTo(20);
		assertThat(result.getTotalPageCount()).isEqualTo(2);
		ArgumentCaptor<Integer> offsetCaptor = ArgumentCaptor.forClass(Integer.class);
		ArgumentCaptor<Integer> pageSizeCaptor = ArgumentCaptor.forClass(Integer.class);
		verify(exhibitionMapper).getShopVisibleExhibitionList(offsetCaptor.capture(), pageSizeCaptor.capture());
		assertThat(offsetCaptor.getValue()).isEqualTo(20);
		assertThat(pageSizeCaptor.getValue()).isEqualTo(20);
	}

	@Test
	@DisplayName("쇼핑 기획전 목록 조회는 매퍼 예외 발생 시 예외를 전파한다")
	// 매퍼 예외 발생 시 서비스 레이어에서 예외가 그대로 전달되는지 검증합니다.
	void getShopExhibitionPage_throwsWhenMapperFails() {
		// 건수 조회 단계에서 예외가 발생하도록 목 동작을 설정합니다.
		when(exhibitionMapper.countShopVisibleExhibitionList()).thenThrow(new IllegalStateException("boom"));

		// 예외 전파 여부를 검증합니다.
		assertThrows(IllegalStateException.class, () -> exhibitionService.getShopExhibitionPage(1));
	}
}

