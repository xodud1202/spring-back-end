package com.xodud1202.springbackend.controller.snippet;

import com.xodud1202.springbackend.common.response.ApiMessageResponse;
import com.xodud1202.springbackend.domain.snippet.SnippetBootstrapResponse;
import com.xodud1202.springbackend.domain.snippet.SnippetDetailVO;
import com.xodud1202.springbackend.domain.snippet.SnippetFavoriteUpdateRequest;
import com.xodud1202.springbackend.domain.snippet.SnippetFolderSavePO;
import com.xodud1202.springbackend.domain.snippet.SnippetFolderVO;
import com.xodud1202.springbackend.domain.snippet.SnippetListResponse;
import com.xodud1202.springbackend.domain.snippet.SnippetSavePO;
import com.xodud1202.springbackend.domain.snippet.SnippetSaveResponse;
import com.xodud1202.springbackend.domain.snippet.SnippetTagSavePO;
import com.xodud1202.springbackend.domain.snippet.SnippetTagVO;
import com.xodud1202.springbackend.service.SnippetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
// 스니펫 메인 화면, 스니펫 CRUD, 폴더/태그 관리 API를 제공합니다.
public class SnippetController {
	private static final String SESSION_ATTR_SNIPPET_USER_NO = "snippetUserNo";

	private final SnippetService snippetService;

