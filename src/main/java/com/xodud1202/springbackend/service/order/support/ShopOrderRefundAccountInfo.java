package com.xodud1202.springbackend.service.order.support;

/**
 * @param refundBankCd   환불 은행코드입니다.
 * @param refundBankNo   환불 계좌번호입니다.
 * @param refundHolderNm 환불 예금주명입니다.
 */ // 쇼핑몰 무통장입금 환불계좌 정규화 결과를 전달합니다.
public record ShopOrderRefundAccountInfo(
	String refundBankCd,
	String refundBankNo,
	String refundHolderNm
) {
	// 쇼핑몰 무통장입금 환불계좌 정규화 결과를 생성합니다.
	public ShopOrderRefundAccountInfo {
	}

	// 환불 은행코드를 반환합니다.
	public String getRefundBankCd() {
		return refundBankCd;
	}

	// 환불 계좌번호를 반환합니다.
	public String getRefundBankNo() {
		return refundBankNo;
	}

	// 환불 예금주명을 반환합니다.
	public String getRefundHolderNm() {
		return refundHolderNm;
	}
}
