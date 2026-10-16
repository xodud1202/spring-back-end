package com.xodud1202.springbackend.domain.news;

/** 뉴스가 존재하는 날짜와 해당 날짜의 최대 시퀀스를 전달합니다. */
public record NewsDailySummaryDate(String yyyymmdd, int seq) {}
