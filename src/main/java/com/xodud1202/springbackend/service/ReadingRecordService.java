package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.common.util.CommonTextUtils;
import com.xodud1202.springbackend.domain.work.reading.ReadingRecord;
import com.xodud1202.springbackend.domain.work.reading.ReadingRecordRequest;
import com.xodud1202.springbackend.mapper.ReadingRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReadingRecordService {
	private static final BigDecimal RATING_STEP = new BigDecimal("0.5");
	private static final BigDecimal MAX_RATING = BigDecimal.valueOf(5);
	private final ReadingRecordMapper mapper;

	public List<ReadingRecord> getReadingRecords(Long owner, String title, String author, LocalDate from, LocalDate to) {
		if (from != null && to != null && to.isBefore(from)) {
			throw new IllegalArgumentException("검색 종료일은 시작일보다 빠를 수 없습니다.");
		}
		return mapper.selectByOwner(owner, normalize(title, "제목", 200, false), normalize(author, "지은이", 200, false), from, to);
	}

	public ReadingRecord getReadingRecord(Long owner, long id) {
		requirePositiveId(id);
		return mapper.selectOne(owner, id);
	}

	public boolean updateReadingRecord(Long owner, long id, ReadingRecordRequest record) {
		requirePositiveId(id);
		return mapper.update(owner, id, validate(record)) > 0 || mapper.selectOne(owner, id) != null;
	}

	public boolean deleteReadingRecord(Long owner, long id) {
		requirePositiveId(id);
		return mapper.delete(owner, id) > 0;
	}

	private void requirePositiveId(long id) {
		if (id <= 0) throw new IllegalArgumentException("독서 기록 번호가 올바르지 않습니다.");
	}

	public void createReadingRecord(Long owner, ReadingRecordRequest record) {
		mapper.insert(owner, validate(record));
	}

	private ReadingRecordRequest validate(ReadingRecordRequest record) {
		if (record.startedOn() != null && record.completedOn() != null
			&& record.completedOn().isBefore(record.startedOn())) {
			throw new IllegalArgumentException("독서 완료일은 시작일보다 빠를 수 없습니다.");
		}
		BigDecimal rating = record.rating();
		if (rating != null && (rating.signum() < 0 || rating.compareTo(MAX_RATING) > 0
			|| rating.remainder(RATING_STEP).signum() != 0)) {
			throw new IllegalArgumentException("평점은 0~5점 사이에서 0.5점 단위로 입력해 주세요.");
		}
		return new ReadingRecordRequest(
			normalize(record.title(), "제목", 200, true),
			normalize(record.author(), "지은이", 200, true),
			normalize(record.translator(), "옮긴이", 200, false),
			record.startedOn(), record.completedOn(),
			normalize(record.content(), "내용", 200, false), rating,
			normalize(record.notes(), "비고", 4000, false)
		);
	}

	private String normalize(String value, String field, int limit, boolean required) {
		String normalized = CommonTextUtils.trimToNull(value);
		if (required && normalized == null) {
			throw new IllegalArgumentException(field + "을(를) 입력해 주세요.");
		}
		if (normalized != null && normalized.length() > limit) {
			throw new IllegalArgumentException(field + "은(는) " + limit + "자 이하로 입력해 주세요.");
		}
		return normalized;
	}
}