package com.xodud1202.springbackend.controller;

import com.xodud1202.springbackend.domain.admin.news.AdminNewsCategoryDeletePO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsCategoryOptionVO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsCategoryRowVO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsCategorySavePO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsListQueryPO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsPressDeletePO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsPressOptionVO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsPressRowVO;
import com.xodud1202.springbackend.domain.admin.news.AdminNewsPressSavePO;
import com.xodud1202.springbackend.domain.news.NewsListPressShardSnapshotPublishResultVO;
import com.xodud1202.springbackend.service.NewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
// 관리자 뉴스 목록/관리 API를 제공하는 컨트롤러입니다.
public class AdminNewsController {
	private final NewsService newsService;

	// 관리자 뉴스 목록 화면 언론사 옵션을 조회합니다.
	@GetMapping("/api/admin/news/press/list")
	public ResponseEntity<Object> getAdminNewsPressOptionList() {
		List<AdminNewsPressOptionVO> list = newsService.getAdminNewsPressOptionList();
		return ResponseEntity.ok(list);
	}

	// 관리자 뉴스 목록 화면 카테고리 옵션을 조회합니다.
	@GetMapping("/api/admin/news/category/list")
	public ResponseEntity<Object> getAdminNewsCategoryOptionList(@RequestParam Long pressNo) {
		if (pressNo == null) {
			return ResponseEntity.badRequest().body(Map.of("message", "언론사를 선택해 주세요."));
		}
		List<AdminNewsCategoryOptionVO> list = newsService.getAdminNewsCategoryOptionList(pressNo);
		return ResponseEntity.ok(list);
	}

	// 관리자 뉴스 목록을 조회합니다.
	@GetMapping("/api/admin/news/list")
	public ResponseEntity<Object> getAdminNewsList(AdminNewsListQueryPO param) {
		String validationMessage = newsService.validateAdminNewsListQuery(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		return ResponseEntity.ok(newsService.getAdminNewsList(param));
	}

	// 관리자 뉴스 RSS 관리 언론사 목록을 조회합니다.
	@GetMapping("/api/admin/news/rss/manage/press/list")
	public ResponseEntity<Object> getAdminNewsPressManageList() {
		List<AdminNewsPressRowVO> list = newsService.getAdminNewsPressManageList();
		return ResponseEntity.ok(list);
	}

	// 관리자 뉴스 RSS 관리 카테고리 목록을 조회합니다.
	@GetMapping("/api/admin/news/rss/manage/category/list")
	public ResponseEntity<Object> getAdminNewsCategoryManageList(@RequestParam Long pressNo) {
		if (pressNo == null) {
			return ResponseEntity.badRequest().body(Map.of("message", "언론사를 선택해 주세요."));
		}
		List<AdminNewsCategoryRowVO> list = newsService.getAdminNewsCategoryManageListByPressNo(pressNo);
		return ResponseEntity.ok(list);
	}

	// 관리자 뉴스 RSS 관리 언론사 목록을 저장합니다.
	@PostMapping("/api/admin/news/rss/manage/press/save")
	public ResponseEntity<Object> saveAdminNewsPress(@RequestBody AdminNewsPressSavePO param) {
		String validationMessage = newsService.validateAdminNewsPressSave(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		return ResponseEntity.ok(newsService.saveAdminNewsPress(param));
	}

	// 관리자 뉴스 RSS 관리 카테고리 목록을 저장합니다.
	@PostMapping("/api/admin/news/rss/manage/category/save")
	public ResponseEntity<Object> saveAdminNewsCategory(@RequestBody AdminNewsCategorySavePO param) {
		String validationMessage = newsService.validateAdminNewsCategorySave(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		return ResponseEntity.ok(newsService.saveAdminNewsCategory(param));
	}

	// 관리자 뉴스 RSS 관리 언론사를 삭제합니다.
	@PostMapping("/api/admin/news/rss/manage/press/delete")
	public ResponseEntity<Object> deleteAdminNewsPress(@RequestBody AdminNewsPressDeletePO param) {
		String validationMessage = newsService.validateAdminNewsPressDelete(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		return ResponseEntity.ok(newsService.deleteAdminNewsPress(param));
	}

	// 관리자 뉴스 RSS 관리 카테고리를 삭제합니다.
	@PostMapping("/api/admin/news/rss/manage/category/delete")
	public ResponseEntity<Object> deleteAdminNewsCategory(@RequestBody AdminNewsCategoryDeletePO param) {
		String validationMessage = newsService.validateAdminNewsCategoryDelete(param);
		if (validationMessage != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}
		return ResponseEntity.ok(newsService.deleteAdminNewsCategory(param));
	}
	
	@GetMapping("/api/news/refresh/file")
	public ResponseEntity<Object> refreshNewsListJsonSnapshot() {
		try {
			NewsListPressShardSnapshotPublishResultVO publishResult = newsService.publishNewsListPressShardJsonSnapshot();
			log.info(
					"뉴스 메타+언론사 shard JSON 업로드 완료 baseTargetPath={}, metaFileName={}, pressShardCount={}, shardSuccessCount={}, shardFailedCount={}, metaJsonByteSize={}, totalShardJsonByteSize={}",
					publishResult.getBaseTargetPath(),
					publishResult.getMetaFileName(),
					publishResult.getPressShardCount(),
					publishResult.getShardSuccessCount(),
					publishResult.getShardFailedCount(),
					publishResult.getMetaJsonByteSize(),
					publishResult.getTotalShardJsonByteSize()
			);

			return ResponseEntity.ok(publishResult);
		} catch (Exception exception) {
			log.error("뉴스 메타+언론사 shard JSON 업로드 실패 message={}", exception.getMessage(), exception);
			return ResponseEntity.internalServerError().body(Map.of("error", exception.getMessage()));
		}
	}
}
