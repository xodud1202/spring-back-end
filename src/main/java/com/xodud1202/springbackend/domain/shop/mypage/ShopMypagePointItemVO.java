package com.xodud1202.springbackend.domain.shop.mypage;

import lombok.Data;

/**
 * 마이페이지 포인트 내역 아이템 VO.
 * CUSTOMER_POINT_DETAIL 테이블의 각 포인트 적립/차감 이력 행을 표현한다.
 */
@Data
public class ShopMypagePointItemVO {

    /** 포인트번호 (CUSTOMER_POINT_BASE FK) */
    private Long pntNo;

    /**
     * 포인트 금액.
     * 양수: 적립/복구, 음수: 사용/차감
     */
    private Integer pntAmt;

    /** 연관 주문번호 (없으면 null) */
    private String ordNo;

    /** 비고 / 내용 */
    private String bigo;

    /** 발생 일시 (YYYY-MM-DD 형식) */
    private String regDt;
}
