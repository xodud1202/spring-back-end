package com.xodud1202.springbackend.domain.shop.mypage;

import lombok.Data;

import java.util.List;

@Data
// 쇼핑몰 마이페이지 반품내역 클레임 단위 항목을 전달합니다.
public class ShopMypageReturnHistoryVO {
	// 클레임 번호입니다.
	private String clmNo;
	// 주문 번호입니다.
	private String ordNo;
	// 반품 신청 일시입니다.
	private String chgDt;
	// 반품 배송비 조정 금액입니다.
	private Integer payDelvAmt;
	// 반품 상품 상세 목록입니다.
	private List<ShopMypageReturnHistoryDetailVO> detailList;
}
