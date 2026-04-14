package com.xodud1202.springbackend.config;

import com.xodud1202.springbackend.controller.work.WorkSessionExtendInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// 업무관리 전용 MVC 인터셉터를 등록합니다.
@Configuration
@RequiredArgsConstructor
public class WorkWebMvcConfig implements WebMvcConfigurer {
	private final WorkSessionExtendInterceptor workSessionExtendInterceptor;

	@Override
	// 업무관리 API 요청에 세션 연장 인터셉터를 연결합니다.
	public void addInterceptors(InterceptorRegistry registry) {
		// 인증 API를 제외한 업무관리 API 전체에 슬라이딩 세션 연장을 적용합니다.
		registry.addInterceptor(workSessionExtendInterceptor)
			.addPathPatterns("/api/work/**")
			.excludePathPatterns("/api/work/auth/**");
	}
}
