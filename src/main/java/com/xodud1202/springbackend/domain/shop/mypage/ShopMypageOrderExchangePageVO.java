package com.xodud1202.springbackend.domain.shop.mypage;

import com.xodud1202.springbackend.domain.shop.cart.ShopCartSiteInfoVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderAddressVO;
import lombok.Data;

import java.util.List;

@Data
// 쇼핑몰 마이페이지 교환 신청 화면 데이터를 전달합니다.
public class ShopMypageOrderExchangePageVO {
	// 주문 정보입니다.
	private ShopMypageOrderGroupVO order;
	// 교환 사유 코드 목록입니다.
	private List<ShopMypageOrderCancelReasonVO> reasonList;
	// 배송비 기준 사이트 정보입니다.
	private ShopCartSiteInfoVO siteInfo;
	// 주문상품별 교환 가능 사이즈 옵션 목록입니다.
	private List<ShopMypageOrderExchangeSizeOptionVO> sizeOptionList;
	// 고객 배송지 목록입니다.
	private List<ShopOrderAddressVO> addressList;
	// 기본 회수지입니다.
	private ShopOrderAddressVO pickupAddress;
	// 기본 교환 배송지입니다.
	private ShopOrderAddressVO deliveryAddress;
	// 고객 연락처입니다.
	private String customerPhoneNumber;
}
