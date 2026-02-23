package com.xodud1202.springbackend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsCategoryDeletePO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsCategoryOptionVO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsCategoryRowVO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsCategorySavePO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsCategorySaveRowPO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsListQueryPO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsListRowVO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsPressDeletePO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsPressOptionVO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsPressRowVO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsPressSavePO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsPressSaveRowPO;
import com.xodud1202.springbackend.domain.news.NewsArticleCreatePO;
import com.xodud1202.springbackend.domain.news.NewsCategorySummaryVO;
import com.xodud1202.springbackend.domain.news.NewsCollectResultVO;
import com.xodud1202.springbackend.domain.news.NewsListJsonSnapshotPublishResultVO;
import com.xodud1202.springbackend.domain.news.NewsListJsonSnapshotVO;
import com.xodud1202.springbackend.domain.news.NewsPressSummaryVO;
import com.xodud1202.springbackend.domain.news.NewsRssTargetVO;
import com.xodud1202.springbackend.domain.news.NewsSnapshotVO;
import com.xodud1202.springbackend.domain.news.NewsTopArticleVO;
import com.xodud1202.springbackend.domain.news.RssArticleItem;
import com.xodud1202.springbackend.mapper.NewsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
// 뉴스 관련 조회/관리/수집 기능을 통합 제공하는 서비스입니다.
public class NewsService {
	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final int MAX_ACTIVE_ARTICLE_COUNT_PER_TARGET = 5;
	private static final String EMPTY_REPLACEMENT = "-";
	private static final int MAX_TOP_ARTICLE_LIMIT = 20;
	private static final int DEFAULT_TOP_ARTICLE_LIMIT = 5;
	private static final String NEWS_LIST_JSON_SCHEMA_VERSION = "1.0.0";
	private static final String NEWS_LIST_JSON_SOURCE = "scheduler-rss";
	private static final String NEWS_LIST_JSON_FILE_NAME = "newsList.json";
	private static final String DEFAULT_NEWS_SNAPSHOT_TARGET_PATH = "/HDD1/Media/nas/news";

	private final NewsMapper newsMapper;
	private final RssFeedClient rssFeedClient;
	private final ObjectMapper objectMapper;
	private final FtpFileService ftpFileService;

	// 관리자 뉴스 목록 화면 언론사 옵션을 조회합니다.
	public List<AdminNewsPressOptionVO> getAdminNewsPressOptionList() {
		return newsMapper.getAdminNewsPressOptionList();
	}

	// 관리자 뉴스 목록 화면 카테고리 옵션을 조회합니다.
	public List<AdminNewsCategoryOptionVO> getAdminNewsCategoryOptionList(Long pressNo) {
		if (pressNo == null) {
			return List.of();
		}
		return newsMapper.getAdminNewsCategoryOptionList(pressNo);
	}

	// 관리자 뉴스 목록 조회 조건을 검증하고 정규화합니다.
	public String validateAdminNewsListQuery(AdminNewsListQueryPO param) {
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}

		String rawFrom = trimToNull(param.getCollectedFrom());
		String normalizedFrom = normalizeDateStart(rawFrom);
		if (rawFrom != null && normalizedFrom == null) {
			return "수집 시작일시 형식이 올바르지 않습니다.";
		}

		String rawTo = trimToNull(param.getCollectedTo());
		String normalizedTo = normalizeDateEnd(rawTo);
		if (rawTo != null && normalizedTo == null) {
			return "수집 종료일시 형식이 올바르지 않습니다.";
		}

		if (normalizedFrom != null && normalizedTo != null) {
			LocalDateTime fromTime = LocalDateTime.parse(normalizedFrom, DATE_TIME_FORMATTER);
			LocalDateTime toTime = LocalDateTime.parse(normalizedTo, DATE_TIME_FORMATTER);
			if (fromTime.isAfter(toTime)) {
				return "수집 시작일시가 종료일시보다 이후입니다.";
			}
		}

