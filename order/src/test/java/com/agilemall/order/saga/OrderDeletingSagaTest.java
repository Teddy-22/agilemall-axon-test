package com.agilemall.order.saga;

import com.agilemall.common.command.delete.DeleteDeliveryCommand;
import com.agilemall.common.command.delete.DeletePaymentCommand;
import com.agilemall.common.command.delete.DeleteReportCommand;
import com.agilemall.common.dto.ReportDTO;
import com.agilemall.common.events.delete.*;
import com.agilemall.common.queries.Queries;
import com.agilemall.order.command.CompleteDeleteOrderCommand;
import com.agilemall.order.events.*;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.test.saga.SagaTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class OrderDeletingSagaTest {
    private SagaTestFixture<OrderDeletingSaga> fixture;
    private String orderId;
    private String paymentId;
    private String deliveryId;
    private String reportId;

    @Mock
    private QueryGateway queryGateway;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        fixture = new SagaTestFixture<>(OrderDeletingSaga.class);
        
        // 테스트에서 공통으로 사용할 데이터 설정
        orderId = "ORDER123";
        paymentId = "PAY123";
        deliveryId = "DEL123";
        reportId = "REP123";

        // Mock ReportDTO
        ReportDTO reportDTO = new ReportDTO();
        reportDTO.setOrderId(orderId);
        reportDTO.setPaymentId(paymentId);
        reportDTO.setDeliveryId(deliveryId);
        reportDTO.setReportId(reportId);

        // Mock QueryGateway
        CompletableFuture<ReportDTO> future = CompletableFuture.completedFuture(reportDTO);
        when(queryGateway.query(eq(Queries.REPORT_BY_ORDER_ID), eq(orderId), eq(ResponseTypes.instanceOf(ReportDTO.class))))
                .thenReturn(future);

        // Register the mock QueryGateway
        fixture.registerResource(queryGateway);
    }

    @Test
    @DisplayName("주문 삭제 Saga 성공 시나리오 테스트")
    void deleteOrderSagaSuccessTest() {
        // Given
        DeletedOrderEvent orderEvent = new DeletedOrderEvent(orderId);

        // When & Then
        fixture.givenNoPriorActivity()
                .whenPublishingA(orderEvent)
                .expectActiveSagas(1)
                .expectDispatchedCommands(
                        DeletePaymentCommand.builder()
                                .paymentId(paymentId)
                                .orderId(orderId)
                                .build()
                );

        // Payment 삭제 성공
        DeletedPaymentEvent paymentEvent = new DeletedPaymentEvent(paymentId, orderId);

        fixture.whenPublishingA(paymentEvent)
                .expectDispatchedCommands(
                        DeleteDeliveryCommand.builder()
                                .deliveryId(deliveryId)
                                .orderId(orderId)
                                .build()
                );

        // Delivery 삭제 성공
        DeletedDeliveryEvent deliveryEvent = new DeletedDeliveryEvent(deliveryId, orderId);

        fixture.whenPublishingA(deliveryEvent)
                .expectDispatchedCommands(
                        DeleteReportCommand.builder()
                                .reportId(reportId)
                                .orderId(orderId)
                                .build()
                );

        // Report 삭제 성공
        DeletedReportEvent reportEvent = new DeletedReportEvent(reportId, orderId);

        fixture.whenPublishingA(reportEvent)
                .expectDispatchedCommands(
                        CompleteDeleteOrderCommand.builder()
                                .orderId(orderId)
                                .build()
                );

        // 주문 삭제 완료
        CompletedDeleteOrderEvent completedEvent = new CompletedDeleteOrderEvent(orderId);

        fixture.whenPublishingA(completedEvent)
                .expectActiveSagas(0);
    }

    @Test
    @DisplayName("주문 삭제 Saga 실패 시나리오 - 결제 삭제 실패")
    void deleteOrderSagaPaymentFailureTest() {
        // Given
        DeletedOrderEvent orderEvent = new DeletedOrderEvent(orderId);

        // When & Then
        fixture.givenNoPriorActivity()
                .whenPublishingA(orderEvent)
                .expectActiveSagas(1)
                .expectDispatchedCommands(
                        DeletePaymentCommand.builder()
                                .paymentId(paymentId)
                                .orderId(orderId)
                                .build()
                );

        // Payment 삭제 실패
        FailedDeletePaymentEvent failedPaymentEvent = new FailedDeletePaymentEvent(paymentId, orderId);

        fixture.whenPublishingA(failedPaymentEvent);
    }

    @Test
    @DisplayName("주문 삭제 Saga 실패 시나리오 - 배송 삭제 실패")
    void deleteOrderSagaDeliveryFailureTest() {
        // Given
        DeletedOrderEvent orderEvent = new DeletedOrderEvent(orderId);

        // When & Then
        fixture.givenNoPriorActivity()
                .whenPublishingA(orderEvent)
                .expectActiveSagas(1)
                .expectDispatchedCommands(
                        DeletePaymentCommand.builder()
                                .paymentId(paymentId)
                                .orderId(orderId)
                                .build()
                );

        // Payment 삭제 성공
        DeletedPaymentEvent paymentEvent = new DeletedPaymentEvent(paymentId, orderId);

        fixture.whenPublishingA(paymentEvent)
                .expectDispatchedCommands(
                        DeleteDeliveryCommand.builder()
                                .deliveryId(deliveryId)
                                .orderId(orderId)
                                .build()
                );

        // Delivery 삭제 실패
        FailedDeleteDeliveryEvent failedDeliveryEvent = new FailedDeleteDeliveryEvent(deliveryId, orderId);

        fixture.whenPublishingA(failedDeliveryEvent);
    }
} 