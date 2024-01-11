package com.agilemall.order.saga;

import com.agilemall.common.command.update.UpdatePaymentCommand;
import com.agilemall.common.config.Constants;
import com.agilemall.common.dto.OrderStatusEnum;
import com.agilemall.common.dto.PaymentStatusEnum;
import com.agilemall.common.dto.ServiceNameEnum;
import com.agilemall.order.events.CancelledUpdateOrderEvent;
import com.agilemall.order.events.CompletedUpdateOrderEvent;
import com.agilemall.common.events.update.FailedUpdatePaymentEvent;
import com.agilemall.common.events.update.UpdatedPaymentEvent;
import com.agilemall.order.command.CompleteUpdateOrderCommand;
import com.agilemall.order.events.FailedCompleteUpdateOrderEvent;
import com.agilemall.order.events.UpdatedOrderEvent;
import com.agilemall.order.service.CompensatingService;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.SagaLifecycle;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Saga
@Slf4j
public class OrderUpdatingSaga {
    @Autowired
    private transient CommandGateway commandGateway;
    @Autowired
    private CompensatingService compensatingService;

    private final HashMap<String, String> aggregateIdMap = new HashMap<>();

    @StartSaga
    @SagaEventHandler(associationProperty = "orderId")
    private void on(UpdatedOrderEvent event) {
        log.info("[Saga] UpdatedOrderEvent is received for Order Id: {}", event.getOrderId());
        log.info("===== [Update Order] #4: <UpdatePaymentCommand> =====");
        aggregateIdMap.put(ServiceNameEnum.ORDER.value(), event.getOrderId());
        aggregateIdMap.put(ServiceNameEnum.PAYMENT.value(), event.getPaymentId());

        UpdatePaymentCommand updatePaymentCommand = UpdatePaymentCommand.builder()
                .paymentId(event.getPaymentId())
                .orderId(event.getOrderId())
                .totalPaymentAmt(event.getTotalPaymentAmt())
                .paymentStatus(PaymentStatusEnum.CREATED.value())
                .paymentDetails(event.getPaymentDetails())
                .build();

        try {
            commandGateway.sendAndWait(updatePaymentCommand, Constants.GATEWAY_TIMEOUT, TimeUnit.SECONDS);
            //throw new Exception("Error is occurred during handle <UpdatedOrderEvent>");
        } catch(Exception e) {
            log.info(e.getMessage());
            if(event.isCompensation()) {  //보상처리이면 수행 안함(무한루프 방지)
                log.info("===== [Update Order] Transaction is Aborted =====");
                SagaLifecycle.end();
                return;
            }
            log.info("===== [Update Order] Compensation <CancelUpdateOrderCommand> ==== ");
            compensatingService.cancelUpdateOrder(aggregateIdMap);
        }
    }

    @SagaEventHandler(associationProperty = "orderId")
    private void on(UpdatedPaymentEvent event) {
        log.info("[Saga] UpdatedPaymentEvent is received for Order Id: {}", event.getOrderId());
        log.info("===== [Update Order] #5: <CompletedOrderUpdateCommand> =====");

        CompleteUpdateOrderCommand cmd = CompleteUpdateOrderCommand.builder()
                .orderId(event.getOrderId())
                .orderStatus(OrderStatusEnum.COMPLETED.value())
                .build();

        try {
            commandGateway.sendAndWait(cmd, Constants.GATEWAY_TIMEOUT, TimeUnit.SECONDS);
        } catch(Exception e) {
            log.error(e.getMessage());
            log.info("===== [Update Order] Compensation <CancelUpdateOrderCommand> ==== ");
            compensatingService.cancelUpdateOrder(aggregateIdMap);
        }
    }

    @SagaEventHandler(associationProperty = "orderId")
    private void on(FailedUpdatePaymentEvent event) {
        log.info("[Saga] FailedUpdatePaymentEvent is received for Order Id: {}", event.getOrderId());
        log.info("===== [Update Order] Compensation <CancelUpdateOrderCommand> ==== ");
        compensatingService.cancelUpdateOrder(aggregateIdMap);
    }

    @SagaEventHandler(associationProperty = "orderId")
    private void on(FailedCompleteUpdateOrderEvent event) {
        log.info("[Saga] FailedCompleteUpdateOrderEvent is received for Order Id: {}", event.getOrderId());

        log.info("===== [Update Order] Compensation <CancelUpdatePaymentCommand> ==== ");
        compensatingService.cancelUpdatePayment(aggregateIdMap);
        log.info("===== [Update Order] Compensation <CancelUpdateOrderCommand> ==== ");
        compensatingService.cancelUpdateOrder(aggregateIdMap);
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "orderId")
    private void on(CompletedUpdateOrderEvent event) {
        log.info("[Saga] [CompletedUpdateOrderEvent] is finished for Order Id: {}", event.getOrderId());
        log.info("===== [Updating Order] Transaction is Finished =====");

        //-- Report service에 레포트 업데이트
        compensatingService.updateReport(event.getOrderId(), false);
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "orderId")
    private void on(CancelledUpdateOrderEvent event) {
        log.info("[Saga] <CancelledUpdateOrderEvent> is finished for Order Id: {}", event.getOrderId());
        log.info("===== [Update Order] Transaction is Aborted =====");

        //-- Report service에 레포트 업데이트
        compensatingService.updateReport(event.getOrderId(), false);
    }
}
