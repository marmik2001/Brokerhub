package com.marmik.brokerhub.broker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single holding item returned from a broker.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HoldingItem {
    private String exchange;
    private String tradingSymbol;
    private String isin;

    /** Represents total quantity (Zerodha: quantity, Dhan: totalQty) */
    private int quantity;
    
    /** Represents T1 quantity (Zerodha: t1Quantity, Dhan: t1Qty) */
    private int t1Quantity;
    
    /** Represents collateral quantity (Zerodha: collateralQuantity, Dhan: collateralQty) */
    private String collateralQuantity;
    
    /** Represents average buy cost (Zerodha: averagePrice, Dhan: avgCostPrice) */
    private double averagePrice;

    /** Last traded market price */
    private double lastPrice;
    private double pnl;
    private double dayChange;
    private double dayChangePercentage;
}
