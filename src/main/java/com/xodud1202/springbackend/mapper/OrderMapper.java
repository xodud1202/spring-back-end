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
import com.xodud1202.springbackend.domain.admin.order.AdminOrderClaimRowVO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderDetailRowVO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderListRowVO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderMasterVO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderPO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderPaymentRowVO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderReturnManageClaimSummaryVO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderReturnManageListRowVO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderReturnManagePO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderReturnManagePickupItemPO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderStartDeliveryKeyItemPO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderStartDeliveryListRowVO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderStartDeliveryPO;
import com.xodud1202.springbackend.domain.admin.order.AdminOrderStartDeliveryPrepareItemPO;
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
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderCancelReasonVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOwnedCouponVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderAmountSummaryVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderDetailItemVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderGroupVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderReturnFeeContextVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageOrderStatusSummaryVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageWishGoodsItemVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageCancelHistoryVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageCancelHistoryDetailVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageReturnPickupAddressVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageReturnHistoryVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypageReturnHistoryDetailVO;
import com.xodud1202.springbackend.domain.shop.mypage.ShopMypagePointItemVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderAddressSavePO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderAddressVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderBaseSavePO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderCancelOrderBaseVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderChangeBaseSavePO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderChangeExchangeAddressSavePO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderChangeDetailSavePO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderCustomerInfoVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderDetailSavePO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderPaymentSavePO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderPaymentVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderPointBaseVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderPointDetailSavePO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderPointDetailVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderRestoreCartItemVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderReturnWithdrawResultVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
// 주문 도메인 매퍼를 정의합니다.
public interface OrderMapper {

	// 관리자 주문 목록을 조회합니다.
	List<AdminOrderListRowVO> getAdminOrderList(AdminOrderPO param);

	// 관리자 주문 목록 건수를 조회합니다.
	int getAdminOrderCount(AdminOrderPO param);

	// 관리자 배송 시작 관리 목록을 조회합니다.
	List<AdminOrderStartDeliveryListRowVO> getAdminOrderStartDeliveryList(AdminOrderStartDeliveryPO param);

	// 관리자 배송 시작 관리 목록 건수를 조회합니다.
	int getAdminOrderStartDeliveryCount(AdminOrderStartDeliveryPO param);

	// 관리자 반품 회수 관리 목록을 조회합니다.
	List<AdminOrderReturnManageListRowVO> getAdminOrderReturnManageList(AdminOrderReturnManagePO param);

	// 관리자 반품 회수 관리 목록 건수를 조회합니다.
	int getAdminOrderReturnManageCount(AdminOrderReturnManagePO param);

	// 관리자 반품 회수 관리 대상 클레임 요약을 조회합니다.
	List<AdminOrderReturnManageClaimSummaryVO> getAdminOrderReturnManageClaimSummaryList(@Param("clmNoList") List<String> clmNoList);

	// 관리자 주문 상세 하단 결제 목록을 조회합니다.
	List<AdminOrderPaymentRowVO> getAdminOrderPaymentList(@Param("ordNo") String ordNo);

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

	// 쇼핑몰 마이페이지 주문상세의 주문번호 1건을 조회합니다.
	ShopMypageOrderGroupVO getShopMypageOrderGroup(@Param("custNo") Long custNo, @Param("ordNo") String ordNo);

	// 쇼핑몰 마이페이지 주문내역의 주문상세 목록을 주문번호 기준으로 조회합니다.
	List<ShopMypageOrderDetailItemVO> getShopMypageOrderDetailList(@Param("ordNoList") List<String> ordNoList);

	// 쇼핑몰 마이페이지 주문취소 사유 코드 목록을 조회합니다.
	List<ShopMypageOrderCancelReasonVO> getShopMypageOrderCancelReasonList();

	// 쇼핑몰 마이페이지 반품 사유 코드 목록을 조회합니다.
	List<ShopMypageOrderCancelReasonVO> getShopMypageOrderReturnReasonList();

