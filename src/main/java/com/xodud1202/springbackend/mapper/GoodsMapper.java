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
import com.xodud1202.springbackend.domain.shop.category.ShopCategoryGoodsItemVO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartCustomerCouponVO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartItemVO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartSavePO;
import com.xodud1202.springbackend.domain.shop.cart.ShopCartSiteInfoVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsBasicVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsCouponTargetVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsCouponVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsDescItemVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsGroupItemVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsImageVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsSiteInfoVO;
import com.xodud1202.springbackend.domain.shop.goods.ShopGoodsSizeItemVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageCouponUnavailableGoodsVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageDownloadableCouponVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOwnedCouponVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderDetailItemVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderGroupVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderStatusSummaryVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageWishGoodsItemVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderAddressSavePO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderAddressVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderBaseSavePO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderCustomerInfoVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderDetailSavePO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderPaymentSavePO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderPaymentVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderPointBaseVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderPointDetailSavePO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderPointDetailVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderRestoreCartItemVO;
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

	// 카테고리별 상품 목록을 조회합니다.
	List<CategoryGoodsVO> getAdminCategoryGoodsList(@Param("categoryId") String categoryId);

	// 쇼핑몰 카테고리 화면 상품 건수를 조회합니다.
	int countShopCategoryGoods(@Param("categoryId") String categoryId);

	// 쇼핑몰 카테고리 화면 상품 목록을 페이징 조회합니다.
	List<ShopCategoryGoodsItemVO> getShopCategoryGoodsList(
		@Param("categoryId") String categoryId,
		@Param("offset") int offset,
		@Param("pageSize") int pageSize
	);

	// 쇼핑몰 상품상세 기본 상품 정보를 조회합니다.
	ShopGoodsBasicVO getShopGoodsBasic(@Param("goodsId") String goodsId);

	// 쇼핑몰 상품상세 이미지 목록을 조회합니다.
	List<ShopGoodsImageVO> getShopGoodsImageList(@Param("goodsId") String goodsId);

	// 쇼핑몰 상품상세 사이즈 목록을 조회합니다.
	List<ShopGoodsSizeItemVO> getShopGoodsSizeList(@Param("goodsId") String goodsId);

	// 쇼핑몰 상품상세 동일 그룹 상품 목록을 조회합니다.
	List<ShopGoodsGroupItemVO> getShopGoodsGroupItemList(@Param("goodsGroupId") String goodsGroupId);

	// 쇼핑몰 상품상세 기기별 설명 목록을 조회합니다.
	List<ShopGoodsDescItemVO> getShopGoodsDescItemList(@Param("goodsId") String goodsId);

	// 쇼핑몰 상품 위시리스트 등록 여부 건수를 조회합니다.
	int countShopWishList(@Param("custNo") Long custNo, @Param("goodsId") String goodsId);

	// 쇼핑몰 마이페이지 위시리스트 전체 건수를 조회합니다.
	int countShopMypageWishGoods(@Param("custNo") Long custNo);

	// 쇼핑몰 마이페이지 위시리스트 목록을 페이징 조회합니다.
	List<ShopMypageWishGoodsItemVO> getShopMypageWishGoodsList(
		@Param("custNo") Long custNo,
		@Param("offset") int offset,
		@Param("pageSize") int pageSize
	);

	// 쇼핑몰 마이페이지 쿠폰함의 현재 사용 가능한 보유 쿠폰 전체 건수를 조회합니다.
	int countShopMypageOwnedCoupon(@Param("custNo") Long custNo);

	// 쇼핑몰 마이페이지 쿠폰함의 현재 사용 가능한 보유 쿠폰 목록을 페이징 조회합니다.
	List<ShopMypageOwnedCouponVO> getShopMypageOwnedCouponPageList(
		@Param("custNo") Long custNo,
		@Param("offset") int offset,
		@Param("pageSize") int pageSize
	);

	// 쇼핑몰 마이페이지 쿠폰 다운로드 탭의 현재 다운로드 가능 쿠폰 전체 건수를 조회합니다.
	int countShopMypageDownloadableCoupon();

	// 쇼핑몰 마이페이지 쿠폰 다운로드 탭의 현재 다운로드 가능 쿠폰 목록을 페이징 조회합니다.
	List<ShopMypageDownloadableCouponVO> getShopMypageDownloadableCouponPageList(
		@Param("offset") int offset,
		@Param("pageSize") int pageSize
	);

	// 쇼핑몰 마이페이지 쿠폰 다운로드 탭의 현재 다운로드 가능 쿠폰 목록을 조회합니다.
	List<ShopMypageDownloadableCouponVO> getShopMypageDownloadableCouponList();

	// 쇼핑몰 마이페이지 주문내역의 주문번호 기준 전체 건수를 조회합니다.
	int countShopMypageOrderGroup(
		@Param("custNo") Long custNo,
		@Param("startDateTime") String startDateTime,
		@Param("endExclusiveDateTime") String endExclusiveDateTime
	);

	// 쇼핑몰 마이페이지 주문내역의 주문번호 기준 페이지 목록을 조회합니다.
	List<ShopMypageOrderGroupVO> getShopMypageOrderGroupList(
		@Param("custNo") Long custNo,
		@Param("startDateTime") String startDateTime,
		@Param("endExclusiveDateTime") String endExclusiveDateTime,
		@Param("offset") int offset,
		@Param("pageSize") int pageSize
	);

	// 쇼핑몰 마이페이지 주문내역의 주문상세 목록을 주문번호 기준으로 조회합니다.
	List<ShopMypageOrderDetailItemVO> getShopMypageOrderDetailList(@Param("ordNoList") List<String> ordNoList);

	// 쇼핑몰 마이페이지 주문내역의 상태별 주문상세 건수 요약을 조회합니다.
	ShopMypageOrderStatusSummaryVO getShopMypageOrderStatusSummary(
		@Param("custNo") Long custNo,
		@Param("startDateTime") String startDateTime,
		@Param("endExclusiveDateTime") String endExclusiveDateTime
	);

	// 쿠폰 사용 불가 상품 전체 건수를 대상 유형과 값 기준으로 조회합니다.
	int countShopMypageCouponUnavailableGoods(
		@Param("cpnTargetCd") String cpnTargetCd,
		@Param("targetValueList") List<String> targetValueList
	);

	// 쿠폰 사용 불가 상품 목록을 대상 유형과 값 기준으로 제한 건수만큼 조회합니다.
	List<ShopMypageCouponUnavailableGoodsVO> getShopMypageCouponUnavailableGoodsList(
		@Param("cpnTargetCd") String cpnTargetCd,
		@Param("targetValueList") List<String> targetValueList,
		@Param("limit") int limit
	);

	// 쇼핑몰 장바구니 목록을 조회합니다.
	List<ShopCartItemVO> getShopCartItemList(@Param("custNo") Long custNo);

	// 쇼핑몰 주문서 대상 장바구니 목록을 조회합니다.
	List<ShopCartItemVO> getShopOrderCartItemList(@Param("custNo") Long custNo, @Param("cartIdList") List<Long> cartIdList);

	// 쇼핑몰 주문서 배송지 목록을 조회합니다.
	List<ShopOrderAddressVO> getShopOrderAddressList(@Param("custNo") Long custNo);

	// 쇼핑몰 주문서 배송지 별칭 중복 건수를 조회합니다.
	int countShopOrderAddressName(@Param("custNo") Long custNo, @Param("addressNm") String addressNm);

	// 쇼핑몰 주문서 배송지 단건 존재 여부를 조회합니다.
	int countShopOrderAddress(@Param("custNo") Long custNo, @Param("addressNm") String addressNm);

	// 쇼핑몰 주문서 결제 기본 고객 정보를 조회합니다.
	ShopOrderCustomerInfoVO getShopOrderCustomerInfo(@Param("custNo") Long custNo);

	// 쇼핑몰 주문 마스터를 등록합니다.
	int insertShopOrderBase(ShopOrderBaseSavePO param);

	// 쇼핑몰 주문 상세를 등록합니다.
	int insertShopOrderDetail(ShopOrderDetailSavePO param);

	// 쇼핑몰 결제 정보를 등록합니다.
	int insertShopPayment(ShopOrderPaymentSavePO param);

	// 쇼핑몰 결제번호 기준 결제 정보를 조회합니다.
	ShopOrderPaymentVO getShopPaymentByPayNo(@Param("payNo") Long payNo);

	// Toss 결제키 해시 기준 결제 정보를 조회합니다.
	ShopOrderPaymentVO getShopPaymentByTossPaymentKeyHash(@Param("tossPaymentKeyHash") String tossPaymentKeyHash);

	// 주문번호 기준 결제 정보를 조회합니다.
	ShopOrderPaymentVO getShopPaymentByOrdNo(@Param("ordNo") String ordNo);

	// 쇼핑몰 주문 마스터 상태를 변경합니다.
	int updateShopOrderBaseStatus(@Param("ordNo") String ordNo, @Param("ordStatCd") String ordStatCd, @Param("udtNo") Long udtNo);

	// 쇼핑몰 주문 마스터 상태와 결제 시각을 변경합니다.
	int updateShopOrderBaseStatusAndDates(
		@Param("ordNo") String ordNo,
		@Param("ordStatCd") String ordStatCd,
		@Param("orderDt") String orderDt,
		@Param("orderConfirmDt") String orderConfirmDt,
		@Param("udtNo") Long udtNo
	);

	// 쇼핑몰 주문 상세 상태를 일괄 변경합니다.
	int updateShopOrderDetailStatus(@Param("ordNo") String ordNo, @Param("ordDtlStatCd") String ordDtlStatCd, @Param("udtNo") Long udtNo);

	// 카드/계좌이체 승인 성공 결과로 결제 정보를 갱신합니다.
	int updateShopPaymentSuccess(
		@Param("payNo") Long payNo,
		@Param("payStatCd") String payStatCd,
		@Param("aprvAmt") Long aprvAmt,
		@Param("tossPaymentKey") String tossPaymentKey,
		@Param("tossPaymentKeyHash") String tossPaymentKeyHash,
		@Param("tradeNo") String tradeNo,
		@Param("apprNo") String apprNo,
		@Param("rspCode") String rspCode,
		@Param("rspMsg") String rspMsg,
		@Param("cardCd") String cardCd,
		@Param("cardNo") String cardNo,
		@Param("rspRawJson") String rspRawJson,
		@Param("apprDt") String apprDt,
		@Param("udtNo") Long udtNo
	);

	// 무통장입금 발급 성공 결과로 결제 정보를 갱신합니다.
	int updateShopPaymentWaitingDeposit(
		@Param("payNo") Long payNo,
		@Param("payStatCd") String payStatCd,
		@Param("aprvAmt") Long aprvAmt,
		@Param("tossPaymentKey") String tossPaymentKey,
		@Param("tossPaymentKeyHash") String tossPaymentKeyHash,
		@Param("tradeNo") String tradeNo,
		@Param("rspCode") String rspCode,
		@Param("rspMsg") String rspMsg,
		@Param("bankCd") String bankCd,
		@Param("bankNo") String bankNo,
		@Param("vactHolderNm") String vactHolderNm,
		@Param("vactDueDt") String vactDueDt,
		@Param("rspRawJson") String rspRawJson,
		@Param("apprDt") String apprDt,
		@Param("udtNo") Long udtNo
	);

	// 결제 실패/취소 결과로 결제 정보를 갱신합니다.
	int updateShopPaymentFailure(
		@Param("payNo") Long payNo,
		@Param("payStatCd") String payStatCd,
		@Param("rspCode") String rspCode,
		@Param("rspMsg") String rspMsg,
		@Param("rspRawJson") String rspRawJson,
		@Param("udtNo") Long udtNo
	);

	// 웹훅 반영 결과로 결제 상태와 원본 전문을 갱신합니다.
	int updateShopPaymentWebhook(
		@Param("payNo") Long payNo,
		@Param("payStatCd") String payStatCd,
		@Param("rspCode") String rspCode,
		@Param("rspMsg") String rspMsg,
		@Param("webhookRawJson") String webhookRawJson,
		@Param("webhookDt") String webhookDt,
		@Param("udtNo") Long udtNo
	);

	// 선택 고객쿠폰을 사용 처리합니다.
	int updateShopCustomerCouponUse(
		@Param("custNo") Long custNo,
		@Param("custCpnNoList") List<Long> custCpnNoList,
		@Param("ordNo") String ordNo,
		@Param("cpnUseYn") String cpnUseYn,
		@Param("udtNo") Long udtNo
	);

	// 주문번호 기준 사용 처리된 고객쿠폰을 원복합니다.
	int restoreShopCustomerCouponUse(@Param("custNo") Long custNo, @Param("ordNo") String ordNo, @Param("udtNo") Long udtNo);

	// 고객의 현재 사용 가능한 포인트 마스터 목록을 조회합니다.
	List<ShopOrderPointBaseVO> getShopAvailablePointBaseList(@Param("custNo") Long custNo);

	// 포인트 마스터 사용 금액을 차감합니다.
	int updateShopCustomerPointUseAmt(@Param("pntNo") Long pntNo, @Param("useAmt") Integer useAmt, @Param("udtNo") Long udtNo);

	// 포인트 마스터 사용 금액을 복구합니다.
	int restoreShopCustomerPointUseAmt(@Param("pntNo") Long pntNo, @Param("restoreAmt") Integer restoreAmt, @Param("udtNo") Long udtNo);

	// 주문 결제 포인트 상세 이력을 등록합니다.
	int insertShopOrderPointDetail(ShopOrderPointDetailSavePO param);

	// 주문번호 기준 포인트 사용 상세 이력을 조회합니다.
	List<ShopOrderPointDetailVO> getShopOrderPointDetailList(@Param("ordNo") String ordNo);

	// 선택 장바구니 번호 목록을 삭제합니다.
	int deleteShopCartByCartIdList(@Param("custNo") Long custNo, @Param("cartIdList") List<Long> cartIdList);

	// 주문번호 기준 장바구니 복구 대상 목록을 조회합니다.
	List<ShopOrderRestoreCartItemVO> getShopOrderRestoreCartItemList(@Param("ordNo") String ordNo);

	// 상품 사이즈 재고를 주문수량만큼 차감합니다.
	int deductShopGoodsSizeStock(
		@Param("goodsId") String goodsId,
		@Param("sizeId") String sizeId,
		@Param("qty") Integer qty,
		@Param("udtNo") Long udtNo
	);

	// 상품 사이즈 재고를 주문수량만큼 복구합니다.
	int restoreShopGoodsSizeStock(
		@Param("goodsId") String goodsId,
		@Param("sizeId") String sizeId,
		@Param("qty") Integer qty,
		@Param("udtNo") Long udtNo
	);

	// 쇼핑몰 주문서 기본 배송지 여부를 일괄 변경합니다.
	int updateShopOrderAddressDefaultYn(@Param("custNo") Long custNo, @Param("defaultYn") String defaultYn, @Param("udtNo") Long udtNo);

	// 쇼핑몰 주문서 배송지를 등록합니다.
	int insertShopOrderAddress(ShopOrderAddressSavePO param);

	// 쇼핑몰 주문서 배송지를 수정합니다.
	int updateShopOrderAddress(
		@Param("custNo") Long custNo,
		@Param("originAddressNm") String originAddressNm,
		@Param("addressNm") String addressNm,
		@Param("postNo") String postNo,
		@Param("baseAddress") String baseAddress,
		@Param("detailAddress") String detailAddress,
		@Param("phoneNumber") String phoneNumber,
		@Param("rsvNm") String rsvNm,
		@Param("defaultYn") String defaultYn,
		@Param("udtNo") Long udtNo
	);

	// 쇼핑몰 상품 위시리스트를 등록합니다.
	int insertShopWishList(@Param("custNo") Long custNo, @Param("goodsId") String goodsId, @Param("regNo") Long regNo);

	// 쇼핑몰 상품 위시리스트를 삭제합니다.
	int deleteShopWishList(@Param("custNo") Long custNo, @Param("goodsId") String goodsId);

	// 쇼핑몰 장바구니 등록 여부 건수를 조회합니다.
	int countShopCart(@Param("custNo") Long custNo, @Param("goodsId") String goodsId, @Param("sizeId") String sizeId);

	// 쇼핑몰 장바구니를 등록합니다.
	int insertShopCart(ShopCartSavePO param);

	// 쇼핑몰 장바구니 수량을 추가합니다.
	int addShopCartQty(
		@Param("custNo") Long custNo,
		@Param("goodsId") String goodsId,
		@Param("sizeId") String sizeId,
		@Param("qty") Integer qty,
		@Param("udtNo") Long udtNo
	);

	// 쇼핑몰 장바구니 현재 수량을 조회합니다.
	Integer getShopCartQty(@Param("custNo") Long custNo, @Param("goodsId") String goodsId, @Param("sizeId") String sizeId);

	// 쇼핑몰 장바구니의 상품 옵션(사이즈/수량)을 변경합니다.
	int updateShopCartOption(
		@Param("custNo") Long custNo,
		@Param("goodsId") String goodsId,
		@Param("sizeId") String sizeId,
		@Param("targetSizeId") String targetSizeId,
		@Param("targetQty") Integer targetQty,
		@Param("udtNo") Long udtNo
	);

	// 쇼핑몰 장바구니 수량을 수정합니다.
	int updateShopCartQty(
		@Param("custNo") Long custNo,
		@Param("goodsId") String goodsId,
		@Param("sizeId") String sizeId,
		@Param("qty") Integer qty,
		@Param("udtNo") Long udtNo
	);

	// 쇼핑몰 장바구니 단건을 삭제합니다.
	int deleteShopCartItem(@Param("custNo") Long custNo, @Param("goodsId") String goodsId, @Param("sizeId") String sizeId);

	// 쇼핑몰 장바구니 전체를 삭제합니다.
	int deleteShopCartAll(@Param("custNo") Long custNo);

	// 쇼핑몰 장바구니 사이트 배송 기준 정보를 조회합니다.
	ShopCartSiteInfoVO getShopCartSiteInfo(@Param("siteId") String siteId);

	// 쇼핑몰 상품상세 사이트 배송 기준 정보를 조회합니다.
	ShopGoodsSiteInfoVO getShopGoodsSiteInfo(@Param("siteId") String siteId);

	// 고객등급별 포인트 적립률을 조회합니다.
	Integer getShopPointSaveRateByCustGradeCd(@Param("custGradeCd") String custGradeCd);

	// 상품의 카테고리 코드 목록을 조회합니다.
	List<String> getShopGoodsCategoryIdList(@Param("goodsId") String goodsId);

	// 상품의 기획전 탭 번호 목록을 조회합니다.
	List<String> getShopGoodsExhibitionTabNoList(@Param("goodsId") String goodsId);

	// 다운로드 가능한 상품쿠폰 목록을 조회합니다.
	List<ShopGoodsCouponVO> getShopActiveGoodsCouponList();

	// 상품쿠폰 대상(적용/제외) 목록을 조회합니다.
	List<ShopGoodsCouponTargetVO> getShopCouponTargetList(@Param("cpnNo") Long cpnNo);

	// 고객이 현재 사용할 수 있는 보유 쿠폰 목록을 조회합니다.
	List<ShopCartCustomerCouponVO> getShopCustomerCouponList(@Param("custNo") Long custNo);

	// 고객의 현재 사용 가능한 보유 포인트 합계를 조회합니다.
	Integer getShopAvailablePointAmt(@Param("custNo") Long custNo);

	// 카테고리 상품 단건을 등록합니다.
	int insertCategoryGoods(CategoryGoodsSavePO param);

	// 카테고리 상품 정렬 순서를 수정합니다.
	int updateCategoryGoodsDispOrd(CategoryGoodsSavePO param);

	// 카테고리 상품 단건을 삭제합니다.
	int deleteCategoryGoods(CategoryGoodsSavePO param);

	// 상품 기준 카테고리 상품 전체를 삭제합니다.
	int deleteCategoryGoodsByGoodsId(@Param("goodsId") String goodsId);

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
