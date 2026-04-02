package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.admin.order.AdminOrderReturnPageVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderReturnPageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
// 주문반품 도메인 비즈니스 로직을 제공합니다.
public class OrderReturnService {
	private final OrderService orderService;

	// 쇼핑몰 마이페이지 반품 신청 화면 데이터를 조회합니다.
	public ShopMypageOrderReturnPageVO getShopMypageOrderReturnPage(Long custNo, String ordNo, Integer ordDtlNo) {
		return orderService.getShopMypageOrderReturnPage(custNo, ordNo, ordDtlNo);
	}

	// 관리자 주문반품 신청 화면 데이터를 조회합니다.
	public AdminOrderReturnPageVO getAdminOrderReturnPage(String ordNo) {
		return orderService.getAdminOrderReturnPage(ordNo);
	}
}
