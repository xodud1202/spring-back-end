package com.xodud1202.springbackend.controller.work;

import com.xodud1202.springbackend.common.exception.GlobalRestExceptionHandler;
import com.xodud1202.springbackend.common.work.WorkSessionPolicy;
import com.xodud1202.springbackend.mapper.ReadingRecordMapper;
import com.xodud1202.springbackend.service.ReadingRecordService;
import com.xodud1202.springbackend.config.properties.JwtProperties;
import com.xodud1202.springbackend.security.SignedLoginTokenService;
import jakarta.servlet.http.Cookie;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WorkReadingRecordControllerTests {
	private static final String PATH = "/api/work/reading-records";
	private ReadingRecordMapper mapper;
	private MockMvc mvc;

	@BeforeEach
	void init() {
		mapper = mock(ReadingRecordMapper.class);
		var controller = new WorkReadingRecordController(new ReadingRecordService(mapper));
		ReflectionTestUtils.setField(controller, "signedLoginTokenService", tokenService("test-key-"));
		mvc = MockMvcBuilders.standaloneSetup(controller)
			.setControllerAdvice(new GlobalRestExceptionHandler()).build();
	}

	@Test
	void requiresExistingWorkSessionForBothMethods() throws Exception {
		mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
		mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON)
			.content("{\"title\":\"책\",\"author\":\"작가\"}")).andExpect(status().isUnauthorized());
		verifyNoInteractions(mapper);
	}

	@Test
	void authenticatedUserListsAndCreatesOnlyWithOwnSessionId() throws Exception {
		when(mapper.selectByOwner(7L)).thenReturn(List.of());
		mvc.perform(get(PATH).session(session())).andExpect(status().isOk())
			.andExpect(header().string("Cache-Control", "no-store")).andExpect(content().json("[]"));
		String value = "가".repeat(200);
		mvc.perform(post(PATH).session(session()).contentType(MediaType.APPLICATION_JSON)
			.content("{\"title\":\" 책 \",\"author\":\"작가\",\"rating\":4.5,\"content\":\"" + value + "\"}"))
			.andExpect(status().isCreated());
		verify(mapper).selectByOwner(7L);
		verify(mapper).insert(eq(7L), argThat(row -> row.title().equals("책")
			&& row.rating().toPlainString().equals("4.5") && row.content().equals(value)));
	}

	@Test
	void rejectsInvalidRatingDatesLongContentAndMissingTitle() throws Exception {
		for (String json : List.of(
			"{\"title\":\"책\",\"author\":\"작가\",\"rating\":4.3}",
			"{\"title\":\"책\",\"author\":\"작가\",\"rating\":5.5}",
			"{\"title\":\"책\",\"author\":\"작가\",\"startedOn\":\"2026-11-19\",\"completedOn\":\"2026-11-18\"}",
			"{\"title\":\"책\",\"author\":\"작가\",\"content\":\"" + "가".repeat(201) + "\"}",
			"{\"title\":\"  \",\"author\":\"작가\"}"
		)) {
			mvc.perform(post(PATH).session(session()).contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isBadRequest());
		}
		verifyNoInteractions(mapper);
	}

	@Test
	void rejectsCookieSignedWithDifferentKeyAndIgnoresOwnerQueryParameter() throws Exception {
		var wrongKey = tokenService("other-key-").generateWorkAuthToken(8L);
		mvc.perform(get(PATH).cookie(new Cookie(WorkSessionPolicy.COOKIE_WORK_USER_NO, wrongKey)))
			.andExpect(status().isUnauthorized());
		mvc.perform(get(PATH).session(session()).param("owner", "8")).andExpect(status().isOk());
		verify(mapper).selectByOwner(7L);
		verifyNoMoreInteractions(mapper);
	}

	private SignedLoginTokenService tokenService(String testSeed) {
		return new SignedLoginTokenService(new JwtProperties(testSeed.repeat(10), null, null, false));
	}

	private MockHttpSession session() {
		MockHttpSession session = new MockHttpSession();
		WorkSessionPolicy.applyAuthenticatedSession(session, 7L);
		return session;
	}
}