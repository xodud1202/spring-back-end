package com.xodud1202.springbackend.domain.shop.mypage;

import lombok.Data;

import java.util.List;

@Data
// 쇼핑몰 마이페이지 주문번호 단위 주문 그룹 정보를 전달합니다.
public class ShopMypageOrderGroupVO {
	// 주문번호입니다.
	private String ordNo;
	// 주문일시입니다.
	private String orderDt;
	// 주문상세 목록입니다.
	private List<ShopMypageOrderDetailItemVO> detailList;
}
