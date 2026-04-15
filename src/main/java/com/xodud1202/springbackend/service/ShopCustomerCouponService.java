package com.xodud1202.springbackend.service;

import static com.xodud1202.springbackend.common.Constants.Shop.CPN_USE_DT_DATETIME;
import static com.xodud1202.springbackend.common.Constants.Shop.CPN_USE_DT_PERIOD;

import com.xodud1202.springbackend.domain.shop.auth.ShopCouponIssueRuleVO;
import com.xodud1202.springbackend.domain.shop.auth.ShopCustomerCouponSavePO;
import com.xodud1202.springbackend.mapper.ShopAuthMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
// 쇼핑몰 고객 쿠폰 발급 로직을 처리합니다.
public class ShopCustomerCouponService {

	private final ShopAuthMapper shopAuthMapper;

	@Transactional
	// 단일 쿠폰을 지정 수량만큼 고객에게 지급하고 실제 지급 건수를 반환합니다.
	public int issueShopCustomerCoupon(Long custNo, Long cpnNo, Integer issueCount) {
		// 쿠폰번호/수량이 유효하지 않으면 지급을 생략합니다.
		if (custNo == null || custNo < 1L || cpnNo == null || issueCount == null || issueCount < 1) {
			return 0;
		}

		// 정상 상태(CPN_STAT_02) 쿠폰만 발급 규칙을 조회합니다.
		ShopCouponIssueRuleVO couponRule = shopAuthMapper.getIssuableCouponIssueRule(cpnNo);
		if (couponRule == null) {
			return 0;
		}

		// 쿠폰 사용 가능 시작/종료 일시를 계산합니다.
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime usableStartDt = resolveCouponUsableStartDateTime(couponRule, now);
		LocalDateTime usableEndDt = resolveCouponUsableEndDateTime(couponRule, now);
		if (usableStartDt == null || usableEndDt == null || usableStartDt.isAfter(usableEndDt)) {
			return 0;
		}

		// 요청 수량만큼 고객 쿠폰을 반복 지급합니다.
		int issuedCount = 0;
		for (int issueIndex = 0; issueIndex < issueCount; issueIndex += 1) {
			ShopCustomerCouponSavePO couponSaveCommand = new ShopCustomerCouponSavePO(
				custNo,
				cpnNo,
				usableStartDt,
				usableEndDt,
				custNo,
				custNo
			);
			issuedCount += shopAuthMapper.insertCustomerCoupon(couponSaveCommand);
		}
		return issuedCount;
	}

	// 쿠폰 사용 가능 시작 일시를 계산합니다.
	private LocalDateTime resolveCouponUsableStartDateTime(ShopCouponIssueRuleVO couponRule, LocalDateTime now) {
		// 기간형 쿠폰은 발급 시점을 시작일시로 사용합니다.
		if (CPN_USE_DT_PERIOD.equals(couponRule.cpnUseDtGb())) {
			return now;
		}

		// 고정일시형 쿠폰은 쿠폰 기본 시작일시를 사용합니다.
		if (CPN_USE_DT_DATETIME.equals(couponRule.cpnUseDtGb())) {
			return couponRule.cpnUseStartDt();
		}
		return null;
	}

	// 쿠폰 사용 가능 종료 일시를 계산합니다.
	private LocalDateTime resolveCouponUsableEndDateTime(ShopCouponIssueRuleVO couponRule, LocalDateTime now) {
		// 기간형 쿠폰은 사용 가능 일수 기준으로 종료일시를 계산합니다.
		if (CPN_USE_DT_PERIOD.equals(couponRule.cpnUseDtGb())) {
			if (couponRule.cpnUsableDt() == null || couponRule.cpnUsableDt() < 1) {
				return null;
			}
			return now.plusDays(couponRule.cpnUsableDt());
		}

		// 고정일시형 쿠폰은 쿠폰 기본 종료일시를 사용합니다.
		if (CPN_USE_DT_DATETIME.equals(couponRule.cpnUseDtGb())) {
			return couponRule.cpnUseEndDt();
		}
		return null;
	}
}
