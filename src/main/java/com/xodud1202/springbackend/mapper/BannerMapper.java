package com.xodud1202.springbackend.mapper;

import com.xodud1202.springbackend.domain.admin.banner.BannerDetailVO;
import com.xodud1202.springbackend.domain.admin.banner.BannerGoodsOrderSavePO;
import com.xodud1202.springbackend.domain.admin.banner.BannerGoodsPO;
import com.xodud1202.springbackend.domain.admin.banner.BannerGoodsVO;
import com.xodud1202.springbackend.domain.admin.banner.BannerImageInfoPO;
import com.xodud1202.springbackend.domain.admin.banner.BannerImageOrderSavePO;
import com.xodud1202.springbackend.domain.admin.banner.BannerPO;
import com.xodud1202.springbackend.domain.admin.banner.BannerSavePO;
import com.xodud1202.springbackend.domain.admin.banner.BannerTabPO;
import com.xodud1202.springbackend.domain.admin.banner.BannerVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
// 관리자 배너 관련 매퍼를 정의합니다.
public interface BannerMapper {
	// 관리자 배너 목록을 조회합니다.
	List<BannerVO> getAdminBannerList(BannerPO param);

	// 관리자 배너 목록 건수를 조회합니다.
	int getAdminBannerCount(BannerPO param);

	// 관리자 배너 상세를 조회합니다.
	BannerDetailVO getAdminBannerDetail(@Param("bannerNo") Integer bannerNo);

	// 이미지 배너 정보를 조회합니다.
	List<BannerImageInfoPO> getBannerImageInfoList(@Param("bannerNo") Integer bannerNo);

	// 상품 탭 목록을 조회합니다.
	List<BannerTabPO> getBannerTabList(@Param("bannerNo") Integer bannerNo);

	// 배너 상품 목록을 조회합니다.
	List<BannerGoodsVO> getBannerGoodsList(@Param("bannerNo") Integer bannerNo);

	// 배너 기본 정보를 등록합니다.
	int insertBannerBase(BannerSavePO param);

	// 배너 기본 정보를 수정합니다.
	int updateBannerBase(BannerSavePO param);

	// 배너 존재 여부를 조회합니다.
	int countBannerByNo(@Param("bannerNo") Integer bannerNo);

	// 배너 기본 정보를 삭제 상태로 변경합니다.
	int updateBannerBaseDelete(@Param("bannerNo") Integer bannerNo, @Param("udtNo") Long udtNo);

	// 이미지 배너 정보를 삭제합니다.
	int deleteImageBannerInfoByBannerNo(@Param("bannerNo") Integer bannerNo);

	// 이미지 배너 정보를 등록합니다.
	int insertImageBannerInfo(BannerImageInfoPO param);

	// 배너 탭을 삭제합니다.
	int deleteBannerTabByBannerNo(@Param("bannerNo") Integer bannerNo);

	// 배너 상품을 삭제합니다.
	int deleteBannerGoodsByBannerNo(@Param("bannerNo") Integer bannerNo);

	// 배너 탭을 등록합니다.
	int insertBannerTab(BannerTabPO param);

	// 배너 상품을 등록합니다.
	int insertBannerGoods(BannerGoodsPO param);

	// 배너 상품 정렬 순서를 저장합니다.
	int updateBannerGoodsOrder(BannerGoodsOrderSavePO param);

	// 이미지 배너 정렬 순서를 저장합니다.
	int updateBannerImageOrder(BannerImageOrderSavePO param);
}
