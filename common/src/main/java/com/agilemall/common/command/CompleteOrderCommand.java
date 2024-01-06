package com.agilemall.common.command;

import lombok.Builder;
import lombok.Data;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.HashMap;

@Data
@Value
@Builder
public class CompleteOrderCommand {
    @TargetAggregateIdentifier
    private String orderId;
    private String orderStatus;
    private HashMap<String, String> aggregateIdMap;
}
