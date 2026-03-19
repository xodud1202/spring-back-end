package com.xodud1202.springbackend.domain.shop.order;

import lombok.Data;

@Data
// 쇼핑몰 주문서 주소 검색 결과 단건을 전달합니다.
public class ShopOrderAddressSearchItemVO {
	// 전체 도로명 주소입니다.
	private String roadAddr;
	// 기본 도로명 주소입니다.
	private String roadAddrPart1;
	// 참고항목 주소입니다.
	private String roadAddrPart2;
	// 지번 주소입니다.
	private String jibunAddr;
	// 우편번호입니다.
	private String zipNo;
	// 행정구역 코드입니다.
	private String admCd;
	// 도로명 코드입니다.
	private String rnMgtSn;
	// 건물관리번호입니다.
	private String bdMgtSn;
}
