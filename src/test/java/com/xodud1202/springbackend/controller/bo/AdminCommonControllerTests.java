package com.xodud1202.springbackend.controller.bo;

import com.xodud1202.springbackend.domain.common.FtpProperties;
import com.xodud1202.springbackend.service.AdminCommonService;
import com.xodud1202.springbackend.service.FtpFileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminCommonControllerTests {

	@Mock
	private AdminCommonService adminCommonService;

	@Mock
	private FtpFileService ftpFileService;

	@Mock
	private FtpProperties ftpProperties;

	@InjectMocks
	private AdminCommonController adminCommonController;

	private MockMvc mockMvc;

	/**
	 * 컨트롤러 단위 업로드 검증용 MockMvc를 초기화합니다.
	 */
	@BeforeEach
	void setUp() {
		// 단일 컨트롤러 기준 standalone MockMvc를 생성합니다.
		mockMvc = MockMvcBuilders.standaloneSetup(adminCommonController).build();
	}

	/**
	 * 브랜드 로고 업로드 성공 응답을 검증합니다.
	 */
	@Test
	@DisplayName("브랜드 로고 업로드 성공 시 이미지 경로를 반환한다")
	void uploadBrandLogoReturnsUploadedPath() throws Exception {
		// 브랜드 업로드 기본 허용 설정을 구성합니다.
		when(ftpProperties.getUploadBrandMaxSize()).thenReturn(30);
		when(ftpProperties.getUploadBrandAllowExtension()).thenReturn("jpg,jpeg,png,gif");

		// 업로드 성공 응답 URL을 목킹합니다.
		when(ftpFileService.uploadBrandLogo(any(), eq("101")))
				.thenReturn("https://image.xodud1202.kro.kr/publist/HDD1/Media/nas/upload/brand/101_20260309120000000.jpg");

		// 정상 이미지 업로드 요청을 전송합니다.
		MockMultipartFile image = new MockMultipartFile(
				"image",
				"brand-logo.jpg",
				"image/jpeg",
				"brand-logo".getBytes()
		);

		mockMvc.perform(
						multipart("/api/upload/brand-logo")
								.file(image)
								.param("brandNo", "101")
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.brandLogoPath").value("https://image.xodud1202.kro.kr/publist/HDD1/Media/nas/upload/brand/101_20260309120000000.jpg"))
				.andExpect(jsonPath("$.message").value("이미지 업로드가 완료되었습니다."));
	}

	/**
	 * 허용되지 않은 확장자 업로드 차단을 검증합니다.
	 */
	@Test
	@DisplayName("브랜드 로고 확장자가 허용되지 않으면 400을 반환한다")
	void uploadBrandLogoRejectsInvalidExtension() throws Exception {
		// 브랜드 업로드 기본 허용 설정을 구성합니다.
		when(ftpProperties.getUploadBrandMaxSize()).thenReturn(30);
		when(ftpProperties.getUploadBrandAllowExtension()).thenReturn("jpg,jpeg,png,gif");

		// 허용되지 않은 확장자 파일 업로드 요청을 전송합니다.
		MockMultipartFile image = new MockMultipartFile(
				"image",
				"brand-logo.webp",
				"image/webp",
				"brand-logo".getBytes()
		);

		mockMvc.perform(
						multipart("/api/upload/brand-logo")
								.file(image)
								.param("brandNo", "101")
				)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("허용되지 않는 파일 형식입니다. 허용 형식: jpg,jpeg,png,gif"));

		// 검증 실패 시 FTP 업로드는 호출되지 않아야 합니다.
		verifyNoInteractions(ftpFileService);
	}

	/**
	 * 브랜드 업로드 설정 누락 차단을 검증합니다.
	 */
	@Test
	@DisplayName("브랜드 업로드 설정이 없으면 400을 반환한다")
	void uploadBrandLogoRejectsMissingConfig() throws Exception {
		// 브랜드 업로드 허용 용량 설정 누락 상태를 구성합니다.
		when(ftpProperties.getUploadBrandMaxSize()).thenReturn(0);

		// 설정 누락 상태에서 업로드 요청을 전송합니다.
		MockMultipartFile image = new MockMultipartFile(
				"image",
				"brand-logo.jpg",
				"image/jpeg",
				"brand-logo".getBytes()
		);

		mockMvc.perform(
						multipart("/api/upload/brand-logo")
								.file(image)
								.param("brandNo", "101")
				)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("브랜드 로고 업로드 설정이 올바르지 않습니다."));

		// 설정 검증 실패 시 FTP 업로드는 호출되지 않아야 합니다.
		verifyNoInteractions(ftpFileService);
	}
}
