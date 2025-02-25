package com.agilemall.order.saga;

import com.agilemall.common.command.create.CreateDeliveryCommand;
import com.agilemall.common.command.create.CreatePaymentCommand;
import com.agilemall.common.dto.OrderDetailDTO;
import com.agilemall.common.dto.OrderStatusEnum;
import com.agilemall.common.dto.PaymentDetailDTO;
import com.agilemall.common.events.create.*;
import com.agilemall.order.command.CompleteOrderCreateCommand;
import com.agilemall.order.events.*;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.test.saga.SagaTestFixture;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.apache.commons.lang3.RandomStringUtils;
import static org.mockito.ArgumentMatchers.any;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderCreatingSagaTest {
    private SagaTestFixture<OrderCreatingSaga> fixture;
    private String orderId;
    private String userId;
    private String paymentId;
    private LocalDateTime orderDateTime;
    private List<OrderDetailDTO> orderDetails;
    private List<PaymentDetailDTO> paymentDetails;

    @BeforeEach
    void setUp() {
        fixture = new SagaTestFixture<>(OrderCreatingSaga.class);
        
        // 테스트에서 공통으로 사용할 데이터 설정
        orderId = "ORDER123";
        userId = "USER123";
        paymentId = "PAY123";
        orderDateTime = LocalDateTime.now();
        
        orderDetails = new ArrayList<>();
        orderDetails.add(new OrderDetailDTO(orderId, "PRODUCT1", 2, 20000));
        
        paymentDetails = new ArrayList<>();
        paymentDetails.add(new PaymentDetailDTO(orderId, paymentId, "10", 12000)); // 카드결제
        paymentDetails.add(new PaymentDetailDTO(orderId, paymentId, "20", 8000));  // 포인트결제
    }

    private static class DeliveryCommandMatcher extends TypeSafeMatcher<List<? super CommandMessage<?>>> {
        private final String orderId;

        public DeliveryCommandMatcher(String orderId) {
            this.orderId = orderId;
        }

        @Override
        protected boolean matchesSafely(List<? super CommandMessage<?>> commands) {
            if (commands.size() != 1) return false;
            CommandMessage<?> commandMessage = (CommandMessage<?>) commands.get(0);
            CreateDeliveryCommand command = (CreateDeliveryCommand) commandMessage.getPayload();
            return command.getOrderId().equals(orderId) &&
                   command.getDeliveryStatus().equals("10") &&
                   command.getDeliveryId().startsWith("SHIP_");
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("a CreateDeliveryCommand with orderId ").appendValue(orderId)
                      .appendText(" and deliveryId starting with 'SHIP_'");
        }
    }

    @Test
    @DisplayName("주문 생성 Saga 성공 시나리오 테스트")
    void createOrderSagaSuccessTest() {
        // Given
        CreatedOrderEvent orderEvent = new CreatedOrderEvent();
        orderEvent.setOrderId(orderId);
        orderEvent.setUserId(userId);
        orderEvent.setOrderDatetime(orderDateTime);
        orderEvent.setOrderStatus(OrderStatusEnum.CREATED.value());
        orderEvent.setOrderDetails(orderDetails);
        orderEvent.setPaymentId(paymentId);
        orderEvent.setPaymentDetails(paymentDetails);
        orderEvent.setTotalOrderAmt(20000);
        orderEvent.setTotalPaymentAmt(20000);
        orderEvent.setCompensation(false);

        // When & Then
        fixture.givenNoPriorActivity()
                .whenPublishingA(orderEvent)
                .expectActiveSagas(1)
                .expectDispatchedCommands(
                        CreatePaymentCommand.builder()
                                .paymentId(paymentId)
                                .orderId(orderId)
                                .totalPaymentAmt(20000)
                                .paymentDetails(paymentDetails)
                                .build()
                );

        // Payment 생성 성공
        CreatedPaymentEvent paymentEvent = new CreatedPaymentEvent();
        paymentEvent.setPaymentId(paymentId);
        paymentEvent.setOrderId(orderId);
        paymentEvent.setTotalPaymentAmt(20000);
        paymentEvent.setPaymentDetails(paymentDetails);
        paymentEvent.setCompensation(false);

        fixture.whenPublishingA(paymentEvent)
                .expectDispatchedCommandsMatching(new DeliveryCommandMatcher(orderId));

        // Delivery 생성 성공
        CreatedDeliveryEvent deliveryEvent = new CreatedDeliveryEvent();
        deliveryEvent.setDeliveryId("SHIP_" + orderId);
        deliveryEvent.setOrderId(orderId);
        deliveryEvent.setDeliveryStatus("10");

        fixture.whenPublishingA(deliveryEvent)
                .expectDispatchedCommands(
                        CompleteOrderCreateCommand.builder()
                                .orderId(orderId)
                                .orderStatus(OrderStatusEnum.COMPLETED.value())
                                .build()
                );

        // 주문 완료
        CompletedCreateOrderEvent completedEvent = new CompletedCreateOrderEvent();
        completedEvent.setOrderId(orderId);
        completedEvent.setOrderStatus(OrderStatusEnum.COMPLETED.value());

        fixture.whenPublishingA(completedEvent)
                .expectActiveSagas(0);
    }

    @Test
    @DisplayName("주문 생성 Saga 실패 시나리오 - 결제 실패")
    void createOrderSagaPaymentFailureTest() {
        // Given
        CreatedOrderEvent orderEvent = new CreatedOrderEvent();
        orderEvent.setOrderId(orderId);
        orderEvent.setUserId(userId);
        orderEvent.setOrderDatetime(orderDateTime);
        orderEvent.setOrderStatus(OrderStatusEnum.CREATED.value());
        orderEvent.setOrderDetails(orderDetails);
        orderEvent.setPaymentId(paymentId);
        orderEvent.setPaymentDetails(paymentDetails);
        orderEvent.setTotalOrderAmt(20000);
        orderEvent.setTotalPaymentAmt(20000);
        orderEvent.setCompensation(false);

        // When & Then
        fixture.givenNoPriorActivity()
                .whenPublishingA(orderEvent)
                .expectActiveSagas(1)
                .expectDispatchedCommands(
                        CreatePaymentCommand.builder()
                                .paymentId(paymentId)
                                .orderId(orderId)
                                .totalPaymentAmt(20000)
                                .paymentDetails(paymentDetails)
                                .build()
                );

        // Payment 생성 실패
        FailedCreatePaymentEvent failedPaymentEvent = new FailedCreatePaymentEvent(paymentId, orderId);

        fixture.whenPublishingA(failedPaymentEvent);
    }

    @Test
    @DisplayName("주문 생성 Saga 실패 시나리오 - 배송 실패")
    void createOrderSagaDeliveryFailureTest() {
        // Given
        CreatedOrderEvent orderEvent = new CreatedOrderEvent();
        orderEvent.setOrderId(orderId);
        orderEvent.setUserId(userId);
        orderEvent.setOrderDatetime(orderDateTime);
        orderEvent.setOrderStatus(OrderStatusEnum.CREATED.value());
        orderEvent.setOrderDetails(orderDetails);
        orderEvent.setPaymentId(paymentId);
        orderEvent.setPaymentDetails(paymentDetails);
        orderEvent.setTotalOrderAmt(20000);
        orderEvent.setTotalPaymentAmt(20000);
        orderEvent.setCompensation(false);

        // When & Then
        fixture.givenNoPriorActivity()
                .whenPublishingA(orderEvent)
                .expectActiveSagas(1)
                .expectDispatchedCommands(
                        CreatePaymentCommand.builder()
                                .paymentId(paymentId)
                                .orderId(orderId)
                                .totalPaymentAmt(20000)
                                .paymentDetails(paymentDetails)
                                .build()
                );

        // Payment 생성 성공
        CreatedPaymentEvent paymentEvent = new CreatedPaymentEvent();
        paymentEvent.setPaymentId(paymentId);
        paymentEvent.setOrderId(orderId);
        paymentEvent.setTotalPaymentAmt(20000);
        paymentEvent.setPaymentDetails(paymentDetails);
        paymentEvent.setCompensation(false);

        fixture.whenPublishingA(paymentEvent)
                .expectDispatchedCommandsMatching(new DeliveryCommandMatcher(orderId));

        // Delivery 생성 실패
        FailedCreateDeliveryEvent failedDeliveryEvent = new FailedCreateDeliveryEvent("SHIP_" + orderId, orderId);

        fixture.whenPublishingA(failedDeliveryEvent);
    }
} 