	@GetMapping("/api/snippet/bootstrap")
	// 스니펫 메인 화면 초기 구동 데이터를 조회합니다.
	public ResponseEntity<SnippetBootstrapResponse> getBootstrap(HttpServletRequest request) {
		try {
			// 로그인 사용자 기준의 초기 화면 데이터를 반환합니다.
			Long snippetUserNo = resolveRequiredSnippetUserNo(request);
			return ResponseEntity.ok(snippetService.getBootstrap(snippetUserNo));
		} catch (SecurityException | IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("스니펫 bootstrap 조회 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("스니펫 초기 데이터 조회에 실패했습니다.", exception);
		}
	}

	@GetMapping("/api/snippet/snippets")
	// 스니펫 목록을 조건과 페이징 기준으로 조회합니다.
	public ResponseEntity<SnippetListResponse> getSnippetList(
		HttpServletRequest request,
		@RequestParam(required = false) String q,
		@RequestParam(required = false) Long folderNo,
		@RequestParam(required = false) Long tagNo,
		@RequestParam(required = false) String languageCd,
		@RequestParam(required = false) String favoriteYn,
		@RequestParam(required = false) Integer page,
		@RequestParam(required = false) Integer size
	) {
		try {
			// 로그인 사용자 기준으로 필터링된 스니펫 목록을 반환합니다.
			Long snippetUserNo = resolveRequiredSnippetUserNo(request);
			return ResponseEntity.ok(snippetService.getSnippetList(snippetUserNo, q, folderNo, tagNo, languageCd, favoriteYn, page, size));
		} catch (SecurityException | IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("스니펫 목록 조회 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("스니펫 목록 조회에 실패했습니다.", exception);
		}
	}

	@GetMapping("/api/snippet/snippets/{snippetNo}")
	// 스니펫 상세 정보를 조회합니다.
	public ResponseEntity<SnippetDetailVO> getSnippetDetail(
		HttpServletRequest request,
		@PathVariable Long snippetNo
	) {
		try {
			// 로그인 사용자 기준으로 대상 스니펫 상세를 반환합니다.
			Long snippetUserNo = resolveRequiredSnippetUserNo(request);
			return ResponseEntity.ok(snippetService.getSnippetDetail(snippetUserNo, snippetNo));
		} catch (SecurityException | IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("스니펫 상세 조회 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("스니펫 상세 조회에 실패했습니다.", exception);
		}
	}

	@PostMapping("/api/snippet/snippets")
	// 신규 스니펫을 등록합니다.
	public ResponseEntity<SnippetSaveResponse> createSnippet(
		HttpServletRequest request,
		@Valid @RequestBody SnippetSavePO command
	) {
		try {
			// 로그인 사용자 기준으로 신규 스니펫을 저장합니다.
			Long snippetUserNo = resolveRequiredSnippetUserNo(request);
			return ResponseEntity.ok(snippetService.createSnippet(snippetUserNo, command));
		} catch (SecurityException | IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("스니펫 등록 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("스니펫 등록에 실패했습니다.", exception);
		}
	}

	@PutMapping("/api/snippet/snippets/{snippetNo}")
	// 기존 스니펫을 수정합니다.
	public ResponseEntity<SnippetSaveResponse> updateSnippet(
		HttpServletRequest request,
		@PathVariable Long snippetNo,
		@Valid @RequestBody SnippetSavePO command
	) {
		try {
			// 로그인 사용자 기준으로 대상 스니펫을 수정합니다.
			Long snippetUserNo = resolveRequiredSnippetUserNo(request);
			return ResponseEntity.ok(snippetService.updateSnippet(snippetUserNo, snippetNo, command));
		} catch (SecurityException | IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("스니펫 수정 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("스니펫 수정에 실패했습니다.", exception);
		}
	}

	@DeleteMapping("/api/snippet/snippets/{snippetNo}")
	// 스니펫을 삭제합니다.
	public ResponseEntity<ApiMessageResponse> deleteSnippet(
		HttpServletRequest request,
		@PathVariable Long snippetNo
	) {
		try {
			// 로그인 사용자 기준으로 대상 스니펫을 삭제합니다.
			Long snippetUserNo = resolveRequiredSnippetUserNo(request);
			snippetService.deleteSnippet(snippetUserNo, snippetNo);
			return ResponseEntity.ok(new ApiMessageResponse("스니펫이 삭제되었습니다."));
		} catch (SecurityException | IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("스니펫 삭제 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("스니펫 삭제에 실패했습니다.", exception);
		}
	}

	@PatchMapping("/api/snippet/snippets/{snippetNo}/favorite")
	// 스니펫 즐겨찾기 여부를 변경합니다.
	public ResponseEntity<ApiMessageResponse> updateFavorite(
		HttpServletRequest request,
		@PathVariable Long snippetNo,
		@Valid @RequestBody SnippetFavoriteUpdateRequest command
	) {
		try {
			// 로그인 사용자 기준으로 즐겨찾기 상태를 갱신합니다.
			Long snippetUserNo = resolveRequiredSnippetUserNo(request);
			snippetService.updateSnippetFavorite(snippetUserNo, snippetNo, command.favoriteYn());
			return ResponseEntity.ok(new ApiMessageResponse("즐겨찾기 상태가 변경되었습니다."));
		} catch (SecurityException | IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("스니펫 즐겨찾기 변경 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("즐겨찾기 변경에 실패했습니다.", exception);
		}
	}

	@PatchMapping("/api/snippet/snippets/{snippetNo}/copied")
	// 스니펫 복사 시 마지막 복사 일시를 갱신합니다.
	public ResponseEntity<ApiMessageResponse> markSnippetCopied(
		HttpServletRequest request,
		@PathVariable Long snippetNo
	) {
		try {
			// 로그인 사용자 기준으로 마지막 복사 일시를 갱신합니다.
			Long snippetUserNo = resolveRequiredSnippetUserNo(request);
			snippetService.markSnippetCopied(snippetUserNo, snippetNo);
			return ResponseEntity.ok(new ApiMessageResponse("복사 이력이 갱신되었습니다."));
		} catch (SecurityException | IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("스니펫 복사 이력 갱신 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("복사 이력 갱신에 실패했습니다.", exception);
		}
	}

	@PostMapping("/api/snippet/folders")
	// 사용자 폴더를 등록합니다.
	public ResponseEntity<SnippetFolderVO> createFolder(
		HttpServletRequest request,
		@Valid @RequestBody SnippetFolderSavePO command
	) {
		try {
			// 로그인 사용자 기준으로 신규 폴더를 저장합니다.
			Long snippetUserNo = resolveRequiredSnippetUserNo(request);
			return ResponseEntity.ok(snippetService.createFolder(snippetUserNo, command));
		} catch (SecurityException | IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("스니펫 폴더 등록 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("폴더 등록에 실패했습니다.", exception);
		}
	}

	@PutMapping("/api/snippet/folders/{folderNo}")
	// 사용자 폴더를 수정합니다.
	public ResponseEntity<SnippetFolderVO> updateFolder(
		HttpServletRequest request,
		@PathVariable Long folderNo,
		@Valid @RequestBody SnippetFolderSavePO command
	) {
		try {
			// 로그인 사용자 기준으로 대상 폴더를 수정합니다.
			Long snippetUserNo = resolveRequiredSnippetUserNo(request);
			return ResponseEntity.ok(snippetService.updateFolder(snippetUserNo, folderNo, command));
		} catch (SecurityException | IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("스니펫 폴더 수정 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("폴더 수정에 실패했습니다.", exception);
		}
	}

	@DeleteMapping("/api/snippet/folders/{folderNo}")
	// 사용자 폴더를 삭제합니다.
	public ResponseEntity<ApiMessageResponse> deleteFolder(
		HttpServletRequest request,
		@PathVariable Long folderNo
	) {
		try {
			// 로그인 사용자 기준으로 대상 폴더를 삭제합니다.
			Long snippetUserNo = resolveRequiredSnippetUserNo(request);
			snippetService.deleteFolder(snippetUserNo, folderNo);
			return ResponseEntity.ok(new ApiMessageResponse("폴더가 삭제되었습니다."));
		} catch (SecurityException | IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("스니펫 폴더 삭제 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("폴더 삭제에 실패했습니다.", exception);
		}
	}

	@PostMapping("/api/snippet/tags")
	// 사용자 태그를 등록합니다.
	public ResponseEntity<SnippetTagVO> createTag(
		HttpServletRequest request,
		@Valid @RequestBody SnippetTagSavePO command
	) {
		try {
			// 로그인 사용자 기준으로 신규 태그를 저장합니다.
			Long snippetUserNo = resolveRequiredSnippetUserNo(request);
			return ResponseEntity.ok(snippetService.createTag(snippetUserNo, command));
		} catch (SecurityException | IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("스니펫 태그 등록 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("태그 등록에 실패했습니다.", exception);
		}
	}

	@PutMapping("/api/snippet/tags/{tagNo}")
	// 사용자 태그를 수정합니다.
	public ResponseEntity<SnippetTagVO> updateTag(
		HttpServletRequest request,
		@PathVariable Long tagNo,
		@Valid @RequestBody SnippetTagSavePO command
	) {
		try {
			// 로그인 사용자 기준으로 대상 태그를 수정합니다.
			Long snippetUserNo = resolveRequiredSnippetUserNo(request);
			return ResponseEntity.ok(snippetService.updateTag(snippetUserNo, tagNo, command));
		} catch (SecurityException | IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("스니펫 태그 수정 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("태그 수정에 실패했습니다.", exception);
		}
	}

	@DeleteMapping("/api/snippet/tags/{tagNo}")
	// 사용자 태그를 삭제합니다.
	public ResponseEntity<ApiMessageResponse> deleteTag(
		HttpServletRequest request,
		@PathVariable Long tagNo
	) {
		try {
			// 로그인 사용자 기준으로 대상 태그를 삭제합니다.
			Long snippetUserNo = resolveRequiredSnippetUserNo(request);
			snippetService.deleteTag(snippetUserNo, tagNo);
			return ResponseEntity.ok(new ApiMessageResponse("태그가 삭제되었습니다."));
		} catch (SecurityException | IllegalArgumentException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("스니펫 태그 삭제 실패 message={}", exception.getMessage(), exception);
			throw new IllegalStateException("태그 삭제에 실패했습니다.", exception);
		}
	}

	// 세션에서 로그인된 스니펫 사용자번호를 읽어옵니다.
	private Long resolveRequiredSnippetUserNo(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null) {
			throw new SecurityException("로그인이 필요합니다.");
		}

		Object sessionValue = session.getAttribute(SESSION_ATTR_SNIPPET_USER_NO);
		if (sessionValue instanceof Long snippetUserNo && snippetUserNo > 0L) {
			return snippetUserNo;
		}
		if (sessionValue instanceof Integer snippetUserNo && snippetUserNo > 0) {
			return snippetUserNo.longValue();
		}
		if (sessionValue instanceof String snippetUserNoText) {
			try {
				Long snippetUserNo = Long.valueOf(snippetUserNoText);
				if (snippetUserNo > 0L) {
					return snippetUserNo;
				}
			} catch (NumberFormatException ignored) {
				// 문자열 파싱 실패는 아래 공통 예외로 처리합니다.
			}
		}
		throw new SecurityException("로그인이 필요합니다.");
	}
}
