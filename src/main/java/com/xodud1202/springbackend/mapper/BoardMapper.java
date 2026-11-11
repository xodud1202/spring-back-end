package com.xodud1202.springbackend.mapper;

import com.xodud1202.springbackend.domain.admin.board.BoardPO;
import com.xodud1202.springbackend.domain.admin.board.BoardVO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 게시판 데이터의 조회와 변경을 담당합니다. */
public interface BoardMapper {
	/** 게시판 구분별로 삭제되지 않은 공개 게시글 중 번호가 가장 큰 한 건을 조회합니다. */
	BoardVO getLatestVisibleBoard(@Param("boardDivCd") String boardDivCd);

	List<BoardVO> getAdminBoardList(BoardPO param);
	int getAdminBoardCount(BoardPO param);
	BoardVO getAdminBoardDetail(BoardPO param);
	int updateAdminBoard(BoardPO param);
	int insertAdminBoard(BoardPO param);
	int deleteAdminBoard(BoardPO param);
}
