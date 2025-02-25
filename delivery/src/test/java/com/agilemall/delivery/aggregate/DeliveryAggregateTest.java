package com.agilemall.delivery.aggregate;

import com.agilemall.common.command.create.CreateDeliveryCommand;
import com.agilemall.common.command.delete.DeleteDeliveryCommand;
import com.agilemall.common.dto.DeliveryStatusEnum;
import com.agilemall.common.events.create.CreatedDeliveryEvent;
import com.agilemall.common.events.delete.DeletedDeliveryEvent;
import com.agilemall.delivery.command.UpdateDeliveryCommand;
import com.agilemall.delivery.events.UpdatedDeliveryEvent;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class DeliveryAggregateTest {
    private FixtureConfiguration<DeliveryAggregate> fixture;
    private String deliveryId;
    private String orderId;

    @BeforeEach
    void setUp() {
        fixture = new AggregateTestFixture<>(DeliveryAggregate.class);
        
        // 테스트에서 공통으로 사용할 데이터 설정
        deliveryId = "DEL123";
        orderId = "ORDER123";
    }

    @Test
    @DisplayName("배송 생성 테스트")
    void createDeliveryTest() {
        // Given
        CreateDeliveryCommand command = CreateDeliveryCommand.builder()
                .deliveryId(deliveryId)
                .orderId(orderId)
                .deliveryStatus(DeliveryStatusEnum.CREATED.value())
                .build();

        // When & Then
        fixture.givenNoPriorActivity()
                .when(command)
                .expectEvents(new CreatedDeliveryEvent() {{
                    setDeliveryId(deliveryId);
                    setOrderId(orderId);
                    setDeliveryStatus(DeliveryStatusEnum.CREATED.value());
                }});
    }

    @Test
    @DisplayName("배송 상태 수정 테스트")
    void updateDeliveryStatusTest() {
        // Given
        CreateDeliveryCommand createCommand = CreateDeliveryCommand.builder()
                .deliveryId(deliveryId)
                .orderId(orderId)
                .deliveryStatus(DeliveryStatusEnum.CREATED.value())
                .build();

        UpdateDeliveryCommand updateCommand = UpdateDeliveryCommand.builder()
                .deliveryId(deliveryId)
                .orderId(orderId)
                .deliveryStatus(DeliveryStatusEnum.DELIVERING.value())
                .build();

        // When & Then
        fixture.given(new CreatedDeliveryEvent() {{
                    setDeliveryId(deliveryId);
                    setOrderId(orderId);
                    setDeliveryStatus(DeliveryStatusEnum.CREATED.value());
                }})
                .when(updateCommand)
                .expectEvents(new UpdatedDeliveryEvent() {{
                    setDeliveryId(deliveryId);
                    setOrderId(orderId);
                    setDeliveryStatus(DeliveryStatusEnum.DELIVERING.value());
                }});
    }

    @Test
    @DisplayName("배송 삭제 테스트")
    void deleteDeliveryTest() {
        // Given
        CreateDeliveryCommand createCommand = CreateDeliveryCommand.builder()
                .deliveryId(deliveryId)
                .orderId(orderId)
                .deliveryStatus(DeliveryStatusEnum.CREATED.value())
                .build();

        DeleteDeliveryCommand deleteCommand = DeleteDeliveryCommand.builder()
                .deliveryId(deliveryId)
                .orderId(orderId)
                .build();

        // When & Then
        fixture.given(new CreatedDeliveryEvent() {{
                    setDeliveryId(deliveryId);
                    setOrderId(orderId);
                    setDeliveryStatus(DeliveryStatusEnum.CREATED.value());
                }})
                .when(deleteCommand)
                .expectEvents(new DeletedDeliveryEvent(deliveryId, orderId));
    }
} 