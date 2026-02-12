package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.news.NewsArticleCreatePO;
import com.xodud1202.springbackend.domain.news.NewsCollectResultVO;
import com.xodud1202.springbackend.domain.news.NewsRssTargetVO;
import com.xodud1202.springbackend.domain.news.RssArticleItem;
import com.xodud1202.springbackend.mapper.NewsArticleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
// 뉴스 RSS를 수집해 기사 테이블에 적재하는 서비스를 제공합니다.
public class NewsRssCollectService {
	private static final int MAX_ARTICLE_COUNT_PER_URL = 5;
	private static final String EMPTY_REPLACEMENT = "-";

	private final NewsArticleMapper newsArticleMapper;
	private final RssFeedClient rssFeedClient;

	// 활성 RSS URL을 순회하며 기사 상위 5건을 수집합니다.
	public NewsCollectResultVO collectNewsArticles() {
		// 수집 대상 URL 목록을 조회합니다.
		List<NewsRssTargetVO> targetList = newsArticleMapper.getActiveNewsRssTargetList();
		int successTargetCount = 0;
		int failedTargetCount = 0;
		int attemptedArticleCount = 0;
		int insertedArticleCount = 0;
		int skippedArticleCount = 0;

		// 대상 URL별로 기사 수집/저장을 수행합니다.
		for (NewsRssTargetVO target : targetList) {
			try {
				List<RssArticleItem> feedItems = rssFeedClient.fetchFeed(target.getRssUrl());
				int topCount = Math.min(feedItems.size(), MAX_ARTICLE_COUNT_PER_URL);
				for (int rankIndex = 0; rankIndex < topCount; rankIndex += 1) {
					RssArticleItem feedItem = feedItems.get(rankIndex);
					attemptedArticleCount += 1;

					// 저장 가능한 기사 데이터인지 검증 후 저장 요청을 수행합니다.
					NewsArticleCreatePO saveParam = buildCreateParam(target, feedItem, rankIndex);
					insertedArticleCount += newsArticleMapper.insertNewsArticle(saveParam);
				}
				successTargetCount += 1;
			} catch (Exception exception) {
				// URL 단위 오류는 로그만 남기고 다음 대상으로 계속 진행합니다.
				failedTargetCount += 1;
				log.warn(
					"뉴스 RSS 수집 실패 pressNo={}, categoryCd={}, rssUrl={}, message={}",
					target.getPressNo(),
					target.getCategoryCd(),
					target.getRssUrl(),
					exception.getMessage()
				);
			}
		}

		// 수집 결과 집계를 반환합니다.
		return NewsCollectResultVO.builder()
			.targetCount(targetList.size())
			.successTargetCount(successTargetCount)
			.failedTargetCount(failedTargetCount)
			.attemptedArticleCount(attemptedArticleCount)
			.insertedArticleCount(insertedArticleCount)
			.skippedArticleCount(skippedArticleCount)
			.build();
	}

	// 기사 원본 데이터를 DB 저장 모델로 변환합니다.
	private NewsArticleCreatePO buildCreateParam(NewsRssTargetVO target, RssArticleItem item, int rankIndex) {
		// GUID/URL/제목/요약/저자/발행일을 DB 컬럼 스펙에 맞춰 정규화합니다.
		String articleGuidOriginal = trimToNull(limitLength(item.guid(), 150));
		String articleUrlOriginal = trimToNull(limitLength(item.link(), 150));
		String articleTitleOriginal = trimToNull(limitLength(item.title(), 500));
		String articleSummaryOriginal = trimToNull(item.summary());
		String thumbnailUrlOriginal = trimToNull(limitLength(item.thumbnailUrl(), 150));
		String authorNmOriginal = trimToNull(limitLength(item.authorNm(), 100));
		LocalDateTime publishedDt = item.publishedDt();
		boolean hasRequiredMissing = articleUrlOriginal == null || articleTitleOriginal == null;

		// NOT NULL 대상(URL, 제목)만 누락 시 "-"로 치환합니다.
		String articleUrl = valueOrDash(articleUrlOriginal);
		String articleTitle = valueOrDash(articleTitleOriginal);
		// GUID 누락 시 URL을 GUID로 대체해 카테고리별 중복 제약을 적용합니다.
		String articleGuid = articleGuidOriginal == null ? articleUrl : articleGuidOriginal;
		String articleSummary = articleSummaryOriginal;
		String thumbnailUrl = thumbnailUrlOriginal;
		String authorNm = authorNmOriginal;
		String articleHashSource = articleUrlOriginal == null
			? buildMissingArticleHashSource(target, item, rankIndex)
			: articleUrl;

		NewsArticleCreatePO saveParam = new NewsArticleCreatePO();
		saveParam.setPressNo(target.getPressNo());
		saveParam.setCategoryCd(trimToNull(limitLength(target.getCategoryCd(), 50)));
		saveParam.setArticleGuid(articleGuid);
		saveParam.setArticleGuidHash(articleGuid == null ? null : sha256(articleGuid));
		saveParam.setArticleUrl(articleUrl);
		saveParam.setArticleUrlHash(sha256(articleHashSource));
		saveParam.setArticleTitle(articleTitle);
		saveParam.setArticleSummary(articleSummary);
		saveParam.setThumbnailUrl(thumbnailUrl);
		saveParam.setAuthorNm(authorNm);
		saveParam.setPublishedDt(publishedDt);
		saveParam.setRankScore(BigDecimal.valueOf(rankIndex + 1L));
		saveParam.setUseYn(hasRequiredMissing ? "N" : "Y");
		return saveParam;
	}

	// 기사 URL 누락 시 중복 방지용 해시 입력값을 생성합니다.
	private String buildMissingArticleHashSource(NewsRssTargetVO target, RssArticleItem item, int rankIndex) {
		StringBuilder builder = new StringBuilder();
		builder.append("MISSING_URL|");
		builder.append(target.getPressNo()).append('|');
		builder.append(trimToNull(target.getCategoryCd())).append('|');
		builder.append(trimToNull(item.guid())).append('|');
		builder.append(trimToNull(item.title())).append('|');
		builder.append(trimToNull(item.summary())).append('|');
		builder.append(trimToNull(item.authorNm())).append('|');
		builder.append(item.publishedDt()).append('|');
		builder.append(rankIndex + 1L);
		return builder.toString();
	}

	// SHA-256 해시 문자열을 생성합니다.
	private String sha256(String source) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = digest.digest(source.getBytes(StandardCharsets.UTF_8));
			StringBuilder builder = new StringBuilder();
			for (byte hashByte : hashBytes) {
				builder.append(String.format("%02x", hashByte));
			}
			return builder.toString();
		} catch (Exception exception) {
			throw new IllegalStateException("해시 생성에 실패했습니다.", exception);
		}
	}

	// 문자열 길이를 컬럼 최대 길이에 맞게 자릅니다.
	private String limitLength(String value, int maxLength) {
		String normalized = trimToNull(value);
		if (normalized == null) {
			return null;
		}
		if (normalized.length() <= maxLength) {
			return normalized;
		}
		return normalized.substring(0, maxLength);
	}

	// 문자열 값이 null이면 대체 문자열("-")로 변환합니다.
	private String valueOrDash(String value) {
		return value == null ? EMPTY_REPLACEMENT : value;
	}

	// 문자열 공백 여부를 정리해 null 가능 값으로 변환합니다.
	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
