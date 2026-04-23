package com.xodud1202.springbackend.config;

import com.github.vertical_blank.sqlformatter.SqlFormatter;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

@Component
@Intercepts({
		@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
		@Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
public class SqlLoggingInterceptor implements Interceptor {
	
	private static final Logger logger = LoggerFactory.getLogger(SqlLoggingInterceptor.class);
	private static final String MASKED_PARAMETER_VALUE = "'[MASKED]'";
	private final Environment environment;

	// SQL 상세 로그 허용 프로필 판단에 필요한 환경 정보를 주입받습니다.
	public SqlLoggingInterceptor(Environment environment) {
		this.environment = environment;
	}
	
	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		if (logger.isDebugEnabled()) {
			MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
			Object parameter = null;
			if (invocation.getArgs().length > 1) {
				parameter = invocation.getArgs()[1];
			}

			BoundSql boundSql = mappedStatement.getBoundSql(parameter);
			Configuration configuration = mappedStatement.getConfiguration();

			// local/dev에서만 파라미터 원문을 허용하고 그 외 프로필은 DEBUG가 켜져도 마스킹합니다.
			logger.debug(getSql(configuration, boundSql, isDetailedSqlLoggingProfile()));
		}
		
		return invocation.proceed();
	}
	
	// 현재 프로필이 SQL 상세 파라미터 로그 허용 대상인지 확인합니다.
	boolean isDetailedSqlLoggingProfile() {
		return environment != null && environment.acceptsProfiles(Profiles.of("local", "dev"));
	}

	// SQL 문자열을 포맷팅하고 프로필 정책에 맞춰 파라미터를 치환합니다.
	String getSql(Configuration configuration, BoundSql boundSql, boolean exposeParameterValues) {
		String sql = boundSql.getSql();
		Object parameterObject = boundSql.getParameterObject();
		List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
		
		// SQL 포맷팅
		String formattedSql = SqlFormatter.format(sql);
		
		// 파라미터 치환
		if (!parameterMappings.isEmpty() && parameterObject != null) {
			TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
			if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
				formattedSql = replaceNextParameter(formattedSql, getParameterValue(parameterObject, exposeParameterValues));
			} else {
				MetaObject metaObject = configuration.newMetaObject(parameterObject);
				for (ParameterMapping parameterMapping : parameterMappings) {
					String propertyName = parameterMapping.getProperty();
					if (metaObject.hasGetter(propertyName)) {
						Object obj = metaObject.getValue(propertyName);
						formattedSql = replaceNextParameter(formattedSql, getParameterValue(obj, exposeParameterValues));
					} else if (boundSql.hasAdditionalParameter(propertyName)) {
						Object obj = boundSql.getAdditionalParameter(propertyName);
						formattedSql = replaceNextParameter(formattedSql, getParameterValue(obj, exposeParameterValues));
					}
				}
			}
		}
		
		return formattedSql;
	}
	
	// SQL의 다음 바인딩 자리표시자를 안전하게 치환합니다.
	private String replaceNextParameter(String formattedSql, String parameterValue) {
		return formattedSql.replaceFirst("\\?", Matcher.quoteReplacement(parameterValue));
	}

	// 파라미터 값을 로그 정책에 따라 원문 또는 마스킹 문자열로 변환합니다.
	private String getParameterValue(Object obj, boolean exposeParameterValues) {
		if (!exposeParameterValues) {
			return MASKED_PARAMETER_VALUE;
		}

		String value;
		if (obj instanceof String) {
			value = "'" + String.valueOf(obj).replace("'", "''") + "'";
		} else if (obj instanceof Date) {
			DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.KOREA);
			value = "'" + formatter.format((Date) obj) + "'";
		} else {
			if (obj != null) {
				value = obj.toString();
			} else {
				value = "NULL";
			}
		}
		return value;
	}
}
