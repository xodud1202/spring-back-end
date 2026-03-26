package com.xodud1202.springbackend.domain.shop.mypage;

import lombok.Data;

import java.util.List;

/**
 * 마이페이지 포인트 내역 페이지 응답 VO.
 * 사용 가능 포인트 요약 정보와 포인트 내역 목록(페이징)을 포함한다.
 */
@Data
public class ShopMypagePointPageVO {

    /** 현재 사용 가능한 포인트 합계 */
    private Integer availablePointAmt;

    /** 7일 이내 만료 예정 포인트 합계 */
    private Integer expiringPointAmt;

    /** 포인트 내역 목록 */
    private List<ShopMypagePointItemVO> pointList;

    /** 전체 포인트 내역 건수 */
    private Integer pointCount;

    /** 현재 페이지 번호 */
    private Integer pageNo;

    /** 페이지 크기 */
    private Integer pageSize;

    /** 전체 페이지 수 */
    private Integer totalPageCount;
}
