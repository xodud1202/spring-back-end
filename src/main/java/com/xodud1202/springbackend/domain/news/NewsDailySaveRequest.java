package com.xodud1202.springbackend.domain.news;

import java.util.List;

public record NewsDailySaveRequest(String yyyymmdd, String requestId, String sourceGeneratedAt,
                                   List<NewsDailyArticle> articles) {}

