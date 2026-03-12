package com.xodud1202.springbackend.domain.shop.exhibition;

import lombok.Data;

import java.util.List;

@Data
// 쇼핑몰 기획전 목록 화면 응답 데이터를 전달합니다.
public class ShopExhibitionPageVO {
	// 기획전 목록입니다.
	private List<ShopExhibitionItemVO> exhibitionList;
	// 전체 기획전 건수입니다.
	private Integer totalCount;
	// 현재 페이지 번호입니다.
	private Integer pageNo;
	// 페이지 크기입니다.
	private Integer pageSize;
	// 전체 페이지 수입니다.
	private Integer totalPageCount;
}

