package com.xodud1202.springbackend.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

// 운영 프로파일에서만 스케줄링 후처리기가 등록되는지 검증하는 테스트입니다.
class ProdSchedulingConfigTests {
	private static final String SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME =
		"org.springframework.context.annotation.internalScheduledAnnotationProcessor";

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(ProdSchedulingConfig.class);

	@Test
	@DisplayName("prod 프로파일이 아니면 스케줄링 후처리기가 등록되지 않는다")
	// 로컬 기본 실행 환경에서는 스케줄링 후처리기가 생성되지 않는지 확인합니다.
	void schedulingIsDisabledWhenProfileIsNotProd() {
		// 기본 프로파일 컨텍스트를 실행합니다.
		contextRunner.run(context -> {
			// 스케줄링 후처리기가 생성되지 않았는지 검증합니다.
			assertThat(context).doesNotHaveBean(SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME);
		});
	}

	@Test
	@DisplayName("prod 프로파일이면 스케줄링 후처리기가 등록된다")
	// 운영 프로파일 활성화 시 스케줄링 후처리기가 생성되는지 확인합니다.
	void schedulingIsEnabledWhenProfileIsProd() {
		// prod 프로파일 컨텍스트를 실행합니다.
		contextRunner.withPropertyValues("spring.profiles.active=prod").run(context -> {
			// 스케줄링 후처리기가 생성되었는지 검증합니다.
			assertThat(context).hasBean(SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME);
		});
	}
}
