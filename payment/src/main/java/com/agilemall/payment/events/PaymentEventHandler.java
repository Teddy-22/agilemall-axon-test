package com.agilemall.payment.events;

import com.agilemall.common.dto.PaymentDetailDTO;
import com.agilemall.common.dto.PaymentStatusEnum;
import com.agilemall.common.events.create.CancelledCreatePaymentEvent;
import com.agilemall.common.events.create.CreatedPaymentEvent;
import com.agilemall.common.events.create.FailedCreatePaymentEvent;
import com.agilemall.common.events.update.FailedUpdatePaymentEvent;
import com.agilemall.common.events.update.UpdatedPaymentEvent;
import com.agilemall.payment.entity.Payment;
import com.agilemall.payment.entity.PaymentDetail;
import com.agilemall.payment.entity.PaymentDetailIdentity;
import com.agilemall.payment.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.AllowReplay;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.ResetHandler;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@ProcessingGroup("payment")
@AllowReplay
public class PaymentEventHandler {
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private transient EventGateway eventGateway;

    @EventHandler
    private void on(CreatedPaymentEvent event) {
        log.info("[@EventHandler] Handle <CreatedPaymentEvent> for Payment Id: {}", event.getPaymentId());
        log.info(event.toString());

        List<PaymentDetail> newPaymentDetails = new ArrayList<>();

        try {
            Payment payment = new Payment();
            payment.setPaymentId(event.getPaymentId());
            payment.setOrderId(event.getOrderId());
            payment.setTotalPaymentAmt(event.getTotalPaymentAmt());
            payment.setPaymentStatus(PaymentStatusEnum.COMPLETED.value());

            for(PaymentDetailDTO paymentDetail:event.getPaymentDetails()) {
                PaymentDetailIdentity paymentDetailIdentity = new PaymentDetailIdentity(
                        paymentDetail.getPaymentId(), paymentDetail.getPaymentKind()
                );
                PaymentDetail newPaymentDetail = new PaymentDetail();
                newPaymentDetail.setPaymentDetailIdentity(paymentDetailIdentity);
                newPaymentDetail.setPaymentAmt(paymentDetail.getPaymentAmt());
                newPaymentDetails.add(newPaymentDetail);
            }
            payment.setPaymentDetails(newPaymentDetails);

            paymentRepository.save(payment);
        } catch(Exception e) {
            log.error("Error is occurred during handle <PaymentProcessedEvent>: {}", e.getMessage());
            eventGateway.publish(new FailedCreatePaymentEvent(event.getPaymentId(), event.getOrderId()));
        }
    }

    @EventHandler
    private void on(CancelledCreatePaymentEvent event) {
        log.info("[@EventHandler] Handle <CancelledCreatePaymentEvent> for Payment Id: {}", event.getPaymentId());
        Optional<Payment> optPay = paymentRepository.findById(event.getPaymentId());
        if(optPay.isPresent()) {
            Payment payment = optPay.get();
            paymentRepository.delete(payment);
        }
    }

    @EventHandler
    private void on(UpdatedPaymentEvent event) {
        Optional<Payment> optPayment = paymentRepository.findById(event.getPaymentId());
        if(!optPayment.isPresent()) {
            log.info("Can't find Payment info for Payment Id: {}", event.getPaymentId());
            eventGateway.publish(new FailedUpdatePaymentEvent(event.getPaymentId(), event.getOrderId()));
            return;
        }

        try {
            Payment payment = optPayment.get();
            payment.setTotalPaymentAmt(event.getTotalPaymentAmt());
            payment.setPaymentStatus(PaymentStatusEnum.COMPLETED.value());
            payment.getPaymentDetails().clear();
            for(PaymentDetailDTO item:event.getPaymentDetails()) {
                payment.getPaymentDetails().add(new PaymentDetail(
                        (new PaymentDetailIdentity(item.getPaymentId(), item.getPaymentKind())),
                        item.getPaymentAmt()));
            }

            paymentRepository.save(payment);
        } catch(Exception e) {
            log.error(e.getMessage());
            eventGateway.publish(new FailedUpdatePaymentEvent(event.getPaymentId(), event.getOrderId()));
        }
    }

    @ResetHandler
    private void replayAll() {
        log.info("[PaymentEventHandler] Executing replayAll");
        paymentRepository.deleteAll();
    }
}
