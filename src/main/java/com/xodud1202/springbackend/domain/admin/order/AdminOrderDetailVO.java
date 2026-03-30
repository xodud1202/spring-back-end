package com.xodud1202.springbackend.domain.admin.order;

import lombok.Data;

import java.util.List;

@Data
// 관리자 주문 상세 조회 전체 응답을 정의합니다.
public class AdminOrderDetailVO {
	// 주문 마스터 정보입니다.
	private AdminOrderMasterVO master;
	// 주문 상세 목록입니다.
	private List<AdminOrderDetailRowVO> list;
	// 주문 클레임 목록입니다.
	private List<AdminOrderClaimRowVO> claimList;
}