		param.setCollectedFrom(normalizedFrom);
		param.setCollectedTo(normalizedTo);
		param.setCategoryCd(trimToNull(param.getCategoryCd()));
		return null;
	}

	// 관리자 뉴스 목록을 페이지 단위로 조회합니다.
	public Map<String, Object> getAdminNewsList(AdminNewsListQueryPO param) {
		AdminNewsListQueryPO resolvedParam = param == null ? new AdminNewsListQueryPO() : param;
		int page = resolvedParam.getPage() == null || resolvedParam.getPage() < 1 ? 1 : resolvedParam.getPage();
		int pageSize = DEFAULT_PAGE_SIZE;
		int offset = (page - 1) * pageSize;

		resolvedParam.setPage(page);
		resolvedParam.setPageSize(pageSize);
		resolvedParam.setOffset(offset);

		List<AdminNewsListRowVO> list = newsMapper.getAdminNewsList(resolvedParam);
		int totalCount = newsMapper.getAdminNewsListCount(resolvedParam);

		Map<String, Object> result = new HashMap<>();
		result.put("list", list);
		result.put("totalCount", totalCount);
		result.put("page", page);
		result.put("pageSize", pageSize);
		return result;
	}

	// 관리자 뉴스 RSS 관리용 언론사 목록을 조회합니다.
	public List<AdminNewsPressRowVO> getAdminNewsPressManageList() {
		return newsMapper.getAdminNewsPressManageList();
	}

	// 관리자 뉴스 RSS 관리용 카테고리 목록을 조회합니다.
	public List<AdminNewsCategoryRowVO> getAdminNewsCategoryManageListByPressNo(Long pressNo) {
		return newsMapper.getAdminNewsCategoryManageListByPressNo(pressNo);
	}

	// 관리자 뉴스 RSS 관리 언론사 저장 요청을 검증합니다.
	public String validateAdminNewsPressSave(AdminNewsPressSavePO param) {
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}
		if (param.getRows() == null || param.getRows().isEmpty()) {
			return "저장할 언론사 데이터가 없습니다.";
		}
		if (param.getRegNo() == null) {
			return "등록자 정보를 확인해 주세요.";
		}
		if (param.getUdtNo() == null) {
			return "수정자 정보를 확인해 주세요.";
		}
		for (AdminNewsPressSaveRowPO row : param.getRows()) {
			if (row == null) {
				return "언론사 데이터 형식이 올바르지 않습니다.";
			}
			String pressNm = trimToNull(row.getPressNm());
			String pressNmError = validateTextLength(pressNm, "언론사명을 입력해 주세요.", 100, "언론사명은 100자 이내로 입력해 주세요.");
			if (pressNmError != null) {
				return pressNmError;
			}
			String useYn = trimToNull(row.getUseYn());
			String useYnError = validateUseYn(useYn);
			if (useYnError != null) {
				return useYnError;
			}
		}
		return null;
	}

	// 관리자 뉴스 RSS 관리 카테고리 저장 요청을 검증합니다.
	public String validateAdminNewsCategorySave(AdminNewsCategorySavePO param) {
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}
		if (param.getPressNo() == null) {
			return "언론사를 선택해 주세요.";
		}
		if (param.getRows() == null || param.getRows().isEmpty()) {
			return "저장할 카테고리 데이터가 없습니다.";
		}
		if (param.getRegNo() == null) {
			return "등록자 정보를 확인해 주세요.";
		}
		if (param.getUdtNo() == null) {
			return "수정자 정보를 확인해 주세요.";
		}
		for (AdminNewsCategorySaveRowPO row : param.getRows()) {
			if (row == null) {
				return "카테고리 데이터 형식이 올바르지 않습니다.";
			}
			String categoryCd = trimToNull(row.getCategoryCd());
			String categoryNm = trimToNull(row.getCategoryNm());
			String useYn = trimToNull(row.getUseYn());
			String sourceNm = trimToNull(row.getSourceNm());
			String rssUrl = trimToNull(row.getRssUrl());
			String categoryCdError = validateTextLength(categoryCd, "카테고리코드를 입력해 주세요.", 50, "카테고리코드는 50자 이내로 입력해 주세요.");
			if (categoryCdError != null) {
				return categoryCdError;
			}
			String categoryNmError = validateTextLength(categoryNm, "카테고리명을 입력해 주세요.", 100, "카테고리명은 100자 이내로 입력해 주세요.");
			if (categoryNmError != null) {
				return categoryNmError;
			}
			String useYnError = validateUseYn(useYn);
			if (useYnError != null) {
				return useYnError;
			}
			String sourceNmError = validateTextLength(sourceNm, "소스명을 입력해 주세요.", 150, "소스명은 150자 이내로 입력해 주세요.");
			if (sourceNmError != null) {
				return sourceNmError;
			}
			String rssUrlError = validateTextLength(rssUrl, "RSS URL을 입력해 주세요.", 150, "RSS URL은 150자 이내로 입력해 주세요.");
			if (rssUrlError != null) {
				return rssUrlError;
			}
		}
		return null;
	}

	// 관리자 뉴스 RSS 관리 언론사 삭제 요청을 검증합니다.
	public String validateAdminNewsPressDelete(AdminNewsPressDeletePO param) {
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}
		if (param.getPressNoList() == null || param.getPressNoList().isEmpty()) {
			return "삭제할 언론사를 선택해 주세요.";
		}
		if (param.getUdtNo() == null) {
			return "수정자 정보를 확인해 주세요.";
		}
		return null;
	}

	// 관리자 뉴스 RSS 관리 카테고리 삭제 요청을 검증합니다.
	public String validateAdminNewsCategoryDelete(AdminNewsCategoryDeletePO param) {
		if (param == null) {
			return "요청 데이터가 없습니다.";
		}
		if (param.getPressNo() == null) {
			return "언론사를 선택해 주세요.";
		}
		if (param.getCategoryCdList() == null || param.getCategoryCdList().isEmpty()) {
			return "삭제할 카테고리를 선택해 주세요.";
		}
		if (param.getUdtNo() == null) {
			return "수정자 정보를 확인해 주세요.";
		}
		return null;
	}

	@Transactional
	// 관리자 뉴스 RSS 관리 언론사를 저장합니다.
	public int saveAdminNewsPress(AdminNewsPressSavePO param) {
		int affectedCount = 0;
		int sortSeq = 1;

		for (AdminNewsPressSaveRowPO row : param.getRows()) {
			String pressNm = trimToNull(row.getPressNm());
			String useYn = trimToNull(row.getUseYn());

			if (row.getPressNo() == null) {
				row.setPressCd(buildAutoPressCd(sortSeq));
				row.setPressNm(pressNm);
				row.setUseYn(useYn);
				row.setSortSeq(sortSeq);
				row.setRegNo(param.getRegNo());
				row.setUdtNo(param.getUdtNo());
				affectedCount += newsMapper.insertAdminNewsPress(row);
			} else {
				row.setPressNm(pressNm);
				row.setUseYn(useYn);
				row.setUdtNo(param.getUdtNo());
				affectedCount += newsMapper.updateAdminNewsPress(row);
			}

			affectedCount += newsMapper.updateAdminNewsPressSortSeq(row.getPressNo(), sortSeq, param.getUdtNo());
			sortSeq += 1;
		}
		return affectedCount;
	}

	@Transactional
	// 관리자 뉴스 RSS 관리 카테고리를 저장합니다.
	public int saveAdminNewsCategory(AdminNewsCategorySavePO param) {
		int affectedCount = 0;
		int sortSeq = 1;

		for (AdminNewsCategorySaveRowPO row : param.getRows()) {
			String categoryCd = trimToNull(row.getCategoryCd());
			String categoryNm = trimToNull(row.getCategoryNm());
			String useYn = trimToNull(row.getUseYn());
			String sourceNm = trimToNull(row.getSourceNm());
			String rssUrl = trimToNull(row.getRssUrl());

			row.setPressNo(param.getPressNo());
			row.setCategoryCd(categoryCd);
			row.setCategoryNm(categoryNm);
			row.setUseYn(useYn);
			row.setSourceNm(sourceNm);
			row.setRssUrl(rssUrl);
			row.setSortSeq(sortSeq);
			row.setRegNo(param.getRegNo());
			row.setUdtNo(param.getUdtNo());

			if (isCreateCategoryRow(row)) {
				affectedCount += newsMapper.insertAdminNewsCategory(row);
			} else {
				affectedCount += newsMapper.updateAdminNewsCategory(row);
			}

			affectedCount += newsMapper.updateAdminNewsCategorySortSeq(
				row.getPressNo(),
				row.getCategoryCd(),
				sortSeq,
				param.getUdtNo()
			);
			sortSeq += 1;
		}
		return affectedCount;
	}

	@Transactional
	// 관리자 뉴스 RSS 관리 언론사를 삭제합니다.
	public int deleteAdminNewsPress(AdminNewsPressDeletePO param) {
		int affectedCount = 0;
		for (Long pressNo : param.getPressNoList()) {
			affectedCount += newsMapper.deleteAdminNewsArticleByPressNo(pressNo);
			affectedCount += newsMapper.deleteAdminNewsCategoryByPressNo(pressNo);
			affectedCount += newsMapper.deleteAdminNewsPressByPressNo(pressNo);
		}
		return affectedCount;
	}

	@Transactional
	// 관리자 뉴스 RSS 관리 카테고리를 삭제합니다.
	public int deleteAdminNewsCategory(AdminNewsCategoryDeletePO param) {
		int affectedCount = 0;
		for (String categoryCd : param.getCategoryCdList()) {
			String resolvedCategoryCd = trimToNull(categoryCd);
			if (resolvedCategoryCd == null) {
				continue;
			}
			affectedCount += newsMapper.deleteAdminNewsArticleByPressNoAndCategoryCd(param.getPressNo(), resolvedCategoryCd);
			affectedCount += newsMapper.deleteAdminNewsCategoryByPressNoAndCategoryCd(param.getPressNo(), resolvedCategoryCd);
		}
		return affectedCount;
	}

	// 카테고리 신규 등록 여부를 판단합니다.
	private boolean isCreateCategoryRow(AdminNewsCategorySaveRowPO row) {
		List<AdminNewsCategoryRowVO> currentList = newsMapper.getAdminNewsCategoryManageListByPressNo(row.getPressNo());
		for (AdminNewsCategoryRowVO current : currentList) {
			if (current.getCategoryCd() != null && current.getCategoryCd().equals(row.getCategoryCd())) {
				return false;
			}
		}
		return true;
	}

	// 자동 생성 언론사 코드를 반환합니다.
	private String buildAutoPressCd(int sortSeq) {
		String timeToken = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
		return "AUTO_PRESS_" + timeToken + "_" + sortSeq;
	}

	// 공개 뉴스 화면용 활성 언론사 목록을 조회합니다.
	public List<NewsPressSummaryVO> getActivePressList() {
		return newsMapper.getActivePressList();
	}

	// 공개 뉴스 화면용 활성 카테고리 목록을 조회합니다.
	public List<NewsCategorySummaryVO> getActiveCategoryList(String pressId) {
		Long pressNo = parsePressNo(pressId);
		if (pressNo == null) {
			return List.of();
		}
		return newsMapper.getActiveCategoryListByPressNo(pressNo);
	}

	// 공개 뉴스 화면용 상위 기사 목록을 조회합니다.
	public List<NewsTopArticleVO> getTopArticleList(String pressId, String categoryId, Integer limit) {
		Long pressNo = parsePressNo(pressId);
		String categoryCd = trimToNull(categoryId);
		if (pressNo == null || categoryCd == null) {
			return List.of();
		}
		int resolvedLimit = resolveTopArticleLimit(limit);
		return newsMapper.getTopArticleListByPressNoAndCategoryCd(pressNo, categoryCd, resolvedLimit);
	}

	// 공개 뉴스 화면용 단일 호출 스냅샷 데이터를 조회합니다.
	public NewsSnapshotVO getNewsSnapshot(String pressId, String categoryId, Integer limit) {
		// 기본 응답 객체와 기사 조회 건수를 먼저 초기화한다.
		NewsSnapshotVO response = new NewsSnapshotVO();
		int resolvedLimit = resolveTopArticleLimit(limit);
		List<NewsPressSummaryVO> pressList = newsMapper.getActivePressList();
		response.setPressList(pressList);
		response.setCategoryList(List.of());
		response.setArticleList(List.of());
		response.setSelectedPressId("");
		response.setSelectedCategoryId("");
		response.setFallbackAppliedYn("N");

		// 활성 언론사가 없으면 빈 응답을 그대로 반환한다.
		if (pressList.isEmpty()) {
			return response;
		}

		// 요청 언론사/카테고리 입력값을 정규화한다.
		String requestedPressId = trimToNull(pressId);
		String requestedCategoryId = trimToNull(categoryId);
		boolean fallbackApplied = false;

		// 요청 언론사가 활성 목록에 존재하면 사용하고, 아니면 1순위 언론사를 선택한다.
		String resolvedPressId = requestedPressId;
		boolean isRequestedPressValid = requestedPressId != null && pressList.stream().anyMatch((pressItem) -> requestedPressId.equals(pressItem.getId()));
		if (!isRequestedPressValid) {
			resolvedPressId = pressList.get(0).getId();
			fallbackApplied = requestedPressId != null;
		}
		response.setSelectedPressId(resolvedPressId);

		// 선택된 언론사의 활성 카테고리 목록을 조회한다.
		Long resolvedPressNo = parsePressNo(resolvedPressId);
		List<NewsCategorySummaryVO> categoryList = resolvedPressNo == null
			? List.of()
			: newsMapper.getActiveCategoryListByPressNo(resolvedPressNo);
		response.setCategoryList(categoryList);

		// 활성 카테고리가 없으면 기사 없이 반환한다.
		if (categoryList.isEmpty()) {
			response.setFallbackAppliedYn(fallbackApplied ? "Y" : "N");
			return response;
		}

		// 요청 카테고리가 활성 목록에 존재하면 사용하고, 아니면 1순위 카테고리를 선택한다.
		String resolvedCategoryId = requestedCategoryId;
		boolean isRequestedCategoryValid = requestedCategoryId != null
			&& categoryList.stream().anyMatch((categoryItem) -> requestedCategoryId.equals(categoryItem.getId()));
		if (!isRequestedCategoryValid) {
			resolvedCategoryId = categoryList.get(0).getId();
			fallbackApplied = fallbackApplied || requestedCategoryId != null;
		}
		response.setSelectedCategoryId(resolvedCategoryId);

		// 최종 선택된 카테고리 기준 기사 목록을 조회한다.
		List<NewsTopArticleVO> articleList = newsMapper.getTopArticleListByPressNoAndCategoryCd(
			resolvedPressNo,
			resolvedCategoryId,
			resolvedLimit
		);
		response.setArticleList(articleList);
		response.setFallbackAppliedYn(fallbackApplied ? "Y" : "N");
		return response;
	}

	// 뉴스 확장프로그램용 직접 조회 JSON 스냅샷 객체를 생성합니다.
	public NewsListJsonSnapshotVO buildNewsListJsonSnapshot() {
		// 스냅샷 생성 기준 시각과 메타 카운터를 초기화합니다.
		LocalDateTime generatedAt = LocalDateTime.now();
		String generatedAtText = generatedAt.format(DATE_TIME_FORMATTER);
		List<AdminNewsPressRowVO> allPressRowList = newsMapper.getAdminNewsPressManageList();
		List<NewsRssTargetVO> rssTargetList = newsMapper.getActiveNewsRssTargetList();
		int successTargetCount = 0;
		int failedTargetCount = 0;

		// 프론트 직접 조회용 루트 구조와 보조 인덱스를 초기화합니다.
		NewsListJsonSnapshotVO snapshot = new NewsListJsonSnapshotVO();
		snapshot.setPressList(new ArrayList<>());
		snapshot.setCategoryListByPressId(new LinkedHashMap<>());
		snapshot.setArticleListByPressCategoryKey(new LinkedHashMap<>());

		Map<String, String> defaultCategoryIdByPressId = new LinkedHashMap<>();
		List<String> pressIdList = new ArrayList<>();
		List<String> categoryKeyList = new ArrayList<>();
		// 활성 언론사 목록과 언론사별 활성 카테고리 목록을 정렬 순서대로 구성합니다.
		for (AdminNewsPressRowVO pressRow : allPressRowList) {
			if (pressRow == null || !"Y".equals(trimToNull(pressRow.getUseYn()))) {
				continue;
			}

			String pressId = String.valueOf(pressRow.getPressNo());
			NewsListJsonSnapshotVO.PressItem pressItem = new NewsListJsonSnapshotVO.PressItem();
			pressItem.setId(pressId);
			pressItem.setName(trimToNull(pressRow.getPressNm()));
			pressItem.setSortSeq(pressRow.getSortSeq());
			pressItem.setUseYn("Y");
			snapshot.getPressList().add(pressItem);
			pressIdList.add(pressId);

			List<AdminNewsCategoryRowVO> categoryRowList = newsMapper.getAdminNewsCategoryManageListByPressNo(pressRow.getPressNo());
			List<NewsListJsonSnapshotVO.CategoryItem> categoryItemList = new ArrayList<>();
			for (AdminNewsCategoryRowVO categoryRow : categoryRowList) {
				if (categoryRow == null || !"Y".equals(trimToNull(categoryRow.getUseYn()))) {
					continue;
				}

				NewsListJsonSnapshotVO.CategoryItem categoryItem = new NewsListJsonSnapshotVO.CategoryItem();
				categoryItem.setId(trimToNull(categoryRow.getCategoryCd()));
				categoryItem.setName(trimToNull(categoryRow.getCategoryNm()));
				categoryItem.setSortSeq(categoryRow.getSortSeq());
				categoryItem.setUseYn("Y");
				categoryItem.setRssUrl(trimToNull(categoryRow.getRssUrl()));
				categoryItem.setSourceNm(trimToNull(categoryRow.getSourceNm()));
				categoryItemList.add(categoryItem);
			}

			snapshot.getCategoryListByPressId().put(pressId, categoryItemList);
			// 언론사별 첫 활성 카테고리를 기본 선택값으로 저장합니다.
			String defaultCategoryId = categoryItemList.isEmpty() ? "" : trimToNull(categoryItemList.get(0).getId());
			defaultCategoryIdByPressId.put(pressId, defaultCategoryId == null ? "" : defaultCategoryId);

			// 카테고리별 기사 배열 키를 미리 생성해 프론트가 빈 배열도 직접 접근 가능하게 합니다.
			for (NewsListJsonSnapshotVO.CategoryItem categoryItem : categoryItemList) {
				String categoryId = trimToNull(categoryItem.getId());
				if (categoryId == null) {
					continue;
				}
				String categoryKey = buildPressCategoryKey(pressId, categoryId);
				snapshot.getArticleListByPressCategoryKey().put(categoryKey, new ArrayList<>());
				categoryKeyList.add(categoryKey);
			}
		}

		// 활성 RSS 대상별로 원본 전체 피드를 조회해 기사 목록을 구성합니다.
		for (NewsRssTargetVO target : rssTargetList) {
			try {
				String pressId = String.valueOf(target.getPressNo());
				String categoryId = trimToNull(target.getCategoryCd());
				if (categoryId == null) {
					failedTargetCount += 1;
					continue;
				}

				String categoryKey = buildPressCategoryKey(pressId, categoryId);
				boolean hasCategoryKey = snapshot.getArticleListByPressCategoryKey().containsKey(categoryKey);
				List<NewsListJsonSnapshotVO.ArticleItem> articleItemList = new ArrayList<>();
				List<RssArticleItem> feedItemList = rssFeedClient.fetchFeed(target.getRssUrl());
				for (int feedIndex = 0; feedIndex < feedItemList.size(); feedIndex += 1) {
					RssArticleItem feedItem = feedItemList.get(feedIndex);
					articleItemList.add(buildSnapshotArticleItem(target, feedItem, feedIndex + 1, generatedAtText));
				}

				snapshot.getArticleListByPressCategoryKey().put(categoryKey, articleItemList);
				if (!hasCategoryKey) {
					categoryKeyList.add(categoryKey);
				}
				successTargetCount += 1;
			} catch (Exception exception) {
				failedTargetCount += 1;
				log.warn(
					"뉴스 JSON 스냅샷 RSS 조회 실패 pressNo={}, categoryCd={}, rssUrl={}, message={}",
					target.getPressNo(),
					target.getCategoryCd(),
					target.getRssUrl(),
					exception.getMessage()
				);
			}
		}

		// 메타/기본선택/보조 인덱스를 스냅샷에 반영합니다.
		NewsListJsonSnapshotVO.Meta meta = new NewsListJsonSnapshotVO.Meta();
		meta.setGeneratedAt(generatedAtText);
		meta.setSchemaVersion(NEWS_LIST_JSON_SCHEMA_VERSION);
		meta.setSource(NEWS_LIST_JSON_SOURCE);
		meta.setTargetCount(rssTargetList.size());
		meta.setSuccessTargetCount(successTargetCount);
		meta.setFailedTargetCount(failedTargetCount);
		snapshot.setMeta(meta);

		NewsListJsonSnapshotVO.DefaultSelection defaultSelection = new NewsListJsonSnapshotVO.DefaultSelection();
		defaultSelection.setDefaultPressId(pressIdList.isEmpty() ? "" : pressIdList.get(0));
		defaultSelection.setDefaultCategoryIdByPressId(defaultCategoryIdByPressId);
		snapshot.setDefaultSelection(defaultSelection);

		NewsListJsonSnapshotVO.SnapshotIndex snapshotIndex = new NewsListJsonSnapshotVO.SnapshotIndex();
		snapshotIndex.setPressIdList(pressIdList);
		snapshotIndex.setCategoryKeyList(categoryKeyList);
		snapshot.setIndex(snapshotIndex);
		return snapshot;
	}

	// 뉴스 확장프로그램용 직접 조회 JSON 스냅샷 문자열을 생성합니다.
	public String buildNewsListJsonSnapshotJson() {
		try {
			return objectMapper.writeValueAsString(buildNewsListJsonSnapshot());
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("뉴스 JSON 스냅샷 직렬화에 실패했습니다.", exception);
		}
	}

	// 스냅샷 기사 항목을 생성합니다.
	private NewsListJsonSnapshotVO.ArticleItem buildSnapshotArticleItem(
		NewsRssTargetVO target,
		RssArticleItem feedItem,
		int rankScore,
		String collectedDt
	) {
		String articleGuid = trimToNull(limitLength(feedItem.guid(), 150));
		String articleUrl = trimToNull(limitLength(feedItem.link(), 150));
		String articleTitle = trimToNull(limitLength(feedItem.title(), 500));
		String articleSummary = trimToNull(feedItem.summary());
		String thumbnailUrl = trimToNull(limitLength(feedItem.thumbnailUrl(), 150));
		String authorNm = trimToNull(limitLength(feedItem.authorNm(), 100));
		boolean hasRequiredMissing = articleUrl == null || articleTitle == null;

		NewsListJsonSnapshotVO.ArticleItem articleItem = new NewsListJsonSnapshotVO.ArticleItem();
		articleItem.setId(buildSnapshotArticleId(target, feedItem, rankScore));
		articleItem.setTitle(valueOrDash(articleTitle));
		articleItem.setUrl(valueOrDash(articleUrl));
		articleItem.setPublishedDt(formatDateTime(feedItem.publishedDt()));
		articleItem.setSummary(articleSummary);
		articleItem.setThumbnailUrl(thumbnailUrl);
		articleItem.setAuthorNm(authorNm);
		articleItem.setRankScore(rankScore);
		articleItem.setUseYn(hasRequiredMissing ? "N" : "Y");
		articleItem.setCollectedDt(collectedDt);
		return articleItem;
	}

	// 스냅샷 기사 식별자를 생성합니다.
	private String buildSnapshotArticleId(NewsRssTargetVO target, RssArticleItem feedItem, int rankScore) {
		StringBuilder builder = new StringBuilder();
		builder.append(target.getPressNo()).append('|');
		builder.append(trimToNull(target.getCategoryCd())).append('|');
		builder.append(trimToNull(feedItem.guid())).append('|');
		builder.append(trimToNull(feedItem.link())).append('|');
		builder.append(trimToNull(feedItem.title())).append('|');
		builder.append(rankScore);
		return sha256(builder.toString());
	}

	// 스냅샷용 언론사-카테고리 키를 생성합니다.
	private String buildPressCategoryKey(String pressId, String categoryId) {
		return pressId + "|" + categoryId;
	}

	// 스냅샷 업로드 대상 FTP 경로를 반환합니다.
	private String resolveNewsSnapshotTargetPath() {
		return ftpFileService.resolveNewsSnapshotTargetPath(DEFAULT_NEWS_SNAPSHOT_TARGET_PATH);
	}

	// 날짜시간 값을 문자열로 포맷합니다.
	private String formatDateTime(LocalDateTime value) {
		return value == null ? null : value.format(DATE_TIME_FORMATTER);
	}

	// 뉴스 확장프로그램용 직접 조회 JSON 스냅샷 파일을 FTP에 원자적으로 업로드합니다.
	public NewsListJsonSnapshotPublishResultVO publishNewsListJsonSnapshot() {
		NewsListJsonSnapshotVO snapshot = buildNewsListJsonSnapshot();
		String snapshotJson;
		try {
			snapshotJson = objectMapper.writeValueAsString(snapshot);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("뉴스 JSON 스냅샷 직렬화에 실패했습니다.", exception);
		}

		String targetPath = resolveNewsSnapshotTargetPath();
		try {
			String tempFileName = ftpFileService.uploadUtf8TextFileAtomically(targetPath, NEWS_LIST_JSON_FILE_NAME, snapshotJson);
			return NewsListJsonSnapshotPublishResultVO.builder()
				.targetPath(targetPath)
				.fileName(NEWS_LIST_JSON_FILE_NAME)
				.tempFileName(tempFileName)
				.targetCount(snapshot.getMeta() == null ? null : snapshot.getMeta().getTargetCount())
				.successTargetCount(snapshot.getMeta() == null ? null : snapshot.getMeta().getSuccessTargetCount())
				.failedTargetCount(snapshot.getMeta() == null ? null : snapshot.getMeta().getFailedTargetCount())
				.jsonByteSize(snapshotJson.getBytes(StandardCharsets.UTF_8).length)
				.build();
		} catch (Exception exception) {
			throw new IllegalStateException("뉴스 JSON 스냅샷 FTP 업로드에 실패했습니다.", exception);
		}
	}

	// RSS 수집을 수행하고 결과를 반환합니다.
	@Transactional
	public NewsCollectResultVO collectNewsArticles() {
		// 수집 시작 전에 보존 기간(7일)을 초과한 기사를 정리합니다.
		int deletedOldArticleCount = newsMapper.deleteNewsArticleOlderThan7Days();
		log.info("뉴스 RSS 수집 전 7일 초과 기사 삭제 완료 deletedOldArticleCount={}", deletedOldArticleCount);

		List<NewsRssTargetVO> targetList = newsMapper.getActiveNewsRssTargetList();
		int successTargetCount = 0;
		int failedTargetCount = 0;
		int attemptedArticleCount = 0;
		int insertedArticleCount = 0;
		int skippedArticleCount = 0;

		for (NewsRssTargetVO target : targetList) {
			try {
				NewsArticleCreatePO resetParam = new NewsArticleCreatePO();
				resetParam.setPressNo(target.getPressNo());
				resetParam.setCategoryCd(target.getCategoryCd());
				newsMapper.resetRankScoreByTarget(resetParam);

				List<RssArticleItem> feedItems = rssFeedClient.fetchFeed(target.getRssUrl());
				int rankScore = 0;
				int activeUseYnCount = 0;
				for (int feedIndex = 0; feedIndex < feedItems.size(); feedIndex += 1) {
					if (activeUseYnCount >= MAX_ACTIVE_ARTICLE_COUNT_PER_TARGET) {
						break;
					}
					RssArticleItem feedItem = feedItems.get(feedIndex);
					rankScore += 1;
					attemptedArticleCount += 1;

					NewsArticleCreatePO saveParam = buildCreateParam(target, feedItem, rankScore);
					int affectedCount = newsMapper.insertNewsArticle(saveParam);
					if (affectedCount > 0) {
						insertedArticleCount += 1;
					}
					if ("Y".equals(saveParam.getUseYn())) {
						activeUseYnCount += 1;
					}
				}
				successTargetCount += 1;
			} catch (Exception exception) {
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

		return NewsCollectResultVO.builder()
			.targetCount(targetList.size())
			.successTargetCount(successTargetCount)
			.failedTargetCount(failedTargetCount)
			.attemptedArticleCount(attemptedArticleCount)
			.insertedArticleCount(insertedArticleCount)
			.skippedArticleCount(skippedArticleCount)
			.build();
	}

	// RSS 기사 저장 파라미터를 생성합니다.
	private NewsArticleCreatePO buildCreateParam(NewsRssTargetVO target, RssArticleItem item, int rankScore) {
		String articleGuidOriginal = trimToNull(limitLength(item.guid(), 150));
		String articleUrlOriginal = trimToNull(limitLength(item.link(), 150));
		String articleTitleOriginal = trimToNull(limitLength(item.title(), 500));
		String articleSummaryOriginal = trimToNull(item.summary());
		String thumbnailUrlOriginal = trimToNull(limitLength(item.thumbnailUrl(), 150));
		String authorNmOriginal = trimToNull(limitLength(item.authorNm(), 100));
		LocalDateTime publishedDt = item.publishedDt();
		boolean hasRequiredMissing = articleUrlOriginal == null || articleTitleOriginal == null;

		String articleUrl = valueOrDash(articleUrlOriginal);
		String articleTitle = valueOrDash(articleTitleOriginal);
		String articleGuid = articleGuidOriginal == null ? articleUrl : articleGuidOriginal;
		String articleSummary = articleSummaryOriginal;
		String thumbnailUrl = thumbnailUrlOriginal;
		String authorNm = authorNmOriginal;
		String articleHashSource = articleUrlOriginal == null
			? buildMissingArticleHashSource(target, item, rankScore)
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
		saveParam.setRankScore(BigDecimal.valueOf(rankScore));
		saveParam.setUseYn(hasRequiredMissing ? "N" : "Y");
		return saveParam;
	}

	// 기사 URL 누락 시 해시 생성 입력값을 생성합니다.
	private String buildMissingArticleHashSource(NewsRssTargetVO target, RssArticleItem item, int rankScore) {
		StringBuilder builder = new StringBuilder();
		builder.append("MISSING_URL|");
		builder.append(target.getPressNo()).append('|');
		builder.append(trimToNull(target.getCategoryCd())).append('|');
		builder.append(trimToNull(item.guid())).append('|');
		builder.append(trimToNull(item.title())).append('|');
		builder.append(trimToNull(item.summary())).append('|');
		builder.append(trimToNull(item.authorNm())).append('|');
		builder.append(item.publishedDt()).append('|');
		builder.append(rankScore);
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

	// 문자열 길이를 최대 길이에 맞게 제한합니다.
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

	// 값이 없으면 대체 문자열로 변환합니다.
	private String valueOrDash(String value) {
		return value == null ? EMPTY_REPLACEMENT : value;
	}

	// 언론사 번호 문자열을 Long으로 변환합니다.
	private Long parsePressNo(String pressId) {
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

	// 상위 기사 조회 건수를 제한합니다.
	private int resolveTopArticleLimit(Integer limit) {
		if (limit == null || limit <= 0) {
			return DEFAULT_TOP_ARTICLE_LIMIT;
		}
		return Math.min(limit, MAX_TOP_ARTICLE_LIMIT);
	}

	// 텍스트 필드의 필수 여부와 길이를 검증합니다.
	private String validateTextLength(String value, String emptyMessage, int maxLength, String maxMessage) {
		if (value == null) {
			return emptyMessage;
		}
		if (value.length() > maxLength) {
			return maxMessage;
		}
		return null;
	}

	// 사용여부 값(Y/N)을 검증합니다.
	private String validateUseYn(String useYn) {
		if (!"Y".equals(useYn) && !"N".equals(useYn)) {
			return "사용여부를 확인해 주세요.";
		}
		return null;
	}

	// 시작일시 문자열을 표준 포맷으로 변환합니다.
	private String normalizeDateStart(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.replace('T', ' ').trim();
		if (normalized.isEmpty()) {
			return null;
		}
		if (normalized.length() == 10) {
			normalized = normalized + " 00:00:00";
		}
		if (normalized.length() == 16) {
			normalized = normalized + ":00";
		}
		try {
			LocalDateTime.parse(normalized, DATE_TIME_FORMATTER);
			return normalized;
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	// 종료일시 문자열을 표준 포맷으로 변환합니다.
	private String normalizeDateEnd(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.replace('T', ' ').trim();
		if (normalized.isEmpty()) {
			return null;
		}
		if (normalized.length() == 10) {
			LocalDateTime date = LocalDateTime.parse(normalized + " 00:00:00", DATE_TIME_FORMATTER);
			return date.plusDays(1).format(DATE_TIME_FORMATTER);
		}
		if (normalized.length() == 16) {
			normalized = normalized + ":00";
		}
		try {
			LocalDateTime.parse(normalized, DATE_TIME_FORMATTER);
			return normalized;
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	// 문자열을 trim 처리하고 빈 값이면 null로 변환합니다.
	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
