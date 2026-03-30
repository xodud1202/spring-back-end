package com.xodud1202.springbackend.service;

// Toss 가상계좌 취소 시 전달할 환불 수취 계좌 정보를 정의합니다.
public record TossPaymentRefundReceiveAccount(
	String bank,
	String accountNumber,
	String holderName
) {
}
