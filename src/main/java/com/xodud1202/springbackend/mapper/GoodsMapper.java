package com.xodud1202.springbackend.mapper;

import com.xodud1202.springbackend.domain.admin.brand.BrandVO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsPO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsDetailVO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsMerchVO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsSavePO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsSizeOrderSavePO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsSizeSavePO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsSizeVO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsVO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsCategorySaveItem;
import com.xodud1202.springbackend.domain.admin.goods.GoodsCategorySavePO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsCategoryVO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsDescSaveItem;
import com.xodud1202.springbackend.domain.admin.goods.GoodsDescVO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsImageSavePO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsImageVO;
import com.xodud1202.springbackend.domain.admin.goods.GoodsImageOrderSavePO;
import com.xodud1202.springbackend.domain.admin.category.CategorySavePO;
import com.xodud1202.springbackend.domain.admin.category.CategoryGoodsSavePO;
import com.xodud1202.springbackend.domain.admin.category.CategoryGoodsVO;
import com.xodud1202.springbackend.domain.admin.category.CategoryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
// 관리자 상품 관련 매퍼를 정의합니다.
public interface GoodsMapper {
	// 관리자 상품 목록을 조회합니다.
	List<GoodsVO> getAdminGoodsList(GoodsPO param);

	// 관리자 상품 목록 건수를 조회합니다.
	int getAdminGoodsCount(GoodsPO param);

	// 상품 분류 목록을 조회합니다.
	List<GoodsMerchVO> getGoodsMerchList();

	// 브랜드 목록을 조회합니다.
	List<BrandVO> getBrandList();

	// 관리자 상품 상세 정보를 조회합니다.
	GoodsDetailVO getAdminGoodsDetail(@Param("goodsId") String goodsId);

	// 관리자 상품을 등록합니다.
	int insertAdminGoods(GoodsSavePO param);

	// 관리자 상품을 수정합니다.
	int updateAdminGoods(GoodsSavePO param);

	// 카테고리 목록을 조회합니다.
	List<CategoryVO> getCategoryList(@Param("categoryLevel") Integer categoryLevel, @Param("parentCategoryId") String parentCategoryId);

	// 관리자 카테고리 트리 목록을 조회합니다.
	List<CategoryVO> getAdminCategoryTreeList();

	// 관리자 카테고리 상세를 조회합니다.
	CategoryVO getAdminCategoryDetail(@Param("categoryId") String categoryId);

	// 관리자 카테고리 최대 정렬 순서를 조회합니다.
	Integer getAdminCategoryMaxDispOrd(@Param("parentCategoryId") String parentCategoryId);

	// 관리자 카테고리 최대 코드값을 조회합니다.
	String getAdminCategoryMaxId(@Param("parentCategoryId") String parentCategoryId);

	// 관리자 카테고리를 등록합니다.
	int insertAdminCategory(CategorySavePO param);

	// 관리자 카테고리를 수정합니다.
	int updateAdminCategory(CategorySavePO param);

	// 관리자 카테고리를 삭제 처리합니다.
	int deleteAdminCategory(CategorySavePO param);

	// 관리자 카테고리 하위 건수를 조회합니다.
	int countAdminCategoryChildren(@Param("categoryId") String categoryId);

	// 관리자 카테고리 중복 여부를 조회합니다.
	int countAdminCategoryById(@Param("categoryId") String categoryId);

	// 관리자 카테고리 사용 여부를 조회합니다.
	int countAdminCategoryGoods(@Param("categoryId") String categoryId);

	// 관리자 상품 카테고리 목록을 조회합니다.
	List<GoodsCategoryVO> getAdminGoodsCategoryList(@Param("goodsId") String goodsId);

	// 카테고리 하위 건수를 조회합니다.
	int countCategoryChildren(@Param("categoryId") String categoryId);

	// 관리자 상품 카테고리를 단건 삭제합니다.
	int deleteAdminGoodsCategory(GoodsCategorySavePO param);

	// 관리자 상품 카테고리를 삭제합니다.
	int deleteAdminGoodsCategoryByGoodsId(@Param("goodsId") String goodsId);

	// 관리자 상품 카테고리를 등록합니다.
	int insertAdminGoodsCategoryList(List<GoodsCategorySaveItem> list);

	// 관리자 상품 카테고리를 단건 등록합니다.
	int insertAdminGoodsCategory(GoodsCategorySavePO param);

	// 관리자 상품 카테고리를 단건 수정합니다.
	int updateAdminGoodsCategory(GoodsCategorySavePO param);

	// 관리자 상품 카테고리 건수를 조회합니다.
	int countAdminGoodsCategory(@Param("goodsId") String goodsId, @Param("categoryId") String categoryId);

	// 카테고리별 상품 목록을 조회합니다.
	List<CategoryGoodsVO> getAdminCategoryGoodsList(@Param("categoryId") String categoryId);

	// 카테고리 상품 단건을 등록합니다.
	int insertCategoryGoods(CategoryGoodsSavePO param);

	// 카테고리 상품 정렬 순서를 수정합니다.
	int updateCategoryGoodsDispOrd(CategoryGoodsSavePO param);

	// 카테고리 상품 단건을 삭제합니다.
	int deleteCategoryGoods(CategoryGoodsSavePO param);

	// 카테고리 상품 존재 여부를 조회합니다.
	int countCategoryGoods(@Param("categoryId") String categoryId, @Param("goodsId") String goodsId);

	// 하위 카테고리 상품 존재 여부를 조회합니다.
	int countCategoryGoodsInChildren(@Param("parentCategoryId") String parentCategoryId, @Param("goodsId") String goodsId);

	// 카테고리 하위 목록을 조회합니다.
	List<CategoryVO> getCategoryChildren(@Param("parentCategoryId") String parentCategoryId);

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

	// 관리자 상품 사이즈 순서를 저장합니다.
	int updateAdminGoodsSizeOrder(GoodsSizeOrderSavePO param);

	// 관리자 상품 이미지 목록을 조회합니다.
	List<GoodsImageVO> getAdminGoodsImageList(@Param("goodsId") String goodsId);

	// 관리자 상품 이미지 단건을 조회합니다.
	GoodsImageVO getAdminGoodsImageByNo(@Param("imgNo") Integer imgNo);

	// 관리자 상품 이미지 정렬 순서 최대값을 조회합니다.
	int getAdminGoodsImageMaxDispOrd(@Param("goodsId") String goodsId);

	// 관리자 상품 이미지를 등록합니다.
	int insertAdminGoodsImage(GoodsImageSavePO param);

	// 관리자 상품 이미지를 삭제합니다.
	int deleteAdminGoodsImage(@Param("imgNo") Integer imgNo);

	// 관리자 상품 이미지 순서를 저장합니다.
	int updateAdminGoodsImageOrder(GoodsImageOrderSavePO param);

	// 관리자 상품 상세 설명 목록을 조회합니다.
	List<GoodsDescVO> getAdminGoodsDescList(@Param("goodsId") String goodsId);

	// 관리자 상품 상세 설명 건수를 조회합니다.
	int countAdminGoodsDesc(@Param("goodsId") String goodsId, @Param("deviceGbCd") String deviceGbCd);

	// 관리자 상품 상세 설명을 등록합니다.
	int insertAdminGoodsDesc(GoodsDescSaveItem param);

	// 관리자 상품 상세 설명을 수정합니다.
	int updateAdminGoodsDesc(GoodsDescSaveItem param);
}
