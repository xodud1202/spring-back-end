package com.xodud1202.springbackend.mapper;

import com.xodud1202.springbackend.domain.admin.coupon.CouponDetailVO;
import com.xodud1202.springbackend.domain.admin.coupon.CouponPO;
import com.xodud1202.springbackend.domain.admin.coupon.CouponSavePO;
import com.xodud1202.springbackend.domain.admin.coupon.CouponTargetRowVO;
import com.xodud1202.springbackend.domain.admin.coupon.CouponTargetSaveRowPO;
import com.xodud1202.springbackend.domain.admin.coupon.CouponVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
// 관리자 쿠폰 목록 매퍼를 정의합니다.
public interface CouponMapper {
	// 관리자 쿠폰 목록을 조회합니다.
	List<CouponVO> getAdminCouponList(CouponPO param);

	// 관리자 쿠폰 목록 건수를 조회합니다.
	int getAdminCouponCount(CouponPO param);

	// 관리자 쿠폰 상세를 조회합니다.
	CouponDetailVO getAdminCouponDetail(@Param("cpnNo") Long cpnNo);

	// 관리자 쿠폰 상품 적용 대상을 조회합니다.
	List<CouponTargetRowVO> getAdminCouponApplyGoodsTargetList(@Param("cpnNo") Long cpnNo);

	// 관리자 쿠폰 기획전 적용 대상을 조회합니다.
	List<CouponTargetRowVO> getAdminCouponApplyExhibitionTargetList(@Param("cpnNo") Long cpnNo);

	// 관리자 쿠폰 카테고리 적용 대상을 조회합니다.
	List<CouponTargetRowVO> getAdminCouponApplyCategoryTargetList(@Param("cpnNo") Long cpnNo);

	// 관리자 쿠폰 상품 제외 대상을 조회합니다.
	List<CouponTargetRowVO> getAdminCouponExcludeGoodsTargetList(@Param("cpnNo") Long cpnNo);

	// 관리자 쿠폰을 등록합니다.
	int insertCouponBase(CouponSavePO param);

	// 관리자 쿠폰을 수정합니다.
	int updateCouponBase(CouponSavePO param);

	// 쿠폰 존재 여부를 조회합니다.
	int countCouponByNo(@Param("cpnNo") Long cpnNo);

	// 관리자 쿠폰 대상을 삭제합니다.
	int deleteCouponTargetByCpnNo(@Param("cpnNo") Long cpnNo);

	// 관리자 쿠폰 대상을 배치 등록합니다.
	int insertCouponTargetBatch(
		@Param("cpnNo") Long cpnNo,
		@Param("regNo") Long regNo,
		@Param("udtNo") Long udtNo,
		@Param("list") List<CouponTargetSaveRowPO> list
	);

	// 유효한 상품 대상 목록을 조회합니다.
	List<CouponTargetRowVO> getExistingGoodsTargetRows(@Param("targetValueList") List<String> targetValueList);

	// 유효한 기획전 대상 목록을 조회합니다.
	List<CouponTargetRowVO> getExistingExhibitionTargetRows(@Param("targetValueList") List<Integer> targetValueList);

	// 유효한 카테고리 대상 목록을 조회합니다.
	List<CouponTargetRowVO> getExistingCategoryTargetRows(@Param("targetValueList") List<String> targetValueList);
}
