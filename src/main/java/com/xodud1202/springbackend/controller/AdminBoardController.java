package com.xodud1202.springbackend.controller;

import com.xodud1202.springbackend.domain.admin.board.BoardPO;
import com.xodud1202.springbackend.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AdminBoardController {
	private final BoardService boardService;

	// 관리자 게시판 목록을 조회합니다.
	@GetMapping("/api/admin/board/list")
	public ResponseEntity<Map<String, Object>> getBoardList(BoardPO param) {
		return ResponseEntity.ok(boardService.getAdminBoardList(param));
	}

	// 관리자 게시판 상세 정보를 조회합니다.
	@GetMapping("/api/admin/board/detail")
	public ResponseEntity<Object> getBoardDetail(BoardPO param) {
		return ResponseEntity.ok(boardService.getAdminBoardDetail(param));
	}

	// 관리자 게시판 정보를 수정합니다.
	@PostMapping("/api/admin/board/update")
	public ResponseEntity<Object> updateBoard(@RequestBody BoardPO param) {
		return ResponseEntity.ok(boardService.updateAdminBoard(param));
	}

	// 관리자 게시판에 게시글을 등록합니다.
	@PostMapping("/api/admin/board/create")
	public ResponseEntity<Object> createBoard(@RequestBody BoardPO param) {
		return ResponseEntity.ok(boardService.insertAdminBoard(param));
	}

	// 관리자 게시글을 삭제 처리합니다.
	@PostMapping("/api/admin/board/delete")
	public ResponseEntity<Object> deleteBoard(@RequestBody BoardPO param) {
		return ResponseEntity.ok(boardService.deleteAdminBoard(param));
	}
}
