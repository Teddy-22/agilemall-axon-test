package com.agilemall.payment.aggregate;

import com.agilemall.common.command.create.CreatePaymentCommand;
import com.agilemall.common.command.delete.DeletePaymentCommand;
import com.agilemall.common.command.update.UpdatePaymentCommand;
import com.agilemall.common.dto.PaymentDetailDTO;
import com.agilemall.common.dto.PaymentStatusEnum;
import com.agilemall.common.events.create.CreatedPaymentEvent;
import com.agilemall.common.events.delete.DeletedPaymentEvent;
import com.agilemall.common.events.update.UpdatedPaymentEvent;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class PaymentAggregateTest {
    private FixtureConfiguration<PaymentAggregate> fixture;
    private String paymentId;
    private String orderId;
    private List<PaymentDetailDTO> paymentDetails;
    private int totalPaymentAmt;

    @BeforeEach
    void setUp() {
        fixture = new AggregateTestFixture<>(PaymentAggregate.class);
        
        // 테스트에서 공통으로 사용할 데이터 설정
        paymentId = "PAY123";
        orderId = "ORDER123";
        totalPaymentAmt = 50000;
        
        paymentDetails = new ArrayList<>();
        paymentDetails.add(new PaymentDetailDTO(orderId, paymentId, "10", 30000)); // 카드결제
        paymentDetails.add(new PaymentDetailDTO(orderId, paymentId, "20", 20000)); // 포인트결제
    }

    @Test
    @DisplayName("결제 생성 테스트")
    void createPaymentTest() {
        // Given
        CreatePaymentCommand command = CreatePaymentCommand.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .totalPaymentAmt(totalPaymentAmt)
                .paymentDetails(paymentDetails)
                .build();

        // When & Then
        fixture.givenNoPriorActivity()
                .when(command)
                .expectEvents(new CreatedPaymentEvent() {{
                    setPaymentId(paymentId);
                    setOrderId(orderId);
                    setTotalPaymentAmt(totalPaymentAmt);
                    setPaymentDetails(paymentDetails);
                }});
    }

    @Test
    @DisplayName("결제 수정 테스트")
    void updatePaymentTest() {
        // Given
        List<PaymentDetailDTO> updatedPaymentDetails = new ArrayList<>();
        updatedPaymentDetails.add(new PaymentDetailDTO(orderId, paymentId, "10", 40000));
        updatedPaymentDetails.add(new PaymentDetailDTO(orderId, paymentId, "20", 20000));

        CreatePaymentCommand createCommand = CreatePaymentCommand.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .totalPaymentAmt(totalPaymentAmt)
                .paymentDetails(paymentDetails)
                .build();

        UpdatePaymentCommand updateCommand = UpdatePaymentCommand.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .totalPaymentAmt(60000)
                .paymentDetails(updatedPaymentDetails)
                .paymentStatus(PaymentStatusEnum.COMPLETED.value())
                .build();

        // When & Then
        fixture.given(new CreatedPaymentEvent() {{
                    setPaymentId(paymentId);
                    setOrderId(orderId);
                    setTotalPaymentAmt(totalPaymentAmt);
                    setPaymentDetails(paymentDetails);
                }})
                .when(updateCommand)
                .expectEvents(new UpdatedPaymentEvent() {{
                    setPaymentId(paymentId);
                    setOrderId(orderId);
                    setTotalPaymentAmt(60000);
                    setPaymentDetails(updatedPaymentDetails);
                    setPaymentStatus(PaymentStatusEnum.COMPLETED.value());
                }});
    }

    @Test
    @DisplayName("결제 삭제 테스트")
    void deletePaymentTest() {
        // Given
        CreatePaymentCommand createCommand = CreatePaymentCommand.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .totalPaymentAmt(totalPaymentAmt)
                .paymentDetails(paymentDetails)
                .build();

        DeletePaymentCommand deleteCommand = DeletePaymentCommand.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .build();

        // When & Then
        fixture.given(new CreatedPaymentEvent() {{
                    setPaymentId(paymentId);
                    setOrderId(orderId);
                    setTotalPaymentAmt(totalPaymentAmt);
                    setPaymentDetails(paymentDetails);
                }})
                .when(deleteCommand)
                .expectEvents(new DeletedPaymentEvent(paymentId, orderId));
    }
} 