package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.news.NewsCollectResultVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
// 실제 DB/RSS 연동으로 뉴스 수집 서비스를 검증합니다.
class NewsRssCollectIntegrationTests {

	// 테스트 대상 뉴스 수집 서비스입니다.
	@Autowired
	private NewsRssCollectService newsRssCollectService;

	@Test
	@DisplayName("실행 검증: 뉴스 RSS 수집 서비스를 호출하면 대상 URL 조회가 수행된다")
	// 실제 수집 로직을 실행해 최소 대상 URL 조회가 되는지 검증합니다.
	void collectNewsArticles_runsAgainstRealDatasource() {
		// 수집 배치를 실제로 실행합니다.
		NewsCollectResultVO result = newsRssCollectService.collectNewsArticles();

		// 활성 대상 URL 존재 여부를 검증합니다.
		assertTrue(result.getTargetCount() > 0, "활성 RSS 대상 URL이 1건 이상이어야 합니다.");
	}
}
