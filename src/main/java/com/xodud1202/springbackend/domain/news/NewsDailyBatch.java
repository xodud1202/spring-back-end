package com.xodud1202.springbackend.domain.news;

import java.time.LocalDateTime;

public record NewsDailyBatch(String yyyymmdd, int seq, String requestId, String payloadHash,
                             String sourceGeneratedAt, int articleCount, Long regNo,
                             String status, String failureCode, LocalDateTime startedAt, LocalDateTime finishedAt) {}

