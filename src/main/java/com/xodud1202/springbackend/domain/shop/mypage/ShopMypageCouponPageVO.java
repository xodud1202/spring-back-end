package com.xodud1202.springbackend.domain.shop.mypage;

import lombok.Data;

import java.util.List;

@Data
// 쇼핑몰 마이페이지 쿠폰함 페이지 응답을 전달합니다.
public class ShopMypageCouponPageVO {
	// 고객이 현재 사용할 수 있는 보유 쿠폰 목록입니다.
	private List<ShopMypageOwnedCouponVO> ownedCouponList;
	// 사용 가능 보유 쿠폰 전체 건수입니다.
	private Integer ownedCouponCount;
	// 보유 쿠폰 현재 페이지 번호입니다.
	private Integer ownedPageNo;
	// 보유 쿠폰 페이지 크기입니다.
	private Integer ownedPageSize;
	// 보유 쿠폰 전체 페이지 수입니다.
	private Integer ownedTotalPageCount;
	// 현재 다운로드 가능한 쿠폰 목록입니다.
	private List<ShopMypageDownloadableCouponVO> downloadableCouponList;
	// 다운로드 가능 쿠폰 전체 건수입니다.
	private Integer downloadableCouponCount;
	// 다운로드 가능 쿠폰 현재 페이지 번호입니다.
	private Integer downloadablePageNo;
	// 다운로드 가능 쿠폰 페이지 크기입니다.
	private Integer downloadablePageSize;
	// 다운로드 가능 쿠폰 전체 페이지 수입니다.
	private Integer downloadableTotalPageCount;
}
