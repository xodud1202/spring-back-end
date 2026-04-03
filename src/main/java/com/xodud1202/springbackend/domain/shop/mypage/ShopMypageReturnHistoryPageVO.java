package com.xodud1202.springbackend.domain.shop.mypage;

import lombok.Data;

import java.util.List;

@Data
// 쇼핑몰 마이페이지 반품내역 페이지 응답을 전달합니다.
public class ShopMypageReturnHistoryPageVO {
	// 클레임 단위 반품 목록입니다.
	private List<ShopMypageReturnHistoryVO> returnList;
	// 검색 기간 내 반품 건수입니다.
	private Integer returnCount;
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
}
