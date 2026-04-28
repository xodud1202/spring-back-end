package com.xodud1202.springbackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xodud1202.springbackend.config.properties.TossProperties;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderExchangeWithdrawPO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderExchangeWithdrawResultVO;
import com.xodud1202.springbackend.domain.shop.order.ShopOrderPaymentVO;
import com.xodud1202.springbackend.mapper.CartMapper;
import com.xodud1202.springbackend.mapper.CommonMapper;
import com.xodud1202.springbackend.mapper.ExhibitionMapper;
import com.xodud1202.springbackend.mapper.GoodsMapper;
import com.xodud1202.springbackend.mapper.OrderMapper;
import com.xodud1202.springbackend.mapper.SiteInfoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.util.ArrayList;
import java.util.List;

import static com.xodud1202.springbackend.common.Constants.Shop.SHOP_ORDER_CHANGE_DTL_GB_EXCHANGE_DELIVERY;
import static com.xodud1202.springbackend.common.Constants.Shop.SHOP_ORDER_CHANGE_DTL_GB_EXCHANGE_PICKUP;
import static com.xodud1202.springbackend.common.Constants.Shop.SHOP_ORDER_CHANGE_DTL_STAT_EXCHANGE_DELIVERY_WAIT;
import static com.xodud1202.springbackend.common.Constants.Shop.SHOP_ORDER_CHANGE_DTL_STAT_EXCHANGE_DELIVERY_WITHDRAW;
import static com.xodud1202.springbackend.common.Constants.Shop.SHOP_ORDER_CHANGE_DTL_STAT_EXCHANGE_PAYMENT_WAIT;
import static com.xodud1202.springbackend.common.Constants.Shop.SHOP_ORDER_CHANGE_DTL_STAT_EXCHANGE_WITHDRAW;
import static com.xodud1202.springbackend.common.Constants.Shop.SHOP_ORDER_CHANGE_STAT_WITHDRAW;
import static com.xodud1202.springbackend.common.Constants.Shop.SHOP_ORDER_ORD_GB_EXCHANGE;
import static com.xodud1202.springbackend.common.Constants.Shop.SHOP_ORDER_PAYMENT_METHOD_VIRTUAL_ACCOUNT;
import static com.xodud1202.springbackend.common.Constants.Shop.SHOP_ORDER_PAY_STAT_CANCEL;
import static com.xodud1202.springbackend.common.Constants.Shop.SHOP_ORDER_PAY_STAT_WAITING_DEPOSIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// 주문교환 서비스의 교환 철회 상태 전이를 확인합니다.
class OrderExchangeServiceTests {
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

	private RecordingTransactionManager transactionManager;

	private OrderExchangeService orderExchangeService;

	@BeforeEach
	// 테스트 대상 서비스와 협력 서비스를 구성합니다.
	void setUp() {
		transactionManager = new RecordingTransactionManager();
		TossProperties tossProperties = new TossProperties("test-client-key", "test-secret-key");
		ObjectMapper objectMapper = new ObjectMapper();
		OrderService orderService = new OrderService(
			cartMapper,
			orderMapper,
			goodsMapper,
			commonMapper,
			exhibitionMapper,
			siteInfoMapper,
			goodsImageService,
			shopAuthService,
			jusoAddressApiClient,
			tossPaymentsClient,
			tossProperties,
			objectMapper,
			transactionManager
		);
		orderExchangeService = new OrderExchangeService(
			orderService,
			orderMapper,
			goodsMapper,
			commonMapper,
			siteInfoMapper,
			tossPaymentsClient,
			transactionManager
		);
	}

