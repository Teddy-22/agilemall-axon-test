package com.agilemall.order.aggregate;

import com.agilemall.common.dto.OrderDetailDTO;
import com.agilemall.common.dto.OrderStatusEnum;
import com.agilemall.order.command.*;
import com.agilemall.order.entity.OrderDetail;
import com.agilemall.order.events.*;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderAggregateTest {
    private FixtureConfiguration<OrderAggregate> fixture;
    private String orderId;
    private String userId;
    private LocalDateTime orderDateTime;
    private List<OrderDetailDTO> orderDetails;

    @BeforeEach
    void setUp() {
        fixture = new AggregateTestFixture<>(OrderAggregate.class);
        
        // 테스트에서 공통으로 사용할 데이터 설정
        orderId = "ORDER123";
        userId = "USER123";
        orderDateTime = LocalDateTime.now();
        orderDetails = new ArrayList<>();
        orderDetails.add(new OrderDetailDTO(orderId, "PRODUCT1", 2, 20000));
    }

    @Test
    @DisplayName("주문 생성 테스트")
    void createOrderTest() {
        // Given
        CreateOrderCommand command = CreateOrderCommand.builder()
                .orderId(orderId)
                .userId(userId)
                .orderDatetime(orderDateTime)
                .orderStatus(OrderStatusEnum.CREATED.value())
                .orderDetails(orderDetails)
                .totalOrderAmt(20000)
                .build();

        // When & Then
        fixture.givenNoPriorActivity()
                .when(command)
                .expectEvents(new CreatedOrderEvent() {{
                    setOrderId(orderId);
                    setUserId(userId);
                    setOrderDatetime(orderDateTime);
                    setOrderStatus(OrderStatusEnum.CREATED.value());
                    setOrderDetails(orderDetails);
                    setTotalOrderAmt(20000);
                }});
    }

    @Test
    @DisplayName("주문 수정 테스트")
    void updateOrderTest() {
        // Given
        List<OrderDetailDTO> updatedOrderDetails = new ArrayList<>();
        updatedOrderDetails.add(new OrderDetailDTO(orderId, "PRODUCT1", 3, 30000));

        CreatedOrderEvent previousEvent = new CreatedOrderEvent();
        previousEvent.setOrderId(orderId);
        previousEvent.setUserId(userId);
        previousEvent.setOrderDatetime(orderDateTime);
        previousEvent.setOrderStatus(OrderStatusEnum.CREATED.value());
        previousEvent.setOrderDetails(orderDetails);
        previousEvent.setTotalOrderAmt(20000);

        UpdateOrderCommand command = UpdateOrderCommand.builder()
                .orderId(orderId)
                .orderDatetime(LocalDateTime.now())
                .orderDetails(updatedOrderDetails)
                .totalOrderAmt(30000)
                .orderStatus(OrderStatusEnum.UPDATED.value())
                .build();

        // When & Then
        fixture.given(previousEvent)
                .when(command)
                .expectEvents(new UpdatedOrderEvent() {{
                    setOrderId(orderId);
                    setOrderDatetime(command.getOrderDatetime());
                    setOrderDetails(updatedOrderDetails);
                    setTotalOrderAmt(30000);
                    setOrderStatus(OrderStatusEnum.UPDATED.value());
                }});
    }

    @Test
    @DisplayName("주문 삭제 테스트")
    void deleteOrderTest() {
        // Given
        CreatedOrderEvent previousEvent = new CreatedOrderEvent();
        previousEvent.setOrderId(orderId);
        previousEvent.setUserId(userId);
        previousEvent.setOrderDatetime(orderDateTime);
        previousEvent.setOrderStatus(OrderStatusEnum.CREATED.value());
        previousEvent.setOrderDetails(orderDetails);
        previousEvent.setTotalOrderAmt(20000);

        DeleteOrderCommand command = DeleteOrderCommand.builder()
                .orderId(orderId)
                .build();

        // When & Then
        fixture.given(previousEvent)
                .when(command)
                .expectEvents(new DeletedOrderEvent(orderId));
    }
} 