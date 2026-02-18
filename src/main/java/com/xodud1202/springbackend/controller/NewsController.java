package com.xodud1202.springbackend.controller;

import com.xodud1202.springbackend.domain.news.NewsCategorySummaryVO;
import com.xodud1202.springbackend.domain.news.NewsPressSummaryVO;
import com.xodud1202.springbackend.domain.news.NewsTopArticleVO;
import com.xodud1202.springbackend.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*", "chrome-extension://*"})
// 뉴스 확장프로그램 조회 API를 제공합니다.
public class NewsController {
	private final NewsService newsService;

	// 활성 언론사 목록을 조회합니다.
	@GetMapping("/api/news/press")
	public ResponseEntity<List<NewsPressSummaryVO>> getPressList() {
		return ResponseEntity.ok(newsService.getActivePressList());
	}

	// 선택한 언론사의 활성 카테고리 목록을 조회합니다.
	@GetMapping("/api/news/categories")
	public ResponseEntity<List<NewsCategorySummaryVO>> getCategoryList(@RequestParam("pressId") String pressId) {
		return ResponseEntity.ok(newsService.getActiveCategoryList(pressId));
	}

	// 선택한 언론사/카테고리의 상위 기사 목록을 조회합니다.
	@GetMapping("/api/news/articles/top")
	public ResponseEntity<List<NewsTopArticleVO>> getTopArticleList(
		@RequestParam("pressId") String pressId,
		@RequestParam("categoryId") String categoryId,
		@RequestParam(value = "limit", required = false) Integer limit
	) {
		return ResponseEntity.ok(newsService.getTopArticleList(pressId, categoryId, limit));
	}
}
