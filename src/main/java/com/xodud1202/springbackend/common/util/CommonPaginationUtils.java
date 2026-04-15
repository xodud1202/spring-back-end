package com.xodud1202.springbackend.common.util;

// 페이지 번호, 페이지 크기, 오프셋 계산 공통 유틸을 제공합니다.
public final class CommonPaginationUtils {
	private CommonPaginationUtils() {
		// 유틸 클래스는 인스턴스화하지 않습니다.
	}

	// 페이지 번호를 1 이상 기본값 기준으로 보정합니다.
	public static int normalizePage(Integer page, int defaultPage) {
		int resolvedDefaultPage = Math.max(defaultPage, 1);
		if (page == null || page < 1) {
			return resolvedDefaultPage;
		}
		return page;
	}

	// 페이지 크기를 기본값과 최대값 범위 안으로 보정합니다.
	public static int normalizePageSize(Integer pageSize, int defaultPageSize, int maxPageSize) {
		int resolvedDefaultPageSize = Math.max(defaultPageSize, 1);
		int resolvedMaxPageSize = Math.max(maxPageSize, resolvedDefaultPageSize);
		if (pageSize == null || pageSize < 1) {
			return resolvedDefaultPageSize;
		}
		return Math.min(pageSize, resolvedMaxPageSize);
	}

	// 오프셋 값을 0 이상으로 보정합니다.
	public static int normalizeNonNegativeOffset(Integer offset) {
		if (offset == null || offset < 0) {
			return 0;
		}
		return offset;
	}

	// 현재 페이지와 페이지 크기를 기준으로 조회 오프셋을 계산합니다.
	public static int calculateOffset(int pageNo, int pageSize) {
		if (pageNo < 1 || pageSize <= 0) {
			return 0;
		}
		return (pageNo - 1) * pageSize;
	}

	// 전체 건수와 페이지 크기를 기준으로 전체 페이지 수를 계산합니다.
	public static int calculateTotalPageCount(int totalCount, int pageSize) {
		if (totalCount <= 0 || pageSize <= 0) {
			return 0;
		}
		return (totalCount + pageSize - 1) / pageSize;
	}

	// 전체 페이지 수 범위 안으로 현재 페이지 번호를 보정합니다.
	public static int resolvePageNoWithinRange(int requestedPageNo, int totalPageCount) {
		if (totalPageCount <= 0) {
			return 1;
		}
		return Math.clamp(requestedPageNo, 1, totalPageCount);
	}
}
