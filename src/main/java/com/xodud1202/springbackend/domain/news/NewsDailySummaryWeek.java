package com.xodud1202.springbackend.domain.news;

import java.util.List;

/** 한국 시간의 오늘 날짜와 최근 일주일 중 뉴스를 선택할 수 있는 날짜를 전달합니다. */
public record NewsDailySummaryWeek(String today, List<NewsDailySummaryDate> dates) {}
