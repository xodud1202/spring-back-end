package com.xodud1202.springbackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xodud1202.springbackend.config.properties.TossProperties;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderPaymentConfirmPO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderPaymentVO;
import com.xodud1202.springbackend.mapper.CartMapper;
import com.xodud1202.springbackend.mapper.CommonMapper;
import com.xodud1202.springbackend.mapper.ExhibitionMapper;
import com.xodud1202.springbackend.mapper.GoodsMapper;
import com.xodud1202.springbackend.mapper.OrderMapper;
import com.xodud1202.springbackend.mapper.SiteInfoMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.util.ArrayList;
import java.util.List;

import static com.xodud1202.springbackend.common.Constants.Shop.SHOP_ORDER_CHANGE_DTL_GB_EXCHANGE_DELIVERY;
import static com.xodud1202.springbackend.common.Constants.Shop.SHOP_ORDER_CHANGE_DTL_GB_EXCHANGE_PICKUP;
import static com.xodud1202.springbackend.common.Constants.Shop.SHOP_ORDER_CHANGE_DTL_STAT_EXCHANGE_APPLY;
import static com.xodud1202.springbackend.common.Constants.Shop.SHOP_ORDER_CHANGE_DTL_STAT_EXCHANGE_PAYMENT_WAIT;
import static com.xodud1202.springbackend.common.Constants.Shop.SHOP_ORDER_CHANGE_DTL_STAT_EXCHANGE_DELIVERY_WITHDRAW;
import static com.xodud1202.springbackend.common.Constants.Shop.SHOP_ORDER_CHANGE_DTL_STAT_EXCHANGE_WITHDRAW;
import static com.xodud1202.springbackend.common.Constants.Shop.SHOP_ORDER_CHANGE_STAT_WITHDRAW;
import static com.xodud1202.springbackend.common.Constants.Shop.SHOP_ORDER_ORD_GB_EXCHANGE;
import static com.xodud1202.springbackend.common.Constants.Shop.SHOP_ORDER_PAYMENT_METHOD_VIRTUAL_ACCOUNT;
import static com.xodud1202.springbackend.common.Constants.Shop.SHOP_ORDER_DTL_STAT_CANCEL;
import static com.xodud1202.springbackend.common.Constants.Shop.SHOP_ORDER_DTL_STAT_DONE;
import static com.xodud1202.springbackend.common.Constants.Shop.SHOP_ORDER_PAY_STAT_CANCEL;
import static com.xodud1202.springbackend.common.Constants.Shop.SHOP_ORDER_PAY_STAT_DONE;
import static com.xodud1202.springbackend.common.Constants.Shop.SHOP_ORDER_PAY_STAT_FAIL;
import static com.xodud1202.springbackend.common.Constants.Shop.SHOP_ORDER_PAY_STAT_READY;
import static com.xodud1202.springbackend.common.Constants.Shop.SHOP_ORDER_PAY_STAT_WAITING_DEPOSIT;
import static com.xodud1202.springbackend.common.Constants.Shop.SHOP_ORDER_STAT_CANCEL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// 주문 서비스의 결제 웹훅 보안 검증을 확인합니다.
class OrderServiceSecurityTests {
	@Mock
	private CartMapper cartMapper;

	@Mock
	private OrderMapper orderMapper;

	@Mock
	private GoodsMapper goodsMapper;

	@Mock
	private CommonMapper commonMapper;

	@Mock
	private ExhibitionMapper exhibitionMapper;

	@Mock
	private SiteInfoMapper siteInfoMapper;

	@Mock
	private GoodsImageService goodsImageService;

	@Mock
	private ShopAuthService shopAuthService;

	@Mock
	private JusoAddressApiClient jusoAddressApiClient;

	@Mock
	private TossPaymentsClient tossPaymentsClient;

	@Spy
	private TossProperties tossProperties = new TossProperties("test-client-key", "test-secret-key");

	@Spy
	private ObjectMapper objectMapper = new ObjectMapper();

	@Spy
	private RecordingTransactionManager transactionManager = new RecordingTransactionManager();

	@InjectMocks
	private OrderService orderService;

