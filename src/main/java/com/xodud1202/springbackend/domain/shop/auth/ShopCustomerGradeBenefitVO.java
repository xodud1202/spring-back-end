package com.xodud1202.springbackend.domain.shop.auth;

// 고객등급별 가입 혜택 정보를 전달합니다.
public record ShopCustomerGradeBenefitVO(
	String custGradeCd,
	Long goodsCpnNo,
	Integer goodsCpnCnt,
	Long cartCpnNo,
	Integer cartCpnCnt,
	Long deliveryCpnNo,
	Integer deliveryCpnCnt
) {
}
