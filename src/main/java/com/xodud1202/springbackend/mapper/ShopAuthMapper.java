package com.xodud1202.springbackend.mapper;

import com.xodud1202.springbackend.domain.shop.auth.ShopCustomerSessionVO;
import com.xodud1202.springbackend.domain.shop.auth.ShopGoogleJoinSavePO;
import org.apache.ibatis.annotations.Param;

// 쇼핑몰 고객 로그인 관련 MyBatis 매퍼를 정의합니다.
public interface ShopAuthMapper {
	// CI 값으로 기존 고객 로그인 정보를 조회합니다.
	ShopCustomerSessionVO getShopCustomerByCi(@Param("ci") String ci);

	// 구글 신규 회원 정보를 CUSTOMER_BASE에 등록합니다.
	int insertShopGoogleCustomer(ShopGoogleJoinSavePO param);

	// 구글 신규 회원 등록 후 REG_NO/UDT_NO를 고객 번호로 갱신합니다.
	int updateShopGoogleCustomerAuditNo(@Param("custNo") Long custNo, @Param("auditNo") Long auditNo);
}
