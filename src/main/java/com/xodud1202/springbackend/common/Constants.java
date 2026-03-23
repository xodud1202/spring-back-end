package com.xodud1202.springbackend.common;

import java.time.format.DateTimeFormatter;

// 스프링 백엔드 전역에서 공유하는 상수 묶음을 제공합니다.
public final class Constants {
	// 유틸리티 클래스의 인스턴스 생성을 막습니다.
	private Constants() {
	}

	// 공통 Y/N 성격 상수를 제공합니다.
	public static final class Common {
		public static final String YES = "Y";
		public static final String NO = "N";

		// 중첩 상수 클래스의 인스턴스 생성을 막습니다.
		private Common() {
		}
	}

	// 쇼핑몰 도메인 전반에서 사용하는 상수를 제공합니다.
	public static final class Shop {
		public static final String SHOP_SITE_ID = "xodud1202";
		public static final String SHOP_CART_GB_CART = "C";
		public static final String SHOP_CART_GB_ORDER = "O";
		public static final String DEFAULT_CUST_GRADE_CD = "CUST_GRADE_01";
		public static final String TARGET_GB_APPLY = "TARGET_GB_01";
		public static final String TARGET_GB_EXCLUDE = "TARGET_GB_02";
		public static final String CPN_TARGET_ALL = "CPN_TARGET_99";
		public static final String CPN_TARGET_GOODS = "CPN_TARGET_01";
		public static final String CPN_TARGET_BRAND = "CPN_TARGET_04";
		public static final String CPN_TARGET_CATEGORY = "CPN_TARGET_03";
		public static final String CPN_TARGET_EXHIBITION = "CPN_TARGET_02";
		public static final String CPN_GB_GOODS = "CPN_GB_01";
		public static final String CPN_GB_CART = "CPN_GB_03";
		public static final String CPN_GB_DELIVERY = "CPN_GB_04";
		public static final String CPN_DC_GB_AMOUNT = "CPN_DC_GB_01";
		public static final String CPN_DC_GB_RATE = "CPN_DC_GB_02";
		public static final String CPN_USE_DT_PERIOD = "CPN_USE_DT_01";
		public static final String CPN_USE_DT_DATETIME = "CPN_USE_DT_02";
		public static final int SHOP_MYPAGE_WISH_PAGE_SIZE = 10;
		public static final int SHOP_MYPAGE_COUPON_PAGE_SIZE = 10;
		public static final int SHOP_MYPAGE_COUPON_TOOLTIP_LIMIT = 10;
		public static final int SHOP_MYPAGE_ORDER_PAGE_SIZE = 5;
		public static final String SHOP_MYPAGE_ORDER_DATE_INVALID_MESSAGE = "조회 기간을 확인해주세요.";
		public static final String SHOP_MYPAGE_ORDER_NO_INVALID_MESSAGE = "주문번호를 확인해주세요.";
		public static final String SHOP_MYPAGE_ORDER_NOT_FOUND_MESSAGE = "주문 정보를 찾을 수 없습니다.";
		public static final String SHOP_MYPAGE_ORDER_DETAIL_ITEM_INVALID_MESSAGE = "주문상품 정보를 확인해주세요.";
		public static final String SHOP_MYPAGE_ORDER_CANCEL_UNAVAILABLE_MESSAGE = "취소 가능한 주문상품 정보를 찾을 수 없습니다.";
		public static final String SHOP_MYPAGE_ORDER_CANCEL_AMOUNT_MISMATCH_MESSAGE = "환불 금액이 상이합니다.";
		public static final DateTimeFormatter SHOP_MYPAGE_ORDER_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		public static final int SHOP_ORDER_ADDRESS_SEARCH_DEFAULT_PAGE = 1;
		public static final int SHOP_ORDER_ADDRESS_SEARCH_DEFAULT_COUNT = 10;
		public static final int SHOP_ORDER_ADDRESS_SEARCH_MAX_COUNT = 100;
		public static final String SHOP_ORDER_DISCOUNT_INVALID_MESSAGE = "할인 혜택 정보를 확인해주세요.";
		public static final String SHOP_ORDER_PAYMENT_INVALID_MESSAGE = "결제 정보를 확인해주세요.";
		public static final String SHOP_ORDER_PAYMENT_PREPARE_MESSAGE = "결제 준비에 실패했습니다.";
		public static final String SHOP_ORDER_PAYMENT_CONFIRM_MESSAGE = "결제 승인 처리에 실패했습니다.";
		public static final String SHOP_ORDER_PAYMENT_STOCK_SHORTAGE_MESSAGE = "재고가 부족한 상품이 있습니다.";
		public static final String SHOP_ORDER_BANK_GRP_CD = "BANK";
		public static final String SHOP_ORDER_PAYMENT_METHOD_CARD = "PAY_METHOD_01";
		public static final String SHOP_ORDER_PAYMENT_METHOD_VIRTUAL_ACCOUNT = "PAY_METHOD_02";
		public static final String SHOP_ORDER_PAYMENT_METHOD_TRANSFER = "PAY_METHOD_03";
		public static final String SHOP_ORDER_TOSS_METHOD_CARD = "CARD";
		public static final String SHOP_ORDER_TOSS_METHOD_VIRTUAL_ACCOUNT = "VIRTUAL_ACCOUNT";
		public static final String SHOP_ORDER_TOSS_METHOD_TRANSFER = "TRANSFER";
		public static final String SHOP_ORDER_PAY_GB_PAYMENT = "PAY_GB_01";
		public static final String SHOP_ORDER_PAY_GB_REFUND = "PAY_GB_02";
		public static final String SHOP_ORDER_PG_GB_TOSS = "TOSS";
		public static final String SHOP_ORDER_ORD_GB_ORDER = "O";
		public static final String SHOP_ORDER_STAT_READY = "ORD_STAT_00";
		public static final String SHOP_ORDER_STAT_WAITING_DEPOSIT = "ORD_STAT_01";
		public static final String SHOP_ORDER_STAT_DONE = "ORD_STAT_02";
		public static final String SHOP_ORDER_STAT_CANCEL = "ORD_STAT_03";
		public static final String SHOP_ORDER_DTL_STAT_READY = "ORD_DTL_STAT_00";
		public static final String SHOP_ORDER_DTL_STAT_WAITING_DEPOSIT = "ORD_DTL_STAT_01";
		public static final String SHOP_ORDER_DTL_STAT_DONE = "ORD_DTL_STAT_02";
		public static final String SHOP_ORDER_DTL_STAT_CANCEL = "ORD_DTL_STAT_99";
		public static final String SHOP_ORDER_PAY_STAT_READY = "PAY_STAT_01";
		public static final String SHOP_ORDER_PAY_STAT_DONE = "PAY_STAT_02";
		public static final String SHOP_ORDER_PAY_STAT_FAIL = "PAY_STAT_03";
		public static final String SHOP_ORDER_PAY_STAT_CANCEL = "PAY_STAT_04";
		public static final String SHOP_ORDER_PAY_STAT_WAITING_DEPOSIT = "PAY_STAT_05";
		public static final String TOSS_API_BASE_URL = "https://api.tosspayments.com";
		public static final String TOSS_API_VERSION = "2022-11-16";
		public static final String SHOP_ORDER_PAYMENT_API_VERSION = TOSS_API_VERSION;
		public static final String SHOP_ORDER_POINT_USE_MEMO = "주문 결제 포인트 사용";
		public static final String SHOP_ORDER_POINT_RESTORE_MEMO = "주문 결제 포인트 복구";
		public static final String SHOP_ORDER_CHANGE_GB_CANCEL = "C";
		public static final String SHOP_ORDER_CHANGE_STAT_PROGRESS = "CHG_STAT_01";
		public static final String SHOP_ORDER_CHANGE_DTL_GB_CANCEL = "CHG_DTL_GB_01";
		public static final String SHOP_ORDER_CHANGE_DTL_STAT_DONE = "CHG_DTL_STAT_02";
		public static final String CUST_GRADE_GRP_CD = "CUST_GRADE";
		public static final String DEFAULT_CUST_STAT_CD = "CUST_STAT_01";
		public static final String GOOGLE_JOIN_GB = "GOOGLE";
		public static final String JOIN_POINT_GIVE_GB_CD = "JOIN_POINT";
		public static final String JOIN_POINT_GIVE_MEMO = "회원가입 포인트 지급";
		public static final String SEX_UNSELECTED = "X";
		public static final String SEX_MALE = "M";
		public static final String SEX_FEMALE = "F";
		public static final String AGREEMENT_Y = Common.YES;
		public static final String AGREEMENT_N = Common.NO;
		public static final String DEVICE_TYPE_WEB = "WEB";
		public static final String DEVICE_TYPE_MOBILE = "MOBILE";
		public static final String DEVICE_TYPE_APP = "APP";
		public static final String DEVICE_GB_PC = "PC";
		public static final String DEVICE_GB_MO = "MO";
		public static final String DEVICE_GB_APP = "APP";

		// 중첩 상수 클래스의 인스턴스 생성을 막습니다.
		private Shop() {
		}
	}
}