	// 쇼핑몰 마이페이지 반품 배송비 계산 이력 컨텍스트를 조회합니다.
	ShopMypageOrderReturnFeeContextVO getShopMypageOrderReturnFeeContext(@Param("ordNo") String ordNo);

	// 주문번호로 고객번호를 조회합니다.
	Long getOrderCustNo(@Param("ordNo") String ordNo);

	// 쇼핑몰 마이페이지 주문내역의 상태별 주문상세 건수 요약을 조회합니다.
	ShopMypageOrderStatusSummaryVO getShopMypageOrderStatusSummary(
		@Param("custNo") Long custNo,
		@Param("startDateTime") String startDateTime,
		@Param("endExclusiveDateTime") String endExclusiveDateTime
	);

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

	// 주문취소 대상 원결제 정보를 조회합니다.
	ShopOrderPaymentVO getShopOrderPaymentForCancel(@Param("ordNo") String ordNo);

	// 주문취소 처리용 주문 마스터 정보를 조회합니다.
	ShopOrderCancelOrderBaseVO getShopOrderCancelOrderBase(@Param("custNo") Long custNo, @Param("ordNo") String ordNo);

	// 주문변경 마스터를 등록합니다.
	int insertShopOrderChangeBase(ShopOrderChangeBaseSavePO param);

	// 주문변경 상세를 등록합니다.
	int insertShopOrderChangeDetail(ShopOrderChangeDetailSavePO param);

	// 주문변경 회수지/도착지 주소를 등록합니다.
	int insertShopOrderChangeExchangeAddress(ShopOrderChangeExchangeAddressSavePO param);

	// 주문상세 취소 수량/상태와 쿠폰/포인트 할인금액 차감을 함께 반영합니다.
	int updateShopOrderDetailCancelQuantity(
		@Param("ordNo") String ordNo,
		@Param("ordDtlNo") Integer ordDtlNo,
		@Param("cancelQty") Integer cancelQty,
		@Param("nextOrdDtlStatCd") String nextOrdDtlStatCd,
		@Param("goodsCpnDcAmt") int goodsCpnDcAmt,
		@Param("cartCpnDcAmt") int cartCpnDcAmt,
		@Param("pointUseAmt") int pointUseAmt,
		@Param("udtNo") Long udtNo
	);

	// 쇼핑몰 주문 마스터 상태를 변경합니다.
	int updateShopOrderBaseStatus(@Param("ordNo") String ordNo, @Param("ordStatCd") String ordStatCd, @Param("udtNo") Long udtNo);

	// 전체취소된 주문 마스터 상태와 배송비쿠폰 적용 정보를 함께 갱신합니다.
	int updateShopOrderBaseFullCancel(@Param("ordNo") String ordNo, @Param("ordStatCd") String ordStatCd, @Param("udtNo") Long udtNo);

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

	// 관리자 주문 상세 상태를 선택 주문상세번호 기준으로 일괄 변경합니다.
	int updateAdminOrderDetailStatusByOrdDtlNoList(
		@Param("ordNo") String ordNo,
		@Param("ordDtlNoList") List<Integer> ordDtlNoList,
		@Param("fromOrdDtlStatCd") String fromOrdDtlStatCd,
		@Param("toOrdDtlStatCd") String toOrdDtlStatCd,
		@Param("udtNo") Long udtNo
	);

	// 관리자 배송 준비중 일괄 처리를 반영합니다.
	int updateAdminOrderStartDeliveryPrepareList(
		@Param("itemList") List<AdminOrderStartDeliveryPrepareItemPO> itemList,
		@Param("fromOrdDtlStatCd") String fromOrdDtlStatCd,
		@Param("toOrdDtlStatCd") String toOrdDtlStatCd,
		@Param("udtNo") Long udtNo
	);

