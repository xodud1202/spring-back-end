package com.xodud1202.springbackend.domain.work.stock;

import com.xodud1202.springbackend.domain.admin.common.UserInfoVO;
import com.xodud1202.springbackend.domain.common.CommonCodeVO;
import lombok.Data;

import java.util.List;

@Data
// 매매일지 화면 초기 구동에 필요한 사용자와 선택 목록을 전달합니다.
public class WorkStockSaleBootstrapResponseVO {
	private UserInfoVO currentUser;
	private List<CommonCodeVO> accountList;
	private List<CommonCodeVO> stockList;
}
