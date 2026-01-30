package com.xodud1202.springbackend.mapper;

import com.xodud1202.springbackend.domain.admin.goods.GoodsPO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsDetailVO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsMerchVO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsSavePO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsSizeSavePO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsSizeVO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GoodsMapper {
	// 관리자 상품 목록을 조회합니다.
	List<GoodsVO> getAdminGoodsList(GoodsPO param);

	// 관리자 상품 목록 건수를 조회합니다.
	int getAdminGoodsCount(GoodsPO param);

	// 상품 분류 목록을 조회합니다.
	List<GoodsMerchVO> getGoodsMerchList();

	// 관리자 상품 상세 정보를 조회합니다.
	GoodsDetailVO getAdminGoodsDetail(@Param("goodsId") String goodsId);

	// 관리자 상품을 등록합니다.
	int insertAdminGoods(GoodsSavePO param);

	// 관리자 상품을 수정합니다.
	int updateAdminGoods(GoodsSavePO param);

	// 관리자 상품 사이즈 목록을 조회합니다.
	List<GoodsSizeVO> getAdminGoodsSizeList(@Param("goodsId") String goodsId);

	// 관리자 상품 사이즈 단건을 조회합니다.
	GoodsSizeVO getAdminGoodsSizeDetail(@Param("goodsId") String goodsId, @Param("sizeId") String sizeId);

	// 관리자 상품 사이즈를 등록합니다.
	int insertAdminGoodsSize(GoodsSizeSavePO param);

	// 관리자 상품 사이즈를 수정합니다.
	int updateAdminGoodsSize(GoodsSizeSavePO param);

	// 관리자 상품 사이즈를 삭제 처리합니다.
	int deleteAdminGoodsSize(GoodsSizeSavePO param);
}
