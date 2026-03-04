package com.xodud1202.springbackend.mapper;

import com.xodud1202.springbackend.domain.admin.coupon.CouponPO;
import com.xodud1202.springbackend.domain.admin.coupon.CouponVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
// 관리자 쿠폰 목록 매퍼를 정의합니다.
public interface CouponMapper {
	// 관리자 쿠폰 목록을 조회합니다.
	List<CouponVO> getAdminCouponList(CouponPO param);

	// 관리자 쿠폰 목록 건수를 조회합니다.
	int getAdminCouponCount(CouponPO param);
}
