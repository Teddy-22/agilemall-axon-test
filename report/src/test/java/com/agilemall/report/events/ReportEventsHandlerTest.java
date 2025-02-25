package com.agilemall.report.events;

import com.agilemall.common.dto.OrderDetailDTO;
import com.agilemall.common.dto.PaymentDetailDTO;
import com.agilemall.common.events.delete.DeletedReportEvent;
import com.agilemall.common.events.update.UpdatedOrderToReportEvent;
import com.agilemall.common.events.update.UpdatedPaymentToReportEvent;
import com.agilemall.report.entity.Report;
import com.agilemall.report.repository.ReportRepository;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReportEventsHandlerTest {

    @Mock
    private ReportRepository reportRepository;

    @InjectMocks
    private ReportEventsHandler reportEventsHandler;

    private String reportId;
    private String orderId;
    private String userId;
    private LocalDateTime orderDateTime;
    private Report report;
    private Gson gson;

    @BeforeEach
    void setUp() {
        reportId = "REPORT123";
        orderId = "ORDER123";
        userId = "USER123";
        orderDateTime = LocalDateTime.now();
        gson = new Gson();

        report = new Report();
        report.setReportId(reportId);
        report.setOrderId(orderId);
        report.setUserId(userId);
        report.setOrderDatetime(orderDateTime);
    }

    @Test
    @DisplayName("주문 정보 업데이트 이벤트 처리 테스트")
    void handleOrderUpdateEventTest() {
        // Given
        List<OrderDetailDTO> orderDetails = new ArrayList<>();
        orderDetails.add(new OrderDetailDTO(orderId, "PRODUCT1", 2, 20000));

        UpdatedOrderToReportEvent event = new UpdatedOrderToReportEvent();
        event.setOrderId(orderId);
        event.setOrderDatetime(orderDateTime);
        event.setTotalOrderAmt(20000);
        event.setOrderStatus("30");
        event.setOrderDetails(orderDetails);

        when(reportRepository.findByOrderId(orderId)).thenReturn(Optional.of(report));

        // When
        reportEventsHandler.on(event);

        // Then
        verify(reportRepository, times(1)).findByOrderId(orderId);
        verify(reportRepository, times(1)).save(any(Report.class));
    }

    @Test
    @DisplayName("결제 정보 업데이트 이벤트 처리 테스트")
    void handlePaymentUpdateEventTest() {
        // Given
        List<PaymentDetailDTO> paymentDetails = new ArrayList<>();
        paymentDetails.add(new PaymentDetailDTO(orderId, "PAY123", "10", 20000));

        UpdatedPaymentToReportEvent event = new UpdatedPaymentToReportEvent();
        event.setOrderId(orderId);
        event.setTotalPaymentAmt(20000);
        event.setPaymentStatus("30");
        event.setPaymentDetails(paymentDetails);

        when(reportRepository.findByOrderId(orderId)).thenReturn(Optional.of(report));

        // When
        reportEventsHandler.on(event);

        // Then
        verify(reportRepository, times(1)).findByOrderId(orderId);
        verify(reportRepository, times(1)).save(any(Report.class));
    }

    @Test
    @DisplayName("리포트 삭제 이벤트 처리 테스트")
    void handleDeleteReportEventTest() {
        // Given
        DeletedReportEvent event = new DeletedReportEvent(reportId, orderId);
        when(reportRepository.findByOrderId(orderId)).thenReturn(Optional.of(report));

        // When
        reportEventsHandler.on(event);

        // Then
        verify(reportRepository, times(1)).findByOrderId(orderId);
        verify(reportRepository, times(1)).delete(any(Report.class));
    }

    @Test
    @DisplayName("존재하지 않는 리포트에 대한 이벤트 처리 테스트")
    void handleEventWithNonExistentReportTest() {
        // Given
        DeletedReportEvent event = new DeletedReportEvent(reportId, orderId);
        when(reportRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        // When
        reportEventsHandler.on(event);

        // Then
        verify(reportRepository, times(1)).findByOrderId(orderId);
        verify(reportRepository, never()).delete(any(Report.class));
    }
} 