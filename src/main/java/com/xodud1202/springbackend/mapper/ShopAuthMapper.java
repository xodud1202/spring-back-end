package com.xodud1202.springbackend.mapper;

import com.xodud1202.springbackend.domain.shop.auth.ShopCouponIssueRuleVO;
import com.xodud1202.springbackend.domain.shop.auth.ShopCustomerCouponSavePO;
import com.xodud1202.springbackend.domain.shop.auth.ShopCustomerGradeBenefitVO;
import com.xodud1202.springbackend.domain.shop.auth.ShopCustomerPointDetailSavePO;
import com.xodud1202.springbackend.domain.shop.auth.ShopCustomerPointSavePO;
import com.xodud1202.springbackend.domain.shop.auth.ShopCustomerSessionVO;
import com.xodud1202.springbackend.domain.shop.auth.ShopGoogleJoinSavePO;
import org.apache.ibatis.annotations.Param;

// 쇼핑몰 고객 로그인 관련 MyBatis 매퍼를 정의합니다.
public interface ShopAuthMapper {
	// CI 값으로 기존 고객 로그인 정보를 조회합니다.
	ShopCustomerSessionVO getShopCustomerByCi(@Param("ci") String ci);

	// 공통코드 코드값으로 코드명을 조회합니다.
	String getCommonCodeName(@Param("grpCd") String grpCd, @Param("cd") String cd);

	// 구글 신규 회원 정보를 CUSTOMER_BASE에 등록합니다.
	int insertShopGoogleCustomer(ShopGoogleJoinSavePO param);

	// 구글 신규 회원 등록 후 REG_NO/UDT_NO를 고객 번호로 갱신합니다.
	int updateShopGoogleCustomerAuditNo(@Param("custNo") Long custNo, @Param("auditNo") Long auditNo);

	// 사이트 기본 가입 포인트 값을 조회합니다.
	Integer getShopJoinPoint(@Param("siteId") String siteId);

	// 고객등급별 쿠폰 혜택 정보를 조회합니다.
	ShopCustomerGradeBenefitVO getCustomerGradeBenefitByCustGradeCd(@Param("custGradeCd") String custGradeCd);

	// 고객 가입 포인트 마스터 이력을 등록합니다.
	int insertCustomerPointBase(ShopCustomerPointSavePO param);

	// 고객 가입 포인트 상세 이력을 등록합니다.
	int insertCustomerPointDetail(ShopCustomerPointDetailSavePO param);

	// 발급 가능한 정상 상태 쿠폰의 사용기간 규칙 정보를 조회합니다.
	ShopCouponIssueRuleVO getIssuableCouponIssueRule(@Param("cpnNo") Long cpnNo);

	// 고객 쿠폰을 1건 지급 등록합니다.
	int insertCustomerCoupon(ShopCustomerCouponSavePO param);
}
