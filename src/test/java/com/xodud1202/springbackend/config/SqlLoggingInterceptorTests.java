package com.xodud1202.springbackend.config;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// MyBatis SQL 로그 인터셉터의 프로필별 파라미터 노출 정책을 확인합니다.
class SqlLoggingInterceptorTests {

	@Test
	@DisplayName("운영 프로필 SQL 로그는 파라미터 원문을 마스킹한다")
	// prod 프로필에서는 DEBUG 로그가 켜져도 민감 파라미터 원문이 남지 않아야 합니다.
	void getSqlMasksParameterValuesOutsideDetailedProfiles() {
		// 운영 프로필 인터셉터와 로그인성 파라미터 SQL을 구성합니다.
		SqlLoggingInterceptor interceptor = new SqlLoggingInterceptor(createEnvironment("prod"));
		Configuration configuration = new Configuration();
		BoundSql boundSql = createLoginBoundSql(configuration);

		// SQL 문자열을 생성하면 파라미터 값 대신 마스킹 값이 치환되어야 합니다.
		String sql = interceptor.getSql(configuration, boundSql, interceptor.isDetailedSqlLoggingProfile());

		assertThat(sql).contains("[MASKED]");
		assertThat(sql).doesNotContain("admin@example.com");
		assertThat(sql).doesNotContain("secret-password");
	}

	@Test
	@DisplayName("local 프로필 SQL 로그는 디버깅용 파라미터 원문을 표시한다")
	// local 프로필에서는 개발 디버깅을 위해 상세 파라미터 로그를 허용합니다.
	void getSqlExposesParameterValuesInLocalProfile() {
		// 로컬 프로필 인터셉터와 로그인성 파라미터 SQL을 구성합니다.
		SqlLoggingInterceptor interceptor = new SqlLoggingInterceptor(createEnvironment("local"));
		Configuration configuration = new Configuration();
		BoundSql boundSql = createLoginBoundSql(configuration);

		// SQL 문자열을 생성하면 local 프로필에서는 원문 파라미터가 확인되어야 합니다.
		String sql = interceptor.getSql(configuration, boundSql, interceptor.isDetailedSqlLoggingProfile());

		assertThat(sql).contains("admin@example.com");
		assertThat(sql).contains("secret-password");
	}

	// 로그인 파라미터가 포함된 BoundSql 테스트 데이터를 생성합니다.
	private BoundSql createLoginBoundSql(Configuration configuration) {
		// MyBatis ParameterMapping을 실제 쿼리 바인딩 순서와 동일하게 구성합니다.
		List<ParameterMapping> parameterMappings = List.of(
			new ParameterMapping.Builder(configuration, "loginId", String.class).build(),
			new ParameterMapping.Builder(configuration, "password", String.class).build()
		);
		return new BoundSql(
			configuration,
			"SELECT * FROM USER_BASE WHERE LOGIN_ID = ? AND PWD = ?",
			parameterMappings,
			new LoginParam("admin@example.com", "secret-password")
		);
	}

	// 활성 프로필이 설정된 테스트 환경을 생성합니다.
	private MockEnvironment createEnvironment(String activeProfile) {
		// acceptsProfiles 검증이 실제 프로필 기준으로 동작하도록 활성 프로필을 지정합니다.
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles(activeProfile);
		return environment;
	}

	// SQL 로그 테스트에 사용할 단순 로그인 파라미터 객체입니다.
	private static final class LoginParam {
		private final String loginId;
		private final String password;

		// 로그인 아이디와 비밀번호 파라미터를 저장합니다.
		private LoginParam(String loginId, String password) {
			this.loginId = loginId;
			this.password = password;
		}

		// 로그인 아이디를 반환합니다.
		public String getLoginId() {
			return loginId;
		}

		// 비밀번호를 반환합니다.
		public String getPassword() {
			return password;
		}
	}
}
