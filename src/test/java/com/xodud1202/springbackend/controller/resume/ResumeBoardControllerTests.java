package com.xodud1202.springbackend.controller.resume;

import com.xodud1202.springbackend.mapper.BoardMapper;
import com.xodud1202.springbackend.service.BoardService;
import org.apache.ibatis.session.SqlSessionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 실제 게시판 매퍼 SQL부터 공개 처리방침과 서비스 약관 응답까지 검증합니다. */
class ResumeBoardControllerTests {
	private JdbcTemplate jdbc;
	private MockMvc mvc;

	/** 운영 DB를 변경하지 않는 독립 DB와 실제 서비스·매퍼를 구성합니다. */
	@BeforeEach
	void setUp() throws Exception {
		// 테스트마다 격리된 테이블을 생성하고 운영 매퍼 XML을 그대로 로드합니다.
		JdbcDataSource dataSource = new JdbcDataSource();
		dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
		jdbc = new JdbcTemplate(dataSource);
		jdbc.execute("CREATE TABLE BOARD_INFO (BOARD_NO INT PRIMARY KEY, BOARD_DIV_CD VARCHAR(20), "
			+ "TITLE VARCHAR(100), CONTENT CLOB, DEL_YN CHAR(1), VIEW_YN CHAR(1))");
		SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
		factoryBean.setDataSource(dataSource);
		org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
		configuration.setMapUnderscoreToCamelCase(true);
		factoryBean.setConfiguration(configuration);
		factoryBean.setMapperLocations(new ClassPathResource("mapper/BoardMapper.xml"));
		SqlSessionFactory factory = factoryBean.getObject();
		BoardMapper mapper = new SqlSessionTemplate(factory).getMapper(BoardMapper.class);
		mvc = MockMvcBuilders.standaloneSetup(new ResumeBoardController(new BoardService(mapper))).build();
	}

	/** 더 큰 번호의 비공개·삭제·다른 구분 게시글을 제외한 최신 공개 글만 반환합니다. */
	@ParameterizedTest
	@CsvSource({"privacy, KKUGIT_PRIVACY, KKUGIT_TERMS", "terms, KKUGIT_TERMS, KKUGIT_PRIVACY"})
	void returnsLatestVisibleKkugitNotice(String noticeType, String boardDivCd, String otherBoardDivCd) throws Exception {
		// 서브쿼리 없이 정렬해도 공개 조건을 먼저 적용하는지 확인합니다.
		jdbc.update("INSERT INTO BOARD_INFO VALUES "
			+ "(1, ?, '이전 문서', '이전 내용', 'N', 'Y'), "
			+ "(2, ?, '현재 문서', '첫째 줄' || CHAR(10) || '둘째 줄', 'N', 'Y'), "
			+ "(3, ?, '비공개', '숨김', 'N', 'N'), "
			+ "(4, ?, '삭제', '삭제 내용', 'Y', 'Y'), "
			+ "(5, ?, '다른 문서', '다른 내용', 'N', 'Y')",
			boardDivCd, boardDivCd, boardDivCd, boardDivCd, otherBoardDivCd);
		mvc.perform(get("/api/resume/notice/kkugit/" + noticeType))
			.andExpect(status().isOk())
			.andExpect(header().string("Cache-Control", "no-store"))
			.andExpect(jsonPath("$.boardNo").value(2))
			.andExpect(jsonPath("$.title").value("현재 문서"))
			.andExpect(jsonPath("$.content").value("첫째 줄\n둘째 줄"));
	}

	/** 공개 문서가 없는 경우와 공개 글이 없는 경우를 모두 빈 결과로 처리합니다. */
	@ParameterizedTest
	@CsvSource({"privacy, KKUGIT_PRIVACY", "terms, KKUGIT_TERMS"})
	void returnsNotFoundWithoutVisibleNotice(String noticeType, String boardDivCd) throws Exception {
		// 빈 테이블에서도 예외 대신 캐시하지 않는 404 응답을 반환합니다.
		mvc.perform(get("/api/resume/notice/kkugit/" + noticeType))
			.andExpect(status().isNotFound())
			.andExpect(header().string("Cache-Control", "no-store"));

		// 게시글이 있어도 삭제되었거나 비공개이면 노출하지 않습니다.
		jdbc.update("INSERT INTO BOARD_INFO VALUES "
			+ "(1, ?, '비공개', '숨김', 'N', 'N'), "
			+ "(2, ?, '삭제', '삭제 내용', 'Y', 'Y')", boardDivCd, boardDivCd);
		mvc.perform(get("/api/resume/notice/kkugit/" + noticeType))
			.andExpect(status().isNotFound());
	}
}
