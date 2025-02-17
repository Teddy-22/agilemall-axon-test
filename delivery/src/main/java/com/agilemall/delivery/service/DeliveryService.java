package com.agilemall.delivery.service;

import com.agilemall.common.command.update.UpdateInventoryQtyCommand;
import com.agilemall.common.config.Constants;
import com.agilemall.common.dto.DeliveryDTO;
import com.agilemall.common.dto.DeliveryStatusEnum;
import com.agilemall.common.dto.InventoryQtyAdjustTypeEnum;
import com.agilemall.common.dto.OrderDetailDTO;
import com.agilemall.common.queries.Queries;
import com.agilemall.common.vo.ResultVO;
import com.agilemall.delivery.command.UpdateDeliveryCommand;
import com.agilemall.delivery.entity.Delivery;
import com.agilemall.delivery.repository.DeliveryRepository;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class DeliveryService {

    private transient final CommandGateway commandGateway;
    private transient final QueryGateway queryGateway;
    private final DeliveryRepository deliveryRepository;

    @Autowired
    public DeliveryService(CommandGateway commandGateway, QueryGateway queryGateway, DeliveryRepository deliveryRepository) {
        this.commandGateway = commandGateway;
        this.queryGateway = queryGateway;
        this.deliveryRepository = deliveryRepository;
    }

    public ResultVO<Delivery> getDelivery(String orderId) {
        log.info("[DeliveryService] Executing <getDelivery> for Order Id: {}", orderId);
        ResultVO<Delivery> retVo = new ResultVO<>();
        Optional<Delivery> optDelivery = deliveryRepository.findByOrderId(orderId);
        if(optDelivery.isPresent()) {
            Delivery delivery = optDelivery.get();
            retVo.setReturnCode(true);
            retVo.setReturnMessage("Find");
            retVo.setResult(delivery);
        } else {
            retVo.setReturnCode(false);
            retVo.setReturnMessage("Can't find delivery info for Order Id <"+orderId+">");
        }
        return retVo;
    }

    public ResultVO<DeliveryDTO> updateDeliveryStatus(DeliveryDTO deliveryDTO) {
        log.info("[DeliveryService] Executing updateDeliveryStatus for Delivery Id: {}", deliveryDTO.getDeliveryId());

        ResultVO<DeliveryDTO> retVo;

        //-- Devering 상태로 변경 시 Inventory의 재고량을 줄이는 요청을 먼저 수행
        if(DeliveryStatusEnum.DELIVERING.value().equals(deliveryDTO.getDeliveryStatus())) {
            retVo = updateInventoryQty(deliveryDTO);
            if(retVo.isReturnCode()) {
                retVo = update(deliveryDTO);
            }
        } else {
            retVo = update(deliveryDTO);
        }
        return retVo;
    }

    private ResultVO<DeliveryDTO> update(DeliveryDTO deliveryDTO) {
        log.info("[DeliveryService] Executing <update> for Delivery Id: {}", deliveryDTO.getDeliveryId());

        ResultVO<DeliveryDTO> retVo = new ResultVO<>();
        try {
            UpdateDeliveryCommand updateDeliveryCommand = UpdateDeliveryCommand.builder()
                    .deliveryId(deliveryDTO.getDeliveryId())
                    .orderId(deliveryDTO.getOrderId())
                    .deliveryStatus(deliveryDTO.getDeliveryStatus())
                    .build();
            commandGateway.sendAndWait(updateDeliveryCommand, Constants.GATEWAY_TIMEOUT, TimeUnit.SECONDS);
            retVo.setReturnCode(true);
            retVo.setReturnMessage("Update delivery status to <"+deliveryDTO.getDeliveryStatus()+"> is success!");
            retVo.setResult(deliveryDTO);
        } catch(Exception e) {
            retVo.setReturnCode(false);
            retVo.setReturnMessage(e.getMessage());
        }
        return retVo;
    }

    private ResultVO<DeliveryDTO> updateInventoryQty(DeliveryDTO deliveryDTO) {
        log.info("[DeliveryService] Executing <updateInventoryQty> for Delivery Id: {}", deliveryDTO.getDeliveryId());

        ResultVO<DeliveryDTO> retVo = new ResultVO<>();

        try {
            List<OrderDetailDTO> orderDetails = queryGateway.query(
                    Queries.ORDER_DETAIL_BY_ORDER_ID,
                    deliveryDTO.getOrderId(),
                    ResponseTypes.multipleInstancesOf(OrderDetailDTO.class)).join();
            if(orderDetails == null) {
                retVo.setReturnCode(false);
                retVo.setReturnMessage("Can't find Order detail info for Order Id <"+deliveryDTO.getDeliveryId()+">");
                return retVo;
            }

            log.info("Get Order details: {}", orderDetails);
            UpdateInventoryQtyCommand cmd;
            List<OrderDetailDTO> successList = new ArrayList<>();
            for(OrderDetailDTO item:orderDetails) {
                cmd = UpdateInventoryQtyCommand.builder()
                        .productId(item.getProductId())
                        .adjustType(InventoryQtyAdjustTypeEnum.DECREASE.value())
                        .adjustQty(item.getQty())
                        .build();
                try {
                    commandGateway.sendAndWait(cmd, Constants.GATEWAY_TIMEOUT, TimeUnit.SECONDS);
                    successList.add(item);
                } catch(Exception e) {
                    log.error("Fail to send <InventoryQtyUpdateCommand>: {}", e.getMessage());
                }
            }

            //실패한 처리가 있을때는 rollback 처리함
            if(successList.size() < orderDetails.size()) {
                for(OrderDetailDTO item:successList) {
                    cmd = UpdateInventoryQtyCommand.builder()
                            .productId(item.getProductId())
                            .adjustType(InventoryQtyAdjustTypeEnum.INCREASE.value())
                            .adjustQty(item.getQty())
                            .build();
                    commandGateway.sendAndWait(cmd, Constants.GATEWAY_TIMEOUT, TimeUnit.SECONDS);
                }
                retVo.setReturnCode(false);
                retVo.setReturnMessage("Fail to update inventory quantity");
            } else {
                retVo.setReturnCode(true);
                retVo.setReturnMessage("Success to update inventory quantity");
            }
        } catch(Exception e) {
            retVo.setReturnCode(false);
            retVo.setReturnMessage(e.getMessage());
        }
        return retVo;
    }
}

