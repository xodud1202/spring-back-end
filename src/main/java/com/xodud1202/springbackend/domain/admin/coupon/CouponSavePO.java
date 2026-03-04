package com.xodud1202.springbackend.domain.admin.coupon;

import lombok.Data;

import java.util.List;

@Data
// 관리자 쿠폰 저장 요청 정보를 정의합니다.
public class CouponSavePO {
	// 쿠폰 번호입니다.
	private Long cpnNo;
	// 쿠폰명입니다.
	private String cpnNm;
	// 쿠폰 상태 코드입니다.
	private String cpnStatCd;
	// 쿠폰 종류 코드입니다.
	private String cpnGbCd;
	// 쿠폰 타겟 코드입니다.
	private String cpnTargetCd;
	// 다운로드 시작일시입니다.
	private String cpnDownStartDt;
	// 다운로드 종료일시입니다.
	private String cpnDownEndDt;
	// 사용가능기간 구분 코드입니다.
	private String cpnUseDtGb;
	// 다운로드 후 사용 가능 일수입니다.
	private Integer cpnUsableDt;
	// 사용 시작일시입니다.
	private String cpnUseStartDt;
	// 사용 종료일시입니다.
	private String cpnUseEndDt;
	// 고객 다운로드 가능 여부입니다.
	private String cpnDownAbleYn;
	// 상태 중지 일시입니다.
	private String statStopDt;
	// 등록자 번호입니다.
	private Long regNo;
	// 수정자 번호입니다.
	private Long udtNo;
	// 적용 대상 목록입니다.
	private List<CouponTargetSaveRowPO> applyTargets;
	// 제외 대상 목록입니다.
	private List<CouponTargetSaveRowPO> excludeTargets;
}
