package com.xodud1202.springbackend.mapper;

import com.xodud1202.springbackend.domain.shop.site.ShopSiteInfoVO;
import org.apache.ibatis.annotations.Param;

// 쇼핑몰 사이트 기본 정보 MyBatis 매퍼를 정의합니다.
public interface SiteInfoMapper {
	// 사이트 아이디로 사이트 통합 정보를 조회합니다.
	ShopSiteInfoVO getShopSiteInfo(@Param("siteId") String siteId);
}
