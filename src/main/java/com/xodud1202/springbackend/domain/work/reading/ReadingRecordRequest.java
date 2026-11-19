package com.xodud1202.springbackend.domain.work.reading;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReadingRecordRequest(String title, String author, String translator, LocalDate startedOn, LocalDate completedOn, String content, BigDecimal rating, String notes, BigDecimal readingRound, Boolean reread) {
}