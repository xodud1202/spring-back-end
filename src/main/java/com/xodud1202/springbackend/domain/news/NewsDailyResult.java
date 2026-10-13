package com.xodud1202.springbackend.domain.news;

import java.util.List;
import java.time.LocalDateTime;

public record NewsDailyResult(String status, String yyyymmdd, Integer seq, String requestId,
                              String sourceGeneratedAt, List<NewsDailyArticle> articles,
                              String failureCode, LocalDateTime startedAt, LocalDateTime finishedAt) {
    public NewsDailyResult(String status, String yyyymmdd, Integer seq, String requestId,
                           String sourceGeneratedAt, List<NewsDailyArticle> articles) {
        this(status, yyyymmdd, seq, requestId, sourceGeneratedAt, articles, null, null, null);
    }
}

