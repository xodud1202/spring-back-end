package com.xodud1202.springbackend;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("외부 MySQL 및 MyBatis 매퍼 의존 통합 컨텍스트 테스트는 로컬/CI 환경에서 제외합니다.")
// 애플리케이션 통합 컨텍스트 테스트의 실행 정책을 정의합니다.
class SpringBackEndApplicationTests {

	@Test
	// 통합 컨텍스트 로딩 테스트의 자리 표시자 메서드입니다.
	void contextLoads() {
	}

}
