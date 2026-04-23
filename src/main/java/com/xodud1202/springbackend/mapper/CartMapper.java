package com.xodud1202.springbackend.mapper;

import com.xodud1202.springbackend.domain.shop.cart.ShopCartItemVO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartSavePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
// 장바구니 도메인 전용 MyBatis 매퍼를 정의합니다.
public interface CartMapper {
	// 쇼핑몰 장바구니 목록을 조회합니다.
	List<ShopCartItemVO> getShopCartItemList(@Param("custNo") Long custNo);

	// 쇼핑몰 장바구니 단건을 장바구니번호 기준으로 조회합니다.
	ShopCartItemVO getShopCartItem(@Param("custNo") Long custNo, @Param("cartId") Long cartId);

	// 쇼핑몰 장바구니의 상품/사이즈 기준 기존 등록 건수를 조회합니다.
	int countShopCartByGoodsIdAndSizeId(
		@Param("custNo") Long custNo,
		@Param("goodsId") String goodsId,
		@Param("sizeId") String sizeId
	);

	// 쇼핑몰 장바구니의 상품/사이즈 기준 장바구니번호를 조회합니다.
	Long getShopCartIdByGoodsIdAndSizeId(
		@Param("custNo") Long custNo,
		@Param("goodsId") String goodsId,
		@Param("sizeId") String sizeId
	);

	// 쇼핑몰 장바구니의 동일 상품/사이즈 중복 행 목록을 조회합니다.
	List<ShopCartItemVO> getShopCartDuplicateItemList(
		@Param("custNo") Long custNo,
		@Param("goodsId") String goodsId,
		@Param("sizeId") String sizeId
	);

	// 동일 고객의 일반 장바구니 쓰기를 직렬화하기 위해 범위 잠금을 획득합니다.
	List<Long> lockShopCartForUpdate(@Param("custNo") Long custNo);

	// 쇼핑몰 장바구니를 등록합니다.
	int insertShopCart(ShopCartSavePO param);

	// 쇼핑몰 장바구니의 동일 상품/사이즈 수량을 추가합니다.
	int addShopCartQtyByGoodsIdAndSizeId(
		@Param("custNo") Long custNo,
		@Param("goodsId") String goodsId,
		@Param("sizeId") String sizeId,
		@Param("qty") Integer qty,
		@Param("exhibitionNo") Integer exhibitionNo,
		@Param("udtNo") Long udtNo
	);

	// 쇼핑몰 장바구니의 현재 수량을 상품/사이즈 기준으로 조회합니다.
	Integer getShopCartQtyByGoodsIdAndSizeId(
		@Param("custNo") Long custNo,
		@Param("goodsId") String goodsId,
		@Param("sizeId") String sizeId
	);

	// 쇼핑몰 장바구니 수량을 장바구니번호 기준으로 수정합니다.
	int updateShopCartQtyByCartId(
		@Param("custNo") Long custNo,
		@Param("cartId") Long cartId,
		@Param("qty") Integer qty,
		@Param("udtNo") Long udtNo
	);

	// 쇼핑몰 장바구니 수량을 장바구니번호 기준으로 원자적으로 증가시키고 기획전 번호를 갱신합니다.
	int addShopCartQtyAndExhibitionByCartId(
		@Param("custNo") Long custNo,
		@Param("cartId") Long cartId,
		@Param("qty") Integer qty,
		@Param("exhibitionNo") Integer exhibitionNo,
		@Param("udtNo") Long udtNo
	);

	// 쇼핑몰 장바구니의 사이즈/수량을 장바구니번호 기준으로 변경합니다.
	int updateShopCartOptionByCartId(
		@Param("custNo") Long custNo,
		@Param("cartId") Long cartId,
		@Param("targetSizeId") String targetSizeId,
		@Param("targetQty") Integer targetQty,
		@Param("udtNo") Long udtNo
	);

	// 쇼핑몰 장바구니를 장바구니번호 기준으로 삭제합니다.
	int deleteShopCartByCartId(@Param("custNo") Long custNo, @Param("cartId") Long cartId);

	// 쇼핑몰 장바구니를 상품/사이즈 기준으로 삭제합니다.
	int deleteShopCartByGoodsIdAndSizeId(
		@Param("custNo") Long custNo,
		@Param("goodsId") String goodsId,
		@Param("sizeId") String sizeId
	);

	// 쇼핑몰 장바구니 전체를 삭제합니다.
	int deleteShopCartAll(@Param("custNo") Long custNo);

	// 쇼핑몰 주문서 대상 장바구니 목록을 조회합니다.
	List<ShopCartItemVO> getShopOrderCartItemList(@Param("custNo") Long custNo, @Param("cartIdList") List<Long> cartIdList);

	// 선택 장바구니 번호 목록을 삭제합니다.
	int deleteShopCartByCartIdList(@Param("custNo") Long custNo, @Param("cartIdList") List<Long> cartIdList);
}