	@Test
	@DisplayName("입금대기 교환 배송비 철회는 클레임 종료 후 PG 취소와 결제 취소 이력을 분리 저장한다")
	// 마지막 교환 상품 철회 시 클레임 마스터를 먼저 닫고, PG 취소 성공 결과는 REQUIRES_NEW로 저장합니다.
	void withdrawShopMypageOrderExchange_cancelsWaitingDepositPaymentAfterClaimClose() {
		// 철회 대상 교환 상세와 입금대기 결제 row를 구성합니다.
		ShopOrderExchangeWithdrawPO param = createWithdrawParam();
		ShopOrderPaymentVO payment = createWaitingDepositExchangePayment();
		when(orderMapper.getShopOrderExchangeWithdrawTarget(7L, "O220260406094219437", 1)).thenReturn(createWithdrawTarget());
		when(orderMapper.withdrawShopOrderExchangeDetail(
			"C220260427164521077",
			"O220260406094219437",
			1,
			SHOP_ORDER_CHANGE_DTL_GB_EXCHANGE_PICKUP,
			SHOP_ORDER_CHANGE_DTL_STAT_EXCHANGE_PAYMENT_WAIT,
			SHOP_ORDER_CHANGE_DTL_STAT_EXCHANGE_WITHDRAW,
			7L
		)).thenReturn(1);
		when(orderMapper.withdrawShopOrderExchangeDetail(
			"C220260427164521077",
			"O220260406094219437",
			1,
			SHOP_ORDER_CHANGE_DTL_GB_EXCHANGE_DELIVERY,
			SHOP_ORDER_CHANGE_DTL_STAT_EXCHANGE_DELIVERY_WAIT,
			SHOP_ORDER_CHANGE_DTL_STAT_EXCHANGE_DELIVERY_WITHDRAW,
			7L
		)).thenReturn(1);
		when(orderMapper.countShopOrderRemainingExchangePickupDetailByClaim(
			"C220260427164521077",
			"O220260406094219437"
		)).thenReturn(0);
		when(orderMapper.withdrawShopOrderChangeBase(
			"C220260427164521077",
			"O220260406094219437",
			SHOP_ORDER_CHANGE_STAT_WITHDRAW,
			7L
		)).thenReturn(1);
		when(orderMapper.getShopOrderExchangePaymentByClmNo("C220260427164521077", 7L)).thenReturn(payment);
		String rawResponse = """
			{"status":"CANCELED","cancels":[{"cancelAmount":6000,"transactionKey":"cancel-tx","canceledAt":"2026-04-27T17:10:11+09:00"}]}
			""";
		when(tossPaymentsClient.cancelPayment("exchange-payment-key", "교환 철회", null)).thenReturn(rawResponse);

		// 교환 철회를 실행합니다.
		ShopOrderExchangeWithdrawResultVO result = orderExchangeService.withdrawShopMypageOrderExchange(param, 7L);

		// 클레임 마스터 종료 후 PG 취소와 결제 이력 저장이 순서대로 호출되어야 합니다.
		InOrder inOrder = inOrder(orderMapper, tossPaymentsClient);
		inOrder.verify(orderMapper).withdrawShopOrderChangeBase(
			"C220260427164521077",
			"O220260406094219437",
			SHOP_ORDER_CHANGE_STAT_WITHDRAW,
			7L
		);
		inOrder.verify(tossPaymentsClient).cancelPayment("exchange-payment-key", "교환 철회", null);
		inOrder.verify(orderMapper).updateShopPaymentFailure(
			202L,
			SHOP_ORDER_PAY_STAT_CANCEL,
			"CANCELED",
			"교환 철회 완료",
			rawResponse,
			7L
		);
		assertThat(result.getClaimClosedYn()).isTrue();
		assertThat(result.getPaymentCancelYn()).isTrue();
		assertThat(result.getPayStatCd()).isEqualTo(SHOP_ORDER_PAY_STAT_CANCEL);
		assertThat(transactionManager.getPropagationBehaviors()).contains(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	// 교환 철회 요청 파라미터를 생성합니다.
	private ShopOrderExchangeWithdrawPO createWithdrawParam() {
		// 주문번호와 주문상세번호를 채운 요청 객체를 반환합니다.
		ShopOrderExchangeWithdrawPO param = new ShopOrderExchangeWithdrawPO();
		param.setOrdNo("O220260406094219437");
		param.setOrdDtlNo(1);
		return param;
	}

	// 교환 철회 대상 상세 조회 결과를 생성합니다.
	private ShopOrderExchangeWithdrawResultVO createWithdrawTarget() {
		// 결제대기 회수 상세와 배송대기 상세가 함께 있는 상태를 구성합니다.
		ShopOrderExchangeWithdrawResultVO target = new ShopOrderExchangeWithdrawResultVO();
		target.setClmNo("C220260427164521077");
		target.setOrdNo("O220260406094219437");
		target.setOrdDtlNo(1);
		target.setChgDtlStatCd(SHOP_ORDER_CHANGE_DTL_STAT_EXCHANGE_PAYMENT_WAIT);
		target.setDeliveryChgDtlStatCd(SHOP_ORDER_CHANGE_DTL_STAT_EXCHANGE_DELIVERY_WAIT);
		return target;
	}

	// 입금대기 상태의 교환 배송비 결제 row를 생성합니다.
	private ShopOrderPaymentVO createWaitingDepositExchangePayment() {
		// PG 취소 호출과 결제 이력 저장에 필요한 최소 필드를 채웁니다.
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
