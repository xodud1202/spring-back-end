package com.xodud1202.springbackend.mapper;

import com.xodud1202.springbackend.domain.admin.exhibition.ExhibitionDetailVO;
import com.xodud1202.springbackend.domain.admin.exhibition.ExhibitionGoodsPO;
import com.xodud1202.springbackend.domain.admin.exhibition.ExhibitionGoodsVO;
import com.xodud1202.springbackend.domain.admin.exhibition.ExhibitionPO;
import com.xodud1202.springbackend.domain.admin.exhibition.ExhibitionSavePO;
import com.xodud1202.springbackend.domain.admin.exhibition.ExhibitionTabPO;
import com.xodud1202.springbackend.domain.admin.exhibition.ExhibitionVO;
import com.xodud1202.springbackend.domain.shop.exhibition.ShopExhibitionItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
// 기획전 관리 매퍼를 정의합니다.
public interface ExhibitionMapper {
	// 기획전 목록을 조회합니다.
	List<ExhibitionVO> getAdminExhibitionList(ExhibitionPO param);

	// 기획전 목록 건수를 조회합니다.
	int getAdminExhibitionCount(ExhibitionPO param);

	// 기획전 상세를 조회합니다.
	ExhibitionDetailVO getAdminExhibitionDetail(@Param("exhibitionNo") Integer exhibitionNo);

	// 기획전 탭 목록을 조회합니다.
	List<ExhibitionTabPO> getExhibitionTabList(@Param("exhibitionNo") Integer exhibitionNo);

	// 기획전 상품 목록을 조회합니다.
	List<ExhibitionGoodsVO> getExhibitionGoodsList(@Param("exhibitionNo") Integer exhibitionNo, @Param("exhibitionTabNo") Integer exhibitionTabNo);

	// 기획전 기본 정보를 등록합니다.
	int insertExhibitionBase(ExhibitionSavePO param);

	// 기획전 기본 정보를 수정합니다.
	int updateExhibitionBase(ExhibitionSavePO param);

	// 기획전 썸네일 URL을 수정합니다.
	int updateExhibitionThumbnail(@Param("exhibitionNo") Integer exhibitionNo, @Param("thumbnailUrl") String thumbnailUrl);

	// 기획전 존재 여부를 조회합니다.
	int countExhibitionByNo(@Param("exhibitionNo") Integer exhibitionNo);

	// 기획전 탭 개수를 조회합니다.
	int countExhibitionTabByNo(@Param("exhibitionNo") Integer exhibitionNo);

	// 기획전 삭제 상태로 변경합니다.
	int updateExhibitionBaseDelete(@Param("exhibitionNo") Integer exhibitionNo, @Param("udtNo") Long udtNo);

	// 기존 탭을 삭제합니다.
	int deleteExhibitionTabByExhibitionNo(@Param("exhibitionNo") Integer exhibitionNo);

	// 탭 번호로 탭을 삭제합니다.
	int deleteExhibitionTabByTabNo(@Param("exhibitionTabNo") Integer exhibitionTabNo);

	// 기존 탭 상품을 삭제합니다.
	int deleteExhibitionGoodsByExhibitionNo(@Param("exhibitionNo") Integer exhibitionNo);

	// 탭 번호 목록의 상품 개수를 조회합니다.
	int countExhibitionGoodsByTabNoList(@Param("tabNoList") List<Integer> tabNoList);

	// 탭 번호 목록의 상품을 삭제합니다.
	int deleteExhibitionGoodsByTabNoList(@Param("tabNoList") List<Integer> tabNoList);

	// 기획전 탭을 등록합니다.
	int insertExhibitionTab(ExhibitionTabPO param);

	// 기획전 탭을 수정합니다.
	int updateExhibitionTab(ExhibitionTabPO param);

	// 기획전 탭 상품을 등록합니다.
	int insertExhibitionGoods(ExhibitionGoodsPO param);

	// 쇼핑몰 기획전 목록 건수를 조회합니다.
	int countShopVisibleExhibitionList();

	// 쇼핑몰 기획전 목록을 페이징 조회합니다.
	List<ShopExhibitionItemVO> getShopVisibleExhibitionList(@Param("offset") int offset, @Param("pageSize") int pageSize);
}
