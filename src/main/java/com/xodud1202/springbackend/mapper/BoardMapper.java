package com.xodud1202.springbackend.mapper;

import com.xodud1202.springbackend.domain.admin.board.BoardPO;
import com.xodud1202.springbackend.domain.admin.board.BoardVO;
import java.util.List;

public interface BoardMapper {
	List<BoardVO> getAdminBoardList(BoardPO param);
	int getAdminBoardCount(BoardPO param);
	BoardVO getAdminBoardDetail(BoardPO param);
}
