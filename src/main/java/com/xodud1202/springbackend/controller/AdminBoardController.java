package com.xodud1202.springbackend.controller;

import com.xodud1202.springbackend.domain.admin.board.BoardPO;
import com.xodud1202.springbackend.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AdminBoardController {
	private final BoardService boardService;

	@GetMapping("/api/admin/board/list")
	public ResponseEntity<Map<String, Object>> getBoardList(BoardPO param) {
		return ResponseEntity.ok(boardService.getAdminBoardList(param));
	}
}
