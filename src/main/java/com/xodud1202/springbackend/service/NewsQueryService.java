package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.news.NewsCategorySummaryVO;
import com.xodud1202.springbackend.domain.news.NewsPressSummaryVO;
import com.xodud1202.springbackend.domain.news.NewsTopArticleVO;
import com.xodud1202.springbackend.mapper.NewsArticleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
// 뉴스 팝업 화면용 조회 데이터를 제공합니다.
public class NewsQueryService {
	// 기사 조회 건수 상한값을 정의합니다.
	private static final int MAX_TOP_ARTICLE_LIMIT = 20;
	// 기사 조회 기본 건수를 정의합니다.
	private static final int DEFAULT_TOP_ARTICLE_LIMIT = 5;

	private final NewsArticleMapper newsArticleMapper;

	// 활성 언론사 목록을 조회합니다.
	public List<NewsPressSummaryVO> getActivePressList() {
		return newsArticleMapper.getActivePressList();
	}

	// 선택한 언론사의 활성 카테고리 목록을 조회합니다.
	public List<NewsCategorySummaryVO> getActiveCategoryList(String pressId) {
		// 언론사 번호 입력값을 정수형으로 변환합니다.
		Long pressNo = parsePressNo(pressId);
		// 언론사 번호가 유효하지 않으면 빈 목록을 반환합니다.
		if (pressNo == null) {
			return List.of();
		}
		return newsArticleMapper.getActiveCategoryListByPressNo(pressNo);
	}

	// 선택한 언론사/카테고리의 상위 기사 목록을 조회합니다.
	public List<NewsTopArticleVO> getTopArticleList(String pressId, String categoryId, Integer limit) {
		// 입력값 공백을 제거해 null 가능 값으로 정리합니다.
		Long pressNo = parsePressNo(pressId);
		String categoryCd = trimToNull(categoryId);
		// 조회 조건이 부족하면 빈 목록을 반환합니다.
		if (pressNo == null || categoryCd == null) {
			return List.of();
		}
		// 요청 건수를 보정해 과도한 조회를 제한합니다.
		int resolvedLimit = resolveTopArticleLimit(limit);
		return newsArticleMapper.getTopArticleListByPressNoAndCategoryCd(pressNo, categoryCd, resolvedLimit);
	}

	// 언론사 번호 문자열을 Long 값으로 변환합니다.
	private Long parsePressNo(String pressId) {
		// 입력 문자열 공백을 제거합니다.
		String resolvedPressId = trimToNull(pressId);
		if (resolvedPressId == null) {
			return null;
		}
		try {
			return Long.parseLong(resolvedPressId);
		} catch (NumberFormatException exception) {
			return null;
		}
	}

	// 기사 조회 건수를 기본값/상한값 기준으로 보정합니다.
	private int resolveTopArticleLimit(Integer limit) {
		// 요청 건수가 없거나 0 이하이면 기본값으로 대체합니다.
		if (limit == null || limit <= 0) {
			return DEFAULT_TOP_ARTICLE_LIMIT;
		}
		// 상한을 초과하면 최대 허용 건수로 제한합니다.
		return Math.min(limit, MAX_TOP_ARTICLE_LIMIT);
	}

	// 문자열 공백을 제거하고 빈값은 null로 변환합니다.
	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
