package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.admin.board.BoardPO;
import com.xodud1202.springbackend.domain.admin.board.BoardVO;
import com.xodud1202.springbackend.mapper.BoardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BoardService {
	private final BoardMapper boardMapper;

	public Map<String, Object> getAdminBoardList(BoardPO param) {
		int page = param.getPage() == null || param.getPage() < 1 ? 1 : param.getPage();
		int pageSize = 20;
		int offset = (page - 1) * pageSize;

		param.setPage(page);
		param.setPageSize(pageSize);
		param.setOffset(offset);

		List<BoardVO> list = boardMapper.getAdminBoardList(param);
		int totalCount = boardMapper.getAdminBoardCount(param);

		Map<String, Object> result = new HashMap<>();
		result.put("list", list);
		result.put("totalCount", totalCount);
		result.put("page", page);
		result.put("pageSize", pageSize);
		return result;
	}
}
