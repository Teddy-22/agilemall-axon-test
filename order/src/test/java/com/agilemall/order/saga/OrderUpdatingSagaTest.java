package com.agilemall.order.saga;

import com.agilemall.common.command.update.UpdatePaymentCommand;
import com.agilemall.common.dto.OrderDetailDTO;
import com.agilemall.common.dto.OrderStatusEnum;
import com.agilemall.common.dto.PaymentDetailDTO;
import com.agilemall.common.dto.PaymentStatusEnum;
import com.agilemall.common.events.update.FailedUpdatePaymentEvent;
import com.agilemall.common.events.update.UpdatedPaymentEvent;
import com.agilemall.order.command.CompleteUpdateOrderCommand;
import com.agilemall.order.events.*;
import org.axonframework.test.saga.SagaTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderUpdatingSagaTest {
    private SagaTestFixture<OrderUpdatingSaga> fixture;
    private String orderId;
    private String paymentId;
    private LocalDateTime orderDateTime;
    private List<OrderDetailDTO> orderDetails;
    private List<PaymentDetailDTO> paymentDetails;

    @BeforeEach
    void setUp() {
        fixture = new SagaTestFixture<>(OrderUpdatingSaga.class);
        
        // 테스트에서 공통으로 사용할 데이터 설정
        orderId = "ORDER123";
        paymentId = "PAY123";
        orderDateTime = LocalDateTime.now();
        
        orderDetails = new ArrayList<>();
        orderDetails.add(new OrderDetailDTO(orderId, "PRODUCT1", 3, 30000)); // 수량 증가
        
        paymentDetails = new ArrayList<>();
        paymentDetails.add(new PaymentDetailDTO(orderId, paymentId, "10", 18000)); // 카드결제
        paymentDetails.add(new PaymentDetailDTO(orderId, paymentId, "20", 12000)); // 포인트결제
    }

    @Test
    @DisplayName("주문 수정 Saga 성공 시나리오 테스트")
    void updateOrderSagaSuccessTest() {
        // Given
        UpdatedOrderEvent orderEvent = new UpdatedOrderEvent();
        orderEvent.setOrderId(orderId);
        orderEvent.setOrderDatetime(orderDateTime);
        orderEvent.setOrderStatus(OrderStatusEnum.UPDATED.value());
        orderEvent.setOrderDetails(orderDetails);
        orderEvent.setPaymentId(paymentId);
        orderEvent.setPaymentDetails(paymentDetails);
        orderEvent.setTotalOrderAmt(30000);
        orderEvent.setTotalPaymentAmt(30000);
        orderEvent.setCompensation(false);

        // When & Then
        fixture.givenNoPriorActivity()
                .whenPublishingA(orderEvent)
                .expectActiveSagas(1)
                .expectDispatchedCommands(
                        UpdatePaymentCommand.builder()
                                .paymentId(paymentId)
                                .orderId(orderId)
                                .totalPaymentAmt(30000)
                                .paymentStatus(PaymentStatusEnum.CREATED.value())
                                .paymentDetails(paymentDetails)
                                .build()
                );

        // Payment 수정 성공
        UpdatedPaymentEvent paymentEvent = new UpdatedPaymentEvent();
        paymentEvent.setPaymentId(paymentId);
        paymentEvent.setOrderId(orderId);
        paymentEvent.setTotalPaymentAmt(30000);
        paymentEvent.setPaymentDetails(paymentDetails);
        paymentEvent.setPaymentStatus(PaymentStatusEnum.CREATED.value());
        paymentEvent.setCompensation(false);

        fixture.whenPublishingA(paymentEvent)
                .expectDispatchedCommands(
                        CompleteUpdateOrderCommand.builder()
                                .orderId(orderId)
                                .orderStatus(OrderStatusEnum.COMPLETED.value())
                                .build()
                );

        // 주문 수정 완료
        CompletedUpdateOrderEvent completedEvent = new CompletedUpdateOrderEvent();
        completedEvent.setOrderId(orderId);
        completedEvent.setOrderStatus(OrderStatusEnum.COMPLETED.value());

        fixture.whenPublishingA(completedEvent)
                .expectActiveSagas(0);
    }

    @Test
    @DisplayName("주문 수정 Saga 실패 시나리오 - 결제 수정 실패")
    void updateOrderSagaPaymentFailureTest() {
        // Given
        UpdatedOrderEvent orderEvent = new UpdatedOrderEvent();
        orderEvent.setOrderId(orderId);
        orderEvent.setOrderDatetime(orderDateTime);
        orderEvent.setOrderStatus(OrderStatusEnum.UPDATED.value());
        orderEvent.setOrderDetails(orderDetails);
        orderEvent.setPaymentId(paymentId);
        orderEvent.setPaymentDetails(paymentDetails);
        orderEvent.setTotalOrderAmt(30000);
        orderEvent.setTotalPaymentAmt(30000);
        orderEvent.setCompensation(false);

        // When & Then
        fixture.givenNoPriorActivity()
                .whenPublishingA(orderEvent)
                .expectActiveSagas(1)
                .expectDispatchedCommands(
                        UpdatePaymentCommand.builder()
                                .paymentId(paymentId)
                                .orderId(orderId)
                                .totalPaymentAmt(30000)
                                .paymentStatus(PaymentStatusEnum.CREATED.value())
                                .paymentDetails(paymentDetails)
                                .build()
                );

        // Payment 수정 실패
        FailedUpdatePaymentEvent failedPaymentEvent = new FailedUpdatePaymentEvent(paymentId, orderId);

        fixture.whenPublishingA(failedPaymentEvent);
    }
} 