	// 관리자 배송 상태 일괄 처리를 반영합니다.
	int updateAdminOrderStartDeliveryStatusList(
		@Param("itemList") List<AdminOrderStartDeliveryKeyItemPO> itemList,
		@Param("fromOrdDtlStatCd") String fromOrdDtlStatCd,
		@Param("toOrdDtlStatCd") String toOrdDtlStatCd,
		@Param("udtNo") Long udtNo,
		@Param("updateDelvCompleteDt") boolean updateDelvCompleteDt
	);

	// 관리자 반품 회수 관리 반품 상세 상태를 클레임번호 기준으로 일괄 변경합니다.
	int updateAdminOrderReturnManageStatusByClaimNoList(
		@Param("clmNoList") List<String> clmNoList,
		@Param("fromChgDtlStatCd") String fromChgDtlStatCd,
		@Param("toChgDtlStatCd") String toChgDtlStatCd,
		@Param("udtNo") Long udtNo
	);

	// 관리자 반품 회수 관리 회수지 택배사와 송장번호를 일괄 저장합니다.
	int updateAdminOrderReturnManagePickupAddressList(
		@Param("itemList") List<AdminOrderReturnManagePickupItemPO> itemList,
		@Param("udtNo") Long udtNo
	);

	// 관리자 반품 회수 관리 회수 시작 일시를 일괄 저장합니다.
	int updateAdminOrderReturnManagePickupStartAddressList(
		@Param("clmNoList") List<String> clmNoList,
		@Param("udtNo") Long udtNo
	);

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

	// 환불 결제 성공 결과를 갱신합니다.
	int updateShopPaymentCancelSuccess(
		@Param("payNo") Long payNo,
		@Param("payStatCd") String payStatCd,
		@Param("cnlAmt") Long cnlAmt,
		@Param("tradeNo") String tradeNo,
		@Param("rspCode") String rspCode,
		@Param("rspMsg") String rspMsg,
		@Param("rspRawJson") String rspRawJson,
		@Param("apprDt") String apprDt,
		@Param("udtNo") Long udtNo
	);

