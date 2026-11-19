package com.xodud1202.springbackend.controller.work;

import com.xodud1202.springbackend.domain.work.reading.ReadingRecord;
import com.xodud1202.springbackend.domain.work.reading.ReadingRecordRequest;
import com.xodud1202.springbackend.service.ReadingRecordService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/work/reading-records")
public class WorkReadingRecordController extends WorkControllerSupport {
	private final ReadingRecordService readingRecordService;

	@GetMapping
	public ResponseEntity<List<ReadingRecord>> getReadingRecords(
		HttpServletRequest request,
		@RequestParam(name = "title", required = false) String title,
		@RequestParam(name = "author", required = false) String author,
		@RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
		@RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
		@RequestParam(name = "reread", required = false) Boolean reread
	) {
		Long owner = resolveRequiredWorkUserNo(request);
		return ResponseEntity.ok().cacheControl(CacheControl.noStore())
			.body(readingRecordService.getReadingRecords(owner, title, author, from, to, reread));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ReadingRecord> getReadingRecord(HttpServletRequest request, @PathVariable("id") long id) {
		Long owner = resolveRequiredWorkUserNo(request);
		ReadingRecord record = readingRecordService.getReadingRecord(owner, id);
		return record == null ? ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(CacheControl.noStore()).build()
			: ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(record);
	}

	@PutMapping("/{id}")
	public ResponseEntity<Map<String, String>> updateReadingRecord(HttpServletRequest request,
		@PathVariable("id") long id, @RequestBody ReadingRecordRequest record) {
		Long owner = resolveRequiredWorkUserNo(request);
		if (!readingRecordService.updateReadingRecord(owner, id, record)) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(CacheControl.noStore()).build();
		}
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("message", "독서 기록을 수정했습니다."));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteReadingRecord(HttpServletRequest request, @PathVariable("id") long id) {
		Long owner = resolveRequiredWorkUserNo(request);
		HttpStatus status = readingRecordService.deleteReadingRecord(owner, id) ? HttpStatus.NO_CONTENT : HttpStatus.NOT_FOUND;
		return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).build();
	}

	@PostMapping
	public ResponseEntity<Map<String, String>> createReadingRecord(
		HttpServletRequest request, @RequestBody ReadingRecordRequest record
	) {
		Long owner = resolveRequiredWorkUserNo(request);
		readingRecordService.createReadingRecord(owner, record);
		return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
			.body(Map.of("message", "독서 기록을 저장했습니다."));
	}
}