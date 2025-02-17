package com.agilemall.common.queries;

public final class Queries {
    // Order queries
    public static final String ORDER_BY_ORDER_ID = "order.getByOrderId";
    public static final String ORDER_DETAIL_BY_ORDER_ID = "order.getDetailByOrderId";

    
    // Payment queries
    public static final String PAYMENT_BY_ORDER_ID = "payment.getByOrderId";
    
    // Delivery queries
    public static final String DELIVERY_BY_ORDER_ID = "delivery.getByOrderId";

    // Inventory queries
    
    // Report queries
    public static final String REPORT_BY_ORDER_ID = "report.getByOrderId";

    // 유틸리티 클래스의 인스턴트화 방지
    private Queries() {}
} 