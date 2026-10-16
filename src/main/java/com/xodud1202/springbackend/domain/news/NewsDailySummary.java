package com.xodud1202.springbackend.domain.news;

import java.util.List;

/** 선택 날짜에 저장된 최대 시퀀스의 뉴스 요약을 전달합니다. */
public record NewsDailySummary(String yyyymmdd, Integer seq, List<NewsDailyArticle> articles) {}
