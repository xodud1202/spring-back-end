package com.xodud1202.springbackend.controller.bo;

import com.xodud1202.springbackend.domain.common.FtpProperties;
import com.xodud1202.springbackend.service.BoardService;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
// 관리자 게시판 이미지 업로드 검증 동작을 확인합니다.
class AdminBoardControllerTests {

	@Mock
	private BoardService boardService;

	@Mock
	private FtpFileService ftpFileService;

	@Mock
	private FtpProperties ftpProperties;

	@InjectMocks
	private AdminBoardController adminBoardController;

	private MockMvc mockMvc;

	/**
	 * 컨트롤러 단위 업로드 검증용 MockMvc를 초기화합니다.
	 */
	@BeforeEach
	void setUp() {
		// 단일 컨트롤러 기준 standalone MockMvc를 생성합니다.
		mockMvc = MockMvcBuilders.standaloneSetup(adminBoardController).build();
	}

	@Test
	@DisplayName("게시판 이미지는 대문자 허용 확장자도 업로드할 수 있다")
	// 확장자 비교가 대소문자를 구분하지 않는지 검증합니다.
	void uploadBoardImageAllowsUppercaseExtension() throws Exception {
		// 게시판 업로드 기본 허용 설정을 구성합니다.
		when(ftpProperties.getUploadBoardMaxSize()).thenReturn(30);
		when(ftpProperties.getUploadBoardAllowExtension()).thenReturn("jpg,jpeg,png,gif");
		when(ftpFileService.uploadBoardRegImage(any())).thenReturn("https://image.example.test/board/board-image.JPG");

		// 대문자 확장자 이미지 업로드 요청을 전송합니다.
		MockMultipartFile image = new MockMultipartFile(
			"image",
			"board-image.JPG",
			"image/jpeg",
			"board-image".getBytes()
		);

		mockMvc.perform(multipart("/api/admin/board/image/upload").file(image))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.imageUrl").value("https://image.example.test/board/board-image.JPG"));
	}

	@Test
	@DisplayName("게시판 이미지 확장자가 허용 목록의 부분 문자열이면 400을 반환한다")
	// jpg 허용 설정이 jp 같은 부분 확장자를 허용하지 않는지 검증합니다.
	void uploadBoardImageRejectsPartialExtensionMatch() throws Exception {
		// 게시판 업로드 기본 허용 설정을 구성합니다.
		when(ftpProperties.getUploadBoardMaxSize()).thenReturn(30);
		when(ftpProperties.getUploadBoardAllowExtension()).thenReturn("jpg,jpeg,png,gif");

		// jpg의 부분 문자열인 jp 확장자 파일 업로드 요청을 전송합니다.
		MockMultipartFile image = new MockMultipartFile(
			"image",
			"board-image.jp",
			"image/jpeg",
			"board-image".getBytes()
		);

		mockMvc.perform(multipart("/api/admin/board/image/upload").file(image))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("허용되지 않은 파일 형식입니다. 허용 형식: jpg,jpeg,png,gif"));

		// 검증 실패 시 FTP 업로드는 호출되지 않아야 합니다.
		verifyNoInteractions(ftpFileService);
	}
}
