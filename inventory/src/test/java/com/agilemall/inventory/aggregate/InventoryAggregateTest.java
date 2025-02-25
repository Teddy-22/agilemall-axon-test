package com.agilemall.inventory.aggregate;

import com.agilemall.common.command.create.CreateInventoryCommand;
import com.agilemall.common.command.update.UpdateInventoryQtyCommand;
import com.agilemall.common.dto.InventoryQtyAdjustTypeEnum;
import com.agilemall.inventory.entity.Inventory;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class InventoryAggregateTest {
    private FixtureConfiguration<Inventory> fixture;
    private String productId;
    private String productName;
    private int unitPrice;
    private int inventoryQty;

    @BeforeEach
    void setUp() {
        fixture = new AggregateTestFixture<>(Inventory.class);
        
        // 테스트에서 공통으로 사용할 데이터 설정
        productId = "PRODUCT1";
        productName = "테스트 상품";
        unitPrice = 10000;
        inventoryQty = 100;
    }

    @Test
    @DisplayName("재고 생성 테스트")
    void createInventoryTest() {
        // Given
        CreateInventoryCommand command = CreateInventoryCommand.builder()
                .productId(productId)
                .productName(productName)
                .unitPrice(unitPrice)
                .inventoryQty(inventoryQty)
                .build();

        // When & Then
        fixture.givenNoPriorActivity()
                .when(command)
                .expectState(state -> {
                    assert state.getProductId().equals(productId);
                    assert state.getProductName().equals(productName);
                    assert state.getUnitPrice() == unitPrice;
                    assert state.getInventoryQty() == inventoryQty;
                })
                .expectNoEvents();
    }

    @Test
    @DisplayName("재고 수량 증가 테스트")
    void increaseInventoryQtyTest() {
        // Given
        Inventory inventory = new Inventory();
        inventory.setProductId(productId);
        inventory.setProductName(productName);
        inventory.setUnitPrice(unitPrice);
        inventory.setInventoryQty(inventoryQty);

        UpdateInventoryQtyCommand updateCommand = UpdateInventoryQtyCommand.builder()
                .productId(productId)
                .adjustType(InventoryQtyAdjustTypeEnum.INCREASE.value())
                .adjustQty(50)
                .build();

        // When & Then
        fixture.given(inventory)
                .when(updateCommand)
                .expectState(state -> {
                    assert state.getInventoryQty() == 150;
                })
                .expectNoEvents();
    }

    @Test
    @DisplayName("재고 수량 감소 테스트")
    void decreaseInventoryQtyTest() {
        // Given
        Inventory inventory = new Inventory();
        inventory.setProductId(productId);
        inventory.setProductName(productName);
        inventory.setUnitPrice(unitPrice);
        inventory.setInventoryQty(inventoryQty);

        UpdateInventoryQtyCommand updateCommand = UpdateInventoryQtyCommand.builder()
                .productId(productId)
                .adjustType(InventoryQtyAdjustTypeEnum.DECREASE.value())
                .adjustQty(30)
                .build();

        // When & Then
        fixture.given(inventory)
                .when(updateCommand)
                .expectState(state -> {
                    assert state.getInventoryQty() == 70;
                })
                .expectNoEvents();
    }

    @Test
    @DisplayName("재고 수량 감소 시 0 이하가 되지 않는지 테스트")
    void decreaseInventoryQtyBelowZeroTest() {
        // Given
        Inventory inventory = new Inventory();
        inventory.setProductId(productId);
        inventory.setProductName(productName);
        inventory.setUnitPrice(unitPrice);
        inventory.setInventoryQty(inventoryQty);

        UpdateInventoryQtyCommand updateCommand = UpdateInventoryQtyCommand.builder()
                .productId(productId)
                .adjustType(InventoryQtyAdjustTypeEnum.DECREASE.value())
                .adjustQty(150)
                .build();

        // When & Then
        fixture.given(inventory)
                .when(updateCommand)
                .expectState(state -> {
                    assert state.getInventoryQty() == 0;
                })
                .expectNoEvents();
    }
} 