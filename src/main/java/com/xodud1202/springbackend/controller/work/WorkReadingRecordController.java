package com.xodud1202.springbackend.controller.work;

import com.xodud1202.springbackend.domain.work.reading.ReadingRecord;
import com.xodud1202.springbackend.domain.work.reading.ReadingRecordRequest;
import com.xodud1202.springbackend.service.ReadingRecordService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/work/reading-records")
public class WorkReadingRecordController extends WorkControllerSupport {
	private final ReadingRecordService readingRecordService;

	@GetMapping
	public ResponseEntity<List<ReadingRecord>> getReadingRecords(HttpServletRequest request) {
		Long owner = resolveRequiredWorkUserNo(request);
		return ResponseEntity.ok().cacheControl(CacheControl.noStore())
			.body(readingRecordService.getReadingRecords(owner));
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