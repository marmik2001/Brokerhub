package com.marmik.brokerhub.broker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single position item returned from a broker.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionItem {
    private String exchange;
    private String tradingSymbol;
    
    /** Identifier from Dhan payload */
    private String securityId;

    /** Net quantity */
    private int quantity;
    
    /** Buy average or cost price */
    private double averagePrice;
    
    /** Current market price */
    private double lastPrice;

    private double unrealizedProfit;
    private double realizedProfit;
    
    /** Total PnL (unrealized + realized) */
    private double totalPnl;

    /** Position direction (e.g., LONG, SHORT) */
    private String positionType;
    
    /** Product type (e.g., CNC, MIS) */
    private String productType;
}
