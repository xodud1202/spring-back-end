package com.xodud1202.springbackend.controller.bo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkReplyFileDownloadVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkReplyFileVO;
import com.xodud1202.springbackend.domain.admin.companywork.AdminCompanyWorkReplyVO;
import com.xodud1202.springbackend.service.CompanyWorkService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
// 관리자 회사 업무 컨트롤러의 댓글 첨부 관련 응답을 검증합니다.
class AdminCompanyWorkControllerTests {

	@Mock
	private CompanyWorkService companyWorkService;

	@Spy
	private ObjectMapper objectMapper = new ObjectMapper();

	@InjectMocks
	private AdminCompanyWorkController adminCompanyWorkController;

	private MockMvc mockMvc;

	/**
	 * 회사 업무 컨트롤러 단위 검증용 MockMvc를 초기화합니다.
	 */
	@BeforeEach
	void setUp() {
		// 단일 컨트롤러 standalone MockMvc를 구성합니다.
		mockMvc = MockMvcBuilders.standaloneSetup(adminCompanyWorkController).build();
	}

	/**
	 * 테스트 간 SecurityContext를 초기화합니다.
	 */
	@AfterEach
	void tearDown() {
		// 이전 테스트 인증 정보가 다음 테스트에 누수되지 않도록 정리합니다.
		SecurityContextHolder.clearContext();
	}

	/**
	 * 인증된 관리자 사용자 컨텍스트를 구성합니다.
	 */
	private void authenticateAdmin() {
		// 컨트롤러 인증 분기 검증용 테스트 인증 정보를 설정합니다.
		TestingAuthenticationToken authentication = new TestingAuthenticationToken("admin", null, "ROLE_ADMIN");
		authentication.setAuthenticated(true);
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	/**
	 * 댓글 멀티파트 저장 성공 응답을 검증합니다.
	 */
	@Test
	@DisplayName("회사 업무 댓글 멀티파트 저장 시 첨부파일 목록을 포함해 반환한다")
	void saveAdminCompanyWorkReplyWithFilesReturnsSavedReply() throws Exception {
		authenticateAdmin();

		// 저장 완료 댓글 응답을 구성합니다.
		AdminCompanyWorkReplyFileVO replyFile = new AdminCompanyWorkReplyFileVO();
		replyFile.setReplyFileSeq(11);
		replyFile.setReplySeq(7L);
		replyFile.setWorkSeq(3L);
		replyFile.setReplyFileNm("회의록.pdf");
		replyFile.setReplyFileUrl("https://image.xodud1202.kro.kr/publist/HDD1/Media/nas/upload/company-work-reply/3/7/reply_101_7_1.pdf");

		AdminCompanyWorkReplyVO reply = new AdminCompanyWorkReplyVO();
		reply.setReplySeq(7L);
		reply.setWorkSeq(3L);
		reply.setReplyComment("<p>확인 부탁드립니다.</p>");
		reply.setRegNo(101L);
		reply.setReplyFileList(List.of(replyFile));

		when(companyWorkService.saveAdminCompanyWorkReply(any(), anyList())).thenReturn(reply);

		// payload와 파일을 포함한 멀티파트 요청을 전송합니다.
		MockMultipartFile payload = new MockMultipartFile(
			"payload",
			"",
			"application/json",
			"""
				{"workSeq":3,"replyComment":"<p>확인 부탁드립니다.</p>","regNo":101,"udtNo":101}
			""".getBytes(StandardCharsets.UTF_8)
		);
		MockMultipartFile file = new MockMultipartFile(
			"files",
			"meeting.pdf",
			"application/pdf",
			"pdf-data".getBytes(StandardCharsets.UTF_8)
		);

		mockMvc.perform(
				multipart("/api/admin/company/work/reply")
					.file(payload)
					.file(file)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.replySeq").value(7))
			.andExpect(jsonPath("$.replyFileList[0].replyFileSeq").value(11))
			.andExpect(jsonPath("$.replyFileList[0].replyFileNm").value("회의록.pdf"));
	}

	/**
	 * 인증되지 않은 댓글 첨부파일 다운로드 차단을 검증합니다.
	 */
	@Test
	@DisplayName("인증되지 않은 회사 업무 댓글 첨부파일 다운로드는 401을 반환한다")
	void downloadAdminCompanyWorkReplyFileRejectsUnauthenticatedRequest() throws Exception {
		// 인증 없이 다운로드 요청을 전송합니다.
		mockMvc.perform(get("/api/admin/company/work/reply/file/download").param("replyFileSeq", "1"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("로그인이 필요합니다."));

		// 인증 차단 시 서비스는 호출되지 않아야 합니다.
		verifyNoInteractions(companyWorkService);
	}

	/**
	 * 인증된 댓글 첨부파일 다운로드 성공 응답을 검증합니다.
	 */
	@Test
	@DisplayName("인증된 회사 업무 댓글 첨부파일 다운로드는 attachment 응답을 반환한다")
	void downloadAdminCompanyWorkReplyFileReturnsAttachmentResponse() throws Exception {
		// 인증된 사용자 컨텍스트를 구성합니다.
		authenticateAdmin();

		// 다운로드 응답 파일 메타와 바이트를 구성합니다.
		AdminCompanyWorkReplyFileDownloadVO response = new AdminCompanyWorkReplyFileDownloadVO();
		response.setReplyFileNm("회의록 1차.pdf");
		response.setFileData("download-data".getBytes(StandardCharsets.UTF_8));
		when(companyWorkService.downloadAdminCompanyWorkReplyFile(anyInt())).thenReturn(response);

		mockMvc.perform(get("/api/admin/company/work/reply/file/download").param("replyFileSeq", "12"))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("filename*=")))
			.andExpect(content().bytes("download-data".getBytes(StandardCharsets.UTF_8)));
	}
}
