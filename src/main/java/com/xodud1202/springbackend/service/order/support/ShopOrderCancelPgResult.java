package com.xodud1202.springbackend.service.order.support;

/**
 * @param rawResponse    PG 원본 응답 문자열입니다.
 * @param rspCode        PG 응답 코드입니다.
 * @param rspMsg         PG 응답 메시지입니다.
 * @param tradeNo        PG 거래키입니다.
 * @param approvedDt     취소 완료 일시입니다.
 * @param canceledAmount 실제 취소 금액입니다.
 */ // PG 취소 성공 응답에서 필요한 요약값을 전달합니다.
public record ShopOrderCancelPgResult(
	String rawResponse,
	String rspCode,
	String rspMsg,
	String tradeNo,
	String approvedDt,
	long canceledAmount
) {
	// PG 취소 성공 응답 요약값을 생성합니다.
	public ShopOrderCancelPgResult {
	}
}
