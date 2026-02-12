package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.news.RssArticleItem;

import java.util.List;

// RSS/Atom 피드 원문을 기사 목록으로 변환하는 클라이언트 계약입니다.
public interface RssFeedClient {
	// RSS URL을 조회해 기사 목록을 반환합니다.
	List<RssArticleItem> fetchFeed(String rssUrl);
}