	@Test
	@DisplayName("Toss 승인 실패 상태는 별도 트랜잭션으로 기록한다")
	// 승인 실패 후 예외가 발생해도 결제 실패/주문 취소 기록은 REQUIRES_NEW로 남겨야 합니다.
	void confirmShopOrderPayment_recordsFailureInRequiresNewTransactionWhenTossConfirmFails() {
		// 결제 준비 row와 Toss 실패 응답을 목으로 구성합니다.
		ShopOrderPaymentVO payment = createReadyPayment();
		when(orderMapper.getShopPaymentByPayNo(101L)).thenReturn(payment);
		when(orderMapper.getShopOrderRestoreCartItemList("ORD202604230001")).thenReturn(List.of());
		String errorBody = """
			{"code":"PAY_PROCESS_CANCELED","message":"승인 실패"}
			""";
		when(tossPaymentsClient.confirmPayment("payment-key", "ORD202604230001", 39000L))
			.thenThrow(new TossPaymentClientException(400, errorBody, null));

		// 결제 승인 요청을 구성합니다.
		ShopOrderPaymentConfirmPO param = new ShopOrderPaymentConfirmPO();
		param.setPayNo(101L);
		param.setOrdNo("ORD202604230001");
		param.setPaymentKey("payment-key");
		param.setAmount(39000L);

		// Toss 승인 실패 메시지를 사용자 예외로 반환합니다.
		assertThatThrownBy(() -> orderService.confirmShopOrderPayment(param, 7L))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("승인 실패");

		// 실패 상태와 주문 취소 상태가 별도 트랜잭션 안에서 저장되도록 호출되어야 합니다.
		verify(orderMapper).updateShopPaymentFailure(
			eq(101L),
			eq(SHOP_ORDER_PAY_STAT_FAIL),
			eq("PAY_PROCESS_CANCELED"),
			eq("승인 실패"),
			eq(errorBody),
			eq(7L)
		);
		verify(orderMapper).updateShopOrderBaseStatus("ORD202604230001", SHOP_ORDER_STAT_CANCEL, 7L);
		verify(orderMapper).updateShopOrderDetailStatus("ORD202604230001", SHOP_ORDER_DTL_STAT_CANCEL, 7L);
		assertThat(transactionManager.getPropagationBehaviors()).contains(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	@Test
	@DisplayName("PAYMENT_STATUS_CHANGED 웹훅은 Toss 재조회 상태와 다르면 상태를 변경하지 않는다")
	// paymentKey를 아는 호출자가 DONE 상태를 위조해도 Toss 재조회 결과와 다르면 차단되는지 검증합니다.
	void handleShopOrderPaymentWebhook_rejectsForgedPaymentStatusChanged() {
		// 로컬 입금대기 결제와 Toss 재조회 결과를 목으로 구성합니다.
		ShopOrderPaymentVO payment = createWaitingDepositPayment();
		when(orderMapper.getShopPaymentByTossPaymentKeyHash(anyString())).thenReturn(payment);
		when(tossPaymentsClient.getPayment("payment-key")).thenReturn("""
			{"paymentKey":"payment-key","orderId":"ORD202604230001","status":"WAITING_FOR_DEPOSIT","totalAmount":39000}
			""");

		// 웹훅 본문은 DONE으로 위조합니다.
		String rawBody = """
			{"eventType":"PAYMENT_STATUS_CHANGED","data":{"paymentKey":"payment-key","orderId":"ORD202604230001","status":"DONE"}}
			""";

		// 검증 실패 시 보안 예외가 발생하고 결제 상태는 갱신되지 않아야 합니다.
		assertThatThrownBy(() -> orderService.handleShopOrderPaymentWebhook(rawBody))
			.isInstanceOf(SecurityException.class)
			.hasMessage("웹훅 검증에 실패했습니다.");
		verify(orderMapper, never()).updateShopPaymentWebhook(
			eq(101L),
			eq(SHOP_ORDER_PAY_STAT_DONE),
			eq("DONE"),
			eq("무통장입금 완료"),
			anyString(),
			anyString(),
			eq(7L)
		);
	}

	@Test
	@DisplayName("PAYMENT_STATUS_CHANGED 웹훅은 Toss 재조회 정보가 모두 일치할 때만 DONE을 반영한다")
	// paymentKey, 주문번호, 금액, 상태가 모두 일치하면 무통장 입금완료 후처리가 실행되는지 검증합니다.
	void handleShopOrderPaymentWebhook_processesVerifiedPaymentStatusChanged() {
		// 로컬 입금대기 결제와 Toss 재조회 결과를 목으로 구성합니다.
		ShopOrderPaymentVO payment = createWaitingDepositPayment();
		when(orderMapper.getShopPaymentByTossPaymentKeyHash(anyString())).thenReturn(payment);
		when(tossPaymentsClient.getPayment("payment-key")).thenReturn("""
			{"paymentKey":"payment-key","orderId":"ORD202604230001","status":"DONE","totalAmount":39000,"approvedAt":"2026-04-23T10:15:30+09:00"}
			""");

		// 검증 가능한 결제 상태 변경 웹훅을 반영합니다.
		orderService.handleShopOrderPaymentWebhook("""
			{"eventType":"PAYMENT_STATUS_CHANGED","createdAt":"2026-04-23T10:15:31+09:00","data":{"paymentKey":"payment-key","orderId":"ORD202604230001","status":"DONE"}}
			""");

		// 입금완료 결제 상태와 주문 상태 갱신이 호출되는지 검증합니다.
		verify(orderMapper).updateShopPaymentWebhook(
			eq(101L),
			eq(SHOP_ORDER_PAY_STAT_DONE),
			eq("DONE"),
			eq("무통장입금 완료"),
			anyString(),
			eq("2026-04-23 10:15:31"),
			eq(7L)
		);
		verify(orderMapper).updateShopOrderDetailStatus("ORD202604230001", SHOP_ORDER_DTL_STAT_DONE, 7L);
	}

	@Test
	@DisplayName("교환 배송비 DEPOSIT_CALLBACK 웹훅은 클레임번호 기준으로 입금완료를 반영한다")
	// Toss orderId가 C로 시작하면 클레임번호 기준 교환 배송비 결제를 찾아 회수 상세를 교환 신청 상태로 변경합니다.
	void handleShopOrderPaymentWebhook_processesExchangeDepositCallbackByClaimNo() {
		// 클레임번호로 조회되는 교환 배송비 입금대기 결제 row를 목으로 구성합니다.
		ShopOrderPaymentVO payment = createWaitingDepositExchangePayment();
		when(orderMapper.getShopExchangePaymentByClmNoForWebhook("C220260427164521077")).thenReturn(payment);
		when(orderMapper.updateShopOrderChangeDetailStatusByClaimGbAndStatus(
			"C220260427164521077",
			SHOP_ORDER_CHANGE_DTL_GB_EXCHANGE_PICKUP,
			SHOP_ORDER_CHANGE_DTL_STAT_EXCHANGE_PAYMENT_WAIT,
			SHOP_ORDER_CHANGE_DTL_STAT_EXCHANGE_APPLY,
			7L
		)).thenReturn(1);

		// 정상 secret이 포함된 Toss 입금완료 콜백을 반영합니다.
		orderService.handleShopOrderPaymentWebhook("""
			{"eventType":"DEPOSIT_CALLBACK","createdAt":"2026-04-27T17:01:02+09:00","orderId":"C220260427164521077","status":"DONE","secret":"deposit-secret"}
			""");

		// 교환 배송비 결제 상태와 교환 회수 상세 상태가 함께 변경되어야 합니다.
		verify(orderMapper).updateShopPaymentWebhook(
			eq(202L),
			eq(SHOP_ORDER_PAY_STAT_DONE),
			eq("DONE"),
			eq("교환 배송비 무통장입금 완료"),
			anyString(),
			eq("2026-04-27 17:01:02"),
			eq(7L)
		);
		verify(orderMapper).updateShopOrderChangeDetailStatusByClaimGbAndStatus(
			"C220260427164521077",
			SHOP_ORDER_CHANGE_DTL_GB_EXCHANGE_PICKUP,
			SHOP_ORDER_CHANGE_DTL_STAT_EXCHANGE_PAYMENT_WAIT,
			SHOP_ORDER_CHANGE_DTL_STAT_EXCHANGE_APPLY,
			7L
		);
		verify(orderMapper, never()).updateShopOrderDetailStatus("O220260406094219437", SHOP_ORDER_DTL_STAT_DONE, 7L);
	}

	@Test
	@DisplayName("교환 배송비 PAYMENT_STATUS_CHANGED 웹훅은 Toss 재조회 정보가 일치할 때만 입금완료를 반영한다")
	// 클레임번호, 결제키, 금액, 상태가 모두 일치하면 교환 배송비 입금완료 후처리를 실행합니다.
	void handleShopOrderPaymentWebhook_processesVerifiedExchangePaymentStatusChanged() {
		// 교환 배송비 입금대기 결제와 Toss 재조회 결과를 목으로 구성합니다.
		ShopOrderPaymentVO payment = createWaitingDepositExchangePayment();
		when(orderMapper.getShopPaymentByTossPaymentKeyHash(anyString())).thenReturn(payment);
		when(tossPaymentsClient.getPayment("exchange-payment-key")).thenReturn("""
			{"paymentKey":"exchange-payment-key","orderId":"C220260427164521077","status":"DONE","totalAmount":6000,"approvedAt":"2026-04-27T17:02:01+09:00"}
			""");
		when(orderMapper.updateShopOrderChangeDetailStatusByClaimGbAndStatus(
			"C220260427164521077",
			SHOP_ORDER_CHANGE_DTL_GB_EXCHANGE_PICKUP,
			SHOP_ORDER_CHANGE_DTL_STAT_EXCHANGE_PAYMENT_WAIT,
			SHOP_ORDER_CHANGE_DTL_STAT_EXCHANGE_APPLY,
			7L
		)).thenReturn(1);

		// 검증 가능한 교환 배송비 결제 상태 변경 웹훅을 반영합니다.
		orderService.handleShopOrderPaymentWebhook("""
			{"eventType":"PAYMENT_STATUS_CHANGED","createdAt":"2026-04-27T17:02:03+09:00","data":{"paymentKey":"exchange-payment-key","orderId":"C220260427164521077","status":"DONE"}}
			""");

		// 교환 배송비 결제 완료 메시지와 회수 상세 상태 변경이 호출되어야 합니다.
		verify(orderMapper).updateShopPaymentWebhook(
			eq(202L),
			eq(SHOP_ORDER_PAY_STAT_DONE),
			eq("DONE"),
			eq("교환 배송비 무통장입금 완료"),
			anyString(),
			eq("2026-04-27 17:02:03"),
			eq(7L)
		);
		verify(orderMapper).updateShopOrderChangeDetailStatusByClaimGbAndStatus(
			"C220260427164521077",
			SHOP_ORDER_CHANGE_DTL_GB_EXCHANGE_PICKUP,
			SHOP_ORDER_CHANGE_DTL_STAT_EXCHANGE_PAYMENT_WAIT,
			SHOP_ORDER_CHANGE_DTL_STAT_EXCHANGE_APPLY,
			7L
		);
	}

	@Test
	@DisplayName("취소된 교환 배송비 DONE 웹훅은 결제와 클레임 상태를 되살리지 않는다")
	// 로컬 결제가 이미 취소된 뒤 도착한 DONE 웹훅은 지연 콜백으로 보고 무시합니다.
	void handleShopOrderPaymentWebhook_ignoresDoneWhenExchangePaymentAlreadyCanceled() {
		// 취소 상태의 교환 배송비 결제 row를 목으로 구성합니다.
		ShopOrderPaymentVO payment = createWaitingDepositExchangePayment();
		payment.setPayStatCd(SHOP_ORDER_PAY_STAT_CANCEL);
		when(orderMapper.getShopExchangePaymentByClmNoForWebhook("C220260427164521077")).thenReturn(payment);

		// 취소 후 지연 도착한 Toss 입금완료 콜백을 반영합니다.
		orderService.handleShopOrderPaymentWebhook("""
			{"eventType":"DEPOSIT_CALLBACK","createdAt":"2026-04-27T17:04:05+09:00","orderId":"C220260427164521077","status":"DONE","secret":"deposit-secret"}
			""");

		// 결제 완료와 교환 신청 상태 전이는 호출되지 않아야 합니다.
		verify(orderMapper, never()).updateShopPaymentWebhook(
			eq(202L),
			eq(SHOP_ORDER_PAY_STAT_DONE),
			eq("DONE"),
			eq("교환 배송비 무통장입금 완료"),
			anyString(),
			anyString(),
			eq(7L)
		);
		verify(orderMapper, never()).updateShopOrderChangeDetailStatusByClaimGbAndStatus(
			"C220260427164521077",
			SHOP_ORDER_CHANGE_DTL_GB_EXCHANGE_PICKUP,
			SHOP_ORDER_CHANGE_DTL_STAT_EXCHANGE_PAYMENT_WAIT,
			SHOP_ORDER_CHANGE_DTL_STAT_EXCHANGE_APPLY,
			7L
		);
	}

	@Test
	@DisplayName("교환 배송비 PAYMENT_STATUS_CHANGED 웹훅은 Toss 재조회 주문번호가 다르면 상태를 변경하지 않는다")
	// paymentKey가 맞아도 Toss 원본 orderId가 클레임번호와 다르면 위조 웹훅으로 보고 차단합니다.
	void handleShopOrderPaymentWebhook_rejectsExchangePaymentStatusChangedWhenTossOrderIdDiffers() {
		// 교환 배송비 입금대기 결제와 불일치 Toss 재조회 결과를 목으로 구성합니다.
		ShopOrderPaymentVO payment = createWaitingDepositExchangePayment();
		when(orderMapper.getShopPaymentByTossPaymentKeyHash(anyString())).thenReturn(payment);
		when(tossPaymentsClient.getPayment("exchange-payment-key")).thenReturn("""
			{"paymentKey":"exchange-payment-key","orderId":"O220260406094219437","status":"DONE","totalAmount":6000}
			""");

		// 교환 배송비 클레임번호와 Toss 원본 주문번호가 다르면 보안 예외가 발생해야 합니다.
		assertThatThrownBy(() -> orderService.handleShopOrderPaymentWebhook("""
			{"eventType":"PAYMENT_STATUS_CHANGED","data":{"paymentKey":"exchange-payment-key","orderId":"C220260427164521077","status":"DONE"}}
			"""))
			.isInstanceOf(SecurityException.class)
			.hasMessage("웹훅 검증에 실패했습니다.");
		verify(orderMapper, never()).updateShopPaymentWebhook(
			eq(202L),
			eq(SHOP_ORDER_PAY_STAT_DONE),
			eq("DONE"),
			eq("교환 배송비 무통장입금 완료"),
			anyString(),
			anyString(),
			eq(7L)
		);
	}

	@Test
	@DisplayName("교환 배송비 EXPIRED 웹훅은 결제 실패와 클레임 철회를 반영한다")
	// 무통장입금 기한이 만료되면 교환 배송비 결제 실패와 교환 클레임 철회 상태를 함께 저장합니다.
	void handleShopOrderPaymentWebhook_expiresExchangeDepositPayment() {
		// 교환 배송비 만료 웹훅 처리 결과를 검증합니다.
		assertExchangeDepositClosedWebhook("EXPIRED", SHOP_ORDER_PAY_STAT_FAIL, "교환 배송비 무통장입금 만료");
	}

	@Test
	@DisplayName("교환 배송비 CANCELED 웹훅은 결제 취소와 클레임 철회를 반영한다")
	// 무통장입금이 취소되면 교환 배송비 결제 취소와 교환 클레임 철회 상태를 함께 저장합니다.
	void handleShopOrderPaymentWebhook_cancelsExchangeDepositPayment() {
		// 교환 배송비 취소 웹훅 처리 결과를 검증합니다.
		assertExchangeDepositClosedWebhook("CANCELED", SHOP_ORDER_PAY_STAT_CANCEL, "교환 배송비 무통장입금 취소");
	}

	// 입금대기 상태의 가상계좌 결제 row를 생성합니다.
	private ShopOrderPaymentVO createWaitingDepositPayment() {
		// 웹훅 검증에 필요한 최소 결제 필드를 채웁니다.
		ShopOrderPaymentVO payment = new ShopOrderPaymentVO();
		payment.setPayNo(101L);
		payment.setOrdNo("ORD202604230001");
		payment.setCustNo(7L);
		payment.setPayStatCd(SHOP_ORDER_PAY_STAT_WAITING_DEPOSIT);
		payment.setPayMethodCd(SHOP_ORDER_PAYMENT_METHOD_VIRTUAL_ACCOUNT);
		payment.setPayAmt(39000L);
		payment.setTossPaymentKey("payment-key");
		return payment;
	}

	// 입금대기 상태의 교환 배송비 가상계좌 결제 row를 생성합니다.
	private ShopOrderPaymentVO createWaitingDepositExchangePayment() {
		// 교환 배송비 웹훅 검증에 필요한 최소 결제 필드를 채웁니다.
		ShopOrderPaymentVO payment = new ShopOrderPaymentVO();
		payment.setPayNo(202L);
		payment.setOrdNo("O220260406094219437");
		payment.setClmNo("C220260427164521077");
		payment.setCustNo(7L);
		payment.setPayStatCd(SHOP_ORDER_PAY_STAT_WAITING_DEPOSIT);
		payment.setPayMethodCd(SHOP_ORDER_PAYMENT_METHOD_VIRTUAL_ACCOUNT);
		payment.setOrdGbCd(SHOP_ORDER_ORD_GB_EXCHANGE);
		payment.setPayAmt(6000L);
		payment.setTossPaymentKey("exchange-payment-key");
		payment.setRspRawJson("""
			{"secret":"deposit-secret"}
			""");
		return payment;
	}

	// 교환 배송비 입금 만료 또는 취소 웹훅 처리 결과를 검증합니다.
	private void assertExchangeDepositClosedWebhook(String paymentStatus, String expectedPayStatCd, String expectedMessage) {
		// 클레임번호로 조회되는 교환 배송비 입금대기 결제 row를 목으로 구성합니다.
		ShopOrderPaymentVO payment = createWaitingDepositExchangePayment();
		when(orderMapper.getShopExchangePaymentByClmNoForWebhook("C220260427164521077")).thenReturn(payment);

		// Toss 입금 만료 또는 취소 콜백을 반영합니다.
		orderService.handleShopOrderPaymentWebhook("""
			{"eventType":"DEPOSIT_CALLBACK","createdAt":"2026-04-27T17:03:04+09:00","orderId":"C220260427164521077","status":"%s","secret":"deposit-secret"}
			""".formatted(paymentStatus));

		// 결제 상태와 클레임 철회 상태가 함께 변경되어야 합니다.
		verify(orderMapper).updateShopPaymentWebhook(
			eq(202L),
			eq(expectedPayStatCd),
			eq(paymentStatus),
			eq(expectedMessage),
			anyString(),
			eq("2026-04-27 17:03:04"),
			eq(7L)
		);
		verify(orderMapper).updateShopOrderChangeBaseStatus("C220260427164521077", SHOP_ORDER_CHANGE_STAT_WITHDRAW, 7L);
		verify(orderMapper).updateShopOrderChangeDetailStatusByClaimAndGb(
			"C220260427164521077",
			SHOP_ORDER_CHANGE_DTL_GB_EXCHANGE_PICKUP,
			SHOP_ORDER_CHANGE_DTL_STAT_EXCHANGE_WITHDRAW,
			7L
		);
		verify(orderMapper).updateShopOrderChangeDetailStatusByClaimAndGb(
			"C220260427164521077",
			SHOP_ORDER_CHANGE_DTL_GB_EXCHANGE_DELIVERY,
			SHOP_ORDER_CHANGE_DTL_STAT_EXCHANGE_DELIVERY_WITHDRAW,
			7L
		);
	}

	// 결제 승인 실패 테스트용 준비 상태 결제 row를 생성합니다.
	private ShopOrderPaymentVO createReadyPayment() {
		// 승인 검증과 실패 저장에 필요한 최소 결제 필드를 채웁니다.
		ShopOrderPaymentVO payment = new ShopOrderPaymentVO();
		payment.setPayNo(101L);
		payment.setOrdNo("ORD202604230001");
		payment.setCustNo(7L);
		payment.setPayStatCd(SHOP_ORDER_PAY_STAT_READY);
		payment.setPayAmt(39000L);
		payment.setReqRawJson("{}");
		return payment;
	}

	// TransactionTemplate 전파 속성을 검증하기 위한 테스트용 트랜잭션 매니저입니다.
	private static final class RecordingTransactionManager extends AbstractPlatformTransactionManager {
		private final List<Integer> propagationBehaviors = new ArrayList<>();

		// 실행된 트랜잭션 전파 속성 목록을 반환합니다.
		private List<Integer> getPropagationBehaviors() {
			return propagationBehaviors;
		}

		@Override
		// 트랜잭션 객체를 생성합니다.
		protected Object doGetTransaction() {
			return new Object();
		}

		@Override
		// 트랜잭션 시작 시 전파 속성을 기록합니다.
		protected void doBegin(Object transaction, TransactionDefinition definition) {
			propagationBehaviors.add(definition.getPropagationBehavior());
		}

		@Override
		// 테스트용 트랜잭션 커밋은 별도 동작 없이 완료합니다.
		protected void doCommit(DefaultTransactionStatus status) {
		}

		@Override
		// 테스트용 트랜잭션 롤백은 별도 동작 없이 완료합니다.
		protected void doRollback(DefaultTransactionStatus status) {
		}
	}
}
