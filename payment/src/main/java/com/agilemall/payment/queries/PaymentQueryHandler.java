package com.agilemall.payment.queries;

import com.agilemall.common.dto.PaymentDTO;
import com.agilemall.common.dto.PaymentDetailDTO;
import com.agilemall.common.queries.Queries;
import com.agilemall.payment.entity.Payment;
import com.agilemall.payment.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PaymentQueryHandler {

    private final PaymentRepository paymentRepository;
    @Autowired
    public PaymentQueryHandler(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @QueryHandler(queryName = Queries.PAYMENT_BY_ORDER_ID)
    private PaymentDTO handle(String orderId) {
        log.info("[@QueryHandler] Handle <{}> for Order Id: {}", Queries.PAYMENT_BY_ORDER_ID, orderId);

        Optional<Payment> optPayment = paymentRepository.findByOrderId(orderId);
        if(optPayment.isPresent()) {
            Payment payment = optPayment.get();
            PaymentDTO paymentDTO = new PaymentDTO();
            BeanUtils.copyProperties(payment, paymentDTO);
            List<PaymentDetailDTO> newDetails = payment.getPaymentDetails().stream()
                    .map(o -> new PaymentDetailDTO(orderId, o.getPaymentDetailIdentity().getPaymentId(), o.getPaymentDetailIdentity().getPaymentKind(), o.getPaymentAmt()))
                    .collect(Collectors.toList());
            paymentDTO.setPaymentDetails(newDetails);

            return paymentDTO;
        } else {
            return null;
        }
    }

}
