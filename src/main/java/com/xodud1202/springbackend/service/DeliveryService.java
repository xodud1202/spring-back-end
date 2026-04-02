package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.admin.order.AdminOrderDetailStatusUpdatePO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderDetailStatusUpdateVO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderStartDeliveryListResponseVO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderStartDeliveryPreparePO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderStartDeliveryStatusPO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderStartDeliveryStatusUpdateVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderDetailStatusUpdatePO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderDetailStatusUpdateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
// 배송 도메인 비즈니스 로직을 제공합니다.
public class DeliveryService {
	private final OrderService orderService;

	// 관리자 주문상세를 상품 준비중 상태로 변경합니다.
	public AdminOrderDetailStatusUpdateVO prepareAdminOrderDetail(AdminOrderDetailStatusUpdatePO param) {
		return orderService.prepareAdminOrderDetail(param);
	}

	// 관리자 배송 시작 관리 목록을 조회합니다.
	public AdminOrderStartDeliveryListResponseVO getAdminOrderStartDeliveryList(
		Integer page,
		Integer pageSize,
		String ordDtlStatCd
	) {
		return orderService.getAdminOrderStartDeliveryList(page, pageSize, ordDtlStatCd);
	}

	// 관리자 상품 준비중 주문을 배송 준비중 상태로 변경합니다.
	public AdminOrderStartDeliveryStatusUpdateVO prepareAdminOrderStartDelivery(AdminOrderStartDeliveryPreparePO param) {
		return orderService.prepareAdminOrderStartDelivery(param);
	}

	// 관리자 배송 준비중 주문을 배송중 상태로 변경합니다.
	public AdminOrderStartDeliveryStatusUpdateVO startAdminOrderStartDelivery(AdminOrderStartDeliveryStatusPO param) {
		return orderService.startAdminOrderStartDelivery(param);
	}

	// 관리자 배송중 주문을 배송완료 상태로 변경합니다.
	public AdminOrderStartDeliveryStatusUpdateVO completeAdminOrderStartDelivery(AdminOrderStartDeliveryStatusPO param) {
		return orderService.completeAdminOrderStartDelivery(param);
	}

	// 쇼핑몰 마이페이지 배송중 주문상품을 배송완료 처리합니다.
	public ShopOrderDetailStatusUpdateVO completeShopMypageOrderDelivery(ShopOrderDetailStatusUpdatePO param, Long custNo) {
		return orderService.completeShopMypageOrderDelivery(param, custNo);
	}
}
