package com.xodud1202.springbackend.mapper;

import com.xodud1202.springbackend.domain.shop.auth.ShopCustomerSessionVO;
import org.apache.ibatis.annotations.Param;

// 쇼핑몰 고객 로그인 관련 MyBatis 매퍼를 정의합니다.
public interface ShopAuthMapper {
	// CI 값으로 기존 고객 로그인 정보를 조회합니다.
	ShopCustomerSessionVO getShopCustomerByCi(@Param("ci") String ci);
}
