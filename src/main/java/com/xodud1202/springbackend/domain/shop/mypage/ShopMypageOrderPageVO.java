package com.xodud1202.springbackend.domain.shop.mypage;

import lombok.Data;

import java.util.List;

@Data
// 쇼핑몰 마이페이지 주문내역 페이지 응답을 전달합니다.
public class ShopMypageOrderPageVO {
	// 주문번호 단위 주문 목록입니다.
	private List<ShopMypageOrderGroupVO> orderList;
	// 검색 기간 내 주문번호 건수입니다.
	private Integer orderCount;
	// 현재 페이지 번호입니다.
	private Integer pageNo;
	// 페이지 크기입니다.
	private Integer pageSize;
	// 전체 페이지 수입니다.
	private Integer totalPageCount;
	// 조회 시작일입니다.
	private String startDate;
	// 조회 종료일입니다.
	private String endDate;
	// 주문상세 상태 요약 정보입니다.
	private ShopMypageOrderStatusSummaryVO statusSummary;
}
