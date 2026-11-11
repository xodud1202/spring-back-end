package com.xodud1202.springbackend.controller.resume;

import com.xodud1202.springbackend.domain.admin.board.BoardVO;
import com.xodud1202.springbackend.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 이력서 프론트에서 제공하는 공개 게시글 조회를 담당합니다. */
@RestController
@RequiredArgsConstructor
public class ResumeBoardController {
	private final BoardService boardService;

	/** 로그인 없이 꾸깃의 최신 공개 개인정보 처리방침을 조회합니다. */
	@GetMapping("/api/resume/notice/kkugit/privacy")
	public ResponseEntity<BoardVO> getKkugitPrivacy() {
		// 다른 게시판을 요청할 수 없도록 개인정보 처리방침 구분을 고정합니다.
		BoardVO board = boardService.getLatestVisibleBoard("KKUGIT_PRIVACY");
		if (board == null) {
			return ResponseEntity.notFound().cacheControl(CacheControl.noStore()).build();
		}

		// 개정된 처리방침이 이전 응답 캐시에 가려지지 않도록 저장을 금지합니다.
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(board);
	}
}