	// 환불 결제 실패 결과를 갱신합니다.
	int updateShopPaymentCancelFailure(
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

	// 고객쿠폰번호 목록 기준으로 사용 상태를 원복합니다.
	int restoreShopCustomerCouponUseByCustCpnNoList(
		@Param("custNo") Long custNo,
		@Param("ordNo") String ordNo,
		@Param("custCpnNoList") List<Long> custCpnNoList,
		@Param("udtNo") Long udtNo
	);

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

	// 주문번호 기준 포인트 사용/복구 누적 요약을 조회합니다.
	List<ShopOrderPointDetailVO> getShopOrderPointDetailBalanceList(@Param("ordNo") String ordNo);

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

	// 고객의 현재 사용 가능한 보유 포인트 합계를 조회합니다.
	Integer getShopAvailablePointAmt(@Param("custNo") Long custNo);

	// 쇼핑몰 마이페이지 취소내역 클레임 전체 건수를 조회합니다.
	int countShopMypageCancelHistory(
		@Param("custNo") Long custNo,
		@Param("startDate") String startDate,
		@Param("endDate") String endDate
	);

	// 쇼핑몰 마이페이지 취소내역 클레임 목록을 페이징 조회합니다.
	List<ShopMypageCancelHistoryVO> getShopMypageCancelHistoryList(
		@Param("custNo") Long custNo,
		@Param("startDate") String startDate,
		@Param("endDate") String endDate,
		@Param("offset") int offset,
		@Param("pageSize") int pageSize
	);

	// 쇼핑몰 마이페이지 취소내역 상품 상세 목록을 클레임번호 기준으로 조회합니다.
	List<ShopMypageCancelHistoryDetailVO> getShopMypageCancelHistoryDetailList(
		@Param("clmNoList") List<String> clmNoList
	);

	// 쇼핑몰 마이페이지 취소상세 클레임을 단건 조회합니다.
	ShopMypageCancelHistoryVO getShopMypageCancelHistoryItemByClmNo(
		@Param("custNo") Long custNo,
		@Param("clmNo") String clmNo
	);

	// 쇼핑몰 마이페이지 반품내역 클레임 전체 건수를 조회합니다.
	int countShopMypageReturnHistory(
		@Param("custNo") Long custNo,
		@Param("startDate") String startDate,
		@Param("endDate") String endDate
	);

	// 쇼핑몰 마이페이지 반품내역 클레임 목록을 페이징 조회합니다.
	List<ShopMypageReturnHistoryVO> getShopMypageReturnHistoryList(
		@Param("custNo") Long custNo,
		@Param("startDate") String startDate,
		@Param("endDate") String endDate,
		@Param("offset") int offset,
		@Param("pageSize") int pageSize
	);

	// 쇼핑몰 마이페이지 반품내역 상품 상세 목록을 클레임번호 기준으로 조회합니다.
	List<ShopMypageReturnHistoryDetailVO> getShopMypageReturnHistoryDetailList(
		@Param("clmNoList") List<String> clmNoList
	);

	// 쇼핑몰 마이페이지 반품상세 클레임을 단건 조회합니다.
	ShopMypageReturnHistoryVO getShopMypageReturnHistoryItemByClmNo(
		@Param("custNo") Long custNo,
		@Param("clmNo") String clmNo
	);

	// 쇼핑몰 마이페이지 반품상세 회수지 1건을 조회합니다.
	ShopMypageReturnPickupAddressVO getShopMypageReturnPickupAddress(@Param("clmNo") String clmNo);

	// 쇼핑몰 마이페이지 반품 철회 대상 주문상품 1건을 조회합니다.
	ShopOrderReturnWithdrawResultVO getShopOrderReturnWithdrawTarget(
		@Param("custNo") Long custNo,
		@Param("ordNo") String ordNo,
		@Param("ordDtlNo") Integer ordDtlNo
	);

	// 쇼핑몰 마이페이지 반품 상세 1건을 철회 상태로 변경합니다.
	int withdrawShopOrderChangeDetail(
		@Param("clmNo") String clmNo,
		@Param("ordNo") String ordNo,
		@Param("ordDtlNo") Integer ordDtlNo,
		@Param("fromChgDtlStatCd") String fromChgDtlStatCd,
		@Param("toChgDtlStatCd") String toChgDtlStatCd,
		@Param("udtNo") Long udtNo
	);

	// 같은 클레임에 남아 있는 반품 상세 건수를 조회합니다.
	int countShopOrderRemainingReturnDetailByClaim(
		@Param("clmNo") String clmNo,
		@Param("ordNo") String ordNo
	);

	// 쇼핑몰 마이페이지 반품 클레임 마스터를 철회 상태로 변경합니다.
	int withdrawShopOrderChangeBase(
		@Param("clmNo") String clmNo,
		@Param("ordNo") String ordNo,
		@Param("toChgStatCd") String toChgStatCd,
		@Param("udtNo") Long udtNo
	);

	// 마이페이지 포인트 내역 목록을 페이징 조회합니다.
	List<ShopMypagePointItemVO> getShopMypagePointItemList(
		@Param("custNo") Long custNo,
		@Param("pageSize") int pageSize,
		@Param("offset") int offset
	);

	// 마이페이지 포인트 내역 전체 건수를 조회합니다.
	Integer getShopMypagePointItemCount(@Param("custNo") Long custNo);

	// 마이페이지 7일 이내 만료 예정 포인트 합계를 조회합니다.
	Integer getShopMypageExpiringPointAmt(@Param("custNo") Long custNo);

	// 관리자 주문 상세 마스터 정보를 조회합니다.
	AdminOrderMasterVO getAdminOrderMaster(@Param("ordNo") String ordNo);

	// 관리자 주문 상세 목록을 조회합니다.
	List<AdminOrderDetailRowVO> getAdminOrderDetailList(@Param("ordNo") String ordNo);

	// 관리자 주문 클레임 목록을 조회합니다.
	List<AdminOrderClaimRowVO> getAdminOrderClaimList(@Param("ordNo") String ordNo);
}
