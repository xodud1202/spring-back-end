package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.admin.brand.BrandVO;
import com.xodud1202.springbackend.mapper.BrandMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// 브랜드 서비스의 쇼핑몰 헤더 브랜드 변환 로직을 검증합니다.
class BrandServiceTests {
	@Mock
	private BrandMapper brandMapper;

	@Mock
	private GoodsService goodsService;

	@InjectMocks
	private BrandService brandService;

	@Test
	@DisplayName("쇼핑몰 헤더 브랜드 조회 시 브랜드 목록을 헤더 응답 형태로 반환한다")
	// 브랜드 목록을 헤더 브랜드 응답 객체로 변환하는지 확인합니다.
	void getShopHeaderBrandList_returnsMappedBrandList() {
		// 브랜드 테스트 데이터를 구성합니다.
		BrandVO brand = new BrandVO();
		brand.setBrandNo(1);
		brand.setBrandNm("xodud1202");
		brand.setBrandLogoPath("https://image.xodud1202.kro.kr/publist/HDD1/Media/nas/image/common/xodud1202_icon.png");

		// 공통 브랜드 조회 결과를 목으로 설정합니다.
		when(goodsService.getBrandList()).thenReturn(List.of(brand));

		// 헤더 브랜드 응답 데이터 매핑 결과를 검증합니다.
		assertThat(brandService.getShopHeaderBrandList()).hasSize(1);
		assertThat(brandService.getShopHeaderBrandList().get(0).getBrandNo()).isEqualTo(1);
		assertThat(brandService.getShopHeaderBrandList().get(0).getBrandNm()).isEqualTo("xodud1202");
		assertThat(brandService.getShopHeaderBrandList().get(0).getBrandLogoPath())
			.isEqualTo("https://image.xodud1202.kro.kr/publist/HDD1/Media/nas/image/common/xodud1202_icon.png");
	}

	@Test
	@DisplayName("쇼핑몰 헤더 브랜드 조회 시 원본 데이터가 없으면 빈 목록을 반환한다")
	// 브랜드 원본 데이터가 없을 때 빈 목록 처리되는지 확인합니다.
	void getShopHeaderBrandList_returnsEmptyListWhenDataMissing() {
		// 공통 브랜드 조회 결과를 null로 설정합니다.
		when(goodsService.getBrandList()).thenReturn(null);

		// 빈 목록 반환 여부를 검증합니다.
		assertThat(brandService.getShopHeaderBrandList()).isEmpty();
	}
}
